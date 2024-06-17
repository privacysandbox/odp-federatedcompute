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

package com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.gcp;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.TransactionContext;
import com.google.common.collect.ImmutableList;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.ModelMetricsEntity;
import java.io.IOException;
import java.sql.SQLException;
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
public class ModelMetricsSpannerDaoTest {

  private static final String PROJECT_ID = "spanner-emulator";
  private static final String INSTANCE = "fcp-model-metrics-unittest";
  private static final String DB_NAME = "test-model-metrics-dao";
  private static final String SDL_FILE_PATH = "shuffler/spanner/metrics_database.sdl";
  private static SpannerTestHarness.Connection spannerEmulatorConnection;

  private DatabaseClient dbClient;
  @InjectMocks private ModelMetricsSpannerDao mockDao;

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
    mockDao = new ModelMetricsSpannerDao(dbClient);
    MockitoAnnotations.initMocks(this);
  }

  @After
  public void teardown() throws SQLException {
    deleteDatabase();
  }

  private void initDbClient() throws SQLException {
    spannerEmulatorConnection.createDatabase();
    this.dbClient = spannerEmulatorConnection.getDatabaseClient();
  }

  private void deleteDatabase() throws SQLException {
    spannerEmulatorConnection.dropDatabase();
  }

  @Test
  public void upsertModelMetrics_notExist_insert() {
    // arrange
    ImmutableList<ModelMetricsEntity> metricsEntities =
        ImmutableList.of(
            ModelMetricsEntity.builder()
                .populationName("us")
                .taskId(111)
                .iterationId(222)
                .metricName("loss")
                .metricValue(1.0)
                .build());

    // act and assert
    assertTrue(mockDao.upsertModelMetrics(metricsEntities));
    double dbMetricValue = getMetric("us", 111, 222, "loss");
    assertThat(dbMetricValue).isEqualTo(1.0);
  }

  @Test
  public void upsertModelMetrics_exist_update() {
    // arrange
    dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              insertMetric(transaction, "us", 111, 222, "loss", 1.0);
              return null;
            });

    ImmutableList<ModelMetricsEntity> metricsEntities =
        ImmutableList.of(
            ModelMetricsEntity.builder()
                .populationName("us")
                .taskId(111)
                .iterationId(222)
                .metricName("loss")
                .metricValue(2.0)
                .build());

    // act and assert
    assertTrue(mockDao.upsertModelMetrics(metricsEntities));
    double dbMetricValue = getMetric("us", 111, 222, "loss");
    assertThat(dbMetricValue).isEqualTo(2.0);
  }

  @Test
  public void upsertModelMetrics_spannerException_returnFalse() {
    // arrange, missing required fields
    ImmutableList<ModelMetricsEntity> metricsEntities =
        ImmutableList.of(ModelMetricsEntity.builder().populationName("us").taskId(111).build());

    // act and assert
    assertFalse(mockDao.upsertModelMetrics(metricsEntities));
  }

  private void insertMetric(
      TransactionContext transaction,
      String populationName,
      long taskId,
      long iterationId,
      String metricName,
      double metricValue) {
    String insertMetric =
        "INSERT INTO ModelMetrics(PopulationName, TaskId, IterationId, MetricName, MetricValue,"
            + " CreatedTime) VALUES(@populationName, @taskId, @iterationId, @metricName,"
            + " @metricValue, PENDING_COMMIT_TIMESTAMP())";
    transaction.executeUpdate(
        Statement.newBuilder(insertMetric)
            .bind("PopulationName")
            .to(populationName)
            .bind("TaskId")
            .to(taskId)
            .bind("IterationId")
            .to(iterationId)
            .bind("MetricName")
            .to(metricName)
            .bind("MetricValue")
            .to(metricValue)
            .build());
  }

  private double getMetric(
      String populationName, long taskId, long iterationId, String metricName) {
    String selectMetric =
        "SELECT MetricValue FROM ModelMetrics WHERE PopulationName = @populationName\n"
            + "  AND TaskId = @taskId\n"
            + "  AND IterationId = @iterationId\n"
            + "  AND MetricName = @metricName\n";

    Statement statement =
        Statement.newBuilder(selectMetric)
            .bind("PopulationName")
            .to(populationName)
            .bind("TaskId")
            .to(taskId)
            .bind("IterationId")
            .to(iterationId)
            .bind("MetricName")
            .to(metricName)
            .build();

    try (ResultSet resultSet = dbClient.singleUseReadOnlyTransaction().executeQuery(statement)) {
      while (resultSet.next()) {
        // there should be only one
        return resultSet.getDouble("MetricValue");
      }
    }

    throw new RuntimeException("Not found, please insert first before querying.");
  }
}
