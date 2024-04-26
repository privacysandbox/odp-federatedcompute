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
    name = "partner",
    srcs = ["Partner.java"],
    deps = [
        ":http_client_utils",
        "//shuffler/proto:task_java_proto",
        "@com_google_protobuf//java/core",
        "@maven//:com_google_auth_google_auth_library_oauth2_http",
        "@maven//:com_google_cloud_google_cloud_storage",
    ],
)

java_library(
    name = "device",
    srcs = ["Device.java"],
    deps = [
        ":http_client_utils",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:compression_utils",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/crypto:payload",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/crypto:public_key_encryption_service",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/crypto:public_key_fetching_service",
        "//shuffler/proto:task_assignments_java_proto",
        "//shuffler/proto:task_java_proto",
        "@com_google_protobuf//java/core",
        "@coordinator-services-and-shared-libraries//:http_client_wrapper",
        "@federatedcompute//fcp/protos/federatedcompute:federated_compute_java_proto",
        "@maven//:com_google_code_gson_gson",
        "@maven//:com_google_crypto_tink_tink",
    ],
)

java_library(
    name = "http_client_utils",
    srcs = ["HttpClientUtils.java"],
    deps = [
        "//shuffler/proto:task_assignments_java_proto",
        "//shuffler/proto:task_java_proto",
        "@com_google_protobuf//java/core",
        "@federatedcompute//fcp/protos/federatedcompute:federated_compute_java_proto",
    ],
)
