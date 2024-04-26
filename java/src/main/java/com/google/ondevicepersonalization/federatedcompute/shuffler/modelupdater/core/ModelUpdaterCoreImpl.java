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
import com.google.fcp.plan.PhaseSession;
import com.google.fcp.plan.PlanSession;
import com.google.fcp.tensorflow.AppFiles;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.Constants;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.crypto.Payload;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobDescription;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.tensorflow.TensorflowPlanSessionFactory;
import com.google.ondevicepersonalization.federatedcompute.shuffler.modelupdater.core.message.ModelUpdaterMessage;
import com.google.protobuf.ByteString;
import com.google.scp.operator.cpio.cryptoclient.DecryptionKeyService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
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
    BlobDescription aggregatedGradientBlob =
        BlobDescription.builder()
            .host(message.getAggregatedGradientBucket())
            .resourceObject(message.getAggregatedGradientObject())
            .build();
    ByteString gradient =
        ByteString.copyFrom(
            Payload.parseAndDecryptPayload(
                blobDao.download(aggregatedGradientBlob), decryptionKeyService));

    BlobDescription checkpointBlob =
        BlobDescription.builder()
            .host(message.getCheckpointBucket())
            .resourceObject(message.getCheckpointObject())
            .build();
    ByteString checkpoint = ByteString.copyFrom(blobDao.download(checkpointBlob));

    BlobDescription planBlob =
        BlobDescription.builder()
            .host(message.getServerPlanBucket())
            .resourceObject(message.getServerPlanObject())
            .build();
    ByteString plan = ByteString.copyFrom(blobDao.download(planBlob));

    PhaseSession phaseSession = null;
    try {
      phaseSession = createPhaseSession(checkpoint, plan);

      applyGradient(gradient, phaseSession);

      if (!Strings.isNullOrEmpty(message.getNewCheckpointOutputBucket())) {
        uploadCheckpoint(
            phaseSession,
            BlobDescription.builder()
                .host(message.getNewCheckpointOutputBucket())
                .resourceObject(message.getNewCheckpointOutputObject())
                .build());
      }

      uploadMetrics(
          phaseSession,
          BlobDescription.builder()
              .host(message.getMetricsOutputBucket())
              .resourceObject(message.getMetricsOutputObject())
              .build(),
          message.getRequestId());

      if (!Strings.isNullOrEmpty(message.getNewClientCheckpointOutputBucket())) {
        uploadClientCheckpoint(
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

  private PhaseSession createPhaseSession(ByteString checkpoint, ByteString plan) {
    // Create tensorflow session
    PlanSession planSession = tensorflowPlanSessionFactory.createPlanSession(plan);
    PhaseSession phaseSession =
        planSession.createPhaseSession(Optional.of(checkpoint), Optional.of(appFiles));
    return phaseSession;
  }

  private void applyGradient(ByteString gradient, PhaseSession phaseSession) {
    // Apply update
    phaseSession.accumulateIntermediateUpdate(gradient);
    phaseSession.applyAggregatedUpdates();
  }

  /** Update checkpoint */
  private void uploadCheckpoint(PhaseSession phaseSession, BlobDescription blobDescription) {
    byte[] serverModel = phaseSession.toCheckpoint().toByteArray();
    try {
      blobDao.upload(blobDescription, serverModel);
    } catch (IOException e) {
      throw new RuntimeException("Failed to upload checkpoint.", e);
    }
  }

  /** Update checkpoint */
  private void uploadClientCheckpoint(PhaseSession phaseSession, BlobDescription blobDescription) {
    byte[] clientModel = phaseSession.getClientCheckpoint(Optional.empty()).toByteArray();
    try {
      blobDao.upload(blobDescription, clientModel);
    } catch (IOException e) {
      throw new RuntimeException("Failed to upload client checkpoint.", e);
    }
  }

  /** Upload metrics of updated checkpoint */
  private void uploadMetrics(
      PhaseSession phaseSession, BlobDescription blobDescription, String requestId) {
    Map<String, Double> metricsMap = phaseSession.getMetrics();
    byte[] metrics =
        metricsMap.keySet().stream()
            .map(key -> "\"" + key + "\":" + metricsMap.get(key))
            .collect(Collectors.joining(", ", "{", "}"))
            .getBytes(StandardCharsets.UTF_8);
    try {
      blobDao.upload(blobDescription, metrics);
    } catch (IOException e) {
      throw new RuntimeException("Failed to upload checkpoint.", e);
    }
  }
}
