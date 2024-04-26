// Copyright 2024 Google LLC
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

import static com.google.common.truth.Truth.assertThat;
import static com.google.ondevicepersonalization.federatedcompute.shuffler.taskassignment.core.TaskAssignmentCoreHelper.createEligibilityTaskInfo;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import com.google.internal.federatedcompute.v1.Resource;
import com.google.ondevicepersonalization.federatedcompute.proto.DataAvailabilityPolicy;
import com.google.ondevicepersonalization.federatedcompute.proto.EligibilityPolicyEvalSpec;
import com.google.ondevicepersonalization.federatedcompute.proto.EligibilityTaskInfo;
import com.google.ondevicepersonalization.federatedcompute.proto.MinimumSeparationPolicy;
import com.google.ondevicepersonalization.federatedcompute.proto.TaskAssignment;
import com.google.ondevicepersonalization.federatedcompute.proto.TaskInfo;
import com.google.ondevicepersonalization.federatedcompute.proto.TrainingInfo;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.RandomGenerator;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AssignmentEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AssignmentEntity.Status;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobDescription;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobManager;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskEntity;
import com.google.protobuf.util.JsonFormat;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(JUnit4.class)
public class TaskAssignmentCoreHelperTest {

  private static Instant NOW = Instant.parse("2023-09-01T00:00:00Z");
  private static final String DEFAULT_POPULATION_NAME = "us";
  private static final String DEFAULT_CLIENT_VERSION = "1.2.3.4";
  private static final long DEFAULT_ITERATION_ID_LONG = 9;
  private static final String DEFAULT_ITERATION_ID_STRING = "9";
  private static final long DEFAULT_TASK_ID_LONG = 13;
  private static final String DEFAULT_TASK_ID_STRING = "13";
  private static final String DEFAULT_SESSION_ID = "session-123";
  private static final String DEFAULT_CORRELATION_ID = "correlation-123";

  private static final TaskEntity DEFAULT_TASK_ENTITY =
      TaskEntity.builder()
          .populationName(DEFAULT_POPULATION_NAME)
          .totalIteration(DEFAULT_TASK_ID_LONG)
          .minAggregationSize(333)
          .maxAggregationSize(444)
          .status(TaskEntity.Status.OPEN)
          .maxParallel(555)
          .correlationId("correlation")
          .minClientVersion(DEFAULT_CLIENT_VERSION)
          .maxClientVersion(DEFAULT_CLIENT_VERSION)
          .info("{\n  \"trafficWeight\": \"1\"\n}")
          .createdTime(NOW)
          .build();

  private static final AssignmentEntity DEFAULT_ASSIGNMENT_ENTITY =
      AssignmentEntity.builder()
          .populationName(DEFAULT_POPULATION_NAME)
          .taskId(DEFAULT_TASK_ID_LONG)
          .iterationId(DEFAULT_ITERATION_ID_LONG)
          .attemptId(0)
          .sessionId(DEFAULT_SESSION_ID)
          .correlationId(DEFAULT_CORRELATION_ID)
          .baseIterationId(8)
          .baseOnResultId(8)
          .resultId(9)
          .status(Status.ASSIGNED)
          .build();

  private @Mock RandomGenerator randomGenerator;
  private @Mock BlobManager mockBlobManager;
  @InjectMocks TaskAssignmentCoreHelper taskAssignmentCoreHelper;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void createEligibilityTaskInfo_noTrainingInfo_returnEmpty() {
    // arrange
    TaskEntity taskEntity = DEFAULT_TASK_ENTITY.toBuilder().taskId(1).build();

    // act and assert
    assertThat(createEligibilityTaskInfo(DEFAULT_ITERATION_ID_LONG, taskEntity))
        .isEqualTo(EligibilityTaskInfo.getDefaultInstance());
  }

  @Test
  public void createEligibilityTaskInfo_hasTrainingTaskWithEligibility_returnExpectedEligibility()
      throws Exception {
    // arrange
    int minExampleCount = 10;
    int minSeparation = 1;
    int currentIterationId = 9;
    TaskInfo taskInfo =
        TaskInfo.newBuilder()
            .setTrainingInfo(
                TrainingInfo.newBuilder()
                    .setEligibilityTaskInfo(
                        EligibilityTaskInfo.newBuilder()
                            .addEligibilityPolicies(
                                EligibilityPolicyEvalSpec.newBuilder()
                                    .setMinSepPolicy(
                                        MinimumSeparationPolicy.newBuilder()
                                            .setMinimumSeparation(minSeparation)))
                            .addEligibilityPolicies(
                                EligibilityPolicyEvalSpec.newBuilder()
                                    .setDataAvailabilityPolicy(
                                        DataAvailabilityPolicy.newBuilder()
                                            .setMinExampleCount(minExampleCount)))))
            .setTrafficWeight(1)
            .build();
    String infoString = JsonFormat.printer().print(taskInfo);
    TaskEntity taskEntity = DEFAULT_TASK_ENTITY.toBuilder().info(infoString).taskId(1).build();

    // act and assert
    assertThat(createEligibilityTaskInfo(currentIterationId, taskEntity))
        .isEqualTo(
            EligibilityTaskInfo.newBuilder()
                .addEligibilityPolicies(
                    EligibilityPolicyEvalSpec.newBuilder()
                        .setMinSepPolicy(
                            MinimumSeparationPolicy.newBuilder()
                                .setMinimumSeparation(minSeparation)
                                .setCurrentIndex(currentIterationId)))
                .addEligibilityPolicies(
                    EligibilityPolicyEvalSpec.newBuilder()
                        .setDataAvailabilityPolicy(
                            DataAvailabilityPolicy.newBuilder()
                                .setMinExampleCount(minExampleCount)))
                .build());
  }

