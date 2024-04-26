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

package com.google.ondevicepersonalization.federatedcompute.shuffler.common.crypto;

import com.google.common.primitives.Ints;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.scp.shared.api.util.ErrorUtil;
import com.google.scp.shared.api.util.HttpClientWrapper;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Class to handle fetching and parsing of public encryption keys */
@Component
public class PublicKeyFetchingService {

  private static final Logger logger = LoggerFactory.getLogger(PublicKeyFetchingService.class);
  private static final int REQUEST_TIMEOUT_DURATION =
      Ints.checkedCast(Duration.ofMinutes(1).toMillis());
  private final String publicKeyServiceBaseUrl;
  private final HttpClientWrapper httpClient;

  public PublicKeyFetchingService(
      String publicKeyServiceBaseUrl, HttpClientWrapper coordinatorPublicKeyServiceHttpClient) {
    this.publicKeyServiceBaseUrl = publicKeyServiceBaseUrl;
    this.httpClient = coordinatorPublicKeyServiceHttpClient;
  }

  public PublicKeys fetchPublicKeys() {
    URI fetchUri = URI.create(String.format("%s/publicKeys", publicKeyServiceBaseUrl));
    var request = new HttpGet(fetchUri);

    final RequestConfig requestConfig =
        RequestConfig.custom()
            // Timeout for requesting a connection from internal connection manager
            .setConnectionRequestTimeout(REQUEST_TIMEOUT_DURATION)
            // Timeout for establishing a request to host
            .setConnectTimeout(REQUEST_TIMEOUT_DURATION)
            // Timeout between data packets received
            .setSocketTimeout(REQUEST_TIMEOUT_DURATION)
            .build();
    request.setConfig(requestConfig);

    try {
      var response = httpClient.execute(request);
      var responseBody = response.responseBody();

      if (response.statusCode() != 200) {
        var errorResponse = ErrorUtil.parseErrorResponse(responseBody);
        var exception = ErrorUtil.toServiceException(errorResponse);

        var message = "Received error from public key vending service";
        logger.error(message, exception);
        throw new IllegalStateException(message, exception);
      } else {
        logger.info("Successfully fetched public keys using Uri: " + fetchUri);
      }

      return parseSuccessResponse(responseBody);
    } catch (IOException e) {
      var message = "Error fetching public keys";
      logger.error(message, e);
      throw new IllegalStateException(message, e);
    }
  }

  private PublicKeys parseSuccessResponse(String responseBody) {
    try {
      Gson gson = new Gson();
      PublicKeys publicKeys = gson.fromJson(responseBody, PublicKeys.class);
      return publicKeys;
    } catch (JsonSyntaxException jse) {
      throw new IllegalStateException("Failed to parse response", jse);
    }
  }
}
