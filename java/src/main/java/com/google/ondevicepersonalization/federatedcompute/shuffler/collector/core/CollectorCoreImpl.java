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

package com.google.ondevicepersonalization.federatedcompute.shuffler.collector.core;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.Constants;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AggregationBatchDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AggregationBatchEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AssignmentDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AssignmentEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AssignmentId;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobManager;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.IterationEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.IterationEntity.Status;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.messaging.MessageSender;
import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.stereotype.Service;

/** The task scheduler implementation. */
@Service
public class CollectorCoreImpl implements CollectorCore {

  private static final Logger logger = LoggerFactory.getLogger(CollectorCoreImpl.class);
  private static final List<String> UUID_PREFIX_LIST = Arrays.asList("0123456789abcdef".split(""));

  private static final String LOCK_PREFIX = "collector_";

  private TaskDao taskDao;
  private BlobDao blobDao;
  private AssignmentDao assignmentDao;
  private AggregationBatchDao aggregationBatchDao;
  private BlobManager blobManager;
  private CollectorCoreImplHelper collectorCoreImplHelper;
  private InstantSource instantSource;
  private MessageSender messageSender;
  private String aggregatorPubsubTopic;
  private String modelUpdaterPubsubTopic;
  private LockRegistry lockRegistry;
  private final int localComputeTimeoutMinutes;
  private final int uploadTimeoutMinutes;
  private final int batchSize;

  public CollectorCoreImpl(
      TaskDao taskDao,
      AssignmentDao assignmentDao,
      AggregationBatchDao aggregationBatchDao,
      BlobDao blobDao,
      BlobManager blobManager,
      CollectorCoreImplHelper collectorCoreImplHelper,
      InstantSource instantSource,
      MessageSender messageSender,
      String aggregatorPubsubTopic,
      String modelUpdaterPubsubTopic,
      LockRegistry lockRegistry,
      int localComputeTimeoutMinutes,
      int uploadTimeoutMinutes,
      int collectorBatchSize) {
    this.taskDao = taskDao;
    this.blobDao = blobDao;
    this.blobManager = blobManager;
    this.collectorCoreImplHelper = collectorCoreImplHelper;
    this.instantSource = instantSource;
    this.assignmentDao = assignmentDao;
    this.aggregationBatchDao = aggregationBatchDao;
    this.messageSender = messageSender;
    this.aggregatorPubsubTopic = aggregatorPubsubTopic;
    this.modelUpdaterPubsubTopic = modelUpdaterPubsubTopic;
    this.lockRegistry = lockRegistry;
    this.localComputeTimeoutMinutes = localComputeTimeoutMinutes;
    this.uploadTimeoutMinutes = uploadTimeoutMinutes;
    this.batchSize = collectorBatchSize;
  }

  private static String trimSlash(String folderName) {
    return folderName.endsWith("/") ? folderName.substring(0, folderName.length() - 1) : folderName;
  }

  public void processCollecting() {
    try {
      MDC.put(Constants.ACTIVITY_ID, UUID.randomUUID().toString());
      MDC.put(Constants.STATUS_ID, "COLLECTING");
      taskDao.getIterationsOfStatus(IterationEntity.Status.COLLECTING).stream()
          .forEach(this::processIteration);
    } finally {
      MDC.clear();
    }
  }

  public void processAggregating() {
    try {
      MDC.put(Constants.ACTIVITY_ID, UUID.randomUUID().toString());
      MDC.put(Constants.STATUS_ID, "AGGREGATING");
      taskDao.getIterationsOfStatus(Status.AGGREGATING).stream().forEach(this::processIteration);
    } finally {
      MDC.clear();
    }
  }

  public void processTimeouts() {
    try {
      MDC.put(Constants.ACTIVITY_ID, UUID.randomUUID().toString());
      MDC.put(Constants.STATUS_ID, "COLLECTING");
      taskDao.getIterationsOfStatus(IterationEntity.Status.COLLECTING).stream()
          .forEach(this::processTimeouts);
    } finally {
      MDC.clear();
    }
  }

