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


# Fail on any error.
set -e
# Print commands
set -x

# Upload all new images.
# This should be run within the dockerbuild file to ensure deterministic builds
bazel run //shuffler/services/taskscheduler:task_scheduler_image_publish
bazel run //shuffler/services/taskmanagement:task_management_image_publish
bazel run //shuffler/services/taskassignment:task_assignment_image_publish
bazel run //shuffler/services/modelupdater:model_updater_image_publish
bazel run //shuffler/services/collector:collector_image_publish
bazel run //shuffler/services/aggregator:aggregator_image_publish
bazel run //shuffler/services/taskbuilder:task_builder_image_publish
