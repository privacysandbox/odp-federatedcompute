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
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/collector:collector_application",
    ],
)

oci_image(
    name = "collector_image",
    base = "@java_base",
    entrypoint = [
        "java",
        "-Dspring.profiles.active=gcp",
        "-XX:InitialRAMPercentage=70.0",
        "-XX:MaxRAMPercentage=70.0",
        "-jar",
        "/collector_application.jar",
    ],
    tars = [
        ":binary_tar",
    ],
)

oci_push(
    name = "collector_image_publish",
    image = ":collector_image",
    remote_tags = ["latest"],
    repository = "europe-west4-docker.pkg.dev/jetbrains-fed-compute/odp/collector_image",
)

# Run a local container with:
# $ bazel run :tarball
# $ docker run collector_image:latest
oci_tarball(
    name = "tarball",
    image = ":collector_image",
    repo_tags = ["collector_image:latest"],
)
