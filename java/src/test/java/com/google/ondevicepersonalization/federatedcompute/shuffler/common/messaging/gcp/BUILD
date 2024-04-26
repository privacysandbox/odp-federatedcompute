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
    name = "pub_sub_message_sender_test",
    size = "small",
    srcs = ["PubSubMessageSenderTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.common.messaging.gcp.PubSubMessageSenderTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/messaging/gcp:pub_sub_message_sender",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/messaging/gcp:pub_sub_publisher_config",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/modelupdater/core/message:model_updater_message",
        "@maven//:com_google_cloud_spring_cloud_gcp_pubsub",
        "@maven//:com_google_code_gson_gson",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
        "@maven//:org_mockito_mockito_core",
        "@maven//:org_springframework_spring_messaging",
    ],
)
