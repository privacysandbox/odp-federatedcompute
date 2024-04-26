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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.TransactionContext;
import com.google.cloud.spanner.Value;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AggregationBatchEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AssignmentEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AssignmentId;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.IterationEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.IterationEntity.Status;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.IterationId;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.InstantSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;

@RunWith(JUnit4.class)
public final class AssignmentSpannerDaoTest {
  private static final String PROJECT_ID = "spanner-emulator";
  private static final String INSTANCE = "fcp-task-unittest";
  private static final String DB_NAME = "test-assignment-dao";
  private static final String SDL_FILE_PATH = "shuffler/spanner/task_database.sdl";
  private static final long ASSIGNED = AssignmentEntity.Status.ASSIGNED.code();
  private static final long LOCAL_FAILED = AssignmentEntity.Status.LOCAL_FAILED.code();
  private static final long LOCAL_COMPLETED = AssignmentEntity.Status.LOCAL_COMPLETED.code();
  private static final long UPLOAD_COMPLETED = AssignmentEntity.Status.UPLOAD_COMPLETED.code();

  private static SpannerTestHarness.Connection spannerEmulatorConnection;
  // GMT time.
  private static Instant NOW = Instant.parse("2023-09-01T00:00:00Z");
  private static Timestamp TS_NOW = Timestamp.ofTimeSecondsAndNanos(1693526400, 0);
  private static InstantSource instanceSource = InstantSource.fixed(NOW);
  private static Instant TWENTY_MINUTES_AGO = NOW.minusSeconds(20 * 60);
  private static Instant TEN_MINUTES_AGO = NOW.minusSeconds(10 * 60);
  private static final Value TASK_INFO =
      Value.json("{\"trafficWeight\": \"1\", \"trainingInfo\": {}}");
  private static final Value ITERATION_INFO =
      Value.json("{\"taskInfo\":{\"trafficWeight\":\"1\",\"trainingInfo\":{}}}");

