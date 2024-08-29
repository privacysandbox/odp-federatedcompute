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

import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.ReadContext;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.TransactionContext;
import com.google.common.collect.ImmutableList;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AggregationBatchDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AggregationBatchEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AggregationBatchId;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.IterationEntity;
import java.time.InstantSource;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/** Spanner implementation of AggregationBatch DAO. */
@Component
public class AggregationBatchSpannerDao implements AggregationBatchDao {
  private static final Logger logger = LoggerFactory.getLogger(AggregationBatchSpannerDao.class);
  private DatabaseClient dbClient;
  private InstantSource instantSource;

  private static final String SELECT_AGGREGATION_BATCHES =
      "SELECT PopulationName, TaskId, IterationId, AttemptId, BatchId, AggregationLevel,"
          + " Status, BatchSize, CreatedByPartition, CreatedTime, AggregatedBy\n"
          + " FROM AggregationBatch\n";

  private static final String SELECT_STATUS_ID_OF_STATUS =
      "SELECT MAX(StatusId) as StatusId \n"
          + " FROM AggregationBatchStatusHistory \n"
          + " WHERE PopulationName = @populationName \n"
          + "  AND TaskId = @taskId \n"
          + "  AND IterationId = @iterationId \n"
          + "  AND AttemptId = @attemptId \n"
          + "  AND AggregationLevel = @aggregationLevel \n"
          + "  AND BatchId = @batchId \n"
          + "  AND Status = @status \n";

  private static final String SELECT_BATCH_IDS_OF_STATUS =
      "SELECT BatchId \n"
          + " FROM AggregationBatch \n"
          + " WHERE PopulationName = @populationName \n"
          + "   AND TaskId = @taskId \n"
          + "   AND IterationId = @iterationId \n"
          + "   AND AttemptId = @attemptId \n"
          + "   AND AggregationLevel = @aggregationLevel \n"
          + "   AND Status = @status \n"
          + " ORDER BY BatchId";

  private static final String SUM_BATCH_SIZE_OF_STATUS =
      "SELECT SUM(BatchSize) as sum \n"
          + " FROM AggregationBatch \n"
          + " WHERE PopulationName = @populationName \n"
          + "   AND TaskId = @taskId \n"
          + "   AND IterationId = @iterationId \n"
          + "   AND AttemptId = @attemptId \n"
          + "   AND AggregationLevel = @aggregationLevel \n"
          + "   AND Status in UNNEST(@status) \n";

  private static final String SELECT_BATCH_IDS_OF_STATUS_FOR_PARTITION =
      "SELECT BatchId \n"
          + " FROM AggregationBatch \n"
          + " WHERE PopulationName = @populationName \n"
          + "   AND TaskId = @taskId \n"
          + "   AND IterationId = @iterationId \n"
          + "   AND AttemptId = @attemptId \n"
          + "   AND AggregationLevel = @aggregationLevel \n"
          + "   AND Status = @status \n"
          + "   AND CreatedByPartition = @createdByPartition \n"
          + " ORDER BY BatchId";

  public AggregationBatchSpannerDao(
      @Qualifier("taskDatabaseClient") DatabaseClient dbClient, InstantSource instantSource) {
    this.dbClient = dbClient;
    this.instantSource = instantSource;
  }

  public Optional<AggregationBatchEntity> getAggregationBatchById(
      AggregationBatchId aggregationBatchId) {
    Statement statement =
        Statement.newBuilder(
                SELECT_AGGREGATION_BATCHES
                    + "WHERE PopulationName = @populationName\n"
                    + "AND TaskId = @taskId\n"
                    + "AND iterationId = @iterationId\n"
                    + "AND attemptId = @attemptId\n"
                    + "AND batchId = @batchId\n")
            .bind("populationName")
            .to(aggregationBatchId.getPopulationName())
            .bind("iterationId")
            .to(aggregationBatchId.getIterationId())
            .bind("taskId")
            .to(aggregationBatchId.getTaskId())
            .bind("attemptId")
            .to(aggregationBatchId.getAttemptId())
            .bind("batchId")
            .to(aggregationBatchId.getBatchId())
            .build();
    try (ResultSet resultSet =
        dbClient
            .singleUse() // Execute a single read or query against Cloud Spanner.
            .executeQuery(statement)) {
      return extractAggregationBatchEntitiesFromResultSet(resultSet).stream().findFirst();
    }
  }

