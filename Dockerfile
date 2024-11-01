# Copyright 2024 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# debian/snapshot:stable-20231030
FROM debian/snapshot@sha256:b70259f2e38475f8efa81edf37bec56a9c538172a53458776655e048d3fb468a

# Install packages and bazel dependencies.
RUN apt-get update && apt-get install --no-install-recommends -y \
    curl \
    unzip \
    zip \
    xz-utils \
    wget \
    default-jdk \
    libtinfo5 \
    g++ \
    docker.io \
    python3 \
    jq

# Install bazel.
RUN wget -q https://github.com/bazelbuild/bazel/releases/download/7.3.2/bazel-7.3.2-installer-linux-x86_64.sh && \
    chmod +x bazel-7.3.2-installer-linux-x86_64.sh && \
    ./bazel-7.3.2-installer-linux-x86_64.sh && \
    cd "/usr/local/lib/bazel/bin" && \
    wget -q https://releases.bazel.build/7.3.2/release/bazel-7.3.2-linux-x86_64 && \
    chmod +x bazel-7.3.2-linux-x86_64
ENV PATH="/usr/local/bin:$PATH"

# Install clang+llvm.
RUN wget -q https://github.com/llvm/llvm-project/releases/download/llvmorg-16.0.0/clang+llvm-16.0.0-x86_64-linux-gnu-ubuntu-18.04.tar.xz && \
    tar -xf clang+llvm-16.0.0-x86_64-linux-gnu-ubuntu-18.04.tar.xz
ENV PATH="/clang+llvm-16.0.0-x86_64-linux-gnu-ubuntu-18.04/bin:$PATH"
