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
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.StandardCookieSpec;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configuration for GcpHttpMessageSender. */
@Configuration
public class GcpHttpMessageSenderConfig {

  @Bean
  public CloseableHttpClient messageSenderHttpClient() {
    PoolingHttpClientConnectionManager connectionManager =
        PoolingHttpClientConnectionManagerBuilder.create()
            .setDefaultSocketConfig(
                SocketConfig.custom().setSoTimeout(Timeout.ofMinutes(1)).build())
            .setPoolConcurrencyPolicy(PoolConcurrencyPolicy.STRICT)
            .setConnPoolPolicy(PoolReusePolicy.LIFO)
            .setDefaultConnectionConfig(
                ConnectionConfig.custom()
                    .setSocketTimeout(Timeout.ofMinutes(1))
                    .setConnectTimeout(Timeout.ofMinutes(1))
                    .setTimeToLive(TimeValue.ofMinutes(10))
                    .build())
            .build();
    return HttpClients.custom()
        .setConnectionManager(connectionManager)
        .setDefaultRequestConfig(
            RequestConfig.custom().setCookieSpec(StandardCookieSpec.STRICT).build())
        .build();
  }

  @Bean
  public GoogleCredentials httpScopedGoogleCredentials() throws IOException {
    GoogleCredentials googleCredentials = GoogleCredentials.getApplicationDefault();
    return googleCredentials.createScoped("https://www.googleapis.com/auth/cloud-platform");
  }
}
