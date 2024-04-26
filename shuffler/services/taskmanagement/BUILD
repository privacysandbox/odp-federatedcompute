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
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/taskmanagement:task_management_application",
    ],
)

oci_image(
    name = "task_management_image",
    base = "@java_base",
    entrypoint = [
        "java",
        "-jar",
        "task_management_application.jar",
    ],
    tars = [
        ":binary_tar",
    ],
)

oci_push(
    name = "task_management_image_publish",
    image = ":task_management_image",
    remote_tags = ["latest"],
    repository = "<registry>/<ORG>/task_management_image",
)

# Run a local container with:
# $ bazel run :tarball
# $ docker run task_management_image:latest
oci_tarball(
    name = "tarball",
    image = ":task_management_image",
    repo_tags = ["task_management_image:latest"],
)
