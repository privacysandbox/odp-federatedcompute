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
import com.google.ondevicepersonalization.federatedcompute.shuffler.aggregator.core.message.AggregatorNotification;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.Constants;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AggregationBatchDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AggregationBatchEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AggregationBatchId;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AssignmentDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AssignmentEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AssignmentId;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobManager;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.IterationEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.IterationEntity.Status;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.IterationId;
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
import java.util.concurrent.TimeUnit;
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
  private String aggregatorNotificationEndpoint;
  private String modelUpdaterPubsubTopic;
  private LockRegistry lockRegistry;
  private final int localComputeTimeoutMinutes;
  private final int uploadTimeoutMinutes;
  private final int batchSize;
  private final Optional<Long> aggregationBatchFailureThreshold;

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
      String aggregatorNotificationEndpoint,
      String modelUpdaterPubsubTopic,
      LockRegistry lockRegistry,
      int localComputeTimeoutMinutes,
      int uploadTimeoutMinutes,
      int collectorBatchSize,
      Optional<Long> aggregationBatchFailureThreshold) {
    this.taskDao = taskDao;
    this.blobDao = blobDao;
    this.blobManager = blobManager;
    this.collectorCoreImplHelper = collectorCoreImplHelper;
    this.instantSource = instantSource;
    this.assignmentDao = assignmentDao;
    this.aggregationBatchDao = aggregationBatchDao;
    this.messageSender = messageSender;
    this.aggregatorPubsubTopic = aggregatorPubsubTopic;
    this.aggregatorNotificationEndpoint = aggregatorNotificationEndpoint;
    this.modelUpdaterPubsubTopic = modelUpdaterPubsubTopic;
    this.lockRegistry = lockRegistry;
    this.localComputeTimeoutMinutes = localComputeTimeoutMinutes;
    this.uploadTimeoutMinutes = uploadTimeoutMinutes;
    this.batchSize = collectorBatchSize;
    this.aggregationBatchFailureThreshold = aggregationBatchFailureThreshold;
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

  public void processAggregatorNotifications(AggregatorNotification.Attributes notification) {
    if (notification.getStatus() != AggregatorNotification.Status.OK) {
      try {
        // Parse requestId into iterationId and batchId
        String requestId = notification.getRequestId();
        int i = requestId.lastIndexOf('_');
        String[] requestIdArr = {requestId.substring(0, i), requestId.substring(i + 1)};

        // The last element is the batchId and the first element is the iterationId
        String batchId = requestIdArr[1];
        IterationId iterationId = IterationId.fromString(requestIdArr[0]);
        MDC.put(Constants.ITERATION_ID, iterationId.toString());

        // Update batch status to FAILED
        AggregationBatchId aggregationBatchId =
            AggregationBatchId.builder()
                .batchId(batchId)
                .populationName(iterationId.getPopulationName())
                .taskId(iterationId.getTaskId())
                .iterationId(iterationId.getIterationId())
                .attemptId(iterationId.getAttemptId())
                .build();
        AggregationBatchEntity aggregationBatch =
            aggregationBatchDao.getAggregationBatchById(aggregationBatchId).orElse(null);
        if (aggregationBatch == null
            || (aggregationBatch.getStatus() != AggregationBatchEntity.Status.PUBLISH_COMPLETED
                && aggregationBatch.getStatus() != AggregationBatchEntity.Status.FAILED)) {
          // Log invalid batch id and ack the message.
          logger.warn("Invalid aggregation batch {} was provided.", batchId);
          return;
        }
        if (aggregationBatch.getStatus() == AggregationBatchEntity.Status.PUBLISH_COMPLETED) {
          if (!aggregationBatchDao.updateAggregationBatchStatus(
              aggregationBatch,
              aggregationBatch.toBuilder().status(AggregationBatchEntity.Status.FAILED).build())) {
            logger.error(
                "Failed to update batch {} from {} to {}",
                batchId,
                AggregationBatchEntity.Status.PUBLISH_COMPLETED,
                AggregationBatchEntity.Status.FAILED);
            // Throw exception to nack the message and retry
            throw new IllegalStateException("Failed to update batch.");
          }
        }

        // Update assignments for batch to FAILED
        List<String> batchAssignmentIds =
            assignmentDao.queryAssignmentIdsOfStatus(
                iterationId, AssignmentEntity.Status.UPLOAD_COMPLETED, Optional.of(batchId));
        if (batchAssignmentIds.size() > 0) {
          int updates =
              assignmentDao.batchUpdateAssignmentStatus(
                  convertAssignmentIds(batchAssignmentIds, iterationId),
                  Optional.of(batchId),
                  AssignmentEntity.Status.UPLOAD_COMPLETED,
                  AssignmentEntity.Status.REMOTE_FAILED);
          if (updates != batchAssignmentIds.size()) {
            logger.error(
                "Failed to update assignments of batch {} from {} to {}",
                batchId,
                AssignmentEntity.Status.UPLOAD_COMPLETED,
                AssignmentEntity.Status.REMOTE_FAILED);
            // Throw exception to nack the message and retry
            throw new IllegalStateException("Failed to update assignments.");
          }
        }

        // Obtain lock before updating the iteration
        String partition = iterationId.toString();
        Lock lock = lockRegistry.obtain(LOCK_PREFIX + partition);
        if (lock.tryLock(30, TimeUnit.SECONDS)) {
          try {
            // If iteration failure count is above threshold, fail the iteration.
            IterationEntity iteration = taskDao.getIterationById(iterationId).get();
            if (aggregationBatchFailureThreshold.isPresent()) {
              long totalFailed =
                  aggregationBatchDao.querySumOfAggregationBatchesOfStatus(
                      iteration,
                      /* AggregationLevel */ 0,
                      List.of(AggregationBatchEntity.Status.FAILED));
              if (totalFailed > aggregationBatchFailureThreshold.get() * batchSize) {
                if (!taskDao.updateIterationStatus(
                    iteration, iteration.toBuilder().status(Status.AGGREGATING_FAILED).build())) {
                  logger.warn(
                      "Failed to update iteration {} from {} to {}",
                      iteration.getId().toString(),
                      iteration.getStatus(),
                      Status.AGGREGATING_FAILED);
                  // Throw exception to nack the message and retry
                  throw new IllegalStateException("Failed to update iteration.");
                }
                return;
              }
            }

            // If iteration is AGGREGATING and total batches sent/completed < reportGoal update
            // status.
            if (iteration.getStatus() == Status.AGGREGATING) {
              if (aggregationBatchDao.querySumOfAggregationBatchesOfStatus(
                      iteration,
                      iteration.getAggregationLevel() - 1,
                      List.of(
                          AggregationBatchEntity.Status.PUBLISH_COMPLETED,
                          AggregationBatchEntity.Status.UPLOAD_COMPLETED))
                  < iteration.getReportGoal()) {
                if (!taskDao.updateIterationStatus(
                    iteration,
                    iteration.toBuilder().status(Status.COLLECTING).aggregationLevel(0).build())) {
                  logger.warn(
                      "Failed to update iteration {} from {} to {}",
                      iteration.getId().toString(),
                      iteration.getStatus(),
                      Status.COLLECTING);
                  // Throw exception to nack the message and retry
                  throw new IllegalStateException("Failed to update iteration.");
                }
              }
            }
          } finally {
            lock.unlock();
          }
        } else {
          logger.error("Failed to obtain lock during processAggregatorNotifications");
          throw new IllegalStateException(
              "Failed to obtain lock during processAggregatorNotifications");
        }
      } catch (Exception e) {
        logger.error("Failed to process message", e);
        throw new IllegalStateException(e);
      } finally {
        MDC.clear();
      }
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
          // Retrieve the latest status after locking.
          iteration = taskDao.getIterationById(iteration.getId()).get();
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
            List.of(AggregationBatchEntity.Status.UPLOAD_COMPLETED));

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
        logger.warn(
            "Failed to update iteration {} from {} to {}",
            iteration.getId().toString(),
            iteration.getStatus(),
            Status.APPLYING);
      }
    }
  }

  private void processCollectingIterationImp(IterationEntity iteration, String partition) {

    // check uploaded results and mark corresponding assignment upload_completed.
    queryAndSetUploadCompleted(iteration, partition);

    // Batch assignments marked UPLOAD_COMPLETED without an assigned batch if threshold is met.
    List<String> leftoverAssignments = queryAndBatchLeftoverAssignments(iteration, partition);

    // Send any full batches ready to be published
    queryAndSendFullBatches(iteration, partition);

    // count the contribution and trigger next step.
    countContributionsAndTriggerAggregation(iteration, leftoverAssignments, partition);
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
    // Get all local_completed and mark them upload_completed if
    // their upload is detected.
    Set<String> allLocalCompleted =
        assignmentDao
            .queryAssignmentIdsOfStatus(
                iteration.getId(), AssignmentEntity.Status.LOCAL_COMPLETED, Optional.empty())
            .stream()
            .collect(Collectors.toSet());
    Set<String> allUploadedAssignmentIds = new HashSet<>();

    // If no assignments in status LOCAL_COMPLETED don't bother checking for uploaded gradients.
    if (allLocalCompleted.size() != 0) {
      allUploadedAssignmentIds =
          Arrays.stream(blobManager.generateDownloadGradientDescriptions(iteration))
              .map((folder) -> blobDao.listByPartition(folder, UUID_PREFIX_LIST))
              .flatMap(Collection::stream)
              .map(CollectorCoreImpl::trimSlash)
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
    }

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
                    convertAssignmentIds(assignmentIds, iteration.getId()),
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
            iteration, assignments, Optional.of(batchId), false, aggregatorNotificationEndpoint),
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
    // Count all publish_completed and upload_completed.
    long publishedAssignments =
        aggregationBatchDao.querySumOfAggregationBatchesOfStatus(
            iteration,
            iteration.getAggregationLevel(),
            List.of(
                AggregationBatchEntity.Status.PUBLISH_COMPLETED,
                AggregationBatchEntity.Status.UPLOAD_COMPLETED));

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
        logger.warn(
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
              convertAssignmentIds(leftoverAssignments, iteration.getId()),
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
      List<AssignmentId> assignmentIds =
          convertAssignmentIds(assignmentIdStrings, iteration.getId());
      int updates =
          assignmentDao.batchUpdateAssignmentStatus(assignmentIds, Optional.empty(), from, to);
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
      Collection<String> assignmentIds, IterationId iteration) {
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
