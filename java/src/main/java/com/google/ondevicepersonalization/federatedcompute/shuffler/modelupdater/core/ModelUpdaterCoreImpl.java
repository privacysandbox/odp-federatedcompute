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

package com.google.ondevicepersonalization.federatedcompute.shuffler.modelupdater.core;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.fcp.aggregation.AggregationSession;
import com.google.fcp.plan.PhaseSession;
import com.google.fcp.plan.PhaseSessionV2;
import com.google.fcp.plan.PlanSession;
import com.google.fcp.tensorflow.AppFiles;
import com.google.internal.federated.plan.Plan;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.Constants;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.crypto.Payload;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobDescription;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.tensorflow.TensorflowPlanSessionFactory;
import com.google.ondevicepersonalization.federatedcompute.shuffler.modelupdater.core.message.ModelUpdaterMessage;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.scp.operator.cpio.cryptoclient.DecryptionKeyService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

/** The task scheduler implementation. */
@Service
public class ModelUpdaterCoreImpl implements ModelUpdaterCore {

  private static final Logger logger = LoggerFactory.getLogger(ModelUpdaterCoreImpl.class);
  private final InstantSource instantSource;
  private final BlobDao blobDao;
  private final TensorflowPlanSessionFactory tensorflowPlanSessionFactory;
  private final DecryptionKeyService decryptionKeyService;
  private final AppFiles appFiles;

  public ModelUpdaterCoreImpl(
      BlobDao blobDao,
      InstantSource instantSource,
      TensorflowPlanSessionFactory tensorflowPlanSessionFactory,
      DecryptionKeyService decryptionKeyService,
      AppFiles appFiles) {
    this.blobDao = blobDao;
    this.instantSource = instantSource;
    this.tensorflowPlanSessionFactory = tensorflowPlanSessionFactory;
    this.decryptionKeyService = decryptionKeyService;
    this.appFiles = appFiles;
  }

  public void process(ModelUpdaterMessage message) {
    try {
      MDC.put(Constants.ACTIVITY_ID, UUID.randomUUID().toString());
      processMessage(message);
    } finally {
      MDC.clear();
    }
  }

  private void processMessage(ModelUpdaterMessage message) {
    Instant startTime = instantSource.instant();
    try {
      MDC.put(Constants.REQUEST_ID, message.getRequestId());
      processMessageImpl(message);
    } catch (Exception e) {
      // TODO(b/329667567): Handle model updating failures. For now, nack() for all failures. This
      // will cause messages
      // to be retried and sent to the DLQ when they can no longer be processed.
      logger.atError().setCause(e).log("Failed processing model updater job.");
      throw e;
    } finally {
      Duration duration = Duration.between(startTime, instantSource.instant());
      logger.info("processing completed in {} second.", duration.getSeconds());
      MDC.remove(Constants.REQUEST_ID);
    }
  }

  private void processMessageImpl(ModelUpdaterMessage message) {
    List<byte[]> gradients =
        message.getIntermediateGradients().stream()
            .parallel()
            .map(
                gradient ->
                    getGradientFullPath(
                        message.getIntermediateGradientBucket(),
                        message.getIntermediateGradientPrefix(),
                        gradient))
            .map(blobDao::downloadAndDecompressIfNeeded)
            .map((payload) -> Payload.parseAndDecryptPayload(payload, decryptionKeyService))
            .collect(Collectors.toList());

    BlobDescription checkpointBlob =
        BlobDescription.builder()
            .host(message.getCheckpointBucket())
            .resourceObject(message.getCheckpointObject())
            .build();
    byte[] checkpoint = blobDao.downloadAndDecompressIfNeeded(checkpointBlob);

    BlobDescription planBlob =
        BlobDescription.builder()
            .host(message.getServerPlanBucket())
            .resourceObject(message.getServerPlanObject())
            .build();
    byte[] plan = blobDao.downloadAndDecompressIfNeeded(planBlob);

    finalize(plan, checkpoint, gradients, message);
  }

  private void finalize(
      byte[] planBytes, byte[] checkpoint, List<byte[]> gradients, ModelUpdaterMessage message) {
    Plan plan;
    try {
      plan = Plan.parseFrom(planBytes);
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalStateException("Failed to decode plan");
    }

    if (plan.getPhase(0).hasServerPhaseV2()) {
      finalizeV2(planBytes, checkpoint, gradients, message);
    } else {
      finalizeV1(planBytes, checkpoint, gradients, message);
    }
  }

  private void finalizeV1(
      byte[] plan, byte[] checkpoint, List<byte[]> gradients, ModelUpdaterMessage message) {
    PhaseSession phaseSession = null;
    try {
      phaseSession = createPhaseSession(ByteString.copyFrom(checkpoint), ByteString.copyFrom(plan));

      applyGradientsV1(gradients, phaseSession);

      if (!Strings.isNullOrEmpty(message.getNewCheckpointOutputBucket())) {
        uploadCheckpointV1(
            phaseSession,
            BlobDescription.builder()
                .host(message.getNewCheckpointOutputBucket())
                .resourceObject(message.getNewCheckpointOutputObject())
                .build());
      }

      uploadMetrics(
          phaseSession.getMetrics(),
          BlobDescription.builder()
              .host(message.getMetricsOutputBucket())
              .resourceObject(message.getMetricsOutputObject())
              .build());

      if (!Strings.isNullOrEmpty(message.getNewClientCheckpointOutputBucket())) {
        uploadClientCheckpointV1(
            phaseSession,
            BlobDescription.builder()
                .host(message.getNewClientCheckpointOutputBucket())
                .resourceObject(message.getNewClientCheckpointOutputObject())
                .build());
      }
    } finally {
      if (phaseSession != null) {
        phaseSession.close();
      }
    }
  }

