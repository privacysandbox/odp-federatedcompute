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
    name = "guid_unique_id_generator_test",
    size = "small",
    srcs = ["GuidUniqueIdGeneratorTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.common.GuidUniqueIdGeneratorTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:constants",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:guid_unique_id_generator",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
    ],
)

java_test(
    name = "java_util_random_generator_test",
    size = "small",
    srcs = ["JavaUtilRandomGeneratorTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.common.JavaUtilRandomGeneratorTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:java_util_random_generator",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
    ],
)

java_test(
    name = "exceptions_test",
    size = "small",
    srcs = ["ExceptionsTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.common.ExceptionsTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:exceptions",
        "@federatedcompute//fcp/java_src/main/java/com/google/fcp/tensorflow:tensorflow_exception",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
    ],
)

java_test(
    name = "compression_utils_test",
    size = "small",
    srcs = ["CompressionUtilsTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.common.CompressionUtilsTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:compression_utils",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
    ],
)

java_test(
    name = "proto_parser_test",
    size = "small",
    srcs = ["ProtoParserTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.common.ProtoParserTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:proto_parser",
        "//shuffler/proto:task_java_proto",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
    ],
)
