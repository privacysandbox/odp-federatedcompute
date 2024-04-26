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

package(default_visibility = ["//visibility:public"])

java_plugin(
    name = "lombok_java_plugin",
    generates_api = True,
    processor_class = "lombok.launch.AnnotationProcessorHider$AnnotationProcessor",
    visibility = ["//visibility:public"],
    deps = ["@maven//:org_projectlombok_lombok"],
)

java_library(
    name = "lombok_java",
    exported_plugins = [
        ":lombok_java_plugin",
    ],
    neverlink = 1,
    exports = [
        "@maven//:org_projectlombok_lombok",
    ],
)

java_library(
    name = "constants",
    srcs = [
        "Constants.java",
    ],
    deps = [],
)

java_library(
    name = "unique_id_generator",
    srcs = [
        "UniqueIdGenerator.java",
    ],
    deps = [
    ],
)

java_library(
    name = "guid_unique_id_generator",
    srcs = [
        "GuidUniqueIdGenerator.java",
    ],
    deps = [
        ":unique_id_generator",
        "@maven//:org_springframework_spring_context",
    ],
)

java_library(
    name = "random_generator",
    srcs = [
        "RandomGenerator.java",
    ],
    deps = [
    ],
)

java_library(
    name = "java_util_random_generator",
    srcs = [
        "JavaUtilRandomGenerator.java",
    ],
    deps = [
        ":random_generator",
        "@maven//:org_springframework_spring_context",
    ],
)

java_library(
    name = "exceptions",
    srcs = [
        "Exceptions.java",
    ],
    deps = [
        "@federatedcompute//fcp/java_src/main/java/com/google/fcp/tensorflow:tensorflow_exception",
    ],
)

java_library(
    name = "compression_utils",
    srcs = [
        "CompressionUtils.java",
    ],
)

java_library(
    name = "proto_parser",
    srcs = [
        "ProtoParser.java",
    ],
    deps = [
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_protobuf_protobuf_java",
        "@maven//:com_google_protobuf_protobuf_java_util",
    ],
)
