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
    name = "task_assignment_controller_test",
    size = "small",
    srcs = ["TaskAssignmentControllerTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.taskassignment.controllers.TaskAssignmentControllerTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/taskassignment/controllers:task_assignment_controller",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/taskassignment/core:task_assignment_core",
        "//shuffler/proto:task_assignments_java_proto",
        "@com_google_protobuf//java/core",
        "@federatedcompute//fcp/protos/federatedcompute:federated_compute_java_proto",
        "@maven//:com_google_truth_truth",
        "@maven//:io_micrometer_micrometer_core",
        "@maven//:junit_junit",
        "@maven//:org_mockito_mockito_core",
        "@maven//:org_springframework_spring_web",
    ],
)
