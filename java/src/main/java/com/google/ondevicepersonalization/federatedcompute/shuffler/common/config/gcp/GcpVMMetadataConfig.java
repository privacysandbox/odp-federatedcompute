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

import com.google.common.base.Strings;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * GCP metadata configuration
 *
 * <p>See https://cloud.google.com/compute/docs/metadata/querying-metadata for GCP metadata service.
 */
@Configuration
public class GcpVMMetadataConfig {

  private static final Logger logger = LoggerFactory.getLogger(GcpVMMetadataConfig.class);

  @Bean
  @Qualifier("projectId")
  public String projectId(
      GoogleCloudArgs googleCloudArgs, GcpVMMetadataServiceClient gcpVMMetadataServiceClient)
      throws IOException {
    String gcpProjectId = googleCloudArgs.getProjectId();
    if (Strings.isNullOrEmpty(gcpProjectId)) {
      gcpProjectId = gcpVMMetadataServiceClient.getGcpProjectId();
    }
    logger.info("Registering gcpProjectId parameter as: " + gcpProjectId);
    return gcpProjectId;
  }

  @Bean
  @Qualifier("env")
  public String env(
      GcpVMMetadataServiceClient gcpVMMetadataServiceClient, GoogleCloudArgs googleCloudArgs)
      throws IOException {
    String environment = googleCloudArgs.getEnvironment();
    if (Strings.isNullOrEmpty(environment)) {
      environment = gcpVMMetadataServiceClient.getMetadata("environment").orElse("");
    }
    logger.info("Registering environment parameter as: " + environment);
    return environment;
  }
}
