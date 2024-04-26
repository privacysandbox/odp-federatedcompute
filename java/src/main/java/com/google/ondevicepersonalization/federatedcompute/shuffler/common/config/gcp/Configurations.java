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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.defaultprovider.EnvironmentVariableDefaultProvider;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.Constants;
import java.io.IOException;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configuration for parameters */
@Configuration
public class Configurations {

  @Bean
  GoogleCloudArgs provideGoogleCloudArgs() {
    GoogleCloudArgs googleCloudArgs = new GoogleCloudArgs();
    JCommander command =
        JCommander.newBuilder()
            .addObject(googleCloudArgs)
            .defaultProvider(new EnvironmentVariableDefaultProvider(Constants.FCP_OPTS, "--/"))
            .build();
    command.parse();
    return googleCloudArgs;
  }

  @Bean
  SecurityArgs provideSecurityArgs() {
    SecurityArgs securityArgs = new SecurityArgs();
    JCommander command =
        JCommander.newBuilder()
            .addObject(securityArgs)
            .defaultProvider(new EnvironmentVariableDefaultProvider(Constants.SECURITY_OPTS, "--/"))
            .build();
    command.parse();
    return securityArgs;
  }

  @Bean
  @Qualifier("httpClient")
  HttpClient provideHttpClient() {
    return HttpClients.createDefault();
  }

  @Bean
  SecretManagerServiceClient provideSecretManagerServiceClient() throws IOException {
    return SecretManagerServiceClient.create();
  }
}
