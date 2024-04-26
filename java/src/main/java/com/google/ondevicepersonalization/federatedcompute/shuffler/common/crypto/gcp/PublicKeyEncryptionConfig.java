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

import com.google.crypto.tink.hybrid.HybridConfig;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.Constants;
import com.google.scp.shared.api.util.HttpClientWrapper;
import java.security.GeneralSecurityException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Public key encryption key service config */
@Configuration
public class PublicKeyEncryptionConfig {
  static {
    try {
      HybridConfig.register();
    } catch (GeneralSecurityException e) {
      throw new RuntimeException("Error initializing tink.");
    }
  }

  @Bean
  public HttpClientWrapper coordinatorPublicKeyServiceHttpClient() {
    return HttpClientWrapper.builder()
        .setExponentialBackoff(
            Constants.COORDINATOR_HTTPCLIENT_RETRY_INITIAL_INTERVAL,
            Constants.COORDINATOR_HTTPCLIENT_RETRY_MULTIPLIER,
            Constants.COORDINATOR_HTTPCLIENT_MAX_ATTEMPTS)
        .build();
  }
}
