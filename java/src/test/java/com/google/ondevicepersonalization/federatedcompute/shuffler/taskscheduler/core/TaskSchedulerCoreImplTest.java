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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.common.collect.ImmutableList;
import com.google.ondevicepersonalization.federatedcompute.proto.IterationInfo;
import com.google.ondevicepersonalization.federatedcompute.proto.TaskInfo;
import com.google.ondevicepersonalization.federatedcompute.proto.TrainingInfo;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.CompressionUtils.CompressionFormat;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.ProtoParser;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobDescription;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobManager;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.IterationEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.IterationEntity.Status;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskEntities;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskEntity;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.google.testing.junit.testparameterinjector.TestParameters;
import java.io.IOException;
import java.time.Instant;
import java.time.InstantSource;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.springframework.integration.support.locks.LockRegistry;

@RunWith(TestParameterInjector.class)
public final class TaskSchedulerCoreImplTest {

  private static final TaskInfo DEFAULT_TASK_INFO =
      TaskInfo.newBuilder()
          .setTrafficWeight(1)
          .setTrainingInfo(TrainingInfo.getDefaultInstance())
          .build();
  private static final String DEFAULT_TASK_INFO_STRING =
      ProtoParser.toJsonString(DEFAULT_TASK_INFO);
  private static final IterationInfo DEFAULT_ITERATION_INFO =
      IterationInfo.newBuilder().setTaskInfo(DEFAULT_TASK_INFO).build();
  private static final String DEFAULT_ITERATION_INFO_STRING =
      ProtoParser.toJsonString(DEFAULT_ITERATION_INFO);
  private static final long FIRST_ITERATION_ID = 1;

  private static final TaskEntity DEFAULT_TASK =
      TaskEntity.builder()
          .populationName("us")
          .taskId(13)
          .totalIteration(3)
          .minAggregationSize(333)
          .maxAggregationSize(444)
          .status(TaskEntity.Status.OPEN)
          .maxParallel(555)
          .correlationId("correlation")
          .minClientVersion("0.0.0.0")
          .maxClientVersion("3.0.0.0")
          .info(DEFAULT_TASK_INFO_STRING)
          .build();

  private static final TaskEntity ZERO_ITERATION_TASK =
      TaskEntity.builder()
          .populationName("us")
          .taskId(13)
          .totalIteration(0)
          .minAggregationSize(333)
          .maxAggregationSize(444)
          .status(TaskEntity.Status.OPEN)
          .maxParallel(555)
          .correlationId("correlation")
          .minClientVersion("0.0.0.0")
          .maxClientVersion("3.0.0.0")
          .info(DEFAULT_TASK_INFO_STRING)
          .build();

  private static final TaskEntity TASK1 =
      TaskEntity.builder()
          .populationName("us")
          .taskId(35)
          .totalIteration(999)
          .minAggregationSize(3)
          .maxAggregationSize(5)
          .status(TaskEntity.Status.CREATED)
          .info(DEFAULT_TASK_INFO_STRING)
          .build();

  private static final TaskEntity TASK2 = TASK1.toBuilder().taskId(36).build();

  private static final IterationEntity BASE_ITERATION1 =
      TaskEntities.createBaseIteration(TASK1).toBuilder()
          .populationName("us")
          .taskId(35)
          .iterationId(0)
          .attemptId(0)
          .status(IterationEntity.Status.COLLECTING)
          .baseIterationId(0)
          .baseOnResultId(0)
          .reportGoal(3)
          .resultId(0)
          .info(DEFAULT_ITERATION_INFO_STRING)
          .build();
  private static final IterationEntity BASE_ITERATION2 =
      BASE_ITERATION1.toBuilder().taskId(36).build();

  private static final BlobDescription NEW_CLIENT_CHECKPOINT1_1 =
      BlobDescription.builder()
          .host("test-m-1")
          .resourceObject("us/35/17/d/0/client_checkpoint")
          .build();

  private static final BlobDescription NEW_METRICS1_1 =
      BlobDescription.builder().host("test-m-1").resourceObject("us/35/17/s/0/metrics").build();
  private static final BlobDescription NEW_CHECKPOINT1_1 =
      BlobDescription.builder().host("test-m-1").resourceObject("us/35/16/s/0/checkpoint").build();
  private static final List<CompressionFormat> COMPRESSION_FORMATS =
      Arrays.asList(CompressionFormat.GZIP);

  private static Instant NOW = Instant.parse("2023-09-01T00:00:00Z");
  private static InstantSource instanceSource = InstantSource.fixed(NOW);

  @Mock TaskDao mockTaskDao;
  @Mock BlobDao mockBlobDao;
  @Mock BlobManager mockBlobManager;
  @Mock TaskSchedulerCoreHelper mockTaskSchedulerCoreHelper;

  @Mock LockRegistry lockRegistry;

