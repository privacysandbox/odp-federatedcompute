#!/bin/bash
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


# Exit on failure
set -e
# Print commands
set -x

# Java
find -name '*.java' -not -path "./federatedcompute/*" | xargs google-java-format -i

# C++ & Proto
find -name '*.cc' -name '*.cpp' -o -name '*.proto' -not -path "./federatedcompute/*" | xargs clang-format -i

# Python
find -name '*.py' -not -path "./federatedcompute/*" | xargs pyformat --in_place

# Bazel
find -name 'BUILD' -name '*.BUILD' -name 'WORKSPACE' -name '*.bzl' -not -path "./federatedcompute/*" | xargs buildifier

# Terraform
terraform fmt -recursive