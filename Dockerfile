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

# debian:stable-20250113
FROM --platform=linux/amd64 debian@sha256:49b5c367b1f381be3fb5693e3500d2ef30d074b600dfa9132f742dc6d1c26dd2

# Set debian sources to use debian snapshot for deterministic packages
RUN echo 'Acquire::Check-Valid-Until "false";' > /etc/apt/apt.conf.d/docker-snapshot.conf

RUN echo '\
Types: deb\n\
URIs: http://snapshot.debian.org/archive/debian/20250113T000000Z\n\
Suites: stable stable-updates\n\
Components: main\n\
Signed-By: /usr/share/keyrings/debian-archive-keyring.gpg\n\
\n\
Types: deb\n\
URIs: http://snapshot.debian.org/archive/debian-security/20250113T000000Z\n\
Suites: stable-security\n\
Components: main\n\
Signed-By: /usr/share/keyrings/debian-archive-keyring.gpg' > /etc/apt/sources.list.d/debian.sources

# Increase reties and timeout to account for stability issues with debian snapshot
RUN echo '\
Acquire::Retries "10";\
Acquire::https::Timeout "600";\
Acquire::http::Timeout "600";\
' > /etc/apt/apt.conf.d/99custom

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
RUN wget -q https://github.com/bazelbuild/bazel/releases/download/7.4.0/bazel-7.4.0-installer-linux-x86_64.sh && \
    chmod +x bazel-7.4.0-installer-linux-x86_64.sh && \
    ./bazel-7.4.0-installer-linux-x86_64.sh && \
    cd "/usr/local/lib/bazel/bin" && \
    wget -q https://releases.bazel.build/7.4.0/release/bazel-7.4.0-linux-x86_64 && \
    chmod +x bazel-7.4.0-linux-x86_64
ENV PATH="/usr/local/bin:$PATH"

# Install clang+llvm.
RUN wget -q https://github.com/llvm/llvm-project/releases/download/llvmorg-16.0.0/clang+llvm-16.0.0-x86_64-linux-gnu-ubuntu-18.04.tar.xz && \
    tar -xf clang+llvm-16.0.0-x86_64-linux-gnu-ubuntu-18.04.tar.xz
ENV PATH="/clang+llvm-16.0.0-x86_64-linux-gnu-ubuntu-18.04/bin:$PATH"
