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

load("@rules_java//java:defs.bzl", "java_test")

java_test(
    name = "public_key_fetching_service_test",
    size = "small",
    srcs = ["PublicKeyFetchingServiceTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.common.crypto.PublicKeyFetchingServiceTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/crypto:public_key_fetching_service",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/crypto:public_keys",
        "@coordinator-services-and-shared-libraries//:http_client_wrapper",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
        "@maven//:org_mockito_mockito_core",
    ],
)

java_test(
    name = "public_key_encryption_service_test",
    size = "small",
    srcs = ["PublicKeyEncryptionServiceTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.common.crypto.PublicKeyEncryptionServiceTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/crypto:payload",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/crypto:public_key",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/crypto:public_key_encryption_service",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/crypto:public_key_fetching_service",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/crypto:public_keys",
        "@coordinator-services-and-shared-libraries//:http_client_wrapper",
        "@maven//:com_google_crypto_tink_tink",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
        "@maven//:org_mockito_mockito_core",
    ],
)
