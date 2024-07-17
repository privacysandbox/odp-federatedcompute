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

# Build all java images and store in local repository with bazel run
bazel run //shuffler/services/taskscheduler:tarball
bazel run //shuffler/services/taskmanagement:tarball
bazel run //shuffler/services/taskassignment:tarball
bazel run //shuffler/services/modelupdater:tarball
bazel run //shuffler/services/collector:tarball
bazel run //shuffler/services/aggregator:tarball
bazel run //shuffler/services/taskbuilder:tarball

