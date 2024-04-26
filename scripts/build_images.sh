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
# This should be run within the dockerbuild file to ensure deterministic builds
bazel build //shuffler/services/taskscheduler:task_scheduler_image "$@"
bazel build //shuffler/services/taskmanagement:task_management_image "$@"
bazel build //shuffler/services/taskassignment:task_assignment_image "$@"
bazel build //shuffler/services/modelupdater:model_updater_image "$@"
bazel build //shuffler/services/collector:collector_image "$@"
bazel build //shuffler/services/aggregator:aggregator_image "$@"

# Print container digests.
set +x
for file in bazel-bin/shuffler/services/*/*/index.json; do
  grep -H digest "$file"
done

