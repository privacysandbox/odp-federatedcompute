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

package(default_visibility = ["//visibility:public"])

java_library(
    name = "google_cloud_args",
    srcs = [
        "GoogleCloudArgs.java",
    ],
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:lombok_java",
        "@maven//:com_beust_jcommander",
    ],
)

java_library(
    name = "security_args",
    srcs = ["SecurityArgs.java"],
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:lombok_java",
        "@maven//:com_beust_jcommander",
    ],
)

java_library(
    name = "configurations",
    srcs = ["Configurations.java"],
    deps = [
        "google_cloud_args",
        "security_args",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:constants",
        "@maven//:com_beust_jcommander",
        "@maven//:com_google_cloud_google_cloud_secretmanager",
        "@maven//:org_apache_httpcomponents_httpclient",
        "@maven//:org_springframework_spring_beans",
        "@maven//:org_springframework_spring_context",
    ],
)

java_library(
    name = "gcp_metadata_config",
    srcs = ["GcpVMMetadataConfig.java"],
    deps = [
        "google_cloud_args",
        ":gcp_metadata_service_client",
        "@maven//:com_beust_jcommander",
        "@maven//:com_google_guava_guava",
        "@maven//:org_slf4j_slf4j_api",
        "@maven//:org_springframework_spring_beans",
        "@maven//:org_springframework_spring_context",
    ],
)

java_library(
    name = "gcp_metadata_service_client",
    srcs = ["GcpVMMetadataServiceClient.java"],
    deps = [
        ":configurations",
        "@maven//:com_google_cloud_google_cloud_secretmanager",
        "@maven//:org_apache_httpcomponents_httpclient",
        "@maven//:org_apache_httpcomponents_httpcore",
        "@maven//:org_springframework_spring_beans",
        "@maven//:org_springframework_spring_context",
    ],
)

java_library(
    name = "gcp_parameter_client",
    srcs = ["GcpParameterClient.java"],
    deps = [
        ":configurations",
        ":gcp_metadata_config",
        ":gcp_metadata_service_client",
        "@maven//:com_google_api_gax",
        "@maven//:com_google_api_grpc_proto_google_cloud_secretmanager_v1",
        "@maven//:com_google_cloud_google_cloud_secretmanager",
        "@maven//:org_slf4j_slf4j_api",
        "@maven//:org_springframework_spring_context",
    ],
)

java_library(
    name = "gcp_parameter_config",
    srcs = ["GcpParameterConfig.java"],
    deps = [
        ":gcp_parameter_client",
        ":google_cloud_args",
        ":security_args",
        "@maven//:com_google_guava_guava",
        "@maven//:org_slf4j_slf4j_api",
        "@maven//:org_springframework_spring_beans",
        "@maven//:org_springframework_spring_context",
    ],
)
