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
    name = "task_scheduler_core_impl_test",
    size = "small",
    srcs = ["TaskSchedulerCoreImplTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.taskscheduler.core.TaskSchedulerCoreImplTest",
    deps = [
        "@maven//:com_google_protobuf_protobuf_java",
        "@maven//:org_springframework_integration_spring_integration_core",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:task_entities",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:proto_parser",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:random_generator",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:blob_dao",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:blob_description",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:blob_manager",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:iteration_entity",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:task_dao",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:task_entity",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/tensorflow:tensorflow_plan_session_factory",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/taskscheduler/core:task_scheduler_core_helper",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/taskscheduler/core:task_scheduler_core_impl",
        "//shuffler/proto:task_java_proto",
        "@maven//:com_google_cloud_google_cloud_spanner",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_testparameterinjector_test_parameter_injector",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
        "@maven//:org_mockito_mockito_core",
        # Import fcp last since it tries to pull in an older version of protobuf.
        "@federatedcompute//fcp/java_src/main/java/com/google/fcp/plan",
    ],
)

java_test(
    name = "task_scheduler_core_helper_test",
    size = "small",
    srcs = ["TaskSchedulerCoreHelperTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.taskscheduler.core.TaskSchedulerCoreHelperTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:proto_parser",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:random_generator",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:blob_dao",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:blob_description",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:blob_manager",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:iteration_entity",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:task_dao",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:task_entity",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/tensorflow:tensorflow_plan_session_factory",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/taskscheduler/core:task_scheduler_core_helper",
        "//shuffler/proto:task_java_proto",
        "@maven//:com_google_protobuf_protobuf_java",
        "@maven//:com_google_protobuf_protobuf_java_util",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
        "@maven//:org_mockito_mockito_core",
        # Import fcp last since it tries to pull in an older version of protobuf.
        "@federatedcompute//fcp/java_src/main/java/com/google/fcp/plan",
    ],
)