  @Mock Lock lock;
  TaskSchedulerCoreImpl taskScheduler;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    taskScheduler =
        new TaskSchedulerCoreImpl(
            mockTaskDao,
            mockBlobDao,
            mockBlobManager,
            instanceSource,
            mockTaskSchedulerCoreHelper,
            lockRegistry,
            COMPRESSION_FORMATS);

    // Init common mocks
    when(lockRegistry.obtain("taskscheduler_" + DEFAULT_TASK.getId().toString())).thenReturn(lock);
    when(lock.tryLock()).thenReturn(true);
    when(mockBlobManager.generateUploadMetricsDescriptions(any()))
        .thenReturn(new BlobDescription[] {NEW_METRICS1_1});
    when(mockBlobManager.generateUploadCheckpointDescriptions(any()))
        .thenReturn(new BlobDescription[] {NEW_CHECKPOINT1_1});
    when(mockBlobManager.generateUploadClientCheckpointDescriptions(any()))
        .thenReturn(new BlobDescription[] {NEW_CLIENT_CHECKPOINT1_1});
  }

  @Test
  public void process_noIteration_createFirstIteration() {
    // arrange
    when(mockTaskDao.getActiveTasks()).thenReturn(ImmutableList.of(DEFAULT_TASK));
    when(mockTaskDao.getLastIterationOfTask(anyString(), anyLong())).thenReturn(Optional.empty());
    when(mockTaskSchedulerCoreHelper.isActiveTaskReadyToStart(any())).thenReturn(true);
    when(mockTaskSchedulerCoreHelper.buildIterationInfo(any()))
        .thenReturn(Optional.of(DEFAULT_ITERATION_INFO));
    IterationEntity expectedEntity =
        IterationEntity.builder()
            .populationName("us")
            .taskId(13)
            .iterationId(FIRST_ITERATION_ID)
            .reportGoal(333)
            .status(IterationEntity.Status.COLLECTING)
            .baseIterationId(0)
            .baseOnResultId(0)
            .resultId(1)
            .info(DEFAULT_ITERATION_INFO_STRING)
            .build();
    when(mockTaskSchedulerCoreHelper.prepareNewIterationAndUploadDeviceCheckpointIfNeeded(
            DEFAULT_TASK, 0, DEFAULT_ITERATION_INFO))
        .thenReturn(expectedEntity);

    // act
    taskScheduler.processActiveTasks();

    // assert
    verify(mockTaskDao, times(1)).createIteration(expectedEntity);
    verify(lockRegistry, times(1)).obtain("taskscheduler_" + DEFAULT_TASK.getId().toString());
    verify(mockTaskDao, times(1)).getLastIterationOfTask("us", 13);
    verify(mockTaskSchedulerCoreHelper, times(1)).isActiveTaskReadyToStart(DEFAULT_TASK);
  }

  @Test
  public void process_noIterationInfo_notCreateNewIteration() {
    // arrange
    when(mockTaskDao.getActiveTasks()).thenReturn(ImmutableList.of(DEFAULT_TASK));
    when(mockTaskDao.getLastIterationOfTask(anyString(), anyLong())).thenReturn(Optional.empty());
    when(mockTaskSchedulerCoreHelper.isActiveTaskReadyToStart(any())).thenReturn(true);
    when(mockTaskSchedulerCoreHelper.buildIterationInfo(any())).thenReturn(Optional.empty());

    // act
    taskScheduler.processActiveTasks();

    // assert
    verify(mockTaskDao, times(1)).getLastIterationOfTask("us", 13);
    verify(mockTaskDao, never()).createIteration(any());
    verify(mockTaskSchedulerCoreHelper, times(1)).isActiveTaskReadyToStart(DEFAULT_TASK);
    verify(mockTaskSchedulerCoreHelper, times(1)).buildIterationInfo(any());
  }

  @Test
  public void processApplying_hasInsufficientCompletedIteration_createNextIteration() {
    // arrange
    IterationEntity iteration =
        IterationEntity.builder()
            .populationName("us")
            .taskId(13)
            .iterationId(1)
            .reportGoal(333)
            .status(Status.APPLYING)
            .baseIterationId(0)
            .baseOnResultId(0)
            .resultId(1)
            .build();
    when(mockTaskDao.getActiveTasks()).thenReturn(ImmutableList.of(DEFAULT_TASK));
    when(mockTaskSchedulerCoreHelper.isApplyingDone(iteration)).thenReturn(true);
    when(mockBlobDao.exists(any())).thenReturn(true);
    when(mockTaskDao.getLastIterationOfTask(anyString(), anyLong()))
        .thenReturn(Optional.of(iteration));
    when(mockTaskSchedulerCoreHelper.buildIterationInfo(any()))
        .thenReturn(Optional.of(DEFAULT_ITERATION_INFO));
    IterationEntity expectedEntity =
        IterationEntity.builder()
            .populationName("us")
            .taskId(13)
            .iterationId(2)
            .reportGoal(333)
            .status(IterationEntity.Status.COLLECTING)
            .baseIterationId(1)
            .baseOnResultId(1)
            .resultId(2)
            .info(DEFAULT_ITERATION_INFO_STRING)
            .build();
    when(mockTaskSchedulerCoreHelper.prepareNewIterationAndUploadDeviceCheckpointIfNeeded(
            DEFAULT_TASK, 1, DEFAULT_ITERATION_INFO))
        .thenReturn(expectedEntity);

    // act
    taskScheduler.processActiveTasks();

    // assert
    verify(mockTaskDao, times(1)).createIteration(expectedEntity);
    verify(mockTaskSchedulerCoreHelper, times(1)).isApplyingDone(iteration);
    verify(mockTaskDao, times(1))
        .updateIterationStatus(iteration, iteration.toBuilder().status(Status.COMPLETED).build());
    verify(mockTaskDao, times(1)).getLastIterationOfTask("us", 13);
  }

  @Test
  public void processCompleted_hasInsufficientCompletedIteration_createNextIteration() {
    // arrange
    IterationEntity iteration =
        IterationEntity.builder()
            .populationName("us")
            .taskId(13)
            .iterationId(1)
            .reportGoal(333)
            .status(Status.COMPLETED)
            .baseIterationId(0)
            .baseOnResultId(0)
            .resultId(1)
            .build();
    when(mockTaskDao.getActiveTasks()).thenReturn(ImmutableList.of(DEFAULT_TASK));
    when(mockTaskDao.getLastIterationOfTask(anyString(), anyLong()))
        .thenReturn(Optional.of(iteration));
    when(mockTaskSchedulerCoreHelper.buildIterationInfo(any()))
        .thenReturn(Optional.of(DEFAULT_ITERATION_INFO));
    IterationEntity expectedEntity =
        IterationEntity.builder()
            .populationName("us")
            .taskId(13)
            .iterationId(2)
            .reportGoal(333)
            .status(IterationEntity.Status.COLLECTING)
            .baseIterationId(1)
            .baseOnResultId(1)
            .resultId(2)
            .info(DEFAULT_ITERATION_INFO_STRING)
            .build();
    when(mockTaskSchedulerCoreHelper.prepareNewIterationAndUploadDeviceCheckpointIfNeeded(
            DEFAULT_TASK, 1, DEFAULT_ITERATION_INFO))
        .thenReturn(expectedEntity);

    // act
    taskScheduler.processActiveTasks();

    // assert
    verify(mockTaskDao, times(1)).createIteration(expectedEntity);
    verify(mockBlobDao, times(0)).exists(any());
    verify(mockTaskDao, times(0))
        .updateIterationStatus(iteration, iteration.toBuilder().status(Status.COMPLETED).build());
    verify(mockTaskDao, times(1)).getLastIterationOfTask("us", 13);
  }

  @Test
  public void processApplying_hasEnoughCompletedIteration_noCreation() {
    // arrange
    IterationEntity iteration =
        IterationEntity.builder()
            .populationName("us")
            .taskId(13)
            .iterationId(3)
            .reportGoal(333)
            .status(Status.APPLYING)
            .baseIterationId(0)
            .baseOnResultId(0)
            .resultId(1)
            .build();
    when(mockTaskDao.getActiveTasks()).thenReturn(ImmutableList.of(DEFAULT_TASK));
    when(mockTaskSchedulerCoreHelper.isApplyingDone(iteration)).thenReturn(true);
    when(mockTaskDao.getLastIterationOfTask(anyString(), anyLong()))
        .thenReturn(Optional.of(iteration));

    // act
    taskScheduler.processActiveTasks();

    // assert
    verify(mockTaskDao, times(1))
        .updateIterationStatus(iteration, iteration.toBuilder().status(Status.COMPLETED).build());
    verify(mockTaskDao, times(0)).createIteration(any());
    verify(mockTaskDao, times(1)).getLastIterationOfTask("us", 13);
  }

  @Test
  public void processApplying_longerThan5min_metricSent() {
    // arrange
    ListAppender<ILoggingEvent> listAppender = prepairListAppender();
    IterationEntity iteration =
        IterationEntity.builder()
            .populationName("us")
            .taskId(13)
            .iterationId(3)
            .reportGoal(333)
            .status(Status.APPLYING)
            .baseIterationId(0)
            .baseOnResultId(0)
            .resultId(1)
            .build();
    when(mockTaskDao.getActiveTasks()).thenReturn(ImmutableList.of(DEFAULT_TASK));
    when(mockTaskSchedulerCoreHelper.isApplyingDone(iteration)).thenReturn(false);
    when(mockTaskDao.getLastIterationOfTask(anyString(), anyLong()))
        .thenReturn(Optional.of(iteration));
    Optional<Instant> instant = Optional.of(Instant.parse("2023-08-31T23:54:00Z"));
    when(mockTaskDao.getIterationCreatedTime(iteration)).thenReturn(instant);

    // act
    taskScheduler.processActiveTasks();

    // assert
    verify(mockTaskDao, times(1)).getIterationCreatedTime(iteration);
    verify(mockTaskDao, times(0)).createIteration(any());
    verify(mockTaskDao, times(1)).getLastIterationOfTask("us", 13);
    List<ILoggingEvent> logsList = listAppender.list;
    assertThat(logsList.size()).isEqualTo(1);
    assertThat(logsList.get(0).getFormattedMessage().contains(
        "Iteration us/13/3/0 in APPLYING status for more than 5 min")).isTrue();
  }

  @Test
  public void processApplying_notLongerThan5min_metricNotSent() {
    // arrange
    ListAppender<ILoggingEvent> listAppender = prepairListAppender();
    IterationEntity iteration =
        IterationEntity.builder()
            .populationName("us")
            .taskId(13)
            .iterationId(3)
            .reportGoal(333)
            .status(Status.APPLYING)
            .baseIterationId(0)
            .baseOnResultId(0)
            .resultId(1)
            .build();
    when(mockTaskDao.getActiveTasks()).thenReturn(ImmutableList.of(DEFAULT_TASK));
    when(mockTaskSchedulerCoreHelper.isApplyingDone(iteration)).thenReturn(false);
    when(mockTaskDao.getLastIterationOfTask(anyString(), anyLong()))
        .thenReturn(Optional.of(iteration));
    Optional<Instant> instant = Optional.of(Instant.parse("2023-08-31T23:56:00Z"));
    when(mockTaskDao.getIterationCreatedTime(iteration)).thenReturn(instant);

    // act
    taskScheduler.processActiveTasks();

    // assert
    verify(mockTaskDao, times(1)).getIterationCreatedTime(iteration);
    verify(mockTaskDao, times(0)).createIteration(any());
    verify(mockTaskDao, times(1)).getLastIterationOfTask("us", 13);
    List<ILoggingEvent> logsList = listAppender.list;
    assertThat(logsList.size()).isEqualTo(0);
  }
  @Test
  public void process_hasEnoughCompletedIteration_noCreation() {
    // arrange
    IterationEntity iteration =
        IterationEntity.builder()
            .populationName("us")
            .taskId(13)
            .iterationId(3)
            .reportGoal(333)
            .status(Status.COMPLETED)
            .baseIterationId(0)
            .baseOnResultId(0)
            .resultId(1)
            .build();
    when(mockTaskDao.getActiveTasks()).thenReturn(ImmutableList.of(DEFAULT_TASK));
    when(mockTaskDao.getLastIterationOfTask(anyString(), anyLong()))
        .thenReturn(Optional.of(iteration));

    // act
    taskScheduler.processActiveTasks();

    // assert
    verify(mockBlobDao, times(0)).exists(any());
    verify(mockTaskDao, times(0))
        .updateIterationStatus(iteration, iteration.toBuilder().status(Status.COMPLETED).build());
    verify(mockTaskDao, times(0)).createIteration(any());
    verify(mockTaskDao, times(1)).getLastIterationOfTask("us", 13);
  }

  @Test
  public void process_hasExceptionInIterationCreation_swallowException() {
    // arrange
    when(mockTaskDao.getActiveTasks()).thenReturn(ImmutableList.of(DEFAULT_TASK));
    when(mockTaskDao.getLastIterationOfTask(anyString(), anyLong())).thenReturn(Optional.empty());
    when(mockTaskDao.createIteration(any()))
        .thenThrow(
            SpannerExceptionFactory.newSpannerException(
                ErrorCode.INVALID_ARGUMENT, "INVALID_ARGUMENT"));
    when(mockTaskSchedulerCoreHelper.isActiveTaskReadyToStart(any())).thenReturn(true);

    // act
    taskScheduler.processActiveTasks();

    // assert
    verify(mockTaskDao, times(1)).getLastIterationOfTask("us", 13);
    verify(mockTaskSchedulerCoreHelper, times(1)).isActiveTaskReadyToStart(DEFAULT_TASK);
  }

  @Test
  @TestParameters("{status: COLLECTING}")
  @TestParameters("{status: AGGREGATING}")
  @TestParameters("{status: APPLYING}")
  public void process_lastIterationInRunning_doNothing(IterationEntity.Status status) {
    // arrange
    when(mockBlobDao.exists(any())).thenReturn(false);
    when(mockTaskDao.getActiveTasks()).thenReturn(ImmutableList.of(DEFAULT_TASK));
    when(mockTaskDao.getLastIterationOfTask(anyString(), anyLong()))
        .thenReturn(
            Optional.of(
                IterationEntity.builder()
                    .populationName("us")
                    .taskId(13)
                    .iterationId(1)
                    .reportGoal(333)
                    .status(status)
                    .baseIterationId(0)
                    .baseOnResultId(0)
                    .resultId(1)
                    .build()));

    // act
    taskScheduler.processActiveTasks();

    // assert
    verify(mockTaskDao, times(0)).createIteration(any());
  }

  @Test
  @TestParameters("{status: AGGREGATING_FAILED}")
  @TestParameters("{status: APPLYING_FAILED}")
  public void process_lastIterationFailed_updateFailure(IterationEntity.Status status) {
    // arrange
    ListAppender<ILoggingEvent> listAppender = prepairListAppender();
    IterationEntity iteration = IterationEntity.builder()
        .populationName("us")
        .taskId(13)
        .iterationId(1)
        .reportGoal(333)
        .status(status)
        .baseIterationId(0)
        .baseOnResultId(0)
        .resultId(1)
        .build();
    when(mockTaskDao.getActiveTasks()).thenReturn(ImmutableList.of(DEFAULT_TASK));
    when(mockTaskDao.getLastIterationOfTask(anyString(), anyLong()))
        .thenReturn(Optional.of(iteration));

    // act
    taskScheduler.processActiveTasks();

    // assert
    verify(mockTaskDao, times(0)).createIteration(any());
    verify(mockTaskDao, times(1))
        .getLastIterationOfTask(DEFAULT_TASK.getPopulationName(), DEFAULT_TASK.getTaskId());
    verify(mockTaskDao, times(1))
        .updateTaskStatus(
            eq(DEFAULT_TASK.getId()), eq(DEFAULT_TASK.getStatus()), eq(TaskEntity.Status.FAILED));
    List<ILoggingEvent> logsList = listAppender.list;
    assertThat(logsList.size()).isEqualTo(1);
    assertThat(logsList.get(0).getFormattedMessage().contains(
        String.format("Iteration %s in %s status", iteration.getId(),
            status.name()))).isTrue();
  }

  @Test
  public void process_lastIterationStopped_doNothing() {
    // arrange
    when(mockTaskDao.getActiveTasks()).thenReturn(ImmutableList.of(DEFAULT_TASK));
    when(mockTaskDao.getLastIterationOfTask(anyString(), anyLong()))
        .thenReturn(
            Optional.of(
                IterationEntity.builder()
                    .populationName("us")
                    .taskId(13)
                    .iterationId(1)
                    .reportGoal(333)
                    .status(Status.STOPPED)
                    .baseIterationId(0)
                    .baseOnResultId(0)
                    .resultId(1)
                    .build()));

    // act
    taskScheduler.processActiveTasks();

    // assert
    verify(mockTaskDao, times(0)).createIteration(any());
  }

  @Test
  public void process_activeTaskNotReadyStart_noCreation() {
    // arrange
    when(mockTaskDao.getActiveTasks()).thenReturn(ImmutableList.of(ZERO_ITERATION_TASK));
    when(mockTaskDao.getLastIterationOfTask(anyString(), anyLong())).thenReturn(Optional.empty());
    when(mockTaskSchedulerCoreHelper.isActiveTaskReadyToStart(any())).thenReturn(false);

    // act
    taskScheduler.processActiveTasks();

    // assert
    verify(mockTaskDao, times(0)).createIteration(any());
    verify(mockTaskSchedulerCoreHelper, times(1)).isActiveTaskReadyToStart(ZERO_ITERATION_TASK);
  }

  @Test
  public void process_requiredFileNotUploaded_noCreation() {
    // arrange
    when(mockTaskDao.getActiveTasks()).thenReturn(ImmutableList.of(DEFAULT_TASK));
    when(mockTaskDao.getLastIterationOfTask(anyString(), anyLong())).thenReturn(Optional.empty());
    when(mockBlobDao.exists(any())).thenReturn(false);

    // act
    taskScheduler.processActiveTasks();

    // assert
    verify(mockTaskDao, times(0)).createIteration(any());
  }

  @Test
  public void process_multipleTasks_createFirstIterations() {
    // arrange
    when(lockRegistry.obtain(
            "taskscheduler_" + DEFAULT_TASK.toBuilder().taskId(14).build().getId().toString()))
        .thenReturn(lock);
    when(lock.tryLock()).thenReturn(true);
    when(mockTaskDao.getActiveTasks())
        .thenReturn(ImmutableList.of(DEFAULT_TASK, DEFAULT_TASK.toBuilder().taskId(14).build()));
    when(mockTaskDao.getLastIterationOfTask(anyString(), anyLong())).thenReturn(Optional.empty());
    when(mockTaskSchedulerCoreHelper.isActiveTaskReadyToStart(any())).thenReturn(true);
    when(mockTaskSchedulerCoreHelper.buildIterationInfo(any()))
        .thenReturn(Optional.of(DEFAULT_ITERATION_INFO));
    IterationEntity expectedIterationEntity1 =
        IterationEntity.builder()
            .populationName("us")
            .taskId(13)
            .iterationId(1)
            .reportGoal(333)
            .status(IterationEntity.Status.COLLECTING)
            .baseIterationId(0)
            .baseOnResultId(0)
            .resultId(1)
            .info(DEFAULT_ITERATION_INFO_STRING)
            .build();
    when(mockTaskSchedulerCoreHelper.prepareNewIterationAndUploadDeviceCheckpointIfNeeded(
            DEFAULT_TASK, 0, DEFAULT_ITERATION_INFO))
        .thenReturn(expectedIterationEntity1);
    IterationEntity expectedIterationEntity2 =
        IterationEntity.builder()
            .populationName("us")
            .taskId(13)
            .iterationId(1)
            .reportGoal(333)
            .status(IterationEntity.Status.COLLECTING)
            .baseIterationId(0)
            .baseOnResultId(0)
            .resultId(1)
            .info(DEFAULT_ITERATION_INFO_STRING)
            .build();
    when(mockTaskSchedulerCoreHelper.prepareNewIterationAndUploadDeviceCheckpointIfNeeded(
            DEFAULT_TASK, 0, DEFAULT_ITERATION_INFO))
        .thenReturn(expectedIterationEntity2);

    // act
    taskScheduler.processActiveTasks();

    // assert
    verify(mockTaskDao, times(1)).createIteration(expectedIterationEntity1);
    verify(mockTaskDao, times(1)).createIteration(expectedIterationEntity2);
    verify(mockTaskDao, times(1)).getLastIterationOfTask("us", 13);
    verify(mockTaskSchedulerCoreHelper, times(1)).isActiveTaskReadyToStart(DEFAULT_TASK);
  }

  @Test
  public void process_multipleTasks_hasIterationExists_handleAlreadyExistException() {
    // arrange
    when(lockRegistry.obtain(
            "taskscheduler_" + DEFAULT_TASK.toBuilder().taskId(14).build().getId().toString()))
        .thenReturn(lock);
    when(lock.tryLock()).thenReturn(true);
    TaskEntity newTask = DEFAULT_TASK.toBuilder().taskId(14).build();
    when(mockTaskDao.getActiveTasks()).thenReturn(ImmutableList.of(DEFAULT_TASK, newTask));
    when(mockTaskDao.getLastIterationOfTask(anyString(), anyLong())).thenReturn(Optional.empty());
    when(mockTaskSchedulerCoreHelper.isActiveTaskReadyToStart(any())).thenReturn(true);
    IterationEntity failedToCreateIteration =
        IterationEntity.builder()
            .populationName("us")
            .taskId(13)
            .iterationId(1)
            .reportGoal(333)
            .status(IterationEntity.Status.COLLECTING)
            .baseIterationId(0)
            .baseOnResultId(0)
            .resultId(1)
            .info(DEFAULT_ITERATION_INFO_STRING)
            .build();
    when(mockTaskSchedulerCoreHelper.prepareNewIterationAndUploadDeviceCheckpointIfNeeded(
            newTask, 0, DEFAULT_ITERATION_INFO))
        .thenReturn(failedToCreateIteration);
    when(mockTaskDao.createIteration(failedToCreateIteration))
        .thenThrow(
            SpannerExceptionFactory.newSpannerException(
                ErrorCode.ALREADY_EXISTS, "ALREADY_EXISTS"));
    when(mockTaskSchedulerCoreHelper.buildIterationInfo(any()))
        .thenReturn(Optional.of(DEFAULT_ITERATION_INFO));

    // act
    taskScheduler.processActiveTasks();

    // assert
    verify(mockTaskDao, times(1)).createIteration(failedToCreateIteration);
    verify(mockTaskDao, times(1)).getLastIterationOfTask("us", 13);
    verify(mockTaskSchedulerCoreHelper, times(1)).isActiveTaskReadyToStart(newTask);
  }

  @Test
  public void process_hasSpannerExceptionWhenGetActiveTasks_throw() {
    // arrange
    when(mockTaskDao.getActiveTasks())
        .thenThrow(
            SpannerExceptionFactory.newSpannerException(
                ErrorCode.INVALID_ARGUMENT, "INVALID_ARGUMENT"));
    // act
    SpannerException expected =
        assertThrows(SpannerException.class, () -> taskScheduler.processActiveTasks());

    // assert
    assertThat(expected).hasMessageThat().contains("INVALID_ARGUMENT");
    assertThat(expected.getErrorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT);
  }

  @Test
  public void process_multipleTasks_oneTaskExceptionWontImpactTheOther() {
    // arrange
    TaskEntity task1 = DEFAULT_TASK;
    TaskEntity task2 = DEFAULT_TASK.toBuilder().taskId(14).build();
    when(lockRegistry.obtain("taskscheduler_" + task1.getId().toString())).thenReturn(lock);
    when(lockRegistry.obtain("taskscheduler_" + task2.getId().toString())).thenReturn(lock);
    when(lock.tryLock()).thenReturn(true);
    when(mockTaskDao.getActiveTasks()).thenReturn(ImmutableList.of(task1, task2));
    when(mockTaskDao.getLastIterationOfTask(task1.getPopulationName(), task1.getTaskId()))
        .thenThrow(
            SpannerExceptionFactory.newSpannerException(
                ErrorCode.INVALID_ARGUMENT, "INVALID_ARGUMENT"));
    when(mockTaskDao.getLastIterationOfTask(task2.getPopulationName(), task2.getTaskId()))
        .thenReturn(Optional.empty());
    when(mockTaskSchedulerCoreHelper.isActiveTaskReadyToStart(any())).thenReturn(true);

    // act
    taskScheduler.processActiveTasks();

    // assert
    verify(mockTaskDao, times(1))
        .getLastIterationOfTask(task1.getPopulationName(), task1.getTaskId());
    verify(mockTaskDao, times(1))
        .getLastIterationOfTask(task2.getPopulationName(), task2.getTaskId());
    verify(mockTaskSchedulerCoreHelper, times(1)).isActiveTaskReadyToStart(task2);
    verify(mockTaskSchedulerCoreHelper, times(1)).buildIterationInfo(any());
    verifyNoMoreInteractions(mockTaskSchedulerCoreHelper);
  }

  @Test
  public void process_multipleTasks_requiredFileNotUploaded_noCreationOfFirstIteration() {
    // arrange
    TaskEntity task1 = DEFAULT_TASK;
    TaskEntity task2 = DEFAULT_TASK.toBuilder().taskId(14).build();
    when(lockRegistry.obtain("taskscheduler_" + task1.getId().toString())).thenReturn(lock);
    when(lockRegistry.obtain("taskscheduler_" + task2.getId().toString())).thenReturn(lock);
    when(lock.tryLock()).thenReturn(true);
    when(mockTaskDao.getActiveTasks()).thenReturn(ImmutableList.of(task1, task2));
    when(mockTaskDao.getLastIterationOfTask(anyString(), anyLong())).thenReturn(Optional.empty());
    when(mockBlobDao.exists(any())).thenReturn(false);

    // act
    taskScheduler.processActiveTasks();

    // assert
    verify(mockTaskDao, times(0)).createIteration(any());
  }

  @Test
  public void processApplying_lastIterationComplete_markTaskComplete() {
    // arrange
    IterationEntity iteration =
        IterationEntity.builder()
            .populationName("us")
            .taskId(13)
            .iterationId(3)
            .reportGoal(333)
            .status(IterationEntity.Status.APPLYING)
            .baseIterationId(0)
            .baseOnResultId(0)
            .resultId(1)
            .build();
    when(mockTaskDao.getActiveTasks()).thenReturn(ImmutableList.of(DEFAULT_TASK));
    when(mockTaskDao.getLastIterationOfTask(anyString(), anyLong()))
        .thenReturn(Optional.of(iteration));
    when(mockTaskSchedulerCoreHelper.isApplyingDone(iteration)).thenReturn(true);

    // act
    taskScheduler.processActiveTasks();

    // assert
    verify(mockTaskSchedulerCoreHelper, times(1)).isApplyingDone(iteration);
    verify(mockTaskDao, times(0)).createIteration(any());
    verify(mockTaskDao, times(1))
        .getLastIterationOfTask(DEFAULT_TASK.getPopulationName(), DEFAULT_TASK.getTaskId());
    verify(mockTaskDao, times(1))
        .updateTaskStatus(
            eq(DEFAULT_TASK.getId()),
            eq(DEFAULT_TASK.getStatus()),
            eq(TaskEntity.Status.COMPLETED));
  }

  @Test
  public void processCompleted_lastIterationComplete_markTaskComplete() {
    // arrange
    when(mockTaskDao.getActiveTasks()).thenReturn(ImmutableList.of(DEFAULT_TASK));
    when(mockTaskDao.getLastIterationOfTask(anyString(), anyLong()))
        .thenReturn(
            Optional.of(
                IterationEntity.builder()
                    .populationName("us")
                    .taskId(13)
                    .iterationId(3)
                    .reportGoal(333)
                    .status(Status.COMPLETED)
                    .baseIterationId(0)
                    .baseOnResultId(0)
                    .resultId(1)
                    .build()));

    // act
    taskScheduler.processActiveTasks();

    // assert
    verify(mockBlobDao, times(0)).exists(any());
    verify(mockTaskDao, times(0)).createIteration(any());
    verify(mockTaskDao, times(1))
        .getLastIterationOfTask(DEFAULT_TASK.getPopulationName(), DEFAULT_TASK.getTaskId());
    verify(mockTaskDao, times(1))
        .updateTaskStatus(
            eq(DEFAULT_TASK.getId()),
            eq(DEFAULT_TASK.getStatus()),
            eq(TaskEntity.Status.COMPLETED));
  }

  @Test
  public void processCreateTask_basicCase_updateTaskStatus() throws IOException {
    // arange
    when(lockRegistry.obtain("taskscheduler_" + TASK1.getId().toString())).thenReturn(lock);
    when(lockRegistry.obtain("taskscheduler_" + TASK2.getId().toString())).thenReturn(lock);
    when(lock.tryLock()).thenReturn(true);
    when(mockTaskDao.getCreatedTasks()).thenReturn(ImmutableList.of(TASK1, TASK2));
    doNothing()
        .when(mockTaskSchedulerCoreHelper)
        .generateAndUploadDeviceCheckpoint(BASE_ITERATION1);
    doNothing()
        .when(mockTaskSchedulerCoreHelper)
        .generateAndUploadDeviceCheckpoint(BASE_ITERATION2);

    // act
    taskScheduler.processCreatedTasks();

    // assert
    verify(lockRegistry, times(1)).obtain("taskscheduler_" + TASK1.getId().toString());
    verify(lockRegistry, times(1)).obtain("taskscheduler_" + TASK2.getId().toString());
    verify(mockTaskSchedulerCoreHelper, times(1))
        .generateAndUploadDeviceCheckpoint(BASE_ITERATION1);
    verify(mockTaskSchedulerCoreHelper, times(1))
        .generateAndUploadDeviceCheckpoint(BASE_ITERATION2);
    verify(mockTaskDao, times(1))
        .updateTaskStatus(TASK1.getId(), TaskEntity.Status.CREATED, TaskEntity.Status.OPEN);
    verify(mockTaskDao, times(1))
        .updateTaskStatus(TASK2.getId(), TaskEntity.Status.CREATED, TaskEntity.Status.OPEN);
    verify(lock, times(2)).unlock();
  }

  @Test
  public void processCreateTask_deviceCheckpointNotReady_noUpdateTaskStatus() throws IOException {
    // arange
    when(lockRegistry.obtain("taskscheduler_" + TASK1.getId().toString())).thenReturn(lock);
    when(lock.tryLock()).thenReturn(true);
    when(mockTaskDao.getCreatedTasks()).thenReturn(ImmutableList.of(TASK1));
    doThrow(new IllegalStateException("some error"))
        .when(mockTaskSchedulerCoreHelper)
        .generateAndUploadDeviceCheckpoint(BASE_ITERATION1);

    // act
    taskScheduler.processCreatedTasks();

    // assert
    verify(mockTaskDao, times(0)).updateTaskStatus(any(), any(), any());
    verify(lock, times(1)).unlock();
  }

  @Test
  public void testProcess_TryLockFailed() {
    when(lockRegistry.obtain("taskscheduler_" + TASK1.getId().toString())).thenReturn(lock);
    when(lock.tryLock()).thenReturn(false);
    when(mockTaskDao.getCreatedTasks()).thenReturn(ImmutableList.of(TASK1));
    when(mockTaskDao.getActiveTasks()).thenReturn(ImmutableList.of(TASK1));

    taskScheduler.processCreatedTasks();
    taskScheduler.processActiveTasks();

    verify(mockTaskDao, times(0)).updateTaskStatus(any(), any(), any());
    verify(mockTaskDao, times(0)).createIteration(any());
  }

  @Test
  public void processCompletedIteration_upsertMetricsSuccessful_updateIterationStatus() {
    when(lockRegistry.obtain("completed_iteration_" + BASE_ITERATION1.getId())).thenReturn(lock);
    when(lock.tryLock()).thenReturn(true);
    when(mockTaskDao.getIterationsOfStatus(any())).thenReturn(ImmutableList.of(BASE_ITERATION1));
    when(mockTaskSchedulerCoreHelper.parseMetricsAndUpsert(BASE_ITERATION1)).thenReturn(true);

    taskScheduler.processCompletedIterations();

    verify(mockTaskDao, times(1)).updateIterationStatus(any(), any());
    verify(lock, times(1)).unlock();
  }

  @Test
  public void processCompletedIteration_tryLockFailed_doNothing() {
    when(lockRegistry.obtain("completed_iteration_" + BASE_ITERATION1.getId())).thenReturn(lock);
    when(lock.tryLock()).thenReturn(false);
    when(mockTaskDao.getIterationsOfStatus(any())).thenReturn(ImmutableList.of(BASE_ITERATION1));

    taskScheduler.processCompletedIterations();

    verify(mockTaskDao, times(0)).updateIterationStatus(any(), any());
    verify(mockTaskSchedulerCoreHelper, times(0)).parseMetricsAndUpsert(any());
    verify(lock, times(0)).unlock();
  }

  @Test
  public void processCompletedIteration_upsertMetricsFailed_doNothing() {
    when(lockRegistry.obtain("completed_iteration_" + BASE_ITERATION1.getId())).thenReturn(lock);
    when(lock.tryLock()).thenReturn(true);
    when(mockTaskDao.getIterationsOfStatus(any())).thenReturn(ImmutableList.of(BASE_ITERATION1));
    when(mockTaskSchedulerCoreHelper.parseMetricsAndUpsert(BASE_ITERATION1)).thenReturn(false);

    taskScheduler.processCompletedIterations();

    verify(mockTaskDao, times(0)).updateIterationStatus(any(), any());
    verify(lock, times(1)).unlock();
  }

  private ListAppender<ILoggingEvent> prepairListAppender() {
    Logger logger = (Logger) LoggerFactory.getLogger(TaskSchedulerCoreImpl.class);

    // create and start a ListAppender
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();

    // add the appender to the logger
    logger.addAppender(listAppender);
    return listAppender;
  }
}
