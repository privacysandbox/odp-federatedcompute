// Copyright 2023 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
////////////////////////////////////////////////////////////////////////////////

package com.google.ondevicepersonalization.federatedcompute.shuffler.taskmanagement.core;

import com.google.ondevicepersonalization.federatedcompute.proto.Task;
import java.util.Optional;

/** Task core service interface. */
public interface TaskManagementCore {

  /** Get task by id. */
  public Optional<Task> getTaskById(String populationName, long taskId);

  /** Create new task. */
  public Task createTask(Task task);
}