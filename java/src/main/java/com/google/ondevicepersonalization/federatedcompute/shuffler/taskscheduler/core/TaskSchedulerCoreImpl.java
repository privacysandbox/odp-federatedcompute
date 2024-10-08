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

package com.google.ondevicepersonalization.federatedcompute.shuffler.taskscheduler.core;

import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.SpannerException;
import com.google.ondevicepersonalization.federatedcompute.proto.IterationInfo;
import com.google.ondevicepersonalization.federatedcompute.proto.TaskInfo;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.CompressionUtils.CompressionFormat;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.Constants;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.ProtoParser;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobManager;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.IterationEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.IterationEntity.Status;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskEntities;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskEntity;
import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.stereotype.Service;

/** The task scheduler implementation. */
@Service
public class TaskSchedulerCoreImpl implements TaskSchedulerCore {

  private static final Logger logger = LoggerFactory.getLogger(TaskSchedulerCoreImpl.class);
  private static final String LOCK_PREFIX = "taskscheduler_";
  private static final String LOCK_COMPLETED_ITERATION_PREFIX = "completed_iteration_";

  private final TaskDao taskDao;
  private final BlobDao blobDao;
  private final BlobManager blobManager;
  private final InstantSource instantSource;
  private final TaskSchedulerCoreHelper taskSchedulerCoreHelper;
  private LockRegistry lockRegistry;
  private final List<CompressionFormat> compressionFormats;

  public TaskSchedulerCoreImpl(
      TaskDao taskDao,
      BlobDao blobDao,
      BlobManager blobManager,
      InstantSource instantSource,
      TaskSchedulerCoreHelper taskSchedulerCoreHelper,
      LockRegistry lockRegistry,
      List<CompressionFormat> compressionFormats) {
    this.taskDao = taskDao;
    this.blobDao = blobDao;
    this.blobManager = blobManager;
    this.instantSource = instantSource;
    this.taskSchedulerCoreHelper = taskSchedulerCoreHelper;
    this.lockRegistry = lockRegistry;
    this.compressionFormats = compressionFormats;

    if (compressionFormats != null
        && compressionFormats.stream().noneMatch(format -> format == CompressionFormat.GZIP)) {
      logger.warn(
          "Only GZIP is currently supported. Client requested format {} including unsupported"
              + " ones.",
          compressionFormats);
    }
  }

  public void processActiveTasks() {
    try {
      MDC.put(Constants.ACTIVITY_ID, UUID.randomUUID().toString());
      taskDao.getActiveTasks().stream().forEach(this::processActiveTask);
    } finally {
      MDC.clear();
    }
  }

  public void processCreatedTasks() {
    try {
      MDC.put(Constants.ACTIVITY_ID, UUID.randomUUID().toString());
      // Create client_checkpoint from init checkpoint.
      taskDao.getCreatedTasks().stream().forEach(this::processCreatedTask);
    } finally {
      MDC.clear();
    }
  }

  public void processCompletedIterations() {
    try {
      MDC.put(Constants.ACTIVITY_ID, UUID.randomUUID().toString());
      // Create client_checkpoint from init checkpoint.
      taskDao.getIterationsOfStatus(Status.COMPLETED).forEach(this::processCompletedIteration);
    } finally {
      MDC.clear();
    }
  }

  private void processCreatedTask(TaskEntity task) {
    try {
      // Create a fake Iteration 0 to extract client checkpoint.
      IterationEntity baseIteration = TaskEntities.createBaseIteration(task);
      Instant startTime = instantSource.instant();
      Lock lock = lockRegistry.obtain(LOCK_PREFIX + task.getId().toString());
      if (lock.tryLock()) {
        try {
          MDC.put(Constants.ITERATION_ID, baseIteration.getId().toString());
          // Generate and upload device checkpoint for new training task
          TaskInfo taskInfo = ProtoParser.toProto(task.getInfo(), TaskInfo.getDefaultInstance());
          if (taskInfo.hasTrainingInfo()) {
            try {
              taskSchedulerCoreHelper.generateAndUploadDeviceCheckpoint(baseIteration);
            } catch (IllegalArgumentException e) {
              logger.warn("Provided iteration's server checkpoint or plan do not exist.");
              return;
            }
          }
          taskDao.updateTaskStatus(task.getId(), TaskEntity.Status.CREATED, TaskEntity.Status.OPEN);
        } finally {
          lock.unlock();
          Duration duration = Duration.between(startTime, instantSource.instant());
          logger.info(
              "processing completed in {} second for task {}.",
              duration.getSeconds(),
              task.getId().toString());
          MDC.remove(Constants.ITERATION_ID);
        }
      }
    } catch (Exception e) {
      logger.atError().setCause(e).log("Failed processing CREATED task.");
    }
  }

