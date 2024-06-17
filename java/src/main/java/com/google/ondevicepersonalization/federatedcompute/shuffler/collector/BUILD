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
    name = "collector_application",
    boot_app_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.collector.CollectorApplication",
    java_library = ":collector",
)

java_library(
    name = "collector",
    srcs = ["CollectorApplication.java"],
    resources = ["//java/src/main/resources:collector_properties", "//java/src/main/resources:logback_spring"],
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/collector/controllers:collector_controller",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/collector/core:collector_core_impl",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/collector/scheduler:scheduled_task",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:constants",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/config/gcp:gcp_parameter_config",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:single_partition_partitioner",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao/gcp:aggregation_batch_spanner_dao",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao/gcp:assignment_spanner_dao",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao/gcp:dao_configurations",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao/gcp:gcs_blob_dao",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao/gcp:gcs_blob_manager",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao/gcp:task_spanner_dao",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/lock/jdbc/gcp:spanner_lock_configuration",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/messaging/gcp:pub_sub_message_sender",
        "@maven//:com_google_cloud_google_cloud_logging_logback",
        "@maven//:com_google_cloud_google_cloud_spanner",
        "@maven//:com_google_cloud_google_cloud_storage",
        "@maven//:com_google_guava_guava",
        "@maven//:io_micrometer_micrometer_registry_prometheus",
        "@maven//:org_springframework_boot_spring_boot",
        "@maven//:org_springframework_boot_spring_boot_autoconfigure",
        "@maven//:org_springframework_boot_spring_boot_loader",
        "@maven//:org_springframework_boot_spring_boot_starter",  # for logging
        "@maven//:org_springframework_boot_spring_boot_starter_actuator",
        "@maven//:org_springframework_boot_spring_boot_starter_web",  # for actuator
        "@maven//:org_springframework_integration_spring_integration_core",
        "@maven//:org_springframework_spring_context",
    ],
)
