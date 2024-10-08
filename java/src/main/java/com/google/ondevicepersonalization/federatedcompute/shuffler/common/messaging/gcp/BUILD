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
    name = "pub_sub_publisher_config",
    srcs = [
        "PubSubPublisherConfig.java",
    ],
    deps = [
        "@maven//:com_google_api_gax",
        "@maven//:com_google_cloud_spring_cloud_gcp_pubsub",
        "@maven//:com_google_cloud_spring_cloud_gcp_starter_pubsub",
        "@maven//:com_google_code_gson_gson",
        "@maven//:org_slf4j_slf4j_api",
        "@maven//:org_springframework_integration_spring_integration_core",
        "@maven//:org_springframework_spring_beans",
        "@maven//:org_springframework_spring_context",
        "@maven//:org_springframework_spring_messaging",
    ],
)

java_library(
    name = "pub_sub_subscriber_config",
    srcs = [
        "PubSubSubscriberConfig.java",
    ],
    deps = [
        "@maven//:com_google_api_gax",
        "@maven//:com_google_cloud_spring_cloud_gcp_pubsub",
        "@maven//:com_google_cloud_spring_cloud_gcp_starter_pubsub",
        "@maven//:com_google_code_gson_gson",
        "@maven//:org_slf4j_slf4j_api",
        "@maven//:org_springframework_integration_spring_integration_core",
        "@maven//:org_springframework_spring_beans",
        "@maven//:org_springframework_spring_context",
        "@maven//:org_springframework_spring_messaging",
    ],
)

java_library(
    name = "pub_sub_message_sender",
    srcs = [
        "PubSubMessageSender.java",
    ],
    deps = [
        ":pub_sub_publisher_config",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/messaging:message_sender",
        "@maven//:com_google_cloud_spring_cloud_gcp_pubsub",
        "@maven//:com_google_code_gson_gson",
        "@maven//:org_springframework_spring_beans",
        "@maven//:org_springframework_spring_context",
        "@maven//:org_springframework_spring_messaging",
    ],
)

java_library(
    name = "gcp_http_message_sender",
    srcs = [
        "GcpHttpMessageSender.java",
    ],
    deps = [
        ":gcp_http_message_sender_config",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/messaging:http_message_sender",
        "@maven//:com_google_auth_google_auth_library_oauth2_http",
        "@coordinator-services-and-shared-libraries//:error_utils",
        "@maven//:com_google_code_gson_gson",
        "@maven//:org_apache_httpcomponents_client5_httpclient5",
        "@maven//:org_apache_httpcomponents_core5_httpcore5",
        "@maven//:org_slf4j_slf4j_api",
        "@maven//:org_springframework_spring_beans",
        "@maven//:org_springframework_spring_context",
        "@maven//:org_springframework_spring_messaging",
    ],
)

java_library(
    name = "gcp_http_message_sender_config",
    srcs = [
        "GcpHttpMessageSenderConfig.java",
    ],
    deps = [
        "@maven//:com_google_auth_google_auth_library_oauth2_http",
        "@maven//:com_google_guava_guava",
        "@maven//:org_apache_httpcomponents_client5_httpclient5",
        "@maven//:org_apache_httpcomponents_core5_httpcore5",
        "@maven//:org_springframework_spring_beans",
        "@maven//:org_springframework_spring_context",
        "@maven//:org_springframework_spring_messaging",
    ],
)