  private void processTimeouts(IterationEntity iteration) {
    Instant startTime = instantSource.instant();
    try {
      String partition = iteration.getId().toString();
      Lock lock = lockRegistry.obtain("timeout_" + LOCK_PREFIX + partition);
      if (lock.tryLock()) {
        try {
          MDC.put(Constants.ITERATION_ID, iteration.getId().toString());
          if (Status.COLLECTING == iteration.getStatus()) {
            // mark the assignment timeout if result is not uploaded in time.
            queryAndSetUploadResultTimeout(iteration);

            // mark the assignment timeout if the local compute is not finished in time.
            queryAndSetLocalComputeTimeout(iteration);
          }
        } finally {
          lock.unlock();
          Duration duration = Duration.between(startTime, instantSource.instant());
          logger.info(
              "processing timeouts completed in {} second for iteration in status {}.",
              duration.getSeconds(),
              iteration.getStatus(),
              iteration.getAggregationLevel());
          MDC.remove(Constants.ITERATION_ID);
        }
      }
    } catch (Exception e) {
      logger.atError().setCause(e).log();
    }
  }

  private void processIteration(IterationEntity iteration) {
    Instant startTime = instantSource.instant();
    try {
      // TODO(b/321997430): Implement partitioning on gradient prefix
      String partition = iteration.getId().toString();
      Lock lock = lockRegistry.obtain(LOCK_PREFIX + partition);
      if (lock.tryLock()) {
        try {
          MDC.put(Constants.ITERATION_ID, iteration.getId().toString());
          if (Status.COLLECTING == iteration.getStatus()) {
            processCollectingIterationImp(iteration, partition);
          } else if (Status.AGGREGATING == iteration.getStatus()) {
            processAggregatingIterationImp(iteration, partition);
          }
        } finally {
          lock.unlock();
          Duration duration = Duration.between(startTime, instantSource.instant());
          logger.info(
              "processing completed in {} second for iteration in status {} level {}.",
              duration.getSeconds(),
              iteration.getStatus(),
              iteration.getAggregationLevel());
          MDC.remove(Constants.ITERATION_ID);
        }
      }
    } catch (Exception e) {
      logger.atError().setCause(e).log();
    }
  }

  private void processAggregatingIterationImp(IterationEntity iteration, String partition) {
    // Only support 1 level of aggregation for now. L0 - Clients, L1 - Intermediates
    if (iteration.getAggregationLevel() == 1) {
      handleFirstLevelAggregation(iteration, partition);
      return;
    }
    logger.error(
        "Invalid iteration {} AggregationLevel found {}",
        iteration.getId().toString(),
        iteration.getAggregationLevel());
  }

  private void handleFirstLevelAggregation(IterationEntity iteration, String partition) {
    // Scan GCS for uploaded intermediates
    Set<String> allUploadedBatchIds =
        blobDao
            .listByPartition(
                blobManager.generateDownloadAggregatedGradientDescription(iteration),
                UUID_PREFIX_LIST)
            .stream()
            .map(CollectorCoreImpl::trimSlash)
            .collect(Collectors.toSet());

    // Get all publish_completed and mark them upload_completed if their upload is detected.
    Set<String> allPublishCompleted =
        new HashSet<>(
            aggregationBatchDao.queryAggregationBatchIdsOfStatus(
                iteration,
                iteration.getAggregationLevel() - 1,
                AggregationBatchEntity.Status.PUBLISH_COMPLETED,
                Optional.empty()));
    Sets.intersection(allUploadedBatchIds, allPublishCompleted).stream()
        .parallel()
        .forEach(
            (batchId) -> {
              AggregationBatchEntity from =
                  AggregationBatchEntity.builder()
                      .populationName(iteration.getPopulationName())
                      .taskId(iteration.getTaskId())
                      .attemptId(iteration.getAttemptId())
                      .iterationId(iteration.getIterationId())
                      .batchId(batchId)
                      .status(AggregationBatchEntity.Status.PUBLISH_COMPLETED)
                      .aggregationLevel(iteration.getAggregationLevel() - 1)
                      .createdByPartition(partition)
                      .build();

              aggregationBatchDao.updateAggregationBatchStatus(
                  from,
                  from.toBuilder().status(AggregationBatchEntity.Status.UPLOAD_COMPLETED).build());
            });

    // Count all publish_completed.
    long publishedAssignments =
        aggregationBatchDao.querySumOfAggregationBatchesOfStatus(
            iteration,
            iteration.getAggregationLevel() - 1,
            AggregationBatchEntity.Status.UPLOAD_COMPLETED);

    // Check if report goal is met
    if (publishedAssignments >= iteration.getReportGoal()) {
      List<String> uploaded =
          aggregationBatchDao.queryAggregationBatchIdsOfStatus(
              iteration,
              iteration.getAggregationLevel() - 1,
              AggregationBatchEntity.Status.UPLOAD_COMPLETED,
              Optional.empty());
      // Send message to model updater
      messageSender.sendMessage(
          collectorCoreImplHelper.createModelUpdaterMessage(iteration, uploaded),
          modelUpdaterPubsubTopic);
      logger.info("Message sent to pubsub for iteration {}", iteration.getId().toString());
      // Update iteration state
      if (!taskDao.updateIterationStatus(
          iteration, iteration.toBuilder().status(Status.APPLYING).aggregationLevel(2).build())) {
        logger.error(
            "Failed to update iteration {} from {} to {}",
            iteration.getId().toString(),
            iteration.getStatus(),
            Status.AGGREGATING);
      }
    }
  }

