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

package com.google.ondevicepersonalization.federatedcompute.shuffler.taskassignment.core;

import com.google.ondevicepersonalization.federatedcompute.proto.TaskAssignment;
import com.google.ondevicepersonalization.federatedcompute.proto.UploadInstruction;
import java.util.Optional;

/** The task assignment service core. */
public interface TaskAssignmentCore {
  public Optional<TaskAssignment> createTaskAssignment(
      String populationName, String clientVersion, String correlationId);

  // Get result upload instruction.
  public Optional<UploadInstruction> getUploadInstruction(
      String populationName, long taskId, String aggregationId, String assignmentId);

  // Report local succeeded.
  public void reportLocalCompleted(
      String populationName, long taskId, String aggregationId, String assignmentId);

  // Report local failed.
  public void reportLocalFailed(
      String populationName, long taskId, String aggregationId, String assignmentId);

  public void reportLocalNotEligible(
      String populationName, long taskId, String aggregationId, String assignmentId);
}