  private static List<AggregationBatchEntity> extractAggregationBatchEntitiesFromResultSet(
      ResultSet resultSet) {
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
              .build());
    }
    return entitiesBuilder.build();
  }

  @Override
  public boolean updateAggregationBatchStatus(
      AggregationBatchEntity from, AggregationBatchEntity to) {
    if (!from.getId().equals(to.getId())) {
      return false;
    }
    return getLastVersionOfStatus(dbClient.singleUseReadOnlyTransaction(), from)
        .map(
            currentStatusId -> {
              try {
                return dbClient
                    .readWriteTransaction()
                    .run(
                        transaction -> {
                          insertStatus(transaction, to, currentStatusId + 1);
                          updateAggregationBatchStatus(transaction, from, to);
                          return true;
                        });
              } catch (SpannerException e) {
                logger.atWarn().setCause(e).log("Failed to update status.");
                return false;
              }
            })
        .orElse(false);
  }

  private Optional<Long> getLastVersionOfStatus(
      ReadContext transaction, AggregationBatchEntity aggregationBatch) {
    Statement.Builder statement;
    if (aggregationBatch.getAggregatedBy() != null) {
      statement =
          Statement.newBuilder(SELECT_STATUS_ID_OF_STATUS + " AND AggregatedBy = @aggregatedBy \n");
      statement.bind("aggregatedBy").to(aggregationBatch.getAggregatedBy());
    } else {
      statement =
          Statement.newBuilder(SELECT_STATUS_ID_OF_STATUS + "  AND AggregatedBy is NULL \n");
    }

    statement
        .bind("populationName")
        .to(aggregationBatch.getPopulationName())
        .bind("taskId")
        .to(aggregationBatch.getTaskId())
        .bind("iterationId")
        .to(aggregationBatch.getIterationId())
        .bind("attemptId")
        .to(aggregationBatch.getAttemptId())
        .bind("aggregationLevel")
        .to(aggregationBatch.getAggregationLevel())
        .bind("batchId")
        .to(aggregationBatch.getBatchId())
        .bind("status")
        .to(aggregationBatch.getStatus().code())
        .build();

    try (ResultSet resultSet = transaction.executeQuery(statement.build())) {
      while (resultSet.next()) {
        // there should be only one
        if (!resultSet.isNull("StatusId")) {
          return Optional.of(resultSet.getLong("StatusId"));
        }
      }
    }

    return Optional.empty();
  }

  private void insertStatus(
      TransactionContext transaction, AggregationBatchEntity aggregationBatch, long statusId) {
    Statement statusHistory =
        Statement.newBuilder(
                "INSERT INTO AggregationBatchStatusHistory (PopulationName, TaskId,\n"
                    + " IterationId,\n"
                    + " AttemptId, BatchId, StatusId, AggregationLevel, Status,\n"
                    + " CreatedByPartition, CreatedTime, AggregatedBy) VALUES(@populationName,"
                    + " @taskId,\n"
                    + " @iterationId, @attemptId, @batchId, @statusId, @aggregationLevel,\n"
                    + " @status, @createdByPartition, @createdTime, @aggregatedBy)")
            .bind("populationName")
            .to(aggregationBatch.getPopulationName())
            .bind("taskId")
            .to(aggregationBatch.getTaskId())
            .bind("iterationId")
            .to(aggregationBatch.getIterationId())
            .bind("attemptId")
            .to(aggregationBatch.getAttemptId())
            .bind("batchId")
            .to(aggregationBatch.getBatchId())
            .bind("statusId")
            .to(statusId)
            .bind("aggregationLevel")
            .to(aggregationBatch.getAggregationLevel())
            .bind("status")
            .to(aggregationBatch.getStatus().code())
            .bind("createdByPartition")
            .to(aggregationBatch.getCreatedByPartition())
            .bind("createdTime")
            .to(TimestampInstantConverter.TO_TIMESTAMP.convert(instantSource.instant()))
            .bind("aggregatedBy")
            .to(aggregationBatch.getAggregatedBy())
            .build();

    long historyInserted = transaction.executeUpdate(statusHistory);
    if (historyInserted != 1) {
      throw new IllegalStateException(
          "AggregationBatchStatusHistory insertion impacted " + historyInserted + " rows.");
    }
  }

  private void updateAggregationBatchStatus(
      TransactionContext transaction, AggregationBatchEntity from, AggregationBatchEntity to) {

    Statement statement =
        Statement.newBuilder(
                "UPDATE AggregationBatch SET Status = @newStatus WHERE\n"
                    + " PopulationName=@populationName AND TaskId=@taskId AND\n"
                    + " IterationId=@iterationId AND AttemptId=@attemptId AND\n"
                    + " BatchId=@batchId AND Status=@oldStatus")
            .bind("populationName")
            .to(from.getPopulationName())
            .bind("taskId")
            .to(from.getTaskId())
            .bind("iterationId")
            .to(from.getIterationId())
            .bind("attemptId")
            .to(from.getAttemptId())
            .bind("batchId")
            .to(from.getBatchId())
            .bind("oldStatus")
            .to(from.getStatus().code())
            .bind("newStatus")
            .to(to.getStatus().code())
            .build();

    long updatedRowCount = transaction.executeUpdate(statement);
    if (updatedRowCount != 1) {
      throw new IllegalStateException(
          String.format(
              "%s aggregation batch is impacted when updating from %s to %s.",
              updatedRowCount, from, to));
    }
  }

  @Override
  public List<String> queryAggregationBatchIdsOfStatus(
      IterationEntity iteration,
      long aggregationLevel,
      AggregationBatchEntity.Status status,
      Optional<String> createdByPartition) {
    Statement.Builder statement;
    if (createdByPartition.isPresent()) {
      statement =
          Statement.newBuilder(SELECT_BATCH_IDS_OF_STATUS_FOR_PARTITION)
              .bind("createdByPartition")
              .to(createdByPartition.get());
    } else {
      statement = Statement.newBuilder(SELECT_BATCH_IDS_OF_STATUS);
    }
    statement
        .bind("populationName")
        .to(iteration.getPopulationName())
        .bind("taskId")
        .to(iteration.getTaskId())
        .bind("iterationId")
        .to(iteration.getIterationId())
        .bind("attemptId")
        .to(iteration.getAttemptId())
        .bind("aggregationLevel")
        .to(aggregationLevel)
        .bind("status")
        .to(status.code())
        .build();

    ImmutableList.Builder<String> result = ImmutableList.builder();
    try (ResultSet resultSet =
        dbClient.singleUseReadOnlyTransaction().executeQuery(statement.build())) {
      while (resultSet.next()) {
        result.add(resultSet.getString("BatchId"));
      }
    }
    return result.build();
  }

  @Override
  public long querySumOfAggregationBatchesOfStatus(
      IterationEntity iteration,
      long aggregationLevel,
      List<AggregationBatchEntity.Status> status) {
    Statement.Builder statement;
    statement = Statement.newBuilder(SUM_BATCH_SIZE_OF_STATUS);
    statement
        .bind("populationName")
        .to(iteration.getPopulationName())
        .bind("taskId")
        .to(iteration.getTaskId())
        .bind("iterationId")
        .to(iteration.getIterationId())
        .bind("attemptId")
        .to(iteration.getAttemptId())
        .bind("aggregationLevel")
        .to(aggregationLevel)
        .bind("status")
        .toInt64Array(status.stream().map(AggregationBatchEntity.Status::code).toList())
        .build();

    try (ResultSet resultSet =
        dbClient.singleUseReadOnlyTransaction().executeQuery(statement.build())) {
      while (resultSet.next()) {
        if (resultSet.isNull("sum")) {
          return 0;
        }
        return resultSet.getLong("sum");
      }
    }
    return 0;
  }

  @Override
  public boolean createAndAssignBatches(
      List<AggregationBatchId> batchesToUpdate, AggregationBatchEntity newBatch) {
    throw new UnsupportedOperationException("Not implemented yet.");
  }
}
