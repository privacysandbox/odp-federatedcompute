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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.ondevicepersonalization.federatedcompute.proto.Task;
import com.google.ondevicepersonalization.federatedcompute.proto.TaskInfo;
import com.google.ondevicepersonalization.federatedcompute.proto.TaskStatus;
import com.google.ondevicepersonalization.federatedcompute.proto.TrainingInfo;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobDescription;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobManager;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskEntity;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import java.time.Instant;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(JUnit4.class)
public final class TaskManagementCoreImplTest {

  private static final Instant INST1 = Instant.parse("2023-07-21T11:19:42.12Z");
  private static final Instant INST2 = Instant.parse("2023-07-22T11:19:42.12Z");
  private static final Instant INST3 = Instant.parse("2023-07-21T11:30:42.12Z");
  private static final Instant INST4 = Instant.parse("2023-07-21T11:35:42.12Z");

  private static final Timestamp TS1 = Timestamps.parseUnchecked("2023-07-21T11:19:42.12Z");
  private static final Timestamp TS2 = Timestamps.parseUnchecked("2023-07-22T11:19:42.12Z");
  private static final Timestamp TS3 = Timestamps.parseUnchecked("2023-07-21T11:30:42.12Z");
  private static final Timestamp TS4 = Timestamps.parseUnchecked("2023-07-21T11:35:42.12Z");
  public static final String CLIENT_PLAN_URL_0 = "gs://bucket0/client_only_plan";
  public static final String CLIENT_PLAN_URL_1 = "gs://bucket1/client_only_plan";
  public static final String SERVER_PHASE_URL_0 = "gs://bucket0/server_phase";
  public static final String SERVER_PHASE_URL_1 = "gs://bucket1/server_phase";
  public static final String INIT_CHECKPOINT_URL_0 = "gs://bucket0/0/checkpoint";
  public static final String INIT_CHECKPOINT_URL_1 = "gs://bucket1/0/checkpoint";
  public static final String METRICS_URL_0 = "gs://bucket0/0/metrics";
  public static final String METRICS_URL_1 = "gs://bucket1/0/metrics";

  private static final TaskEntity DEFAULT_TASK_ENTITY_WITHOUT_INFO =
      TaskEntity.builder()
          .populationName("us")
          .taskId(15)
          .totalIteration(13)
          .minAggregationSize(100)
          .maxAggregationSize(200)
          .status(TaskEntity.Status.OPEN)
          .maxParallel(5)
          .correlationId("abc")
          .minClientVersion("0.0.0.0")
          .maxClientVersion("0.0.0.1")
          .startTaskNoEarlierThan(INST1)
          .doNotCreateIterationAfter(INST2)
          .startedTime(INST3)
          .stopTime(INST4)
          .build();
  private static final TaskEntity DEFAULT_TASK_ENTITY_WITH_INFO =
      DEFAULT_TASK_ENTITY_WITHOUT_INFO.toBuilder()
          .info("{\n  \"trafficWeight\": \"10\"\n}")
          .build();

  private static final Task DEFAULT_TASK_WITHOUT_INFO =
      Task.newBuilder()
          .setPopulationName("us")
          .setTaskId(15)
          .setTotalIteration(13)
          .setMinAggregationSize(100)
          .setMaxAggregationSize(200)
          .setStatus(TaskStatus.Enum.OPEN)
          .setMaxParallel(5)
          .setCorrelationId("abc")
          .setMinClientVersion("0.0.0.0")
          .setMaxClientVersion("0.0.0.1")
          .setStartTaskNoEarlierThan(TS1)
          .setDoNotCreateIterationAfter(TS2)
          .setStartedTime(TS3)
          .setStopTime(TS4)
          .addAllClientOnlyPlanUrl(ImmutableList.of(CLIENT_PLAN_URL_0, CLIENT_PLAN_URL_1))
          .addAllServerPhaseUrl(ImmutableList.of(SERVER_PHASE_URL_0, SERVER_PHASE_URL_1))
          .addAllInitCheckpointUrl(ImmutableList.of(INIT_CHECKPOINT_URL_0, INIT_CHECKPOINT_URL_1))
          .addAllMetricsUrl(ImmutableList.of(METRICS_URL_0, METRICS_URL_1))
          .build();
  private static final Task DEFAULT_TASK_WITH_INFO =
      DEFAULT_TASK_WITHOUT_INFO.toBuilder()
          .setInfo(TaskInfo.newBuilder().setTrafficWeight(10))
          .build();

