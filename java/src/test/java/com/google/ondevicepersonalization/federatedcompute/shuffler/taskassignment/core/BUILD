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
    name = "task_assignment_core_impl_test",
    size = "small",
    srcs = ["TaskAssignmentCoreImplTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.taskassignment.core.TaskAssignmentCoreImplTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:constants",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:unique_id_generator",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:assignment_dao",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:assignment_entity",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:assignment_id",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:blob_description",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:blob_manager",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:iteration_entity",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:task_dao",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:task_entity",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/taskassignment/core:task_assignment_core_helper",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/taskassignment/core:task_assignment_core_impl",
        "//shuffler/proto:task_assignments_java_proto",
        "//shuffler/proto:task_java_proto",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
        "@maven//:org_mockito_mockito_core",
    ],
)

java_test(
    name = "task_assignment_core_helper_test",
    size = "small",
    srcs = ["TaskAssignmentCoreHelperTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.taskassignment.core.TaskAssignmentCoreHelperTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:constants",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:random_generator",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:assignment_entity",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:assignment_id",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:blob_description",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:blob_manager",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:iteration_entity",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:iteration_id",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:task_entity",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/taskassignment/core:task_assignment_core_helper",
        "//shuffler/proto:task_assignments_java_proto",
        "//shuffler/proto:task_java_proto",
        "@federatedcompute//fcp/protos/federatedcompute:federated_compute_java_proto",
        "@maven//:com_google_protobuf_protobuf_java_util",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
        "@maven//:org_mockito_mockito_core",
    ],
)
