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
    name = "task_dao",
    srcs = [
        "TaskDao.java",
    ],
    deps = [
        ":iteration_entity",
        ":iteration_id",
        ":task_entity",
        ":task_id",
    ],
)

java_library(
    name = "task_id",
    srcs = [
        "TaskId.java",
    ],
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:lombok_java",
    ],
)

java_library(
    name = "task_entity",
    srcs = [
        "TaskEntity.java",
    ],
    deps = [
        ":task_id",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:lombok_java",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:proto_parser",
        "//shuffler/proto:task_java_proto",
        "@maven//:com_google_guava_guava",
    ],
)

java_library(
    name = "task_entities",
    srcs = [
        "TaskEntities.java",
    ],
    deps = [
        ":iteration_entity",
        ":task_entity",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:lombok_java",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:proto_parser",
        "//shuffler/proto:task_java_proto",
    ],
)

java_library(
    name = "iteration_id",
    srcs = [
        "IterationId.java",
    ],
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:lombok_java",
        "@maven//:com_google_guava_guava",
    ],
)

java_library(
    name = "iteration_entity",
    srcs = [
        "IterationEntity.java",
    ],
    deps = [
        "iteration_id",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:lombok_java",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:proto_parser",
        "//shuffler/proto:task_java_proto",
    ],
)

java_library(
    name = "assignment_id",
    srcs = [
        "AssignmentId.java",
    ],
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:lombok_java",
    ],
)

java_library(
    name = "aggregation_batch_id",
    srcs = [
        "AggregationBatchId.java",
    ],
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:lombok_java",
    ],
)

java_library(
    name = "assignment_entity",
    srcs = [
        "AssignmentEntity.java",
    ],
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:lombok_java",
    ],
)

java_library(
    name = "aggregation_batch_entity",
    srcs = [
        "AggregationBatchEntity.java",
    ],
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:lombok_java",
    ],
)

java_library(
    name = "aggregation_batch_dao",
    srcs = [
        "AggregationBatchDao.java",
    ],
    deps = [
        ":aggregation_batch_entity",
        ":aggregation_batch_id",
        ":iteration_entity",
    ],
)

java_library(
    name = "assignment_dao",
    srcs = [
        "AssignmentDao.java",
    ],
    deps = [
        ":assignment_entity",
        ":iteration_entity",
        ":iteration_id",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:assignment_id",
    ],
)

java_library(
    name = "authorization_token_dao",
    srcs = [
        "AuthorizationTokenDao.java",
    ],
    deps = [
    ],
)

java_library(
    name = "partitioner",
    srcs = [
        "Partitioner.java",
    ],
    deps = [
    ],
)

java_library(
    name = "single_partition_partitioner",
    srcs = [
        "SinglePartitionPartitioner.java",
    ],
    deps = [
        ":partitioner",
        "@maven//:org_springframework_spring_context",
    ],
)

java_library(
    name = "blob_description",
    srcs = [
        "BlobDescription.java",
    ],
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:lombok_java",
    ],
)

java_library(
    name = "blob_manager",
    srcs = [
        "BlobManager.java",
    ],
    deps = [
        ":assignment_entity",
        ":blob_description",
        ":iteration_entity",
        ":task_entity",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:lombok_java",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:assignment_id",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:iteration_id",
    ],
)

java_library(
    name = "blob_dao",
    srcs = [
        "BlobDao.java",
    ],
    deps = [
        ":blob_description",
    ],
)
