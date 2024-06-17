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

package com.google.ondevicepersonalization.federatedcompute.shuffler.taskscheduler.core;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.fcp.plan.PhaseSession;
import com.google.fcp.plan.PhaseSessionV2;
import com.google.fcp.plan.TensorflowPhaseSessionV2;
import com.google.fcp.plan.TensorflowPlanSession;
import com.google.ondevicepersonalization.federatedcompute.proto.CheckPointSelector;
import com.google.ondevicepersonalization.federatedcompute.proto.EvaluationInfo;
import com.google.ondevicepersonalization.federatedcompute.proto.EveryKHoursCheckpointSelector;
import com.google.ondevicepersonalization.federatedcompute.proto.EveryKIterationsCheckpointSelector;
import com.google.ondevicepersonalization.federatedcompute.proto.IterationInfo;
import com.google.ondevicepersonalization.federatedcompute.proto.TaskInfo;
import com.google.ondevicepersonalization.federatedcompute.proto.TrainingInfo;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.CompressionUtils.CompressionFormat;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.ProtoParser;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.RandomGenerator;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobDescription;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobManager;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.IterationEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.ModelMetricsDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.ModelMetricsEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.tensorflow.TensorflowPlanSessionFactory;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(JUnit4.class)
public class TaskSchedulerCoreHelperTest {

  private static final TaskInfo TRAINING_TASK_INFO =
      TaskInfo.newBuilder()
          .setTrafficWeight(1)
          .setTrainingInfo(TrainingInfo.getDefaultInstance())
          .build();
  private static final String TRAINING_TASK_INFO_STRING =
      ProtoParser.toJsonString(TRAINING_TASK_INFO);
  private static final IterationInfo TRAINING_ITERATION_INFO =
      IterationInfo.newBuilder().setTaskInfo(TRAINING_TASK_INFO).build();
  private static final String TRAINING_ITERATION_INFO_STRING =
      ProtoParser.toJsonString(TRAINING_ITERATION_INFO);
  private static final TaskInfo EVALUATION_TASK_INFO =
      TaskInfo.newBuilder()
          .setTrafficWeight(1)
          .setEvaluationInfo(EvaluationInfo.getDefaultInstance())
          .build();
  private static final String EVALUATION_TASK_INFO_STRING =
      ProtoParser.toJsonString(EVALUATION_TASK_INFO);
  private static final IterationInfo EVALUATION_ITERATION_INFO =
      IterationInfo.newBuilder().setTaskInfo(EVALUATION_TASK_INFO).build();
  private static final String EVALUATION_ITERATION_INFO_STRING =
      ProtoParser.toJsonString(EVALUATION_ITERATION_INFO);

  private static final TaskEntity TRAINING_TASK =
      TaskEntity.builder()
          .populationName("us")
          .taskId(13)
          .totalIteration(3)
          .info(TRAINING_TASK_INFO_STRING)
          .build();

  private static final TaskEntity EVALUATION_TASK =
      TRAINING_TASK.toBuilder().info(EVALUATION_TASK_INFO_STRING).build();

  private static final IterationEntity TRAINING_ITERATION =
      IterationEntity.builder()
          .populationName("us")
          .taskId(13)
          .iterationId(1)
          .info(TRAINING_ITERATION_INFO_STRING)
          .build();

  private static final IterationEntity EVALUATION_ITERATION =
      IterationEntity.builder()
          .populationName("us")
          .taskId(14)
          .iterationId(1)
          .info(EVALUATION_ITERATION_INFO_STRING)
          .build();

  private static final BlobDescription DEVICE_PLAN_DESCRIPTION_1 =
      BlobDescription.builder().host("test-m-1").resourceObject("us/36/18/d/0/device_plan").build();

  private static final BlobDescription[] DEVICE_PLAN_DESCRIPTIONS =
      new BlobDescription[] {DEVICE_PLAN_DESCRIPTION_1};

  private static final BlobDescription SERVER_PLAN_DESCRIPTION_1 =
      BlobDescription.builder().host("test-m-1").resourceObject("us/36/18/d/0/server_plan").build();

  private static final BlobDescription[] SERVER_PLAN_DESCRIPTIONS =
      new BlobDescription[] {SERVER_PLAN_DESCRIPTION_1};

