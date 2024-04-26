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
    name = "task_management_controller_test",
    size = "small",
    srcs = ["TaskManagementControllerTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.taskmanagement.controllers.TaskManagementControllerTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/taskmanagement/controllers:task_management_controller",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/taskmanagement/core:task_management_core",
        "//shuffler/proto:task_java_proto",
        "@com_google_protobuf//java/core",
        "@maven//:com_google_protobuf_protobuf_java_util",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
        "@maven//:org_mockito_mockito_core",
        "@maven//:org_springframework_spring_web",
    ],
)
