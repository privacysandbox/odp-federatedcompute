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

package com.google.ondevicepersonalization.federatedcompute.shuffler.taskscheduler.core;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.fcp.plan.PhaseSession;
import com.google.fcp.plan.PlanSession;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.internal.federated.plan.Plan;
import com.google.ondevicepersonalization.federatedcompute.proto.CheckPointSelector;
import com.google.ondevicepersonalization.federatedcompute.proto.EvaluationInfo;
import com.google.ondevicepersonalization.federatedcompute.proto.IterationInfo;
import com.google.ondevicepersonalization.federatedcompute.proto.TaskInfo;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.ProtoParser;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.RandomGenerator;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobDescription;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobManager;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.IterationEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.ModelMetricsDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.ModelMetricsEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskEntities;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.tensorflow.TensorflowPlanSessionFactory;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** A helper class for TaskSchedulerCore. */
@Component
public class TaskSchedulerCoreHelper {

  private static final Logger logger = LoggerFactory.getLogger(TaskSchedulerCoreHelper.class);
  @Autowired private TaskDao taskDao;
  @Autowired private ModelMetricsDao modelMetricsDao;
  @Autowired private RandomGenerator randomGenerator;
  @Autowired private BlobManager blobManager;
  @Autowired private BlobDao blobDao;
  @Autowired private TensorflowPlanSessionFactory tensorflowPlanSessionFactory;