  private static final BlobDescription CHECKPOINT_DESCRIPTIONS_1 =
      BlobDescription.builder().host("test-m-1").resourceObject("us/36/18/d/0/checkpoint").build();

  private static final BlobDescription[] CHECKPOINT_DESCRIPTIONS =
      new BlobDescription[] {CHECKPOINT_DESCRIPTIONS_1};

  private static final BlobDescription CLIENT_CHECKPOINT_DESCRIPTIONS_1 =
      BlobDescription.builder()
          .host("test-m-1")
          .resourceObject("us/36/18/d/0/client_checkpoint")
          .build();

  private static final BlobDescription[] CLIENT_CHECKPOINT_DESCRIPTIONS =
      new BlobDescription[] {CLIENT_CHECKPOINT_DESCRIPTIONS_1};

  private static final BlobDescription METRICS_DESCRIPTIONS_1 =
      BlobDescription.builder().host("test-m-1").resourceObject("us/36/18/d/0/metrics").build();

  private static final BlobDescription[] METRICS_DESCRIPTIONS =
      new BlobDescription[] {METRICS_DESCRIPTIONS_1};

  private static final List<CompressionFormat> COMPRESSION_FORMATS =
      Arrays.asList(CompressionFormat.GZIP);

  private byte[] PLAN;
  private static final byte[] CHECKPOINT = new byte[] {0, 0};

  private @Mock RandomGenerator mockRandomGenerator;
  private @Mock TaskDao mockTaskDao;
  private @Mock ModelMetricsDao mockModelMetricsDao;
  private @Mock BlobDao mockBlobDao;
  private @Mock BlobManager mockBlobManager;
  private @Mock TensorflowPlanSessionFactory mockTensorflowPlanSessionFactory;
  private @Mock TensorflowPlanSession mockTensorflowPlanSession;
  private @Mock PhaseSessionV2.IntermediateResult mockIntermediateResult;
  private @Mock TensorflowPhaseSessionV2 mockTensorflowPhaseSessionV2;
  private @Mock PhaseSession mockPhaseSession;

  @InjectMocks TaskSchedulerCoreHelper taskSchedulerCoreHelper;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    when(mockBlobManager.generateUploadDevicePlanDescriptions(any()))
        .thenReturn(DEVICE_PLAN_DESCRIPTIONS);
    when(mockBlobManager.generateUploadServerPlanDescription(any()))
        .thenReturn(SERVER_PLAN_DESCRIPTIONS);
    when(mockBlobManager.generateUploadCheckpointDescriptions(any()))
        .thenReturn(CHECKPOINT_DESCRIPTIONS);
    when(mockBlobManager.generateUploadClientCheckpointDescriptions(any()))
        .thenReturn(CLIENT_CHECKPOINT_DESCRIPTIONS);
    when(mockBlobManager.generateUploadMetricsDescriptions(any())).thenReturn(METRICS_DESCRIPTIONS);

