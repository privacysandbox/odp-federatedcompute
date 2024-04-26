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
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.UniqueIdGenerator;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AssignmentDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AssignmentEntity.Status;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AssignmentId;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobDescription;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobManager;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.IterationEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskEntity;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Task assignment core implementation. */
@Service
public class TaskAssignmentCoreImpl implements TaskAssignmentCore {

  private static final Logger logger = LoggerFactory.getLogger(TaskAssignmentCoreImpl.class);

  private TaskDao taskDao;
  private AssignmentDao assignmentDao;
  private UniqueIdGenerator idGenerator;
  private BlobManager blobManager;
  private TaskAssignmentCoreHelper taskAssignmentCoreHelper;

  public TaskAssignmentCoreImpl(
      TaskDao taskDao,
      AssignmentDao assignmentDao,
      UniqueIdGenerator idGenerator,
      BlobManager blobManager,
      TaskAssignmentCoreHelper taskAssignmentCoreHelper) {
    this.taskDao = taskDao;
    this.assignmentDao = assignmentDao;
    this.idGenerator = idGenerator;
    this.blobManager = blobManager;
    this.taskAssignmentCoreHelper = taskAssignmentCoreHelper;
  }

  public Optional<TaskAssignment> createTaskAssignment(
      String populationName, String clientVersion, String correlationId) {
    Map<TaskEntity, IterationEntity> taskEntityIterationEntityMap =
        taskDao.getOpenTasksAndIterations(populationName, clientVersion);
    Optional<TaskEntity> selectedTask =
        taskAssignmentCoreHelper.selectTask(taskEntityIterationEntityMap.keySet());

    if (selectedTask.isEmpty()) {
      return Optional.empty();
    }

    IterationEntity iterationEntity = taskEntityIterationEntityMap.get(selectedTask.get());

    return assignmentDao
        .createAssignment(iterationEntity, correlationId, idGenerator.generate())
        .flatMap(
            assignmentEntity ->
                taskAssignmentCoreHelper.createTaskAssignment(
                    selectedTask.get(), assignmentEntity));
  }

  public Optional<UploadInstruction> getUploadInstruction(
      String populationName, long taskId, String aggregationId, String assignmentId) {
    return assignmentDao
        .getAssignment(
            AssignmentId.builder()
                .populationName(populationName)
                .taskId(taskId)
                .iterationId(Long.parseLong(aggregationId))
                .attemptId(0)
                .assignmentId(assignmentId)
                .build())
        .filter(assignment -> assignment.getStatus() == Status.LOCAL_COMPLETED)
        .map(blobManager::generateUploadGradientDescription)
        .map(this::convertToUploadInstruction);
  }

  // Report local succeeded.
  public void reportLocalCompleted(
      String populationName, long taskId, String aggregationId, String assignmentId) {
    assignmentDao.updateAssignmentStatus(
        AssignmentId.builder()
            .populationName(populationName)
            .taskId(taskId)
            .iterationId(Long.parseLong(aggregationId))
            .attemptId(0)
            .assignmentId(assignmentId)
            .build(),
        Status.ASSIGNED,
        Status.LOCAL_COMPLETED);
  }

  // Report local failed.
  public void reportLocalFailed(
      String populationName, long taskId, String aggregationId, String assignmentId) {
    assignmentDao.updateAssignmentStatus(
        AssignmentId.builder()
            .populationName(populationName)
            .taskId(taskId)
            .iterationId(Long.parseLong(aggregationId))
            .attemptId(0)
            .assignmentId(assignmentId)
            .build(),
        Status.ASSIGNED,
        Status.LOCAL_FAILED);
  }

  // Report local not eligible.
  public void reportLocalNotEligible(
      String populationName, long taskId, String aggregationId, String assignmentId) {
    assignmentDao.updateAssignmentStatus(
        AssignmentId.builder()
            .populationName(populationName)
            .taskId(taskId)
            .iterationId(Long.parseLong(aggregationId))
            .attemptId(0)
            .assignmentId(assignmentId)
            .build(),
        Status.ASSIGNED,
        Status.LOCAL_NOT_ELIGIBLE);
  }

  private UploadInstruction convertToUploadInstruction(BlobDescription blobDescription) {
    return UploadInstruction.newBuilder()
        .setUploadLocation(blobDescription.getUrl())
        .putAllExtraRequestHeaders(blobDescription.getHeaders())
        .build();
  }
}
