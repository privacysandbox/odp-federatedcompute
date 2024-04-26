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

load("@rules_python//python:defs.bzl", "py_binary", "py_library", "py_test")

package(default_visibility = ["//visibility:public"])

py_binary(
    name = "task_builder_client",
    srcs = ["task_builder_client.py"],
    main = "task_builder_client.py",
    deps = [
        ":common",
        ":config_validator",
        ":task_utils",
        ":dp_utils",
        ":dataset_utils",
        ":http_utils",
        ":artifact_utils",
        ":learning_process_utils",
        "//shuffler/proto:task_builder_py_pb2",
        "@pypi_absl_py//:pkg",
        "@pypi_google_cloud_storage//:pkg",
        "@pypi_tensorflow_federated//:pkg",
    ]
)

py_library(
    name = "task_utils",
    srcs = ["task_utils.py"],
    srcs_version = "PY3",
    deps = [
        ":common",
        ":http_utils",
        ":test_utils",
        "//shuffler/proto:task_py_pb2",
        "//shuffler/proto:task_builder_py_pb2",
        "@pypi_google_cloud_storage//:pkg",
    ]
)

py_library(
    name = "common",
    srcs = ["common.py"],
    srcs_version = "PY3",
    deps = [
        "@pypi_absl_py//:pkg",
        "@pypi_tensorflow//:pkg",
    ]
)

py_library(
    name = "tensorflow_checkpoints",
    srcs = ["tensorflow_checkpoints.py"],
    srcs_version = "PY3",
    deps = [
        "@federatedcompute//fcp/artifact_building:checkpoint_utils",
        "@federatedcompute//fcp/artifact_building:graph_helpers",
        "@federatedcompute//fcp/artifact_building:plan_utils",
        "@federatedcompute//fcp/protos:plan_py_pb2",
        "@pypi_tensorflow//:pkg",
        "@pypi_tensorflow_federated//:pkg",
    ],
)

py_library(
    name = "config_validator",
    srcs = ["config_validator.py"],
    srcs_version = "PY3",
    deps = [
        ":common",
        ":dp_utils",
        "//shuffler/proto:task_builder_py_pb2",
    ]
)

py_library(
    name = "artifact_utils",
    srcs = ["artifact_utils.py"],
    srcs_version = "PY3",
    deps = [
        ":common",
        ":tensorflow_checkpoints",
        "//shuffler/proto:task_builder_py_pb2",
        "@federatedcompute//fcp/artifact_building:data_spec",
        "@federatedcompute//fcp/artifact_building:federated_compute_plan_builder",
        "@federatedcompute//fcp/artifact_building:plan_utils",
        "@federatedcompute//fcp/protos:plan_py_pb2",
        "@pypi_google_cloud_storage//:pkg",
        "@pypi_tensorflow//:pkg",
        "@pypi_tensorflow_federated//:pkg",
    ],
)

py_library(
    name = "http_utils",
    srcs = ["http_utils.py"],
    srcs_version = "PY3",
    deps = [
        ":common",
        "//shuffler/proto:task_py_pb2",
    ],
)

py_library(
    name = "metrics_utils",
    srcs = ["metrics_utils.py"],
    srcs_version = "PY3",
    deps = [
        ":common",
        "@pypi_tensorflow//:pkg",
        "@pypi_tensorflow_federated//:pkg",
    ],
)

py_library(
    name = "dataset_utils",
    srcs = ["dataset_utils.py"],
    srcs_version = "PY3",
    deps = [
        ":common",
        "@pypi_tensorflow//:pkg",
    ],
)

py_library(
    name = "dp_utils",
    srcs = ["dp_utils.py"],
    srcs_version = "PY3",
    deps = [
        ":common",
        "@differential_privacy//dp_accounting:accounting",
    ]
)

py_library(
    name = "learning_process_utils",
    srcs = ["learning_process_utils.py"],
    srcs_version = "PY3",
    deps = [
        ":common",
        ":metrics_utils",
        "//shuffler/proto:task_builder_py_pb2",
        "@pypi_tensorflow//:pkg",
        "@pypi_tensorflow_federated//:pkg",
    ]
)

py_library(
    name = "example_model",
    srcs = ["example_model.py"],
    srcs_version = "PY3",
    deps = [
        "@pypi_tensorflow//:pkg",
    ],
)

py_library(
    name = "test_utils",
    srcs = ["test_utils.py"],
    srcs_version = "PY3",
    deps = [
        ":example_model",
        "@pypi_tensorflow//:pkg",
        "@pypi_tensorflow_federated//:pkg",
    ],
)

py_test(
    name = "artifact_utils_test",
    srcs = ["artifact_utils_test.py"],
    deps = [
        ":artifact_utils",
        ":common",
        ":dataset_utils",
        ":learning_process_utils",
        ":tensorflow_checkpoints",
        ":test_utils",
        "//shuffler/proto:task_py_pb2",
        "//shuffler/proto:task_builder_py_pb2",
        "@federatedcompute//fcp/artifact_building:federated_compute_plan_builder",
        "@pypi_absl_py//:pkg",
    ],
)

py_test(
    name = "http_utils_test",
    srcs = ["http_utils_test.py"],
    deps = [
        ":common",
        ":http_utils",
        "//shuffler/proto:task_py_pb2",
        "@pypi_absl_py//:pkg",
        "@pypi_responses//:pkg",
    ],
)

py_test(
    name = "config_validator_test",
    srcs = ["config_validator_test.py"],
    deps = [
        ":common",
        ":config_validator",
        "@pypi_absl_py//:pkg",
    ]
)

py_test(
    name = "task_utils_test",
    srcs = ["task_utils_test.py"],
    deps = [
        ":common",
        ":task_utils",
        ":test_utils",
        "//shuffler/proto:task_py_pb2",
        "//shuffler/proto:task_builder_py_pb2",
        "@pypi_google_cloud_storage//:pkg",
    ]
)

py_test(
    name = "tensorflow_checkpoints_test",
    srcs = ["tensorflow_checkpoints_test.py"],
    deps = [
        ":tensorflow_checkpoints",
        ":test_utils",
        "@pypi_tensorflow//:pkg",
        "@pypi_tensorflow_federated//:pkg",
    ],
)

py_test(
    name = "dp_utils_test",
    srcs = ["dp_utils_test.py"],
    deps = [
        ":dp_utils",
        "@pypi_absl_py//:pkg",
    ]
)

py_test(
    name = "learning_process_utils_test",
    srcs = ["learning_process_utils_test.py"],
    deps = [
        ":common",
        ":learning_process_utils",
        ":test_utils",
        "//shuffler/proto:task_builder_py_pb2",
        "@pypi_absl_py//:pkg",
    ]
)

py_test(
    name = "dataset_utils_test",
    srcs = ["dataset_utils_test.py"],
    deps = [
        ":dataset_utils",
        "@pypi_absl_py//:pkg",
        "@pypi_tensorflow//:pkg",
        "@pypi_tensorflow_federated//:pkg",
    ],
)
