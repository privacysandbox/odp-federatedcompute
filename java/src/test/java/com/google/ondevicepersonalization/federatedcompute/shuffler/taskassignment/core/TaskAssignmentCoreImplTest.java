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

import static com.google.common.truth.Truth.assertThat;
import static com.google.ondevicepersonalization.federatedcompute.shuffler.common.Constants.MIN_SEPARATION_POLICY_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.ondevicepersonalization.federatedcompute.proto.EligibilityPolicyEvalSpec;
import com.google.ondevicepersonalization.federatedcompute.proto.EligibilityTaskInfo;
import com.google.ondevicepersonalization.federatedcompute.proto.MinimumSeparationPolicy;
import com.google.ondevicepersonalization.federatedcompute.proto.TaskAssignment;
import com.google.ondevicepersonalization.federatedcompute.proto.UploadInstruction;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.UniqueIdGenerator;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AssignmentDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AssignmentEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AssignmentEntity.Status;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AssignmentId;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobDescription;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobManager;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.IterationEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskEntity;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(JUnit4.class)
public final class TaskAssignmentCoreImplTest {

  private static Instant NOW = Instant.parse("2023-09-01T00:00:00Z");
  private static final String DEFAULT_SESSION_ID = "session-123";
  private static final String DEFAULT_CORRELATION_ID = "correlation-123";
  private static final String DEFAULT_POPULATION_NAME = "us";
  private static final long DEFAULT_TASK_ID = 13;
  private static final String DEFAULT_CLIENT_VERSION = "1.2.3.4";
  private static final long DEFAULT_ITERATION_ID_LONG = 9;
  private static final String DEFAULT_ITERATION_ID = String.valueOf(DEFAULT_ITERATION_ID_LONG);
  private static final long MIN_SEPARATION = 1;

  private static final MinimumSeparationPolicy MINIMUM_SEPARATION_POLICY =
      MinimumSeparationPolicy.newBuilder()
          .setCurrentIndex(DEFAULT_ITERATION_ID_LONG)
          .setMinimumSeparation(MIN_SEPARATION)
          .build();
  private static final EligibilityTaskInfo DEFAULT_ELIGIBILITY_TASK_INFO =
      EligibilityTaskInfo.newBuilder()
          .addEligibilityPolicies(
              EligibilityPolicyEvalSpec.newBuilder()
                  .setId(MIN_SEPARATION_POLICY_ID)
                  .setMinSepPolicy(MINIMUM_SEPARATION_POLICY))
          .build();
  private static final AssignmentId DEFAULT_ASSIGNMENT_ID =
      AssignmentId.builder()
          .populationName(DEFAULT_POPULATION_NAME)
          .taskId(DEFAULT_TASK_ID)
          .iterationId(DEFAULT_ITERATION_ID_LONG)
          .attemptId(0)
          .assignmentId(DEFAULT_SESSION_ID)
          .build();

  private static final TaskEntity DEFAULT_TASK_ENTITY =
      TaskEntity.builder()
          .populationName(DEFAULT_POPULATION_NAME)
          .totalIteration(DEFAULT_TASK_ID)
          .minAggregationSize(333)
          .maxAggregationSize(444)
          .status(TaskEntity.Status.OPEN)
          .maxParallel(555)
          .correlationId("correlation")
          .minClientVersion(DEFAULT_CLIENT_VERSION)
          .maxClientVersion(DEFAULT_CLIENT_VERSION)
          .createdTime(NOW)
          .info("")
          .build();

  private static final IterationEntity DEFAULT_ITERATION_ENTITY =
      IterationEntity.builder()
          .populationName(DEFAULT_POPULATION_NAME)
          .taskId(DEFAULT_TASK_ID)
          .iterationId(DEFAULT_ITERATION_ID_LONG)
          .attemptId(0)
          .status(IterationEntity.Status.COLLECTING)
          .baseIterationId(DEFAULT_ITERATION_ID_LONG - 1)
          .baseOnResultId(DEFAULT_ITERATION_ID_LONG - 1)
          .reportGoal(3)
          .resultId(DEFAULT_ITERATION_ID_LONG - 1)
          .build();
  private static final AssignmentEntity DEFAULT_ASSIGNMENT_ENTITY =
      AssignmentEntity.builder()
          .populationName(DEFAULT_POPULATION_NAME)
          .taskId(DEFAULT_TASK_ID)
          .iterationId(DEFAULT_ITERATION_ID_LONG)
          .attemptId(0)
          .sessionId(DEFAULT_SESSION_ID)
          .correlationId(DEFAULT_CORRELATION_ID)
          .baseIterationId(8)
          .baseOnResultId(8)
          .resultId(9)
          .status(Status.ASSIGNED)
          .build();

