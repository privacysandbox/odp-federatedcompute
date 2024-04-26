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
# All commands run relative to this directory
cd "$(dirname "${BASH_SOURCE[0]}")"

VERSION="3_10"

touch "requirements_lock_$VERSION.txt"
bazel run --experimental_convenience_symlinks=ignore //:requirements_"$VERSION".update
sed -i '/^#/d' requirements_lock_"$VERSION".txt
mv requirements_lock_"$VERSION".txt ../../requirements_lock_"$VERSION".txt
