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

package com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.gcp;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.cloud.Timestamp;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.TransactionContext;
import com.google.cloud.spanner.Value;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AssignmentEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.CheckInResult;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.IterationEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.IterationId;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskId;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.InstantSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(TestParameterInjector.class)
public final class TaskSpannerDaoTest {

  private static final String PROJECT_ID = "spanner-emulator";
  private static final String INSTANCE = "fcp-task-unittest";
  private static final String DB_NAME = "test-task-dao";
  private static final String SDL_FILE_PATH = "shuffler/spanner/task_database.sdl";

  private static SpannerTestHarness.Connection spannerEmulatorConnection;

  // GMT time.
  private static Instant NOW = Instant.parse("2023-09-01T00:00:00Z");
  private static Timestamp TS_NOW = Timestamp.ofTimeSecondsAndNanos(1693526400, 0);
  private static InstantSource instanceSource = InstantSource.fixed(NOW);
  private DatabaseClient dbClient;
  private TaskSpannerDao dao;
  private static final long ITERATION_COLLECTING = 0;
  private static final long ITERATION_AGGREGATING = 1;
  private static final long ITERATION_APPLYING = 4;
  private static final long TASK_OPEN = 0;
  private static final long TASK_CREATED = 2;
  private static final long TASK_COMPLETED = 1;
  private static final String MIN_CLIENT_VERSION = "00000000";
  private static final String MAX_CLIENT_VERSION = "99999999";
  private static final long MIN_AGGREGATION_SIZE = 1;
  private static final long MAX_AGGREGATION_SIZE = 2;
  private static final long ASSIGNED = AssignmentEntity.Status.ASSIGNED.code();
  private static final long LOCAL_FAILED = AssignmentEntity.Status.LOCAL_FAILED.code();

  private static final String TASK_INFO = "{\"trafficWeight\":\"1\",\"trainingInfo\":{}}";
  private static final String ITERATION_INFO =
      "{\"taskInfo\":{\"trafficWeight\":\"1\",\"trainingInfo\":{}}}";

  private static final TaskEntity DEFAULT_TASK =
      TaskEntity.builder()
          .populationName("us")
          .totalIteration(222)
          .minAggregationSize(MIN_AGGREGATION_SIZE)
          .maxAggregationSize(MAX_AGGREGATION_SIZE)
          .status(TaskEntity.Status.OPEN)
          .maxParallel(555)
          .correlationId("correlation")
          .minClientVersion(MIN_CLIENT_VERSION)
          .maxClientVersion(MAX_CLIENT_VERSION)
          .createdTime(NOW)
          .info(TASK_INFO)
          .build();

  public static final IterationEntity DEFAULT_ITERATION =
      IterationEntity.builder()
          .populationName("us")
          .taskId(111)
          .iterationId(1)
          .attemptId(0)
          .status(IterationEntity.Status.fromCode(0))
          .baseIterationId(0)
          .baseOnResultId(0)
          .reportGoal(333)
          .resultId(1)
          .info(ITERATION_INFO)
          .aggregationLevel(0)
          .maxAggregationSize(MAX_AGGREGATION_SIZE)
          .minClientVersion(MIN_CLIENT_VERSION)
          .maxClientVersion(MAX_CLIENT_VERSION)
          .build();

  private void deleteDatabase() throws SQLException {
    spannerEmulatorConnection.dropDatabase();
  }

  private void initDbClient() throws SQLException {
    spannerEmulatorConnection.createDatabase();
    this.dbClient = spannerEmulatorConnection.getDatabaseClient();
  }

  @BeforeClass
  public static void setup() throws SQLException, IOException {
    spannerEmulatorConnection =
        SpannerTestHarness.useSpannerEmulatorWithCustomInputs(
            PROJECT_ID, INSTANCE, DB_NAME, SDL_FILE_PATH);
  }

  @AfterClass
  public static void cleanup() throws SQLException {
    spannerEmulatorConnection.stop();
  }

  @Before
  public void initializeDatabase() throws SQLException {
    initDbClient();
    this.dao = new TaskSpannerDao(dbClient, instanceSource);
  }

  @After
  public void teardown() throws SQLException {
    deleteDatabase();
  }

