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

package com.google.ondevicepersonalization.federatedcompute.shuffler.common.messaging.gcp;

import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configuration for GcpHttpMessageSender. */
@Configuration
public class GcpHttpMessageSenderConfig {

  @Bean
  public CloseableHttpClient messageSenderHttpClient() {
    RequestConfig requestConfig =
        RequestConfig.custom()
            .setConnectTimeout(3000)
            .setSocketTimeout(3000)
            .setConnectionRequestTimeout(3000)
            .setExpectContinueEnabled(true)
            .build();
    PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
    connManager.setMaxTotal(100);
    connManager.setDefaultMaxPerRoute(100);
    connManager.setValidateAfterInactivity(2000);
    return HttpClientBuilder.create()
        .setDefaultRequestConfig(requestConfig)
        .setConnectionManager(connManager)
        .evictExpiredConnections()
        .evictIdleConnections(30, TimeUnit.SECONDS)
        .build();
  }

  @Bean
  public GoogleCredentials httpScopedGoogleCredentials() throws IOException {
    GoogleCredentials googleCredentials = GoogleCredentials.getApplicationDefault();
    return googleCredentials.createScoped("https://www.googleapis.com/auth/cloud-platform");
  }
}
