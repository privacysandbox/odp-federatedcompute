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
    name = "task_entity_status_converter_test",
    size = "small",
    srcs = ["TaskEntityStatusConverterTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.common.converters.TaskEntityStatusConverterTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/converters:task_entity_status_converter",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:task_entity",
        "//shuffler/proto:task_java_proto",
        "@maven//:com_google_testparameterinjector_test_parameter_injector",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
    ],
)

java_test(
    name = "timestamp_instant_converter_test",
    size = "small",
    srcs = ["TimestampInstantConverterTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.common.converters.TimestampInstantConverterTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/converters:timestamp_instant_converter",
        "@com_google_protobuf//java/core",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
    ],
)

java_test(
    name = "task_entity_converter_test",
    size = "small",
    srcs = ["TaskEntityConverterTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.common.converters.TaskEntityConverterTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/converters:task_entity_converter",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:task_entity",
        "//shuffler/proto:task_java_proto",
        "@com_google_protobuf//java/core",
        "@maven//:com_google_protobuf_protobuf_java",
        "@maven//:com_google_protobuf_protobuf_java_util",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
        "@maven//:org_mockito_mockito_core",
    ],
)
