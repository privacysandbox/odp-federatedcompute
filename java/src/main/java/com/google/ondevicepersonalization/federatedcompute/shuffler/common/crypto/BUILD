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
    name = "payload",
    srcs = [
        "Payload.java",
    ],
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:compression_utils",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:lombok_java",
        "@coordinator-services-and-shared-libraries//:crypto_client",
        "@maven//:com_google_code_gson_gson",
        "@maven//:com_google_crypto_tink_tink",
    ],
)

java_library(
    name = "public_key",
    srcs = [
        "PublicKey.java",
    ],
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:lombok_java",
    ],
)

java_library(
    name = "public_keys",
    srcs = [
        "PublicKeys.java",
    ],
    deps = [
        ":public_key",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:lombok_java",
    ],
)

java_library(
    name = "public_key_fetching_service",
    srcs = ["PublicKeyFetchingService.java"],
    deps = [
        ":public_keys",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:constants",
        "@coordinator-services-and-shared-libraries//:error_utils",
        "@coordinator-services-and-shared-libraries//:http_client_wrapper",
        "@maven//:com_google_code_gson_gson",
        "@maven//:com_google_guava_guava",
        "@maven//:org_apache_httpcomponents_httpclient",
        "@maven//:org_slf4j_slf4j_api",
        "@maven//:org_springframework_spring_beans",
        "@maven//:org_springframework_spring_context",
    ],
)

java_library(
    name = "public_key_encryption_service",
    srcs = ["PublicKeyEncryptionService.java"],
    deps = [
        ":payload",
        ":public_key",
        ":public_key_fetching_service",
        ":public_keys",
        "@coordinator-services-and-shared-libraries//:key_utils",
        "@maven//:com_google_crypto_tink_tink",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_protobuf_protobuf_java",
        "@maven//:org_slf4j_slf4j_api",
        "@maven//:org_springframework_spring_beans",
        "@maven//:org_springframework_spring_context",
    ],
)
