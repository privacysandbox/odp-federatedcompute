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
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.ReadContext;
import com.google.cloud.spanner.ReadOnlyTransaction;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.TransactionContext;
import com.google.cloud.spanner.Value;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.Constants;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.IterationEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.IterationId;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskEntity.Status;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskId;
import java.time.Instant;
import java.time.InstantSource;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/** Spanner implementation of TaskDao. */
@Component
public class TaskSpannerDao implements TaskDao {

  private static final Logger logger = LoggerFactory.getLogger(TaskSpannerDao.class);

  private static final String SELECT_TASK_ENTITIES =
      "SELECT PopulationName, TaskId, TotalIteration, MinAggregationSize,\n"
          + " MaxAggregationSize, StartTaskNoEarlierThan, DoNotCreateIterationAfter,\n"
          + " MaxParallel, CreatedTime, CorrelationId, MinClientVersion, MaxClientVersion,\n"
          + " Status, Info\n"
          + " FROM Task\n";

  private static final String SELECT_ITERATIONS =
      "SELECT PopulationName, TaskId, IterationId, AttemptId, Status, BaseIterationId,"
          + " BaseOnResultId, ReportGoal, ResultId, Info, AggregationLevel, MaxAggregationSize,\n"
          + " MinClientVersion, MaxClientVersion,\n"
          + "FROM Iteration\n";

  private static final String INSERT_INTO_TASK_STATUS_HISTORY =
      "INSERT INTO TaskStatusHistory (\n"
          + " PopulationName,\n"
          + " TaskId,\n"
          + " StatusId,\n"
          + " Status,\n"
          + " CreatedTime)\n"
          + " VALUES(\n"
          + " @populationName,\n"
          + " @taskId,\n"
          + " @statusId,\n"
          + " @status,\n"
          + " @createdTime)\n";

  private static final String INSERT_INTO_ITERATION_STATUS_HISTORY =
      "INSERT INTO IterationStatusHistory (\n"
          + " PopulationName,\n"
          + " TaskId,\n"
          + " IterationId,\n"
          + " AttemptId,\n"
          + " StatusId,\n"
          + " Status,\n"
          + " CreatedTime,"
          + " AggregationLevel)\n"
          + " VALUES(\n"
          + " @populationName,\n"
          + " @taskId,\n"
          + " @iterationId,\n"
          + " @attemptId,\n"
          + " @statusId,\n"
          + " @status,\n"
          + " @createdTime,"
          + " @aggregationLevel)\n";

  private static final String UPDATE_TASK_STATUS =
      "UPDATE Task\n"
          + " SET Status = @newStatus\n"
          + " WHERE PopulationName=@populationName\n"
          + " AND TaskId=@taskId\n"
          + " AND Status=@oldStatus\n";

  private static final String SELECT_TASK_STATUS_ID_OF_STATUS =
      "SELECT MAX(StatusId) as StatusId\n"
          + " FROM TaskStatusHistory\n"
          + " WHERE PopulationName = @populationName\n"
          + "  AND TaskId = @taskId\n"
          + "  AND Status = @status\n";

  private static final String SELECT_ITERATION_STATUS_ID_OF_STATUS =
      "SELECT MAX(StatusId) as StatusId\n"
          + " FROM IterationStatusHistory\n"
          + " WHERE PopulationName = @populationName\n"
          + "  AND TaskId = @taskId\n"
          + "  AND IterationId = @iterationId\n"
          + "  AND AttemptId = @attemptId\n"
          + "  AND Status = @status\n"
          + "  AND AggregationLevel = @aggregationLevel\n";

  private static final String UPDATE_ITERATION_STATUS =
      "UPDATE Iteration\n"
          + " SET Status = @newStatus,\n"
          + " AggregationLevel = @newAggregationLevel"
          + " WHERE PopulationName=@populationName\n"
          + " AND TaskId=@taskId\n"
          + " AND IterationId=@iterationId\n"
          + " AND AttemptId=@attemptId\n"
          + " AND Status=@oldStatus\n"
          + " AND AggregationLevel=@oldAggregationLevel\n";

  private DatabaseClient dbClient;

