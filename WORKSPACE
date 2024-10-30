# Copyright 2021 Google LLC
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

# This file uses repository rules to fetch external dependencies.
#
# We require that all uses of repository rules are effectively deterministic -
# they may fail, but otherwise must always produce exactly the same output for
# given parameters. For the http_archive rule, this means that you must specify
# a sha256 parameter; consequently, make sure to use stable URLs, rather than
# e.g. one that names a git branch.
#
# This is so that all(*) inputs to the build are fully determined (i.e. a
# change to build inputs requires a change to our sources), which avoids
# confusing outcomes from caching. If it is ever productive to clear your Bazel
# cache, that's a bug.
#
# (*) A Bazel build depends on local compiler toolchains (and Bazel itself), so
# it can be useful to pick a particular container image too (like some version
# of http://l.gcr.io/bazel).
#

workspace(name = "com_google_ondevicepersonalization_federatedcompute")

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

############
# Java build dependencies.
############

RULES_JVM_EXTERNAL_TAG = "6.0"

RULES_JVM_EXTERNAL_SHA = "85fd6bad58ac76cc3a27c8e051e4255ff9ccd8c92ba879670d195622e7c0a9b7"

http_archive(
    name = "rules_jvm_external",
    sha256 = RULES_JVM_EXTERNAL_SHA,
    strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
    url = "https://github.com/bazelbuild/rules_jvm_external/releases/download/%s/rules_jvm_external-%s.tar.gz" % (RULES_JVM_EXTERNAL_TAG, RULES_JVM_EXTERNAL_TAG),
)

load("@rules_jvm_external//:repositories.bzl", "rules_jvm_external_deps")

rules_jvm_external_deps()

load("@rules_jvm_external//:setup.bzl", "rules_jvm_external_setup")

rules_jvm_external_setup()

load("@rules_jvm_external//:defs.bzl", "maven_install")

maven_install(
    name = "maven",
    artifacts = [
        # common
        "org.projectlombok:lombok:1.18.34",
        "com.google.protobuf:protobuf-java-util:3.24.3",
        "com.google.protobuf:protobuf-java:3.24.3",
        "com.google.guava:guava:32.1.2-jre",
        "com.beust:jcommander:1.82",
        "com.google.code.gson:gson:2.10.1",
        "com.google.auto.value:auto-value:1.7.4",
        "com.google.code.findbugs:jsr305:3.0.2",
        "io.github.resilience4j:resilience4j-core:1.7.1",
        "io.github.resilience4j:resilience4j-retry:1.7.1",
        # Apache 4.x used for coordinator dependency.
        "org.apache.httpcomponents:httpcore:4.4.14",
        "org.apache.httpcomponents:httpclient:4.5.13",
        "org.apache.httpcomponents.client5:httpclient5:5.3.1",
        "org.apache.httpcomponents.core5:httpcore5:5.1.4",
        "org.apache.httpcomponents.core5:httpcore5-h2:5.1.4",  # Explicit transitive dependency to avoid https://issues.apache.org/jira/browse/HTTPCLIENT-2222
        "com.fasterxml.jackson.datatype:jackson-datatype-guava:2.15.2",
        "com.fasterxml.jackson.core:jackson-annotations:2.15.2",
        "com.fasterxml.jackson.core:jackson-core:2.15.2",
        "com.fasterxml.jackson.core:jackson-databind:2.15.2",
        "com.google.crypto.tink:tink:1.12.0",
        "com.google.inject:guice:7.0.0",
        "org.slf4j:slf4j-api:2.0.7",
        "com.google.cloud:google-cloud-logging-logback:0.131.8-alpha",
        # spring
        "org.springframework.boot:spring-boot-autoconfigure:3.3.4",
        "org.springframework.boot:spring-boot-starter-actuator:3.3.4",
        "org.springframework.boot:spring-boot-starter-web:3.3.4",
        "org.springframework.boot:spring-boot-starter:3.3.4",
        "org.springframework.boot:spring-boot:3.3.4",
        "org.springframework.boot:spring-boot-loader:3.3.4",
        "org.springframework.boot:spring-boot-starter-jdbc:3.3.4",
        "org.springframework:spring-beans:6.1.13",
        "org.springframework:spring-web:6.1.13",
        "org.springframework:spring-context:6.1.13",
        "org.springframework:spring-webmvc:6.1.13",
        "org.springframework.integration:spring-integration-jdbc:6.1.3",
        "jakarta.servlet:jakarta.servlet-api:6.1.0",
        "org.springframework.integration:spring-integration-core:6.3.4",
        # micrometer
        "io.micrometer:micrometer-core:1.12.1",
        "io.micrometer:micrometer-registry-prometheus:1.12.1",
        # junit
        "com.google.testparameterinjector:test-parameter-injector:1.12",
        "com.google.truth:truth:1.1.5",
        "junit:junit:4.13",
        "org.mockito:mockito-core:5.5.0",
        # test-container
        "org.testcontainers:testcontainers:1.19.0",
        # gcp
        "com.google.api:gax:2.31.1",
        "com.google.cloud:google-cloud-spanner:6.43.2",
        "com.google.cloud:google-cloud-storage:2.25.0",
        "com.google.cloud:spring-cloud-gcp-starter-pubsub:4.9.4",
        "com.google.auth:google-auth-library-oauth2-http:1.11.0",
        "com.google.crypto.tink:tink-gcpkms:1.9.0",
        "com.google.cloud:google-cloud-secretmanager:2.24.0",
        "org.postgresql:postgresql:42.7.3",
        "com.google.cloud:google-cloud-spanner-pgadapter:0.23.1",
        "com.kohlschutter.junixsocket:junixsocket-core:2.9.0",
        # threetenbp
        "org.threeten:threetenbp:1.6.8",
    ],
    repositories = [
        "https://maven.google.com",
        "https://repo1.maven.org/maven2",
    ],
)

