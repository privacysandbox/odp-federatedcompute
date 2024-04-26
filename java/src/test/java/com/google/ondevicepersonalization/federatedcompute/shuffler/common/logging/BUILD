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
    name = "logging_interceptors_test",
    size = "small",
    srcs = ["LoggingInterceptorsTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.common.logging.LoggingInterceptorsTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/logging:logging_interceptors",
        "//shuffler/proto:task_assignments_java_proto",
        "@federatedcompute//fcp/protos/federatedcompute:federated_compute_java_proto",
        "@maven//:com_google_protobuf_protobuf_java_util",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
    ],
)

java_test(
    name = "logging_filter_test",
    size = "small",
    srcs = ["LoggingFilterTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.common.logging.LoggingFilterTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:unique_id_generator",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/logging:logging_filter",
        "@maven//:com_google_truth_truth",
        "@maven//:jakarta_servlet_jakarta_servlet_api",
        "@maven//:junit_junit",
        "@maven//:org_mockito_mockito_core",
        "@maven//:org_slf4j_slf4j_api",
        "@maven//:org_springframework_spring_web",
    ],
)

java_test(
    name = "logging_interceptor_test",
    size = "small",
    srcs = ["LoggingInterceptorTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.common.logging.LoggingInterceptorTest",
    deps = [
        ":test_java_proto",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/logging:logging_interceptor",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/logging:response_proto",
        "@maven//:ch_qos_logback_logback_classic",
        "@maven//:ch_qos_logback_logback_core",
        "@maven//:com_google_protobuf_protobuf_java",
        "@maven//:com_google_protobuf_protobuf_java_util",
        "@maven//:com_google_testparameterinjector_test_parameter_injector",
        "@maven//:com_google_truth_truth",
        "@maven//:jakarta_servlet_jakarta_servlet_api",
        "@maven//:junit_junit",
        "@maven//:org_mockito_mockito_core",
        "@maven//:org_slf4j_slf4j_api",
        "@maven//:org_springframework_spring_web",
    ],
)

proto_library(
    name = "test_proto",
    srcs = ["test.proto"],
)

java_proto_library(
    name = "test_java_proto",
    deps = [":test_proto"],
)
