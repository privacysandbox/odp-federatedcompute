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

load("@rules_oci//oci:defs.bzl", "oci_image", "oci_push", "oci_tarball")
load("@rules_pkg//:pkg.bzl", "pkg_tar")

pkg_tar(
    name = "binary_tar",
    # Bring the java_binary
    srcs = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/aggregator:aggregator_application",
        "@federatedcompute//fcp/java_src/main/java/com/google/fcp/aggregation/jni:libaggregation-jni.so",
        "@federatedcompute//fcp/java_src/main/java/com/google/fcp/tensorflow/jni:libtensorflow-jni.so",
    ],
)

oci_image(
    name = "aggregator_image",
    base = "@java_base",
    entrypoint = [
        "java",
        "-Djava.library.path=/",
        "-Dspring.profiles.active=gcp",
        "-XX:InitialRAMPercentage=70.0",
        "-XX:MaxRAMPercentage=70.0",
        "-jar",
        "/aggregator_application.jar",
    ],
    # Set ENCRYPTION_OPTS to below to disable encryption for local dev. Note this will change the resulting hash.
    # "--public_key_service_base_url 'https://publickeyservice.odp.gcp.privacysandboxservices.com/.well-known/odp/v1/public-keys' --should_encrypt_aggregator_output 'false'"
    env = {"ENCRYPTION_OPTS": "--public_key_service_base_url 'https://publickeyservice.odp.gcp.privacysandboxservices.com/.well-known/odp/v1/public-keys'"},
    exposed_ports = ["8082/tcp"],
    labels = {
        "tee.launch_policy.allow_env_override": "FCP_OPTS",
        "tee.launch_policy.log_redirect": "always",
        "tee.launch_policy.monitoring_memory_allow": "always",
    },
    tars = [
        # This target contains all the installed packages.
        "@bookworm//:bookworm",
        ":binary_tar",
    ],
    user = "root"
)

oci_push(
    name = "aggregator_image_publish",
    image = ":aggregator_image",
    remote_tags = ["latest"],
    repository = "<registry>/<ORG>/aggregator_image",
)

# Run a local container with:
# $ bazel run :tarball
# $ docker run aggregator_image:latest
oci_tarball(
    name = "tarball",
    image = ":aggregator_image",
    repo_tags = ["aggregator_image:latest"],
)
