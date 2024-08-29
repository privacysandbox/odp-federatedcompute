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
    name = "collector_core_impl_test",
    size = "small",
    srcs = ["CollectorCoreImplTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.collector.core.CollectorCoreImplTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/aggregator/core/message:aggregator_message",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/aggregator/core/message:aggregator_notification",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/collector/core:collector_core_impl",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/collector/core:collector_core_impl_helper",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:constants",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:aggregation_batch_dao",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:aggregation_batch_entity",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:assignment_dao",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:assignment_entity",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:assignment_id",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:blob_dao",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:blob_description",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:blob_manager",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:iteration_entity",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:iteration_id",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:task_dao",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/messaging:message_sender",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/modelupdater/core/message:model_updater_message",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
        "@maven//:org_mockito_mockito_core",
        "@maven//:org_springframework_integration_spring_integration_core",
    ],
)

java_test(
    name = "collector_core_impl_helper_test",
    size = "small",
    srcs = ["CollectorCoreImplHelperTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.collector.core.CollectorCoreImplHelperTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/aggregator/core/message:aggregator_message",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/collector/core:collector_core_impl_helper",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:proto_parser",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:blob_description",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:blob_manager",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:iteration_entity",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/modelupdater/core/message:model_updater_message",
        "//shuffler/proto:task_java_proto",
        "@maven//:com_google_cloud_spring_cloud_gcp_pubsub",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
        "@maven//:org_mockito_mockito_core",
    ],
)