  /**
   * Constructs an optional IterationInfo object based on the provided TaskInfo. This method is used
   * when creating a new iteration.
   *
   * @param taskInfo The TaskInfo object containing details of the task.
   * @return An Optional containing the built IterationInfo object.
   */
  public Optional<IterationInfo> buildIterationInfo(TaskInfo taskInfo) {
    IterationInfo.Builder iterationInfoBuilder = IterationInfo.newBuilder();

    iterationInfoBuilder.setTaskInfo(taskInfo);

    if (!taskInfo.hasEvaluationInfo()) {
      return Optional.of(iterationInfoBuilder.build());
    }

    EvaluationInfo evaluationInfo = taskInfo.getEvaluationInfo();
    String trainingPopulationName = evaluationInfo.getTrainingPopulationName();
    long trainingTaskId = evaluationInfo.getTrainingTaskId();

    ImmutableList.Builder<Long> builder = ImmutableList.builder();
    CheckPointSelector checkPointSelector = evaluationInfo.getCheckPointSelector();
    switch (checkPointSelector.getSelectorTypeCase()) {
      case ITERATION_SELECTOR:
        long iterationInterval = checkPointSelector.getIterationSelector().getSize();
        builder.addAll(
            taskDao.getIterationIdsPerEveryKIterationsSelector(
                trainingPopulationName, trainingTaskId, iterationInterval, 24));
        break;
      case DURATION_SELECTOR:
        long hoursInterval = checkPointSelector.getDurationSelector().getHours();
        builder.addAll(
            taskDao.getIterationIdsPerEveryKHoursSelector(
                trainingPopulationName, trainingTaskId, hoursInterval, 24));
        break;
      default:
        throw new UnsupportedOperationException("not supported yet");
    }

    ImmutableList<Long> iterationIds = builder.build();
    if (iterationIds.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(
        iterationInfoBuilder
            .setEvaluationTrainingIterationId(
                iterationIds.get(randomGenerator.nextInt(iterationIds.size())))
            .build());
  }

  /**
   * Determines if an active Task is ready to begin execution. This method is used when processing
   * first iteration of a task.
   *
   * @param task The TaskEntity object to evaluate for readiness
   * @return true if the task is ready to start, false otherwise
   */
  public boolean isActiveTaskReadyToStart(TaskEntity task) {
    if (task.getTotalIteration() == 0) {
      return false;
    }

    BlobDescription[] devicePlanDescriptions =
        blobManager.generateUploadDevicePlanDescriptions(task);
    BlobDescription[] serverPlanDescriptions =
        blobManager.generateUploadServerPlanDescription(task);

    if (!Strings.isNullOrEmpty(task.getInfo())) {
      TaskInfo taskInfo = ProtoParser.toProto(task.getInfo(), TaskInfo.getDefaultInstance());

      if (taskInfo.hasEvaluationInfo()) {
        return blobDao.checkExistsAndGzipContentIfNeeded(devicePlanDescriptions)
            && blobDao.checkExistsAndGzipContentIfNeeded(serverPlanDescriptions);
      }
    }

    IterationEntity baseIterationEntity = TaskEntities.createBaseIteration(task);
    BlobDescription[] initCheckpointDescriptions =
        blobManager.generateUploadCheckpointDescriptions(baseIterationEntity);
    return blobDao.checkExistsAndGzipContentIfNeeded(devicePlanDescriptions)
        && blobDao.checkExistsAndGzipContentIfNeeded(serverPlanDescriptions)
        && blobDao.checkExistsAndGzipContentIfNeeded(initCheckpointDescriptions);
  }

  /**
   * Generates a device checkpoint, and uploads them to a remote location.
   *
   * @param iteration The iteration associated with the checkpoint.
   * @throws IllegalArgumentException if any of the following conditions occur: * The server
   *     checkpoint or plan files do not exist for the provided iteration.
   * @throws IllegalStateException if any of the following conditions occur: * Failed to upload the
   *     client checkpoint.
   */
  public void generateAndUploadDeviceCheckpoint(IterationEntity iteration) {
    BlobDescription checkpointBlob = blobManager.generateDownloadCheckpointDescription(iteration);
    BlobDescription planBlob = blobManager.generateDownloadServerPlanDescription(iteration);
    if (!blobDao.exists(new BlobDescription[] {checkpointBlob, planBlob})) {
      throw new IllegalArgumentException(
          "The server checkpoint or plan do not exist for provided iteration.");
    }

    ByteString checkpoint =
        ByteString.copyFrom(blobDao.downloadAndDecompressIfNeeded(checkpointBlob));
    ByteString plan = ByteString.copyFrom(blobDao.downloadAndDecompressIfNeeded(planBlob));

    if (!createAndUploadClientCheckpoint(checkpoint, plan, iteration)) {
      throw new IllegalStateException("Failed to upload client checkpoint.");
    }
  }

  private boolean createAndUploadClientCheckpoint(
      ByteString checkpointBytes, ByteString planBytes, IterationEntity iteration) {
    Plan plan;
    try {
      plan = Plan.parseFrom(planBytes);
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalStateException("Failed to decode plan");
    }
    final ByteString clientCheckpoint;
    if (plan.getPhase(0).hasServerPhaseV2()) {
      clientCheckpoint =
          tensorflowPlanSessionFactory
              .createPhaseSessionV2(planBytes)
              .getClientCheckpoint(checkpointBytes)
              .clientCheckpoint();
    } else {
      PhaseSession phaseSession = null;
      try {
        phaseSession = createPhaseSession(checkpointBytes, planBytes);
        clientCheckpoint = phaseSession.getClientCheckpoint(Optional.empty());
      } finally {
        if (phaseSession != null) {
          phaseSession.close();
        }
      }
    }

    boolean allSucceeded =
        Arrays.stream(blobManager.generateUploadClientCheckpointDescriptions(iteration))
            .map(newModelBlob -> uploadNoThrow(newModelBlob, clientCheckpoint.toByteArray()))
            .allMatch(succeeded -> succeeded);

    return allSucceeded;
  }

  private PhaseSession createPhaseSession(ByteString checkpoint, ByteString plan) {
    // Create tensorflow session
    PlanSession planSession = tensorflowPlanSessionFactory.createPlanSession(plan);
    PhaseSession phaseSession =
        planSession.createPhaseSession(Optional.of(checkpoint), Optional.empty());
    return phaseSession;
  }

  private boolean uploadNoThrow(BlobDescription blob, byte[] content) {
    try {
      blobDao.compressAndUpload(blob, content);
      return true;
    } catch (IOException e) {
      logger.atError().setCause(e).log("failed to upload to %s\n", blob.getUrl());
      logger.error(e.toString());
      return false;
    }
  }

  /**
   * Prepares a new iteration entity and, if necessary for evaluation tasks, generates and uploads
   * device checkpoints.
   *
   * @param task The task associated with the iteration.
   * @param baseIterationId The base iteration ID for the new iteration.
   * @param iterationInfo Information about the iteration.
   * @return The newly prepared IterationEntity.
   */
  public IterationEntity prepareNewIterationAndUploadDeviceCheckpointIfNeeded(
      TaskEntity task, long baseIterationId, IterationInfo iterationInfo) {
    // Evaluation task depend on a training server checkpoint to generate the eval device
    // checkpoint.
    // Because the training iteration is selected during evaluation iteration creation, once
    // selected,
    // the device checkpoint needs to be uploaded.
    if (iterationInfo.getTaskInfo().hasEvaluationInfo()) {
      IterationEntity newIteration =
          TaskEntities.createBaseIteration(task).toBuilder()
              .iterationId(baseIterationId + 1)
              .baseIterationId(baseIterationId + 1)
              // baseOnResultId indicates the device checkpoint used for assignment distribution.
              // Evaluation iterations use the checkpoint generated within the same iteration.
              .baseOnResultId(baseIterationId + 1)
              .resultId(baseIterationId + 1)
              .info(ProtoParser.toJsonString(iterationInfo))
              .build();
      generateAndUploadDeviceCheckpoint(newIteration);
      return newIteration;
    }

    return TaskEntities.createBaseIteration(task).toBuilder()
        .iterationId(baseIterationId + 1)
        .baseIterationId(baseIterationId)
        // baseOnResultId indicates the device checkpoint used for assignment distribution.
        // Training iterations use the device checkpoint generated by previous iteration.
        .baseOnResultId(baseIterationId)
        .resultId(baseIterationId + 1)
        .info(ProtoParser.toJsonString(iterationInfo))
        .build();
  }

  /**
   * Determines if the process with applying state is completed. This is determined by checking the
   * existence of files associated with the iteration.
   *
   * @param iteration The IterationEntity representing the iteration to be checked.
   * @return True if all required blobs exist, otherwise false.
   */
  public boolean isApplyingDone(IterationEntity iteration) {
    BlobDescription[] metricsBlob = blobManager.generateUploadMetricsDescriptions(iteration);

    if (iteration.getIterationInfo().getTaskInfo().hasEvaluationInfo()) {
      return blobDao.exists(metricsBlob);
    }

    BlobDescription[] newCheckpointBlob =
        blobManager.generateUploadCheckpointDescriptions(iteration);
    BlobDescription[] newClientCheckpointBlob =
        blobManager.generateUploadClientCheckpointDescriptions(iteration);
    return blobDao.exists(newCheckpointBlob)
        && blobDao.exists(newClientCheckpointBlob)
        && blobDao.exists(metricsBlob);
  }

  public boolean parseMetricsAndUpsert(IterationEntity iteration) {
    BlobDescription metricsDescription =
        blobManager.generateUploadMetricsDescriptions(iteration)[0];
    if (!blobDao.exists(new BlobDescription[] {metricsDescription})) {
      logger.atInfo().log("Metrics blob does not exist for iteration {}", iteration.getId());
      return false;
    }

    String metricsStr =
        new String(
            blobDao.downloadAndDecompressIfNeeded(metricsDescription), StandardCharsets.UTF_8);
    Gson gson = new Gson();
    Type type = new TypeToken<Map<String, Double>>() {}.getType();
    Map<String, Double> metricsMap = gson.fromJson(metricsStr, type);
    List<ModelMetricsEntity> modelMetricsList =
        metricsMap.entrySet().stream()
            .map(
                entry ->
                    ModelMetricsEntity.builder()
                        .populationName(iteration.getPopulationName())
                        .taskId(iteration.getTaskId())
                        .iterationId(iteration.getIterationId())
                        .metricName(entry.getKey())
                        .metricValue(entry.getValue())
                        .build())
            .collect(Collectors.toList());

    return modelMetricsDao.upsertModelMetrics(modelMetricsList);
  }
}
