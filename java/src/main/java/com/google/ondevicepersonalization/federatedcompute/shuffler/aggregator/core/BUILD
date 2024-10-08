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
    name = "aggregator_core",
    srcs = [
        "AggregatorCore.java",
    ],
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/aggregator/core/message:aggregator_message",
    ],
)

java_library(
    name = "aggregator_core_impl",
    srcs = [
        "AggregatorCoreImpl.java",
    ],
    deps = [
        ":aggregator_core",
        "@federatedcompute//fcp/java_src/main/java/com/google/fcp/tensorflow:app_files",
        "@federatedcompute//fcp/java_src/main/java/com/google/fcp/tensorflow:tensorflow_exception",
        "@maven//:com_google_crypto_tink_tink",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/aggregator/core/message:aggregator_message",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/aggregator/core/message:aggregator_notification",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:compression_utils",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:exceptions",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:non_retryable_exception",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:constants",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/crypto:payload",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:blob_dao",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:blob_description",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/tensorflow:tensorflow_plan_session_factory",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/crypto:public_key_encryption_service",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/messaging:http_message_sender",
        "@coordinator-services-and-shared-libraries//:crypto_client",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_protobuf_protobuf_java",
        "@maven//:org_slf4j_slf4j_api",
        "@maven//:org_springframework_spring_context",
        "@maven//:com_google_code_gson_gson",
        # Import fcp last since it tries to pull in an older version of protobuf.
        "@federatedcompute//fcp/protos:plan_java_proto",
        "@federatedcompute//fcp/java_src/main/java/com/google/fcp/aggregation",
        "@federatedcompute//fcp/java_src/main/java/com/google/fcp/plan:phase_v2",
        "@federatedcompute//fcp/java_src/main/java/com/google/fcp/plan",
    ],
)