http_archive(
    name = "rules_spring",
    patches = [
        # Remove local build system output to maintain deterministic build.
        "//patches:rules_spring.patch",
        "//patches:rules_spring_manifest.patch",
    ],
    sha256 = "87b337f95f9c09a2e5875f0bca533b050c9ccb8b0d2c92915e290520b79d0912",
    urls = [
        "https://github.com/salesforce/rules_spring/releases/download/2.3.2/rules-spring-2.3.2.zip",
    ],
)

http_archive(
    name = "coordinator-services-and-shared-libraries",
    build_file = "coordinator.BUILD",
    sha256 = "7879ff1c7bf0f96a6214725fe87bf39ef5297db4aab8d39d7b6fef49c2643c55",
    strip_prefix = "coordinator-services-and-shared-libraries-1.7.0",
    urls = [
        "https://github.com/privacysandbox/coordinator-services-and-shared-libraries/archive/refs/tags/v1.7.0.tar.gz",
    ],
)

################
# Import FCP and dependencies.
################

# Points to git submodule
local_repository(
    name = "federatedcompute",
    path = "federatedcompute",
)

http_archive(
    name = "oak",
    sha256 = "8bf19718abc453bea10c676178d37479bc309dc193d875e391c27853f1203c8e",
    strip_prefix = "oak-0acf3f6dc0af2035d40884fe1258b1e0e7db5488",
    url = "https://github.com/project-oak/oak/archive/0acf3f6dc0af2035d40884fe1258b1e0e7db5488.tar.gz",
)

# The following enables the use of the library functions in the differential-
# privacy github repo
http_archive(
    name = "com_google_cc_differential_privacy",
    sha256 = "6e6e1cd7a819695caae408f4fa938129ab7a86e83fe2410137c85e50131abbe0",
    strip_prefix = "differential-privacy-3.0.0/cc",
    url = "https://github.com/google/differential-privacy/archive/refs/tags/v3.0.0.tar.gz",
)

http_archive(
    name = "com_google_differential_privacy",
    sha256 = "6e6e1cd7a819695caae408f4fa938129ab7a86e83fe2410137c85e50131abbe0",
    strip_prefix = "differential-privacy-3.0.0",
    url = "https://github.com/google/differential-privacy/archive/refs/tags/v3.0.0.tar.gz",
)

http_archive(
    name = "org_tensorflow_federated",
    sha256 = "2bb1e641a84f05bc1776eefc98eb108454807071ae0bed45ee5e8bee896511be",
    strip_prefix = "tensorflow-federated-9c5a51af41fccc8e720a63aa754465767113877d",
    url = "https://github.com/tensorflow/federated/archive/9c5a51af41fccc8e720a63aa754465767113877d.tar.gz",
)

http_archive(
    name = "platforms",
    urls = [
        "https://mirror.bazel.build/github.com/bazelbuild/platforms/releases/download/0.0.7/platforms-0.0.7.tar.gz",
        "https://github.com/bazelbuild/platforms/releases/download/0.0.7/platforms-0.0.7.tar.gz",
    ],
    sha256 = "3a561c99e7bdbe9173aa653fd579fe849f1d8d67395780ab4770b1f381431d51",
)

