# Copyright 2022 Google LLC
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
"""Sets up a repository for using the toolchain-provided TensorFlow."""

def _tf_custom_op_configure_impl(repository_ctx):
    """Defines a repository for using the toolchain-provided TensorFlow package.

    This is a lot like new_local_repository except that (a) the files to
    include are dynamically determined using TensorFlow's `tf.sysconfig` Python
    module, and (b) it provides build rules to compile and link C++ code with
    the necessary options to be compatible with the toolchain-provided TensorFlow
    package.
    """

    tensorflow_dirname = str(repository_ctx.path(Label("@" + repository_ctx.attr.tensorflow_repository.workspace_name + "//:WORKSPACE")).dirname)

    # Name of the sub-directory that will link to TensorFlow C++ headers.
    headers_dir = "headers"

    # Name of the file that will link to libtensorflow_framework.so.
    library_file = "libtensorflow_framework.so"

    # Compile flags
    include_dir = tensorflow_dirname + "/site-packages/tensorflow/include"
    copts = ["-D_GLIBCXX_USE_CXX11_ABI=1", "-DEIGEN_MAX_ALIGN_BYTES=64"]
    cxxopts = ["--std=c++17"]
    repository_ctx.symlink(include_dir, headers_dir)

    # Link flags
    link_dir = tensorflow_dirname + "/site-packages/tensorflow"
    library = "libtensorflow_framework.so.2"
    linkopts = []
    repository_ctx.symlink(link_dir + "/" + library, library_file)

    # Create a BUILD file providing targets for the TensorFlow C++ headers and
    # framework library.
    repository_ctx.template(
        "BUILD",
        Label("@federatedcompute//fcp/tensorflow/system_provided_tf:templates/BUILD.tpl"),
        substitutions = {
            "%{HEADERS_DIR}": headers_dir,
            "%{LIBRARY_FILE}": library_file,
        },
        executable = False,
    )

    # Create a bzl file providing rules for compiling C++ code compatible with
    # the TensorFlow package.
    repository_ctx.template(
        "system_provided_tf.bzl",
        Label("@federatedcompute//fcp/tensorflow/system_provided_tf:templates/system_provided_tf.bzl.tpl"),
        substitutions = {
            "%{COPTS}": str(copts),
            "%{CXXOPTS}": str(cxxopts),
            "%{LINKOPTS}": str(linkopts),
            "%{REPOSITORY_NAME}": repository_ctx.name,
        },
        executable = False,
    )

_tf_custom_op_configure_attrs = {
    "tensorflow_repository": attr.label(mandatory = True),
}

toolchain_provided_tf = repository_rule(
    implementation = _tf_custom_op_configure_impl,
    attrs = _tf_custom_op_configure_attrs,
    configure = True,
    doc = """Creates a repository with targets for the system-provided TensorFlow.

This repository defines (a) //:tf_headers providing the C++ TensorFlow headers,
(b) //:libtensorflow_framework providing the TensorFlow framework shared
library, and (c) //:system_provided_tf.bzl for building custom op libraries
that are compatible with the toolchain-provided TensorFlow package.
""",
    local = True,
)