  private void processActiveTask(TaskEntity task) {
    try {
      Lock lock = lockRegistry.obtain(LOCK_PREFIX + task.getId().toString());
      if (lock.tryLock()) {
        try {
          Optional<IterationEntity> lastIteration =
              taskDao.getLastIterationOfTask(task.getPopulationName(), task.getTaskId());
          lastIteration.ifPresentOrElse(
              iteration -> processLastIteration(task, iteration),
              () -> processNoLastIteration(task));
        } finally {
          lock.unlock();
        }
      }
    } catch (Exception e) {
      // catch and log all exception for one task avoiding breaking other tasks' processing
      logger.atError().setCause(e).log("Failed to process task {}", task.getId());
    }
  }

  private void processLastIteration(TaskEntity task, IterationEntity iteration) {
    switch (iteration.getStatus()) {
      case COLLECTING:
        return;
      case AGGREGATING:
        return;
      case APPLYING:
        if (taskSchedulerCoreHelper.isApplyingDone(iteration)) {
          taskDao.updateIterationStatus(
              iteration, iteration.toBuilder().status(Status.COMPLETED).build());
          handleCompletedIteration(task, iteration);
        } else {
          Optional<Instant> iterationCreatedTime = taskDao.getIterationCreatedTime(iteration);
          Instant currentTime = instantSource.instant();
          // If Iteration is in APPLYING stage for more than 5 min log as warning.
          iterationCreatedTime
              .filter(createdTime -> createdTime.isBefore(currentTime.minusSeconds(300)))
              .ifPresent(createdTime -> logger.warn(
                  "Iteration {} in APPLYING status for more than 5 min",
                  iteration.getId()));
        }
        return;
      case COMPLETED:
        handleCompletedIteration(task, iteration);
        return;
      case AGGREGATING_FAILED:
        taskDao.updateTaskStatus(task.getId(), task.getStatus(), TaskEntity.Status.FAILED);
        logger.warn("Iteration {} in AGGREGATING_FAILED status", iteration.getId());
        return;
      case APPLYING_FAILED:
        taskDao.updateTaskStatus(task.getId(), task.getStatus(), TaskEntity.Status.FAILED);
        logger.warn("Iteration {} in APPLYING_FAILED status", iteration.getId());
        return;
      case STOPPED:
        throw new UnsupportedOperationException(
            "Iteration STOPPED status is currently unsupported");
      default:
        throw new IllegalStateException("cant' handle status " + iteration.getStatus(), null);
    }
  }

  private void handleCompletedIteration(TaskEntity task, IterationEntity iteration) {
    if (iteration.getIterationId() == task.getTotalIteration()) {
      taskDao.updateTaskStatus(task.getId(), task.getStatus(), TaskEntity.Status.COMPLETED);
    } else {
      createNewIteration(task, iteration.getIterationId());
    }
  }

  private void processNoLastIteration(TaskEntity task) {
    if (!taskSchedulerCoreHelper.isActiveTaskReadyToStart(task)) {
      return;
    }
    createNewIteration(task, 0);
  }

  private void createNewIteration(TaskEntity task, long baseIterationid) {
    Optional<IterationInfo> iterationInfo =
        taskSchedulerCoreHelper.buildIterationInfo(
            ProtoParser.toProto(task.getInfo(), TaskInfo.getDefaultInstance()));

    if (iterationInfo.isEmpty()) {
      return;
    }

    IterationEntity newIteration =
        taskSchedulerCoreHelper.prepareNewIterationAndUploadDeviceCheckpointIfNeeded(
            task, baseIterationid, iterationInfo.get());

    try {
      taskDao.createIteration(newIteration);
    } catch (SpannerException exception) {
      if (exception.getErrorCode() == ErrorCode.ALREADY_EXISTS) {
        logger.atWarn().setCause(exception).log("{} already exists. ", task.getId());
      } else {
        throw exception;
      }
    }
  }

  private void processCompletedIteration(IterationEntity iterationEntity) {
    try {
      Instant startTime = instantSource.instant();
      Lock lock = lockRegistry.obtain(LOCK_COMPLETED_ITERATION_PREFIX + iterationEntity.getId());
      if (lock.tryLock()) {
        try {
          boolean upsertMetricsSuccess =
              taskSchedulerCoreHelper.parseMetricsAndUpsert(iterationEntity);
          if (upsertMetricsSuccess) {
            taskDao.updateIterationStatus(
                iterationEntity, iterationEntity.toBuilder().status(Status.POST_PROCESSED).build());
          }
        } finally {
          lock.unlock();
          Duration duration = Duration.between(startTime, instantSource.instant());
          logger.info(
              "processing completed iteration in {} second for iteration {}.",
              duration.getSeconds(),
              iterationEntity.getId().toString());
        }
      }
    } catch (Exception e) {
      // catch and log all exception for one task avoiding breaking other tasks' processing
      logger
          .atError()
          .setCause(e)
          .log("Failed to process completed iteration {}", iterationEntity.getId());
    }
  }
}