# Tensorflow v2.14.0
http_archive(
    name = "org_tensorflow",
    patches = [
        # This patch works around failures in GitHub infrastructure to
        # download versions of LLVM pointed to by non-HEAD TensorFlow.
        "@federatedcompute//fcp/patches:tensorflow_llvm_url.patch",
        # This patch replaces tf_gen_op_wrapper_py's dependency on @tensorflow
        # with @pypi_tensorflow.
        "@federatedcompute//fcp/patches:tensorflow_tf_gen_op_wrapper_py.patch",
        # gRPC v1.48.0-pre1 and later include zconf.h in addition to zlib.h;
        # TensorFlow's build rule for zlib only exports the latter.
        "@federatedcompute//fcp/patches:tensorflow_zlib.patch",
    ],
    sha256 = "ce357fd0728f0d1b0831d1653f475591662ec5bca736a94ff789e6b1944df19f",
    strip_prefix = "tensorflow-2.14.0",
    urls = [
        "https://github.com/tensorflow/tensorflow/archive/v2.14.0.tar.gz",
    ],
)

# The following is copied from TensorFlow's own WORKSPACE, see
# https://github.com/tensorflow/tensorflow/blob/v2.14.0/WORKSPACE#L6
http_archive(
    name = "bazel_skylib",
    sha256 = "74d544d96f4a5bb630d465ca8bbcfe231e3594e5aae57e1edbf17a6eb3ca2506",
    urls = [
        "https://storage.googleapis.com/mirror.tensorflow.org/github.com/bazelbuild/bazel-skylib/releases/download/1.3.0/bazel-skylib-1.3.0.tar.gz",
        "https://github.com/bazelbuild/bazel-skylib/releases/download/1.3.0/bazel-skylib-1.3.0.tar.gz",
    ],
)

http_archive(
    name = "rules_python",
    sha256 = "be04b635c7be4604be1ef20542e9870af3c49778ce841ee2d92fcb42f9d9516a",
    strip_prefix = "rules_python-0.35.0",
    url = "https://github.com/bazelbuild/rules_python/releases/download/0.35.0/rules_python-0.35.0.tar.gz",
)

load("@rules_python//python:repositories.bzl", "py_repositories", "python_register_toolchains")

py_repositories()

python_register_toolchains(
    name = "python",
    ignore_root_user_error = True,
    python_version = "3.10",
)

http_archive(
    name = "rules_proto",
    sha256 = "dc3fb206a2cb3441b485eb1e423165b231235a1ea9b031b4433cf7bc1fa460dd",
    strip_prefix = "rules_proto-5.3.0-21.7",
    urls = [
        "https://github.com/bazelbuild/rules_proto/archive/refs/tags/5.3.0-21.7.tar.gz",
    ],
)

http_archive(
    name = "com_google_protobuf",
    sha256 = "75be42bd736f4df6d702a0e4e4d30de9ee40eac024c4b845d17ae4cc831fe4ae",
    strip_prefix = "protobuf-21.7",
    urls = [
        "https://mirror.bazel.build/github.com/protocolbuffers/protobuf/archive/v21.7.tar.gz",
        "https://github.com/protocolbuffers/protobuf/archive/v21.7.tar.gz",
    ],
)

load("@rules_proto//proto:repositories.bzl", "rules_proto_dependencies", "rules_proto_toolchains")

rules_proto_toolchains()

load("@federatedcompute//fcp/tensorflow/pip_tf:defs.bzl", "TF_ADDITIVE_BUILD_CONTENT")
load("@python//:defs.bzl", "interpreter")
load("@rules_python//python:pip.bzl", "package_annotation", "pip_parse")

http_archive(
    name = "differential_privacy",
    repo_mapping = {
        "@accounting_py_pip_deps": "@pypi",
    },
    sha256 = "6e6e1cd7a819695caae408f4fa938129ab7a86e83fe2410137c85e50131abbe0",
    strip_prefix = "differential-privacy-3.0.0/python/dp_accounting/",
    urls = [
        "https://github.com/google/differential-privacy/archive/refs/tags/v3.0.0.tar.gz",
    ],
)

