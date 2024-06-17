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
    name = "model_updater_core_impl_test",
    size = "small",
    srcs = ["ModelUpdaterCoreImplTest.java"],
    resources = ["//java/src/test/java/resources:plans"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.modelupdater.core.ModelUpdaterCoreImplTest",
    deps = [
        "@coordinator-services-and-shared-libraries//:crypto_client",
        "@federatedcompute//fcp/java_src/main/java/com/google/fcp/aggregation",
        "@federatedcompute//fcp/java_src/main/java/com/google/fcp/plan:phase_v2",
        "@federatedcompute//fcp/java_src/main/java/com/google/fcp/tensorflow:app_files",
        "@maven//:com_google_crypto_tink_tink",
        "@federatedcompute//fcp/java_src/main/java/com/google/fcp/tensorflow:tensorflow_exception",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:blob_dao",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:blob_description",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:iteration_entity",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:iteration_id",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:task_entity",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/tensorflow:tensorflow_plan_session_factory",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/modelupdater/core/message:model_updater_message",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/modelupdater/core:model_updater_core_impl",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_truth_truth",
        "@maven//:com_google_protobuf_protobuf_java",
        "@maven//:junit_junit",
        "@maven//:org_mockito_mockito_core",
        # Import fcp last since it tries to pull in an older version of protobuf.
        "@federatedcompute//fcp/java_src/main/java/com/google/fcp/plan",
    ],
)