  TaskManagementCoreImpl taskCore;
  @Mock TaskDao mockTaskDao;
  @Mock BlobManager mockBlobManager;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    taskCore = new TaskManagementCoreImpl(mockTaskDao, mockBlobManager);
    when(mockBlobManager.generateUploadDevicePlanDescriptions(any()))
        .thenReturn(
            new BlobDescription[] {
              BlobDescription.builder().url(CLIENT_PLAN_URL_0).build(),
              BlobDescription.builder().url(CLIENT_PLAN_URL_1).build()
            });
    when(mockBlobManager.generateUploadServerPlanDescription(any()))
        .thenReturn(
            new BlobDescription[] {
              BlobDescription.builder().url(SERVER_PHASE_URL_0).build(),
              BlobDescription.builder().url(SERVER_PHASE_URL_1).build()
            });
    when(mockBlobManager.generateUploadCheckpointDescriptions(any()))
        .thenReturn(
            new BlobDescription[] {
              BlobDescription.builder().url(INIT_CHECKPOINT_URL_0).build(),
              BlobDescription.builder().url(INIT_CHECKPOINT_URL_1).build()
            });
    when(mockBlobManager.generateUploadMetricsDescriptions(any()))
        .thenReturn(
            new BlobDescription[] {
              BlobDescription.builder().url(METRICS_URL_0).build(),
              BlobDescription.builder().url(METRICS_URL_1).build()
            });
  }

  @Test
  public void testGetTaskByIdSucceeded() {
    // arrange
    when(mockTaskDao.getTaskById(anyString(), anyLong()))
        .thenReturn(Optional.of(DEFAULT_TASK_ENTITY_WITH_INFO));

    // act
    Optional<Task> result = taskCore.getTaskById("us", 15);

    // assert
    assertThat(result.get()).isEqualTo(DEFAULT_TASK_WITH_INFO);
    verify(mockTaskDao, times(1)).getTaskById("us", 15);
  }

  @Test
  public void testGetTaskByIdFailed() {
    // arrange
    when(mockTaskDao.getTaskById(anyString(), anyLong())).thenReturn(Optional.empty());

    // act
    Optional<Task> result = taskCore.getTaskById("us", 1);

    // assert
    assertFalse(result.isPresent());
    verify(mockTaskDao, times(1)).getTaskById("us", 1);
  }

  @Test
  public void testCreateTask() {
    // arrange
    when(mockTaskDao.createTask(any()))
        .thenReturn(DEFAULT_TASK_ENTITY_WITH_INFO.toBuilder().taskId(789).build());

    // act
    Task result = taskCore.createTask(DEFAULT_TASK_WITH_INFO);

    // assert
    assertThat(result).isEqualTo(DEFAULT_TASK_WITH_INFO.toBuilder().setTaskId(789).build());
    verify(mockTaskDao, times(1))
        .createTask(DEFAULT_TASK_ENTITY_WITH_INFO.toBuilder().taskId(0).build());
  }

  @Test
  public void createTask_hasUnknownStatus_setStatusToCreated() {
    // arrange
    when(mockTaskDao.createTask(any()))
        .thenReturn(
            DEFAULT_TASK_ENTITY_WITH_INFO.toBuilder()
                .status(TaskEntity.Status.CREATED)
                .taskId(789)
                .build());

    // act
    Task result =
        taskCore.createTask(
            DEFAULT_TASK_WITH_INFO.toBuilder().setStatus(TaskStatus.Enum.UNKNOWN).build());

    // assert
    assertThat(result)
        .isEqualTo(
            DEFAULT_TASK_WITH_INFO.toBuilder()
                .setStatus(TaskStatus.Enum.CREATED)
                .setTaskId(789)
                .build());
    verify(mockTaskDao, times(1))
        .createTask(
            DEFAULT_TASK_ENTITY_WITH_INFO.toBuilder()
                .status(TaskEntity.Status.CREATED)
                .taskId(0)
                .build());
  }

  @Test
  public void createTask_withoutInfo_setInfoToDefault() {
    // arrange
    when(mockTaskDao.createTask(any()))
        .thenReturn(
            DEFAULT_TASK_ENTITY_WITHOUT_INFO.toBuilder()
                .info("{\n  \"trafficWeight\": \"1\",\n  \"trainingInfo\": {\n  }\n}")
                .taskId(789)
                .build());

    // act
    Task result = taskCore.createTask(DEFAULT_TASK_WITHOUT_INFO);

    // assert
    assertThat(result)
        .isEqualTo(
            DEFAULT_TASK_WITHOUT_INFO.toBuilder()
                .setInfo(
                    TaskInfo.newBuilder()
                        .setTrafficWeight(1)
                        .setTrainingInfo(TrainingInfo.getDefaultInstance()))
                .setTaskId(789)
                .build());

    verify(mockTaskDao, times(1))
        .createTask(
            DEFAULT_TASK_ENTITY_WITHOUT_INFO.toBuilder()
                .info("{\n  \"trafficWeight\": \"1\",\n  \"trainingInfo\": {\n  }\n}")
                .taskId(0)
                .build());
  }
}
