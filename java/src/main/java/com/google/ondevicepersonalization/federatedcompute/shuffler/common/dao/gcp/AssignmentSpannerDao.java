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

import com.google.cloud.Timestamp;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.ReadContext;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.TransactionContext;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.Constants;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AggregationBatchEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AssignmentDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AssignmentEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AssignmentEntity.Status;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AssignmentId;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.IterationEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.IterationId;
import java.time.Instant;
import java.time.InstantSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/** Spanner implementation of Assignment DAO. */
@Component
public class AssignmentSpannerDao implements AssignmentDao {

  private static final Logger logger = LoggerFactory.getLogger(AssignmentSpannerDao.class);
  private static final int ASSIGNMENT_BATCH_UPDATE_SIZE = 50;
  private DatabaseClient dbClient;
  private InstantSource instantSource;
  private ListeningExecutorService executorService;
  private static final String SELECT_ASSIGNMENT =
      "SELECT a.PopulationName, a.TaskId, a.IterationId,\n"
          + " a.AttemptId, a.SessionId, a.CorrelationId, a.Status, a.BatchId,\n"
          + " i.BaseIterationId, i.BaseOnResultId, i.ResultId\n"
          + " FROM Assignment as a INNER JOIN Iteration as i\n"
          + "  ON a.PopulationName = i.PopulationName\n"
          + "   AND a.TaskId = i.TaskId\n"
          + "   AND a.IterationId = i.IterationId\n"
          + "   AND a.AttemptId = i.AttemptId\n"
          + " WHERE a.PopulationName = @populationName\n"
          + "  AND a.TaskId = @taskId\n"
          + "  AND a.IterationId = @iterationId\n"
          + "  AND a.AttemptId = @attemptId\n"
          + "  AND a.SessionId = @sessionId\n";

  private static final String SELECT_STATUS_ID_OF_STATUS =
      "SELECT MAX(StatusId) as StatusId \n"
          + " FROM AssignmentStatusHistory \n"
          + " WHERE PopulationName = @populationName \n"
          + "  AND TaskId = @taskId \n"
          + "  AND IterationId = @iterationId \n"
          + "  AND AttemptId = @attemptId \n"
          + "  AND SessionId = @sessionId \n"
          + "  AND Status = @status \n";

  private static final String SELECT_ASSIGNMENT_IDS_OF_STATUS_BEFORE =
      "SELECT h1.SessionId\n"
          + " FROM AssignmentStatusHistory AS h1\n"
          + " WHERE h1.PopulationName = @populationName \n"
          + "   AND h1.TaskId = @taskId \n"
          + "   AND h1.IterationId = @iterationId \n"
          + "   AND h1.AttemptId = @attemptId \n"
          + "   AND h1.Status = @status\n"
          + "   AND h1.CreatedTime < @createdBefore\n"
          + "   AND h1.StatusId = ( \n"
          + "      SELECT MAX(StatusId) \n"
          + "        FROM AssignmentStatusHistory AS h2 \n"
          + "          WHERE h1.PopulationName = h2.PopulationName\n"
          + "            AND h1.TaskId = h2.TaskId\n"
          + "            AND h1.IterationId = h2.IterationId\n"
          + "            AND h1.AttemptId = h2.AttemptId\n"
          + "            AND h1.SessionId = h2.SessionId\n"
          + ")\n"
          + "ORDER BY h1.CreatedTime";

  private static final String SELECT_ASSIGNMENT_IDS_OF_STATUS =
      "SELECT SessionId \n"
          + " FROM Assignment \n"
          + " WHERE PopulationName = @populationName \n"
          + "   AND TaskId = @taskId \n"
          + "   AND IterationId = @iterationId \n"
          + "   AND AttemptId = @attemptId \n"
          + "   AND Status = @status \n"
          + "   %s \n"
          + "ORDER BY SessionId";

  public AssignmentSpannerDao(
      @Qualifier("taskDatabaseClient") DatabaseClient dbClient,
      InstantSource instantSource,
      Optional<ListeningExecutorService> executorService) {
    this.dbClient = dbClient;
    this.instantSource = instantSource;
    this.executorService = executorService.orElse(MoreExecutors.newDirectExecutorService());
  }

