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

package com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao;

import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AssignmentEntity.Status;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Task Assignment DAO. */
public interface AssignmentDao {

  /** Creates a task assignment. */
  Optional<AssignmentEntity> createAssignment(
      IterationEntity iteration, String correlationId, String sessionId);

  /** Get a task assignment. */
  Optional<AssignmentEntity> getAssignment(AssignmentId assignmentId);

  /** Update assignment status. */
  boolean updateAssignmentStatus(AssignmentId assignmentId, Status from, Status to);

  /** Query assignments of status updated before a specific time. */
  List<String> queryAssignmentIdsOfStatus(
      IterationId iterationId, Status status, Instant updatedBefore);

  /** Query assignments of status and batch */
  List<String> queryAssignmentIdsOfStatus(
      IterationId iterationId, Status status, Optional<String> batchId);

  /**
   * Batch update assignment status.
   *
   * @return Number of assignment statuses updated.
   */
  public int batchUpdateAssignmentStatus(
      List<AssignmentId> assignmentIds, Optional<String> batchId, Status from, Status to);

  /**
   * Creates a new batch for the iteration, assigns the provided assignments to the new batch, and
   * marks the assignments as status using the provided to and from status.
   *
   * @return True if the updated succeeded, false otherwise.
   */
  public boolean createBatchAndUpdateAssignments(
      List<AssignmentId> assignmentIds,
      IterationEntity iteration,
      Status from,
      Status to,
      String batchId,
      String partition);
}
