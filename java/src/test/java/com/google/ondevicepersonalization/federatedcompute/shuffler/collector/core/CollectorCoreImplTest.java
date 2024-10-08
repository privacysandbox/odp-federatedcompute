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

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.ondevicepersonalization.federatedcompute.shuffler.aggregator.core.message.AggregatorMessage;
import com.google.ondevicepersonalization.federatedcompute.shuffler.aggregator.core.message.AggregatorNotification;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AggregationBatchDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AggregationBatchEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AssignmentDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AssignmentEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AssignmentId;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobDescription;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobManager;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.IterationEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.IterationEntity.Status;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.messaging.MessageSender;
import com.google.ondevicepersonalization.federatedcompute.shuffler.modelupdater.core.message.ModelUpdaterMessage;
import java.time.Instant;
import java.time.InstantSource;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.integration.support.locks.LockRegistry;

@RunWith(JUnit4.class)
public final class CollectorCoreImplTest {
  private static Instant NOW = Instant.parse("2023-09-01T00:00:00Z");
  private static InstantSource instantSource = InstantSource.fixed(NOW);
  private static Instant LOCAL_COMPUTE_TIMEOUT_BEORE_THIS_TIME = NOW.minusSeconds(60 * 15);
  private static Instant UPLOAD_TIMEOUT_BEFORE_THIS_TIME = NOW.minusSeconds(60 * 15);
  @Mock TaskDao taskDao;
  @Mock BlobDao blobDao;
  @Mock BlobManager blobManager;
  @Mock AggregationBatchDao aggregationBatchDao;
  @Mock AssignmentDao assignmentDao;
  @Mock CollectorCoreImplHelper collectorCoreImplHelper;

  @Mock MessageSender messageSender;
  @Mock LockRegistry lockRegistry;

  @Mock Lock lock;

  private CollectorCoreImpl core;

  private static final IterationEntity ITERATION1 =
      IterationEntity.builder()
          .populationName("us")
          .taskId(35)
          .iterationId(17)
          .attemptId(0)
          .status(IterationEntity.Status.COLLECTING)
          .baseIterationId(16)
          .baseOnResultId(16)
          .reportGoal(3)
          .resultId(17)
          .build();

  private static final IterationEntity ITERATION2 =
      IterationEntity.builder()
          .populationName("us")
          .taskId(36)
          .iterationId(18)
          .attemptId(0)
          .status(IterationEntity.Status.COLLECTING)
          .baseIterationId(17)
          .baseOnResultId(17)
          .reportGoal(3)
          .resultId(18)
          .build();

  private static final AggregatorNotification.Attributes ATTRIBUTES1 =
      AggregatorNotification.Attributes.builder()
          .requestId("us/35/17/0_batch")
          .status(AggregatorNotification.Status.ERROR)
          .errorReason(AggregatorNotification.ErrorReason.AGGREGATION_ERROR)
          .build();

  private static final AggregationBatchEntity BATCH_ENTITY1 =
      AggregationBatchEntity.builder()
          .populationName("us")
          .taskId(35)
          .iterationId(17)
          .attemptId(0)
          .status(AggregationBatchEntity.Status.PUBLISH_COMPLETED)
          .batchId("batch")
          .aggregationLevel(0)
          .batchSize(50)
          .createdByPartition("us/36/18/0_batch")
          .build();

  private static final IterationEntity AGG_ITERATION1 =
      IterationEntity.builder()
          .populationName("us")
          .taskId(35)
          .iterationId(17)
          .attemptId(0)
          .status(Status.AGGREGATING)
          .baseIterationId(16)
          .baseOnResultId(16)
          .reportGoal(3)
          .resultId(17)
          .aggregationLevel(1)
          .build();

  private static final IterationEntity AGG_ITERATION2 =
      IterationEntity.builder()
          .populationName("us")
          .taskId(36)
          .iterationId(17)
          .attemptId(0)
          .status(Status.AGGREGATING)
          .baseIterationId(16)
          .baseOnResultId(16)
          .reportGoal(3)
          .resultId(17)
          .aggregationLevel(2)
          .build();

  private static final BlobDescription DIR1_1 =
      BlobDescription.builder().host("test-1-1").resourceObject("us/35/17/d/").build();
  private static final BlobDescription DIR1_2 =
      BlobDescription.builder().host("test-1-2").resourceObject("us/35/17/d/").build();

  private static final BlobDescription DIR2_1 =
      BlobDescription.builder().host("test-2-1").resourceObject("us/36/18/d/").build();
  private static final BlobDescription DIR2_2 =
      BlobDescription.builder().host("test-2-2").resourceObject("us/36/18/d/").build();
  private static final BlobDescription PLAN_1 =
      BlobDescription.builder()
          .host("test-1-1")
          .resourceObject("us/35/17/s/0/server_plan")
          .url("gs://test-1-1/us/35/17/s/0/server_plan")
          .build();
  private static final BlobDescription PLAN_2 =
      BlobDescription.builder()
          .host("test-1-1")
          .resourceObject("us/36/18/s/0/server_plan")
          .url("gs://test-1-1/us/36/18/s/0/server_plan")
          .build();

  private static final BlobDescription GRADIENT1 =
      BlobDescription.builder().host("test-g-1").resourceObject("us/35/17/s/0/gradient").build();

  private static final BlobDescription CURRENT_MODEL1 =
      BlobDescription.builder().host("test-m-1").resourceObject("us/35/16/s/0/checkpoint").build();
  private static final BlobDescription CURRENT_MODEL2 =
      BlobDescription.builder().host("test-m-1").resourceObject("us/36/17/s/0/checkpoint").build();
  // publish model to
  private static final BlobDescription NEW_MODEL1_1 =
      BlobDescription.builder().host("test-m-1").resourceObject("us/35/17/s/0/checkpoint").build();
  private static final BlobDescription NEW_MODEL2_1 =
      BlobDescription.builder().host("test-m-1").resourceObject("us/36/18/s/0/checkpoint").build();
  private static final BlobDescription NEW_METRICS1_1 =
      BlobDescription.builder().host("test-m-1").resourceObject("us/35/17/s/0/metrics").build();
  private static final BlobDescription NEW_METRICS2_1 =
      BlobDescription.builder().host("test-m-1").resourceObject("us/36/18/s/0/metrics").build();

  private static final BlobDescription NEW_CLIENT_CHECKPOINT1_1 =
      BlobDescription.builder()
          .host("test-m-1")
          .resourceObject("us/35/17/d/0/client_checkpoint")
          .build();
  private static final BlobDescription NEW_CLIENT_CHECKPOINT2_1 =
      BlobDescription.builder()
          .host("test-m-1")
          .resourceObject("us/36/18/d/0/client_checkpoint")
          .build();

  private static final BlobDescription RESULT_1 =
      BlobDescription.builder()
          .host("test-1-1")
          .resourceObject("us/35/17/s/0/checkpoint")
          .url("gs://test-1-1/us/35/17/s/0/checkpoint")
          .build();
  private static final BlobDescription RESULT_2 =
      BlobDescription.builder()
          .host("test-1-1")
          .resourceObject("us/36/18/s/0/checkpoint")
          .url("gs://test-1-1/us/36/18/s/0/checkpoint")
          .build();

  private static final ModelUpdaterMessage MODEL_UPDATER_MESSAGE =
      ModelUpdaterMessage.builder()
          .serverPlanBucket(PLAN_1.getHost())
          .serverPlanObject(PLAN_1.getResourceObject())
          .intermediateGradientBucket(GRADIENT1.getHost())
          .intermediateGradientPrefix(GRADIENT1.getResourceObject())
          .intermediateGradients(List.of("iter_1/gradient", "iter_2/gradient"))
          .checkpointBucket(CURRENT_MODEL1.getHost())
          .checkpointObject(CURRENT_MODEL1.getResourceObject())
          .newCheckpointOutputBucket(NEW_MODEL1_1.getHost())
          .newCheckpointOutputObject(NEW_MODEL1_1.getResourceObject())
          .newClientCheckpointOutputBucket(NEW_CLIENT_CHECKPOINT1_1.getHost())
          .newClientCheckpointOutputObject(NEW_CLIENT_CHECKPOINT1_1.getResourceObject())
          .metricsOutputBucket(NEW_METRICS1_1.getHost())
          .metricsOutputObject(NEW_METRICS1_1.getResourceObject())
          .requestId(ITERATION1.getId().toString())
          .build();

