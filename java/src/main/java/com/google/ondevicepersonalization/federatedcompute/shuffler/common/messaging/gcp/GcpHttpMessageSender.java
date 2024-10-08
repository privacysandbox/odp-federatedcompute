// Copyright 2024 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
////////////////////////////////////////////////////////////////////////////////

package com.google.ondevicepersonalization.federatedcompute.shuffler.common.messaging.gcp;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.gson.Gson;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.messaging.HttpMessageSender;
import com.google.scp.shared.api.util.ErrorUtil;
import java.io.IOException;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.HttpVersion;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/** PubSub message sender class. Used to send/publish messages to PubSub. */
@Component
public class GcpHttpMessageSender implements HttpMessageSender {
  private static final Logger logger = LoggerFactory.getLogger(GcpHttpMessageSender.class);
  private final CloseableHttpClient httpClient;
  private final GoogleCredentials googleCredentials;

  @Autowired
  public GcpHttpMessageSender(
      @Qualifier("messageSenderHttpClient") CloseableHttpClient httpClient,
      @Qualifier("httpScopedGoogleCredentials") GoogleCredentials googleCredentials) {
    this.httpClient = httpClient;
    this.googleCredentials = googleCredentials;
  }

  @Override
  public <T> void sendMessage(T message, String endpoint) {
    try {
      HttpPost request = new HttpPost(endpoint);
      request.setVersion(HttpVersion.HTTP_1_1);
      request.setHeader("Content-Type", "application/json");
      request.addHeader("Authorization", String.format("Bearer %s", getAccessToken()));

      Gson gson = new Gson();
      request.setEntity(new StringEntity(gson.toJson(message), ContentType.APPLICATION_JSON));

      CloseableHttpResponse response = httpClient.execute(request);
      try {
        String responseBody = new String(response.getEntity().getContent().readAllBytes());

        if (response.getCode() != HttpStatus.SC_OK) {
          var errorResponse = ErrorUtil.parseErrorResponse(responseBody);
          var exception = ErrorUtil.toServiceException(errorResponse);

          var errMessage = "Received error from endpoint when sending message";
          logger.error(errMessage, exception);
          throw new IllegalStateException(errMessage, exception);
        }
      } finally {
        EntityUtils.consumeQuietly(response.getEntity());
        response.close();
      }
    } catch (Exception e) {
      String errorMessage = "Failed to send http message to " + endpoint;
      logger.atError().setCause(e).log(errorMessage);
      throw new IllegalStateException(errorMessage, e);
    }
  }

  private String getAccessToken() throws IOException {
    googleCredentials.refreshIfExpired();
    String accessToken = googleCredentials.getAccessToken().getTokenValue();
    return accessToken;
  }
}
