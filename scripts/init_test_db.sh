#! /bin/bash
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


set -e
set -x

# create instance
gcloud spanner instances create fcp-task-unittest \
   --config=emulator-config --description="fcp task emulator" --nodes=1
# create table in emulator
gcloud spanner databases create test-task-dao --ddl-file=shuffler/spanner/task_database.sdl --instance=fcp-task-unittest
gcloud spanner databases create test-assignment-dao --ddl-file=shuffler/spanner/task_database.sdl --instance=fcp-task-unittest
gcloud spanner databases create authorization-token-dao --ddl-file=shuffler/spanner/task_database.sdl --instance=fcp-task-unittest