  private static final AssignmentEntity DEFAULT_ASSIGNMENT_ENTITY_LOCAL_COMPLETED =
      DEFAULT_ASSIGNMENT_ENTITY.toBuilder().status(Status.LOCAL_COMPLETED).build();

  private static final AssignmentEntity DEFAULT_ASSIGNMENT_ENTITY_LOCAL_FAILED =
      DEFAULT_ASSIGNMENT_ENTITY.toBuilder().status(Status.LOCAL_FAILED).build();

  private static final TaskAssignment DEFAULT_TASK_ASSIGNMENT =
      TaskAssignment.newBuilder()
          .setPopulationName(DEFAULT_POPULATION_NAME)
          .setTaskId("13")
          .setAggregationId(DEFAULT_ITERATION_ID)
          .setAssignmentId(DEFAULT_SESSION_ID)
          .setTaskName("/population/us/task/13")
          .setSelfUri(
              "/population/us/task/13/aggregation/"
                  + DEFAULT_ITERATION_ID
                  + "/task-assignment/session-123")
          .build();

  private @Mock UniqueIdGenerator mockIdGenerator;
  private @Mock TaskDao mockTaskDao;
  private @Mock AssignmentDao mockAssignmentDao;
  private @Mock BlobManager mockBlobManager;
  private @Mock TaskAssignmentCoreHelper mockTaskAssignmentCoreHelper;