  private DatabaseClient dbClient;
  @InjectMocks AssignmentSpannerDao dao;

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
    this.dao = new AssignmentSpannerDao(dbClient, instanceSource, Optional.empty());
    MockitoAnnotations.initMocks(this);
  }

  @After
  public void teardown() throws SQLException {
    deleteDatabase();
  }

  @Test
  public void testCreateAssignment_Succeeded() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(transaction, "us", /* taskId= */ 111, /* status= */ 0);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= */ 0,
                  /* reportGoal= */ 300);
              return null;
            });

    IterationEntity iterationEntity =
        createIterationEntity(
            /* populationName= */ "us",
            /* taskId= */ 111,
            /* iterationId= */ 9,
            /* status= */ Status.COLLECTING,
            /* reportGoal= */ 300);

    // act
    Optional<AssignmentEntity> result =
        dao.createAssignment(
            /* iterationEntity= */ iterationEntity,
            /* correlationId= */ "abc",
            /* sessionId= */ "xyz");

    // assert
    assertThat(result)
        .isEqualTo(
            Optional.of(
                AssignmentEntity.builder()
                    .populationName("us")
                    .taskId(111)
                    .iterationId(9)
                    .attemptId(0)
                    .sessionId("xyz")
                    .correlationId("abc")
                    .baseIterationId(8)
                    .baseOnResultId(8)
                    .resultId(9)
                    .status(AssignmentEntity.Status.ASSIGNED)
                    .build()));
    assertThat(queryStatusHistories(getId(result.get()))).isEqualTo(Arrays.asList(ASSIGNED));
  }

  @Test
  public void testCreateAssignment_multipleTaskAvailable_createUnderTargetTask() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction, /* populationName= */ "us", /* taskId= */ 111, /* status= */ 1);
              insertTask(
                  transaction, /* populationName= */ "us", /* taskId= */ 112, /* status= */ 0);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 112,
                  /* iterationId= */ 10,
                  /* status= */ 0,
                  /* reportGoal= */ 3);

              return null;
            });
    IterationEntity iterationEntity =
        createIterationEntity(
            /* populationName= */ "us",
            /* taskId= */ 112,
            /* iterationId= */ 10,
            /* status= */ Status.COLLECTING,
            /* reportGoal= */ 3);

    // act
    Optional<AssignmentEntity> result =
        dao.createAssignment(
            /* iterationEntity= */ iterationEntity,
            /* correlationId= */ "abc",
            /* sessionId= */ "xyz");

    // assert
    assertThat(result)
        .isEqualTo(
            Optional.of(
                AssignmentEntity.builder()
                    .populationName("us")
                    .taskId(112)
                    .iterationId(10)
                    .attemptId(0)
                    .sessionId("xyz")
                    .correlationId("abc")
                    .baseIterationId(9)
                    .baseOnResultId(9)
                    .resultId(10)
                    .status(AssignmentEntity.Status.ASSIGNED)
                    .build()));
  }

  @Test
  public void testCreateAssignment_noParentIteration_throwException() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(transaction, "us", /* taskId= */ 111, /* status= */ 0);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= */ 0,
                  /* reportGoal= */ 300);
              return null;
            });
    IterationEntity iterationEntity =
        createIterationEntity(
            /* populationName= */ "cn",
            /* taskId= */ 111,
            /* iterationId= */ 9,
            /* status= */ Status.COLLECTING,
            /* reportGoal= */ 300);

    // act
    SpannerException expected =
        assertThrows(
            SpannerException.class,
            () ->
                dao.createAssignment(
                    /* iterationEntity= */ iterationEntity,
                    /* correlationId= */ "abc",
                    /* sessionId= */ "xyz"));

    // assert
    assertThat(expected)
        .hasMessageThat()
        .contains("Insert failed because key was not found in parent table");
    assertThat(expected.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
  }

  @Test
  public void testCreateAssignment_multiplePopulationWithTargetPopulation_created() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(transaction, "aaa", /* taskId= */ 111, /* status= */ 0);
              insertTask(transaction, "bbb", /* taskId= */ 111, /* status= */ 0);
              insertTask(transaction, "ccc", /* taskId= */ 111, /* status= */ 0);
              insertIteration(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= */ 0,
                  /* reportGoal= */ 300);
              insertIteration(
                  transaction,
                  /* populationName= */ "bbb",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= */ 0,
                  /* reportGoal= */ 300);
              insertIteration(
                  transaction,
                  /* populationName= */ "ccc",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= */ 0,
                  /* reportGoal= */ 300);
              return null;
            });

    IterationEntity iterationEntity =
        createIterationEntity(
            /* populationName= */ "bbb",
            /* taskId= */ 111,
            /* iterationId= */ 9,
            /* status= */ Status.COLLECTING,
            /* reportGoal= */ 2);

    // act
    Optional<AssignmentEntity> result =
        dao.createAssignment(
            /* iterationEntity= */ iterationEntity,
            /* correlationId= */ "abc",
            /* sessionId= */ "xyz");

    // assert
    assertThat(result)
        .isEqualTo(
            Optional.of(
                AssignmentEntity.builder()
                    .populationName("bbb")
                    .taskId(111)
                    .iterationId(9)
                    .attemptId(0)
                    .sessionId("xyz")
                    .correlationId("abc")
                    .baseIterationId(8)
                    .baseOnResultId(8)
                    .resultId(9)
                    .status(AssignmentEntity.Status.ASSIGNED)
                    .build()));
  }

  @Test
  public void testGetAssignment_Suceeded() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(transaction, "us", /* taskId= */ 111, /* status= */ 0);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= */ 0,
                  /* reportGoal= */ 300);
              insertAssignment(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-1",
                  /* createdTime */ TS_NOW,
                  /* active */ true,
                  /* batchId */ null,
                  /* withStatusHistory */ true);
              return null;
            });
    // act
    Optional<AssignmentEntity> result =
        dao.getAssignment(
            AssignmentId.builder()
                .populationName("us")
                .taskId(111)
                .iterationId(9)
                .attemptId(0)
                .assignmentId("assignment-1")
                .build());

    // assert
    assertThat(result)
        .isEqualTo(
            Optional.of(
                AssignmentEntity.builder()
                    .populationName("us")
                    .taskId(111)
                    .iterationId(9)
                    .attemptId(0)
                    .sessionId("assignment-1")
                    .baseIterationId(8)
                    .baseOnResultId(8)
                    .resultId(9)
                    .status(AssignmentEntity.Status.ASSIGNED)
                    .batchId(null)
                    .build()));
  }

  @Test
  public void testGetAssignment_NullBatchIdSuceeded() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(transaction, "us", /* taskId= */ 111, /* status= */ 0);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= */ 0,
                  /* reportGoal= */ 300);
              insertAssignment(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-1",
                  /* createdTime */ TS_NOW,
                  /* active */ true,
                  /* batchId */ null,
                  /* withStatusHistory */ true);
              return null;
            });
    // act
    Optional<AssignmentEntity> result =
        dao.getAssignment(
            AssignmentId.builder()
                .populationName("us")
                .taskId(111)
                .iterationId(9)
                .attemptId(0)
                .assignmentId("assignment-1")
                .build());

    // assert
    assertThat(result)
        .isEqualTo(
            Optional.of(
                AssignmentEntity.builder()
                    .populationName("us")
                    .taskId(111)
                    .iterationId(9)
                    .attemptId(0)
                    .sessionId("assignment-1")
                    .baseIterationId(8)
                    .baseOnResultId(8)
                    .resultId(9)
                    .status(AssignmentEntity.Status.ASSIGNED)
                    .build()));
  }

  @Test
  public void testGetAssignment_NotFound() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(transaction, "us", /* taskId= */ 111, /* status= */ 0);
              insertIteration(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= */ 0,
                  /* reportGoal= */ 300);
              insertAssignment(
                  transaction,
                  /* populationName= */ "us",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-1",
                  /* createdTime */ TS_NOW,
                  /* active */ true,
                  /* batchId */ null,
                  /* withStatusHistory */ true);
              return null;
            });
    // act
    Optional<AssignmentEntity> result =
        dao.getAssignment(
            AssignmentId.builder()
                .populationName("us")
                .taskId(111)
                .iterationId(9)
                .attemptId(0)
                .assignmentId("assignment-2")
                .build());

    // assert
    assertThat(result).isEqualTo(Optional.empty());
  }

  @Test
  public void testUpdateAssignmentStatus_Succeeded() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction, /* populationName= */ "aaa", /* taskId= */ 111, /* status= */ 0);
              insertIteration(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= */ 0,
                  /* reportGoal= */ 300);
              insertAssignment(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-1",
                  /* createdTime */ TS_NOW,
                  /* active */ true,
                  /* batchId */ null,
                  /* withStatusHistory */ true);
              return null;
            });

    // act
    AssignmentId assignmentId =
        AssignmentId.builder()
            .populationName("aaa")
            .taskId(111)
            .iterationId(9)
            .attemptId(0)
            .assignmentId("assignment-1")
            .build();
    boolean updated =
        dao.updateAssignmentStatus(
            assignmentId,
            AssignmentEntity.Status.ASSIGNED,
            AssignmentEntity.Status.LOCAL_COMPLETED);

    // assert
    assertThat(updated).isTrue();
    assertThat(queryAssignmentStatusById(assignmentId).getStatus())
        .isEqualTo(AssignmentEntity.Status.LOCAL_COMPLETED);
  }

  @Test
  public void testUpdateAssignmentStatus_FailedBatchId() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction, /* populationName= */ "aaa", /* taskId= */ 111, /* status= */ 0);
              insertIteration(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= */ 0,
                  /* reportGoal= */ 300);
              insertAssignment(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-1",
                  /* createdTime */ TS_NOW,
                  /* active */ true,
                  /* batchId */ "batch",
                  /* withStatusHistory */ true);
              return null;
            });

    // act
    AssignmentId assignmentId =
        AssignmentId.builder()
            .populationName("aaa")
            .taskId(111)
            .iterationId(9)
            .attemptId(0)
            .assignmentId("assignment-1")
            .build();
    boolean updated =
        dao.updateAssignmentStatus(
            assignmentId,
            AssignmentEntity.Status.ASSIGNED,
            AssignmentEntity.Status.LOCAL_COMPLETED);

    // assert
    assertThat(updated).isFalse();
    assertThat(queryAssignmentStatusById(assignmentId).getStatus())
        .isEqualTo(AssignmentEntity.Status.ASSIGNED);
  }

  @Test
  public void testBatchUpdateAssignmentStatus_Succeeded() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction, /* populationName= */ "aaa", /* taskId= */ 111, /* status= */ 0);
              insertIteration(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= */ 0,
                  /* reportGoal= */ 300);
              insertAssignment(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-1",
                  /* createdTime */ TS_NOW,
                  /* active */ true,
                  /* batchId */ null,
                  /* withStatusHistory */ true);
              insertAssignment(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-2",
                  /* createdTime */ TS_NOW,
                  /* active */ true,
                  /* batchId */ null,
                  /* withStatusHistory */ true);
              return null;
            });

    // act
    AssignmentId assignmentId1 =
        AssignmentId.builder()
            .populationName("aaa")
            .taskId(111)
            .iterationId(9)
            .attemptId(0)
            .assignmentId("assignment-1")
            .build();
    AssignmentId assignmentId2 =
        AssignmentId.builder()
            .populationName("aaa")
            .taskId(111)
            .iterationId(9)
            .attemptId(0)
            .assignmentId("assignment-2")
            .build();
    int updated =
        dao.batchUpdateAssignmentStatus(
            List.of(assignmentId1, assignmentId2),
            AssignmentEntity.Status.ASSIGNED,
            AssignmentEntity.Status.LOCAL_COMPLETED);

    // assert
    assertThat(updated).isEqualTo(2);
    assertThat(queryAssignmentStatusById(assignmentId1).getStatus())
        .isEqualTo(AssignmentEntity.Status.LOCAL_COMPLETED);
    assertThat(queryAssignmentStatusById(assignmentId2).getStatus())
        .isEqualTo(AssignmentEntity.Status.LOCAL_COMPLETED);
  }

  @Test
  public void testBatchUpdateAssignmentStatus_WithOneStatusMismatch() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction, /* populationName= */ "aaa", /* taskId= */ 111, /* status= */ 0);
              insertIteration(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= */ 0,
                  /* reportGoal= */ 300);
              insertAssignment(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-1",
                  /* createdTime */ TS_NOW,
                  /* active */ true,
                  /* batchId */ null,
                  /* withStatusHistory */ true);
              insertAssignment(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-2",
                  /* createdTime */ TS_NOW,
                  /* active */ true,
                  /* batchId */ null,
                  /* withStatusHistory */ true);
              insertAssignment(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-3",
                  /* createdTime */ TS_NOW,
                  /* active */ true,
                  /* batchId */ null,
                  /* withStatusHistory */ false);
              insertAssignmentStatusHist(
                  /* transaction= */ transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* assignmentId= */ "assignment-3",
                  /* statusId= */ 1,
                  /* status= */ LOCAL_COMPLETED,
                  /* batchId= */ null,
                  /* createdTime= */ TS_NOW);
              return null;
            });

    // act
    AssignmentId assignmentId1 =
        AssignmentId.builder()
            .populationName("aaa")
            .taskId(111)
            .iterationId(9)
            .attemptId(0)
            .assignmentId("assignment-1")
            .build();
    AssignmentId assignmentId2 =
        AssignmentId.builder()
            .populationName("aaa")
            .taskId(111)
            .iterationId(9)
            .attemptId(0)
            .assignmentId("assignment-2")
            .build();
    AssignmentId assignmentId3 =
        AssignmentId.builder()
            .populationName("aaa")
            .taskId(111)
            .iterationId(9)
            .attemptId(0)
            .assignmentId("assignment-3")
            .build();
    int updated =
        dao.batchUpdateAssignmentStatus(
            List.of(assignmentId1, assignmentId2, assignmentId3),
            AssignmentEntity.Status.ASSIGNED,
            AssignmentEntity.Status.LOCAL_COMPLETED);

    // assert
    assertThat(updated).isEqualTo(2);
    assertThat(queryAssignmentStatusById(assignmentId1).getStatus())
        .isEqualTo(AssignmentEntity.Status.LOCAL_COMPLETED);
    assertThat(queryAssignmentStatusById(assignmentId2).getStatus())
        .isEqualTo(AssignmentEntity.Status.LOCAL_COMPLETED);
    assertThat(queryAssignmentStatusById(assignmentId3).getStatus())
        .isEqualTo(AssignmentEntity.Status.ASSIGNED);
  }

  @Test
  public void testCreateBatchAndUpdateAssignmentStatus_Succeeded() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction, /* populationName= */ "aaa", /* taskId= */ 111, /* status= */ 0);
              insertIteration(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= */ Status.COLLECTING.code(),
                  /* reportGoal= */ 300);
              insertAssignment(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-1",
                  /* createdTime */ TS_NOW,
                  /* active */ true,
                  /* batchId */ null,
                  /* withStatusHistory */ false);
              insertAssignmentStatusHist(
                  /* transaction= */ transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* assignmentId= */ "assignment-1",
                  /* statusId= */ 1,
                  /* status= */ LOCAL_COMPLETED,
                  /* batchId= */ null,
                  /* createdTime= */ TS_NOW);
              insertAssignment(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-2",
                  /* createdTime */ TS_NOW,
                  /* active */ true,
                  /* batchId */ null,
                  /* withStatusHistory */ false);
              insertAssignmentStatusHist(
                  /* transaction= */ transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* assignmentId= */ "assignment-2",
                  /* statusId= */ 1,
                  /* status= */ LOCAL_COMPLETED,
                  /* batchId= */ null,
                  /* createdTime= */ TS_NOW);
              return null;
            });

    // act
    AssignmentId assignmentId1 =
        AssignmentId.builder()
            .populationName("aaa")
            .taskId(111)
            .iterationId(9)
            .attemptId(0)
            .assignmentId("assignment-1")
            .build();
    AssignmentId assignmentId2 =
        AssignmentId.builder()
            .populationName("aaa")
            .taskId(111)
            .iterationId(9)
            .attemptId(0)
            .assignmentId("assignment-2")
            .build();
    IterationEntity iterationEntity =
        createIterationEntity(
            /* populationName= */ "aaa",
            /* taskId= */ 111,
            /* iterationId= */ 9,
            /* status= */ Status.COLLECTING,
            /* reportGoal= */ 300);
    String uuid = UUID.randomUUID().toString();
    AggregationBatchEntity batchEntity =
        AggregationBatchEntity.builder()
            .batchId(uuid)
            .populationName(iterationEntity.getPopulationName())
            .taskId(iterationEntity.getTaskId())
            .attemptId(iterationEntity.getAttemptId())
            .iterationId(iterationEntity.getIterationId())
            .build();
    boolean updated =
        dao.createBatchAndUpdateAssignments(
            List.of(assignmentId1, assignmentId2),
            iterationEntity,
            AssignmentEntity.Status.LOCAL_COMPLETED,
            AssignmentEntity.Status.UPLOAD_COMPLETED,
            uuid,
            "abc");

    // assert
    assertThat(updated).isEqualTo(true);
    assertThat(queryAssignmentStatusById(assignmentId1).getStatus())
        .isEqualTo(AssignmentEntity.Status.UPLOAD_COMPLETED);
    assertThat(queryAssignmentStatusById(assignmentId2).getStatus())
        .isEqualTo(AssignmentEntity.Status.UPLOAD_COMPLETED);
    assertThat(queryBatchStatus(batchEntity).getStatus())
        .isEqualTo(AggregationBatchEntity.Status.FULL);
  }

  @Test
  public void testCreateBatchAndUpdateAssignmentStatus_HistoryMismatch() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction, /* populationName= */ "aaa", /* taskId= */ 111, /* status= */ 0);
              insertIteration(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= */ Status.COLLECTING.code(),
                  /* reportGoal= */ 300);
              insertAssignment(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-1",
                  /* createdTime */ TS_NOW,
                  /* active */ true,
                  /* batchId */ null,
                  /* withStatusHistory */ true);
              return null;
            });

    // act
    AssignmentId assignmentId1 =
        AssignmentId.builder()
            .populationName("aaa")
            .taskId(111)
            .iterationId(9)
            .attemptId(0)
            .assignmentId("assignment-1")
            .build();
    IterationEntity iterationEntity =
        createIterationEntity(
            /* populationName= */ "aaa",
            /* taskId= */ 111,
            /* iterationId= */ 9,
            /* status= */ Status.COLLECTING,
            /* reportGoal= */ 300);
    String uuid = UUID.randomUUID().toString();
    AggregationBatchEntity batchEntity =
        AggregationBatchEntity.builder()
            .batchId(uuid)
            .populationName(iterationEntity.getPopulationName())
            .taskId(iterationEntity.getTaskId())
            .attemptId(iterationEntity.getAttemptId())
            .iterationId(iterationEntity.getIterationId())
            .build();
    boolean updated =
        dao.createBatchAndUpdateAssignments(
            List.of(assignmentId1),
            iterationEntity,
            AssignmentEntity.Status.LOCAL_COMPLETED,
            AssignmentEntity.Status.UPLOAD_COMPLETED,
            uuid,
            "abc");

    // assert
    assertThat(updated).isEqualTo(false);
    assertThat(queryAssignmentStatusById(assignmentId1).getStatus())
        .isEqualTo(AssignmentEntity.Status.ASSIGNED);
    assertThrows(IllegalStateException.class, () -> queryBatchStatus(batchEntity));
  }

  @Test
  public void testUpdateAssignmentStatus_AssignmentNotFound() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction, /* populationName= */ "aaa", /* taskId= */ 111, /* status= */ 0);
              insertIteration(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= */ 0,
                  /* reportGoal= */ 300);
              insertAssignment(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-1",
                  /* createdTime */ TS_NOW,
                  /* active */ true,
                  /* batchId */ null,
                  /* withStatusHistory */ true);
              return null;
            });

    // act
    AssignmentId assignmentId =
        AssignmentId.builder()
            .populationName("aaa")
            .taskId(111)
            .iterationId(9)
            .attemptId(0)
            .assignmentId("assignment-2")
            .build();
    boolean updated =
        dao.updateAssignmentStatus(
            assignmentId,
            AssignmentEntity.Status.ASSIGNED,
            AssignmentEntity.Status.LOCAL_COMPLETED);

    // assert
    assertThat(updated).isFalse();
    assertThat(
            queryAssignmentStatusById(assignmentId.toBuilder().assignmentId("assignment-1").build())
                .getStatus())
        .isEqualTo(AssignmentEntity.Status.ASSIGNED);
  }

  @Test
  public void testUpdateAssignmentStatus_StatusMismatch() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction, /* populationName= */ "aaa", /* taskId= */ 111, /* status= */ 0);
              insertIteration(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= */ 0,
                  /* reportGoal= */ 300);
              insertAssignment(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-1",
                  /* createdTime */ TS_NOW,
                  /* active */ true,
                  /* batchId */ null,
                  /* withStatusHistory */ true);
              return null;
            });

    // act
    AssignmentId assignmentId =
        AssignmentId.builder()
            .populationName("aaa")
            .taskId(111)
            .iterationId(9)
            .attemptId(0)
            .assignmentId("assignment-1")
            .build();
    boolean updated =
        dao.updateAssignmentStatus(
            assignmentId,
            AssignmentEntity.Status.LOCAL_COMPLETED,
            AssignmentEntity.Status.UPLOAD_COMPLETED);

    // assert
    assertThat(updated).isFalse();
    assertThat(
            queryAssignmentStatusById(assignmentId.toBuilder().assignmentId("assignment-1").build())
                .getStatus())
        .isEqualTo(AssignmentEntity.Status.ASSIGNED);
  }

  @Test
  public void testUpdateAssignmentStatus_AssignmentTableStatusMismatch_NoChanngeInDB() {
    // scenario:
    //  assigment table: assigned
    //  status    table: local_completed
    //  update from local_completed to upload_completed
    // This scenario should not happen.

    // arrange
    ListAppender<ILoggingEvent> listAppender = prepairListAppender();
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction, /* populationName= */ "aaa", /* taskId= */ 111, /* status= */ 0);
              insertIteration(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= */ 0,
                  /* reportGoal= */ 300);
              insertAssignment(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-1",
                  /* createdTime */ TS_NOW,
                  /* active */ true,
                  /* batchId */ null,
                  /* withStatusHistory */ false);
              insertAssignmentStatusHist(
                  /* transaction= */ transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* assignmentId= */ "assignment-1",
                  /* statusId= */ 1,
                  /* status= */ LOCAL_COMPLETED,
                  /* batchId= */ null,
                  /* createdTime= */ TS_NOW);

              return null;
            });

    // act
    AssignmentId assignmentId =
        AssignmentId.builder()
            .populationName("aaa")
            .taskId(111)
            .iterationId(9)
            .attemptId(0)
            .assignmentId("assignment-1")
            .build();
    boolean updated =
        dao.updateAssignmentStatus(
            assignmentId,
            AssignmentEntity.Status.LOCAL_COMPLETED,
            AssignmentEntity.Status.UPLOAD_COMPLETED);

    // assert
    assertThat(updated).isFalse();
    assertThat(queryAssignmentStatusById(assignmentId).getStatus())
        .isEqualTo(AssignmentEntity.Status.ASSIGNED);
    assertThat(queryStatusHistories(assignmentId)).isEqualTo(Arrays.asList(LOCAL_COMPLETED));
    List<ILoggingEvent> logsList = listAppender.list;
    assertThat(logsList.size()).isEqualTo(1);
    assertThat(logsList.get(0).getFormattedMessage().contains("Failed to update status.")).isTrue();
  }

  @Test
  public void testUpdateAssignmentStatus_StatusTableStatusMismatch_NoChanngeInDB() {
    // scenario:
    //  assigment table: assigned
    //  status    table:
    //                  1. assigned
    //                  2. local_completed
    //  update from assigned to local_completed
    // This scenario should not happen.

    // arrange
    ListAppender<ILoggingEvent> listAppender = prepairListAppender();
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction, /* populationName= */ "aaa", /* taskId= */ 111, /* status= */ 0);
              insertIteration(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= */ 0,
                  /* reportGoal= */ 300);
              insertAssignment(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-1",
                  /* createdTime */ TS_NOW,
                  /* active */ true,
                  /* batchId */ null,
                  /* withStatusHistory */ false);
              insertAssignmentStatusHist(
                  /* transaction= */ transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* assignmentId= */ "assignment-1",
                  /* statusId= */ 1,
                  /* status= */ ASSIGNED,
                  /* batchId= */ null,
                  /* createdTime= */ TS_NOW);

              insertAssignmentStatusHist(
                  /* transaction= */ transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* assignmentId= */ "assignment-1",
                  /* statusId= */ 2,
                  /* status= */ LOCAL_COMPLETED,
                  /* batchId= */ null,
                  /* createdTime= */ TS_NOW);

              return null;
            });

    // act
    AssignmentId assignmentId =
        AssignmentId.builder()
            .populationName("aaa")
            .taskId(111)
            .iterationId(9)
            .attemptId(0)
            .assignmentId("assignment-1")
            .build();
    boolean updated =
        dao.updateAssignmentStatus(
            assignmentId,
            AssignmentEntity.Status.ASSIGNED,
            AssignmentEntity.Status.LOCAL_COMPLETED);

    // assert
    assertThat(updated).isFalse();
    assertThat(queryAssignmentStatusById(assignmentId).getStatus())
        .isEqualTo(AssignmentEntity.Status.ASSIGNED);
    assertThat(queryStatusHistories(assignmentId))
        .isEqualTo(Arrays.asList(ASSIGNED, LOCAL_COMPLETED));
    List<ILoggingEvent> logsList = listAppender.list;
    assertThat(logsList.size()).isEqualTo(1);
    assertThat(logsList.get(0).getFormattedMessage().contains("Failed to update status")).isTrue();
  }

  @Test
  public void testQueryAssignmentIdsOfStatus_LastStatusCount_NotTimeout() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction, /* populationName= */ "aaa", /* taskId= */ 111, /* status= */ 0);
              insertIteration(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= */ 0,
                  /* reportGoal= */ 300);
              insertAssignment(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-1",
                  /* createdTime */ toTs(NOW.minusSeconds(60)),
                  /* active */ false,
                  /* batchId */ null,
                  /* withStatusHistory */ false);
              insertAssignmentStatusHist(
                  /* transaction= */ transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* assignmentId= */ "assignment-1",
                  /* statusId= */ 1,
                  /* status= */ ASSIGNED,
                  /* batchId= */ null,
                  /* createdTime= */ toTs(NOW.minusSeconds(60)));

              insertAssignmentStatusHist(
                  /* transaction= */ transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* assignmentId= */ "assignment-1",
                  /* statusId= */ 2,
                  /* status= */ LOCAL_COMPLETED,
                  /* batchId= */ null,
                  /* createdTime= */ toTs(NOW.minusSeconds(30)));

              return null;
            });
    // act
    List<String> result =
        dao.queryAssignmentIdsOfStatus(
            IterationId.builder()
                .populationName("aaa")
                .taskId(111)
                .iterationId(9)
                .attemptId(0)
                .build(),
            AssignmentEntity.Status.LOCAL_COMPLETED,
            NOW.minusSeconds(40));

    // assert
    assertThat(result).isEqualTo(Arrays.asList());
  }

  @Test
  public void testQueryAssignmentIdsOfStatus_LastStatusCount_Timout() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction, /* populationName= */ "aaa", /* taskId= */ 111, /* status= */ 0);
              insertIteration(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= */ 0,
                  /* reportGoal= */ 300);
              insertAssignment(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-1",
                  /* createdTime */ toTs(NOW.minusSeconds(60)),
                  /* active */ false,
                  /* batchId */ null,
                  /* withStatusHistory */ false);
              insertAssignmentStatusHist(
                  /* transaction= */ transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* assignmentId= */ "assignment-1",
                  /* statusId= */ 1,
                  /* status= */ ASSIGNED,
                  /* batchId= */ null,
                  /* createdTime= */ toTs(NOW.minusSeconds(60)));

              insertAssignmentStatusHist(
                  /* transaction= */ transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* assignmentId= */ "assignment-1",
                  /* statusId= */ 2,
                  /* status= */ LOCAL_COMPLETED,
                  /* batchId= */ null,
                  /* createdTime= */ toTs(NOW.minusSeconds(30)));

              return null;
            });
    // act
    List<String> result =
        dao.queryAssignmentIdsOfStatus(
            IterationId.builder()
                .populationName("aaa")
                .taskId(111)
                .iterationId(9)
                .attemptId(0)
                .build(),
            AssignmentEntity.Status.LOCAL_COMPLETED,
            NOW.minusSeconds(20));

    // assert
    assertThat(result).isEqualTo(Arrays.asList("assignment-1"));
  }

  @Test
  public void testQueryAssignmentIdsOfStatus_LastStatusCount_StatusNotMatch() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction, /* populationName= */ "aaa", /* taskId= */ 111, /* status= */ 0);
              insertIteration(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= */ 0,
                  /* reportGoal= */ 300);
              insertAssignment(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-1",
                  /* createdTime */ toTs(NOW.minusSeconds(60)),
                  /* active */ false,
                  /* batchId */ null,
                  /* withStatusHistory */ false);
              insertAssignmentStatusHist(
                  /* transaction= */ transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* assignmentId= */ "assignment-1",
                  /* statusId= */ 1,
                  /* status= */ ASSIGNED,
                  /* batchId= */ null,
                  /* createdTime= */ toTs(NOW.minusSeconds(60)));

              insertAssignmentStatusHist(
                  /* transaction= */ transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* assignmentId= */ "assignment-1",
                  /* statusId= */ 2,
                  /* status= */ ASSIGNED,
                  /* batchId= */ null,
                  /* createdTime= */ toTs(NOW.minusSeconds(30)));

              return null;
            });
    // act
    List<String> result =
        dao.queryAssignmentIdsOfStatus(
            IterationId.builder()
                .populationName("aaa")
                .taskId(111)
                .iterationId(9)
                .attemptId(0)
                .build(),
            AssignmentEntity.Status.LOCAL_COMPLETED,
            NOW.minusSeconds(20));

    // assert
    assertThat(result).isEqualTo(Arrays.asList());
  }

  @Test
  public void testQueryAssignmentIdsOfStatus_MoreThanOneReturned() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction, /* populationName= */ "aaa", /* taskId= */ 111, /* status= */ 0);
              insertIteration(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= */ 0,
                  /* reportGoal= */ 300);
              insertAssignment(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-1",
                  /* createdTime */ toTs(NOW.minusSeconds(60)),
                  /* active */ true,
                  /* batchId */ null,
                  /* withStatusHistory */ true);
              insertAssignment(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-2",
                  /* createdTime */ toTs(NOW.minusSeconds(50)),
                  /* active */ true,
                  /* batchId */ null,
                  /* withStatusHistory */ true);

              return null;
            });
    // act
    List<String> result =
        dao.queryAssignmentIdsOfStatus(
            IterationId.builder()
                .populationName("aaa")
                .taskId(111)
                .iterationId(9)
                .attemptId(0)
                .build(),
            AssignmentEntity.Status.ASSIGNED,
            NOW.minusSeconds(40));

    // assert
    assertThat(result).isEqualTo(Arrays.asList("assignment-1", "assignment-2"));
  }

  @Test
  public void testQueryAssignmentIdsOfStatus_IterationMismatch() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction, /* populationName= */ "aaa", /* taskId= */ 111, /* status= */ 0);
              insertIteration(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= */ 0,
                  /* reportGoal= */ 300);
              insertAssignment(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-1",
                  /* createdTime */ toTs(NOW.minusSeconds(60)),
                  /* active */ true,
                  /* batchId */ null,
                  /* withStatusHistory */ true);
              insertAssignment(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-2",
                  /* createdTime */ toTs(NOW.minusSeconds(50)),
                  /* active */ true,
                  /* batchId */ null,
                  /* withStatusHistory */ true);

              return null;
            });
    // act
    List<String> result =
        dao.queryAssignmentIdsOfStatus(
            IterationId.builder()
                .populationName("aaa")
                .taskId(111)
                .iterationId(8)
                .attemptId(0)
                .build(),
            AssignmentEntity.Status.ASSIGNED,
            NOW.minusSeconds(40));

    // assert
    assertThat(result).isEqualTo(Arrays.asList());
  }

  @Test
  public void testQueryAssignmentIdsOfStatusAndBatch_NullBatch_LastStatusCount() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction, /* populationName= */ "aaa", /* taskId= */ 111, /* status= */ 0);
              insertIteration(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= */ 0,
                  /* reportGoal= */ 300);
              insertAssignment(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-1",
                  /* createdTime */ toTs(NOW),
                  /* active */ false,
                  /* batchId */ null,
                  /* withStatusHistory */ false);
              insertAssignmentStatusHist(
                  /* transaction= */ transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* assignmentId= */ "assignment-1",
                  /* statusId= */ 1,
                  /* status= */ ASSIGNED,
                  /* batchId= */ null,
                  /* createdTime= */ toTs(NOW));

              insertAssignmentStatusHist(
                  /* transaction= */ transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* assignmentId= */ "assignment-1",
                  /* statusId= */ 2,
                  /* status= */ LOCAL_COMPLETED,
                  /* batchId= */ null,
                  /* createdTime= */ toTs(NOW));

              return null;
            });
    // act
    List<String> result =
        dao.queryAssignmentIdsOfStatus(
            IterationId.builder()
                .populationName("aaa")
                .taskId(111)
                .iterationId(9)
                .attemptId(0)
                .build(),
            AssignmentEntity.Status.LOCAL_COMPLETED,
            Optional.empty());

    // assert
    assertThat(result).isEqualTo(Arrays.asList("assignment-1"));
  }

  @Test
  public void testQueryAssignmentIdsOfStatusAndBatch_LastStatusCount() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction, /* populationName= */ "aaa", /* taskId= */ 111, /* status= */ 0);
              insertIteration(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= */ 0,
                  /* reportGoal= */ 300);
              insertAssignment(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-1",
                  /* createdTime */ toTs(NOW),
                  /* active */ false,
                  /* batchId */ "batch",
                  /* withStatusHistory */ false);
              insertAssignmentStatusHist(
                  /* transaction= */ transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* assignmentId= */ "assignment-1",
                  /* statusId= */ 1,
                  /* status= */ ASSIGNED,
                  /* batchId= */ "batch",
                  /* createdTime= */ toTs(NOW));

              insertAssignmentStatusHist(
                  /* transaction= */ transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* assignmentId= */ "assignment-1",
                  /* statusId= */ 2,
                  /* status= */ LOCAL_COMPLETED,
                  /* batchId= */ "batch",
                  /* createdTime= */ toTs(NOW));

              return null;
            });
    // act
    List<String> result =
        dao.queryAssignmentIdsOfStatus(
            IterationId.builder()
                .populationName("aaa")
                .taskId(111)
                .iterationId(9)
                .attemptId(0)
                .build(),
            AssignmentEntity.Status.LOCAL_COMPLETED,
            Optional.of("batch"));

    // assert
    assertThat(result).isEqualTo(Arrays.asList("assignment-1"));
  }

  @Test
  public void testQueryAssignmentIdsOfStatusAndBatch_NullBatchMultiple() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction, /* populationName= */ "aaa", /* taskId= */ 111, /* status= */ 0);
              insertIteration(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= */ 0,
                  /* reportGoal= */ 300);
              insertAssignment(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-1",
                  /* createdTime */ toTs(NOW),
                  /* active */ true,
                  /* batchId */ null,
                  /* withStatusHistory */ true);
              insertAssignment(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-2",
                  /* createdTime */ toTs(NOW),
                  /* active */ true,
                  /* batchId */ null,
                  /* withStatusHistory */ true);
              insertAssignment(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-3",
                  /* createdTime */ toTs(NOW),
                  /* active */ true,
                  /* batchId */ "batch",
                  /* withStatusHistory */ true);
              return null;
            });
    // act
    List<String> result =
        dao.queryAssignmentIdsOfStatus(
            IterationId.builder()
                .populationName("aaa")
                .taskId(111)
                .iterationId(9)
                .attemptId(0)
                .build(),
            AssignmentEntity.Status.ASSIGNED,
            Optional.empty());

    // assert
    assertThat(result).isEqualTo(Arrays.asList("assignment-1", "assignment-2"));
  }

  @Test
  public void testQueryAssignmentIdsOfStatusAndBatch_Multiple() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction, /* populationName= */ "aaa", /* taskId= */ 111, /* status= */ 0);
              insertIteration(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= */ 0,
                  /* reportGoal= */ 300);
              insertAssignment(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-1",
                  /* createdTime */ toTs(NOW),
                  /* active */ true,
                  /* batchId */ "batch",
                  /* withStatusHistory */ true);
              insertAssignment(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-2",
                  /* createdTime */ toTs(NOW),
                  /* active */ true,
                  /* batchId */ "batch",
                  /* withStatusHistory */ true);
              insertAssignment(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-3",
                  /* createdTime */ toTs(NOW),
                  /* active */ true,
                  /* batchId */ "batch3",
                  /* withStatusHistory */ true);
              return null;
            });
    // act
    List<String> result =
        dao.queryAssignmentIdsOfStatus(
            IterationId.builder()
                .populationName("aaa")
                .taskId(111)
                .iterationId(9)
                .attemptId(0)
                .build(),
            AssignmentEntity.Status.ASSIGNED,
            Optional.of("batch"));

    // assert
    assertThat(result).isEqualTo(Arrays.asList("assignment-1", "assignment-2"));
  }

  @Test
  public void testQueryAssignmentIdsOfStatusAndBatch_IterationMismatch() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertTask(
                  transaction, /* populationName= */ "bbb", /* taskId= */ 111, /* status= */ 0);
              insertIteration(
                  transaction,
                  /* populationName= */ "bbb",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* status= */ 0,
                  /* reportGoal= */ 300);
              insertAssignment(
                  transaction,
                  /* populationName= */ "bbb",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* sessionId */ "assignment-1",
                  /* createdTime */ toTs(NOW),
                  /* active */ true,
                  /* batchId */ "batch",
                  /* withStatusHistory */ true);
              return null;
            });
    // act
    List<String> result =
        dao.queryAssignmentIdsOfStatus(
            IterationId.builder()
                .populationName("aaa")
                .taskId(111)
                .iterationId(9)
                .attemptId(0)
                .build(),
            AssignmentEntity.Status.ASSIGNED,
            Optional.of("batch"));

    // assert
    assertThat(result).isEqualTo(List.of());
  }

  private void insertTask(
      TransactionContext transaction, String populationName, long taskId, long status) {
    String insertTask =
        "INSERT INTO Task(PopulationName, TaskId, TotalIteration, MinAggregationSize,"
            + " MaxAggregationSize, Status, MaxParallel, CorrelationId, MinClientVersion,"
            + " MaxClientVersion, Info) \n"
            + "VALUES(@populationName, @taskId, @totalIteration, @minAggregationSize,"
            + " @maxAggregationSize, @status, @maxParallel, @correlationId, @minClientVersion,"
            + " @maxClientVersion, @info)";

    transaction.executeUpdate(
        Statement.newBuilder(insertTask)
            .bind("populationName")
            .to(populationName)
            .bind("taskId")
            .to(taskId)
            .bind("totalIteration")
            .to(222)
            .bind("minAggregationSize")
            .to(333)
            .bind("maxAggregationSize")
            .to(444)
            .bind("status")
            .to(status)
            .bind("maxParallel")
            .to(555)
            .bind("correlationId")
            .to("correlation")
            .bind("minClientVersion")
            .to("0.0.0.0")
            .bind("maxClientVersion")
            .to("3.0.0.0")
            .bind("info")
            .to(TASK_INFO)
            .build());
  }

  private void insertIteration(
      TransactionContext transaction,
      String populationName,
      long taskId,
      long iterationId,
      long status,
      long reportGoal) {
    String insertIteration =
        "INSERT INTO Iteration(PopulationName, TaskId, IterationId, AttemptId, Status,"
            + " BaseIterationId, BaseOnResultId, ReportGoal, ResultId, Info, AggregationLevel)"
            + " VALUES(@populationName, @taskId, @iterationId, @attemptId, @status,"
            + " @baseIterationId, @baseOnResultId, @reportGoal, @resultId, @info,"
            + " @aggregationLevel)";
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
            .to(ITERATION_INFO)
            .bind("aggregationLevel")
            .to(1)
            .build());
  }

  private void insertAssignment(
      TransactionContext transaction,
      String populationName,
      long taskId,
      long iterationId,
      String assignmentId,
      Timestamp createdTime,
      boolean active,
      String batchId,
      boolean withStatusHistory) {
    long status = active ? ASSIGNED : LOCAL_FAILED;
    String insertIteration =
        "INSERT INTO Assignment(PopulationName, TaskId, IterationId, AttemptId, SessionId,\n"
            + " CorrelationId, Status, CreatedTime, BatchId)\n"
            + " VALUES(@populationName, @taskId, @iterationId, @attemptId, @sessionId,\n"
            + " @correlationId, @status, @createdTime, @batchId)\n";
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
            .bind("batchId")
            .to(batchId)
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
        /* batchId= */ batchId,
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
      String batchId,
      Timestamp createdTime) {
    String insertIteration =
        "INSERT INTO AssignmentStatusHistory(PopulationName, TaskId, IterationId, AttemptId,"
            + " SessionId,\n"
            + " StatusId, BatchId, CreatedTime, Status)\n"
            + " VALUES(@populationName, @taskId, @iterationId, @attemptId, @sessionId,\n"
            + " @statusId, @batchId, @createdTime, @status)\n";
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
            .bind("batchId")
            .to(batchId)
            .bind("createdTime")
            .to(createdTime)
            .bind("statusId")
            .to(statusId)
            .bind("status")
            .to(status)
            .build());
  }

  private List<Long> queryStatusHistories(AssignmentId id) {
    Statement statement =
        Statement.newBuilder(
                "SELECT * FROM AssignmentStatusHistory WHERE PopulationName = @populationName AND\n"
                    + " TaskId=@taskId AND IterationId=@iterationId AND AttemptId=@attemptId AND\n"
                    + " SessionId=@sessionId ORDER BY StatusId\n")
            .bind("populationName")
            .to(id.getPopulationName())
            .bind("taskId")
            .to(id.getTaskId())
            .bind("iterationId")
            .to(id.getIterationId())
            .bind("attemptId")
            .to(id.getAttemptId())
            .bind("sessionId")
            .to(id.getAssignmentId())
            .build();

    try (ResultSet resultSet = dbClient.singleUse().executeQuery(statement)) {
      List<Long> status = new ArrayList();

      while (resultSet.next()) {
        status.add(resultSet.getLong("Status"));
      }
      return status;
    }
  }

  private AssignmentEntity queryAssignmentStatusById(AssignmentId id) {
    try (ResultSet resultSet =
        dbClient
            .singleUse()
            .read(
                "Assignment",
                KeySet.newBuilder()
                    .addKey(
                        Key.of(
                            id.getPopulationName(),
                            id.getTaskId(),
                            id.getIterationId(),
                            id.getAttemptId(),
                            id.getAssignmentId()))
                    .build(), // Read all rows in a table.
                Arrays.asList("Status", "BatchId"))) {
      while (resultSet.next()) {
        return AssignmentEntity.builder()
            .status(AssignmentEntity.Status.fromCode(resultSet.getLong("Status")))
            .batchId(resultSet.isNull("BatchId") ? null : resultSet.getString("BatchId"))
            .build();
      }
      throw new IllegalStateException("Failed to get assignment status.");
    }
  }

  private AggregationBatchEntity queryBatchStatus(AggregationBatchEntity entity) {
    try (ResultSet resultSet =
        dbClient
            .singleUse()
            .read(
                "AggregationBatch",
                KeySet.newBuilder()
                    .addKey(
                        Key.of(
                            entity.getPopulationName(),
                            entity.getTaskId(),
                            entity.getIterationId(),
                            entity.getAttemptId(),
                            entity.getBatchId()))
                    .build(), // Read all rows in a table.
                Arrays.asList("Status"))) {
      while (resultSet.next()) {
        return AggregationBatchEntity.builder()
            .status(AggregationBatchEntity.Status.fromCode(resultSet.getLong("Status")))
            .build();
      }
      throw new IllegalStateException("Failed to get batch status.");
    }
  }

  private AssignmentId getId(AssignmentEntity entity) {
    return AssignmentId.builder()
        .populationName(entity.getPopulationName())
        .taskId(entity.getTaskId())
        .iterationId(entity.getIterationId())
        .attemptId(entity.getAttemptId())
        .assignmentId(entity.getSessionId())
        .build();
  }

  private ListAppender<ILoggingEvent> prepairListAppender() {
    Logger logger = (Logger) LoggerFactory.getLogger(AssignmentSpannerDao.class);

    // create and start a ListAppender
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();

    // add the appender to the logger
    logger.addAppender(listAppender);
    return listAppender;
  }

  private Timestamp toTs(Instant instant) {
    return Timestamp.ofTimeSecondsAndNanos(instant.getEpochSecond(), instant.getNano());
  }

  private IterationEntity createIterationEntity(
      String populationName, long taskId, long iterationId, Status status, long reportGoal) {
    return IterationEntity.builder()
        .populationName(populationName)
        .taskId(taskId)
        .iterationId(iterationId)
        .attemptId(0)
        .reportGoal(reportGoal)
        .status(status)
        .baseIterationId(iterationId - 1)
        .baseOnResultId(iterationId - 1)
        .resultId(iterationId)
        .aggregationLevel(0)
        .build();
  }
}
