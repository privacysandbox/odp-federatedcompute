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
    name = "task_scheduler_application",
    boot_app_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.taskscheduler.TaskSchedulerApplication",
    # use the spring boot 3 launcher
    boot_launcher_class = 'org.springframework.boot.loader.launch.JarLauncher',
    java_library = ":task_scheduler",
)

java_library(
    name = "task_scheduler",
    srcs = ["TaskSchedulerApplication.java"],
    resources = ["//java/src/main/resources:taskscheduler_properties", "//java/src/main/resources:logback_spring"],
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:constants",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:java_util_random_generator",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/config/gcp:gcp_parameter_config",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:partitioner",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:single_partition_partitioner",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao/gcp:dao_configurations",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao/gcp:gcs_blob_dao",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao/gcp:gcs_blob_manager",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao/gcp:model_metrics_spanner_dao",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao/gcp:task_spanner_dao",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/lock/jdbc/gcp:spanner_lock_configuration",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/taskscheduler/controllers:task_scheduler_controller",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/taskscheduler/core:task_scheduler_core_impl",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/taskscheduler/scheduler:scheduled_task",
        "@maven//:com_google_cloud_google_cloud_spanner",
        "@maven//:com_google_cloud_google_cloud_storage",
        "@maven//:com_google_cloud_google_cloud_logging_logback",
        "@maven//:org_springframework_boot_spring_boot",
        "@maven//:org_springframework_boot_spring_boot_autoconfigure",
        "@maven//:org_springframework_boot_spring_boot_loader",
        "@maven//:org_springframework_boot_spring_boot_starter",  # for logging
        "@maven//:org_springframework_spring_context",
    ],
)
