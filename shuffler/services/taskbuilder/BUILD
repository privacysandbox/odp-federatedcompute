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
load("@aspect_bazel_lib//lib:tar.bzl", "mtree_spec", "tar")

# Produce the manifest for a tar file of our py_binary, but don't tar it up yet, so we can split
# into fine-grained layers for better docker performance.
mtree_spec(
    name = "task_builder_tar_manifest",
    srcs = ["//python/taskbuilder:task_builder"],
)

# match *only* external repositories that have the string "python"
PY_INTERPRETER_REGEX = "\\.runfiles/.*python.*-.*/"

genrule(
    name = "interpreter_tar_manifest",
    srcs = [":task_builder_tar_manifest"],
    outs = ["interpreter_tar_manifest.spec"],
    cmd = "grep '{}' $< >$@".format(PY_INTERPRETER_REGEX),
)

# One layer with only the python interpreter
tar(
    name = "interpreter_layer",
    srcs = ["//python/taskbuilder:task_builder"],
    mtree = ":interpreter_tar_manifest",
)

# match *only* external pip like repositories that contain the string "site-packages"
SITE_PACKAGES_REGEX = "\\.runfiles/.*/site-packages/.*"

genrule(
    name = "packages_tar_manifest",
    srcs = [":task_builder_tar_manifest"],
    outs = ["packages_tar_manifest.spec"],
    cmd = "grep '{}' $< >$@".format(SITE_PACKAGES_REGEX),
)

# One layer with the third-party pip packages
tar(
    name = "packages_layer",
    srcs = ["//python/taskbuilder:task_builder"],
    mtree = ":packages_tar_manifest",
)

# Any lines that didn't match one of the two grep above...
genrule(
    name = "app_tar_manifest",
    srcs = [":task_builder_tar_manifest"],
    outs = ["app_tar_manifest.spec"],
    cmd = "grep -v '{}' $< | grep -v '{}' >$@".format(SITE_PACKAGES_REGEX, PY_INTERPRETER_REGEX),
)

# ... go into the third layer which is the application. We assume it changes the most frequently.
tar(
    name = "app_layer",
    srcs = ["//python/taskbuilder:task_builder"],
    mtree = ":app_tar_manifest",
)

# Finally we can construct the image with a base python and our layers on top
oci_image(
    name = "task_builder_image",
    base = "@python_base",
    entrypoint = ["/python/taskbuilder/task_builder"],
    tars = [
        "@bookworm//:bookworm",
        ":interpreter_layer",
        ":packages_layer",
        ":app_layer",
    ],
)

oci_push(
    name = "task_builder_image_publish",
    image = ":task_builder_image",
    remote_tags = ["latest"],
    repository = "<registry>/<ORG>/task_builder_image",
)

# $ bazel run //shuffler/services/taskbuilder:tarball
# $ docker run --rm task_builder_image:latest
oci_tarball(
    name = "tarball",
    image = ":task_builder_image",
    repo_tags = ["task_builder_image:latest"],
)