  private void processCollectingIterationImp(IterationEntity iteration, String partition) {

    // check uploaded results and mark corresponding assignment upload_completed.
    Set<String> allUploadedAssignmentIds = queryAndSetUploadCompleted(iteration, partition);
    long uploadCompletedFromFolder = allUploadedAssignmentIds.size();

    // Batch assignments marked UPLOAD_COMPLETED without an assigned batch if threshold is met.
    List<String> leftoverAssignments = queryAndBatchLeftoverAssignments(iteration, partition);

    // Send any full batches ready to be published
    queryAndSendFullBatches(iteration, partition);

    // count the contribution and trigger next step.
    long publishedAssignmentsCount =
        countContributionsAndTriggerAggregation(iteration, leftoverAssignments, partition);

    if (uploadCompletedFromFolder < publishedAssignmentsCount) {
      logger.error(
          "iteration '{}', {} results found in folder but {} are upload_completed.",
          iteration.getId().toString(),
          uploadCompletedFromFolder,
          publishedAssignmentsCount);
    }
  }

  private List<String> queryAndBatchLeftoverAssignments(
      IterationEntity iteration, String partition) {
    List<String> unBatchedAssignments =
        assignmentDao.queryAssignmentIdsOfStatus(
            iteration.getId(), AssignmentEntity.Status.UPLOAD_COMPLETED, Optional.empty());

    return partitionAndBatchAssignments(
        iteration,
        unBatchedAssignments,
        partition,
        /* from= */ AssignmentEntity.Status.UPLOAD_COMPLETED,
        /* to= */ AssignmentEntity.Status.UPLOAD_COMPLETED);
  }

  private Set<String> queryAndSetUploadCompleted(IterationEntity iteration, String partition) {
    Set<String> allUploadedAssignmentIds =
        Arrays.stream(blobManager.generateDownloadGradientDescriptions(iteration))
            .map((folder) -> blobDao.listByPartition(folder, UUID_PREFIX_LIST))
            .flatMap(Collection::stream)
            .map(CollectorCoreImpl::trimSlash)
            .collect(Collectors.toSet());

    // Get all local_completed and mark them upload_completed if
    // their upload is detected.
    Set<String> allLocalCompleted =
        assignmentDao
            .queryAssignmentIdsOfStatus(
                iteration.getId(), AssignmentEntity.Status.LOCAL_COMPLETED, Optional.empty())
            .stream()
            .collect(Collectors.toSet());

    List<String> leftoverAssignments =
        partitionAndBatchAssignments(
            iteration,
            new ArrayList<>(
                new TreeSet<>(Sets.intersection(allUploadedAssignmentIds, allLocalCompleted))),
            partition,
            /* from= */ AssignmentEntity.Status.LOCAL_COMPLETED,
            /* to= */ AssignmentEntity.Status.UPLOAD_COMPLETED);
    batchUpdateAssignments(
        /* iteration= */ iteration,
        /* assignmentIds= */ leftoverAssignments,
        /* from= */ AssignmentEntity.Status.LOCAL_COMPLETED,
        /* to= */ AssignmentEntity.Status.UPLOAD_COMPLETED);

    return allUploadedAssignmentIds;
  }