  public Optional<AssignmentEntity> createAssignment(
      IterationEntity iteration, String correlationId, String sessionId) {
    Timestamp now = TimestampInstantConverter.TO_TIMESTAMP.convert(instantSource.instant());

    return dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              AssignmentId assignmentId =
                  AssignmentId.builder()
                      .populationName(iteration.getPopulationName())
                      .taskId(iteration.getTaskId())
                      .iterationId(iteration.getIterationId())
                      .attemptId(iteration.getAttemptId())
                      .assignmentId(sessionId)
                      .build();
              insertAssignment(
                  transaction, assignmentId, AssignmentEntity.Status.ASSIGNED, now, correlationId);
              insertStatus(
                  transaction,
                  assignmentId,
                  Constants.FIRST_ASSIGNMENT_STATUS_ID,
                  AssignmentEntity.Status.ASSIGNED,
                  now);

              return Optional.of(
                  AssignmentEntity.builder()
                      .populationName(iteration.getPopulationName())
                      .taskId(iteration.getTaskId())
                      .iterationId(iteration.getIterationId())
                      .attemptId(iteration.getAttemptId())
                      .sessionId(sessionId)
                      .correlationId(correlationId)
                      .baseIterationId(iteration.getBaseIterationId())
                      .baseOnResultId(iteration.getBaseOnResultId())
                      .resultId(iteration.getResultId())
                      .status(AssignmentEntity.Status.ASSIGNED)
                      .build());
            });
  }

  public Optional<AssignmentEntity> getAssignment(AssignmentId assignmentId) {
    Statement statement =
        bindAssigmentId(Statement.newBuilder(SELECT_ASSIGNMENT), assignmentId).build();

    try (ResultSet resultSet = dbClient.singleUseReadOnlyTransaction().executeQuery(statement)) {
      while (resultSet.next()) {
        // there should be only one
        return Optional.of(
            AssignmentEntity.builder()
                .populationName(resultSet.getString("PopulationName"))
                .taskId(resultSet.getLong("TaskId"))
                .iterationId(resultSet.getLong("IterationId"))
                .attemptId(resultSet.getLong("AttemptId"))
                .sessionId(resultSet.getString("SessionId"))
                .baseIterationId(resultSet.getLong("BaseIterationId"))
                .baseOnResultId(resultSet.getLong("BaseOnResultId"))
                .resultId(resultSet.getLong("ResultId"))
                .batchId(resultSet.isNull("BatchId") ? null : resultSet.getString("BatchId"))
                .status(AssignmentEntity.Status.fromCode(resultSet.getLong("Status")))
                .build());
      }
    }

    return Optional.empty();
  }

  public boolean updateAssignmentStatus(
      AssignmentId assignmentId, AssignmentEntity.Status from, AssignmentEntity.Status to) {
    // Ensure the latest Status is as expected and batchId is NULL for updating status as batchId
    // being set is an end-state.
    return getLastVersionOfStatus(
            dbClient.singleUseReadOnlyTransaction(), assignmentId, from, Optional.empty())
        .map(
            currentStatusId -> {
              try {
                return dbClient
                    .readWriteTransaction()
                    .run(
                        transaction -> {
                          insertStatus(
                              transaction,
                              assignmentId,
                              currentStatusId + 1,
                              to,
                              TimestampInstantConverter.TO_TIMESTAMP.convert(
                                  instantSource.instant()));
                          updateAssignmentStatus(transaction, assignmentId, from, to);
                          return true;
                        });
              } catch (SpannerException e) {
                logger.atWarn().setCause(e).log("Failed to update status.");
                return false;
              }
            })
        .orElse(false);
  }

  private void updateAssignmentStatus(
      TransactionContext transaction,
      AssignmentId assignmentId,
      AssignmentEntity.Status from,
      AssignmentEntity.Status to) {

    Statement statement =
        Statement.newBuilder(
                "UPDATE Assignment SET Status = @newStatus WHERE\n"
                    + " PopulationName=@populationName AND TaskId=@taskId AND\n"
                    + " IterationId=@iterationId AND AttemptId=@attemptId AND\n"
                    + " SessionId=@SessionId AND Status=@oldStatus")
            .bind("populationName")
            .to(assignmentId.getPopulationName())
            .bind("taskId")
            .to(assignmentId.getTaskId())
            .bind("iterationId")
            .to(assignmentId.getIterationId())
            .bind("attemptId")
            .to(assignmentId.getAttemptId())
            .bind("sessionId")
            .to(assignmentId.getAssignmentId())
            .bind("oldStatus")
            .to(from.code())
            .bind("newStatus")
            .to(to.code())
            .build();

    long updatedRowCount = transaction.executeUpdate(statement);
    if (updatedRowCount != 1) {
      throw new IllegalStateException(
          String.format(
              "%s assignment is impacted when updating %s from %s to %s.",
              updatedRowCount, assignmentId.toString(), from, to));
    }
  }

  public List<String> queryAssignmentIdsOfStatus(
      IterationId iterationId, AssignmentEntity.Status status, Instant updatedBefore) {

    Statement statement =
        Statement.newBuilder(SELECT_ASSIGNMENT_IDS_OF_STATUS_BEFORE)
            .bind("populationName")
            .to(iterationId.getPopulationName())
            .bind("taskId")
            .to(iterationId.getTaskId())
            .bind("iterationId")
            .to(iterationId.getIterationId())
            .bind("attemptId")
            .to(iterationId.getAttemptId())
            .bind("status")
            .to(status.code())
            .bind("createdBefore")
            .to(TimestampInstantConverter.TO_TIMESTAMP.convert(updatedBefore))
            .build();

    ImmutableList.Builder<String> result = ImmutableList.builder();

    try (ResultSet resultSet = dbClient.singleUseReadOnlyTransaction().executeQuery(statement)) {
      while (resultSet.next()) {
        result.add(resultSet.getString("SessionId"));
      }
    }

    return result.build();
  }

  public List<String> queryAssignmentIdsOfStatus(
      IterationId iterationId, AssignmentEntity.Status status, Optional<String> batchId) {

    Statement.Builder statement;
    if (batchId.isPresent()) {
      statement =
          Statement.newBuilder(
              String.format(SELECT_ASSIGNMENT_IDS_OF_STATUS, " AND BatchId = @batchId \n"));
      statement.bind("batchId").to(batchId.get());
    } else {
      statement =
          Statement.newBuilder(
              String.format(SELECT_ASSIGNMENT_IDS_OF_STATUS, " AND BatchId is NULL \n"));
    }

    statement
        .bind("populationName")
        .to(iterationId.getPopulationName())
        .bind("taskId")
        .to(iterationId.getTaskId())
        .bind("iterationId")
        .to(iterationId.getIterationId())
        .bind("attemptId")
        .to(iterationId.getAttemptId())
        .bind("status")
        .to(status.code())
        .build();

    ImmutableList.Builder<String> result = ImmutableList.builder();

    try (ResultSet resultSet =
        dbClient.singleUseReadOnlyTransaction().executeQuery(statement.build())) {
      while (resultSet.next()) {
        result.add(resultSet.getString("SessionId"));
      }
    }

    return result.build();
  }

  private Optional<Long> getLastVersionOfStatus(
      ReadContext transaction,
      AssignmentId assignmentId,
      AssignmentEntity.Status from,
      Optional<String> batchId) {
    Statement.Builder statement;
    if (batchId.isPresent()) {
      statement = Statement.newBuilder(SELECT_STATUS_ID_OF_STATUS + " AND BatchId = @batchId \n");
      statement.bind("batchId").to(batchId.get());
    } else {
      statement = Statement.newBuilder(SELECT_STATUS_ID_OF_STATUS + "  AND BatchId is NULL \n");
    }
    bindAssigmentId(statement, assignmentId).bind("status").to(from.code()).build();

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

  private static Statement.Builder bindAssigmentId(
      Statement.Builder builder, AssignmentId assignmentId) {
    return builder
        .bind("populationName")
        .to(assignmentId.getPopulationName())
        .bind("taskId")
        .to(assignmentId.getTaskId())
        .bind("iterationId")
        .to(assignmentId.getIterationId())
        .bind("attemptId")
        .to(assignmentId.getAttemptId())
        .bind("sessionId")
        .to(assignmentId.getAssignmentId());
  }

  private void insertAssignment(
      TransactionContext transaction,
      AssignmentId assignmentId,
      AssignmentEntity.Status status,
      Timestamp createdTime,
      String correlationId) {
    Statement insertStatement =
        Statement.newBuilder(
                "INSERT INTO Assignment (PopulationName, TaskId, IterationId,\n"
                    + " AttemptId, SessionId, CorrelationId, Status,\n"
                    + " CreatedTime) VALUES(@populationName, @taskId,\n"
                    + " @iterationId, @attemptId, @sessionId, @correlationId,\n"
                    + " @status, @createdTime)")
            .bind("populationName")
            .to(assignmentId.getPopulationName())
            .bind("taskId")
            .to(assignmentId.getTaskId())
            .bind("iterationId")
            .to(assignmentId.getIterationId())
            .bind("attemptId")
            .to(assignmentId.getAttemptId())
            .bind("sessionId")
            .to(assignmentId.getAssignmentId())
            .bind("correlationId")
            .to(correlationId)
            .bind("status")
            .to(status.code())
            .bind("createdTime")
            .to(createdTime)
            .build();
    long assignmentCreated = transaction.executeUpdate(insertStatement);
    if (assignmentCreated != 1) {
      throw new IllegalStateException(
          "Assignment insertion impacted " + assignmentCreated + " rows.");
    }
  }

  private void insertStatus(
      TransactionContext transaction,
      AssignmentId assignmentId,
      long statusId,
      AssignmentEntity.Status status,
      Timestamp createdTime) {
    Statement statusHistory =
        Statement.newBuilder(
                "INSERT INTO AssignmentStatusHistory (PopulationName, TaskId,\n"
                    + " IterationId,\n"
                    + " AttemptId, SessionId, StatusId, Status,\n"
                    + " CreatedTime) VALUES(@populationName, @taskId,\n"
                    + " @iterationId, @attemptId, @sessionId, @statusId,\n"
                    + " @status, @createdTime)")
            .bind("populationName")
            .to(assignmentId.getPopulationName())
            .bind("taskId")
            .to(assignmentId.getTaskId())
            .bind("iterationId")
            .to(assignmentId.getIterationId())
            .bind("attemptId")
            .to(assignmentId.getAttemptId())
            .bind("sessionId")
            .to(assignmentId.getAssignmentId())
            .bind("statusId")
            .to(statusId)
            .bind("status")
            .to(status.code())
            .bind("createdTime")
            .to(createdTime)
            .build();

    long historyInserted = transaction.executeUpdate(statusHistory);
    if (historyInserted != 1) {
      throw new IllegalStateException(
          "AssignmentStatusHistory insertion impacted " + historyInserted + " rows.");
    }
  }

  @Override
  public int batchUpdateAssignmentStatus(
      List<AssignmentId> assignmentIds, Optional<String> batchId, Status from, Status to) {
    try {
      List<List<AssignmentId>> partitions =
          Lists.partition(assignmentIds, ASSIGNMENT_BATCH_UPDATE_SIZE);
      List<ListenableFuture<Integer>> futures = new ArrayList<>();
      for (List<AssignmentId> partition : partitions) {
        futures.add(
            executorService.submit(
                () -> singleBatchUpdateAssignmentStatus(partition, batchId, from, to)));
      }
      return Futures.successfulAsList(futures).get().stream()
          .filter(Objects::nonNull)
          .mapToInt(Integer::intValue)
          .sum();
    } catch (Exception e) {
      logger.error("Failed to update assignment statuses.", e);
      throw new RuntimeException(e);
    }
  }

  private int singleBatchUpdateAssignmentStatus(
      List<AssignmentId> assignmentIds, Optional<String> batchId, Status from, Status to) {
    try {
      List<Mutation> mutations = new ArrayList<>();
      return dbClient
          .readWriteTransaction()
          .run(
              transaction -> {
                for (AssignmentId assignmentId : assignmentIds) {
                  // Ensure the latest Status is as expected.
                  Long currentStatusId =
                      getLastVersionOfStatus(transaction, assignmentId, from, batchId).orElse(null);
                  if (currentStatusId == null) {
                    logger.error(
                        "Failed to find status of assignment id {}",
                        assignmentId.getAssignmentId());
                    continue;
                  }

                  mutations.add(
                      Mutation.newInsertBuilder("AssignmentStatusHistory")
                          .set("PopulationName")
                          .to(assignmentId.getPopulationName())
                          .set("TaskId")
                          .to(assignmentId.getTaskId())
                          .set("IterationId")
                          .to(assignmentId.getIterationId())
                          .set("AttemptId")
                          .to(assignmentId.getAttemptId())
                          .set("SessionId")
                          .to(assignmentId.getAssignmentId())
                          .set("StatusId")
                          .to(currentStatusId + 1)
                          .set("Status")
                          .to(to.code())
                          .set("CreatedTime")
                          .to(
                              TimestampInstantConverter.TO_TIMESTAMP.convert(
                                  instantSource.instant()))
                          .build());

                  mutations.add(
                      Mutation.newUpdateBuilder("Assignment")
                          .set("PopulationName")
                          .to(assignmentId.getPopulationName())
                          .set("TaskId")
                          .to(assignmentId.getTaskId())
                          .set("IterationId")
                          .to(assignmentId.getIterationId())
                          .set("AttemptId")
                          .to(assignmentId.getAttemptId())
                          .set("SessionId")
                          .to(assignmentId.getAssignmentId())
                          .set("Status")
                          .to(to.code())
                          .build());
                }
                transaction.buffer(mutations);
                int assignmentsMutatedCount = mutations.size() / 2;
                if (assignmentsMutatedCount != assignmentIds.size()) {
                  logger.error(
                      "Failed to update {} assignments for partition",
                      assignmentIds.size() - assignmentsMutatedCount);
                }
                return assignmentsMutatedCount;
              })
          .intValue();
    } catch (SpannerException e) {
      logger.atWarn().setCause(e).log("Failed to update statuses.");
      return 0;
    }
  }

  @Override
  public boolean createBatchAndUpdateAssignments(
      List<AssignmentId> assignmentIds,
      IterationEntity iteration,
      Status from,
      Status to,
      String batchId,
      String partition) {
    try {
      List<Mutation> mutations = new ArrayList<>();
      Timestamp now = TimestampInstantConverter.TO_TIMESTAMP.convert(instantSource.instant());
      return dbClient
          .readWriteTransaction()
          .run(
              transaction -> {
                for (AssignmentId assignmentId : assignmentIds) {
                  // Ensure the latest state of assignment batchId is NULL and Status is from.
                  Long currentStatusId =
                      getLastVersionOfStatus(transaction, assignmentId, from, Optional.empty())
                          .orElse(null);
                  if (currentStatusId == null) {
                    logger.error(
                        "Failed to find status of assignment id {}",
                        assignmentId.getAssignmentId());
                    return false;
                  }

                  mutations.add(
                      Mutation.newInsertBuilder("AssignmentStatusHistory")
                          .set("PopulationName")
                          .to(assignmentId.getPopulationName())
                          .set("TaskId")
                          .to(assignmentId.getTaskId())
                          .set("IterationId")
                          .to(assignmentId.getIterationId())
                          .set("AttemptId")
                          .to(assignmentId.getAttemptId())
                          .set("SessionId")
                          .to(assignmentId.getAssignmentId())
                          .set("StatusId")
                          .to(currentStatusId + 1)
                          .set("Status")
                          .to(to.code())
                          .set("CreatedTime")
                          .to(now)
                          .set("BatchId")
                          .to(batchId)
                          .build());

                  mutations.add(
                      Mutation.newUpdateBuilder("Assignment")
                          .set("PopulationName")
                          .to(assignmentId.getPopulationName())
                          .set("TaskId")
                          .to(assignmentId.getTaskId())
                          .set("IterationId")
                          .to(assignmentId.getIterationId())
                          .set("AttemptId")
                          .to(assignmentId.getAttemptId())
                          .set("SessionId")
                          .to(assignmentId.getAssignmentId())
                          .set("Status")
                          .to(to.code())
                          .set("BatchId")
                          .to(batchId)
                          .build());
                }
                // Create the batch and history
                mutations.add(
                    Mutation.newInsertBuilder("AggregationBatch")
                        .set("PopulationName")
                        .to(iteration.getPopulationName())
                        .set("TaskId")
                        .to(iteration.getTaskId())
                        .set("IterationId")
                        .to(iteration.getIterationId())
                        .set("AttemptId")
                        .to(iteration.getAttemptId())
                        .set("BatchId")
                        .to(batchId)
                        .set("AggregationLevel")
                        .to(iteration.getAggregationLevel())
                        .set("BatchSize")
                        .to(assignmentIds.size())
                        .set("CreatedByPartition")
                        .to(partition)
                        .set("Status")
                        .to(AggregationBatchEntity.Status.FULL.code())
                        .set("CreatedTime")
                        .to(now)
                        .build());
                mutations.add(
                    Mutation.newInsertBuilder("AggregationBatchStatusHistory")
                        .set("PopulationName")
                        .to(iteration.getPopulationName())
                        .set("TaskId")
                        .to(iteration.getTaskId())
                        .set("IterationId")
                        .to(iteration.getIterationId())
                        .set("AttemptId")
                        .to(iteration.getAttemptId())
                        .set("BatchId")
                        .to(batchId)
                        .set("StatusId")
                        .to(Constants.FIRST_AGGREGATION_BATCH_STATUS_ID)
                        .set("AggregationLevel")
                        .to(iteration.getAggregationLevel())
                        .set("CreatedByPartition")
                        .to(partition)
                        .set("Status")
                        .to(AggregationBatchEntity.Status.FULL.code())
                        .set("CreatedTime")
                        .to(now)
                        .build());
                transaction.buffer(mutations);
                return true;
              })
          .booleanValue();
    } catch (SpannerException e) {
      logger.atWarn().setCause(e).log("Failed to update statuses.");
      return false;
    }
  }
}
