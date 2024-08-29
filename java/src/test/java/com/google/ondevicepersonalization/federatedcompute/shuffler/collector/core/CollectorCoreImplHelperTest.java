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

package com.google.ondevicepersonalization.federatedcompute.shuffler.collector.core;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;

import com.google.ondevicepersonalization.federatedcompute.proto.EvaluationInfo;
import com.google.ondevicepersonalization.federatedcompute.proto.IterationInfo;
import com.google.ondevicepersonalization.federatedcompute.proto.TaskInfo;
import com.google.ondevicepersonalization.federatedcompute.proto.TrainingInfo;
import com.google.ondevicepersonalization.federatedcompute.shuffler.aggregator.core.message.AggregatorMessage;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.ProtoParser;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobDescription;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobManager;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.IterationEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.modelupdater.core.message.ModelUpdaterMessage;
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
public class CollectorCoreImplHelperTest {

  private @Mock BlobManager mockBlobManager;
  @InjectMocks CollectorCoreImplHelper collectorCoreImplHelper;

  private static final TaskInfo TRAINING_TASK_INFO =
      TaskInfo.newBuilder()
          .setTrafficWeight(1)
          .setTrainingInfo(TrainingInfo.getDefaultInstance())
          .build();
  private static final IterationInfo TRAINING_ITERATION_INFO =
      IterationInfo.newBuilder().setTaskInfo(TRAINING_TASK_INFO).build();
  private static final String TRAINING_ITERATION_INFO_STRING =
      ProtoParser.toJsonString(TRAINING_ITERATION_INFO);
  private static final TaskInfo EVALUATION_TASK_INFO =
      TaskInfo.newBuilder()
          .setTrafficWeight(1)
          .setEvaluationInfo(EvaluationInfo.getDefaultInstance())
          .build();
  private static final IterationInfo EVALUATION_ITERATION_INFO =
      IterationInfo.newBuilder().setTaskInfo(EVALUATION_TASK_INFO).build();
  private static final String EVALUATION_ITERATION_INFO_STRING =
      ProtoParser.toJsonString(EVALUATION_ITERATION_INFO);

  private static final IterationEntity TRAINING_ITERATION =
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
          .info(TRAINING_ITERATION_INFO_STRING)
          .build();

  private static final IterationEntity EVALUATION_ITERATION =
      TRAINING_ITERATION.toBuilder().info(EVALUATION_ITERATION_INFO_STRING).build();