  private static final AggregatorMessage AGGREGATOR_MESSAGE =
      AggregatorMessage.builder()
          .serverPlanBucket(PLAN_1.getHost())
          .serverPlanObject(PLAN_1.getResourceObject())
          .aggregatedGradientOutputBucket(RESULT_1.getHost())
          .aggregatedGradientOutputObject(RESULT_1.getResourceObject())
          .gradientBucket(GRADIENT1.getHost())
          .gradientPrefix(GRADIENT1.getResourceObject())
          .gradients(List.of("iter_1/gradient", "iter_2/gradient"))
          .requestId(ITERATION1.getId().toString())
          .build();

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    core =
        new CollectorCoreImpl(
            taskDao,
            assignmentDao,
            aggregationBatchDao,
            blobDao,
            blobManager,
            collectorCoreImplHelper,
            instantSource,
            messageSender,
            "agTopic",
            "endpoint",
            "muTopic",
            lockRegistry,
            15,
            15,
            50,
            Optional.of(10L));

    when(blobManager.generateDownloadServerPlanDescription(ITERATION1)).thenReturn(PLAN_1);
    when(blobManager.generateDownloadServerPlanDescription(ITERATION2)).thenReturn(PLAN_2);
    when(blobManager.generateDownloadCheckpointDescription(ITERATION1)).thenReturn(CURRENT_MODEL1);
    when(blobManager.generateDownloadCheckpointDescription(ITERATION2)).thenReturn(CURRENT_MODEL2);
    when(blobManager.generateUploadCheckpointDescriptions(ITERATION1))
        .thenReturn(new BlobDescription[] {NEW_MODEL1_1});
    when(blobManager.generateUploadMetricsDescriptions(ITERATION1))
        .thenReturn(new BlobDescription[] {NEW_METRICS1_1});
    when(blobManager.generateUploadClientCheckpointDescriptions(ITERATION1))
        .thenReturn(new BlobDescription[] {NEW_CLIENT_CHECKPOINT1_1});
    when(blobManager.generateUploadCheckpointDescriptions(ITERATION2))
        .thenReturn(
            new BlobDescription[] {
              NEW_MODEL2_1,
            });
    when(blobManager.generateUploadMetricsDescriptions(ITERATION2))
        .thenReturn(
            new BlobDescription[] {
              NEW_METRICS2_1,
            });
    when(blobManager.generateUploadClientCheckpointDescriptions(ITERATION2))
        .thenReturn(new BlobDescription[] {NEW_CLIENT_CHECKPOINT2_1});
    when(blobManager.generateDownloadServerPlanDescription(ITERATION1)).thenReturn(PLAN_1);
    when(blobManager.generateDownloadServerPlanDescription(ITERATION2)).thenReturn(PLAN_2);
    when(blobManager.generateUploadAggregatedGradientDescription(ITERATION1)).thenReturn(RESULT_1);
    when(blobManager.generateUploadAggregatedGradientDescription(ITERATION2)).thenReturn(RESULT_2);
    when(blobManager.generateDownloadAggregatedGradientDescription(ITERATION1))
        .thenReturn(RESULT_1);
    when(blobManager.generateDownloadAggregatedGradientDescription(ITERATION2))
        .thenReturn(RESULT_2);
    when(taskDao.getIterationById(ITERATION1.getId())).thenReturn(Optional.of(ITERATION1));
    when(taskDao.getIterationById(ITERATION2.getId())).thenReturn(Optional.of(ITERATION2));
    when(lockRegistry.obtain("collector_" + AGG_ITERATION1.getId().toString())).thenReturn(lock);
    when(lockRegistry.obtain("collector_" + ITERATION1.getId().toString())).thenReturn(lock);
    when(lock.tryLock(anyLong(), any())).thenReturn(true);
  }

  @Test
  public void testProcess_TryLockFailed() {
    when(lockRegistry.obtain("collector_" + AGG_ITERATION2.getId().toString())).thenReturn(lock);
    when(lockRegistry.obtain("collector_" + ITERATION1.getId().toString())).thenReturn(lock);
    when(lock.tryLock()).thenReturn(false);
    when(taskDao.getIterationsOfStatus(Status.AGGREGATING))
        .thenReturn(ImmutableList.of(AGG_ITERATION2));
    when(taskDao.getIterationsOfStatus(Status.COLLECTING)).thenReturn(ImmutableList.of(ITERATION1));

    core.processAggregating();
    core.processCollecting();

    verify(lockRegistry, times(1)).obtain("collector_" + AGG_ITERATION2.getId().toString());
    verify(lockRegistry, times(1)).obtain("collector_" + ITERATION1.getId().toString());
    verify(messageSender, times(0)).sendMessage(any(), any());
    verify(taskDao, times(0)).updateIterationStatus(any(), any());
  }

  @Test
  public void testProcessAg_OneIterationL1() {
    when(taskDao.getIterationById(AGG_ITERATION1.getId())).thenReturn(Optional.of(AGG_ITERATION1));
    when(lockRegistry.obtain("collector_" + AGG_ITERATION1.getId().toString())).thenReturn(lock);
    when(lock.tryLock()).thenReturn(true);
    when(blobManager.generateDownloadAggregatedGradientDescription(AGG_ITERATION1))
        .thenReturn(RESULT_1);
    when(blobDao.listByPartition(eq(RESULT_1), any()))
        .thenReturn(ImmutableList.of("batch1", "batch2"));
    when(taskDao.getIterationsOfStatus(any())).thenReturn(ImmutableList.of(AGG_ITERATION1));
    when(aggregationBatchDao.queryAggregationBatchIdsOfStatus(any(), anyLong(), any(), any()))
        .thenReturn(List.of("batch1", "batch2"));
    when(collectorCoreImplHelper.createModelUpdaterMessage(any(), any()))
        .thenReturn(MODEL_UPDATER_MESSAGE);
    when(aggregationBatchDao.querySumOfAggregationBatchesOfStatus(
            any(), anyLong(), eq(List.of(AggregationBatchEntity.Status.UPLOAD_COMPLETED))))
        .thenReturn(3L);

    core.processAggregating();
    verify(lockRegistry, times(1)).obtain("collector_" + AGG_ITERATION1.getId().toString());
    verify(blobDao, times(1)).listByPartition(eq(RESULT_1), any());
    verify(aggregationBatchDao, times(1))
        .queryAggregationBatchIdsOfStatus(
            AGG_ITERATION1, 0, AggregationBatchEntity.Status.PUBLISH_COMPLETED, Optional.empty());
    verify(aggregationBatchDao, times(1))
        .queryAggregationBatchIdsOfStatus(
            AGG_ITERATION1, 0, AggregationBatchEntity.Status.UPLOAD_COMPLETED, Optional.empty());
    verify(aggregationBatchDao, times(2)).updateAggregationBatchStatus(any(), any());
    verify(aggregationBatchDao, times(1))
        .querySumOfAggregationBatchesOfStatus(
            AGG_ITERATION1, 0, List.of(AggregationBatchEntity.Status.UPLOAD_COMPLETED));
    verify(collectorCoreImplHelper, times(1))
        .createModelUpdaterMessage(eq(AGG_ITERATION1), eq(List.of("batch1", "batch2")));
    verify(messageSender, times(1)).sendMessage(eq(MODEL_UPDATER_MESSAGE), eq("muTopic"));
    verify(taskDao, times(1))
        .updateIterationStatus(
            AGG_ITERATION1,
            AGG_ITERATION1.toBuilder().status(Status.APPLYING).aggregationLevel(2).build());
  }

  @Test
  public void testProcess_OneIteration() {
    // arange
    when(lockRegistry.obtain("collector_" + ITERATION1.getId().toString())).thenReturn(lock);
    when(lockRegistry.obtain("collector_" + ITERATION2.getId().toString())).thenReturn(lock);
    when(lock.tryLock()).thenReturn(true);
    when(taskDao.getIterationsOfStatus(any())).thenReturn(ImmutableList.of(ITERATION1, ITERATION2));
    when(blobManager.generateDownloadGradientDescriptions(ITERATION1))
        .thenReturn(new BlobDescription[] {DIR1_1, DIR1_2});
    when(blobManager.generateDownloadGradientDescriptions(ITERATION2))
        .thenReturn(new BlobDescription[] {DIR2_1, DIR2_2});
    when(blobDao.listByPartition(eq(DIR1_1), any()))
        .thenReturn(ImmutableList.of("iter1_1", "iter1_2"));
    when(blobDao.listByPartition(eq(DIR1_2), any())).thenReturn(ImmutableList.of());
    when(blobDao.listByPartition(eq(DIR2_1), any()))
        .thenReturn(ImmutableList.of("iter2_1", "iter2_2"));
    when(blobDao.listByPartition(eq(DIR2_2), any())).thenReturn(ImmutableList.of("iter2_3"));
    when(assignmentDao.queryAssignmentIdsOfStatus(
            ITERATION1.getId(), AssignmentEntity.Status.LOCAL_COMPLETED, Optional.empty()))
        .thenReturn(ImmutableList.of("iter1_1", "iter1_2"));
    when(assignmentDao.queryAssignmentIdsOfStatus(
            ITERATION2.getId(), AssignmentEntity.Status.LOCAL_COMPLETED, Optional.empty()))
        .thenReturn(ImmutableList.of("iter2_1", "iter2_2", "iter2_3"));
    when(assignmentDao.queryAssignmentIdsOfStatus(
            ITERATION1.getId(), AssignmentEntity.Status.UPLOAD_COMPLETED, Optional.empty()))
        .thenReturn(ImmutableList.of("iter1_1", "iter1_2"));
    when(assignmentDao.queryAssignmentIdsOfStatus(
            ITERATION2.getId(), AssignmentEntity.Status.UPLOAD_COMPLETED, Optional.empty()))
        .thenReturn(ImmutableList.of("iter2_1", "iter2_2", "iter2_3"));
    when(assignmentDao.createBatchAndUpdateAssignments(any(), any(), any(), any(), any(), any()))
        .thenReturn(true);
    when(aggregationBatchDao.queryAggregationBatchIdsOfStatus(any(), anyLong(), any(), any()))
        .thenReturn(List.of());
    when(aggregationBatchDao.querySumOfAggregationBatchesOfStatus(
            ITERATION1, 0, List.of(AggregationBatchEntity.Status.PUBLISH_COMPLETED)))
        .thenReturn(0L);
    when(aggregationBatchDao.querySumOfAggregationBatchesOfStatus(
            ITERATION2, 0, List.of(AggregationBatchEntity.Status.PUBLISH_COMPLETED)))
        .thenReturn(0L);
    when(aggregationBatchDao.updateAggregationBatchStatus(any(), any())).thenReturn(true);
    when(assignmentDao.queryAssignmentIdsOfStatus(
            eq(ITERATION2.getId()),
            eq(AssignmentEntity.Status.UPLOAD_COMPLETED),
            any(Optional.class)))
        .thenReturn(ImmutableList.of("iter2_1", "iter2_2", "iter2_3"));
    when(collectorCoreImplHelper.createAggregatorMessage(any(), any(), any(), anyBoolean(), any()))
        .thenReturn(AGGREGATOR_MESSAGE);

    // act
    core.processCollecting();

    // assert
    verify(lockRegistry, times(1)).obtain("collector_" + ITERATION1.getId().toString());
    verify(lockRegistry, times(1)).obtain("collector_" + ITERATION2.getId().toString());
    verify(taskDao, times(1)).getIterationsOfStatus(IterationEntity.Status.COLLECTING);
    verify(blobManager, times(1)).generateDownloadGradientDescriptions(ITERATION1);
    verify(blobManager, times(1)).generateDownloadGradientDescriptions(ITERATION2);
    verify(blobDao, times(1)).listByPartition(eq(DIR1_1), any());
    verify(blobDao, times(1)).listByPartition(eq(DIR1_2), any());
    verify(blobDao, times(1)).listByPartition(eq(DIR2_1), any());
    verify(blobDao, times(1)).listByPartition(eq(DIR2_2), any());
    verify(messageSender, times(1)).sendMessage(any(), any());
    verify(messageSender, times(1)).sendMessage(eq(AGGREGATOR_MESSAGE), eq("agTopic"));
    verify(taskDao, times(1))
        .updateIterationStatus(
            ITERATION2,
            ITERATION2.toBuilder().status(Status.AGGREGATING).aggregationLevel(1).build());
    verify(taskDao, times(0))
        .updateIterationStatus(
            ITERATION1,
            ITERATION1.toBuilder().status(Status.AGGREGATING).aggregationLevel(1).build());
    verify(assignmentDao, times(0))
        .createBatchAndUpdateAssignments(
            eq(
                List.of(
                    toAssignmentId(ITERATION1, "iter1_1"), toAssignmentId(ITERATION1, "iter1_2"))),
            eq(ITERATION1),
            eq(AssignmentEntity.Status.UPLOAD_COMPLETED),
            eq(AssignmentEntity.Status.UPLOAD_COMPLETED),
            any(),
            eq(ITERATION1.getId().toString()));
    verify(assignmentDao, times(1))
        .createBatchAndUpdateAssignments(
            eq(
                List.of(
                    toAssignmentId(ITERATION2, "iter2_1"),
                    toAssignmentId(ITERATION2, "iter2_2"),
                    toAssignmentId(ITERATION2, "iter2_3"))),
            eq(ITERATION2),
            eq(AssignmentEntity.Status.UPLOAD_COMPLETED),
            eq(AssignmentEntity.Status.UPLOAD_COMPLETED),
            any(),
            eq(ITERATION2.getId().toString()));

    verify(aggregationBatchDao, times(1))
        .queryAggregationBatchIdsOfStatus(
            ITERATION1,
            0,
            AggregationBatchEntity.Status.FULL,
            Optional.of(ITERATION1.getId().toString()));
    verify(aggregationBatchDao, times(1))
        .queryAggregationBatchIdsOfStatus(
            ITERATION2,
            0,
            AggregationBatchEntity.Status.FULL,
            Optional.of(ITERATION2.getId().toString()));
    verify(aggregationBatchDao, times(1))
        .querySumOfAggregationBatchesOfStatus(
            ITERATION1, 0, List.of(AggregationBatchEntity.Status.PUBLISH_COMPLETED));
    verify(aggregationBatchDao, times(1))
        .querySumOfAggregationBatchesOfStatus(
            ITERATION2, 0, List.of(AggregationBatchEntity.Status.PUBLISH_COMPLETED));
    verify(aggregationBatchDao, times(1)).updateAggregationBatchStatus(any(), any());
    verify(collectorCoreImplHelper, times(1))
        .createAggregatorMessage(
            eq(ITERATION2),
            eq(List.of("iter2_1", "iter2_2", "iter2_3")),
            any(),
            eq(false),
            eq("endpoint"));
    verify(assignmentDao, times(1))
        .queryAssignmentIdsOfStatus(
            eq(ITERATION1.getId()),
            eq(AssignmentEntity.Status.UPLOAD_COMPLETED),
            any(Optional.class));
  }

  @Test
  public void testProcessTimeouts_TimeoutLocalCompute() {
    // arange
    when(lockRegistry.obtain("timeout_collector_" + ITERATION1.getId().toString()))
        .thenReturn(lock);
    when(lockRegistry.obtain("timeout_collector_" + ITERATION2.getId().toString()))
        .thenReturn(lock);
    when(lock.tryLock()).thenReturn(true);
    when(taskDao.getIterationsOfStatus(any())).thenReturn(ImmutableList.of(ITERATION1, ITERATION2));
    when(blobManager.generateDownloadGradientDescriptions(ITERATION1))
        .thenReturn(new BlobDescription[] {DIR1_1});
    when(blobManager.generateDownloadGradientDescriptions(ITERATION2))
        .thenReturn(new BlobDescription[] {DIR2_1});
    when(assignmentDao.queryAssignmentIdsOfStatus(
            eq(ITERATION1.getId()), eq(AssignmentEntity.Status.ASSIGNED), any(Instant.class)))
        .thenReturn(ImmutableList.of("iter1_1", "iter1_2"));
    when(assignmentDao.queryAssignmentIdsOfStatus(
            eq(ITERATION2.getId()), eq(AssignmentEntity.Status.ASSIGNED), any(Instant.class)))
        .thenReturn(ImmutableList.of("iter2_1"));

    // act
    core.processTimeouts();

    // assert
    verify(assignmentDao, times(1))
        .queryAssignmentIdsOfStatus(
            eq(ITERATION1.getId()), eq(AssignmentEntity.Status.ASSIGNED), any(Instant.class));
    verify(assignmentDao, times(1))
        .queryAssignmentIdsOfStatus(
            eq(ITERATION2.getId()), eq(AssignmentEntity.Status.ASSIGNED), any(Instant.class));
    verify(assignmentDao, times(1))
        .batchUpdateAssignmentStatus(
            List.of(toAssignmentId(ITERATION1, "iter1_1"), toAssignmentId(ITERATION1, "iter1_2")),
            Optional.empty(),
            AssignmentEntity.Status.ASSIGNED,
            AssignmentEntity.Status.LOCAL_TIMEOUT);
    verify(assignmentDao, times(1))
        .batchUpdateAssignmentStatus(
            List.of(toAssignmentId(ITERATION2, "iter2_1")),
            Optional.empty(),
            AssignmentEntity.Status.ASSIGNED,
            AssignmentEntity.Status.LOCAL_TIMEOUT);
  }

  @Test
  public void testProcessTimeouts_TimeoutResultUpload() {
    // arange
    when(lockRegistry.obtain("timeout_collector_" + ITERATION1.getId().toString()))
        .thenReturn(lock);
    when(lockRegistry.obtain("timeout_collector_" + ITERATION2.getId().toString()))
        .thenReturn(lock);
    when(lock.tryLock()).thenReturn(true);
    when(taskDao.getIterationsOfStatus(any())).thenReturn(ImmutableList.of(ITERATION1, ITERATION2));
    when(blobManager.generateDownloadGradientDescriptions(ITERATION1))
        .thenReturn(new BlobDescription[] {DIR1_1});
    when(blobManager.generateDownloadGradientDescriptions(ITERATION2))
        .thenReturn(new BlobDescription[] {DIR2_1});
    when(assignmentDao.queryAssignmentIdsOfStatus(
            eq(ITERATION1.getId()),
            eq(AssignmentEntity.Status.LOCAL_COMPLETED),
            eq(LOCAL_COMPUTE_TIMEOUT_BEORE_THIS_TIME)))
        .thenReturn(ImmutableList.of("iter1_1", "iter1_2"));
    when(assignmentDao.queryAssignmentIdsOfStatus(
            eq(ITERATION2.getId()),
            eq(AssignmentEntity.Status.LOCAL_COMPLETED),
            eq(LOCAL_COMPUTE_TIMEOUT_BEORE_THIS_TIME)))
        .thenReturn(ImmutableList.of("iter2_1"));

    // act
    core.processTimeouts();

    // assert
    verify(assignmentDao, times(1))
        .queryAssignmentIdsOfStatus(
            eq(ITERATION1.getId()),
            eq(AssignmentEntity.Status.LOCAL_COMPLETED),
            eq(LOCAL_COMPUTE_TIMEOUT_BEORE_THIS_TIME));
    verify(assignmentDao, times(1))
        .queryAssignmentIdsOfStatus(
            eq(ITERATION2.getId()),
            eq(AssignmentEntity.Status.LOCAL_COMPLETED),
            eq(LOCAL_COMPUTE_TIMEOUT_BEORE_THIS_TIME));
    verify(assignmentDao, times(1))
        .batchUpdateAssignmentStatus(
            List.of(toAssignmentId(ITERATION1, "iter1_1"), toAssignmentId(ITERATION1, "iter1_2")),
            Optional.empty(),
            AssignmentEntity.Status.LOCAL_COMPLETED,
            AssignmentEntity.Status.UPLOAD_TIMEOUT);
    verify(assignmentDao, times(1))
        .batchUpdateAssignmentStatus(
            List.of(toAssignmentId(ITERATION2, "iter2_1")),
            Optional.empty(),
            AssignmentEntity.Status.LOCAL_COMPLETED,
            AssignmentEntity.Status.UPLOAD_TIMEOUT);
  }

  @Test
  public void testProcess_SetResultUploadCompletedWithBatches() {
    // arange
    CollectorCoreImpl impl =
        new CollectorCoreImpl(
            taskDao,
            assignmentDao,
            aggregationBatchDao,
            blobDao,
            blobManager,
            collectorCoreImplHelper,
            instantSource,
            messageSender,
            "agTopic",
            "endpoint",
            "muTopic",
            lockRegistry,
            15,
            15,
            2,
            Optional.empty());
    when(lockRegistry.obtain("collector_" + ITERATION1.getId().toString())).thenReturn(lock);
    when(lock.tryLock()).thenReturn(true);
    when(taskDao.getIterationsOfStatus(any())).thenReturn(ImmutableList.of(ITERATION1));
    when(blobManager.generateDownloadGradientDescriptions(ITERATION1))
        .thenReturn(new BlobDescription[] {DIR1_1});
    when(blobDao.listByPartition(eq(DIR1_1), any()))
        .thenReturn(ImmutableList.of("iter1_1/", "iter1_2/", "iter1_3/"));
    when(assignmentDao.queryAssignmentIdsOfStatus(
            eq(ITERATION1.getId()),
            eq(AssignmentEntity.Status.LOCAL_COMPLETED),
            eq(Optional.empty())))
        .thenReturn(ImmutableList.of("iter1_1", "iter1_2", "iter1_3"));
    when(assignmentDao.queryAssignmentIdsOfStatus(
            ITERATION1.getId(), AssignmentEntity.Status.UPLOAD_COMPLETED, Optional.empty()))
        .thenReturn(ImmutableList.of("iter1_3"));
    when(assignmentDao.createBatchAndUpdateAssignments(any(), any(), any(), any(), any(), any()))
        .thenReturn(true);
    when(aggregationBatchDao.queryAggregationBatchIdsOfStatus(
            any(), anyLong(), eq(AggregationBatchEntity.Status.FULL), any()))
        .thenReturn(List.of("batch-1"));
    when(aggregationBatchDao.queryAggregationBatchIdsOfStatus(
            any(), anyLong(), eq(AggregationBatchEntity.Status.PUBLISH_COMPLETED), any()))
        .thenReturn(List.of("batch-1"));
    when(aggregationBatchDao.querySumOfAggregationBatchesOfStatus(
            ITERATION1, 0, List.of(AggregationBatchEntity.Status.PUBLISH_COMPLETED)))
        .thenReturn(2L);
    when(aggregationBatchDao.updateAggregationBatchStatus(any(), any())).thenReturn(true);
    when(assignmentDao.queryAssignmentIdsOfStatus(
            eq(ITERATION1.getId()),
            eq(AssignmentEntity.Status.UPLOAD_COMPLETED),
            any(Optional.class)))
        .thenReturn(ImmutableList.of("iter1_3"));
    when(assignmentDao.queryAssignmentIdsOfStatus(
            ITERATION1.getId(), AssignmentEntity.Status.UPLOAD_COMPLETED, Optional.of("batch-1")))
        .thenReturn(ImmutableList.of("iter1_1", "iter1_2"));
    when(collectorCoreImplHelper.createAggregatorMessage(any(), any(), any(), anyBoolean(), any()))
        .thenReturn(AGGREGATOR_MESSAGE);

    // act
    impl.processCollecting();

    // assert
    verify(assignmentDao, times(1))
        .queryAssignmentIdsOfStatus(
            eq(ITERATION1.getId()),
            eq(AssignmentEntity.Status.LOCAL_COMPLETED),
            eq(Optional.empty()));
    verify(assignmentDao, times(1))
        .batchUpdateAssignmentStatus(
            List.of(toAssignmentId(ITERATION1, "iter1_3")),
            Optional.empty(),
            AssignmentEntity.Status.LOCAL_COMPLETED,
            AssignmentEntity.Status.UPLOAD_COMPLETED);
    verify(assignmentDao, times(1))
        .createBatchAndUpdateAssignments(
            eq(
                List.of(
                    toAssignmentId(ITERATION1, "iter1_1"), toAssignmentId(ITERATION1, "iter1_2"))),
            eq(ITERATION1),
            eq(AssignmentEntity.Status.LOCAL_COMPLETED),
            eq(AssignmentEntity.Status.UPLOAD_COMPLETED),
            any(),
            eq(ITERATION1.getId().toString()));
    verify(assignmentDao, times(1))
        .createBatchAndUpdateAssignments(
            eq(List.of(toAssignmentId(ITERATION1, "iter1_3"))),
            eq(ITERATION1),
            eq(AssignmentEntity.Status.UPLOAD_COMPLETED),
            eq(AssignmentEntity.Status.UPLOAD_COMPLETED),
            any(),
            eq(ITERATION1.getId().toString()));
    verify(aggregationBatchDao, times(1))
        .queryAggregationBatchIdsOfStatus(
            ITERATION1,
            0,
            AggregationBatchEntity.Status.FULL,
            Optional.of(ITERATION1.getId().toString()));
    verify(aggregationBatchDao, times(1))
        .querySumOfAggregationBatchesOfStatus(
            ITERATION1, 0, List.of(AggregationBatchEntity.Status.PUBLISH_COMPLETED));
    verify(aggregationBatchDao, times(2)).updateAggregationBatchStatus(any(), any());
    verify(collectorCoreImplHelper, times(1))
        .createAggregatorMessage(
            eq(ITERATION1), eq(List.of("iter1_1", "iter1_2")), any(), eq(false), eq("endpoint"));
    verify(collectorCoreImplHelper, times(1))
        .createAggregatorMessage(
            eq(ITERATION1), eq(List.of("iter1_3")), any(), eq(false), eq("endpoint"));
    verify(assignmentDao, times(3))
        .queryAssignmentIdsOfStatus(
            eq(ITERATION1.getId()),
            eq(AssignmentEntity.Status.UPLOAD_COMPLETED),
            any(Optional.class));
  }

  @Test
  public void testProcess_SetResultUploadCompletedWithBatches_FailUpdate() {
    // arange
    CollectorCoreImpl impl =
        new CollectorCoreImpl(
            taskDao,
            assignmentDao,
            aggregationBatchDao,
            blobDao,
            blobManager,
            collectorCoreImplHelper,
            instantSource,
            messageSender,
            "agTopic",
            "endpoint",
            "muTopic",
            lockRegistry,
            15,
            15,
            2,
            Optional.empty());
    when(lockRegistry.obtain("collector_" + ITERATION1.getId().toString())).thenReturn(lock);
    when(lock.tryLock()).thenReturn(true);
    when(taskDao.getIterationsOfStatus(any())).thenReturn(ImmutableList.of(ITERATION1));
    when(blobManager.generateDownloadGradientDescriptions(ITERATION1))
        .thenReturn(new BlobDescription[] {DIR1_1});
    when(blobDao.listByPartition(eq(DIR1_1), any()))
        .thenReturn(ImmutableList.of("iter1_1/", "iter1_2/", "iter1_3/"));
    when(assignmentDao.createBatchAndUpdateAssignments(any(), any(), any(), any(), any(), any()))
        .thenReturn(true);
    when(assignmentDao.createBatchAndUpdateAssignments(
            any(),
            any(),
            eq(AssignmentEntity.Status.UPLOAD_COMPLETED),
            eq(AssignmentEntity.Status.UPLOAD_COMPLETED),
            any(),
            any()))
        .thenReturn(false);
    when(aggregationBatchDao.queryAggregationBatchIdsOfStatus(
            any(), anyLong(), eq(AggregationBatchEntity.Status.FULL), any()))
        .thenReturn(List.of("batch-1"));
    when(aggregationBatchDao.queryAggregationBatchIdsOfStatus(
            any(), anyLong(), eq(AggregationBatchEntity.Status.PUBLISH_COMPLETED), any()))
        .thenReturn(List.of("batch-1"));
    when(aggregationBatchDao.querySumOfAggregationBatchesOfStatus(
            ITERATION1, 0, List.of(AggregationBatchEntity.Status.PUBLISH_COMPLETED)))
        .thenReturn(2L);
    when(aggregationBatchDao.updateAggregationBatchStatus(any(), any())).thenReturn(true);
    when(assignmentDao.queryAssignmentIdsOfStatus(
            eq(ITERATION1.getId()),
            eq(AssignmentEntity.Status.UPLOAD_COMPLETED),
            any(Optional.class)))
        .thenReturn(ImmutableList.of());
    when(assignmentDao.queryAssignmentIdsOfStatus(
            eq(ITERATION1.getId()),
            eq(AssignmentEntity.Status.UPLOAD_COMPLETED),
            eq(Optional.empty())))
        .thenReturn(ImmutableList.of("iter1_3"));
    when(assignmentDao.queryAssignmentIdsOfStatus(
            eq(ITERATION1.getId()),
            eq(AssignmentEntity.Status.LOCAL_COMPLETED),
            eq(Optional.empty())))
        .thenReturn(ImmutableList.of("iter1_1", "iter1_2", "iter1_3"));
    when(assignmentDao.queryAssignmentIdsOfStatus(
            ITERATION1.getId(), AssignmentEntity.Status.UPLOAD_COMPLETED, Optional.of("batch-1")))
        .thenReturn(ImmutableList.of("iter1_1", "iter1_2"));
    when(collectorCoreImplHelper.createAggregatorMessage(any(), any(), any(), anyBoolean(), any()))
        .thenReturn(AGGREGATOR_MESSAGE);

    // act
    impl.processCollecting();

    // assert
    verify(assignmentDao, times(1))
        .queryAssignmentIdsOfStatus(
            eq(ITERATION1.getId()),
            eq(AssignmentEntity.Status.LOCAL_COMPLETED),
            eq(Optional.empty()));
    verify(assignmentDao, times(1))
        .batchUpdateAssignmentStatus(
            List.of(toAssignmentId(ITERATION1, "iter1_3")),
            Optional.empty(),
            AssignmentEntity.Status.LOCAL_COMPLETED,
            AssignmentEntity.Status.UPLOAD_COMPLETED);
    verify(assignmentDao, times(1))
        .createBatchAndUpdateAssignments(
            eq(
                List.of(
                    toAssignmentId(ITERATION1, "iter1_1"), toAssignmentId(ITERATION1, "iter1_2"))),
            eq(ITERATION1),
            eq(AssignmentEntity.Status.LOCAL_COMPLETED),
            eq(AssignmentEntity.Status.UPLOAD_COMPLETED),
            any(),
            eq(ITERATION1.getId().toString()));
    verify(assignmentDao, times(1))
        .createBatchAndUpdateAssignments(
            eq(List.of(toAssignmentId(ITERATION1, "iter1_3"))),
            eq(ITERATION1),
            eq(AssignmentEntity.Status.UPLOAD_COMPLETED),
            eq(AssignmentEntity.Status.UPLOAD_COMPLETED),
            any(),
            eq(ITERATION1.getId().toString()));
    verify(aggregationBatchDao, times(1))
        .queryAggregationBatchIdsOfStatus(
            ITERATION1,
            0,
            AggregationBatchEntity.Status.FULL,
            Optional.of(ITERATION1.getId().toString()));
    verify(aggregationBatchDao, times(1))
        .querySumOfAggregationBatchesOfStatus(
            ITERATION1, 0, List.of(AggregationBatchEntity.Status.PUBLISH_COMPLETED));
    verify(aggregationBatchDao, times(1)).updateAggregationBatchStatus(any(), any());
    verify(collectorCoreImplHelper, times(1))
        .createAggregatorMessage(
            eq(ITERATION1), eq(List.of("iter1_1", "iter1_2")), any(), eq(false), eq("endpoint"));
    verify(collectorCoreImplHelper, times(0))
        .createAggregatorMessage(
            eq(ITERATION1), eq(List.of("iter1_3")), any(), eq(false), eq("endpoint"));
    verify(assignmentDao, times(2))
        .queryAssignmentIdsOfStatus(
            eq(ITERATION1.getId()),
            eq(AssignmentEntity.Status.UPLOAD_COMPLETED),
            any(Optional.class));
  }

  @Test
  public void testProcess_SetResultUploadCompleted() {
    // arange

    when(lockRegistry.obtain("collector_" + ITERATION1.getId().toString())).thenReturn(lock);
    when(lock.tryLock()).thenReturn(true);
    when(taskDao.getIterationsOfStatus(any())).thenReturn(ImmutableList.of(ITERATION1));
    when(blobManager.generateDownloadGradientDescriptions(ITERATION1))
        .thenReturn(new BlobDescription[] {DIR1_1});
    when(blobDao.listByPartition(eq(DIR1_1), any()))
        .thenReturn(ImmutableList.of("iter1_1/", "iter1_2/", "iter1_3/"));
    when(assignmentDao.queryAssignmentIdsOfStatus(
            eq(ITERATION1.getId()),
            eq(AssignmentEntity.Status.LOCAL_COMPLETED),
            eq(Optional.empty())))
        .thenReturn(ImmutableList.of("iter1_1", "iter1_3", "iter1_4"));

    // act
    core.processCollecting();

    // assert
    verify(assignmentDao, times(1))
        .queryAssignmentIdsOfStatus(
            eq(ITERATION1.getId()),
            eq(AssignmentEntity.Status.LOCAL_COMPLETED),
            eq(Optional.empty()));
    verify(assignmentDao, times(1))
        .batchUpdateAssignmentStatus(
            List.of(toAssignmentId(ITERATION1, "iter1_1"), toAssignmentId(ITERATION1, "iter1_3")),
            Optional.empty(),
            AssignmentEntity.Status.LOCAL_COMPLETED,
            AssignmentEntity.Status.UPLOAD_COMPLETED);
  }

  @Test
  public void testProcessAggregatorNotifications_successHandleFailure() {
    when(taskDao.getIterationById(any())).thenReturn(Optional.of(AGG_ITERATION1));
    when(aggregationBatchDao.getAggregationBatchById(any())).thenReturn(Optional.of(BATCH_ENTITY1));
    when(aggregationBatchDao.updateAggregationBatchStatus(any(), any())).thenReturn(true);
    when(assignmentDao.queryAssignmentIdsOfStatus(
            AGG_ITERATION1.getId(),
            AssignmentEntity.Status.UPLOAD_COMPLETED,
            Optional.of(BATCH_ENTITY1.getBatchId())))
        .thenReturn(ImmutableList.of("iter1_1", "iter1_2"));
    when(assignmentDao.batchUpdateAssignmentStatus(any(), any(), any(), any())).thenReturn(2);
    when(aggregationBatchDao.querySumOfAggregationBatchesOfStatus(any(), anyLong(), any()))
        .thenReturn(1L);
    when(taskDao.updateIterationStatus(any(), any())).thenReturn(true);

    core.processAggregatorNotifications(ATTRIBUTES1);
    verify(taskDao, times(1)).getIterationById(AGG_ITERATION1.getId());
    verify(aggregationBatchDao, times(1)).getAggregationBatchById(BATCH_ENTITY1.getId());
    verify(aggregationBatchDao, times(1))
        .updateAggregationBatchStatus(
            BATCH_ENTITY1,
            BATCH_ENTITY1.toBuilder().status(AggregationBatchEntity.Status.FAILED).build());
    verify(assignmentDao, times(1))
        .queryAssignmentIdsOfStatus(
            AGG_ITERATION1.getId(),
            AssignmentEntity.Status.UPLOAD_COMPLETED,
            Optional.of(BATCH_ENTITY1.getBatchId()));
    verify(assignmentDao, times(1))
        .batchUpdateAssignmentStatus(
            List.of(
                toAssignmentId(AGG_ITERATION1, "iter1_1"),
                toAssignmentId(AGG_ITERATION1, "iter1_2")),
            Optional.of(BATCH_ENTITY1.getBatchId()),
            AssignmentEntity.Status.UPLOAD_COMPLETED,
            AssignmentEntity.Status.REMOTE_FAILED);
    verify(aggregationBatchDao, times(1))
        .querySumOfAggregationBatchesOfStatus(
            AGG_ITERATION1,
            AGG_ITERATION1.getAggregationLevel() - 1,
            List.of(
                AggregationBatchEntity.Status.PUBLISH_COMPLETED,
                AggregationBatchEntity.Status.UPLOAD_COMPLETED));
    verify(taskDao, times(1))
        .updateIterationStatus(
            AGG_ITERATION1,
            AGG_ITERATION1.toBuilder().status(Status.COLLECTING).aggregationLevel(0).build());
  }

  @Test
  public void testProcessAggregatorNotifications_successHandleFailed() {
    when(taskDao.getIterationById(any())).thenReturn(Optional.of(AGG_ITERATION1));
    when(aggregationBatchDao.getAggregationBatchById(any()))
        .thenReturn(
            Optional.of(
                BATCH_ENTITY1.toBuilder().status(AggregationBatchEntity.Status.FAILED).build()));
    when(aggregationBatchDao.updateAggregationBatchStatus(any(), any())).thenReturn(true);
    when(assignmentDao.queryAssignmentIdsOfStatus(
            AGG_ITERATION1.getId(),
            AssignmentEntity.Status.UPLOAD_COMPLETED,
            Optional.of(BATCH_ENTITY1.getBatchId())))
        .thenReturn(ImmutableList.of("iter1_1", "iter1_2"));
    when(assignmentDao.batchUpdateAssignmentStatus(any(), any(), any(), any())).thenReturn(2);
    when(aggregationBatchDao.querySumOfAggregationBatchesOfStatus(any(), anyLong(), any()))
        .thenReturn(1L);
    when(taskDao.updateIterationStatus(any(), any())).thenReturn(true);

    core.processAggregatorNotifications(ATTRIBUTES1);
    verify(taskDao, times(1)).getIterationById(AGG_ITERATION1.getId());
    verify(aggregationBatchDao, times(1)).getAggregationBatchById(BATCH_ENTITY1.getId());
    verify(aggregationBatchDao, times(0)).updateAggregationBatchStatus(any(), any());
    verify(assignmentDao, times(1))
        .queryAssignmentIdsOfStatus(
            AGG_ITERATION1.getId(),
            AssignmentEntity.Status.UPLOAD_COMPLETED,
            Optional.of(BATCH_ENTITY1.getBatchId()));
    verify(assignmentDao, times(1))
        .batchUpdateAssignmentStatus(
            List.of(
                toAssignmentId(AGG_ITERATION1, "iter1_1"),
                toAssignmentId(AGG_ITERATION1, "iter1_2")),
            Optional.of(BATCH_ENTITY1.getBatchId()),
            AssignmentEntity.Status.UPLOAD_COMPLETED,
            AssignmentEntity.Status.REMOTE_FAILED);
    verify(aggregationBatchDao, times(1))
        .querySumOfAggregationBatchesOfStatus(
            AGG_ITERATION1,
            AGG_ITERATION1.getAggregationLevel() - 1,
            List.of(
                AggregationBatchEntity.Status.PUBLISH_COMPLETED,
                AggregationBatchEntity.Status.UPLOAD_COMPLETED));
    verify(taskDao, times(1))
        .updateIterationStatus(
            AGG_ITERATION1,
            AGG_ITERATION1.toBuilder().status(Status.COLLECTING).aggregationLevel(0).build());
  }

  @Test
  public void testProcessAggregatorNotifications_successOK() {
    core.processAggregatorNotifications(
        ATTRIBUTES1.toBuilder().status(AggregatorNotification.Status.OK).build());
    verify(aggregationBatchDao, times(0))
        .updateAggregationBatchStatus(
            BATCH_ENTITY1,
            BATCH_ENTITY1.toBuilder().status(AggregationBatchEntity.Status.FAILED).build());
    verify(assignmentDao, times(0))
        .batchUpdateAssignmentStatus(
            List.of(
                toAssignmentId(AGG_ITERATION1, "iter1_1"),
                toAssignmentId(AGG_ITERATION1, "iter1_2")),
            Optional.of(BATCH_ENTITY1.getBatchId()),
            AssignmentEntity.Status.UPLOAD_COMPLETED,
            AssignmentEntity.Status.REMOTE_FAILED);
    verify(taskDao, times(0))
        .updateIterationStatus(
            AGG_ITERATION1,
            AGG_ITERATION1.toBuilder().status(Status.COLLECTING).aggregationLevel(0).build());
  }

  @Test
  public void testProcessAggregatorNotifications_invalidRequestId() {
    assertThrows(
        IllegalStateException.class,
        () ->
            core.processAggregatorNotifications(
                ATTRIBUTES1.toBuilder().requestId("helloWorld").build()));
  }

  @Test
  public void testProcessAggregatorNotifications_invalidBatchId() {
    when(aggregationBatchDao.getAggregationBatchById(any())).thenReturn(Optional.empty());

    core.processAggregatorNotifications(ATTRIBUTES1);
    verify(aggregationBatchDao, times(1)).getAggregationBatchById(BATCH_ENTITY1.getId());
    verify(aggregationBatchDao, times(0))
        .updateAggregationBatchStatus(
            BATCH_ENTITY1,
            BATCH_ENTITY1.toBuilder().status(AggregationBatchEntity.Status.FAILED).build());
    verify(assignmentDao, times(0))
        .batchUpdateAssignmentStatus(
            List.of(
                toAssignmentId(AGG_ITERATION1, "iter1_1"),
                toAssignmentId(AGG_ITERATION1, "iter1_2")),
            Optional.of(BATCH_ENTITY1.getBatchId()),
            AssignmentEntity.Status.UPLOAD_COMPLETED,
            AssignmentEntity.Status.REMOTE_FAILED);
    verify(taskDao, times(0))
        .updateIterationStatus(
            AGG_ITERATION1,
            AGG_ITERATION1.toBuilder().status(Status.COLLECTING).aggregationLevel(0).build());
  }

  @Test
  public void testProcessAggregatorNotifications_invalidBatchStatus() {
    when(aggregationBatchDao.getAggregationBatchById(any()))
        .thenReturn(
            Optional.of(
                BATCH_ENTITY1.toBuilder().status(AggregationBatchEntity.Status.FULL).build()));

    core.processAggregatorNotifications(ATTRIBUTES1);
    verify(aggregationBatchDao, times(1)).getAggregationBatchById(BATCH_ENTITY1.getId());
    verify(aggregationBatchDao, times(0))
        .updateAggregationBatchStatus(
            BATCH_ENTITY1,
            BATCH_ENTITY1.toBuilder().status(AggregationBatchEntity.Status.FAILED).build());
    verify(assignmentDao, times(0))
        .batchUpdateAssignmentStatus(
            List.of(
                toAssignmentId(AGG_ITERATION1, "iter1_1"),
                toAssignmentId(AGG_ITERATION1, "iter1_2")),
            Optional.of(BATCH_ENTITY1.getBatchId()),
            AssignmentEntity.Status.UPLOAD_COMPLETED,
            AssignmentEntity.Status.REMOTE_FAILED);
    verify(taskDao, times(0))
        .updateIterationStatus(
            AGG_ITERATION1,
            AGG_ITERATION1.toBuilder().status(Status.COLLECTING).aggregationLevel(0).build());
  }

  @Test
  public void testProcessAggregatorNotifications_failUpdateAggregationBatch() {
    when(aggregationBatchDao.getAggregationBatchById(any())).thenReturn(Optional.of(BATCH_ENTITY1));
    when(aggregationBatchDao.updateAggregationBatchStatus(any(), any())).thenReturn(false);

    assertThrows(
        IllegalStateException.class, () -> core.processAggregatorNotifications(ATTRIBUTES1));
    verify(aggregationBatchDao, times(1)).getAggregationBatchById(BATCH_ENTITY1.getId());
    verify(aggregationBatchDao, times(1))
        .updateAggregationBatchStatus(
            BATCH_ENTITY1,
            BATCH_ENTITY1.toBuilder().status(AggregationBatchEntity.Status.FAILED).build());
    verify(assignmentDao, times(0))
        .batchUpdateAssignmentStatus(
            List.of(
                toAssignmentId(AGG_ITERATION1, "iter1_1"),
                toAssignmentId(AGG_ITERATION1, "iter1_2")),
            Optional.of(BATCH_ENTITY1.getBatchId()),
            AssignmentEntity.Status.UPLOAD_COMPLETED,
            AssignmentEntity.Status.REMOTE_FAILED);
    verify(taskDao, times(0))
        .updateIterationStatus(
            AGG_ITERATION1,
            AGG_ITERATION1.toBuilder().status(Status.COLLECTING).aggregationLevel(0).build());
  }

  @Test
  public void testProcessAggregatorNotifications_failUpdateAssignments() {
    when(aggregationBatchDao.getAggregationBatchById(any())).thenReturn(Optional.of(BATCH_ENTITY1));
    when(aggregationBatchDao.updateAggregationBatchStatus(any(), any())).thenReturn(true);
    when(assignmentDao.queryAssignmentIdsOfStatus(
            AGG_ITERATION1.getId(),
            AssignmentEntity.Status.UPLOAD_COMPLETED,
            Optional.of(BATCH_ENTITY1.getBatchId())))
        .thenReturn(ImmutableList.of("iter1_1", "iter1_2"));
    when(assignmentDao.batchUpdateAssignmentStatus(any(), any(), any(), any())).thenReturn(1);
    when(aggregationBatchDao.querySumOfAggregationBatchesOfStatus(any(), anyLong(), any()))
        .thenReturn(1L);
    when(taskDao.updateIterationStatus(any(), any())).thenReturn(true);

    assertThrows(
        IllegalStateException.class, () -> core.processAggregatorNotifications(ATTRIBUTES1));
    verify(aggregationBatchDao, times(1)).getAggregationBatchById(BATCH_ENTITY1.getId());
    verify(aggregationBatchDao, times(1))
        .updateAggregationBatchStatus(
            BATCH_ENTITY1,
            BATCH_ENTITY1.toBuilder().status(AggregationBatchEntity.Status.FAILED).build());
    verify(assignmentDao, times(1))
        .queryAssignmentIdsOfStatus(
            AGG_ITERATION1.getId(),
            AssignmentEntity.Status.UPLOAD_COMPLETED,
            Optional.of(BATCH_ENTITY1.getBatchId()));
    verify(assignmentDao, times(1))
        .batchUpdateAssignmentStatus(
            List.of(
                toAssignmentId(AGG_ITERATION1, "iter1_1"),
                toAssignmentId(AGG_ITERATION1, "iter1_2")),
            Optional.of(BATCH_ENTITY1.getBatchId()),
            AssignmentEntity.Status.UPLOAD_COMPLETED,
            AssignmentEntity.Status.REMOTE_FAILED);
    verify(aggregationBatchDao, times(0))
        .querySumOfAggregationBatchesOfStatus(
            AGG_ITERATION1,
            AGG_ITERATION1.getAggregationLevel() - 1,
            List.of(
                AggregationBatchEntity.Status.PUBLISH_COMPLETED,
                AggregationBatchEntity.Status.UPLOAD_COMPLETED));
    verify(taskDao, times(0))
        .updateIterationStatus(
            AGG_ITERATION1,
            AGG_ITERATION1.toBuilder().status(Status.COLLECTING).aggregationLevel(0).build());
  }

  @Test
  public void testProcessAggregatorNotifications_successIterationCollecting() {
    when(taskDao.getIterationById(any())).thenReturn(Optional.of(ITERATION1));
    when(aggregationBatchDao.getAggregationBatchById(any())).thenReturn(Optional.of(BATCH_ENTITY1));
    when(aggregationBatchDao.updateAggregationBatchStatus(any(), any())).thenReturn(true);
    when(assignmentDao.queryAssignmentIdsOfStatus(
            ITERATION1.getId(),
            AssignmentEntity.Status.UPLOAD_COMPLETED,
            Optional.of(BATCH_ENTITY1.getBatchId())))
        .thenReturn(ImmutableList.of("iter1_1", "iter1_2"));
    when(assignmentDao.batchUpdateAssignmentStatus(any(), any(), any(), any())).thenReturn(2);
    when(aggregationBatchDao.querySumOfAggregationBatchesOfStatus(any(), anyLong(), any()))
        .thenReturn(1L);
    when(taskDao.updateIterationStatus(any(), any())).thenReturn(true);

    core.processAggregatorNotifications(ATTRIBUTES1);
    verify(aggregationBatchDao, times(1)).getAggregationBatchById(BATCH_ENTITY1.getId());
    verify(aggregationBatchDao, times(1))
        .updateAggregationBatchStatus(
            BATCH_ENTITY1,
            BATCH_ENTITY1.toBuilder().status(AggregationBatchEntity.Status.FAILED).build());
    verify(assignmentDao, times(1))
        .queryAssignmentIdsOfStatus(
            ITERATION1.getId(),
            AssignmentEntity.Status.UPLOAD_COMPLETED,
            Optional.of(BATCH_ENTITY1.getBatchId()));
    verify(assignmentDao, times(1))
        .batchUpdateAssignmentStatus(
            List.of(toAssignmentId(ITERATION1, "iter1_1"), toAssignmentId(ITERATION1, "iter1_2")),
            Optional.of(BATCH_ENTITY1.getBatchId()),
            AssignmentEntity.Status.UPLOAD_COMPLETED,
            AssignmentEntity.Status.REMOTE_FAILED);
    verify(taskDao, times(1)).getIterationById(ITERATION1.getId());
    verify(aggregationBatchDao, times(0))
        .querySumOfAggregationBatchesOfStatus(
            ITERATION1,
            ITERATION1.getAggregationLevel() - 1,
            List.of(
                AggregationBatchEntity.Status.PUBLISH_COMPLETED,
                AggregationBatchEntity.Status.UPLOAD_COMPLETED));
    verify(taskDao, times(0))
        .updateIterationStatus(
            ITERATION1,
            ITERATION1.toBuilder().status(Status.COLLECTING).aggregationLevel(0).build());
  }

  @Test
  public void testProcessAggregatorNotifications_successReportGoalMet() {
    when(taskDao.getIterationById(any())).thenReturn(Optional.of(AGG_ITERATION1));
    when(aggregationBatchDao.getAggregationBatchById(any()))
        .thenReturn(
            Optional.of(
                BATCH_ENTITY1.toBuilder().status(AggregationBatchEntity.Status.FAILED).build()));
    when(aggregationBatchDao.updateAggregationBatchStatus(any(), any())).thenReturn(true);
    when(assignmentDao.queryAssignmentIdsOfStatus(
            AGG_ITERATION1.getId(),
            AssignmentEntity.Status.UPLOAD_COMPLETED,
            Optional.of(BATCH_ENTITY1.getBatchId())))
        .thenReturn(ImmutableList.of("iter1_1", "iter1_2"));
    when(assignmentDao.batchUpdateAssignmentStatus(any(), any(), any(), any())).thenReturn(2);
    when(aggregationBatchDao.querySumOfAggregationBatchesOfStatus(any(), anyLong(), any()))
        .thenReturn(20L);
    when(taskDao.updateIterationStatus(any(), any())).thenReturn(true);

    core.processAggregatorNotifications(ATTRIBUTES1);
    verify(taskDao, times(1)).getIterationById(AGG_ITERATION1.getId());
    verify(aggregationBatchDao, times(1)).getAggregationBatchById(BATCH_ENTITY1.getId());
    verify(aggregationBatchDao, times(0)).updateAggregationBatchStatus(any(), any());
    verify(assignmentDao, times(1))
        .queryAssignmentIdsOfStatus(
            AGG_ITERATION1.getId(),
            AssignmentEntity.Status.UPLOAD_COMPLETED,
            Optional.of(BATCH_ENTITY1.getBatchId()));
    verify(assignmentDao, times(1))
        .batchUpdateAssignmentStatus(
            List.of(
                toAssignmentId(AGG_ITERATION1, "iter1_1"),
                toAssignmentId(AGG_ITERATION1, "iter1_2")),
            Optional.of(BATCH_ENTITY1.getBatchId()),
            AssignmentEntity.Status.UPLOAD_COMPLETED,
            AssignmentEntity.Status.REMOTE_FAILED);
    verify(aggregationBatchDao, times(1))
        .querySumOfAggregationBatchesOfStatus(
            AGG_ITERATION1,
            AGG_ITERATION1.getAggregationLevel() - 1,
            List.of(
                AggregationBatchEntity.Status.PUBLISH_COMPLETED,
                AggregationBatchEntity.Status.UPLOAD_COMPLETED));
    verify(taskDao, times(0))
        .updateIterationStatus(
            AGG_ITERATION1,
            AGG_ITERATION1.toBuilder().status(Status.COLLECTING).aggregationLevel(0).build());
  }

  @Test
  public void testProcessAggregatorNotifications_failureThresholdMet() {
    when(taskDao.getIterationById(any())).thenReturn(Optional.of(AGG_ITERATION1));
    when(aggregationBatchDao.getAggregationBatchById(any()))
            .thenReturn(
                    Optional.of(
                            BATCH_ENTITY1.toBuilder().status(AggregationBatchEntity.Status.FAILED).build()));
    when(aggregationBatchDao.updateAggregationBatchStatus(any(), any())).thenReturn(true);
    when(assignmentDao.queryAssignmentIdsOfStatus(
            AGG_ITERATION1.getId(),
            AssignmentEntity.Status.UPLOAD_COMPLETED,
            Optional.of(BATCH_ENTITY1.getBatchId())))
            .thenReturn(ImmutableList.of("iter1_1", "iter1_2"));
    when(assignmentDao.batchUpdateAssignmentStatus(any(), any(), any(), any())).thenReturn(2);
    when(aggregationBatchDao.querySumOfAggregationBatchesOfStatus(any(), anyLong(), any()))
            .thenReturn(600L);
    when(taskDao.updateIterationStatus(any(), any())).thenReturn(true);

    core.processAggregatorNotifications(ATTRIBUTES1);
    verify(taskDao, times(1)).getIterationById(AGG_ITERATION1.getId());
    verify(aggregationBatchDao, times(1)).getAggregationBatchById(BATCH_ENTITY1.getId());
    verify(aggregationBatchDao, times(0)).updateAggregationBatchStatus(any(), any());
    verify(assignmentDao, times(1))
            .queryAssignmentIdsOfStatus(
                    AGG_ITERATION1.getId(),
                    AssignmentEntity.Status.UPLOAD_COMPLETED,
                    Optional.of(BATCH_ENTITY1.getBatchId()));
    verify(assignmentDao, times(1))
            .batchUpdateAssignmentStatus(
                    List.of(
                            toAssignmentId(AGG_ITERATION1, "iter1_1"),
                            toAssignmentId(AGG_ITERATION1, "iter1_2")),
                    Optional.of(BATCH_ENTITY1.getBatchId()),
                    AssignmentEntity.Status.UPLOAD_COMPLETED,
                    AssignmentEntity.Status.REMOTE_FAILED);
    verify(aggregationBatchDao, times(1))
            .querySumOfAggregationBatchesOfStatus(
                    AGG_ITERATION1,
                    0,
                    List.of(
                            AggregationBatchEntity.Status.FAILED));
    verify(taskDao, times(1))
            .updateIterationStatus(
                    AGG_ITERATION1,
                    AGG_ITERATION1.toBuilder().status(Status.AGGREGATING_FAILED).build());
  }

  @Test
  public void testProcessAggregatorNotifications_failedIterationStatusUpdate() {
    when(taskDao.getIterationById(any())).thenReturn(Optional.of(AGG_ITERATION1));
    when(aggregationBatchDao.getAggregationBatchById(any()))
        .thenReturn(
            Optional.of(
                BATCH_ENTITY1.toBuilder().status(AggregationBatchEntity.Status.FAILED).build()));
    when(aggregationBatchDao.updateAggregationBatchStatus(any(), any())).thenReturn(true);
    when(assignmentDao.queryAssignmentIdsOfStatus(
            AGG_ITERATION1.getId(),
            AssignmentEntity.Status.UPLOAD_COMPLETED,
            Optional.of(BATCH_ENTITY1.getBatchId())))
        .thenReturn(ImmutableList.of("iter1_1", "iter1_2"));
    when(assignmentDao.batchUpdateAssignmentStatus(any(), any(), any(), any())).thenReturn(2);
    when(aggregationBatchDao.querySumOfAggregationBatchesOfStatus(any(), anyLong(), any()))
        .thenReturn(1L);
    when(taskDao.updateIterationStatus(any(), any())).thenReturn(false);

    assertThrows(
        IllegalStateException.class, () -> core.processAggregatorNotifications(ATTRIBUTES1));
    verify(taskDao, times(1)).getIterationById(AGG_ITERATION1.getId());
    verify(aggregationBatchDao, times(1)).getAggregationBatchById(BATCH_ENTITY1.getId());
    verify(aggregationBatchDao, times(0)).updateAggregationBatchStatus(any(), any());
    verify(assignmentDao, times(1))
        .queryAssignmentIdsOfStatus(
            AGG_ITERATION1.getId(),
            AssignmentEntity.Status.UPLOAD_COMPLETED,
            Optional.of(BATCH_ENTITY1.getBatchId()));
    verify(assignmentDao, times(1))
        .batchUpdateAssignmentStatus(
            List.of(
                toAssignmentId(AGG_ITERATION1, "iter1_1"),
                toAssignmentId(AGG_ITERATION1, "iter1_2")),
            Optional.of(BATCH_ENTITY1.getBatchId()),
            AssignmentEntity.Status.UPLOAD_COMPLETED,
            AssignmentEntity.Status.REMOTE_FAILED);
    verify(aggregationBatchDao, times(1))
        .querySumOfAggregationBatchesOfStatus(
            AGG_ITERATION1,
            AGG_ITERATION1.getAggregationLevel() - 1,
            List.of(
                AggregationBatchEntity.Status.PUBLISH_COMPLETED,
                AggregationBatchEntity.Status.UPLOAD_COMPLETED));
    verify(taskDao, times(1))
        .updateIterationStatus(
            AGG_ITERATION1,
            AGG_ITERATION1.toBuilder().status(Status.COLLECTING).aggregationLevel(0).build());
  }

  @Test
  public void testProcessAggregatorNotifications_failedLock() throws Exception {
    when(lockRegistry.obtain("collector_" + AGG_ITERATION1.getId().toString())).thenReturn(lock);
    when(lock.tryLock(anyLong(), any())).thenReturn(false);
    when(taskDao.getIterationById(any())).thenReturn(Optional.of(AGG_ITERATION1));
    when(aggregationBatchDao.getAggregationBatchById(any()))
            .thenReturn(
                    Optional.of(
                            BATCH_ENTITY1.toBuilder().status(AggregationBatchEntity.Status.FAILED).build()));
    when(aggregationBatchDao.updateAggregationBatchStatus(any(), any())).thenReturn(true);
    when(assignmentDao.queryAssignmentIdsOfStatus(
            AGG_ITERATION1.getId(),
            AssignmentEntity.Status.UPLOAD_COMPLETED,
            Optional.of(BATCH_ENTITY1.getBatchId())))
            .thenReturn(ImmutableList.of("iter1_1", "iter1_2"));
    when(assignmentDao.batchUpdateAssignmentStatus(any(), any(), any(), any())).thenReturn(2);
    when(aggregationBatchDao.querySumOfAggregationBatchesOfStatus(any(), anyLong(), any()))
            .thenReturn(1L);
    when(taskDao.updateIterationStatus(any(), any())).thenReturn(false);

    assertThrows(
            IllegalStateException.class, () -> core.processAggregatorNotifications(ATTRIBUTES1));
    verify(taskDao, times(0)).getIterationById(AGG_ITERATION1.getId());
    verify(aggregationBatchDao, times(1)).getAggregationBatchById(BATCH_ENTITY1.getId());
    verify(aggregationBatchDao, times(0)).updateAggregationBatchStatus(any(), any());
    verify(assignmentDao, times(1))
            .queryAssignmentIdsOfStatus(
                    AGG_ITERATION1.getId(),
                    AssignmentEntity.Status.UPLOAD_COMPLETED,
                    Optional.of(BATCH_ENTITY1.getBatchId()));
    verify(assignmentDao, times(1))
            .batchUpdateAssignmentStatus(
                    List.of(
                            toAssignmentId(AGG_ITERATION1, "iter1_1"),
                            toAssignmentId(AGG_ITERATION1, "iter1_2")),
                    Optional.of(BATCH_ENTITY1.getBatchId()),
                    AssignmentEntity.Status.UPLOAD_COMPLETED,
                    AssignmentEntity.Status.REMOTE_FAILED);
    verify(aggregationBatchDao, times(0))
            .querySumOfAggregationBatchesOfStatus(
                    AGG_ITERATION1,
                    AGG_ITERATION1.getAggregationLevel() - 1,
                    List.of(
                            AggregationBatchEntity.Status.PUBLISH_COMPLETED,
                            AggregationBatchEntity.Status.UPLOAD_COMPLETED));
    verify(taskDao, times(0))
            .updateIterationStatus(
                    AGG_ITERATION1,
                    AGG_ITERATION1.toBuilder().status(Status.COLLECTING).aggregationLevel(0).build());
  }

  private AssignmentId toAssignmentId(IterationEntity iteration, String assignmentId) {
    return AssignmentId.builder()
        .populationName(iteration.getPopulationName())
        .taskId(iteration.getTaskId())
        .iterationId(iteration.getIterationId())
        .attemptId(iteration.getAttemptId())
        .assignmentId(assignmentId)
        .build();
  }
}
