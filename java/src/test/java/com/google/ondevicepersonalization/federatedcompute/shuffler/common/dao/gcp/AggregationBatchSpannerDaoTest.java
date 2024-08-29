/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.gcp;

import static com.google.common.truth.Truth.assertThat;
import static com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AggregationBatchEntity.Status.FULL;
import static com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AggregationBatchEntity.Status.PUBLISH_COMPLETED;
import static com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AggregationBatchEntity.Status.UPLOAD_COMPLETED;
import static org.junit.Assert.assertTrue;

import com.google.cloud.Timestamp;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.TransactionContext;
import com.google.cloud.spanner.Value;
import com.google.common.collect.ImmutableList;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AggregationBatchDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AggregationBatchEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AggregationBatchId;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.IterationEntity;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.InstantSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

@RunWith(JUnit4.class)
public class AggregationBatchSpannerDaoTest {
  private static final String PROJECT_ID = "spanner-emulator";
  private static final String INSTANCE = "fcp-task-unittest";
  private static final String DB_NAME = "test-db-dao";
  private static final String SDL_FILE_PATH = "shuffler/spanner/task_database.sdl";
  private static final Value TASK_INFO =
      Value.json("{\"trafficWeight\": \"1\", \"trainingInfo\": {}}");
  private static final Value ITERATION_INFO =
      Value.json("{\"taskInfo\":{\"trafficWeight\":\"1\",\"trainingInfo\":{}}}");
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
          .info(ITERATION_INFO.getAsString())
          .aggregationLevel(0)
          .minClientVersion("0")
          .maxAggregationSize(444)
          .maxClientVersion("99999999")
          .build();

  // GMT time.
  private static Instant NOW = Instant.parse("2023-09-01T00:00:00Z");
  private static InstantSource instanceSource = InstantSource.fixed(NOW);
  private static SpannerTestHarness.Connection spannerEmulatorConnection;

