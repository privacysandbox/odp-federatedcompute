// Copyright 2023 Google LLC
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
package com.google.ondevicepersonalization.federatedcompute.shuffler.common.config.gcp;

import com.beust.jcommander.Parameter;
import lombok.Getter;

@Getter
public class SecurityArgs {
  @Parameter(
      names = "--key_attestation_validation_base_url",
      description = "The base url of the key attestation validation service")
  private String keyAttestationServiceBaseUrl;

  @Parameter(
      names = "--key_attestation_api_key",
      description = "The api key of the key attestation validation service")
  private String keyAttestationApiKey;

  @Parameter(
      names = "--is_authentication_enabled",
      description = "Whether to enable authentication when accepting clients' requests")
  private Boolean isAuthenticationEnabled;

  @Parameter(
      names = "--allow_rooted_devices",
      description =
          "Whether to allow rooted devices when accepting clients' requests. "
              + "This setting will have no effect when authentication is disabled.")
  private Boolean allowRootedDevices;
}
