# Copyright 2024 Google LLC
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
    name = "key_attestation_manager_test",
    size = "small",
    srcs = ["KeyAttestationManagerTest.java"],
    data = ["//java/src/test/java/resources:key_attestation_verification_file_group"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.common.security.KeyAttestationManagerTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/security:key_attestation_manager",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
        "@maven//:org_apache_httpcomponents_client5_httpclient5",
        "@maven//:org_apache_httpcomponents_core5_httpcore5",
        "@maven//:org_mockito_mockito_core",
    ],
)

java_test(
    name = "authentication_filter_test",
    size = "small",
    srcs = ["AuthenticationFilterTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.common.security.AuthenticationFilterTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:constants",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:unique_id_generator",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:authorization_token_dao",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/security:authentication_filter",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/security:key_attestation_manager",
        "//shuffler/proto:task_assignments_java_proto",
        "@federatedcompute//fcp/protos/federatedcompute:federated_compute_java_proto",
        "@maven//:com_google_protobuf_protobuf_java",
        "@maven//:com_google_truth_truth",
        "@maven//:jakarta_servlet_jakarta_servlet_api",
        "@maven//:junit_junit",
        "@maven//:org_mockito_mockito_core",
    ],
)