pip_parse(
    name = "pypi",
    annotations = {
        "tensorflow": package_annotation(
            additive_build_content = TF_ADDITIVE_BUILD_CONTENT,
        )
    },
    extra_pip_args = [
        # Disable build isolation to avoid un-pinned build dependencies to be installed
        # https://pip.pypa.io/en/stable/reference/build-system/pyproject-toml/#build-time-dependencies
        # Pre-installed build deps: https://github.com/bazelbuild/rules_python/blob/2a5ba18d60b25e10b99d0fa87b1da51f40d9f0d3/python/private/pypi/deps.bzl
        "--no-build-isolation",
    ],
    python_interpreter_target = interpreter,
    requirements_lock = "//:requirements_lock_3_10.txt",
)

load("@pypi//:requirements.bzl", "install_deps")

install_deps()

http_archive(
    name = "com_github_grpc_grpc",
    sha256 = "76900ab068da86378395a8e125b5cc43dfae671e09ff6462ddfef18676e2165a",
    strip_prefix = "grpc-1.50.0",
    urls = ["https://github.com/grpc/grpc/archive/refs/tags/v1.50.0.tar.gz"],
)

load("@org_tensorflow//tensorflow:workspace3.bzl", "tf_workspace3")

tf_workspace3()

load("@org_tensorflow//tensorflow:workspace2.bzl", "tf_workspace2")

tf_workspace2()

load("@org_tensorflow//tensorflow:workspace1.bzl", "tf_workspace1")

tf_workspace1(False)

load("@org_tensorflow//tensorflow:workspace0.bzl", "tf_workspace0")

tf_workspace0()

load("//tools:toolchain_provided_tf.bzl", "toolchain_provided_tf")

toolchain_provided_tf(
    name = "system_provided_tf",
    tensorflow_repository = "@pypi_tensorflow",
)

################
# Rules for container building
################
# Add bazel-lib to support python image creation
http_archive(
    name = "aspect_bazel_lib",
    sha256 = "0e31778f1fd574d2c05d238bfc4c785fa4b7e50a5ef38b506e01cfd8ec2fccb3",
    strip_prefix = "bazel-lib-2.9.2",
    url = "https://github.com/bazel-contrib/bazel-lib/releases/download/v2.9.2/bazel-lib-v2.9.2.tar.gz",
)

load("@aspect_bazel_lib//lib:repositories.bzl", "aspect_bazel_lib_dependencies", "aspect_bazel_lib_register_toolchains")

aspect_bazel_lib_dependencies()

aspect_bazel_lib_register_toolchains()

# Add rules_oci for container building
http_archive(
    name = "rules_oci",
    sha256 = "58b7a175ee90c12583afeca388523adf6a4e5a0528f330b41c302b91a4d6fc06",
    strip_prefix = "rules_oci-1.6.0",
    url = "https://github.com/bazel-contrib/rules_oci/releases/download/v1.6.0/rules_oci-v1.6.0.tar.gz",
)

load("@rules_oci//oci:dependencies.bzl", "rules_oci_dependencies")

rules_oci_dependencies()

load("@rules_oci//oci:repositories.bzl", "LATEST_CRANE_VERSION", "oci_register_toolchains")

oci_register_toolchains(
    name = "oci",
    crane_version = LATEST_CRANE_VERSION,
)

load("@rules_oci//oci:pull.bzl", "oci_pull")

oci_pull(
    # Use python_base: https://github.com/bazelbuild/rules_python/issues/691
    name = "python_base",
    # python:slim
    digest = "sha256:d0c9bf03d80a3c00963f1b973760094c0b2070638fa64dd4f67d2247c0110efc",
    image = "python",
)

# Distroless image for running Java.
oci_pull(
    name = "java_base",
    # Using SHA-256 for reproducibility. The tag is nonroot.
    digest = "sha256:fb33b0098b86d965a1ef64ec49b8ff0a28ae8be939f0df736c4af9251180a896",
    image = "gcr.io/distroless/java17-debian11",
    platforms = ["linux/amd64"],
)

# Pull individual deb packages to support Tensorflow JNI.
_DEB_TO_LAYER = """\
alias(
    name = "layer",
    actual = ":data.tar.xz",
    visibility = ["//visibility:public"],
)
"""

http_archive(
    name = "libcc1-16_amd64",
    build_file_content = _DEB_TO_LAYER,
    sha256 = "1703fb027276d6f57fc0aaedafe9ce5bc0317148e6b35c57d5d9e6ab2da84fcd",
    urls = [
        "https://snapshot.debian.org/archive/debian/20231015T085924Z/pool/main/l/llvm-toolchain-16/libc%2B%2B1-16_16.0.6-16_amd64.deb",
    ],
)