  private void finalizeV2(
      byte[] plan, byte[] checkpoint, List<byte[]> gradients, ModelUpdaterMessage message) {
    byte[] aggregateResult = applyGradientsV2(gradients, plan);
    PhaseSessionV2 phaseSessionV2 =
        tensorflowPlanSessionFactory.createPhaseSessionV2(ByteString.copyFrom(plan));

    PhaseSessionV2.IntermediateResult intermediateResult =
        phaseSessionV2.getClientCheckpoint(ByteString.copyFrom(checkpoint));

    PhaseSessionV2.Result result =
        phaseSessionV2.getResult(
            ByteString.copyFrom(aggregateResult), intermediateResult.interemdiateState());

    if (!Strings.isNullOrEmpty(message.getNewCheckpointOutputBucket())) {
      BlobDescription checkpointBlob =
          BlobDescription.builder()
              .host(message.getNewCheckpointOutputBucket())
              .resourceObject(message.getNewCheckpointOutputObject())
              .build();
      try {
        blobDao.compressAndUpload(checkpointBlob, result.updatedServerState().toByteArray());
      } catch (IOException e) {
        throw new RuntimeException("Failed to upload checkpoint.", e);
      }
    }

    uploadMetrics(
        result.metrics(),
        BlobDescription.builder()
            .host(message.getMetricsOutputBucket())
            .resourceObject(message.getMetricsOutputObject())
            .build());

    if (!Strings.isNullOrEmpty(message.getNewClientCheckpointOutputBucket())) {
      ByteString newClientCheckpoint =
          tensorflowPlanSessionFactory
              .createPhaseSessionV2(ByteString.copyFrom(plan))
              .getClientCheckpoint(result.updatedServerState())
              .clientCheckpoint();
      BlobDescription newClientCheckpointBlob =
          BlobDescription.builder()
              .host(message.getNewClientCheckpointOutputBucket())
              .resourceObject(message.getNewClientCheckpointOutputObject())
              .build();
      try {
        blobDao.compressAndUpload(newClientCheckpointBlob, newClientCheckpoint.toByteArray());
      } catch (IOException e) {
        throw new RuntimeException("Failed to upload client checkpoint.", e);
      }
    }
  }

  private PhaseSession createPhaseSession(ByteString checkpoint, ByteString plan) {
    // Create tensorflow session
    PlanSession planSession = tensorflowPlanSessionFactory.createPlanSession(plan);
    PhaseSession phaseSession =
        planSession.createPhaseSession(Optional.of(checkpoint), Optional.of(appFiles));
    return phaseSession;
  }

  private void applyGradientsV1(List<byte[]> gradients, PhaseSession phaseSession) {
    // Apply update
    gradients.stream()
        .map(ByteString::copyFrom)
        .forEach(phaseSession::accumulateIntermediateUpdate);
    phaseSession.applyAggregatedUpdates();
  }

  private byte[] applyGradientsV2(List<byte[]> encryptedGradients, byte[] plan) {
    // Take the sqrt to enforce two layers of in-memory tree aggregation.
    int partitionSize = (int) Math.ceil(Math.sqrt(encryptedGradients.size()));

    // Layer 1
    List<List<byte[]>> partitionedGradients = Lists.partition(encryptedGradients, partitionSize);
    encryptedGradients =
        partitionedGradients.parallelStream()
            .map(gradients -> aggregateV2(gradients, plan))
            .collect(Collectors.toList());

    // Layer 2
    try (AggregationSession aggregationSession =
        tensorflowPlanSessionFactory.createAggregationSession(plan)) {
      aggregationSession.mergeWith(encryptedGradients.toArray(byte[][]::new));
      return aggregationSession.report();
    }
  }

  private byte[] aggregateV2(List<byte[]> encryptedGradients, byte[] plan) {
    try (AggregationSession aggregationSession =
        tensorflowPlanSessionFactory.createAggregationSession(plan)) {
      aggregationSession.mergeWith(encryptedGradients.toArray(byte[][]::new));
      return aggregationSession.serialize();
    }
  }

  /** Update checkpoint */
  private void uploadCheckpointV1(PhaseSession phaseSession, BlobDescription blobDescription) {
    byte[] serverModel = phaseSession.toCheckpoint().toByteArray();
    try {
      blobDao.compressAndUpload(blobDescription, serverModel);
    } catch (IOException e) {
      throw new RuntimeException("Failed to upload checkpoint.", e);
    }
  }

  /** Update checkpoint */
  private void uploadClientCheckpointV1(
      PhaseSession phaseSession, BlobDescription blobDescription) {
    byte[] clientModel = phaseSession.getClientCheckpoint(Optional.empty()).toByteArray();
    try {
      blobDao.compressAndUpload(blobDescription, clientModel);
    } catch (IOException e) {
      throw new RuntimeException("Failed to upload client checkpoint.", e);
    }
  }

  /** Upload metrics of updated checkpoint */
  private void uploadMetrics(Map<String, Double> metricsMap, BlobDescription blobDescription) {
    byte[] metrics =
        metricsMap.keySet().stream()
            .map(key -> "\"" + key + "\":" + metricsMap.get(key))
            .collect(Collectors.joining(", ", "{", "}"))
            .getBytes(StandardCharsets.UTF_8);
    try {
      blobDao.compressAndUpload(blobDescription, metrics);
    } catch (IOException e) {
      throw new RuntimeException("Failed to upload checkpoint.", e);
    }
  }

  private BlobDescription getGradientFullPath(String bucket, String prefix, String gradient) {
    return BlobDescription.builder().host(bucket).resourceObject(prefix + gradient).build();
  }
}
