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

import static com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.CheckInResult.ITERATION_NOT_ACTIVE;

import com.google.internal.federatedcompute.v1.RejectionInfo;
import com.google.internal.federatedcompute.v1.RejectionReason;
import com.google.internal.federatedcompute.v1.ResourceCompressionFormat;
import com.google.internal.federatedcompute.v1.RetryWindow;
import com.google.ondevicepersonalization.federatedcompute.proto.CreateTaskAssignmentResponse;
import com.google.ondevicepersonalization.federatedcompute.proto.TaskAssignment;
import com.google.ondevicepersonalization.federatedcompute.proto.UploadInstruction;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.CompressionUtils.CompressionFormat;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.UniqueIdGenerator;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AssignmentDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AssignmentEntity.Status;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AssignmentId;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobDescription;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobManager;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.CheckInResult;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.IterationEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskDao;
import com.google.protobuf.Duration;
import java.util.List;
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

  public CreateTaskAssignmentResponse createTaskAssignment(
      String populationName, String clientVersion, String correlationId, CompressionFormat format) {
    // Check which tasks with the given population name are available for check in
    Map<IterationEntity, CheckInResult> activeIterations =
        taskDao.getAvailableCheckInsForPopulation(populationName, clientVersion);
    // Get the result of the check in. The lowest CheckInResult should be the one that matters.
    CheckInResult checkInResult =
        CheckInResult.fromCode(
            activeIterations.values().stream()
                .map(CheckInResult::code)
                .min(Long::compare)
                .orElse(ITERATION_NOT_ACTIVE.code()));
    if (checkInResult != CheckInResult.SUCCESS) {
      return createTaskAssignmentResponseWithRejectionInfo(checkInResult);
    }
    // Handle traffic selection for iterations with possible check in success.
    List<IterationEntity> openIterations =
        activeIterations.entrySet().stream()
            .filter(entry -> entry.getValue() == CheckInResult.SUCCESS)
            .map(Map.Entry::getKey)
            .toList();
    Optional<IterationEntity> selectIterationEntity =
        taskAssignmentCoreHelper.selectIterationEntity(openIterations);

    if (selectIterationEntity.isEmpty()) {
      // The SUCCESS entries contains only tasks with zero weight
      // Consider tasks with zero weight to be NOT_ACTIVE for now.
      return createTaskAssignmentResponseWithRejectionInfo(ITERATION_NOT_ACTIVE);
    }

    Optional<TaskAssignment> ta =
        assignmentDao
            .createAssignment(selectIterationEntity.get(), correlationId, idGenerator.generate())
            .flatMap(
                assignmentEntity ->
                    taskAssignmentCoreHelper.createTaskAssignment(
                        selectIterationEntity.get(), assignmentEntity, format));

    if (ta.isPresent()) {
      return CreateTaskAssignmentResponse.newBuilder().setTaskAssignment(ta.get()).build();
    } else {
      throw new IllegalStateException("Failed to create task assignment");
    }
  }

  private CreateTaskAssignmentResponse createTaskAssignmentResponseWithRejectionInfo(
      CheckInResult checkInResult) {
    CreateTaskAssignmentResponse.Builder builder = CreateTaskAssignmentResponse.newBuilder();
    logger.info("CreateTaskAssignment rejection due to check in result {}", checkInResult);
    switch (checkInResult) {
      case ITERATION_FULL ->
          builder.setRejectionInfo(
              RejectionInfo.newBuilder()
                  .setReason(RejectionReason.Enum.NO_TASK_AVAILABLE)
                  .setRetryWindow(
                      RetryWindow.newBuilder()
                          .setDelayMin(Duration.newBuilder().setSeconds(60))
                          .setDelayMax(Duration.newBuilder().setSeconds(300))));
      case ITERATION_NOT_OPEN ->
          builder.setRejectionInfo(
              RejectionInfo.newBuilder()
                  .setReason(RejectionReason.Enum.NO_TASK_AVAILABLE)
                  .setRetryWindow(
                      RetryWindow.newBuilder()
                          .setDelayMin(Duration.newBuilder().setSeconds(60))
                          .setDelayMax(Duration.newBuilder().setSeconds(300))));
      case CLIENT_VERSION_MISMATCH ->
          builder.setRejectionInfo(
              RejectionInfo.newBuilder()
                  .setReason(RejectionReason.Enum.CLIENT_VERSION_MISMATCH)
                  .setRetryWindow(
                      RetryWindow.newBuilder()
                          .setDelayMin(Duration.newBuilder().setSeconds(86400)) // 1 Day
                          .setDelayMax(Duration.newBuilder().setSeconds(86400 * 2))));
      case ITERATION_NOT_ACTIVE ->
          builder.setRejectionInfo(
              RejectionInfo.newBuilder()
                  .setReason(RejectionReason.Enum.NO_ACTIVE_TASK_EXISTS)
                  .setRetryWindow(
                      RetryWindow.newBuilder()
                          .setDelayMin(Duration.newBuilder().setSeconds(86400))
                          .setDelayMax(Duration.newBuilder().setSeconds(86400 * 2))));
      default ->
          throw new IllegalStateException("Invalid check in result " + checkInResult + " found.");
    }
    return builder.build();
  }

  public Optional<UploadInstruction> getUploadInstruction(
      String populationName,
      long taskId,
      String aggregationId,
      String assignmentId,
      CompressionFormat compressionFormat) {
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
        .map(
            assignment1 ->
                blobManager.generateUploadGradientDescription(assignment1, compressionFormat))
        .map(this::convertToUploadInstruction);
  }

  // Report local succeeded.
  public void reportLocalCompleted(
      String populationName, long taskId, String aggregationId, String assignmentId) {
    reportLocalResult(populationName, taskId, aggregationId, assignmentId, Status.LOCAL_COMPLETED);
  }

  // Report local failed.
  public void reportLocalFailed(
      String populationName, long taskId, String aggregationId, String assignmentId) {
    reportLocalResult(populationName, taskId, aggregationId, assignmentId, Status.LOCAL_FAILED);
  }

  // Report local not eligible.
  public void reportLocalNotEligible(
      String populationName, long taskId, String aggregationId, String assignmentId) {
    reportLocalResult(
        populationName, taskId, aggregationId, assignmentId, Status.LOCAL_NOT_ELIGIBLE);
  }

  public void reportLocalFailedExampleGeneration(
      String populationName, long taskId, String aggregationId, String assignmentId) {
    reportLocalResult(
        populationName,
        taskId,
        aggregationId,
        assignmentId,
        Status.LOCAL_FAILED_EXAMPLE_GENERATION);
  }

  public void reportLocalFailedModelComputation(
      String populationName, long taskId, String aggregationId, String assignmentId) {
    reportLocalResult(
        populationName, taskId, aggregationId, assignmentId, Status.LOCAL_FAILED_MODEL_COMPUTATION);
  }

  public void reportLocalFailedOpsError(
      String populationName, long taskId, String aggregationId, String assignmentId) {
    reportLocalResult(
        populationName, taskId, aggregationId, assignmentId, Status.LOCAL_FAILED_OPS_ERROR);
  }

  public void reportLocalResult(
      String populationName,
      long taskId,
      String aggregationId,
      String assignmentId,
      Status newStatus) {
    assignmentDao.updateAssignmentStatus(
        AssignmentId.builder()
            .populationName(populationName)
            .taskId(taskId)
            .iterationId(Long.parseLong(aggregationId))
            .attemptId(0)
            .assignmentId(assignmentId)
            .build(),
        Status.ASSIGNED,
        newStatus);
  }

  private UploadInstruction convertToUploadInstruction(BlobDescription blobDescription) {
    return UploadInstruction.newBuilder()
        .setUploadLocation(blobDescription.getUrl())
        .putAllExtraRequestHeaders(blobDescription.getHeaders())
        // client side only supports gzip, always return gzip for now
        .setCompressionFormat(ResourceCompressionFormat.RESOURCE_COMPRESSION_FORMAT_GZIP)
        .build();
  }
}
