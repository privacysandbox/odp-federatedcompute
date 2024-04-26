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

package com.google.ondevicepersonalization.federatedcompute.shuffler.common.crypto.gcp;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.base.Strings;
import com.google.crypto.tink.hybrid.HybridConfig;
import com.google.crypto.tink.integration.gcpkms.GcpKmsClient;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.Constants;
import com.google.scp.operator.cpio.cryptoclient.DecryptionKeyService;
import com.google.scp.operator.cpio.cryptoclient.EncryptionKeyFetchingService;
import com.google.scp.operator.cpio.cryptoclient.HttpEncryptionKeyFetchingService;
import com.google.scp.operator.cpio.cryptoclient.MultiPartyDecryptionKeyServiceImpl;
import com.google.scp.shared.api.util.HttpClientWrapper;
import com.google.scp.shared.clients.configclient.gcp.CredentialsHelper;
import com.google.scp.shared.crypto.tink.CloudAeadSelector;
import com.google.scp.shared.gcp.util.GcpHttpInterceptorUtil;
import java.io.IOException;
import java.security.GeneralSecurityException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Decryption key service config */
@Configuration
public class MultiPartyDecryptionConfig {

  static {
    try {
      HybridConfig.register();
    } catch (GeneralSecurityException e) {
      throw new RuntimeException("Error initializing tink.");
    }
  }

  @Bean
  public GoogleCredentials coordinatorACredentials(String wipProviderA, String serviceAccountA)
      throws IOException {
    String googleApplicationCredentials = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
    // Check for GOOGLE_APPLICATION_CREDENTIALS for local development.
    // This should not be set in the TEE otherwise attestation will fail causing key fetch errors.
    if (!Strings.isNullOrEmpty(googleApplicationCredentials)) {
      return GoogleCredentials.getApplicationDefault();
    }
    return CredentialsHelper.getAttestedCredentials(wipProviderA, serviceAccountA);
  }

  @Bean
  public GoogleCredentials coordinatorBCredentials(String wipProviderB, String serviceAccountB)
      throws IOException {
    String googleApplicationCredentials = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
    // Check for GOOGLE_APPLICATION_CREDENTIALS for local development.
    // This should not be set in the TEE otherwise attestation will fail causing key fetch errors.
    if (!Strings.isNullOrEmpty(googleApplicationCredentials)) {
      return GoogleCredentials.getApplicationDefault();
    }
    return CredentialsHelper.getAttestedCredentials(wipProviderB, serviceAccountB);
  }

  @Bean
  public HttpClientWrapper coordinatorAHttpClient(String encryptionKeyServiceACloudfunctionUrl) {
    return HttpClientWrapper.builder()
        .setInterceptor(
            GcpHttpInterceptorUtil.createHttpInterceptor(encryptionKeyServiceACloudfunctionUrl))
        .setExponentialBackoff(
            Constants.COORDINATOR_HTTPCLIENT_RETRY_INITIAL_INTERVAL,
            Constants.COORDINATOR_HTTPCLIENT_RETRY_MULTIPLIER,
            Constants.COORDINATOR_HTTPCLIENT_MAX_ATTEMPTS)
        .build();
  }

  @Bean
  public HttpClientWrapper coordinatorBHttpClient(String encryptionKeyServiceBCloudfunctionUrl) {
    return HttpClientWrapper.builder()
        .setInterceptor(
            GcpHttpInterceptorUtil.createHttpInterceptor(encryptionKeyServiceBCloudfunctionUrl))
        .setExponentialBackoff(
            Constants.COORDINATOR_HTTPCLIENT_RETRY_INITIAL_INTERVAL,
            Constants.COORDINATOR_HTTPCLIENT_RETRY_MULTIPLIER,
            Constants.COORDINATOR_HTTPCLIENT_MAX_ATTEMPTS)
        .build();
  }

  @Bean
  public EncryptionKeyFetchingService coordinatorAEncryptionKeyFetchingService(
      HttpClientWrapper coordinatorAHttpClient, String encryptionKeyServiceABaseUrl) {
    return new HttpEncryptionKeyFetchingService(
        coordinatorAHttpClient, encryptionKeyServiceABaseUrl);
  }

  @Bean
  public EncryptionKeyFetchingService coordinatorBEncryptionKeyFetchingService(
      HttpClientWrapper coordinatorBHttpClient, String encryptionKeyServiceBBaseUrl) {
    return new HttpEncryptionKeyFetchingService(
        coordinatorBHttpClient, encryptionKeyServiceBBaseUrl);
  }

  @Bean
  public CloudAeadSelector coordinatorAKmsClient(GoogleCredentials coordinatorACredentials) {
    return (kmsKeyResourceName) -> {
      GcpKmsClient client = new GcpKmsClient();
      try {
        client.withCredentials(coordinatorACredentials);
        return client.getAead(kmsKeyResourceName);
      } catch (GeneralSecurityException e) {
        throw new RuntimeException(
            String.format("Error getting gcloud Aead with uri %s.", kmsKeyResourceName), e);
      }
    };
  }

  @Bean
  public CloudAeadSelector coordinatorBKmsClient(GoogleCredentials coordinatorBCredentials) {
    return (kmsKeyResourceName) -> {
      GcpKmsClient client = new GcpKmsClient();
      try {
        client.withCredentials(coordinatorBCredentials);
        return client.getAead(kmsKeyResourceName);
      } catch (GeneralSecurityException e) {
        throw new RuntimeException(
            String.format("Error getting gcloud Aead with uri %s.", kmsKeyResourceName), e);
      }
    };
  }

  @Bean
  public DecryptionKeyService decryptionKeyService(
      CloudAeadSelector coordinatorAKmsClient,
      CloudAeadSelector coordinatorBKmsClient,
      EncryptionKeyFetchingService coordinatorAEncryptionKeyFetchingService,
      EncryptionKeyFetchingService coordinatorBEncryptionKeyFetchingService) {
    return new MultiPartyDecryptionKeyServiceImpl(
        coordinatorAEncryptionKeyFetchingService,
        coordinatorBEncryptionKeyFetchingService,
        coordinatorAKmsClient,
        coordinatorBKmsClient);
  }
}
