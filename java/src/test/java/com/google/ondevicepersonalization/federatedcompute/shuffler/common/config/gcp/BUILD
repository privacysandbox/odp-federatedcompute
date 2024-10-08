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
    name = "gcp_metadata_service_client_test",
    size = "small",
    srcs = ["GcpVMMetadataServiceClientTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.common.config.gcp.GcpVMMetadataServiceClientTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/config/gcp:gcp_metadata_service_client",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_protobuf_protobuf_java",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
        "@maven//:org_apache_httpcomponents_client5_httpclient5",
        "@maven//:org_apache_httpcomponents_core5_httpcore5",
        "@maven//:org_apache_httpcomponents_httpclient",
        "@maven//:org_apache_httpcomponents_httpcore",
        "@maven//:org_mockito_mockito_core",
    ],
)

java_test(
    name = "gcp_parameter_client_test",
    size = "small",
    srcs = ["GcpParameterClientTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.common.config.gcp.GcpParameterClientTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/config/gcp:gcp_parameter_client",
        "@maven//:com_google_api_gax",
        "@maven//:com_google_api_grpc_proto_google_cloud_secretmanager_v1",
        "@maven//:com_google_cloud_google_cloud_secretmanager",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_protobuf_protobuf_java",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
        "@maven//:org_mockito_mockito_core",
    ],
)
