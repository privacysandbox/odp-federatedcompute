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

java_library(
    name = "task_entity_converter",
    srcs = ["TaskEntityConverter.java"],
    deps = [
        ":task_entity_status_converter",
        ":timestamp_instant_converter",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:task_entity",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:proto_parser",
        "//shuffler/proto:task_java_proto",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_protobuf_protobuf_java",
        "@maven//:com_google_protobuf_protobuf_java_util",
    ],
)

java_library(
    name = "task_entity_status_converter",
    srcs = ["TaskEntityStatusConverter.java"],
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:task_entity",
        "//shuffler/proto:task_java_proto",
        "@maven//:com_google_guava_guava",
    ],
)

java_library(
    name = "timestamp_instant_converter",
    srcs = ["TimestampInstantConverter.java"],
    deps = [
        "@com_google_protobuf//java/core",
        "@maven//:com_google_guava_guava",
    ],
)