  private DatabaseClient dbClient;
  @InjectMocks AggregationBatchDao dao;

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
    this.dao = new AggregationBatchSpannerDao(dbClient, instanceSource);
    MockitoAnnotations.initMocks(this);
  }

  @After
  public void teardown() throws SQLException {
    deleteDatabase();
  }

  @Test
  public void testQuerySumOfAggregationBatchesOfStatus_success() {
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
              insertBatch(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* batchId= */ "batch-1",
                  /* aggregationLevel= */ 0,
                  /* status= */ PUBLISH_COMPLETED,
                  /* batchSize= */ 50,
                  /* createdByPartition */ "abc",
                  /* createdTime */ toTs(NOW),
                  /* aggregatedBy */ "batch-a",
                  /* withStatusHistory */ true);
              insertBatch(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* batchId= */ "batch-2",
                  /* aggregationLevel= */ 0,
                  /* status= */ PUBLISH_COMPLETED,
                  /* batchSize= */ 25,
                  /* createdByPartition */ "abc",
                  /* createdTime */ toTs(NOW),
                  /* aggregatedBy */ "batch-a",
                  /* withStatusHistory */ true);
              insertIteration(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 10,
                  /* status= */ 0,
                  /* reportGoal= */ 300);
              insertBatch(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 10, // Different iteration
                  /* batchId= */ "batch-3",
                  /* aggregationLevel= */ 0,
                  /* status= */ PUBLISH_COMPLETED,
                  /* batchSize= */ 25,
                  /* createdByPartition */ "abc",
                  /* createdTime */ toTs(NOW),
                  /* aggregatedBy */ "batch-a",
                  /* withStatusHistory */ true);
              return null;
            });
    // act
    long result =
        dao.querySumOfAggregationBatchesOfStatus(
            DEFAULT_ITERATION.toBuilder()
                .populationName("aaa")
                .taskId(111)
                .iterationId(9)
                .attemptId(0)
                .build(),
            0,
            List.of(PUBLISH_COMPLETED));

    // assert
    assertThat(result).isEqualTo(75);
  }

  @Test
  public void testQuerySumOfAggregationBatchesOfStatus_noMatch() {
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
              insertBatch(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* batchId= */ "batch-1",
                  /* aggregationLevel= */ 0,
                  /* status= */ PUBLISH_COMPLETED,
                  /* batchSize= */ 50,
                  /* createdByPartition */ "abc",
                  /* createdTime */ toTs(NOW),
                  /* aggregatedBy */ "batch-a",
                  /* withStatusHistory */ true);
              insertBatch(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* batchId= */ "batch-2",
                  /* aggregationLevel= */ 0,
                  /* status= */ PUBLISH_COMPLETED,
                  /* batchSize= */ 25,
                  /* createdByPartition */ "abc",
                  /* createdTime */ toTs(NOW),
                  /* aggregatedBy */ "batch-a",
                  /* withStatusHistory */ true);
              return null;
            });
    // act
    long result =
        dao.querySumOfAggregationBatchesOfStatus(
            DEFAULT_ITERATION.toBuilder()
                .populationName("aaa")
                .taskId(111)
                .iterationId(9)
                .attemptId(0)
                .build(),
            0,
            List.of(FULL));

    // assert
    assertThat(result).isEqualTo(0);
  }

  @Test
  public void testQuerySumOfAggregationBatchesOfStatus_matchMultiple() {
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
              insertBatch(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* batchId= */ "batch-1",
                  /* aggregationLevel= */ 0,
                  /* status= */ PUBLISH_COMPLETED,
                  /* batchSize= */ 50,
                  /* createdByPartition */ "abc",
                  /* createdTime */ toTs(NOW),
                  /* aggregatedBy */ "batch-a",
                  /* withStatusHistory */ true);
              insertBatch(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* batchId= */ "batch-2",
                  /* aggregationLevel= */ 0,
                  /* status= */ PUBLISH_COMPLETED,
                  /* batchSize= */ 25,
                  /* createdByPartition */ "abc",
                  /* createdTime */ toTs(NOW),
                  /* aggregatedBy */ "batch-a",
                  /* withStatusHistory */ true);
              insertBatch(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* batchId= */ "batch-3",
                  /* aggregationLevel= */ 0,
                  /* status= */ FULL,
                  /* batchSize= */ 25,
                  /* createdByPartition */ "abc",
                  /* createdTime */ toTs(NOW),
                  /* aggregatedBy */ "batch-a",
                  /* withStatusHistory */ true);
              return null;
            });
    // act
    long result =
        dao.querySumOfAggregationBatchesOfStatus(
            DEFAULT_ITERATION.toBuilder()
                .populationName("aaa")
                .taskId(111)
                .iterationId(9)
                .attemptId(0)
                .build(),
            0,
            List.of(FULL, PUBLISH_COMPLETED));

    // assert
    assertThat(result).isEqualTo(100);
  }

  @Test
  public void testQuerySumOfAggregationBatchesOfStatus_noMatchLevel() {
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
              insertBatch(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* batchId= */ "batch-1",
                  /* aggregationLevel= */ 0,
                  /* status= */ PUBLISH_COMPLETED,
                  /* batchSize= */ 50,
                  /* createdByPartition */ "abc",
                  /* createdTime */ toTs(NOW),
                  /* aggregatedBy */ "batch-a",
                  /* withStatusHistory */ true);
              insertBatch(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* batchId= */ "batch-2",
                  /* aggregationLevel= */ 0,
                  /* status= */ PUBLISH_COMPLETED,
                  /* batchSize= */ 25,
                  /* createdByPartition */ "abc",
                  /* createdTime */ toTs(NOW),
                  /* aggregatedBy */ "batch-a",
                  /* withStatusHistory */ true);
              return null;
            });
    // act
    long result =
        dao.querySumOfAggregationBatchesOfStatus(
            DEFAULT_ITERATION.toBuilder()
                .populationName("aaa")
                .taskId(111)
                .iterationId(9)
                .attemptId(0)
                .build(),
            1,
            List.of(PUBLISH_COMPLETED));

    // assert
    assertThat(result).isEqualTo(0);
  }

  @Test
  public void testQueryBatchIdsOfStatus_LastStatusCountSingle() {
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
              insertBatch(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* batchId= */ "batch-1",
                  /* aggregationLevel= */ 0,
                  /* status= */ PUBLISH_COMPLETED,
                  /* batchSize= */ 50,
                  /* createdByPartition */ "abc",
                  /* createdTime */ toTs(NOW),
                  /* aggregatedBy */ "batch-a",
                  /* withStatusHistory */ true);
              return null;
            });
    // act
    List<String> result =
        dao.queryAggregationBatchIdsOfStatus(
            DEFAULT_ITERATION.toBuilder()
                .populationName("aaa")
                .taskId(111)
                .iterationId(9)
                .attemptId(0)
                .build(),
            0,
            PUBLISH_COMPLETED,
            Optional.empty());

    // assert
    assertThat(result).isEqualTo(Arrays.asList("batch-1"));
  }

  @Test
  public void testQueryGetAggregationBatchById_Match() {
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
              insertBatch(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* batchId= */ "batch-1",
                  /* aggregationLevel= */ 0,
                  /* status= */ FULL,
                  /* batchSize= */ 50,
                  /* createdByPartition */ "abc",
                  /* createdTime */ toTs(NOW),
                  /* aggregatedBy */ "batch-a",
                  /* withStatusHistory */ true);
              return null;
            });

    AggregationBatchId batchId =
        AggregationBatchId.builder()
            .populationName("aaa")
            .taskId(111)
            .iterationId(9)
            .attemptId(0)
            .batchId("batch-1")
            .build();

    // act
    Optional<AggregationBatchEntity> result = dao.getAggregationBatchById(batchId);

    // assert
    AggregationBatchEntity entity =
        AggregationBatchEntity.builder()
            .populationName("aaa")
            .taskId(111)
            .iterationId(9)
            .attemptId(0)
            .batchId("batch-1")
            .status(FULL)
            .batchSize(50)
            .createdByPartition("abc")
            .aggregatedBy("batch-a")
            .build();
    assertThat(result.get()).isEqualTo(entity);
  }

  @Test
  public void testQueryGetAggregationBatchById_NoMatch() {
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
              insertBatch(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* batchId= */ "batch-1",
                  /* aggregationLevel= */ 0,
                  /* status= */ FULL,
                  /* batchSize= */ 50,
                  /* createdByPartition */ "abc",
                  /* createdTime */ toTs(NOW),
                  /* aggregatedBy */ "batch-a",
                  /* withStatusHistory */ true);
              return null;
            });

    AggregationBatchId batchId =
        AggregationBatchId.builder()
            .populationName("aaa")
            .taskId(111)
            .iterationId(9)
            .attemptId(0)
            .batchId("batch-2")
            .build();

    // act
    Optional<AggregationBatchEntity> result = dao.getAggregationBatchById(batchId);

    // assert
    assertTrue(result.isEmpty());
  }

  @Test
  public void testQueryBatchIdsOfStatus_LastStatusCount_NotMatch() {
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
              insertBatch(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* batchId= */ "batch-1",
                  /* aggregationLevel= */ 0,
                  /* status= */ FULL,
                  /* batchSize= */ 50,
                  /* createdByPartition */ "abc",
                  /* createdTime */ toTs(NOW),
                  /* aggregatedBy */ "batch-a",
                  /* withStatusHistory */ true);
              return null;
            });
    // act
    List<String> result =
        dao.queryAggregationBatchIdsOfStatus(
            DEFAULT_ITERATION.toBuilder()
                .populationName("aaa")
                .taskId(111)
                .iterationId(9)
                .attemptId(0)
                .build(),
            0,
            PUBLISH_COMPLETED,
            Optional.empty());

    // assert
    assertThat(result).isEqualTo(Arrays.asList());
  }

  @Test
  public void testQueryBatchIdsOfStatus_NoMatchLevel() {
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
              insertBatch(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* batchId= */ "batch-1",
                  /* aggregationLevel= */ 0,
                  /* status= */ PUBLISH_COMPLETED,
                  /* batchSize= */ 50,
                  /* createdByPartition */ "abc",
                  /* createdTime */ toTs(NOW),
                  /* aggregatedBy */ "batch-a",
                  /* withStatusHistory */ true);
              return null;
            });
    // act
    List<String> result =
        dao.queryAggregationBatchIdsOfStatus(
            DEFAULT_ITERATION.toBuilder()
                .populationName("aaa")
                .taskId(111)
                .iterationId(9)
                .attemptId(0)
                .build(),
            1,
            PUBLISH_COMPLETED,
            Optional.empty());

    // assert
    assertThat(result).isEqualTo(Arrays.asList());
  }

  @Test
  public void testQueryBatchIdsOfStatus_LastStatusCountMultiple() {
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
              insertBatch(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* batchId= */ "batch-1",
                  /* aggregationLevel= */ 0,
                  /* status= */ FULL,
                  /* batchSize= */ 50,
                  /* createdByPartition */ "abc",
                  /* createdTime */ toTs(NOW),
                  /* aggregatedBy */ "batch-a",
                  /* withStatusHistory */ true);
              insertBatch(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* batchId= */ "batch-2",
                  /* aggregationLevel= */ 0,
                  /* status= */ FULL,
                  /* batchSize= */ 50,
                  /* createdByPartition */ "abc",
                  /* createdTime */ toTs(NOW.minusSeconds(50)),
                  /* aggregatedBy */ "batch-a",
                  /* withStatusHistory */ true);
              return null;
            });
    // act
    List<String> result =
        dao.queryAggregationBatchIdsOfStatus(
            DEFAULT_ITERATION.toBuilder()
                .populationName("aaa")
                .taskId(111)
                .iterationId(9)
                .attemptId(0)
                .build(),
            0,
            FULL,
            Optional.empty());

    // assert
    assertThat(result).isEqualTo(Arrays.asList("batch-1", "batch-2"));
  }

  @Test
  public void testQueryBatchIdsOfStatus_LastStatusCount_IterationMismatch() {
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
              insertBatch(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* batchId= */ "batch-1",
                  /* aggregationLevel= */ 0,
                  /* status= */ FULL,
                  /* batchSize= */ 50,
                  /* createdByPartition */ "abc",
                  /* createdTime */ toTs(NOW),
                  /* aggregatedBy */ "batch-a",
                  /* withStatusHistory */ true);
              return null;
            });
    // act
    List<String> result =
        dao.queryAggregationBatchIdsOfStatus(
            DEFAULT_ITERATION.toBuilder()
                .populationName("aaa")
                .taskId(111)
                .iterationId(11)
                .attemptId(0)
                .build(),
            0,
            FULL,
            Optional.empty());

    // assert
    assertThat(result).isEqualTo(Arrays.asList());
  }

  @Test
  public void testQueryBatchIdsOfStatusByPartition_LastStatusCountSingle() {
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
              insertBatch(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* batchId= */ "batch-1",
                  /* aggregationLevel= */ 0,
                  /* status= */ PUBLISH_COMPLETED,
                  /* batchSize= */ 50,
                  /* createdByPartition */ "abc",
                  /* createdTime */ toTs(NOW),
                  /* aggregatedBy */ "batch-a",
                  /* withStatusHistory */ true);
              insertBatch(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* batchId= */ "batch-2",
                  /* aggregationLevel= */ 0,
                  /* status= */ PUBLISH_COMPLETED,
                  /* batchSize= */ 50,
                  /* createdByPartition */ "def",
                  /* createdTime */ toTs(NOW),
                  /* aggregatedBy */ "batch-a",
                  /* withStatusHistory */ true);
              return null;
            });
    // act
    List<String> result =
        dao.queryAggregationBatchIdsOfStatus(
            DEFAULT_ITERATION.toBuilder()
                .populationName("aaa")
                .taskId(111)
                .iterationId(9)
                .attemptId(0)
                .build(),
            0,
            PUBLISH_COMPLETED,
            Optional.of("abc"));

    // assert
    assertThat(result).isEqualTo(Arrays.asList("batch-1"));
  }

  @Test
  public void testQueryBatchIdsOfStatusByPartition_LastStatusCount_StatusNotMatch() {
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
              insertBatch(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* batchId= */ "batch-1",
                  /* aggregationLevel= */ 0,
                  /* status= */ FULL,
                  /* batchSize= */ 50,
                  /* createdByPartition */ "abc",
                  /* createdTime */ toTs(NOW),
                  /* aggregatedBy */ "batch-a",
                  /* withStatusHistory */ false);
              return null;
            });
    // act
    List<String> result =
        dao.queryAggregationBatchIdsOfStatus(
            DEFAULT_ITERATION.toBuilder()
                .populationName("aaa")
                .taskId(111)
                .iterationId(9)
                .attemptId(0)
                .build(),
            0,
            PUBLISH_COMPLETED,
            Optional.of("abc"));

    // assert
    assertThat(result).isEqualTo(Arrays.asList());
  }

  @Test
  public void testQueryBatchIdsOfStatusByPartition_LastStatusCount_PartitionNotMatch() {
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
              insertBatch(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* batchId= */ "batch-1",
                  /* aggregationLevel= */ 0,
                  /* status= */ PUBLISH_COMPLETED,
                  /* batchSize= */ 50,
                  /* createdByPartition */ "def",
                  /* createdTime */ toTs(NOW),
                  /* aggregatedBy */ "batch-a",
                  /* withStatusHistory */ true);
              return null;
            });
    // act
    List<String> result =
        dao.queryAggregationBatchIdsOfStatus(
            DEFAULT_ITERATION.toBuilder()
                .populationName("aaa")
                .taskId(111)
                .iterationId(9)
                .attemptId(0)
                .build(),
            0,
            PUBLISH_COMPLETED,
            Optional.of("abc"));

    // assert
    assertThat(result).isEqualTo(Arrays.asList());
  }

  @Test
  public void testQueryBatchIdsOfStatusByPartition_LastStatusCountMultiple() {
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
              insertBatch(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* batchId= */ "batch-1",
                  /* aggregationLevel= */ 0,
                  /* status= */ FULL,
                  /* batchSize= */ 50,
                  /* createdByPartition */ "abc",
                  /* createdTime */ toTs(NOW),
                  /* aggregatedBy */ "batch-a",
                  /* withStatusHistory */ true);
              insertBatch(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* batchId= */ "batch-2",
                  /* aggregationLevel= */ 0,
                  /* status= */ FULL,
                  /* batchSize= */ 50,
                  /* createdByPartition */ "abc",
                  /* createdTime */ toTs(NOW.minusSeconds(50)),
                  /* aggregatedBy */ "batch-a",
                  /* withStatusHistory */ true);
              return null;
            });
    // act
    List<String> result =
        dao.queryAggregationBatchIdsOfStatus(
            DEFAULT_ITERATION.toBuilder()
                .populationName("aaa")
                .taskId(111)
                .iterationId(9)
                .attemptId(0)
                .build(),
            0,
            FULL,
            Optional.of("abc"));

    // assert
    assertThat(result).isEqualTo(Arrays.asList("batch-1", "batch-2"));
  }

  @Test
  public void testQueryBatchIdsOfStatusByPartition_LastStatusCount_IterationMismatch() {
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
              insertBatch(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* batchId= */ "batch-1",
                  /* aggregationLevel= */ 0,
                  /* status= */ FULL,
                  /* batchSize= */ 50,
                  /* createdByPartition */ "abc",
                  /* createdTime */ toTs(NOW),
                  /* aggregatedBy */ "batch-a",
                  /* withStatusHistory */ true);
              return null;
            });
    // act
    List<String> result =
        dao.queryAggregationBatchIdsOfStatus(
            DEFAULT_ITERATION.toBuilder()
                .populationName("aaa")
                .taskId(111)
                .iterationId(11)
                .attemptId(0)
                .build(),
            0,
            FULL,
            Optional.of("abc"));

    // assert
    assertThat(result).isEqualTo(Arrays.asList());
  }

  @Test
  public void testUpdateAggregationBatchStatus_Succeeded() {
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
              insertBatch(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* batchId= */ "batch-1",
                  /* aggregationLevel= */ 0,
                  /* status= */ FULL,
                  /* batchSize= */ 50,
                  /* createdByPartition */ "abc",
                  /* createdTime */ toTs(NOW),
                  /* aggregatedBy */ "batch-a",
                  /* withStatusHistory */ true);
              return null;
            });

    // act
    AggregationBatchEntity entity =
        AggregationBatchEntity.builder()
            .populationName("aaa")
            .taskId(111)
            .iterationId(9)
            .attemptId(0)
            .batchId("batch-1")
            .aggregationLevel(0)
            .status(FULL)
            .batchSize(50)
            .createdByPartition("abc")
            .createdTime(NOW)
            .aggregatedBy("batch-a")
            .build();
    boolean updated =
        dao.updateAggregationBatchStatus(
            entity, entity.toBuilder().status(PUBLISH_COMPLETED).build());

    // assert
    assertThat(updated).isTrue();
    assertThat(queryAggregationBatch(entity).get().getStatus()).isEqualTo(PUBLISH_COMPLETED);
    assertThat(queryAggregationBatchStatusHistories(entity))
        .isEqualTo(Arrays.asList(FULL.code(), PUBLISH_COMPLETED.code()));
  }

  @Test
  public void testUpdateAggregationBatchStatus_NotFound() {
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
              insertBatch(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* batchId= */ "batch-1",
                  /* aggregationLevel= */ 0,
                  /* status= */ FULL,
                  /* batchSize= */ 50,
                  /* createdByPartition */ "abc",
                  /* createdTime */ toTs(NOW),
                  /* aggregatedBy */ "batch-a",
                  /* withStatusHistory */ true);
              return null;
            });

    // act
    AggregationBatchEntity entity =
        AggregationBatchEntity.builder()
            .populationName("aaa")
            .taskId(111)
            .iterationId(9)
            .attemptId(0)
            .batchId("batch-1")
            .aggregationLevel(0)
            .status(FULL)
            .batchSize(50)
            .createdByPartition("abc")
            .createdTime(NOW)
            .aggregatedBy("batch-a")
            .build();
    boolean updated =
        dao.updateAggregationBatchStatus(
            entity.toBuilder().batchId("unknown").build(),
            entity.toBuilder().batchId("unknown").status(PUBLISH_COMPLETED).build());

    // assert
    assertThat(updated).isFalse();
    assertThat(queryAggregationBatch(entity).get().getStatus()).isEqualTo(FULL);
    assertThat(queryAggregationBatchStatusHistories(entity)).isEqualTo(Arrays.asList(FULL.code()));
  }

  @Test
  public void testUpdateAggregationBatchStatus_IdMismatch() {
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
              insertBatch(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* batchId= */ "batch-1",
                  /* aggregationLevel= */ 0,
                  /* status= */ FULL,
                  /* batchSize= */ 50,
                  /* createdByPartition */ "abc",
                  /* createdTime */ toTs(NOW),
                  /* aggregatedBy */ "batch-a",
                  /* withStatusHistory */ true);
              return null;
            });

    // act
    AggregationBatchEntity entity =
        AggregationBatchEntity.builder()
            .populationName("aaa")
            .taskId(111)
            .iterationId(9)
            .attemptId(0)
            .batchId("batch-1")
            .aggregationLevel(0)
            .status(FULL)
            .batchSize(50)
            .createdByPartition("abc")
            .createdTime(NOW)
            .aggregatedBy("batch-a")
            .build();
    boolean updated =
        dao.updateAggregationBatchStatus(
            entity.toBuilder().build(),
            entity.toBuilder().batchId("unknown").status(PUBLISH_COMPLETED).build());

    // assert
    assertThat(updated).isFalse();
    assertThat(queryAggregationBatch(entity).get().getStatus()).isEqualTo(FULL);
    assertThat(queryAggregationBatchStatusHistories(entity)).isEqualTo(Arrays.asList(FULL.code()));
  }

  @Test
  public void testUpdateAggregationBatchStatus_FromStatusMismatch() {
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
              insertBatch(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* batchId= */ "batch-1",
                  /* aggregationLevel= */ 0,
                  /* status= */ FULL,
                  /* batchSize= */ 50,
                  /* createdByPartition */ "abc",
                  /* createdTime */ toTs(NOW),
                  /* aggregatedBy */ "batch-a",
                  /* withStatusHistory */ true);
              return null;
            });

    // act
    AggregationBatchEntity entity =
        AggregationBatchEntity.builder()
            .populationName("aaa")
            .taskId(111)
            .iterationId(9)
            .attemptId(0)
            .batchId("batch-1")
            .aggregationLevel(0)
            .status(FULL)
            .batchSize(50)
            .createdByPartition("abc")
            .createdTime(NOW)
            .aggregatedBy("batch-a")
            .build();
    boolean updated =
        dao.updateAggregationBatchStatus(
            entity.toBuilder().status(PUBLISH_COMPLETED).build(),
            entity.toBuilder().status(UPLOAD_COMPLETED).build());

    // assert
    assertThat(updated).isFalse();
    assertThat(queryAggregationBatch(entity).get().getStatus()).isEqualTo(FULL);
    assertThat(queryAggregationBatchStatusHistories(entity)).isEqualTo(Arrays.asList(FULL.code()));
  }

  @Test
  public void testUpdateAggregationBatchStatus_HistoryMismatch() {
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
              insertBatch(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* batchId= */ "batch-1",
                  /* aggregationLevel= */ 0,
                  /* status= */ FULL,
                  /* batchSize= */ 50,
                  /* createdByPartition */ "abc",
                  /* createdTime */ toTs(NOW),
                  /* aggregatedBy */ "batch-a",
                  /* withStatusHistory */ false);
              insertBatchStatusHistory(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* batchId= */ "batch-1",
                  /* aggregationLevel= */ 0,
                  /* statusId= */ 2,
                  /* status= */ PUBLISH_COMPLETED,
                  /* createdByPartition */ "abc",
                  /* createdTime */ toTs(NOW),
                  /* aggregatedBy */ "batch-a");
              return null;
            });

    // act
    AggregationBatchEntity entity =
        AggregationBatchEntity.builder()
            .populationName("aaa")
            .taskId(111)
            .iterationId(9)
            .attemptId(0)
            .batchId("batch-1")
            .aggregationLevel(0)
            .status(FULL)
            .batchSize(50)
            .createdByPartition("abc")
            .createdTime(NOW)
            .aggregatedBy("batch-a")
            .build();
    boolean updated =
        dao.updateAggregationBatchStatus(
            entity.toBuilder().build(), entity.toBuilder().status(PUBLISH_COMPLETED).build());

    // assert
    assertThat(updated).isFalse();
    assertThat(queryAggregationBatch(entity).get().getStatus()).isEqualTo(FULL);
    assertThat(queryAggregationBatchStatusHistories(entity))
        .isEqualTo(Arrays.asList(PUBLISH_COMPLETED.code()));
  }

  @Test
  public void testUpdateAggregationBatchStatus_NoHistory() {
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
              insertBatch(
                  transaction,
                  /* populationName= */ "aaa",
                  /* taskId= */ 111,
                  /* iterationId= */ 9,
                  /* batchId= */ "batch-1",
                  /* aggregationLevel= */ 0,
                  /* status= */ FULL,
                  /* batchSize= */ 50,
                  /* createdByPartition */ "abc",
                  /* createdTime */ toTs(NOW),
                  /* aggregatedBy */ "batch-a",
                  /* withStatusHistory */ false);
              return null;
            });

    // act
    AggregationBatchEntity entity =
        AggregationBatchEntity.builder()
            .populationName("aaa")
            .taskId(111)
            .iterationId(9)
            .attemptId(0)
            .batchId("batch-1")
            .aggregationLevel(0)
            .status(FULL)
            .batchSize(50)
            .createdByPartition("abc")
            .createdTime(NOW)
            .aggregatedBy("batch-a")
            .build();
    boolean updated =
        dao.updateAggregationBatchStatus(
            entity.toBuilder().build(), entity.toBuilder().status(PUBLISH_COMPLETED).build());

    // assert
    assertThat(updated).isFalse();
    assertThat(queryAggregationBatch(entity).get().getStatus()).isEqualTo(FULL);
    assertThat(queryAggregationBatchStatusHistories(entity)).isEqualTo(Arrays.asList());
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
            + " BaseIterationId, BaseOnResultId, ReportGoal, ResultId, Info, AggregationLevel,"
            + " MinClientVersion, MaxClientVersion, MaxAggregationSize) VALUES(@populationName,"
            + " @taskId, @iterationId, @attemptId, @status, @baseIterationId, @baseOnResultId,"
            + " @reportGoal, @resultId, @info, @aggregationLevel, @minClientVersion,"
            + " @maxClientVersion, @maxAggregationSize)";
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
            .bind("minClientVersion")
            .to("0")
            .bind("maxClientVersion")
            .to("99999999")
            .bind("maxAggregationSize")
            .to(444)
            .build());
  }

  private void insertBatch(
      TransactionContext transaction,
      String populationName,
      long taskId,
      long iterationId,
      String batchId,
      long aggregationLevel,
      AggregationBatchEntity.Status status,
      long batchSize,
      String createdByPartition,
      Timestamp createdTime,
      String aggregatedBy,
      boolean withStatusHistory) {
    String insertBatch =
        "INSERT INTO AggregationBatch(PopulationName, TaskId, IterationId, AttemptId, BatchId,\n"
            + " AggregationLevel, Status, BatchSize, CreatedByPartition, CreatedTime,"
            + " AggregatedBy)\n"
            + " VALUES(@populationName, @taskId, @iterationId, @attemptId, @batchId,\n"
            + " @aggregationLevel, @status, @batchSize, @createdByPartition, @createdTime,"
            + " @aggregatedBy)\n";
    transaction.executeUpdate(
        Statement.newBuilder(insertBatch)
            .bind("populationName")
            .to(populationName)
            .bind("taskId")
            .to(taskId)
            .bind("iterationId")
            .to(iterationId)
            .bind("attemptId")
            .to(0)
            .bind("batchId")
            .to(batchId)
            .bind("aggregationLevel")
            .to(aggregationLevel)
            .bind("batchSize")
            .to(batchSize)
            .bind("createdByPartition")
            .to(createdByPartition)
            .bind("createdTime")
            .to(createdTime)
            .bind("status")
            .to(status.code())
            .bind("aggregatedBy")
            .to(aggregatedBy)
            .build());

    if (!withStatusHistory) {
      return;
    }

    insertBatchStatusHistory(
        /* transaction= */ transaction,
        /* populationName= */ populationName,
        /* taskId= */ taskId,
        /* iterationId= */ iterationId,
        /* batchId= */ batchId,
        /* statusId= */ 1,
        /* aggregationLevel= */ aggregationLevel,
        /* status= */ status,
        /* createdByPartition= */ createdByPartition,
        /* createdTime= */ createdTime,
        /* aggregatedBy= */ aggregatedBy);
  }

  private void insertBatchStatusHistory(
      TransactionContext transaction,
      String populationName,
      long taskId,
      long iterationId,
      String batchId,
      long statusId,
      long aggregationLevel,
      AggregationBatchEntity.Status status,
      String createdByPartition,
      Timestamp createdTime,
      String aggregatedBy) {
    String insertBatch =
        "INSERT INTO AggregationBatchStatusHistory(PopulationName, TaskId, IterationId, AttemptId,"
            + " BatchId,\n"
            + " StatusId, AggregationLevel, Status, CreatedByPartition, CreatedTime,"
            + " AggregatedBy)\n"
            + " VALUES(@populationName, @taskId, @iterationId, @attemptId, @batchId,\n"
            + " @statusId, @aggregationLevel, @status, @createdByPartition, @createdTime,"
            + " @aggregatedBy)\n";
    transaction.executeUpdate(
        Statement.newBuilder(insertBatch)
            .bind("populationName")
            .to(populationName)
            .bind("taskId")
            .to(taskId)
            .bind("iterationId")
            .to(iterationId)
            .bind("attemptId")
            .to(0)
            .bind("batchId")
            .to(batchId)
            .bind("statusId")
            .to(statusId)
            .bind("aggregationLevel")
            .to(aggregationLevel)
            .bind("createdByPartition")
            .to(createdByPartition)
            .bind("createdTime")
            .to(createdTime)
            .bind("status")
            .to(status.code())
            .bind("aggregatedBy")
            .to(aggregatedBy)
            .build());
  }

  private Optional<AggregationBatchEntity> queryAggregationBatch(AggregationBatchEntity batch) {
    Statement statement =
        Statement.newBuilder(
                "SELECT * FROM AggregationBatch WHERE PopulationName = @populationName AND"
                    + " TaskId=@taskId AND IterationId=@iterationId AND AttemptId=@attemptId AND"
                    + " BatchId=@batchId")
            .bind("populationName")
            .to(batch.getPopulationName())
            .bind("taskId")
            .to(batch.getTaskId())
            .bind("iterationId")
            .to(batch.getIterationId())
            .bind("attemptId")
            .to(batch.getAttemptId())
            .bind("batchId")
            .to(batch.getBatchId())
            .build();

    try (ResultSet resultSet =
        dbClient
            .singleUse() // Execute a single read or query against Cloud Spanner.
            .executeQuery(statement)) {
      ImmutableList.Builder<AggregationBatchEntity> entitiesBuilder = ImmutableList.builder();
      while (resultSet.next()) {
        entitiesBuilder.add(
            AggregationBatchEntity.builder()
                .populationName(resultSet.getString("PopulationName"))
                .taskId(resultSet.getLong("TaskId"))
                .iterationId(resultSet.getLong("IterationId"))
                .attemptId(resultSet.getLong("AttemptId"))
                .batchId(resultSet.getString("BatchId"))
                .status(AggregationBatchEntity.Status.fromCode(resultSet.getLong("Status")))
                .batchSize(resultSet.getLong("BatchSize"))
                .createdByPartition(resultSet.getString("CreatedByPartition"))
                .aggregatedBy(
                    resultSet.isNull("AggregatedBy") ? null : resultSet.getString("AggregatedBy"))
                .aggregationLevel(resultSet.getLong("AggregationLevel"))
                .build());
      }
      return entitiesBuilder.build().stream().findFirst();
    }
  }

  private List<Long> queryAggregationBatchStatusHistories(AggregationBatchEntity entity) {
    Statement statement =
        Statement.newBuilder(
                "SELECT * FROM AggregationBatchStatusHistory WHERE PopulationName = @populationName"
                    + " AND\n"
                    + " TaskId=@taskId AND IterationId=@iterationId AND AttemptId=@attemptId and"
                    + " BatchId=@batchId \n"
                    + " ORDER BY StatusId\n")
            .bind("populationName")
            .to(entity.getPopulationName())
            .bind("taskId")
            .to(entity.getTaskId())
            .bind("iterationId")
            .to(entity.getIterationId())
            .bind("attemptId")
            .to(entity.getAttemptId())
            .bind("batchId")
            .to(entity.getBatchId())
            .build();

    try (ResultSet resultSet = dbClient.singleUse().executeQuery(statement)) {
      List<Long> status = new ArrayList();

      while (resultSet.next()) {
        status.add(resultSet.getLong("Status"));
      }
      return status;
    }
  }

  private Timestamp toTs(Instant instant) {
    return Timestamp.ofTimeSecondsAndNanos(instant.getEpochSecond(), instant.getNano());
  }
}
