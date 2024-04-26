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

load("@rules_spring//springboot:springboot.bzl", "springboot")

package(default_visibility = ["//visibility:public"])

java_library(
    name = "collector_core",
    srcs = [
        "CollectorCore.java",
    ],
    deps = [
    ],
)

java_library(
    name = "collector_core_impl",
    srcs = [
        "CollectorCoreImpl.java",
    ],
    deps = [
        "collector_core_impl_helper",
        ":collector_core",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/aggregator/core/message:aggregator_message",
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
        "@maven//:com_google_cloud_google_cloud_spanner",
        "@maven//:com_google_guava_guava",
        "@maven//:org_slf4j_slf4j_api",
        "@maven//:org_springframework_integration_spring_integration_core",
        "@maven//:org_springframework_integration_spring_integration_jdbc",
        "@maven//:org_springframework_spring_context",
    ],
)

java_library(
    name = "collector_core_impl_helper",
    srcs = [
        "CollectorCoreImplHelper.java",
    ],
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/aggregator/core/message:aggregator_message",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:constants",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:blob_description",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:blob_manager",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:iteration_entity",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/modelupdater/core/message:model_updater_message",
        "@maven//:org_springframework_spring_beans",
        "@maven//:org_springframework_spring_context",
    ],
)
