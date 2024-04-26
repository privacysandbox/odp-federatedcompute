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
    name = "task_spanner_dao_test",
    size = "small",
    srcs = ["TaskSpannerDaoTest.java"],
    data = ["//shuffler/spanner:sdl_bundle"],
    tags = [
        "manual",
        "requires-docker",
    ],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.gcp.TaskSpannerDaoTest",
    deps = [
        ":spanner_test_harness",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:assignment_entity",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:iteration_entity",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:iteration_id",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:task_dao",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:task_entity",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:task_id",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao/gcp:task_spanner_dao",
        "@maven//:com_google_cloud_google_cloud_core",
        "@maven//:com_google_cloud_google_cloud_spanner",
        "@maven//:com_google_testparameterinjector_test_parameter_injector",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
    ],
)

java_test(
    name = "assignment_spanner_dao_test",
    size = "small",
    srcs = ["AssignmentSpannerDaoTest.java"],
    data = ["//shuffler/spanner:sdl_bundle"],
    tags = [
        "manual",
        "requires-docker",
    ],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.gcp.AssignmentSpannerDaoTest",
    deps = [
        ":spanner_test_harness",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:aggregation_batch_entity",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:assignment_dao",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:assignment_entity",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:assignment_id",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:iteration_entity",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:iteration_id",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao/gcp:assignment_spanner_dao",
        "@maven//:ch_qos_logback_logback_classic",
        "@maven//:ch_qos_logback_logback_core",
        "@maven//:com_google_cloud_google_cloud_core",
        "@maven//:com_google_cloud_google_cloud_spanner",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
        "@maven//:org_mockito_mockito_core",
        "@maven//:org_slf4j_slf4j_api",
    ],
)

java_test(
    name = "aggregation_batch_spanner_dao_test",
    size = "small",
    srcs = ["AggregationBatchSpannerDaoTest.java"],
    data = ["//shuffler/spanner:sdl_bundle"],
    tags = [
        "manual",
        "requires-docker",
    ],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.gcp.AggregationBatchSpannerDaoTest",
    deps = [
        ":spanner_test_harness",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:aggregation_batch_dao",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:aggregation_batch_entity",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:aggregation_batch_id",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:iteration_entity",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:iteration_id",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao/gcp:aggregation_batch_spanner_dao",
        "@maven//:ch_qos_logback_logback_classic",
        "@maven//:ch_qos_logback_logback_core",
        "@maven//:com_google_cloud_google_cloud_core",
        "@maven//:com_google_cloud_google_cloud_spanner",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
        "@maven//:org_mockito_mockito_core",
        "@maven//:org_slf4j_slf4j_api",
    ],
)

java_test(
    name = "gcs_blob_manager_test",
    size = "small",
    srcs = ["GCSBlobManagerTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.gcp.GCSBlobManagerTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:assignment_entity",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:assignment_id",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:blob_description",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:blob_manager",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:iteration_entity",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:iteration_id",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:partitioner",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:task_entity",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao/gcp:gcs_blob_manager",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao/gcp:gcs_config",
        "@maven//:com_google_cloud_google_cloud_storage",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
        "@maven//:org_mockito_mockito_core",
        "@maven//:org_springframework_spring_context",
    ],
)

java_test(
    name = "gcs_blob_dao_test",
    size = "small",
    srcs = ["GCSBlobDaoTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.gcp.GCSBlobDaoTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:blob_description",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao/gcp:gcs_blob_dao",
        "@maven//:com_google_api_gax",
        "@maven//:com_google_cloud_google_cloud_storage",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
        "@maven//:org_mockito_mockito_core",
        "@maven//:org_springframework_spring_context",
    ],
)

java_test(
    name = "authorization_token_spanner_dao_test",
    size = "small",
    srcs = ["AuthorizationTokenSpannerDaoTest.java"],
    data = ["//shuffler/spanner:sdl_bundle"],
    tags = [
        "manual",
        "requires-docker",
    ],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.gcp.AuthorizationTokenSpannerDaoTest",
    deps = [
        ":spanner_test_harness",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:authorization_token_dao",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao/gcp:authorization_token_spanner_dao",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao/gcp:timestamp_instant_converter",
        "@maven//:ch_qos_logback_logback_classic",
        "@maven//:ch_qos_logback_logback_core",
        "@maven//:com_google_cloud_google_cloud_core",
        "@maven//:com_google_cloud_google_cloud_spanner",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
        "@maven//:org_mockito_mockito_core",
        "@maven//:org_slf4j_slf4j_api",
    ],
)

java_test(
    name = "timestamp_instant_converter_test",
    size = "small",
    srcs = ["TimestampInstantConverterTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.gcp.TimestampInstantConverterTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao/gcp:timestamp_instant_converter",
        "@com_google_protobuf//java/core",
        "@maven//:com_google_cloud_google_cloud_core",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
    ],
)

java_test(
    name = "gcs_config_test",
    size = "small",
    srcs = ["GCSConfigTest.java"],
    test_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.gcp.GCSConfigTest",
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao/gcp:gcs_config",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
    ],
)

java_library(
    name = "spanner_test_harness",
    srcs = ["SpannerTestHarness.java"],
    deps = [
        "@maven//:com_google_cloud_google_cloud_spanner",
        "@maven//:com_google_guava_guava",
        "@maven//:org_testcontainers_testcontainers",
    ],
)
