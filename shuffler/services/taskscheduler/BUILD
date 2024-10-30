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

_DEBS = [
    "libcc1-16_amd64",
    "libccabi1-16_amd64",
    "libc6_amd64",
    "libunwind-16_amd64",
    "libgcc-s1_amd64",
    "gcc-13-base_amd64",
]

[
    genrule(
        name = "decompress_" + deb,
        srcs = [
            "@{}//:layer".format(deb),
        ],
        outs = ["{}.tar".format(deb)],
        cmd = "xz --decompress --stdout $< >$@",
    )
    for deb in _DEBS
]

pkg_tar(
    name = "binary_tar",
    # Bring the java_binary
    srcs = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/taskscheduler:task_scheduler_application",
        "@federatedcompute//fcp/java_src/main/java/com/google/fcp/tensorflow/jni:libtensorflow-jni.so",
    ],
)

oci_image(
    name = "task_scheduler_image",
    base = "@java_base",
    entrypoint = [
        "java",
        "-Djava.library.path=/",
        "-Dspring.profiles.active=gcp",
        "-XX:InitialRAMPercentage=70.0",
        "-XX:MaxRAMPercentage=70.0",
        "-jar",
        "/task_scheduler_application.jar",
    ],
    tars = [
        "libcc1-16_amd64.tar",
        "libccabi1-16_amd64.tar",
        "libc6_amd64.tar",
        "libunwind-16_amd64.tar",
        "libgcc-s1_amd64.tar",
        "gcc-13-base_amd64.tar",
        ":binary_tar",
    ],
)

oci_push(
    name = "task_scheduler_image_publish",
    image = ":task_scheduler_image",
    remote_tags = ["latest"],
    repository = "<registry>/<ORG>/task_scheduler_image",
)

# Run a local container with:
# $ bazel run :tarball
# $ docker run task_scheduler_image:latest
oci_tarball(
    name = "tarball",
    image = ":task_scheduler_image",
    repo_tags = ["task_scheduler_image:latest"],
)
