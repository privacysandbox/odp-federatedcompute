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
    name = "pub_sub_message_receiver",
    srcs = [
        "PubSubMessageReceiver.java",
    ],
    deps = [
        ":collector_config",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/collector/core:collector_core",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/aggregator/core/message:aggregator_notification",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/messaging/gcp:pub_sub_subscriber_config",
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
    name = "collector_config",
    srcs = ["CollectorConfig.java"],
    deps = [
        "@maven//:org_springframework_spring_beans",
        "@maven//:org_springframework_spring_context",
    ],
)