  private static final BlobDescription SERVER_PLAN_DESCRIPTIONS =
      BlobDescription.builder().host("test-m-1").resourceObject("us/35/17/s/0/plan").build();
  private static final BlobDescription SERVER_CHECKPOINT =
      BlobDescription.builder().host("test-m-1").resourceObject("us/35/17/s/0/checkpoint").build();
  private static final BlobDescription GRADIENT =
      BlobDescription.builder().host("test-g-1").resourceObject("us/35/17/s/0/").build();
  private static final BlobDescription CLIENT_GRADIENT =
      BlobDescription.builder().host("test-g-1").resourceObject("us/35/17/s/0/").build();
  private static final BlobDescription AGGREGATION_GRADIENT =
      BlobDescription.builder().host("test-a-1").resourceObject("us/35/17/s/0/").build();
  private static final BlobDescription METRICS_DESCRIPTIONS =
      BlobDescription.builder().host("test-g-1").resourceObject("us/35/17/s/0/metrics").build();
  private static final BlobDescription UPLOAD_SERVER_CHECKPOINT =
      BlobDescription.builder().host("test-m-1").resourceObject("us/35/18/s/0/checkpoint").build();
  private static final BlobDescription UPLOAD_CLIENT_CHECKPOINT =
      BlobDescription.builder()
          .host("test-g-1")
          .resourceObject("us/35/17/s/0/client_checkpoint")
          .build();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(mockBlobManager.generateDownloadServerPlanDescription(any()))
        .thenReturn(SERVER_PLAN_DESCRIPTIONS);
    when(mockBlobManager.generateDownloadCheckpointDescription(isA(IterationEntity.class)))
        .thenReturn(SERVER_CHECKPOINT);
    when(mockBlobManager.generateDownloadAggregatedGradientDescription(any())).thenReturn(GRADIENT);
    when(mockBlobManager.generateUploadMetricsDescriptions(any()))
        .thenReturn(
            new BlobDescription[] {
              METRICS_DESCRIPTIONS,
            });
    when(mockBlobManager.generateUploadCheckpointDescriptions(isA(IterationEntity.class)))
        .thenReturn(
            new BlobDescription[] {
              UPLOAD_SERVER_CHECKPOINT,
            });
    when(mockBlobManager.generateUploadClientCheckpointDescriptions(isA(IterationEntity.class)))
        .thenReturn(
            new BlobDescription[] {
              UPLOAD_CLIENT_CHECKPOINT,
            });
    when(mockBlobManager.generateUploadAggregatedGradientDescription(any()))
        .thenReturn(AGGREGATION_GRADIENT);
    when(mockBlobManager.generateDownloadGradientDescriptions(any()))
        .thenReturn(new BlobDescription[] {CLIENT_GRADIENT});
  }

  @Test
  public void createAggregatorMessage_trainingIteration_success() {
    // act
    AggregatorMessage aggregatorMessage =
        collectorCoreImplHelper.createAggregatorMessage(
            TRAINING_ITERATION, List.of("1", "2"), Optional.of("batch"), false, "endpoint");

    // assert
    assertThat(aggregatorMessage)
        .isEqualTo(
            AggregatorMessage.builder()
                .serverPlanBucket(SERVER_PLAN_DESCRIPTIONS.getHost())
                .serverPlanObject(SERVER_PLAN_DESCRIPTIONS.getResourceObject())
                .gradientBucket(CLIENT_GRADIENT.getHost())
                .gradientPrefix(CLIENT_GRADIENT.getResourceObject())
                .gradients(List.of("1/gradient", "2/gradient"))
                .aggregatedGradientOutputBucket(AGGREGATION_GRADIENT.getHost())
                .aggregatedGradientOutputObject(
                    AGGREGATION_GRADIENT.getResourceObject() + "batch/gradient")
                .requestId(TRAINING_ITERATION.getId().toString() + "_batch")
                .accumulateIntermediateUpdates(false)
                .notificationEndpoint("endpoint")
                .build());
  }

  @Test
  public void createAggregatorMessage_trainingIterationIntermediate_success() {
    // act
    AggregatorMessage aggregatorMessage =
        collectorCoreImplHelper.createAggregatorMessage(
            TRAINING_ITERATION, List.of("1", "2"), Optional.empty(), true, "endpoint");

    // assert
    assertThat(aggregatorMessage)
        .isEqualTo(
            AggregatorMessage.builder()
                .serverPlanBucket(SERVER_PLAN_DESCRIPTIONS.getHost())
                .serverPlanObject(SERVER_PLAN_DESCRIPTIONS.getResourceObject())
                .gradientBucket(GRADIENT.getHost())
                .gradientPrefix(GRADIENT.getResourceObject())
                .gradients(List.of("1/gradient", "2/gradient"))
                .aggregatedGradientOutputBucket(AGGREGATION_GRADIENT.getHost())
                .aggregatedGradientOutputObject(
                    AGGREGATION_GRADIENT.getResourceObject() + "gradient")
                .accumulateIntermediateUpdates(true)
                .requestId(TRAINING_ITERATION.getId().toString())
                .notificationEndpoint("endpoint")
                .build());
  }

  @Test
  public void createModelUpdaterMessage_trainingIteration_success() {
    // act
    ModelUpdaterMessage modelUpdaterMessage =
        collectorCoreImplHelper.createModelUpdaterMessage(TRAINING_ITERATION, List.of("1", "2"));

    // assert
    assertThat(modelUpdaterMessage)
        .isEqualTo(
            ModelUpdaterMessage.builder()
                .serverPlanBucket(SERVER_PLAN_DESCRIPTIONS.getHost())
                .serverPlanObject(SERVER_PLAN_DESCRIPTIONS.getResourceObject())
                .intermediateGradientBucket(GRADIENT.getHost())
                .intermediateGradientPrefix(GRADIENT.getResourceObject())
                .intermediateGradients(List.of("1/gradient", "2/gradient"))
                .checkpointBucket(SERVER_CHECKPOINT.getHost())
                .checkpointObject(SERVER_CHECKPOINT.getResourceObject())
                .newCheckpointOutputBucket(UPLOAD_SERVER_CHECKPOINT.getHost())
                .newCheckpointOutputObject(UPLOAD_SERVER_CHECKPOINT.getResourceObject())
                .newClientCheckpointOutputBucket(UPLOAD_CLIENT_CHECKPOINT.getHost())
                .newClientCheckpointOutputObject(UPLOAD_CLIENT_CHECKPOINT.getResourceObject())
                .metricsOutputBucket(METRICS_DESCRIPTIONS.getHost())
                .metricsOutputObject(METRICS_DESCRIPTIONS.getResourceObject())
                .requestId(TRAINING_ITERATION.getId().toString())
                .build());
  }

  @Test
  public void createModelUpdaterMessage_evaluationIteration_success() {
    // act
    ModelUpdaterMessage modelUpdaterMessage =
        collectorCoreImplHelper.createModelUpdaterMessage(EVALUATION_ITERATION, List.of("1", "2"));

    // assert
    assertThat(modelUpdaterMessage)
        .isEqualTo(
            ModelUpdaterMessage.builder()
                .serverPlanBucket(SERVER_PLAN_DESCRIPTIONS.getHost())
                .serverPlanObject(SERVER_PLAN_DESCRIPTIONS.getResourceObject())
                .intermediateGradientBucket(GRADIENT.getHost())
                .intermediateGradientPrefix(GRADIENT.getResourceObject())
                .intermediateGradients(List.of("1/gradient", "2/gradient"))
                .checkpointBucket(SERVER_CHECKPOINT.getHost())
                .checkpointObject(SERVER_CHECKPOINT.getResourceObject())
                .metricsOutputBucket(METRICS_DESCRIPTIONS.getHost())
                .metricsOutputObject(METRICS_DESCRIPTIONS.getResourceObject())
                .requestId(TRAINING_ITERATION.getId().toString())
                .build());
    assertThat(modelUpdaterMessage.getNewCheckpointOutputBucket()).isNull();
    assertThat(modelUpdaterMessage.getNewCheckpointOutputObject()).isNull();
    assertThat(modelUpdaterMessage.getNewClientCheckpointOutputBucket()).isNull();
    assertThat(modelUpdaterMessage.getNewClientCheckpointOutputObject()).isNull();
  }
}