  @Test
  public void selectTask_hasEmptyEntity_returnEmpty() {
    // arrange
    Set<TaskEntity> taskEntitySet = ImmutableSet.of();

    // act and assert
    assertThat(taskAssignmentCoreHelper.selectTask(taskEntitySet)).isEqualTo(Optional.empty());
  }

  @Test
  public void selectTask_hasOneEntity_returnFirst() {
    // arrange
    Set<TaskEntity> taskEntitySet = ImmutableSet.of(DEFAULT_TASK_ENTITY);

    // act and assert
    assertThat(taskAssignmentCoreHelper.selectTask(taskEntitySet))
        .isEqualTo(Optional.of(DEFAULT_TASK_ENTITY));
  }

  @Test
  public void selectTask_hasMultipleEntities_returnValidOne() {
    // arrange
    TaskEntity taskEntity1 =
        DEFAULT_TASK_ENTITY.toBuilder().taskId(1).info("{\n  \"trafficWeight\": \"1\"\n}").build();
    TaskEntity taskEntity2 =
        DEFAULT_TASK_ENTITY.toBuilder().taskId(2).info("{\n  \"trafficWeight\": \"3\"\n}").build();

    Set<TaskEntity> taskEntitySet = ImmutableSet.of(taskEntity1, taskEntity2);

    // act and assert
    when(randomGenerator.nextLong(anyLong())).thenReturn(0L);
    assertThat(taskAssignmentCoreHelper.selectTask(taskEntitySet).get()).isEqualTo(taskEntity1);

    when(randomGenerator.nextLong(anyLong())).thenReturn(1L);
    assertThat(taskAssignmentCoreHelper.selectTask(taskEntitySet).get()).isEqualTo(taskEntity2);

    when(randomGenerator.nextLong(anyLong())).thenReturn(2L);
    assertThat(taskAssignmentCoreHelper.selectTask(taskEntitySet).get()).isEqualTo(taskEntity2);

    when(randomGenerator.nextLong(anyLong())).thenReturn(3L);
    assertThat(taskAssignmentCoreHelper.selectTask(taskEntitySet).get()).isEqualTo(taskEntity2);
    verify(randomGenerator, times(4)).nextLong(4L);
  }

  @Test
  public void selectTask_hasInvalidInfo_throwException() {
    // arrange
    TaskEntity taskEntity1 =
        DEFAULT_TASK_ENTITY.toBuilder().taskId(1).info("random_string").build();
    TaskEntity taskEntity2 =
        DEFAULT_TASK_ENTITY.toBuilder().taskId(2).info("random_string").build();

    Set<TaskEntity> taskEntitySet = ImmutableSet.of(taskEntity1, taskEntity2);

    // act and assert
    assertThrows(
        IllegalStateException.class, () -> taskAssignmentCoreHelper.selectTask(taskEntitySet));
  }

  @Test
  public void createTaskAssignment_base_returnAssignment() {
    // arrange
    when(mockBlobManager.generateDownloadCheckpointDescription(any(), any()))
        .thenReturn(BlobDescription.builder().url("https://checkkpoint").build());
    when(mockBlobManager.generateDownloadDevicePlanDescription(any()))
        .thenReturn(BlobDescription.builder().url("https://plan").build());

    // act and assert
    assertThat(
            taskAssignmentCoreHelper
                .createTaskAssignment(DEFAULT_TASK_ENTITY, DEFAULT_ASSIGNMENT_ENTITY)
                .get())
        .isEqualTo(
            TaskAssignment.newBuilder()
                .setPopulationName(DEFAULT_POPULATION_NAME)
                .setTaskId(DEFAULT_TASK_ID_STRING)
                .setAggregationId(DEFAULT_ITERATION_ID_STRING)
                .setAssignmentId(DEFAULT_SESSION_ID)
                .setTaskName("/population/us/task/13")
                .setSelfUri(
                    "/population/us/task/13/aggregation/"
                        + DEFAULT_ITERATION_ID_STRING
                        + "/task-assignment/session-123")
                .setInitCheckpoint(Resource.newBuilder().setUri("https://checkkpoint"))
                .setPlan(Resource.newBuilder().setUri("https://plan"))
                .setEligibilityTaskInfo(EligibilityTaskInfo.getDefaultInstance())
                .build());
  }
}