    PLAN = getClass().getResourceAsStream("/resources/plan_v1").readAllBytes();
  }

  @Test
  public void buildIterationInfo_noEvaluationInfo_returnIterationInfoWithTaskInfoOnly() {
    // prepare
    TaskInfo taskInfo = TaskInfo.newBuilder().setTrafficWeight(1).build();

    // act and assert
    assertThat(taskSchedulerCoreHelper.buildIterationInfo(taskInfo).get())
        .isEqualTo(IterationInfo.newBuilder().setTaskInfo(taskInfo).build());
  }

  @Test
  public void
      buildIterationInfo_hasEvaluationInfoWithIterationSelector_returnIterationInfoWithEvaluation() {
    // prepare
    TaskInfo taskInfo =
        TaskInfo.newBuilder()
            .setTrafficWeight(1)
            .setEvaluationInfo(
                EvaluationInfo.newBuilder()
                    .setTrainingPopulationName("us")
                    .setTrainingTaskId(1)
                    .setCheckPointSelector(
                        CheckPointSelector.newBuilder()
                            .setIterationSelector(
                                EveryKIterationsCheckpointSelector.newBuilder().setSize(2))))
            .build();
    when(mockRandomGenerator.nextInt(anyInt())).thenReturn(2);
    when(mockTaskDao.getIterationIdsPerEveryKIterationsSelector(
            anyString(), anyLong(), anyLong(), anyLong()))
        .thenReturn(ImmutableList.of(1L, 2L, 3L));

    // act and assert
    assertThat(taskSchedulerCoreHelper.buildIterationInfo(taskInfo).get())
        .isEqualTo(
            IterationInfo.newBuilder()
                .setTaskInfo(taskInfo)
                .setEvaluationTrainingIterationId(3L)
                .build());
    verify(mockTaskDao).getIterationIdsPerEveryKIterationsSelector("us", 1, 2, 24);
    verifyNoMoreInteractions(mockTaskDao);
  }

  @Test
  public void
      buildIterationInfo_hasEvaluationInfoWithDurationSelector_returnIterationInfoWithEvaluation() {
    // prepare
    TaskInfo taskInfo =
        TaskInfo.newBuilder()
            .setTrafficWeight(1)
            .setEvaluationInfo(
                EvaluationInfo.newBuilder()
                    .setTrainingPopulationName("us")
                    .setTrainingTaskId(1)
                    .setCheckPointSelector(
                        CheckPointSelector.newBuilder()
                            .setDurationSelector(
                                EveryKHoursCheckpointSelector.newBuilder().setHours(2))))
            .build();

    // act and assert
    when(mockRandomGenerator.nextInt(anyInt())).thenReturn(2);
    when(mockTaskDao.getIterationIdsPerEveryKHoursSelector(
            anyString(), anyLong(), anyLong(), anyLong()))
        .thenReturn(ImmutableList.of(1L, 2L, 3L));

    // act and assert
    assertThat(taskSchedulerCoreHelper.buildIterationInfo(taskInfo).get())
        .isEqualTo(
            IterationInfo.newBuilder()
                .setTaskInfo(taskInfo)
                .setEvaluationTrainingIterationId(3L)
                .build());
    verify(mockTaskDao).getIterationIdsPerEveryKHoursSelector("us", 1, 2, 24);
    verifyNoMoreInteractions(mockTaskDao);
  }

  @Test
  public void buildIterationInfo_hasInvalidEvaluationInfo_throwException() {
    // prepare
    TaskInfo taskInfo =
        TaskInfo.newBuilder()
            .setTrafficWeight(1)
            .setEvaluationInfo(
                EvaluationInfo.newBuilder().setTrainingPopulationName("us").setTrainingTaskId(1))
            .build();

    // act and assert
    assertThrows(
        UnsupportedOperationException.class,
        () -> taskSchedulerCoreHelper.buildIterationInfo(taskInfo));
  }

  @Test
  public void isTaskReadyToStart_hasZeroTotalIteration_returnFalse() {
    // prepare
    TaskEntity task = TRAINING_TASK.toBuilder().totalIteration(0).build();

    // act and assert
    assertFalse(taskSchedulerCoreHelper.isActiveTaskReadyToStart(task));
    verify(mockBlobDao, times(0)).exists(any());
  }

  @Test
  public void isTaskReadyToStart_taskWithoutInfo_treatAsTrainingTask() {
    // prepare
    when(mockBlobDao.checkExistsAndGzipContentIfNeeded(any())).thenReturn(true);

    // act and assert
    assertTrue(taskSchedulerCoreHelper.isActiveTaskReadyToStart(TRAINING_TASK));
    verify(mockBlobDao, times(3)).checkExistsAndGzipContentIfNeeded(any());
  }

  @Test
  public void isTaskReadyToStart_trainingTaskMissingBlobFiles_returnFalse() {
    // prepare
    when(mockBlobDao.checkExistsAndGzipContentIfNeeded(DEVICE_PLAN_DESCRIPTIONS)).thenReturn(true);
    when(mockBlobDao.checkExistsAndGzipContentIfNeeded(SERVER_PLAN_DESCRIPTIONS)).thenReturn(true);
    when(mockBlobDao.checkExistsAndGzipContentIfNeeded(CHECKPOINT_DESCRIPTIONS)).thenReturn(false);

    // act and assert
    assertFalse(taskSchedulerCoreHelper.isActiveTaskReadyToStart(TRAINING_TASK));
    verify(mockBlobDao, times(3)).checkExistsAndGzipContentIfNeeded(any());
  }

  @Test
  public void isTaskReadyToStart_trainingTaskWithEnoughBlobFiles_returnFalse() {
    // prepare
    when(mockBlobDao.checkExistsAndGzipContentIfNeeded(DEVICE_PLAN_DESCRIPTIONS)).thenReturn(true);
    when(mockBlobDao.checkExistsAndGzipContentIfNeeded(SERVER_PLAN_DESCRIPTIONS)).thenReturn(true);
    when(mockBlobDao.checkExistsAndGzipContentIfNeeded(CHECKPOINT_DESCRIPTIONS)).thenReturn(true);

    // act and assert
    assertTrue(taskSchedulerCoreHelper.isActiveTaskReadyToStart(TRAINING_TASK));
    verify(mockBlobDao, times(3)).checkExistsAndGzipContentIfNeeded(any());
  }

  @Test
  public void isTaskReadyToStart_evaluationTaskMissingBlobFiles_returnFalse() {
    // prepare
    when(mockBlobDao.checkExistsAndGzipContentIfNeeded(DEVICE_PLAN_DESCRIPTIONS)).thenReturn(true);
    when(mockBlobDao.checkExistsAndGzipContentIfNeeded(SERVER_PLAN_DESCRIPTIONS)).thenReturn(false);

    // act and assert
    assertFalse(taskSchedulerCoreHelper.isActiveTaskReadyToStart(EVALUATION_TASK));
    verify(mockBlobDao, times(2)).checkExistsAndGzipContentIfNeeded(any());
  }

  @Test
  public void isTaskReadyToStart_evaluationTaskWithEnoughBlobFiles_returnTrue() {
    // prepare
    when(mockBlobDao.checkExistsAndGzipContentIfNeeded(DEVICE_PLAN_DESCRIPTIONS)).thenReturn(true);
    when(mockBlobDao.checkExistsAndGzipContentIfNeeded(SERVER_PLAN_DESCRIPTIONS)).thenReturn(true);

    // act and assert
    assertTrue(taskSchedulerCoreHelper.isActiveTaskReadyToStart(EVALUATION_TASK));
    verify(mockBlobDao, times(2)).checkExistsAndGzipContentIfNeeded(any());
  }

  @Test
  public void generateAndUploadDeviceCheckpoint_trainingTaskAndPlanAndCheckpointReady_complete()
      throws IOException {
    // prepare
    prepareUploadDeviceCheckpoint(TRAINING_ITERATION);
    doNothing().when(mockBlobDao).compressAndUpload(any(), any());
    byte[] plan_v2 = getClass().getResourceAsStream("/resources/plan_v2").readAllBytes();
    when(mockBlobDao.downloadAndDecompressIfNeeded(SERVER_PLAN_DESCRIPTION_1)).thenReturn(plan_v2);

    // act and assert
    taskSchedulerCoreHelper.generateAndUploadDeviceCheckpoint(TRAINING_ITERATION);
    verify(mockBlobManager, times(1))
        .generateDownloadCheckpointDescription(isA(IterationEntity.class));
    verify(mockBlobManager, times(1)).generateDownloadServerPlanDescription(any());
    verify(mockBlobDao, times(1)).exists(any());
    verify(mockBlobDao, times(2)).downloadAndDecompressIfNeeded(any());
    verify(mockBlobManager, times(1)).generateUploadClientCheckpointDescriptions(any());
    verify(mockTensorflowPlanSessionFactory, times(1)).createPhaseSessionV2(any());
    verify(mockTensorflowPhaseSessionV2, times(1)).getClientCheckpoint(any());
    verify(mockBlobDao, times(2)).compressAndUpload(any(), any());
  }

  @Test
  public void generateAndUploadDeviceCheckpoint_trainingTaskAndPlanV2AndCheckpointReady_complete()
      throws IOException {
    // prepare
    prepareUploadDeviceCheckpoint(TRAINING_ITERATION);
    doNothing().when(mockBlobDao).compressAndUpload(any(), any());

    // act and assert
    taskSchedulerCoreHelper.generateAndUploadDeviceCheckpoint(TRAINING_ITERATION);
    verify(mockBlobManager, times(1))
        .generateDownloadCheckpointDescription(isA(IterationEntity.class));
    verify(mockBlobManager, times(1)).generateDownloadServerPlanDescription(any());
    verify(mockBlobDao, times(1)).exists(any());
    verify(mockBlobDao, times(2)).downloadAndDecompressIfNeeded(any());
    verify(mockBlobManager, times(1)).generateUploadClientCheckpointDescriptions(any());
    verify(mockTensorflowPlanSessionFactory, times(1)).createPlanSession(any());
    verify(mockPhaseSession, times(1)).getClientCheckpoint(any());
    verify(mockBlobDao, times(2)).compressAndUpload(any(), any());
  }

  @Test
  public void generateAndUploadDeviceCheckpoint_planAndCheckpointNotReady_throwException() {
    // prepare
    when(mockBlobDao.exists(any())).thenReturn(false);

    // act and assert
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> taskSchedulerCoreHelper.generateAndUploadDeviceCheckpoint(TRAINING_ITERATION));
    assertThat(exception).hasMessageThat().contains("server checkpoint or plan do not exist");
    verify(mockBlobManager, times(1))
        .generateDownloadCheckpointDescription(isA(IterationEntity.class));
    verify(mockBlobManager, times(1)).generateDownloadServerPlanDescription(any());
    verify(mockBlobDao, times(1)).exists(any());
    verifyNoInteractions(mockTensorflowPlanSessionFactory);
    verifyNoInteractions(mockPhaseSession);
    verifyNoMoreInteractions(mockBlobDao);
  }

  @Test
  public void generateAndUploadDeviceCheckpoint_blobDaoUploadHasException_throwException()
      throws IOException {
    // prepare
    prepareUploadDeviceCheckpoint(TRAINING_ITERATION);
    doThrow(new IOException()).when(mockBlobDao).compressAndUpload(any(), any());

    // act and assert
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> taskSchedulerCoreHelper.generateAndUploadDeviceCheckpoint(TRAINING_ITERATION));
    assertThat(exception).hasMessageThat().contains("Failed to upload client checkpoint");
    verify(mockBlobManager, times(1))
        .generateDownloadCheckpointDescription(isA(IterationEntity.class));
    verify(mockBlobManager, times(1)).generateDownloadServerPlanDescription(any());
    verify(mockBlobDao, times(1)).exists(any());
    verify(mockTensorflowPlanSessionFactory, times(1)).createPlanSession(any());
    verify(mockPhaseSession, times(1)).getClientCheckpoint(any());
    verify(mockBlobDao, times(1)).compressAndUpload(any(), any());
  }

  @Test
  public void prepareNewIteration_trainingTask_returnEntityWithoutUpload() {
    // prepare
    IterationInfo iterationInfo =
        IterationInfo.newBuilder()
            .setTaskInfo(
                TaskInfo.newBuilder()
                    .setTrafficWeight(1)
                    .setTrainingInfo(TrainingInfo.getDefaultInstance()))
            .build();

    // act
    IterationEntity entity =
        taskSchedulerCoreHelper.prepareNewIterationAndUploadDeviceCheckpointIfNeeded(
            TRAINING_TASK, 10, iterationInfo);

    // assert
    assertThat(entity)
        .isEqualTo(
            IterationEntity.builder()
                .populationName("us")
                .taskId(13)
                .iterationId(11)
                .resultId(11)
                .baseIterationId(10)
                .baseOnResultId(10)
                .attemptId(0)
                .reportGoal(0)
                .status(IterationEntity.Status.COLLECTING)
                .info(ProtoParser.toJsonString(iterationInfo))
                .aggregationLevel(0)
                .build());
    verifyNoInteractions(mockBlobDao);
    verifyNoInteractions(mockBlobManager);
    verifyNoInteractions(mockTensorflowPlanSessionFactory);
    verifyNoInteractions(mockPhaseSession);
  }

  @Test
  public void prepareNewIteration_evaluationTask_returnEntityWithoutUpload() throws IOException {
    // prepare
    IterationInfo iterationInfo =
        IterationInfo.newBuilder()
            .setTaskInfo(
                TaskInfo.newBuilder()
                    .setTrafficWeight(1)
                    .setEvaluationInfo(EvaluationInfo.getDefaultInstance()))
            .build();
    IterationEntity expectedEntity =
        IterationEntity.builder()
            .populationName("us")
            .taskId(13)
            .iterationId(11)
            .resultId(11)
            .baseIterationId(11)
            .baseOnResultId(11)
            .attemptId(0)
            .reportGoal(0)
            .status(IterationEntity.Status.COLLECTING)
            .info(ProtoParser.toJsonString(iterationInfo))
            .aggregationLevel(0)
            .build();
    prepareUploadDeviceCheckpoint(expectedEntity);

    // act
    IterationEntity entity =
        taskSchedulerCoreHelper.prepareNewIterationAndUploadDeviceCheckpointIfNeeded(
            TRAINING_TASK, 10, iterationInfo);

    // assert
    assertThat(entity).isEqualTo(expectedEntity);
    verify(mockBlobManager, times(1))
        .generateDownloadCheckpointDescription(isA(IterationEntity.class));
    verify(mockBlobManager, times(1)).generateDownloadServerPlanDescription(any());
    verify(mockBlobDao, times(1)).exists(any());
    verify(mockBlobDao, times(2)).downloadAndDecompressIfNeeded(any());
    verify(mockBlobManager, times(1)).generateUploadClientCheckpointDescriptions(any());
    verify(mockTensorflowPlanSessionFactory, times(1)).createPlanSession(any());
    verify(mockPhaseSession, times(1)).getClientCheckpoint(any());
    verify(mockBlobDao, times(2)).compressAndUpload(any(), any());
  }

  @Test
  public void isApplyingDone_metricsNotReady_returnFalse() {
    // prepare
    when(mockBlobDao.exists(METRICS_DESCRIPTIONS)).thenReturn(false);
    when(mockBlobDao.exists(CHECKPOINT_DESCRIPTIONS)).thenReturn(true);
    when(mockBlobDao.exists(CLIENT_CHECKPOINT_DESCRIPTIONS)).thenReturn(true);

    // act and assert
    assertFalse(taskSchedulerCoreHelper.isApplyingDone(EVALUATION_ITERATION));
    assertFalse(taskSchedulerCoreHelper.isApplyingDone(TRAINING_ITERATION));
  }

  @Test
  public void isApplyingDone_evalIterationWithMetricsReady_returnTrue() {
    // prepare
    when(mockBlobDao.exists(METRICS_DESCRIPTIONS)).thenReturn(true);

    // act and assert
    assertTrue(taskSchedulerCoreHelper.isApplyingDone(EVALUATION_ITERATION));
  }

  @Test
  public void isApplyingDone_trainingIterationWithCheckpointNotReady_returnFalse() {
    // prepare
    when(mockBlobDao.exists(METRICS_DESCRIPTIONS)).thenReturn(true);
    when(mockBlobDao.exists(CHECKPOINT_DESCRIPTIONS)).thenReturn(false);
    when(mockBlobDao.exists(CLIENT_CHECKPOINT_DESCRIPTIONS)).thenReturn(true);

    // act and assert
    assertFalse(taskSchedulerCoreHelper.isApplyingDone(TRAINING_ITERATION));
  }

  @Test
  public void isApplyingDone_trainingIterationWithClientCheckpointNotReady_returnFalse() {
    // prepare
    when(mockBlobDao.exists(METRICS_DESCRIPTIONS)).thenReturn(true);
    when(mockBlobDao.exists(CHECKPOINT_DESCRIPTIONS)).thenReturn(true);
    when(mockBlobDao.exists(CLIENT_CHECKPOINT_DESCRIPTIONS)).thenReturn(false);

    // act and assert
    assertFalse(taskSchedulerCoreHelper.isApplyingDone(TRAINING_ITERATION));
  }

  @Test
  public void isApplyingDone_trainingIterationWithAllBlobsReady_returnTrue() {
    // prepare
    when(mockBlobDao.exists(METRICS_DESCRIPTIONS)).thenReturn(true);
    when(mockBlobDao.exists(CHECKPOINT_DESCRIPTIONS)).thenReturn(true);
    when(mockBlobDao.exists(CLIENT_CHECKPOINT_DESCRIPTIONS)).thenReturn(true);

    // act and assert
    assertTrue(taskSchedulerCoreHelper.isApplyingDone(TRAINING_ITERATION));
  }

  @Test
  public void parseMetricsAndUpsert_metricsReady_returnTrue() {
    // prepare
    when(mockBlobManager.generateUploadMetricsDescriptions(TRAINING_ITERATION))
        .thenReturn(METRICS_DESCRIPTIONS);
    when(mockBlobDao.exists(METRICS_DESCRIPTIONS)).thenReturn(true);
    String jsonString = "{\"loss\": 0.5, \"auc-roc\": 0.7}";
    when(mockBlobDao.downloadAndDecompressIfNeeded(METRICS_DESCRIPTIONS[0]))
        .thenReturn(jsonString.getBytes(StandardCharsets.UTF_8));
    when(mockModelMetricsDao.upsertModelMetrics(any())).thenReturn(true);

    // act and assert
    assertTrue(taskSchedulerCoreHelper.parseMetricsAndUpsert(TRAINING_ITERATION));
    verify(mockModelMetricsDao, times(1))
        .upsertModelMetrics(
            ImmutableList.of(
                ModelMetricsEntity.builder()
                    .populationName("us")
                    .taskId(13)
                    .iterationId(1)
                    .metricName("loss")
                    .metricValue(0.5)
                    .build(),
                ModelMetricsEntity.builder()
                    .populationName("us")
                    .taskId(13)
                    .iterationId(1)
                    .metricName("auc-roc")
                    .metricValue(0.7)
                    .build()));
  }

  @Test
  public void parseMetricsAndUpsert_metricsNotExist_returnFalse() {
    // prepare
    when(mockBlobManager.generateUploadMetricsDescriptions(TRAINING_ITERATION))
        .thenReturn(METRICS_DESCRIPTIONS);
    when(mockBlobDao.exists(METRICS_DESCRIPTIONS)).thenReturn(false);

    // act and assert
    assertFalse(taskSchedulerCoreHelper.parseMetricsAndUpsert(TRAINING_ITERATION));
    verifyNoInteractions(mockModelMetricsDao);
  }

  private void prepareUploadDeviceCheckpoint(IterationEntity iterationEntity) {
    when(mockBlobDao.exists(any())).thenReturn(true);
    when(mockBlobManager.generateDownloadCheckpointDescription(iterationEntity))
        .thenReturn(CHECKPOINT_DESCRIPTIONS_1);
    when(mockBlobManager.generateDownloadServerPlanDescription(iterationEntity))
        .thenReturn(SERVER_PLAN_DESCRIPTION_1);
    when(mockBlobManager.generateUploadClientCheckpointDescriptions(iterationEntity))
        .thenReturn(new BlobDescription[] {CHECKPOINT_DESCRIPTIONS_1, CHECKPOINT_DESCRIPTIONS_1});

    when(mockBlobDao.downloadAndDecompressIfNeeded(SERVER_PLAN_DESCRIPTION_1)).thenReturn(PLAN);
    when(mockBlobDao.downloadAndDecompressIfNeeded(CHECKPOINT_DESCRIPTIONS_1))
        .thenReturn(CHECKPOINT);
    when(mockTensorflowPlanSessionFactory.createPlanSession(any()))
        .thenReturn(mockTensorflowPlanSession);
    when(mockTensorflowPlanSessionFactory.createPhaseSessionV2(any()))
        .thenReturn(mockTensorflowPhaseSessionV2);
    when(mockTensorflowPhaseSessionV2.getClientCheckpoint(any()))
        .thenReturn(mockIntermediateResult);
    when(mockIntermediateResult.clientCheckpoint()).thenReturn(ByteString.copyFrom(new byte[] {9}));
    when(mockTensorflowPlanSession.createPhaseSession(any(), eq(Optional.empty())))
        .thenReturn(mockPhaseSession);
    doNothing().when(mockPhaseSession).accumulateIntermediateUpdate(any());
    doNothing().when(mockPhaseSession).applyAggregatedUpdates();
    when(mockPhaseSession.getClientCheckpoint(any()))
        .thenReturn(ByteString.copyFrom(new byte[] {9}));
  }
}
