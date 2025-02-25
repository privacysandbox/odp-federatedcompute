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
    name = "single_partition_partitioner_test",
    size = "small",
    srcs = ["SinglePartitionPartitionerTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.SinglePartitionPartitionerTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:single_partition_partitioner",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
    ],
)

java_test(
    name = "task_entity_status_test",
    size = "small",
    srcs = ["TaskEntityStatusTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskEntityStatusTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:task_entity",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_testparameterinjector_test_parameter_injector",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
    ],
)

java_test(
    name = "check_in_result_test",
    size = "small",
    srcs = ["CheckInResultTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.CheckInResultTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:check_in_result",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_testparameterinjector_test_parameter_injector",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
    ],
)

java_test(
    name = "task_id_test",
    size = "small",
    srcs = ["TaskIdTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskIdTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:task_id",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
    ],
)

java_test(
    name = "assignment_entity_test",
    size = "small",
    srcs = ["AssignmentEntityTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AssignmentEntityTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:assignment_entity",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
    ],
)

java_test(
    name = "aggregation_batch_entity_test",
    size = "small",
    srcs = ["AggregationBatchEntityTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AggregationBatchEntityTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:aggregation_batch_entity",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
    ],
)

java_test(
    name = "iteration_entity_test",
    size = "small",
    srcs = ["IterationEntityTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.IterationEntityTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:iteration_entity",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:iteration_id",
        "//shuffler/proto:task_java_proto",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
    ],
)

java_test(
    name = "iteration_id_test",
    size = "small",
    srcs = ["IterationIdTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.IterationIdTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:iteration_id",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
    ],
)

java_test(
    name = "assignment_id_test",
    size = "small",
    srcs = ["AssignmentIdTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AssignmentIdTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:assignment_id",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
    ],
)

java_test(
    name = "aggregation_batch_id_test",
    size = "small",
    srcs = ["AggregationBatchIdTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AggregationBatchIdTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:aggregation_batch_id",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
    ],
)

java_test(
    name = "task_entity_test",
    size = "small",
    srcs = ["TaskEntityTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskEntityTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:proto_parser",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:task_entity",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:task_id",
        "//shuffler/proto:task_java_proto",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
    ],
)

java_test(
    name = "task_entities_test",
    size = "small",
    srcs = ["TaskEntitiesTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskEntitiesTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:proto_parser",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:iteration_entity",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:task_entities",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:task_entity",
        "//shuffler/proto:task_java_proto",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
    ],
)

java_test(
    name = "model_metrics_entity_test",
    size = "small",
    srcs = ["ModelMetricsEntityTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.ModelMetricsEntityTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:model_metrics_entity",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
    ],
)