  /**
   * Partitions the provided list into batches of the configured batchSize. Each batch will be
   * updated in the DB and corresponding assignments will have their status updated from {@code
   * from} to {@code to} and have their batchId set.
   *
   * @param iteration The current iteration of the assignments
   * @param assignments List of assignments to partition and batch
   * @param partition Current partition batching the assignments
   * @param from Current assignment status
   * @param to New assignmnet status
   * @return The list of remaining assignments whose batchSize was not enough to create a FULL
   *     batch.
   */
  private List<String> partitionAndBatchAssignments(
      IterationEntity iteration,
      List<String> assignments,
      String partition,
      AssignmentEntity.Status from,
      AssignmentEntity.Status to) {
    if (assignments.size() == 0) {
      return new ArrayList<>();
    }
    List<List<String>> batchedAssignmentIds = Lists.partition(assignments, batchSize);
    batchedAssignmentIds.parallelStream()
        .forEach(
            (assignmentIds) -> {
              if (assignmentIds.size() == batchSize) {
                assignmentDao.createBatchAndUpdateAssignments(
                    convertAssignmentIds(assignmentIds, iteration),
                    iteration,
                    /* from= */ from,
                    /* to= */ to,
                    /* batchId= */ UUID.randomUUID().toString(),
                    /* partition= */ partition);
              }
            });
    // Return un-batched assignments
    List<String> lastPartition = batchedAssignmentIds.get(batchedAssignmentIds.size() - 1);
    if (lastPartition.size() < batchSize) {
      return lastPartition;
    }
    return new ArrayList<>();
  }

  private void queryAndSendFullBatches(IterationEntity iteration, String partition) {
    List<String> batchIds =
        aggregationBatchDao.queryAggregationBatchIdsOfStatus(
            iteration,
            iteration.getAggregationLevel(),
            AggregationBatchEntity.Status.FULL,
            Optional.of(partition));
    batchIds.stream()
        .parallel()
        .forEach((batchId) -> createAndSendAggregationMessage(iteration, batchId, partition));
  }

  private boolean createAndSendAggregationMessage(
      IterationEntity iteration, String batchId, String partition) {
    List<String> assignments =
        assignmentDao.queryAssignmentIdsOfStatus(
            iteration.getId(), AssignmentEntity.Status.UPLOAD_COMPLETED, Optional.of(batchId));
    messageSender.sendMessage(
        collectorCoreImplHelper.createAggregatorMessage(
            iteration, assignments, Optional.of(batchId), false),
        aggregatorPubsubTopic);
    logger.info(
        "Message sent to pubsub for iteration {} and batch {}",
        iteration.getId().toString(),
        batchId);
    AggregationBatchEntity from =
        AggregationBatchEntity.builder()
            .populationName(iteration.getPopulationName())
            .taskId(iteration.getTaskId())
            .attemptId(iteration.getAttemptId())
            .iterationId(iteration.getIterationId())
            .batchId(batchId)
            .status(AggregationBatchEntity.Status.FULL)
            .aggregationLevel(iteration.getAggregationLevel())
            .createdByPartition(partition)
            .build();
    if (!aggregationBatchDao.updateAggregationBatchStatus(
        from, from.toBuilder().status(AggregationBatchEntity.Status.PUBLISH_COMPLETED).build())) {
      logger.error(
          "Failed to update batch {} from {} to {}",
          batchId,
          from.getStatus(),
          AggregationBatchEntity.Status.PUBLISH_COMPLETED);
      return false;
    }
    return true;
  }