  private InstantSource instantSource;
  private Cache<String, Long> cache;

  public TaskSpannerDao(
      @Qualifier("taskDatabaseClient") DatabaseClient dbClient, InstantSource instantSource) {
    this.dbClient = dbClient;
    this.instantSource = instantSource;
    this.cache = CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.SECONDS).build();
  }

  public Optional<TaskEntity> getTaskById(String populationName, long taskId) {
    return getTaskById(populationName, taskId, dbClient.singleUseReadOnlyTransaction());
  }

  private Optional<TaskEntity> getTaskById(
      String populationName, long taskId, ReadContext transaction) {
    Statement statement =
        Statement.newBuilder(
                SELECT_TASK_ENTITIES
                    + "WHERE PopulationName = @populationName\n"
                    + "AND TaskId = @taskId")
            .bind("populationName")
            .to(populationName)
            .bind("taskId")
            .to(taskId)
            .build();

    return queryTaskEntities(transaction, statement).stream().findFirst();
  }

  public TaskEntity createTask(TaskEntity entity) {
    Timestamp now = TimestampInstantConverter.TO_TIMESTAMP.convert(instantSource.instant());
    return dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              List<TaskId> activeTaskIds = getActiveTaskIdsWithSameType(transaction, entity);

              if (!activeTaskIds.isEmpty()) {
                throw SpannerExceptionFactory.newSpannerException(
                    ErrorCode.FAILED_PRECONDITION,
                    "failed to create new created task: only one active training and/or one"
                        + " evaluation task is allowed under one population, there are already"
                        + " active tasks: "
                        + activeTaskIds);
              }

              ResultSet resultSetMaxTaskId =
                  transaction.executeQuery(
                      Statement.newBuilder(
                              "SELECT MAX(TaskId) as MaxTaskId FROM Task WHERE PopulationName =\n"
                                  + " @populationName")
                          .bind("populationName")
                          .to(entity.getPopulationName())
                          .build());
              long maxTaskId = 0;
              while (resultSetMaxTaskId.next()) {
                if (!resultSetMaxTaskId.isNull("MaxTaskId")) {
                  maxTaskId = resultSetMaxTaskId.getLong("MaxTaskId");
                }
                break;
              }

              long newTaskId = maxTaskId + 1;

              // create task.
              createTask(transaction, entity, newTaskId, now);

              // insert status
              TaskId taskId =
                  TaskId.builder()
                      .populationName(entity.getPopulationName())
                      .taskId(newTaskId)
                      .build();
              insertTaskStatusHistory(transaction, taskId, 0, entity.getStatus(), now);

              return getTaskById(entity.getPopulationName(), newTaskId, transaction)
                  .orElseThrow(
                      () ->
                          new IllegalStateException(
                              "failed to get new created task, id: " + newTaskId));
            });
  }

  private List<TaskId> getActiveTaskIdsWithSameType(ReadContext transaction, TaskEntity newEntity) {
    String populationName = newEntity.getPopulationName();
    Statement statement =
        Statement.newBuilder(
                SELECT_TASK_ENTITIES
                    + "WHERE PopulationName = @populationName\n"
                    + "AND Status in UNNEST(@status)")
            .bind("populationName")
            .to(populationName)
            .bind("status")
            .toInt64Array(Status.getActiveStatus().stream().map(Status::code).toList())
            .build();
    return queryTaskEntities(transaction, statement).stream()
        .filter(
            dbEntity ->
                dbEntity.getProtoInfo().getInfoCase() == newEntity.getProtoInfo().getInfoCase())
        .map(TaskEntity::getId)
        .toList();
  }

  private List<TaskEntity> queryTaskEntities(ReadContext transaction, Statement statement) {
    try (ResultSet resultSet = transaction.executeQuery(statement)) {
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
                .createdTime(
                    TimestampInstantConverter.TO_INSTANT.convert(
                        resultSet.getTimestamp("CreatedTime")))
                .info(Optional.ofNullable(resultSet.getJson("Info")).orElse(""))
                .build());
      }
      return entitiesBuilder.build();
    }
  }

  public List<TaskEntity> getActiveTasks() {
    // TODO(b/295241356): Update query limit and order to better values
    Statement.Builder statementBuilder =
        Statement.newBuilder(
                SELECT_TASK_ENTITIES
                    + "WHERE Status = @status ORDER BY PopulationName, TaskId DESC LIMIT 1000\n")
            .bind("status")
            .to(TaskEntity.Status.OPEN.code());

    return queryTaskEntities(dbClient.singleUseReadOnlyTransaction(), statementBuilder.build());
  }

  public List<TaskEntity> getCreatedTasks() {
    // TODO(b/295241356): Update query limit and order to better values
    Statement.Builder statementBuilder =
        Statement.newBuilder(
                SELECT_TASK_ENTITIES
                    + "WHERE Status = @status ORDER BY PopulationName, TaskId DESC LIMIT 1000\n")
            .bind("status")
            .to(TaskEntity.Status.CREATED.code());

    return queryTaskEntities(dbClient.singleUseReadOnlyTransaction(), statementBuilder.build());
  }

  public Optional<IterationEntity> getIterationById(IterationId iterationId) {
    Statement statement =
        Statement.newBuilder(
                SELECT_ITERATIONS
                    + "WHERE PopulationName = @populationName\n"
                    + "AND TaskId = @taskId\n"
                    + "AND iterationId = @iterationId\n"
                    + "AND attemptId = @attemptId")
            .bind("populationName")
            .to(iterationId.getPopulationName())
            .bind("iterationId")
            .to(iterationId.getIterationId())
            .bind("taskId")
            .to(iterationId.getTaskId())
            .bind("attemptId")
            .to(iterationId.getAttemptId())
            .build();
    try (ResultSet resultSet =
        dbClient
            .singleUse() // Execute a single read or query against Cloud Spanner.
            .executeQuery(statement)) {
      return extractIterationEntitiesFromResultSet(resultSet).stream().findFirst();
    }
  }

  public Optional<IterationEntity> getLastIterationOfTask(String populationName, long taskId) {
    Statement statement =
        Statement.newBuilder(
                SELECT_ITERATIONS
                    + "WHERE PopulationName = @populationName\n"
                    + "AND TaskId = @taskId\n"
                    + "ORDER BY IterationId DESC, AttemptId DESC\n"
                    + "LIMIT 1")
            .bind("populationName")
            .to(populationName)
            .bind("taskId")
            .to(taskId)
            .build();
    try (ResultSet resultSet =
        dbClient
            .singleUse() // Execute a single read or query against Cloud Spanner.
            .executeQuery(statement)) {
      return extractIterationEntitiesFromResultSet(resultSet).stream().findFirst();
    }
  }

  public List<IterationEntity> getOpenIterations(String populationName, String clientVersion) {
    Statement statement =
        Statement.newBuilder(
                SELECT_ITERATIONS
                    + "@{FORCE_INDEX=IterationPopulationNameStatusClientVersionIndex}\n"
                    + "WHERE Status = @status\n"
                    + "  AND PopulationName = @populationName\n"
                    + "  AND MinClientVersion <= @clientVersion\n"
                    + "  AND MaxClientVersion >= @clientVersion")
            .bind("status")
            .to(IterationEntity.Status.COLLECTING.code())
            .bind("clientVersion")
            .to(clientVersion)
            .bind("populationName")
            .to(populationName)
            .build();

    ImmutableList.Builder<IterationEntity> builder = ImmutableList.builder();
    try (ReadOnlyTransaction transaction = dbClient.readOnlyTransaction()) {
      ResultSet resultSet = transaction.executeQuery(statement);
      while (resultSet.next()) {
        IterationEntity iterationEntity =
            IterationEntity.builder()
                .populationName(resultSet.getString("PopulationName"))
                .taskId(resultSet.getLong("TaskId"))
                .iterationId(resultSet.getLong("IterationId"))
                .attemptId(resultSet.getLong("AttemptId"))
                .reportGoal(resultSet.getLong("ReportGoal"))
                .status(IterationEntity.Status.fromCode(resultSet.getLong("Status")))
                .baseIterationId(resultSet.getLong("BaseIterationId"))
                .baseOnResultId(resultSet.getLong("BaseOnResultId"))
                .resultId(resultSet.getLong("ResultId"))
                .info(resultSet.getJson("Info"))
                .maxAggregationSize(resultSet.getLong("MaxAggregationSize"))
                .minClientVersion(resultSet.getString("MinClientVersion"))
                .maxClientVersion(resultSet.getString("MaxClientVersion"))
                .build();
        // If recently rejected a request for this population due to too many active assignments,
        // cache and
        // reject again to reduce running this query.
        if (cache.getIfPresent(iterationEntity.getId().toString()) == null) {
          logger.debug("Cache miss for iteration. Querying.");
          ResultSet countResult =
              transaction.executeQuery(
                  Statement.newBuilder(
                          "SELECT COUNT(*) as assigned FROM Assignment WHERE\n"
                              + "    PopulationName = @populationName\n"
                              + "    AND TaskId = @taskId\n"
                              + "    AND IterationId = @iterationId\n"
                              + "    AND AttemptId = @attemptId\n"
                              + "    AND Status <= @maxActiveStatus\n"
                              + "    HAVING assigned < @maxAggregationSize;")
                      .bind("populationName")
                      .to(iterationEntity.getPopulationName())
                      .bind("taskId")
                      .to(iterationEntity.getTaskId())
                      .bind("iterationId")
                      .to(iterationEntity.getIterationId())
                      .bind("attemptId")
                      .to(iterationEntity.getAttemptId())
                      .bind("maxActiveStatus")
                      .to(Constants.MAX_ACTIVE_ASSIGNMENT_STATUS_CODE)
                      .bind("maxAggregationSize")
                      .to(iterationEntity.getMaxAggregationSize())
                      .build());
          if (countResult.next()) {
            // there should be only one
            builder.add(iterationEntity);
          } else {
            // Cache the result of activeAssignments > maxAggregationSize
            cache.put(iterationEntity.getId().toString(), iterationEntity.getMaxAggregationSize());
          }
        }
      }
    }
    return builder.build();
  }

  private static List<IterationEntity> extractIterationEntitiesFromResultSet(ResultSet resultSet) {
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
              .info(resultSet.getJson("Info"))
              .aggregationLevel(resultSet.getLong("AggregationLevel"))
              .maxAggregationSize(resultSet.getLong("MaxAggregationSize"))
              .minClientVersion(resultSet.getString("MinClientVersion"))
              .maxClientVersion(resultSet.getString("MaxClientVersion"))
              .build());
    }
    return entitiesBuilder.build();
  }

  public IterationEntity createIteration(IterationEntity iteration) {
    Timestamp now = TimestampInstantConverter.TO_TIMESTAMP.convert(instantSource.instant());
    return dbClient
        .readWriteTransaction()
        .run(
            transaction -> {
              // insert new iteration in iteration table.
              createIteration(transaction, iteration);

              // insert status history
              insertIterationStatusHistory(
                  transaction,
                  iteration.getId(),
                  Constants.FIRST_ITERATION_STATUS_ID,
                  iteration.getStatus(),
                  now,
                  iteration.getAggregationLevel());

              // get the status.
              return getIterationById(
                      iteration.getPopulationName(),
                      iteration.getTaskId(),
                      iteration.getIterationId(),
                      iteration.getAttemptId(),
                      transaction)
                  .orElseThrow(
                      () ->
                          new IllegalStateException(
                              "failed to get new created iteration, id: "
                                  + iteration.getPopulationName()
                                  + "/"
                                  + iteration.getTaskId()
                                  + "/"
                                  + iteration.getIterationId()
                                  + "/"
                                  + iteration.getAttemptId()));
            });
  }

  private void createIteration(TransactionContext transaction, IterationEntity iteration) {
    Statement statement =
        Statement.newBuilder(
                "INSERT INTO Iteration (PopulationName, TaskId, IterationId, AttemptId, Status,"
                    + " BaseIterationId, BaseOnResultId, ReportGoal, Info, ExpirationTime,"
                    + " ResultId, AggregationLevel, MaxAggregationSize, MinClientVersion,"
                    + " MaxClientVersion) VALUES(@populationName, @taskId, @iterationId,"
                    + " @attemptId, @status, @baseIterationId, @baseResultId, @reportGoal, @info,"
                    + " TIMESTAMP_ADD(CURRENT_TIMESTAMP(), INTERVAL 1 HOUR), @resultId,"
                    + " @aggregationLevel, @maxAggregationSize, "
                    + "@minClientVersion, @maxClientVersion)")
            .bind("populationName")
            .to(iteration.getPopulationName())
            .bind("taskId")
            .to(iteration.getTaskId())
            .bind("iterationId")
            .to(iteration.getIterationId())
            .bind("attemptId")
            .to(iteration.getAttemptId())
            .bind("status")
            .to(iteration.getStatus().code())
            .bind("baseIterationId")
            .to(iteration.getBaseIterationId())
            .bind("baseResultId")
            .to(iteration.getBaseOnResultId())
            .bind("reportGoal")
            .to(iteration.getReportGoal())
            .bind("resultId")
            .to(iteration.getResultId())
            .bind("info")
            .to(Value.json(iteration.getInfo()))
            .bind("aggregationLevel")
            .to(iteration.getAggregationLevel())
            .bind("maxAggregationSize")
            .to(iteration.getMaxAggregationSize())
            .bind("minClientVersion")
            .to(iteration.getMinClientVersion())
            .bind("maxClientVersion")
            .to(iteration.getMaxClientVersion())
            .build();
    long impatedRows = transaction.executeUpdate(statement);
    if (impatedRows > 1) {
      throw new IllegalStateException(
          "should not insert more than 1 rows when creating iteration, iteration id: "
              + iteration.getPopulationName()
              + "/"
              + iteration.getTaskId()
              + "/"
              + iteration.getIterationId()
              + "/"
              + iteration.getAttemptId());
    }
  }

  private void createTask(
      TransactionContext transaction, TaskEntity entity, long newTaskId, Timestamp createdTime) {
    Statement createTaskStatement =
        Statement.newBuilder(
                "INSERT INTO Task(PopulationName, TaskId, TotalIteration,\n"
                    + " MinAggregationSize,\n"
                    + " MaxAggregationSize, Status, MaxParallel, CorrelationId,\n"
                    + " CreatedTime,\n"
                    + " MinClientVersion,\n"
                    + " MaxClientVersion,\n"
                    + " Info)\n"
                    + " VALUES(@populationName, @taskId, @totalIteration,\n"
                    + " @minAggregationSize, @maxAggregationSize, @status,\n"
                    + " @maxParallel,\n"
                    + " @correlationId, @createdTime, @minClientVersion,\n"
                    + " @maxClientVersion, @info)\n")
            .bind("populationName")
            .to(entity.getPopulationName())
            .bind("taskId")
            .to(newTaskId)
            .bind("totalIteration")
            .to(entity.getTotalIteration())
            .bind("minAggregationSize")
            .to(entity.getMinAggregationSize())
            .bind("maxAggregationSize")
            .to(entity.getMaxAggregationSize())
            .bind("status")
            .to(entity.getStatus().code())
            .bind("maxParallel")
            .to(entity.getMaxParallel())
            .bind("correlationId")
            .to(entity.getCorrelationId())
            .bind("createdTime")
            .to(createdTime)
            .bind("minClientVersion")
            .to(entity.getMinClientVersion())
            .bind("maxClientVersion")
            .to(entity.getMaxClientVersion())
            .bind("info")
            .to(Value.json(entity.getInfo()))
            .build();
    long numTaskCreated = transaction.executeUpdate(createTaskStatement);
    if (numTaskCreated != 1) {
      throw new IllegalStateException("Task insertion impacted " + numTaskCreated + " rows.");
    }
  }

  private Optional<IterationEntity> getIterationById(
      String populationName,
      long taskId,
      long iterationId,
      long attemptId,
      ReadContext transaction) {
    Statement statement =
        Statement.newBuilder(
                SELECT_ITERATIONS
                    + "WHERE PopulationName = @populationName\n"
                    + "AND TaskId = @taskId\n"
                    + "AND IterationId = @iterationId\n"
                    + "AND AttemptId = @attemptId")
            .bind("populationName")
            .to(populationName)
            .bind("taskId")
            .to(taskId)
            .bind("iterationId")
            .to(iterationId)
            .bind("attemptId")
            .to(attemptId)
            .build();

    try (ResultSet resultSet = transaction.executeQuery(statement)) {
      return extractIterationEntitiesFromResultSet(resultSet).stream().findFirst();
    }
  }

  public boolean updateIterationStatus(IterationEntity from, IterationEntity to) {
    Preconditions.checkArgument(from.getId().equals(to.getId()));
    Timestamp now = TimestampInstantConverter.TO_TIMESTAMP.convert(instantSource.instant());
    return getLastVersionOfIterationStatus(
            from.getId(), from.getStatus(), from.getAggregationLevel())
        .map(
            currentStatusId -> {
              try {
                return dbClient
                    .readWriteTransaction()
                    .run(
                        transaction -> {
                          // insert new status to status history
                          insertIterationStatusHistory(
                              transaction,
                              to.getId(),
                              currentStatusId + 1,
                              to.getStatus(),
                              now,
                              to.getAggregationLevel());

                          // update status in iteration table.
                          updateIterationStatus(transaction, from, to);
                          return true;
                        });
              } catch (SpannerException e) {
                logger.atWarn().setCause(e).log("Failed to update status.");
                return false;
              }
            })
        .orElse(false);
  }

  private void updateIterationStatus(
      TransactionContext transaction, IterationEntity from, IterationEntity to) {
    Statement statement =
        bindIterationId(Statement.newBuilder(UPDATE_ITERATION_STATUS), from.getId())
            .bind("oldStatus")
            .to(from.getStatus().code())
            .bind("newStatus")
            .to(to.getStatus().code())
            .bind("oldAggregationLevel")
            .to(from.getAggregationLevel())
            .bind("newAggregationLevel")
            .to(to.getAggregationLevel())
            .build();
    long updatedRowCount = transaction.executeUpdate(statement);
    if (updatedRowCount != 1) {
      throw new IllegalStateException(
          String.format(
              "%s rows are impacted when updating %s from %s to %s.",
              updatedRowCount, from.getId().toString(), from, to));
    }
  }

  public boolean updateTaskStatus(TaskId taskId, TaskEntity.Status from, TaskEntity.Status to) {

    Timestamp now = TimestampInstantConverter.TO_TIMESTAMP.convert(instantSource.instant());
    return getLastVersionOfTaskStatus(taskId, from)
        .map(
            currentStatusId -> {
              try {
                return dbClient
                    .readWriteTransaction()
                    .run(
                        transaction -> {
                          // insert task
                          insertTaskStatusHistory(
                              transaction, taskId, currentStatusId + 1, to, now);

                          // update status
                          updateTaskStatus(transaction, taskId, from, to);
                          return true;
                        });
              } catch (SpannerException e) {
                return false;
              }
            })
        .orElse(false);
  }

  private void updateTaskStatus(
      TransactionContext transaction, TaskId taskId, TaskEntity.Status from, TaskEntity.Status to) {
    Statement statement =
        bindTaskId(Statement.newBuilder(UPDATE_TASK_STATUS), taskId)
            .bind("oldStatus")
            .to(from.code())
            .bind("newStatus")
            .to(to.code())
            .build();
    long updatedRowCount = transaction.executeUpdate(statement);
    if (updatedRowCount != 1) {
      throw new IllegalStateException(
          String.format(
              "%s rows are impacted when updating %s from %s to %s.",
              updatedRowCount, taskId.toString(), from, to));
    }
  }

  private Optional<Long> getLastVersionOfIterationStatus(
      IterationId iterationId, IterationEntity.Status status, long aggregationLevel) {
    Statement statement =
        bindIterationId(Statement.newBuilder(SELECT_ITERATION_STATUS_ID_OF_STATUS), iterationId)
            .bind("status")
            .to(status.code())
            .bind("aggregationLevel")
            .to(aggregationLevel)
            .build();
    try (ResultSet resultSet = dbClient.singleUseReadOnlyTransaction().executeQuery(statement)) {
      while (resultSet.next()) {
        // there should be only one
        if (!resultSet.isNull("StatusId")) {
          return Optional.of(resultSet.getLong("StatusId"));
        }
      }
    }

    return Optional.empty();
  }

  private Optional<Long> getLastVersionOfTaskStatus(TaskId taskId, TaskEntity.Status status) {
    Statement statement =
        bindTaskId(Statement.newBuilder(SELECT_TASK_STATUS_ID_OF_STATUS), taskId)
            .bind("status")
            .to(status.code())
            .build();
    try (ResultSet resultSet = dbClient.singleUseReadOnlyTransaction().executeQuery(statement)) {
      while (resultSet.next()) {
        // there should be only one
        if (!resultSet.isNull("StatusId")) {
          return Optional.of(resultSet.getLong("StatusId"));
        }
      }
    }

    return Optional.empty();
  }

  private void insertIterationStatusHistory(
      TransactionContext transaction,
      IterationId iterationId,
      long statusId,
      IterationEntity.Status status,
      Timestamp createdTime,
      long aggregationLevel) {
    Statement statement =
        bindIterationId(Statement.newBuilder(INSERT_INTO_ITERATION_STATUS_HISTORY), iterationId)
            .bind("statusId")
            .to(statusId)
            .bind("status")
            .to(status.code())
            .bind("createdTime")
            .to(createdTime)
            .bind("aggregationLevel")
            .to(aggregationLevel)
            .build();
    long historyInserted = transaction.executeUpdate(statement);
    if (historyInserted != 1) {
      throw new IllegalStateException(
          "IterationStatusHistory insertion impacted " + historyInserted + " rows.");
    }
  }

  private void insertTaskStatusHistory(
      TransactionContext transaction,
      TaskId taskId,
      long statusId,
      TaskEntity.Status status,
      Timestamp createdTime) {
    Statement statement =
        bindTaskId(Statement.newBuilder(INSERT_INTO_TASK_STATUS_HISTORY), taskId)
            .bind("statusId")
            .to(statusId)
            .bind("status")
            .to(status.code())
            .bind("createdTime")
            .to(createdTime)
            .build();
    long historyInserted = transaction.executeUpdate(statement);
    if (historyInserted != 1) {
      throw new IllegalStateException(
          "TaskStatusHistory insertion impacted " + historyInserted + " rows.");
    }
  }

  private Statement.Builder bindIterationId(Statement.Builder builder, IterationId iterId) {
    return builder
        .bind("populationName")
        .to(iterId.getPopulationName())
        .bind("taskId")
        .to(iterId.getTaskId())
        .bind("iterationId")
        .to(iterId.getIterationId())
        .bind("attemptId")
        .to(iterId.getAttemptId());
  }

  private Statement.Builder bindTaskId(Statement.Builder builder, TaskId iterId) {
    return builder
        .bind("populationName")
        .to(iterId.getPopulationName())
        .bind("taskId")
        .to(iterId.getTaskId());
  }

  public List<IterationEntity> getIterationsOfStatus(IterationEntity.Status status) {
    // TODO(b/295241356): Update query limit and order to better values
    Statement statement =
        Statement.newBuilder(
                SELECT_ITERATIONS
                    + "@{FORCE_INDEX=InterationStatusIndex}\n"
                    + "WHERE Status = @status\n"
                    + "ORDER BY PopulationName, TaskId DESC, IterationId DESC, AttemptId DESC LIMIT"
                    + " 1000\n")
            .bind("status")
            .to(status.code())
            .build();
    try (ResultSet resultSet =
        dbClient
            .singleUse() // Execute a single read or query against Cloud Spanner.
            .executeQuery(statement)) {
      return extractIterationEntitiesFromResultSet(resultSet);
    }
  }

  @Override
  public List<Long> getIterationIdsPerEveryKIterationsSelector(
      String populationName, long taskId, long iterationInterval, long withInPastHours) {
    Timestamp earliestTime =
        TimestampInstantConverter.TO_TIMESTAMP.convert(
            instantSource.instant().minus(withInPastHours, ChronoUnit.HOURS));
    String query =
        "SELECT MIN(itr.IterationId) as Id,\n"
            + "FLOOR((itr.IterationId - 1) / @iterationInterval) AS BucketNumber\n"
            + "FROM IterationStatusHistory itr\n"
            + "Where PopulationName=@populationName and TaskId=@taskId and Status=@status\n"
            + "and CreatedTime >= @earliestTime\n"
            + "GROUP BY BucketNumber\n"
            + "ORDER BY BucketNumber";

    Statement statement =
        Statement.newBuilder(query)
            .bind("populationName")
            .to(populationName)
            .bind("taskId")
            .to(taskId)
            .bind("iterationInterval")
            .to(iterationInterval)
            .bind("earliestTime")
            .to(earliestTime)
            .bind("status")
            .to(IterationEntity.Status.COMPLETED.code())
            .build();

    ImmutableList.Builder<Long> result = ImmutableList.builder();
    try (ResultSet resultSet = dbClient.singleUseReadOnlyTransaction().executeQuery(statement)) {
      while (resultSet.next()) {
        result.add(resultSet.getLong("Id"));
      }
    }

    return result.build();
  }

  public List<Long> getIterationIdsPerEveryKHoursSelector(
      String populationName, long taskId, long intervalInHours, long withInHours) {
    Timestamp earliestTime =
        TimestampInstantConverter.TO_TIMESTAMP.convert(
            instantSource.instant().minus(withInHours, ChronoUnit.HOURS));
    long intervalInSecond = intervalInHours * 3600;
    String query =
        "SELECT MIN(itr.IterationId) as Id,\n"
            + "FLOOR(UNIX_SECONDS(itr.CreatedTime) / @intervalInSecond) as BucketNumber\n"
            + "FROM IterationStatusHistory itr\n"
            + "Where PopulationName=@populationName and TaskId=@taskId and Status=@status\n"
            + "and CreatedTime >= @earliestTime\n"
            + "GROUP BY BucketNumber\n"
            + "ORDER BY BucketNumber";

    Statement statement =
        Statement.newBuilder(query)
            .bind("populationName")
            .to(populationName)
            .bind("taskId")
            .to(taskId)
            .bind("intervalInSecond")
            .to(intervalInSecond)
            .bind("earliestTime")
            .to(earliestTime)
            .bind("status")
            .to(IterationEntity.Status.COMPLETED.code())
            .build();

    ImmutableList.Builder<Long> result = ImmutableList.builder();
    try (ResultSet resultSet = dbClient.singleUseReadOnlyTransaction().executeQuery(statement)) {
      while (resultSet.next()) {
        result.add(resultSet.getLong("Id"));
      }
    }

    return result.build();
  }

  public Optional<Instant> getIterationCreatedTime(IterationEntity iterationEntity) {
    String query =
        "SELECT CreatedTime\n"
            + "FROM IterationStatusHistory\n"
            + "Where PopulationName=@populationName and TaskId=@taskId\n"
            + "and IterationId=@iterationId and Status=@status\n"
            + "and AttemptId=@attemptId and AggregationLevel=@aggregationLevel\n"
            + "ORDER BY CreatedTime DESC\n"
            + "LIMIT 1";
    Statement statement =
        Statement.newBuilder(query)
            .bind("populationName")
            .to(iterationEntity.getPopulationName())
            .bind("taskId")
            .to(iterationEntity.getTaskId())
            .bind("iterationId")
            .to(iterationEntity.getIterationId())
            .bind("status")
            .to(iterationEntity.getStatus().code())
            .bind("AttemptId")
            .to(iterationEntity.getAttemptId())
            .bind("AggregationLevel")
            .to(iterationEntity.getAggregationLevel())
            .build();
    Optional<Instant> createdTime = Optional.empty();
    try (ResultSet resultSet =
        dbClient
            .singleUse() // Execute a single read or query against Cloud Spanner.
            .executeQuery(statement)) {
      while (resultSet.next()) {
        createdTime = Optional.of(TimestampInstantConverter.TO_INSTANT.convert(
            resultSet.getTimestamp("CreatedTime")));
      }
    }
    return createdTime;
  }
}
