/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ondevicepersonalization.federatedcompute.shuffler.common.config.gcp;

import static java.util.stream.Collectors.joining;

import com.google.api.gax.rpc.NotFoundException;
import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Parameter client implementation for getting runtime parameters from GCP secret manager */
@Component
public final class GcpParameterClient {

  private static final Logger logger = LoggerFactory.getLogger(GcpParameterClient.class);
  private static final String DEFAULT_PARAM_PREFIX = "fc";

  private final SecretManagerServiceClient secretManagerServiceClient;
  private final String projectId;
  private final String environment;

  public GcpParameterClient(
      SecretManagerServiceClient secretManagerServiceClient, String projectId, String env) {
    this.secretManagerServiceClient = secretManagerServiceClient;
    this.projectId = projectId;
    this.environment = env;
  }

  public Optional<String> getParameter(String param) {
    String fullParamName =
        Stream.of(DEFAULT_PARAM_PREFIX, environment, param)
            .filter(e -> !e.isEmpty())
            .collect(joining("-"));

    String secretName =
        String.format("projects/%s/secrets/%s/versions/latest", projectId, fullParamName);

    logger.info("Fetching parameter %s from GCP secret manager.".format(secretName));
    AccessSecretVersionResponse response;
    try {
      response = this.secretManagerServiceClient.accessSecretVersion(secretName);
    } catch (NotFoundException e) {
      return Optional.empty();
    } catch (Exception e) {
      throw new IllegalStateException(
          String.format("Error reading parameter %s from GCP secret manager.", param), e);
    }

    return Optional.of(response.getPayload().getData().toStringUtf8());
  }
}