  TaskAssignmentCoreImpl taskAssignment;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    taskAssignment =
        new TaskAssignmentCoreImpl(
            mockTaskDao,
            mockAssignmentDao,
            mockIdGenerator,
            mockBlobManager,
            mockTaskAssignmentCoreHelper);
  }

  @Test
  public void testCreatTaskAssignment_Success() {
    // arrange
    when(mockTaskDao.getOpenTasksAndIterations(anyString(), anyString()))
        .thenReturn(ImmutableMap.of(DEFAULT_TASK_ENTITY, DEFAULT_ITERATION_ENTITY));
    when(mockIdGenerator.generate()).thenReturn(DEFAULT_SESSION_ID);
    when(mockAssignmentDao.createAssignment(any(), anyString(), anyString()))
        .thenReturn(Optional.of(DEFAULT_ASSIGNMENT_ENTITY));
    when(mockTaskAssignmentCoreHelper.selectTask(any()))
        .thenReturn(Optional.of(DEFAULT_TASK_ENTITY));
    when(mockTaskAssignmentCoreHelper.createTaskAssignment(any(), any()))
        .thenReturn(Optional.of(DEFAULT_TASK_ASSIGNMENT));

    // act
    Optional<TaskAssignment> result =
        taskAssignment.createTaskAssignment(
            DEFAULT_POPULATION_NAME, DEFAULT_CLIENT_VERSION, DEFAULT_CORRELATION_ID);

    // assert
    assertThat(result).isEqualTo(Optional.of(DEFAULT_TASK_ASSIGNMENT));
    verify(mockTaskDao, times(1))
        .getOpenTasksAndIterations(DEFAULT_POPULATION_NAME, DEFAULT_CLIENT_VERSION);
    verify(mockAssignmentDao, times(1))
        .createAssignment(DEFAULT_ITERATION_ENTITY, DEFAULT_CORRELATION_ID, DEFAULT_SESSION_ID);
    verify(mockIdGenerator, times(1)).generate();
    verify(mockTaskAssignmentCoreHelper, times(1))
        .createTaskAssignment(DEFAULT_TASK_ENTITY, DEFAULT_ASSIGNMENT_ENTITY);
  }

  @Test
  public void testCreatTaskAssignment_notOpenTaskAndIteration_returnEmpty() {
    // arrange
    when(mockIdGenerator.generate()).thenReturn(DEFAULT_SESSION_ID);
    when(mockTaskDao.getOpenTasksAndIterations(anyString(), anyString()))
        .thenReturn(ImmutableMap.of());

    // act
    Optional<TaskAssignment> result =
        taskAssignment.createTaskAssignment(
            DEFAULT_POPULATION_NAME, DEFAULT_CLIENT_VERSION, DEFAULT_CORRELATION_ID);

    // assert
    assertThat(result).isEqualTo(Optional.empty());
    verify(mockTaskDao, times(1))
        .getOpenTasksAndIterations(DEFAULT_POPULATION_NAME, DEFAULT_CLIENT_VERSION);
    verifyNoInteractions(mockAssignmentDao);
    verifyNoInteractions(mockIdGenerator);
  }

  @Test
  public void testGetUploadInstruction_Succeeded() {
    // arrange
    Map<String, String> headers = new HashMap<>();
    headers.put("x", "y");
    when(mockAssignmentDao.getAssignment(any()))
        .thenReturn(Optional.of(DEFAULT_ASSIGNMENT_ENTITY_LOCAL_COMPLETED));
    when(mockBlobManager.generateUploadGradientDescription(any()))
        .thenReturn(BlobDescription.builder().url("https://gradient").headers(headers).build());

    // act
    Optional<UploadInstruction> result =
        taskAssignment.getUploadInstruction(
            /* populationName= */ DEFAULT_POPULATION_NAME,
            /* taskId= */ DEFAULT_TASK_ID,
            /* aggregationId= */ DEFAULT_ITERATION_ID,
            /* assignmentId= */ DEFAULT_SESSION_ID);

    // assert
    assertThat(result)
        .isEqualTo(
            Optional.of(
                UploadInstruction.newBuilder()
                    .setUploadLocation("https://gradient")
                    .putAllExtraRequestHeaders(headers)
                    .build()));
    verify(mockAssignmentDao, times(1)).getAssignment(DEFAULT_ASSIGNMENT_ID);
    verify(mockBlobManager, times(1))
        .generateUploadGradientDescription(DEFAULT_ASSIGNMENT_ENTITY_LOCAL_COMPLETED);
  }

  @Test
  public void testGetUploadInstruction_AssignmentNotFound() {
    // arrange
    when(mockAssignmentDao.getAssignment(any())).thenReturn(Optional.empty());

    // act
    Optional<UploadInstruction> result =
        taskAssignment.getUploadInstruction(
            /* populationName= */ DEFAULT_POPULATION_NAME,
            /* taskId= */ DEFAULT_TASK_ID,
            /* aggregationId */ DEFAULT_ITERATION_ID,
            /* assignmentId= */ DEFAULT_SESSION_ID);

    // assert
    assertThat(result).isEqualTo(Optional.empty());
    verify(mockAssignmentDao, times(1)).getAssignment(DEFAULT_ASSIGNMENT_ID);
    verify(mockBlobManager, times(0)).generateUploadGradientDescription(any());
  }

  @Test
  public void testGetUploadInstruction_AssignmentNotFoundBecauseOfStatus() {
    // arrange
    when(mockAssignmentDao.getAssignment(any()))
        .thenReturn(Optional.of(DEFAULT_ASSIGNMENT_ENTITY_LOCAL_FAILED));

    // act
    Optional<UploadInstruction> result =
        taskAssignment.getUploadInstruction(
            /* populationName= */ DEFAULT_POPULATION_NAME,
            /* taskId= */ DEFAULT_TASK_ID,
            /* aggregationId */ DEFAULT_ITERATION_ID,
            /* assignmentId= */ DEFAULT_SESSION_ID);

    // assert
    assertThat(result).isEqualTo(Optional.empty());
    verify(mockAssignmentDao, times(1)).getAssignment(DEFAULT_ASSIGNMENT_ID);
    verify(mockBlobManager, times(0)).generateUploadGradientDescription(any());
  }

  @Test
  public void testReportLocalComplete_Succeeded() {
    // act
    taskAssignment.reportLocalCompleted(
        /* populationName= */ DEFAULT_POPULATION_NAME,
        /* taskId= */ DEFAULT_TASK_ID,
        /* aggregationId */ DEFAULT_ITERATION_ID,
        /* assignmentId= */ DEFAULT_SESSION_ID);

    // assert
    verify(mockAssignmentDao, times(1))
        .updateAssignmentStatus(
            /* assignmentId= */ DEFAULT_ASSIGNMENT_ID,
            /* from= */ Status.ASSIGNED,
            Status.LOCAL_COMPLETED);
  }

  @Test
  public void testReportLocalFailed_Succeeded() {
    // act
    taskAssignment.reportLocalFailed(
        /* populationName= */ DEFAULT_POPULATION_NAME,
        /* taskId= */ DEFAULT_TASK_ID,
        /* aggregationId */ DEFAULT_ITERATION_ID,
        /* assignmentId= */ DEFAULT_SESSION_ID);

    // assert
    verify(mockAssignmentDao, times(1))
        .updateAssignmentStatus(
            /* assignmentId= */ DEFAULT_ASSIGNMENT_ID,
            /* from= */ Status.ASSIGNED,
            Status.LOCAL_FAILED);
  }

  @Test
  public void testReportLocalNotEligible_Succeeded() {
    // act
    taskAssignment.reportLocalNotEligible(
        /* populationName= */ DEFAULT_POPULATION_NAME,
        /* taskId= */ DEFAULT_TASK_ID,
        /* aggregationId */ DEFAULT_ITERATION_ID,
        /* assignmentId= */ DEFAULT_SESSION_ID);

    // assert
    verify(mockAssignmentDao, times(1))
        .updateAssignmentStatus(
            /* assignmentId= */ DEFAULT_ASSIGNMENT_ID,
            /* from= */ Status.ASSIGNED,
            Status.LOCAL_NOT_ELIGIBLE);
  }
}
