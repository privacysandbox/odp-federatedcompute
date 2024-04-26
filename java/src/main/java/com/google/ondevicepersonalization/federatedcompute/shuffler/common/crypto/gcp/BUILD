# Copyright 2023 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

package(default_visibility = ["//visibility:public"])

java_library(
    name = "multiparty_decryption_config",
    srcs = ["MultiPartyDecryptionConfig.java"],
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:constants",
        "@coordinator-services-and-shared-libraries//:cloud_aead_selector",
        "@coordinator-services-and-shared-libraries//:crypto_client",
        "@coordinator-services-and-shared-libraries//:gcp_credentials_helper",
        "@coordinator-services-and-shared-libraries//:http_client_wrapper",
        "@maven//:com_google_auth_google_auth_library_oauth2_http",
        "@maven//:com_google_crypto_tink_tink",
        "@maven//:com_google_crypto_tink_tink_gcpkms",
        "@maven//:com_google_guava_guava",
        "@maven//:org_springframework_spring_beans",
        "@maven//:org_springframework_spring_context",
    ],
)

java_library(
    name = "public_key_decryption_config",
    srcs = ["PublicKeyEncryptionConfig.java"],
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:constants",
        "@coordinator-services-and-shared-libraries//:http_client_wrapper",
        "@maven//:com_google_crypto_tink_tink",
        "@maven//:com_google_guava_guava",
        "@maven//:org_springframework_spring_beans",
        "@maven//:org_springframework_spring_context",
    ],
)
