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
    name = "aggregator_application",
    boot_app_class = "com.google.ondevicepersonalization.federatedcompute.shuffler.aggregator.AggregatorApplication",
    # use the spring boot 3 launcher
    boot_launcher_class = 'org.springframework.boot.loader.launch.JarLauncher',
    java_library = ":aggregator",
)

java_library(
    name = "aggregator",
    srcs = ["AggregatorApplication.java"],
    resources = ["//java/src/main/resources:aggregator_properties", "//java/src/main/resources:logback_spring"],
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/aggregator/controllers:aggregator_controller",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/aggregator/core:aggregator_core_impl",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/aggregator/core/gcp:aggregator_config",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/aggregator/core/gcp:pub_sub_message_receiver",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/aggregator/scheduler:scheduled_task",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:constants",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/config/gcp:gcp_parameter_config",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/crypto/gcp:multiparty_decryption_config",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/crypto/gcp:public_key_decryption_config",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao/gcp:gcs_blob_dao",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/messaging/gcp:pub_sub_subscriber_config",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/messaging/gcp:gcp_http_message_sender",
        "@maven//:io_micrometer_micrometer_registry_prometheus",
        "@maven//:com_google_cloud_google_cloud_logging_logback",
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
