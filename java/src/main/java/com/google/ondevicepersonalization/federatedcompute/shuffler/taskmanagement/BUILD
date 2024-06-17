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

load("@rules_spring//springboot:springboot.bzl", "springboot")

package(default_visibility = ["//visibility:public"])

# Build the app as a Spring Boot executable jar
springboot(
    name = "task_management_application",
    boot_app_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.taskmanagement.TaskManagementApplication",
    java_library = ":task_management",
)

java_library(
    name = "task_management",
    srcs = ["TaskManagementApplication.java"],
    resources = ["//java/src/main/resources:taskmanagement_properties", "//java/src/main/resources:logback_spring"],
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:constants",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:guid_unique_id_generator",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/config/gcp:gcp_parameter_config",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:single_partition_partitioner",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao/gcp:dao_configurations",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao/gcp:gcs_blob_manager",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao/gcp:task_spanner_dao",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/logging:logging_filter",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/logging:logging_interceptor",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/logging:logging_interceptor_config",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/taskmanagement/controllers:task_management_controller",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/taskmanagement/core:task_management_core_impl",
        "@maven//:com_google_cloud_google_cloud_spanner",
        "@maven//:com_google_cloud_google_cloud_storage",
        "@maven//:com_google_cloud_google_cloud_logging_logback",
        "@maven//:io_micrometer_micrometer_registry_prometheus",
        "@maven//:org_springframework_boot_spring_boot",
        "@maven//:org_springframework_boot_spring_boot_autoconfigure",
        "@maven//:org_springframework_boot_spring_boot_loader",
        "@maven//:org_springframework_boot_spring_boot_starter_actuator",
        "@maven//:org_springframework_boot_spring_boot_starter_web",
        "@maven//:org_springframework_spring_context",
        "@maven//:org_springframework_spring_web",
    ],
)