  // See command in the bug to prepare the running env.
  @Test
  public void testGetTaskById_Succeeded() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "gb",
                  /* taskId= */ 111,
                  /* status= */ 1,
                  /* insertStatusHist= */ true);
              return null;
            });

    // act
    Optional<TaskEntity> result = dao.getTaskById("gb", 111);

    // assert
    assertThat(result)
        .isEqualTo(
            Optional.of(
                TaskEntity.builder()
                    .populationName("gb")
                    .taskId(111)
                    .totalIteration(222)
                    .minAggregationSize(MIN_AGGREGATION_SIZE)
                    .maxAggregationSize(MAX_AGGREGATION_SIZE)
                    .status(TaskEntity.Status.COMPLETED)
                    .maxParallel(555)
                    .correlationId("correlation")
                    .minClientVersion(MIN_CLIENT_VERSION)
                    .maxClientVersion(MAX_CLIENT_VERSION)
                    .createdTime(NOW)
                    .info(TASK_INFO)
                    .build()));
  }

  @Test
  public void testGetTaskById_NotFound() {
    // act
    Optional<TaskEntity> result = dao.getTaskById("gb", 111);

    // assert
    assertThat(result).isEqualTo(Optional.empty());
  }

  @Test
  public void testCreateNewTaskToEmptyDB_Succeeded() {
    // arrange
    TaskEntity task = DEFAULT_TASK.toBuilder().populationName("cn").build();

    // act
    TaskEntity createdTask = dao.createTask(task);

    // assert
    assertThat(createdTask).isEqualTo(task.toBuilder().taskId(1).build());
    assertThat(queryTaskStatusHistories(createdTask.getId())).isEqualTo(Arrays.asList(TASK_OPEN));
  }

  @Test
  public void testCreateNewTaskFindMaxIdInPopulation_Succeeded() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "gb",
                  /* taskId= */ 111,
                  /* status= */ 1,
                  /* insertStatusHist= */ true);
              return null;
            });
    TaskEntity task = DEFAULT_TASK.toBuilder().populationName("gb").build();

    // act
    TaskEntity createdTask = dao.createTask(task);

    // assert
    assertThat(createdTask).isEqualTo(task.toBuilder().taskId(112).build());
    assertThat(queryTaskStatusHistories(createdTask.getId())).isEqualTo(Arrays.asList(TASK_OPEN));
  }

  @Test
  public void testCreateNewTaskHasActiveTaskWithSameType_Failed() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "gb",
                  /* taskId= */ 111,
                  /* status= */ 2,
                  /* insertStatusHist= */ true);
              return null;
            });
    TaskEntity task = DEFAULT_TASK.toBuilder().populationName("gb").build();

    // act
    SpannerException exception = assertThrows(SpannerException.class, () -> dao.createTask(task));

    // assert
    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FAILED_PRECONDITION);
    assertThat(exception)
        .hasMessageThat()
        .contains("only one active training and/or one evaluation task is allowed");
  }

  @Test
  public void testCreateNewTaskHasActiveTaskWithDifferentType_Succeeded() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "gb",
                  /* taskId= */ 111,
                  /* status= */ 1,
                  /* insertStatusHist= */ true);
              return null;
            });
    TaskEntity task =
        DEFAULT_TASK.toBuilder()
            .populationName("us")
            .info("{\"evaluationInfo\":{},\"trafficWeight\":\"1\"}")
            .build();

    // act
    TaskEntity createdTask = dao.createTask(task);

    // assert
    assertThat(createdTask).isEqualTo(task.toBuilder().taskId(1).build());
    assertThat(queryTaskStatusHistories(createdTask.getId())).isEqualTo(Arrays.asList(TASK_OPEN));
  }

  @Test
  public void testCreateNewTaskToDifferentPopulation_Succeeded() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "gb",
                  /* taskId= */ 111,
                  /* status= */ 1,
                  /* insertStatusHist= */ true);
              return null;
            });
    TaskEntity task = DEFAULT_TASK.toBuilder().populationName("us").build();

    // act
    TaskEntity createdTask = dao.createTask(task);

    // assert
    assertThat(createdTask).isEqualTo(task.toBuilder().taskId(1).build());
    assertThat(queryTaskStatusHistories(createdTask.getId())).isEqualTo(Arrays.asList(TASK_OPEN));
  }

  @Test
  public void testGetActiveTasks_Succeeded() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "zz",
                  /* taskId= */ 111,
                  /* status= */ 0,
                  /* insertStatusHist= */ true);
              insertTask(
                  transaction,
                  "yy",
                  /* taskId= */ 112,
                  /* status= */ 1,
                  /* insertStatusHist= */ true);
              insertTask(
                  transaction,
                  "xx",
                  /* taskId= */ 113,
                  /* status= */ 0,
                  /* insertStatusHist= */ true);
              return null;
            });

    // act
    List<TaskEntity> tasks = dao.getActiveTasks();

    // assert
    assertThat(tasks)
        .isEqualTo(
            ImmutableList.of(
                DEFAULT_TASK.toBuilder().populationName("xx").taskId(113).build(),
                DEFAULT_TASK.toBuilder().populationName("zz").taskId(111).build()));
  }

  @Test
  public void testGetActiveTasks_NoActiveTask() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "zz",
                  /* taskId= */ 111,
                  /* status= */ 1,
                  /* insertStatusHist= */ true);
              insertTask(
                  transaction,
                  "yy",
                  /* taskId= */ 112,
                  /* status= */ 1,
                  /* insertStatusHist= */ true);
              insertTask(
                  transaction,
                  "xx",
                  /* taskId= */ 113,
                  /* status= */ 1,
                  /* insertStatusHist= */ true);
              return null;
            });

    // act
    List<TaskEntity> tasks = dao.getActiveTasks();

    // assert
    assertThat(tasks).isEqualTo(ImmutableList.of());
  }

  @Test
  public void testGetActiveTasks_NoTask() {
    // act
    List<TaskEntity> tasks = dao.getActiveTasks();

    // assert
    assertThat(tasks).isEqualTo(ImmutableList.of());
  }

  @Test
  public void testGetCreatedTasks_Succeeded() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "zz",
                  /* taskId= */ 111,
                  /* status= */ TASK_CREATED,
                  /* insertStatusHist= */ true);
              insertTask(
                  transaction,
                  "yy",
                  /* taskId= */ 112,
                  /* status= */ 0,
                  /* insertStatusHist= */ true);
              insertTask(
                  transaction,
                  "xx",
                  /* taskId= */ 113,
                  /* status= */ TASK_CREATED,
                  /* insertStatusHist= */ true);
              return null;
            });

    // act
    List<TaskEntity> tasks = dao.getCreatedTasks();

    // assert
    assertThat(tasks)
        .isEqualTo(
            ImmutableList.of(
                DEFAULT_TASK.toBuilder()
                    .status(TaskEntity.Status.CREATED)
                    .populationName("xx")
                    .taskId(113)
                    .build(),
                DEFAULT_TASK.toBuilder()
                    .status(TaskEntity.Status.CREATED)
                    .populationName("zz")
                    .taskId(111)
                    .build()));
  }

  @Test
  public void testGetLastIteration_Succeeded() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= */ 0,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= */ 50,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 10,
                  /* status= */ 0,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ true);
              return null;
            });

    // act
    Optional<IterationEntity> iteration = dao.getLastIterationOfTask("us", 111);

    // assert
    assertThat(iteration)
        .isEqualTo(
            Optional.of(
                IterationEntity.builder()
                    .populationName("us")
                    .taskId(111)
                    .iterationId(10)
                    .attemptId(0)
                    .reportGoal(300)
                    .status(IterationEntity.Status.fromCode(0))
                    .baseIterationId(9)
                    .baseOnResultId(9)
                    .resultId(10)
                    .info(ITERATION_INFO)
                    .aggregationLevel(0)
                    .maxAggregationSize(301)
                    .minClientVersion(MIN_CLIENT_VERSION)
                    .maxClientVersion(MAX_CLIENT_VERSION)
                    .build()));
  }

  @Test
  public void testIterationById_Succeeded() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= */ 0,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= */ 50,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 10,
                  /* status= */ 0,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ true);
              return null;
            });

    // act
    IterationId iterationId =
        IterationId.builder().iterationId(10).taskId(111).attemptId(0).populationName("us").build();
    Optional<IterationEntity> iteration = dao.getIterationById(iterationId);

    // assert
    assertThat(iteration)
        .isEqualTo(
            Optional.of(
                IterationEntity.builder()
                    .populationName("us")
                    .taskId(111)
                    .iterationId(10)
                    .attemptId(0)
                    .reportGoal(300)
                    .status(IterationEntity.Status.fromCode(0))
                    .baseIterationId(9)
                    .baseOnResultId(9)
                    .resultId(10)
                    .info(ITERATION_INFO)
                    .aggregationLevel(0)
                    .maxAggregationSize(301)
                    .minClientVersion(MIN_CLIENT_VERSION)
                    .maxClientVersion(MAX_CLIENT_VERSION)
                    .build()));
  }

  @Test
  public void testGetLastIterationRespectTaskId_Succeeded() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= */ 0,
                  /* insertStatusHist= */ true);
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 112,
                  /* status= */ 0,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= */ 50,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 112,
                  /* iterationId= */ 10,
                  /* status= */ 0,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ true);
              return null;
            });

    // act
    Optional<IterationEntity> iteration = dao.getLastIterationOfTask("us", 111);

    // assert
    assertThat(iteration)
        .isEqualTo(
            Optional.of(
                IterationEntity.builder()
                    .populationName("us")
                    .taskId(111)
                    .iterationId(9)
                    .attemptId(0)
                    .reportGoal(300)
                    .status(IterationEntity.Status.fromCode(50))
                    .baseIterationId(8)
                    .baseOnResultId(8)
                    .resultId(9)
                    .info(ITERATION_INFO)
                    .aggregationLevel(0)
                    .maxAggregationSize(301)
                    .minClientVersion(MIN_CLIENT_VERSION)
                    .maxClientVersion(MAX_CLIENT_VERSION)
                    .build()));
  }

  @Test
  public void testGetLastIteration_NoIterationOnSelectedTask() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= */ 0,
                  /* insertStatusHist= */ true);
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 112,
                  /* status= */ 0,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= */ 50,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ true);
              return null;
            });

    // act
    Optional<IterationEntity> iteration = dao.getLastIterationOfTask("us", 112);

    // assert
    assertThat(iteration).isEqualTo(Optional.empty());
  }

  @Test
  public void testGetLastIteration_NoTask() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= */ 0,
                  /* insertStatusHist= */ true);
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 112,
                  /* status= */ 0,
                  /* insertStatusHist= */ true);
              return null;
            });

    // act
    Optional<IterationEntity> iteration = dao.getLastIterationOfTask("us", 113);

    // assert
    assertThat(iteration).isEqualTo(Optional.empty());
  }

  @Test
  public void testCreateAndUpdateIteration_Succeeded() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= */ 0,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 1,
                  /* status= */ 0,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ true);
              return null;
            });
    IterationEntity toCreated =
        DEFAULT_ITERATION.toBuilder()
            .iterationId(1)
            .attemptId(2)
            .status(IterationEntity.Status.fromCode(ITERATION_APPLYING))
            .baseIterationId(3)
            .baseOnResultId(4)
            .reportGoal(333)
            .resultId(5)
            .aggregationLevel(0)
            .build();
    IterationEntity iter =
        DEFAULT_ITERATION.toBuilder()
            .populationName("us")
            .taskId(111)
            .iterationId(1)
            .attemptId(0)
            .build();
    // act
    boolean updated =
        dao.createAndUpdateIteration(
            toCreated, iter, iter.toBuilder().status(IterationEntity.Status.AGGREGATING).build());

    // assert
    assertThat(updated).isTrue();
    assertThat(queryIterationById(toCreated.getId()).get().getStatus())
        .isEqualTo(toCreated.getStatus());
    assertThat(queryIterationStatusHistories(toCreated.getId()))
        .isEqualTo(Arrays.asList(ITERATION_APPLYING));

    // assert
    assertThat(queryIterationById(iter.getId()).get().getStatus())
        .isEqualTo(IterationEntity.Status.AGGREGATING);
    assertThat(queryIterationStatusHistories(iter.getId()))
        .isEqualTo(Arrays.asList(ITERATION_COLLECTING, ITERATION_AGGREGATING));
  }

  @Test
  public void testCreateAndUpdateIteration_Failed() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= */ 0,
                  /* insertStatusHist= */ true);
              return null;
            });
    IterationEntity toCreated =
        DEFAULT_ITERATION.toBuilder()
            .iterationId(1)
            .attemptId(2)
            .status(IterationEntity.Status.fromCode(ITERATION_APPLYING))
            .baseIterationId(3)
            .baseOnResultId(4)
            .reportGoal(333)
            .resultId(5)
            .aggregationLevel(0)
            .build();
    IterationEntity iter =
        DEFAULT_ITERATION.toBuilder()
            .populationName("us")
            .taskId(111)
            .iterationId(1)
            .attemptId(0)
            .build();
    // act
    boolean updated =
        dao.createAndUpdateIteration(
            toCreated, iter, iter.toBuilder().status(IterationEntity.Status.AGGREGATING).build());

    // assert
    assertThat(updated).isFalse();
    assertThat(queryIterationById(toCreated.getId()).isEmpty());
  }

  @Test
  public void testCreateIteration_Succeeded() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= */ 0,
                  /* insertStatusHist= */ true);
              return null;
            });
    IterationEntity toCreated =
        DEFAULT_ITERATION.toBuilder()
            .iterationId(1)
            .attemptId(2)
            .status(IterationEntity.Status.fromCode(ITERATION_APPLYING))
            .baseIterationId(3)
            .baseOnResultId(4)
            .reportGoal(333)
            .resultId(5)
            .aggregationLevel(0)
            .build();

    // act
    IterationEntity createdIteration = dao.createIteration(toCreated);

    // assert
    assertThat(createdIteration).isEqualTo(toCreated);
    assertThat(queryIterationStatusHistories(createdIteration.getId()))
        .isEqualTo(Arrays.asList(ITERATION_APPLYING));
  }

  @Test
  public void testCreateIteration_DuplicateId() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= */ 0,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 1,
                  /* status= */ 0,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ true);
              return null;
            });

    // act
    SpannerException expected =
        assertThrows(SpannerException.class, () -> dao.createIteration(DEFAULT_ITERATION));

    // assert
    assertThat(expected).hasMessageThat().contains("ALREADY_EXISTS");
    assertThat(expected.getErrorCode()).isEqualTo(ErrorCode.ALREADY_EXISTS);
  }

  @Test
  public void testUpdateIterationStatus_Succeeded() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= */ 0,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 1,
                  /* status= */ 0,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ true);
              return null;
            });

    // act
    IterationEntity iter =
        DEFAULT_ITERATION.toBuilder()
            .populationName("us")
            .taskId(111)
            .iterationId(1)
            .attemptId(0)
            .build();
    boolean updated =
        dao.updateIterationStatus(
            iter, iter.toBuilder().status(IterationEntity.Status.AGGREGATING).build());

    // assert
    assertThat(updated).isTrue();
    assertThat(queryIterationById(iter.getId()).get().getStatus())
        .isEqualTo(IterationEntity.Status.AGGREGATING);
    assertThat(queryIterationStatusHistories(iter.getId()))
        .isEqualTo(Arrays.asList(ITERATION_COLLECTING, ITERATION_AGGREGATING));
  }

  @Test
  public void testUpdateIterationStatus_AggregationLevelSucceeded() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= */ 0,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 1,
                  /* status= */ 0,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ true);
              return null;
            });

    // act
    IterationEntity iter =
        DEFAULT_ITERATION.toBuilder()
            .populationName("us")
            .taskId(111)
            .iterationId(1)
            .attemptId(0)
            .build();
    boolean updated = dao.updateIterationStatus(iter, iter.toBuilder().aggregationLevel(1).build());

    // assert
    assertThat(updated).isTrue();
    assertThat(queryIterationById(iter.getId()).get().getAggregationLevel()).isEqualTo(1);
    assertThat(queryIterationStatusHistories(iter.getId()))
        .isEqualTo(Arrays.asList(ITERATION_COLLECTING, ITERATION_COLLECTING));
  }

  @Test
  public void testUpdateIterationStatus_AggregationLevelMismatch() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= */ 0,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 1,
                  /* status= */ 0,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ true);
              return null;
            });

    // act
    IterationEntity iter =
        DEFAULT_ITERATION.toBuilder()
            .populationName("us")
            .taskId(111)
            .iterationId(1)
            .attemptId(0)
            .build();
    boolean updated =
        dao.updateIterationStatus(
            iter.toBuilder().aggregationLevel(1).build(),
            iter.toBuilder().aggregationLevel(1).build());

    // assert
    assertThat(updated).isFalse();
    assertThat(queryIterationById(iter.getId()).get().getAggregationLevel()).isEqualTo(0);
    assertThat(queryIterationStatusHistories(iter.getId()))
        .isEqualTo(Arrays.asList(ITERATION_COLLECTING));
  }

  @Test
  public void testUpdateIterationStatus_IterationNotFound() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= */ 0,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 2,
                  /* status= */ 0,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ true);
              return null;
            });

    // act
    // act
    IterationEntity iter =
        DEFAULT_ITERATION.toBuilder()
            .populationName("us")
            .taskId(111)
            .iterationId(1)
            .attemptId(0)
            .build();
    boolean updated =
        dao.updateIterationStatus(
            iter, iter.toBuilder().status(IterationEntity.Status.AGGREGATING).build());

    // assert
    assertThat(updated).isFalse();
    assertThat(
            queryIterationById(
                    IterationId.builder()
                        .populationName("us")
                        .taskId(111)
                        .iterationId(2)
                        .attemptId(0)
                        .build())
                .get()
                .getStatus())
        .isEqualTo(IterationEntity.Status.COLLECTING);
  }

  @Test
  public void testUpdateIterationStatus_FromStatusNotMatch() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= */ 0,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 1,
                  /* status= */ 0,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ true);
              return null;
            });

    // act
    IterationEntity iter =
        DEFAULT_ITERATION.toBuilder()
            .populationName("us")
            .taskId(111)
            .iterationId(1)
            .attemptId(0)
            .status(IterationEntity.Status.AGGREGATING)
            .build();
    boolean updated =
        dao.updateIterationStatus(
            iter, iter.toBuilder().status(IterationEntity.Status.APPLYING).build());

    // assert
    assertThat(updated).isFalse();
    assertThat(
            queryIterationById(
                    IterationId.builder()
                        .populationName("us")
                        .taskId(111)
                        .iterationId(1)
                        .attemptId(0)
                        .build())
                .get()
                .getStatus())
        .isEqualTo(IterationEntity.Status.COLLECTING);
  }

  @Test
  public void testUpdateIterationStatus_FromAggLvlNotMatch() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= */ 0,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 1,
                  /* status= */ 0,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ true);
              return null;
            });

    // act
    IterationEntity iter =
        DEFAULT_ITERATION.toBuilder()
            .populationName("us")
            .taskId(111)
            .iterationId(1)
            .attemptId(0)
            .aggregationLevel(10)
            .build();
    boolean updated =
        dao.updateIterationStatus(iter, iter.toBuilder().aggregationLevel(11).build());

    // assert
    assertThat(updated).isFalse();
    assertThat(
            queryIterationById(
                    IterationId.builder()
                        .populationName("us")
                        .taskId(111)
                        .iterationId(1)
                        .attemptId(0)
                        .build())
                .get()
                .getAggregationLevel())
        .isEqualTo(0);
  }

  private enum MisMatchFrom {
    COLLECTING(IterationEntity.Status.COLLECTING),
    APPLYING(IterationEntity.Status.APPLYING);

    final IterationEntity.Status code;

    MisMatchFrom(IterationEntity.Status code) {
      this.code = code;
    }
  }

  @Test
  public void testUpdateIterationStatus_NoUpdateIfStatusHistoryMismatch(
      @TestParameter MisMatchFrom from) {
    // Not expecting this situation in real world. Just for testing purpose.
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= */ 0,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 1,
                  /* status= */ ITERATION_COLLECTING,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ false);

              insertIterationStatusHist(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 1,
                  /* statusId= */ 0,
                  /* status= */ ITERATION_APPLYING,
                  /* createdTime= */ TS_NOW,
                  /* aggregationLevel= */ 0);
              return null;
            });

    // act
    IterationEntity iter =
        DEFAULT_ITERATION.toBuilder()
            .populationName("us")
            .taskId(111)
            .iterationId(1)
            .attemptId(0)
            .build();
    boolean updated =
        dao.updateIterationStatus(
            iter, iter.toBuilder().status(IterationEntity.Status.AGGREGATING).build());

    // assert
    assertThat(updated).isFalse();
    assertThat(queryIterationById(iter.getId()).get().getStatus())
        .isEqualTo(IterationEntity.Status.COLLECTING);
    assertThat(queryIterationStatusHistories(iter.getId()))
        .isEqualTo(Arrays.asList(ITERATION_APPLYING));
  }

  @Test
  public void testUpdateIterationStatus_NoUpdateIfFromStatusIsNotTopStatus() {
    // Not expecting this situation in real world. Just for testing purpose.
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= */ 0,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 1,
                  /* status= */ ITERATION_COLLECTING,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ false);

              insertIterationStatusHist(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 1,
                  /* statusId= */ 0,
                  /* status= */ ITERATION_COLLECTING,
                  /* createdTime= */ TS_NOW,
                  /* aggregationLevel= */ 0);

              insertIterationStatusHist(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 1,
                  /* statusId= */ 1,
                  /* status= */ ITERATION_APPLYING,
                  /* createdTime= */ TS_NOW,
                  /* aggregationLevel= */ 0);
              return null;
            });

    // act
    IterationEntity iter =
        DEFAULT_ITERATION.toBuilder()
            .populationName("us")
            .taskId(111)
            .iterationId(1)
            .attemptId(0)
            .build();
    boolean updated =
        dao.updateIterationStatus(
            iter, iter.toBuilder().status(IterationEntity.Status.AGGREGATING).build());

    // assert
    assertThat(updated).isFalse();
    assertThat(queryIterationById(iter.getId()).get().getStatus())
        .isEqualTo(IterationEntity.Status.COLLECTING);
    assertThat(queryIterationStatusHistories(iter.getId()))
        .isEqualTo(Arrays.asList(ITERATION_COLLECTING, ITERATION_APPLYING));
  }

  @Test
  public void testUpdateIterationStatus_NoUpdateIfNoHistoryStatus(
      @TestParameter MisMatchFrom from) {
    // Not expecting this situation in real world. Just for testing purpose.
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= */ 0,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 1,
                  /* status= */ ITERATION_COLLECTING,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ false);

              return null;
            });

    // act
    IterationEntity iter =
        DEFAULT_ITERATION.toBuilder()
            .populationName("us")
            .taskId(111)
            .iterationId(1)
            .attemptId(0)
            .build();
    boolean updated =
        dao.updateIterationStatus(
            iter, iter.toBuilder().status(IterationEntity.Status.AGGREGATING).build());

    // assert
    assertThat(updated).isFalse();
    assertThat(queryIterationById(iter.getId()).get().getStatus())
        .isEqualTo(IterationEntity.Status.COLLECTING);
    assertThat(queryIterationStatusHistories(iter.getId())).isEqualTo(Arrays.asList());
  }

  @Test
  public void testUpdateTasktatus_Succeeded() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= */ 0,
                  /* insertStatusHist= */ true);
              return null;
            });

    // act
    boolean updated =
        dao.updateTaskStatus(
            TaskId.builder().populationName("us").taskId(111).build(),
            TaskEntity.Status.OPEN,
            TaskEntity.Status.COMPLETED);

    // assert
    TaskId taskId = TaskId.builder().populationName("us").taskId(111).build();
    assertThat(updated).isTrue();
    assertThat(queryTaskById(taskId).get().getStatus()).isEqualTo(TaskEntity.Status.COMPLETED);
    assertThat(queryTaskStatusHistories(taskId))
        .isEqualTo(Arrays.asList(TASK_OPEN, TASK_COMPLETED));
  }

  @Test
  public void testUpdateTasktatus_NotFound() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= */ 0,
                  /* insertStatusHist= */ true);
              return null;
            });

    // act
    boolean updated =
        dao.updateTaskStatus(
            TaskId.builder().populationName("us").taskId(112).build(),
            TaskEntity.Status.OPEN,
            TaskEntity.Status.COMPLETED);

    // assert
    TaskId taskId = TaskId.builder().populationName("us").taskId(111).build();
    assertThat(updated).isFalse();
    assertThat(queryTaskById(taskId).get().getStatus()).isEqualTo(TaskEntity.Status.OPEN);
    assertThat(queryTaskStatusHistories(taskId)).isEqualTo(Arrays.asList(TASK_OPEN));
  }

  @Test
  public void testUpdateTasktatus_StatusMisMatch() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= */ 0,
                  /* insertStatusHist= */ true);
              return null;
            });

    // act
    boolean updated =
        dao.updateTaskStatus(
            TaskId.builder().populationName("us").taskId(111).build(),
            TaskEntity.Status.CANCELED,
            TaskEntity.Status.COMPLETED);

    // assert
    TaskId taskId = TaskId.builder().populationName("us").taskId(111).build();
    assertThat(updated).isFalse();
    assertThat(queryTaskById(taskId).get().getStatus()).isEqualTo(TaskEntity.Status.OPEN);
    assertThat(queryTaskStatusHistories(taskId)).isEqualTo(Arrays.asList(TASK_OPEN));
  }

  private enum TaskMisMatchFrom {
    COLLECTING(TaskEntity.Status.OPEN),
    APPLYING(TaskEntity.Status.CREATED);

    final TaskEntity.Status code;

    TaskMisMatchFrom(TaskEntity.Status code) {
      this.code = code;
    }
  }

  @Test
  public void testUpdateTasktatus_NoUpdateIfStatusNotMatchHistory(
      @TestParameter TaskMisMatchFrom from) {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= */ TASK_OPEN,
                  /* insertStatusHist= */ false);
              insertTaskStatusHist(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* statusId= */ 0,
                  /* status= */ TASK_CREATED,
                  TS_NOW);
              return null;
            });

    // act
    TaskId taskId = TaskId.builder().populationName("us").taskId(111).build();
    boolean updated = dao.updateTaskStatus(taskId, from.code, TaskEntity.Status.COMPLETED);

    // assert
    assertThat(updated).isFalse();
    assertThat(queryTaskById(taskId).get().getStatus()).isEqualTo(TaskEntity.Status.OPEN);
    assertThat(queryTaskStatusHistories(taskId)).isEqualTo(Arrays.asList(TASK_CREATED));
  }

  @Test
  public void testUpdateTasktatus_NoUpdateIfNoHistory(@TestParameter TaskMisMatchFrom from) {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= */ TASK_OPEN,
                  /* insertStatusHist= */ false);
              return null;
            });

    // act
    TaskId taskId = TaskId.builder().populationName("us").taskId(111).build();
    boolean updated = dao.updateTaskStatus(taskId, from.code, TaskEntity.Status.COMPLETED);

    // assert
    assertThat(updated).isFalse();
    assertThat(queryTaskById(taskId).get().getStatus()).isEqualTo(TaskEntity.Status.OPEN);
    assertThat(queryTaskStatusHistories(taskId)).isEqualTo(Arrays.asList());
  }

  @Test
  public void testUpdateTasktatus_NoUpdateIfFromStatusNotOnTop() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= */ TASK_OPEN,
                  /* insertStatusHist= */ false);
              insertTaskStatusHist(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* statusId= */ 0,
                  /* status= */ TASK_OPEN,
                  TS_NOW);
              insertTaskStatusHist(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* statusId= */ 1,
                  /* status= */ TASK_CREATED,
                  TS_NOW);
              return null;
            });

    // act
    TaskId taskId = TaskId.builder().populationName("us").taskId(111).build();
    boolean updated =
        dao.updateTaskStatus(taskId, TaskEntity.Status.OPEN, TaskEntity.Status.COMPLETED);

    // assert
    assertThat(updated).isFalse();
    assertThat(queryTaskById(taskId).get().getStatus()).isEqualTo(TaskEntity.Status.OPEN);
    assertThat(queryTaskStatusHistories(taskId)).isEqualTo(Arrays.asList(TASK_OPEN, TASK_CREATED));
  }

  @Test
  public void testGetIterationsOfStatusCollecting_Succedded() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= */ 0,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 8,
                  /* status= */ 0,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= */ 50,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 10,
                  /* status= */ 0,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ true);
              return null;
            });

    // act
    List<IterationEntity> iterations = dao.getIterationsOfStatus(IterationEntity.Status.COLLECTING);

    // assert
    assertThat(iterations)
        .isEqualTo(
            ImmutableList.of(
                IterationEntity.builder()
                    .populationName("us")
                    .taskId(111)
                    .iterationId(10)
                    .attemptId(0)
                    .status(IterationEntity.Status.COLLECTING)
                    .baseIterationId(9)
                    .baseOnResultId(9)
                    .reportGoal(300)
                    .resultId(10)
                    .info(ITERATION_INFO)
                    .aggregationLevel(0)
                    .maxAggregationSize(301)
                    .minClientVersion(MIN_CLIENT_VERSION)
                    .maxClientVersion(MAX_CLIENT_VERSION)
                    .build(),
                IterationEntity.builder()
                    .populationName("us")
                    .taskId(111)
                    .iterationId(8)
                    .attemptId(0)
                    .status(IterationEntity.Status.COLLECTING)
                    .baseIterationId(7)
                    .baseOnResultId(7)
                    .reportGoal(300)
                    .resultId(8)
                    .info(ITERATION_INFO)
                    .aggregationLevel(0)
                    .maxAggregationSize(301)
                    .minClientVersion(MIN_CLIENT_VERSION)
                    .maxClientVersion(MAX_CLIENT_VERSION)
                    .build()));
  }

  @Test
  public void testGetIterationsOfStatusCompleted_Succedded() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= */ 0,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 8,
                  /* status= */ 0,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= */ 50,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 10,
                  /* status= */ 0,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ true);
              return null;
            });

    // act
    List<IterationEntity> iterations = dao.getIterationsOfStatus(IterationEntity.Status.COMPLETED);

    // assert
    assertThat(iterations)
        .isEqualTo(
            ImmutableList.of(
                IterationEntity.builder()
                    .populationName("us")
                    .taskId(111)
                    .iterationId(9)
                    .attemptId(0)
                    .status(IterationEntity.Status.COMPLETED)
                    .baseIterationId(8)
                    .baseOnResultId(8)
                    .reportGoal(300)
                    .resultId(9)
                    .info(ITERATION_INFO)
                    .aggregationLevel(0)
                    .maxAggregationSize(301)
                    .minClientVersion(MIN_CLIENT_VERSION)
                    .maxClientVersion(MAX_CLIENT_VERSION)
                    .build()));
  }

  @Test
  public void testGetIterationsOfStatusAggregating_NotFound() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= */ 0,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 8,
                  /* status= */ 0,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= */ 50,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 10,
                  /* status= */ 0,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ true);
              return null;
            });

    // act
    List<IterationEntity> iterations =
        dao.getIterationsOfStatus(IterationEntity.Status.AGGREGATING);

    // assert
    assertThat(iterations).isEqualTo(ImmutableList.of());
  }

  @Test
  public void getOpenIterations_hasNoCollectingIterations_returnEmpty() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= OPEN */ 0,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 8,
                  /* status= COMPLETED*/ 50,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= AGGREGATING*/ 1,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ true);
              return null;
            });

    // act
    List<IterationEntity> iterations = dao.getOpenIterations("us", MIN_CLIENT_VERSION);

    // assert
    assertTrue(iterations.isEmpty());
  }

  @Test
  public void getOpenIterations_hasOneCollectingIteration_returnOne() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= OPEN */ 0,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 8,
                  /* status= COMPLETED*/ 50,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= COLLECTING*/ 0,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ true);
              return null;
            });

    // act
    List<IterationEntity> iterations = dao.getOpenIterations("us", MIN_CLIENT_VERSION);

    // assert
    ImmutableList<IterationId> iterationIds =
        iterations.stream().map(i -> i.getId()).collect(toImmutableList());
    assertThat(iterationIds)
        .containsExactlyElementsIn(
            ImmutableList.of(
                IterationId.builder()
                    .populationName("us")
                    .taskId(111)
                    .iterationId(9)
                    .attemptId(0)
                    .build()));
  }
  ;

  @Test
  public void
      getOpenIterations_hasMultipleCollectingIterationWithNotEnoughAssignment_returnMultiple() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= OPEN */ 0,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= COLLECTING*/ 0,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ true);
              insertAssignment(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-1",
                  /* createdTime */ TS_NOW,
                  /* active */ true,
                  /* withStatusHistory */ true);
              insertAssignment(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-2",
                  /* createdTime */ TS_NOW,
                  /* active */ false,
                  /* withStatusHistory */ true);
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 222,
                  /* status= OPEN */ 0,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 222,
                  /* iterationId= */ 5,
                  /* status= COLLECTING*/ 0,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ true);
              insertAssignment(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 222,
                  /* iterationId= */ 5,
                  /* sessionId */ "assignment-1",
                  /* createdTime */ TS_NOW,
                  /* active */ true,
                  /* withStatusHistory */ true);
              return null;
            });

    // act
    List<IterationEntity> iterations = dao.getOpenIterations("us", MIN_CLIENT_VERSION);

    // assert
    ImmutableList<IterationId> iterationIds =
        iterations.stream().map(i -> i.getId()).collect(toImmutableList());
    assertThat(iterationIds)
        .containsExactlyElementsIn(
            ImmutableList.of(
                IterationId.builder()
                    .populationName("us")
                    .taskId(111)
                    .iterationId(9)
                    .attemptId(0)
                    .build(),
                IterationId.builder()
                    .populationName("us")
                    .taskId(222)
                    .iterationId(5)
                    .attemptId(0)
                    .build()));
  }

  @Test
  public void getOpenIterations_hasOpenIterationOnMultiplePopulation_returnCorrectIterations() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= OPEN */ 0,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= COLLECTING*/ 0,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ true);
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 222,
                  /* status= OPEN */ 0,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 222,
                  /* iterationId= */ 5,
                  /* status= COLLECTING*/ 0,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ true);
              insertTask(
                  transaction,
                  "ca",
                  /* taskId= */ 123,
                  /* status= OPEN */ 0,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "ca",
                  /* taskId= */ 123,
                  /* iterationId= */ 5,
                  /* status= COLLECTING*/ 0,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ true);
              return null;
            });

    // query population 'us' and assert
    // act
    List<IterationEntity> usIterations = dao.getOpenIterations("us", MIN_CLIENT_VERSION);

    // assert
    ImmutableList<IterationId> usIterationIds =
        usIterations.stream().map(i -> i.getId()).collect(toImmutableList());
    assertThat(usIterationIds)
        .containsExactlyElementsIn(
            ImmutableList.of(
                IterationId.builder()
                    .populationName("us")
                    .taskId(111)
                    .iterationId(9)
                    .attemptId(0)
                    .build(),
                IterationId.builder()
                    .populationName("us")
                    .taskId(222)
                    .iterationId(5)
                    .attemptId(0)
                    .build()));

    // query population ca and assert
    // act
    List<IterationEntity> caIterations = dao.getOpenIterations("ca", MIN_CLIENT_VERSION);

    // assert
    ImmutableList<IterationId> caIterationIds =
        caIterations.stream().map(i -> i.getId()).collect(toImmutableList());
    assertThat(caIterationIds)
        .containsExactlyElementsIn(
            ImmutableList.of(
                IterationId.builder()
                    .populationName("ca")
                    .taskId(123)
                    .iterationId(5)
                    .attemptId(0)
                    .build()));
  }

  @Test
  public void getOpenIterations_hasEnoughActiveAssignments_returnEmpty() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= OPEN */ 0,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= COLLECTING*/ 0,
                  /* reportGoal= */ 1,
                  /* insertStatusHist= */ true);
              insertAssignment(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-1",
                  /* createdTime */ TS_NOW,
                  /* active */ true,
                  /* withStatusHistory */ true);
              insertAssignment(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-2",
                  /* createdTime */ TS_NOW,
                  /* active */ true,
                  /* withStatusHistory */ true);
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 222,
                  /* status= OPEN */ 0,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 222,
                  /* iterationId= */ 5,
                  /* status= COLLECTING*/ 0,
                  /* reportGoal= */ 1,
                  /* insertStatusHist= */ true);
              insertAssignment(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 222,
                  /* iterationId= */ 5,
                  /* sessionId */ "assignment-1",
                  /* createdTime */ TS_NOW,
                  /* active */ true,
                  /* withStatusHistory */ true);
              insertAssignment(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 222,
                  /* iterationId= */ 5,
                  /* sessionId */ "assignment-2",
                  /* createdTime */ TS_NOW,
                  /* active */ true,
                  /* withStatusHistory */ true);
              return null;
            });

    // act
    List<IterationEntity> iterations = dao.getOpenIterations("us", MIN_CLIENT_VERSION);

    // assert
    assertTrue(iterations.isEmpty());
  }

  @Test
  public void getAvailableCheckInsForPopulation_returnMultipleSuccess() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= OPEN */ 0,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= COLLECTING*/ 0,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ true);
              insertAssignment(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-1",
                  /* createdTime */ TS_NOW,
                  /* active */ true,
                  /* withStatusHistory */ true);
              insertAssignment(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-2",
                  /* createdTime */ TS_NOW,
                  /* active */ false,
                  /* withStatusHistory */ true);
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 222,
                  /* status= OPEN */ 0,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 222,
                  /* iterationId= */ 5,
                  /* status= COLLECTING*/ 0,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ true);
              insertAssignment(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 222,
                  /* iterationId= */ 5,
                  /* sessionId */ "assignment-1",
                  /* createdTime */ TS_NOW,
                  /* active */ true,
                  /* withStatusHistory */ true);
              return null;
            });

    // act
    Map<IterationEntity, CheckInResult> iterations =
        dao.getAvailableCheckInsForPopulation("us", MIN_CLIENT_VERSION);

    // assert
    ImmutableList<IterationId> iterationIds =
        iterations.keySet().stream().map(i -> i.getId()).collect(toImmutableList());
    assertThat(iterationIds)
        .containsExactlyElementsIn(
            ImmutableList.of(
                IterationId.builder()
                    .populationName("us")
                    .taskId(111)
                    .iterationId(9)
                    .attemptId(0)
                    .build(),
                IterationId.builder()
                    .populationName("us")
                    .taskId(222)
                    .iterationId(5)
                    .attemptId(0)
                    .build()));
    Collection<CheckInResult> results = iterations.values();
    assertThat(results)
        .containsExactlyElementsIn(ImmutableList.of(CheckInResult.SUCCESS, CheckInResult.SUCCESS));
  }

  @Test
  public void getAvailableCheckInsForPopulation_returnSingleSuccessSingleFull() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= OPEN */ 0,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= COLLECTING*/ 0,
                  /* reportGoal= */ 1,
                  /* insertStatusHist= */ true);
              insertAssignment(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-1",
                  /* createdTime */ TS_NOW,
                  /* active */ true,
                  /* withStatusHistory */ true);
              insertAssignment(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-2",
                  /* createdTime */ TS_NOW,
                  /* active */ true,
                  /* withStatusHistory */ true);
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 222,
                  /* status= OPEN */ 0,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 222,
                  /* iterationId= */ 5,
                  /* status= COLLECTING*/ 0,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ true);
              insertAssignment(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 222,
                  /* iterationId= */ 5,
                  /* sessionId */ "assignment-1",
                  /* createdTime */ TS_NOW,
                  /* active */ true,
                  /* withStatusHistory */ true);
              return null;
            });

    // act
    Map<IterationEntity, CheckInResult> iterations =
        dao.getAvailableCheckInsForPopulation("us", MIN_CLIENT_VERSION);

    // assert
    ImmutableList<IterationId> iterationIds =
        iterations.keySet().stream().map(i -> i.getId()).collect(toImmutableList());
    assertThat(iterationIds)
        .containsExactlyElementsIn(
            ImmutableList.of(
                IterationId.builder()
                    .populationName("us")
                    .taskId(111)
                    .iterationId(9)
                    .attemptId(0)
                    .build(),
                IterationId.builder()
                    .populationName("us")
                    .taskId(222)
                    .iterationId(5)
                    .attemptId(0)
                    .build()));
    Collection<CheckInResult> results = iterations.values();
    assertThat(results)
        .containsExactlyElementsIn(
            ImmutableList.of(CheckInResult.SUCCESS, CheckInResult.ITERATION_FULL));
  }

  @Test
  public void getAvailableCheckInsForPopulation_returnSingleMismatch() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= OPEN */ 0,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= COLLECTING*/ 0,
                  /* reportGoal= */ 1,
                  /* insertStatusHist= */ true);
              return null;
            });

    // act
    Map<IterationEntity, CheckInResult> iterations =
        dao.getAvailableCheckInsForPopulation(
            "us", String.valueOf(Long.parseLong(MAX_CLIENT_VERSION) + 1L));

    // assert
    ImmutableList<IterationId> iterationIds =
        iterations.keySet().stream().map(i -> i.getId()).collect(toImmutableList());
    assertThat(iterationIds)
        .containsExactlyElementsIn(
            ImmutableList.of(
                IterationId.builder()
                    .populationName("us")
                    .taskId(111)
                    .iterationId(9)
                    .attemptId(0)
                    .build()));
    Collection<CheckInResult> results = iterations.values();
    assertThat(results)
        .containsExactlyElementsIn(ImmutableList.of(CheckInResult.CLIENT_VERSION_MISMATCH));
  }

  @Test
  public void getAvailableCheckInsForPopulation_returnSingleNotOpen() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= CANCELED */ 1,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= AGGREGATING*/ 1,
                  /* reportGoal= */ 1,
                  /* insertStatusHist= */ true);
              return null;
            });

    // act
    Map<IterationEntity, CheckInResult> iterations =
        dao.getAvailableCheckInsForPopulation(
            "us", String.valueOf(Long.parseLong(MAX_CLIENT_VERSION) + 1L));

    // assert
    ImmutableList<IterationId> iterationIds =
        iterations.keySet().stream().map(i -> i.getId()).collect(toImmutableList());
    assertThat(iterationIds)
        .containsExactlyElementsIn(
            ImmutableList.of(
                IterationId.builder()
                    .populationName("us")
                    .taskId(111)
                    .iterationId(9)
                    .attemptId(0)
                    .build()));
    Collection<CheckInResult> results = iterations.values();
    assertThat(results)
        .containsExactlyElementsIn(ImmutableList.of(CheckInResult.ITERATION_NOT_OPEN));
  }

  @Test
  public void getAvailableCheckInsForPopulation_returnSingleNotActive() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= CANCELED */ 1,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= POST_PROCESSED*/ 51,
                  /* reportGoal= */ 1,
                  /* insertStatusHist= */ true);
              return null;
            });

    // act
    Map<IterationEntity, CheckInResult> iterations =
        dao.getAvailableCheckInsForPopulation(
            "us", String.valueOf(Long.parseLong(MAX_CLIENT_VERSION) + 1L));

    // assert
    assertThat(iterations).isEmpty();
  }

  @Test
  public void
      getIterationIdsPerEveryKIterationsSelector_hasValidTrainingIterations_returnCorrectOnes() {
    ImmutableList<Long> iterationIds = ImmutableList.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= CANCELED */ 101,
                  /* insertStatusHist= */ true);
              insertIterations(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationIds= */ iterationIds,
                  /* status= COMPLETED*/ 50,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ true,
                  /* timestamp= */ TS_NOW);

              return null;
            });

    // act and assert
    assertEquals(
        dao.getIterationIdsPerEveryKIterationsSelector("us", 111, 1, 24),
        ImmutableList.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L));
    assertEquals(
        dao.getIterationIdsPerEveryKIterationsSelector("us", 111, 2, 24),
        ImmutableList.of(1L, 3L, 5L, 7L, 9L));
    assertEquals(
        dao.getIterationIdsPerEveryKIterationsSelector("us", 111, 3, 24),
        ImmutableList.of(1L, 4L, 7L, 10L));
  }

  @Test
  public void getIterationIdsPerEveryKIterationsSelector_emptyTrainingIterations_returnEmpty() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= CANCELED */ 101,
                  /* insertStatusHist= */ true);

              return null;
            });

    // act and assert
    assertThat(dao.getIterationIdsPerEveryKIterationsSelector("us", 111, 3, 24)).isEmpty();
  }

  @Test
  public void getIterationIdsPerEveryKIterationsSelector_hasOldIterations_returnExpected() {
    // arrange
    ImmutableList<Long> iterationIds = ImmutableList.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L);
    Instant past25HoursInstant = NOW.minusSeconds(3600 * 25);
    Timestamp past25Hours =
        Timestamp.ofTimeSecondsAndNanos(
            past25HoursInstant.getEpochSecond(), past25HoursInstant.getNano());
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= CANCELED */ 101,
                  /* insertStatusHist= */ true);
              insertIterations(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationIds= */ iterationIds,
                  /* status= COMPLETED*/ 50,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ true,
                  past25Hours);

              return null;
            });

    // act and assert
    assertThat(dao.getIterationIdsPerEveryKIterationsSelector("us", 111, 1, 24)).isEmpty();
    assertEquals(
        dao.getIterationIdsPerEveryKIterationsSelector("us", 111, 1, 26),
        ImmutableList.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L));
  }

  @Test
  public void getIterationIdsPerEveryKHoursSelector_hasValidTrainingIterations_returnCorrectOnes() {
    // arrange, insert 80 iteration history record from 20 hours ago with 1 record every 15 minutes,
    // the latest record is with timestamp at now.
    Instant past20HoursInstant = NOW.minusSeconds(3600 * 20);
    ImmutableMap<Long, Timestamp> iterationIdTimestampMap =
        buildIterationIdTimestampMap(past20HoursInstant, 1, 80, 900);
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= CANCELED */ 101,
                  /* insertStatusHist= */ true);
              insertIterations(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* status= COMPLETED*/ 50,
                  /* reportGoal= */ 300,
                  true,
                  iterationIdTimestampMap);
              return null;
            });

    // act and assert
    assertEquals(
        dao.getIterationIdsPerEveryKHoursSelector("us", 111, 4, 24),
        ImmutableList.of(1L, 17L, 33L, 49L, 65L));
    // first 6 hour duration split at between iteration id 1 and 9, thus 1 was selected in first
    // slot, 9 is selected at second slot. After that, next selected is 6 hours after previous
    // one(every 24 iterations, e.g. between 9 and 33)
    assertEquals(
        dao.getIterationIdsPerEveryKHoursSelector("us", 111, 6, 24),
        ImmutableList.of(1L, 9L, 33L, 57L));
  }

  @Test
  public void getIterationIdsPerEveryKHoursSelector_emptyTrainingIterations_returnEmpty() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= CANCELED */ 101,
                  /* insertStatusHist= */ true);
              return null;
            });

    // act and assert
    assertThat(dao.getIterationIdsPerEveryKHoursSelector("us", 111, 3, 24)).isEmpty();
  }

  @Test
  public void getIterationIdsPerEveryKHoursSelector_hasOldIterations_returnExpected() {
    // insert 20 iteration history record from 30 hours ago with 1 record every 15 minutes,
    // the latest record is with timestamp 25 hours ago.
    Instant past30HoursInstant = NOW.minusSeconds(3600 * 30);
    ImmutableMap<Long, Timestamp> iterationIdTimestampMap =
        buildIterationIdTimestampMap(past30HoursInstant, 1, 20, 900);
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= CANCELED */ 101,
                  /* insertStatusHist= */ true);
              insertIterations(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* status= COMPLETED*/ 50,
                  /* reportGoal= */ 300,
                  true,
                  iterationIdTimestampMap);
              return null;
            });

    // act and assert
    assertThat(dao.getIterationIdsPerEveryKHoursSelector("us", 111, 1, 24)).isEmpty();
    assertEquals(
        ImmutableList.of(1L, 5L, 9L, 13L, 17L),
        dao.getIterationIdsPerEveryKHoursSelector("us", 111, 1, 31));
  }

  @Test
  public void getIterationCreatedTime_returnExpected() {
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction,
                  "us",
                  /* taskId= */ 111,
                  /* status= */ 0,
                  /* insertStatusHist= */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 1,
                  /* status= */ ITERATION_COLLECTING,
                  /* reportGoal= */ 300,
                  /* insertStatusHist= */ false);

              insertIterationStatusHist(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 1,
                  /* statusId= */ 0,
                  /* status= */ ITERATION_COLLECTING,
                  /* createdTime= */ TS_NOW,
                  /* aggregationLevel= */ 0);

              insertIterationStatusHist(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 1,
                  /* statusId= */ 1,
                  /* status= */ ITERATION_APPLYING,
                  /* createdTime= */ TS_NOW,
                  /* aggregationLevel= */ 0);
              return null;
            });

    // act and assert
    Optional<Instant> iterationCreatedTime = dao.getIterationCreatedTime(DEFAULT_ITERATION);
    Optional<Instant> emptyIterationCreatedTime =
        dao.getIterationCreatedTime(
            IterationEntity.builder()
                .populationName("none")
                .taskId(111)
                .iterationId(10)
                .attemptId(0)
                .reportGoal(300)
                .status(IterationEntity.Status.fromCode(0))
                .baseIterationId(9)
                .baseOnResultId(9)
                .resultId(10)
                .info(ITERATION_INFO)
                .aggregationLevel(0)
                .maxAggregationSize(301)
                .minClientVersion(MIN_CLIENT_VERSION)
                .maxClientVersion(MAX_CLIENT_VERSION)
                .build());
    assertTrue(emptyIterationCreatedTime.isEmpty());
    assertTrue(iterationCreatedTime.isPresent());
    assertTrue(
        iterationCreatedTime.get().equals(TimestampInstantConverter.TO_INSTANT.convert(TS_NOW)));
  }

  private ImmutableMap<Long, Timestamp> buildIterationIdTimestampMap(
      Instant startTime, long startIterationId, long count, int intervalInSeconds) {
    ImmutableMap.Builder<Long, Timestamp> builder = ImmutableMap.builder();
    for (long i = 0; i < count; i++) {
      long iterationId = startIterationId + i;
      long epochSecond = startTime.getEpochSecond() + i * intervalInSeconds;
      builder.put(iterationId, Timestamp.ofTimeSecondsAndNanos(epochSecond, startTime.getNano()));
    }

    return builder.build();
  }

  private Optional<TaskEntity> queryTaskById(TaskId taskId) {
    Statement statement =
        Statement.newBuilder(
                "SELECT * FROM Task WHERE PopulationName = @populationName AND TaskId=@taskId")
            .bind("populationName")
            .to(taskId.getPopulationName())
            .bind("taskId")
            .to(taskId.getTaskId())
            .build();

    try (ResultSet resultSet =
        dbClient
            .singleUse() // Execute a single read or query against Cloud Spanner.
            .executeQuery(statement)) {
      ImmutableList.Builder<TaskEntity> entitiesBuilder = ImmutableList.builder();
      while (resultSet.next()) {
        entitiesBuilder.add(
            TaskEntity.builder()
                .populationName(resultSet.getString("PopulationName"))
                .taskId(resultSet.getLong("TaskId"))
                .totalIteration(resultSet.getLong("TotalIteration"))
                .minAggregationSize(resultSet.getLong("MinAggregationSize"))
                .maxAggregationSize(resultSet.getLong("MaxAggregationSize"))
                .maxParallel(resultSet.getLong("MaxParallel"))
                .correlationId(resultSet.getString("CorrelationId"))
                .minClientVersion(resultSet.getString("MinClientVersion"))
                .maxClientVersion(resultSet.getString("MaxClientVersion"))
                .status(TaskEntity.Status.fromCode(resultSet.getLong("Status")))
                .build());
      }
      return entitiesBuilder.build().stream().findFirst();
    }
  }

  private Optional<IterationEntity> queryIterationById(IterationId iterId) {
    Statement statement =
        Statement.newBuilder(
                "SELECT * FROM Iteration WHERE PopulationName = @populationName AND TaskId=@taskId"
                    + " AND IterationId=@iterationId AND AttemptId=@attemptId")
            .bind("populationName")
            .to(iterId.getPopulationName())
            .bind("taskId")
            .to(iterId.getTaskId())
            .bind("iterationId")
            .to(iterId.getIterationId())
            .bind("attemptId")
            .to(iterId.getAttemptId())
            .build();

    try (ResultSet resultSet =
        dbClient
            .singleUse() // Execute a single read or query against Cloud Spanner.
            .executeQuery(statement)) {
      ImmutableList.Builder<IterationEntity> entitiesBuilder = ImmutableList.builder();
      while (resultSet.next()) {
        entitiesBuilder.add(
            IterationEntity.builder()
                .populationName(resultSet.getString("PopulationName"))
                .taskId(resultSet.getLong("TaskId"))
                .iterationId(resultSet.getLong("IterationId"))
                .attemptId(resultSet.getLong("AttemptId"))
                .status(IterationEntity.Status.fromCode(resultSet.getLong("Status")))
                .baseIterationId(resultSet.getLong("BaseIterationId"))
                .baseOnResultId(resultSet.getLong("BaseOnResultId"))
                .reportGoal(resultSet.getLong("ReportGoal"))
                .resultId(resultSet.getLong("ResultId"))
                .aggregationLevel(resultSet.getLong("AggregationLevel"))
                .build());
      }
      return entitiesBuilder.build().stream().findFirst();
    }
  }

  private void insertTask(
      TransactionContext transaction,
      String populationName,
      long taskId,
      long status,
      boolean insertStatusHist) {
    String insertTask =
        "INSERT INTO Task(PopulationName, TaskId, TotalIteration, MinAggregationSize,"
            + " MaxAggregationSize, Status, MaxParallel, CreatedTime, CorrelationId,"
            + " MinClientVersion, MaxClientVersion, Info) \n"
            + "VALUES(@populationName, @taskId, @totalIteration, @minAggregationSize,"
            + " @maxAggregationSize, @status, @maxParallel, @createdTime, @correlationId,"
            + " @minClientVersion, @maxClientVersion, @info)";

    transaction.executeUpdate(
        Statement.newBuilder(insertTask)
            .bind("populationName")
            .to(populationName)
            .bind("taskId")
            .to(taskId)
            .bind("totalIteration")
            .to(222)
            .bind("minAggregationSize")
            .to(MIN_AGGREGATION_SIZE)
            .bind("maxAggregationSize")
            .to(MAX_AGGREGATION_SIZE)
            .bind("status")
            .to(status)
            .bind("maxParallel")
            .to(555)
            .bind("createdTime")
            .to(TS_NOW)
            .bind("correlationId")
            .to("correlation")
            .bind("minClientVersion")
            .to(MIN_CLIENT_VERSION)
            .bind("maxClientVersion")
            .to(MAX_CLIENT_VERSION)
            .bind("info")
            .to(Value.json(TASK_INFO))
            .build());
    if (insertStatusHist) {
      insertTaskStatusHist(transaction, populationName, taskId, 0, status, TS_NOW);
    }
  }

  private void insertIteration(
      TransactionContext transaction,
      String populationName,
      long taskId,
      long iterationId,
      long status,
      long reportGoal,
      boolean insertStatusHist) {
    insertIterationWithTimestamp(
        transaction,
        populationName,
        taskId,
        iterationId,
        status,
        reportGoal,
        insertStatusHist,
        TS_NOW);
  }

  private void insertIterationWithTimestamp(
      TransactionContext transaction,
      String populationName,
      long taskId,
      long iterationId,
      long status,
      long reportGoal,
      boolean insertStatusHist,
      Timestamp timestamp) {
    String insertIteration =
        "INSERT INTO Iteration(PopulationName, TaskId, IterationId, AttemptId, Status,"
            + " BaseIterationId, BaseOnResultId, ReportGoal, ResultId, Info, AggregationLevel,"
            + " MaxAggregationSize, MinClientVersion, MaxClientVersion )"
            + " VALUES(@populationName, @taskId, @iterationId, @attemptId, @status,"
            + " @baseIterationId, @baseOnResultId, @reportGoal, @resultId, @info,"
            + " @aggregationLevel, @maxAggregationSize, @minClientVersion, @maxClientVersion)";
    transaction.executeUpdate(
        Statement.newBuilder(insertIteration)
            .bind("PopulationName")
            .to(populationName)
            .bind("taskId")
            .to(taskId)
            .bind("iterationId")
            .to(iterationId)
            .bind("attemptId")
            .to(0)
            .bind("status")
            .to(status)
            .bind("baseIterationId")
            .to(iterationId - 1)
            .bind("baseOnResultId")
            .to(iterationId - 1)
            .bind("reportGoal")
            .to(reportGoal)
            .bind("resultId")
            .to(iterationId)
            .bind("info")
            .to(Value.json(ITERATION_INFO))
            .bind("aggregationLevel")
            .to(0)
            .bind("maxAggregationSize")
            .to(reportGoal + 1)
            .bind("minClientVersion")
            .to(MIN_CLIENT_VERSION)
            .bind("maxClientVersion")
            .to(MAX_CLIENT_VERSION)
            .build());
    if (insertStatusHist) {
      insertIterationStatusHist(
          transaction, populationName, taskId, iterationId, 0, status, timestamp, 0);
    }
  }

  private void insertIterationStatusHist(
      TransactionContext transaction,
      String populationName,
      long taskId,
      long iterationId,
      long statusId,
      long status,
      Timestamp createdTime,
      long aggregationLevel) {
    String insertStatus =
        "INSERT INTO IterationStatusHistory(PopulationName, TaskId, IterationId, AttemptId,\n"
            + " StatusId, CreatedTime, Status, AggregationLevel)\n"
            + " VALUES(@populationName, @taskId, @iterationId, @attemptId,\n"
            + " @statusId, @createdTime, @status, @aggregationLevel)\n";
    transaction.executeUpdate(
        Statement.newBuilder(insertStatus)
            .bind("populationName")
            .to(populationName)
            .bind("taskId")
            .to(taskId)
            .bind("iterationId")
            .to(iterationId)
            .bind("attemptId")
            .to(0)
            .bind("statusId")
            .to(status)
            .bind("createdTime")
            .to(createdTime)
            .bind("statusId")
            .to(statusId)
            .bind("status")
            .to(status)
            .bind("aggregationLevel")
            .to(aggregationLevel)
            .build());
  }

  private void insertIterations(
      TransactionContext transaction,
      String populationName,
      long taskId,
      List<Long> iterationIds,
      long status,
      long reportGoal,
      boolean insertStatusHist,
      Timestamp timestamp) {
    iterationIds.forEach(
        iterationId ->
            insertIterationWithTimestamp(
                transaction,
                populationName,
                taskId,
                iterationId,
                status,
                reportGoal,
                insertStatusHist,
                timestamp));
  }

  private void insertIterations(
      TransactionContext transaction,
      String populationName,
      long taskId,
      long status,
      long reportGoal,
      boolean insertStatusHist,
      Map<Long, Timestamp> timestampsByIterationId) {
    timestampsByIterationId
        .entrySet()
        .forEach(
            entry ->
                insertIterationWithTimestamp(
                    transaction,
                    populationName,
                    taskId,
                    entry.getKey(),
                    status,
                    reportGoal,
                    insertStatusHist,
                    entry.getValue()));
  }

  private void insertAssignment(
      TransactionContext transaction,
      String populationName,
      long taskId,
      long iterationId,
      String assignmentId,
      Timestamp createdTime,
      boolean active,
      boolean withStatusHistory) {
    long status = active ? ASSIGNED : LOCAL_FAILED;
    String insertIteration =
        "INSERT INTO Assignment(PopulationName, TaskId, IterationId, AttemptId, SessionId,\n"
            + " CorrelationId, Status, CreatedTime)\n"
            + " VALUES(@populationName, @taskId, @iterationId, @attemptId, @sessionId,\n"
            + " @correlationId, @status, @createdTime)\n";
    transaction.executeUpdate(
        Statement.newBuilder(insertIteration)
            .bind("populationName")
            .to(populationName)
            .bind("taskId")
            .to(taskId)
            .bind("iterationId")
            .to(iterationId)
            .bind("attemptId")
            .to(0)
            .bind("sessionId")
            .to(assignmentId)
            .bind("correlationId")
            .to("correlation-a")
            .bind("createdTime")
            .to(createdTime)
            .bind("status")
            .to(status)
            .build());

    if (!withStatusHistory) {
      return;
    }

    insertAssignmentStatusHist(
        /* transaction= */ transaction,
        /* populationName= */ populationName,
        /* taskId= */ taskId,
        /* iterationId= */ iterationId,
        /* assignmentId= */ assignmentId,
        /* statusId= */ 1,
        /* status= */ status,
        /* createdTime= */ createdTime);
  }

  private void insertAssignmentStatusHist(
      TransactionContext transaction,
      String populationName,
      long taskId,
      long iterationId,
      String assignmentId,
      long statusId,
      long status,
      Timestamp createdTime) {
    String insertIteration =
        "INSERT INTO AssignmentStatusHistory(PopulationName, TaskId, IterationId, AttemptId,"
            + " SessionId,\n"
            + " StatusId, CreatedTime, Status)\n"
            + " VALUES(@populationName, @taskId, @iterationId, @attemptId, @sessionId,\n"
            + " @statusId, @createdTime, @status)\n";
    transaction.executeUpdate(
        Statement.newBuilder(insertIteration)
            .bind("populationName")
            .to(populationName)
            .bind("taskId")
            .to(taskId)
            .bind("iterationId")
            .to(iterationId)
            .bind("attemptId")
            .to(0)
            .bind("sessionId")
            .to(assignmentId)
            .bind("statusId")
            .to(status)
            .bind("createdTime")
            .to(createdTime)
            .bind("statusId")
            .to(statusId)
            .bind("status")
            .to(status)
            .build());
  }

  private List<Long> queryIterationStatusHistories(IterationId id) {
    Statement statement =
        Statement.newBuilder(
                "SELECT * FROM IterationStatusHistory WHERE PopulationName = @populationName AND\n"
                    + " TaskId=@taskId AND IterationId=@iterationId AND AttemptId=@attemptId \n"
                    + " ORDER BY StatusId\n")
            .bind("populationName")
            .to(id.getPopulationName())
            .bind("taskId")
            .to(id.getTaskId())
            .bind("iterationId")
            .to(id.getIterationId())
            .bind("attemptId")
            .to(id.getAttemptId())
            .build();

    try (ResultSet resultSet = dbClient.singleUse().executeQuery(statement)) {
      List<Long> status = new ArrayList();

      while (resultSet.next()) {
        status.add(resultSet.getLong("Status"));
      }
      return status;
    }
  }

  private void insertTaskStatusHist(
      TransactionContext transaction,
      String populationName,
      long taskId,
      long statusId,
      long status,
      Timestamp createdTime) {
    String insertStatus =
        "INSERT INTO TaskStatusHistory(PopulationName, TaskId,\n"
            + " StatusId, CreatedTime, Status)\n"
            + " VALUES(@populationName, @taskId, \n"
            + " @statusId, @createdTime, @status)\n";
    transaction.executeUpdate(
        Statement.newBuilder(insertStatus)
            .bind("populationName")
            .to(populationName)
            .bind("taskId")
            .to(taskId)
            .bind("statusId")
            .to(status)
            .bind("createdTime")
            .to(createdTime)
            .bind("statusId")
            .to(statusId)
            .bind("status")
            .to(status)
            .build());
  }

  private List<Long> queryTaskStatusHistories(TaskId id) {
    Statement statement =
        Statement.newBuilder(
                "SELECT * FROM TaskStatusHistory WHERE PopulationName = @populationName AND\n"
                    + " TaskId=@taskId\n"
                    + " ORDER BY StatusId\n")
            .bind("populationName")
            .to(id.getPopulationName())
            .bind("taskId")
            .to(id.getTaskId())
            .build();

    try (ResultSet resultSet = dbClient.singleUse().executeQuery(statement)) {
      List<Long> status = new ArrayList();

      while (resultSet.next()) {
        status.add(resultSet.getLong("Status"));
      }
      return status;
    }
  }
}