  private long countContributionsAndTriggerAggregation(
      IterationEntity iteration, List<String> leftoverAssignments, String partition) {
    // Count all publish_completed.
    long publishedAssignments =
        aggregationBatchDao.querySumOfAggregationBatchesOfStatus(
            iteration,
            iteration.getAggregationLevel(),
            AggregationBatchEntity.Status.PUBLISH_COMPLETED);

    // Check if report goal is met with leftovers.
    // Batch remaining leftovers if applicable and updated total published assignments.
    if (publishedAssignments + leftoverAssignments.size() >= iteration.getReportGoal()) {
      if (createAndSendNewBatch(iteration, leftoverAssignments, partition)) {
        publishedAssignments += leftoverAssignments.size();
      }
    }

    // Final check for publishedAssignments and updating iteration status.
    if (publishedAssignments >= iteration.getReportGoal()) {
      // Update iteration state
      if (!taskDao.updateIterationStatus(
          iteration,
          iteration.toBuilder().status(Status.AGGREGATING).aggregationLevel(1).build())) {
        logger.error(
            "Failed to update iteration {} from {} to {}",
            iteration.getId().toString(),
            iteration.getStatus(),
            Status.AGGREGATING);
      }
    }
    return publishedAssignments;
  }

  private boolean createAndSendNewBatch(
      IterationEntity iteration, List<String> leftoverAssignments, String partition) {
    // Batch and send the remaining leftovers
    if (leftoverAssignments.size() > 0) {
      String newBatchId = UUID.randomUUID().toString();
      boolean createSuccess =
          assignmentDao.createBatchAndUpdateAssignments(
              convertAssignmentIds(leftoverAssignments, iteration),
              iteration,
              /* from= */ AssignmentEntity.Status.UPLOAD_COMPLETED,
              /* to= */ AssignmentEntity.Status.UPLOAD_COMPLETED,
              /* batchId= */ newBatchId,
              /* partition= */ partition);
      if (!createSuccess) {
        logger.error("Failed to create final batch");
        return false;
      }
      if (!createAndSendAggregationMessage(iteration, newBatchId, partition)) {
        logger.error("Failed to send final batch {}", newBatchId);
        return false;
      }
      return true;
    }
    return false;
  }

  private void queryAndSetUploadResultTimeout(IterationEntity iteration) {
    queryAndSetTimeout(
        iteration,
        AssignmentEntity.Status.LOCAL_COMPLETED,
        Duration.ofMinutes(uploadTimeoutMinutes),
        AssignmentEntity.Status.UPLOAD_TIMEOUT);
  }

  private void queryAndSetLocalComputeTimeout(IterationEntity iteration) {
    queryAndSetTimeout(
        iteration,
        AssignmentEntity.Status.ASSIGNED,
        Duration.ofMinutes(localComputeTimeoutMinutes),
        AssignmentEntity.Status.LOCAL_TIMEOUT);
  }

  private void queryAndSetTimeout(
      IterationEntity iteration,
      AssignmentEntity.Status currentStatus,
      Duration timeout,
      AssignmentEntity.Status timeoutStatus) {
    Instant timeoutThreshold = instantSource.instant().minusSeconds(timeout.getSeconds());
    batchUpdateAssignments(
        /* iteration= */ iteration,
        /* assignmentIds= */ assignmentDao.queryAssignmentIdsOfStatus(
            iteration.getId(), currentStatus, timeoutThreshold),
        /* from= */ currentStatus,
        /* to= */ timeoutStatus);
  }

  private void batchUpdateAssignments(
      IterationEntity iteration,
      Collection<String> assignmentIdStrings,
      AssignmentEntity.Status from,
      AssignmentEntity.Status to) {
    try {
      List<AssignmentId> assignmentIds = convertAssignmentIds(assignmentIdStrings, iteration);
      int updates = assignmentDao.batchUpdateAssignmentStatus(assignmentIds, from, to);
      if (updates != assignmentIdStrings.size()) {
        logger.warn(
            "Failed to update {} assignment statuses from {} to {}.",
            assignmentIdStrings.size() - updates,
            from,
            to);
      }
    } catch (Exception e) {
      logger.error("Failed to update assignment statuses.", e);
      throw new RuntimeException(e);
    }
  }

  private List<AssignmentId> convertAssignmentIds(
      Collection<String> assignmentIds, IterationEntity iteration) {
    return assignmentIds.stream()
        .map(
            assignmentId ->
                AssignmentId.builder()
                    .populationName(iteration.getPopulationName())
                    .taskId(iteration.getTaskId())
                    .iterationId(iteration.getIterationId())
                    .attemptId(iteration.getAttemptId())
                    .assignmentId(assignmentId)
                    .build())
        .collect(Collectors.toList());
  }
}
