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
set -x

# Use local toolchain for coverage to reduce shared library size
bazel coverage --combined_report=lcov --incompatible_enable_cc_toolchain_resolution=false --test_output=errors -- //java/... //python/... //java/src/test/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao/gcp:task_spanner_dao_test //java/src/test/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao/gcp:assignment_spanner_dao_test //java/src/test/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao/gcp:aggregation_batch_spanner_dao_test

genhtml --output genhtml "$(bazel info output_path)/_coverage/_coverage_report.dat"