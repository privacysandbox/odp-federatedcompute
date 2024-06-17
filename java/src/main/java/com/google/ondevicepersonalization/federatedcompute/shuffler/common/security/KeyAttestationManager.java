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

package com.google.ondevicepersonalization.federatedcompute.shuffler.common.security;

import static com.google.ondevicepersonalization.federatedcompute.shuffler.common.Constants.FEDERATED_COMPUTE_PACKAGE_ALLOWED;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.scp.shared.api.util.ErrorUtil;
import java.net.URI;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/** Manage the key attestation http communication with Key Attestation Validation Service. */
@Component
public class KeyAttestationManager {

  private static final Logger logger = LoggerFactory.getLogger(KeyAttestationManager.class);

  private final String keyAttestationServiceBaseUrl;

  private final String keyAttestationApiKey;

  private final CloseableHttpClient httpClient;
  private final Boolean allowRootedDevices;

  private static final String CHALLENGE_BODY = "{\"ttl\": {\"seconds\": 3600}}";

  @Autowired
  public KeyAttestationManager(
      @Qualifier("keyAttestationServiceBaseUrl") Optional<String> keyAttestationServiceBaseUrl,
      @Qualifier("keyAttestationApiKey") Optional<String> keyAttestationApiKey,
      CloseableHttpClient keyAttestationServiceHttpClient,
      @Qualifier("isAuthenticationEnabled") Boolean isAuthenticationEnabled,
      @Qualifier("allowRootedDevices") Boolean allowRootedDevices) {
    if (isAuthenticationEnabled) {
      this.keyAttestationServiceBaseUrl = keyAttestationServiceBaseUrl.orElseThrow();
      this.keyAttestationApiKey = keyAttestationApiKey.orElseThrow();
    } else {
      this.keyAttestationServiceBaseUrl = null;
      this.keyAttestationApiKey = null;
    }
    this.httpClient = keyAttestationServiceHttpClient;
    this.allowRootedDevices = allowRootedDevices;
  }

  /**
   * Fetches a public key challenge from Key attestation validation service.
   *
   * @return the challenge in byte array.
   */
  public byte[] fetchChallenge() {
    URI fetchUri = URI.create(String.format("%s:generateChallenge", keyAttestationServiceBaseUrl));
    HttpPost request = new HttpPost(fetchUri);
    request.setHeader("Content-Type", "application/json; utf-8");
    request.setHeader("Accept", "application/json");
    request.setHeader("X-Goog-Api-Key", keyAttestationApiKey);
    try {
      request.setEntity(new StringEntity(CHALLENGE_BODY));
      CloseableHttpResponse response = httpClient.execute(request);
      try {
        String responseBody = new String(response.getEntity().getContent().readAllBytes());

        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
          var errorResponse = ErrorUtil.parseErrorResponse(responseBody);
          var exception = ErrorUtil.toServiceException(errorResponse);

          var message = "Received error from KAVS when creating challenge";
          logger.error(message, exception);
          throw new IllegalStateException(message, exception);
        }
        return parseFetchChallengeBody(responseBody);
      } finally {
        EntityUtils.consumeQuietly(response.getEntity());
        response.close();
      }
    } catch (Exception exception) {
      String message = "Failed to fetch challenge from KAVS using URI: " + fetchUri;
      logger.error(message, exception);
      throw new IllegalStateException(message, exception);
    }
  }

  /**
   * Verifies whether an attestation record is from a genuine device in a certified package.
   *
   * @param attestationRecord is the record to verify
   * @return whether attestationRecord is from a genuine device with certified packages. This method
   *     throws an exception if there is error parsing the response or the return status is not
   *     successful.
   */
  public boolean isAttestationRecordVerified(String attestationRecord) {
    logger.debug("Verifying attestation record: " + attestationRecord);
    URI verifyURI =
        URI.create(String.format("%s:verifyKeyAttestationRecord", keyAttestationServiceBaseUrl));
    HttpPost request = new HttpPost(verifyURI);
    request.setHeader("Content-Type", "application/json; utf-8");
    request.setHeader("Accept", "application/json");
    request.setHeader("X-Goog-Api-Key", keyAttestationApiKey);
    try {
      JsonObject verifyRequestJson = new JsonObject();
      verifyRequestJson.add(
          "keyAttestationCertificateChain",
          JsonParser.parseString(attestationRecord).getAsJsonArray());
      logger.debug(
          "Sending request to KAVS server for verification with entity: "
              + verifyRequestJson.toString());
      request.setEntity(new StringEntity(verifyRequestJson.toString()));

      CloseableHttpResponse response = httpClient.execute(request);
      try {
        String responseBody = new String(response.getEntity().getContent().readAllBytes());

        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
          var errorResponse = ErrorUtil.parseErrorResponse(responseBody);
          var exception = ErrorUtil.toServiceException(errorResponse);
          String message = "Received error from KAVS when verifying challenge";
          logger.error(message, exception);
          throw new IllegalStateException(message, exception);
        }
        return isVerificationResponseBodyValid(responseBody);
      } finally {
        EntityUtils.consumeQuietly(response.getEntity());
        response.close();
      }
    } catch (Exception exception) {
      String message = "Failed to verify attestation record from KAVS using URI: " + verifyURI;
      logger.error(message, exception);
      throw new IllegalStateException(message, exception);
    }
  }

  private byte[] parseFetchChallengeBody(String responseBody) {
    JsonElement responseJson = JsonParser.parseString(responseBody);
    String encodedChallenge = responseJson.getAsJsonObject().get("challenge").getAsString();
    return Base64.getDecoder().decode(encodedChallenge);
  }

  private boolean isVerificationResponseBodyValid(String responseBody) {
    try {
      JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();

      // 1. Boot State
      // Skip boot state check if rooted devices are allowed.
      // This recommended to only be enabled for testing purposes.
      if (!allowRootedDevices) {
        JsonObject rootOfTrust =
            responseJson
                .getAsJsonObject("keyDescription")
                .getAsJsonObject("teeEnforced")
                .getAsJsonObject("rootOfTrust");
        logger.debug("rootOfTrust Object: " + rootOfTrust.toString());
        if (!rootOfTrust.has("verifiedBootState")
            || !Objects.equals(rootOfTrust.get("verifiedBootState").getAsString(), "VERIFIED")) {
          return false;
        }

        if (!rootOfTrust.has("deviceLocked") || !rootOfTrust.get("deviceLocked").getAsBoolean()) {
          return false;
        }
      }

      // 2. Package name
      JsonArray packageInfos =
          responseJson
              .getAsJsonObject("keyDescription")
              .getAsJsonObject("softwareEnforced")
              .getAsJsonObject("attestationApplicationId")
              .getAsJsonArray("packageInfos");
      logger.debug("packageInfos Object: " + packageInfos.toString());
      if (packageInfos.isEmpty()) {
        return false;
      }
      return FEDERATED_COMPUTE_PACKAGE_ALLOWED.contains(
          packageInfos.get(0).getAsJsonObject().get("name").getAsString());
    } catch (Exception exception) {
      String message = "Error parsing attestation record verification from KAVS";
      logger.error(message, exception);
      throw new IllegalStateException(message, exception);
    }
  }
}