http_archive(
    name = "libccabi1-16_amd64",
    build_file_content = _DEB_TO_LAYER,
    sha256 = "336673e61d95836b77a776b437bf705f1235cf69a8e848c810b2fdbade9c0a1e",
    urls = [
        "https://snapshot.debian.org/archive/debian/20231015T085924Z/pool/main/l/llvm-toolchain-16/libc%2B%2Babi1-16_16.0.6-16_amd64.deb",
    ],
)

http_archive(
    name = "libc6_amd64",
    build_file_content = _DEB_TO_LAYER,
    sha256 = "f1b1479c4efbc78637d108b3ca281df6d44fe198a94be8f9621c78dc090340a2",
    urls = [
        "https://snapshot.debian.org/archive/debian/20231003T205808Z/pool/main/g/glibc/libc6_2.37-12_amd64.deb",
    ],
)

http_archive(
    name = "libunwind-16_amd64",
    build_file_content = _DEB_TO_LAYER,
    sha256 = "cfe5e000e6f48479db390a88dbef30cd774e4b0e61aff3761d05027177c9cb3d",
    urls = [
        "https://snapshot.debian.org/archive/debian/20231015T085924Z/pool/main/l/llvm-toolchain-16/libunwind-16_16.0.6-16_amd64.deb",
    ],
)

http_archive(
    name = "libgcc-s1_amd64",
    build_file_content = _DEB_TO_LAYER,
    sha256 = "1e992a4f8e84890c29ca5984faeeebdc90e6a4b9b6dfcc5dbbc2dc1c248272e3",
    urls = [
        "https://snapshot.debian.org/archive/debian/20231007T024024Z/pool/main/g/gcc-13/libgcc-s1_13.2.0-5_amd64.deb",
    ],
)

http_archive(
    name = "gcc-13-base_amd64",
    build_file_content = _DEB_TO_LAYER,
    sha256 = "adf1429d782333621d7913bb9f2ff1e4b0bf50be06b49d3d0597ebeabaed4641",
    urls = [
        "https://snapshot.debian.org/archive/debian/20231007T024024Z/pool/main/g/gcc-13/gcc-13-base_13.2.0-5_amd64.deb",
    ],
)

################
# C++ Clang/LLVM Toolchain
################

http_archive(
    name = "toolchains_llvm",
    canonical_id = "0.10.3",
    sha256 = "b7cd301ef7b0ece28d20d3e778697a5e3b81828393150bed04838c0c52963a01",
    strip_prefix = "toolchains_llvm-0.10.3",
    url = "https://github.com/grailbio/bazel-toolchain/releases/download/0.10.3/toolchains_llvm-0.10.3.tar.gz",
)

load("@toolchains_llvm//toolchain:deps.bzl", "bazel_toolchain_dependencies")

bazel_toolchain_dependencies()

load("@toolchains_llvm//toolchain:rules.bzl", "llvm_toolchain")

llvm_toolchain(
    name = "llvm_toolchain",
    llvm_versions = {
        "": "16.0.0",
    },
    sha256 = {
        "": "2b8a69798e8dddeb57a186ecac217a35ea45607cb2b3cf30014431cff4340ad1",
    },
    strip_prefix = {
        "": "clang+llvm-16.0.0-x86_64-linux-gnu-ubuntu-18.04",
    },
    urls = {
        "": ["https://github.com/llvm/llvm-project/releases/download/llvmorg-16.0.0/clang+llvm-16.0.0-x86_64-linux-gnu-ubuntu-18.04.tar.xz"],
    },
)

load("@llvm_toolchain//:toolchains.bzl", "llvm_register_toolchains")

llvm_register_toolchains()


################
# pybind11
################

http_archive(
  name = "pybind11_bazel",
  strip_prefix = "pybind11_bazel-2.12.0",
  sha256 = "a58c25c5fe063a70057fa20cb8e15f3bda19b1030305bcb533af1e45f36a4a55",
  urls = ["https://github.com/pybind/pybind11_bazel/archive/pybind11_bazel-2.12.0.zip"],
)
# We still require the pybind library.
http_archive(
  name = "pybind11",
  build_file = "@pybind11_bazel//:pybind11-BUILD.bazel",
  strip_prefix = "pybind11-2.12.0",
  sha256 = "411f77380c43798506b39ec594fc7f2b532a13c4db674fcf2b1ca344efaefb68",
  urls = ["https://github.com/pybind/pybind11/archive/pybind11-2.12.0.zip"],
)
