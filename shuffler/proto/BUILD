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

load("@rules_proto//proto:defs.bzl", "proto_library")
load("@rules_python//python:proto.bzl", "py_proto_library")

proto_library(
    name = "task_proto",
    srcs = ["task.proto"],
    deps = [
        "@com_google_protobuf//:timestamp_proto",
    ],
)

java_proto_library(
    name = "task_java_proto",
    deps = [":task_proto"],
)

py_proto_library(
    name = "task_py_pb2",
    deps = [":task_proto"],
)

proto_library(
    name = "task_assignments_proto",
    srcs = ["task_assignments.proto"],
    deps = [
        ":task_proto",
        "@federatedcompute//fcp/protos/federatedcompute:federated_compute_proto",
    ],
)

java_proto_library(
    name = "task_assignments_java_proto",
    deps = [":task_assignments_proto"],
)

proto_library(
    name = "task_builder_proto",
    srcs = ["task_builder.proto"],
    deps = [
        ":task_proto",
        "@federatedcompute//fcp/protos/federatedcompute:federated_compute_proto",
    ],
)

py_proto_library(
    name = "task_builder_py_pb2",
    deps = [":task_builder_proto"],
)
