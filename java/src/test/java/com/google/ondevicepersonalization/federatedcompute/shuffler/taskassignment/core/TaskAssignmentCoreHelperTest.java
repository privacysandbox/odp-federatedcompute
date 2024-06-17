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

import com.google.common.collect.ImmutableList;
import com.google.internal.federatedcompute.v1.Resource;
import com.google.internal.federatedcompute.v1.ResourceCompressionFormat;
import com.google.ondevicepersonalization.federatedcompute.proto.DataAvailabilityPolicy;
import com.google.ondevicepersonalization.federatedcompute.proto.EligibilityPolicyEvalSpec;
import com.google.ondevicepersonalization.federatedcompute.proto.EligibilityTaskInfo;
import com.google.ondevicepersonalization.federatedcompute.proto.IterationInfo;
import com.google.ondevicepersonalization.federatedcompute.proto.MinimumSeparationPolicy;
import com.google.ondevicepersonalization.federatedcompute.proto.TaskAssignment;
import com.google.ondevicepersonalization.federatedcompute.proto.TaskInfo;
import com.google.ondevicepersonalization.federatedcompute.proto.TrainingInfo;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.CompressionUtils.CompressionFormat;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.ProtoParser;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.RandomGenerator;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AssignmentEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AssignmentEntity.Status;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobDescription;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobManager;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.IterationEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
  private static final long DEFAULT_ITERATION_ID_LONG = 9;
  private static final String DEFAULT_ITERATION_ID_STRING = "9";
  private static final long DEFAULT_TASK_ID_LONG = 13;
  private static final String DEFAULT_TASK_ID_STRING = "13";
  private static final String DEFAULT_SESSION_ID = "session-123";
  private static final String DEFAULT_CORRELATION_ID = "correlation-123";

  private static final TaskInfo DEFAULT_TASK_INFO =
      TaskInfo.newBuilder().setTrafficWeight(1).build();

  private static final IterationInfo DEFAULT_ITERATION_INFO =
      IterationInfo.newBuilder().setTaskInfo(DEFAULT_TASK_INFO).build();

  private static final IterationEntity DEFAULT_ITERATION_ENTITY =
      IterationEntity.builder()
          .populationName(DEFAULT_POPULATION_NAME)
          .taskId(DEFAULT_TASK_ID_LONG)
          .iterationId(DEFAULT_ITERATION_ID_LONG)
          .attemptId(0)
          .status(IterationEntity.Status.COLLECTING)
          .baseIterationId(DEFAULT_ITERATION_ID_LONG - 1)
          .baseOnResultId(DEFAULT_ITERATION_ID_LONG - 1)
          .reportGoal(3)
          .resultId(DEFAULT_ITERATION_ID_LONG - 1)
          .info(ProtoParser.toJsonString(DEFAULT_ITERATION_INFO))
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
  public void createEligibilityTaskInfo_noTrainingInfo_returnDefault() {
    // act and assert
    assertThat(createEligibilityTaskInfo(DEFAULT_ITERATION_ID_LONG, DEFAULT_TASK_INFO))
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
        DEFAULT_TASK_INFO.toBuilder()
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
            .build();

    // act and assert
    assertThat(createEligibilityTaskInfo(currentIterationId, taskInfo))
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
  public void selectTaskInfo_hasEmptyEntity_returnEmpty() {
    // arrange
    List<IterationEntity> iterationEntityList = ImmutableList.of();

    // act and assert
    assertThat(taskAssignmentCoreHelper.selectIterationEntity(iterationEntityList))
        .isEqualTo(Optional.empty());
  }

  @Test
  public void selectTaskInfo_hasOneEntity_returnFirst() {
    // arrange
    List<IterationEntity> iterationEntityList = ImmutableList.of(DEFAULT_ITERATION_ENTITY);

    // act and assert
    assertThat(taskAssignmentCoreHelper.selectIterationEntity(iterationEntityList))
        .isEqualTo(Optional.of(DEFAULT_ITERATION_ENTITY));
  }

  @Test
  public void selectTaskInfo_hasMultipleEntities_returnValidOne() {
    // arrange
    TaskInfo taskInfo1 = TaskInfo.newBuilder().setTrafficWeight(1).build();
    IterationInfo iterationInfo1 = IterationInfo.newBuilder().setTaskInfo(taskInfo1).build();
    IterationEntity iterationEntity1 =
        DEFAULT_ITERATION_ENTITY.toBuilder()
            .taskId(1)
            .info(ProtoParser.toJsonString(iterationInfo1))
            .build();
    TaskInfo taskInfo2 = TaskInfo.newBuilder().setTrafficWeight(3).build();
    IterationInfo iterationInfo2 = IterationInfo.newBuilder().setTaskInfo(taskInfo2).build();
    IterationEntity iterationEntity2 =
        DEFAULT_ITERATION_ENTITY.toBuilder()
            .taskId(2)
            .info(ProtoParser.toJsonString(iterationInfo2))
            .build();

    List<IterationEntity> iterationEntityList =
        ImmutableList.of(iterationEntity1, iterationEntity2);

    // act and assert
    when(randomGenerator.nextLong(anyLong())).thenReturn(0L);
    assertThat(taskAssignmentCoreHelper.selectIterationEntity(iterationEntityList).get())
        .isEqualTo(iterationEntity1);

    when(randomGenerator.nextLong(anyLong())).thenReturn(1L);
    assertThat(taskAssignmentCoreHelper.selectIterationEntity(iterationEntityList).get())
        .isEqualTo(iterationEntity2);

    when(randomGenerator.nextLong(anyLong())).thenReturn(2L);
    assertThat(taskAssignmentCoreHelper.selectIterationEntity(iterationEntityList).get())
        .isEqualTo(iterationEntity2);

    when(randomGenerator.nextLong(anyLong())).thenReturn(3L);
    assertThat(taskAssignmentCoreHelper.selectIterationEntity(iterationEntityList).get())
        .isEqualTo(iterationEntity2);
    verify(randomGenerator, times(4)).nextLong(4L);
  }

  @Test
  public void selectTaskInfo_hasInvalidInfo_throwException() {
    // arrange
    IterationEntity iterationEntity1 =
        DEFAULT_ITERATION_ENTITY.toBuilder().taskId(1).info("random_string").build();
    IterationEntity iterationEntity2 =
        DEFAULT_ITERATION_ENTITY.toBuilder().taskId(2).info("random_string").build();

    List<IterationEntity> iterationEntityList =
        ImmutableList.of(iterationEntity1, iterationEntity2);

    // act and assert
    assertThrows(
        IllegalStateException.class,
        () -> taskAssignmentCoreHelper.selectIterationEntity(iterationEntityList));
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
                .createTaskAssignment(
                    DEFAULT_ITERATION_ENTITY, DEFAULT_ASSIGNMENT_ENTITY, CompressionFormat.NONE)
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

    assertThat(
            taskAssignmentCoreHelper
                .createTaskAssignment(
                    DEFAULT_ITERATION_ENTITY, DEFAULT_ASSIGNMENT_ENTITY, CompressionFormat.GZIP)
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
                .setInitCheckpoint(
                    Resource.newBuilder()
                        .setUri("https://checkkpoint")
                        .setCompressionFormat(
                            ResourceCompressionFormat.RESOURCE_COMPRESSION_FORMAT_GZIP))
                .setPlan(
                    Resource.newBuilder()
                        .setUri("https://plan")
                        .setCompressionFormat(
                            ResourceCompressionFormat.RESOURCE_COMPRESSION_FORMAT_GZIP))
                .setEligibilityTaskInfo(EligibilityTaskInfo.getDefaultInstance())
                .build());
  }
}
