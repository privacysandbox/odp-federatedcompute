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

package com.google.ondevicepersonalization.federatedcompute.shuffler.aggregator.core;

import com.google.fcp.plan.PhaseSession;
import com.google.fcp.plan.PlanSession;
import com.google.fcp.tensorflow.AppFiles;
import com.google.gson.Gson;
import com.google.ondevicepersonalization.federatedcompute.shuffler.aggregator.core.message.AggregatorMessage;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.CompressionUtils;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.Constants;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.crypto.Payload;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.crypto.PublicKeyEncryptionService;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobDescription;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.tensorflow.TensorflowPlanSessionFactory;
import com.google.protobuf.ByteString;
import com.google.scp.operator.cpio.cryptoclient.DecryptionKeyService;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

/** The aggregator core implementation. */
@Service
public class AggregatorCoreImpl implements AggregatorCore {

  private static final Logger logger = LoggerFactory.getLogger(AggregatorCoreImpl.class);

  private BlobDao blobDao;
  private InstantSource instantSource;

  private TensorflowPlanSessionFactory tensorflowPlanSessionFactory;
  private DecryptionKeyService decryptionKeyService;
  private PublicKeyEncryptionService publicKeyEncryptionService;
  private AppFiles appFiles;

  public AggregatorCoreImpl(
      BlobDao blobDao,
      InstantSource instantSource,
      TensorflowPlanSessionFactory tensorflowPlanSessionFactory,
      DecryptionKeyService decryptionKeyService,
      PublicKeyEncryptionService publicKeyEncryptionService,
      AppFiles appFiles) {
    this.blobDao = blobDao;
    this.instantSource = instantSource;
    this.tensorflowPlanSessionFactory = tensorflowPlanSessionFactory;
    this.decryptionKeyService = decryptionKeyService;
    this.publicKeyEncryptionService = publicKeyEncryptionService;
    this.appFiles = appFiles;
  }

  public void process(AggregatorMessage message) {
    try {
      MDC.put(Constants.ACTIVITY_ID, UUID.randomUUID().toString());
      processMessage(message);
    } finally {
      MDC.clear();
    }
  }

  private void processMessage(AggregatorMessage message) {
    Instant startTime = instantSource.instant();
    try {
      MDC.put(Constants.REQUEST_ID, message.getRequestId());
      processMessageImpl(message);
    } catch (Exception e) {
      // TODO(b/329667567): Handle aggregation failures. For now, nack() for all failures. This will
      // cause messages to be retried and sent to the DLQ when they can no longer be processed.
      logger.atError().setCause(e).log("Failed processing iteration aggregation.");
      throw e;
    } finally {
      Duration duration = Duration.between(startTime, instantSource.instant());
      logger.info("processing completed in {} second.", duration.getSeconds());
      MDC.remove(Constants.REQUEST_ID);
    }
  }

  private void processMessageImpl(AggregatorMessage message) {
    List<ByteString> encryptedGradients =
        message.getGradients().stream()
            .parallel()
            .map(
                gradient ->
                    getGradientFullPath(
                        message.getGradientBucket(), message.getGradientPrefix(), gradient))
            .map(blobDao::download)
            .map((payload) -> Payload.parseAndDecryptPayload(payload, decryptionKeyService))
            .map(ByteString::copyFrom)
            .collect(Collectors.toList());

    ByteString plan =
        ByteString.copyFrom(
            blobDao.download(
                BlobDescription.builder()
                    .host(message.getServerPlanBucket())
                    .resourceObject(message.getServerPlanObject())
                    .build()));

    // TODO(b/295060730): Support parallel aggregation. This is currently blocked by limited tmpfs
    // volume size on supported TEEs.
    byte[] aggregatedResult =
        aggregate(encryptedGradients, plan, message.isAccumulateIntermediateUpdates());
    byte[] packagedAggregatedResult = encryptAndPackage(aggregatedResult);

    BlobDescription aggregatedResultLocation =
        BlobDescription.builder()
            .host(message.getAggregatedGradientOutputBucket())
            .resourceObject(message.getAggregatedGradientOutputObject())
            .build();
    try {
      blobDao.upload(aggregatedResultLocation, packagedAggregatedResult);
    } catch (IOException e) {
      logger.atError().setCause(e).log("failed to upload aggregated result.");
      throw new RuntimeException("Failed to upload aggregated result", e);
    }
  }

  private byte[] encryptAndPackage(byte[] data) {
    Payload payload =
        publicKeyEncryptionService.encryptPayload(
            CompressionUtils.compressWithGzip(data), new byte[0]);
    Gson gson = new Gson();
    return gson.toJson(payload).getBytes();
  }

  private byte[] aggregate(
      List<ByteString> encryptedGradients, ByteString plan, boolean accumulateIntermediateUpdates) {
    PhaseSession phaseSession = null;
    try {
      // Create tensorflow session
      PlanSession planSession = tensorflowPlanSessionFactory.createPlanSession(plan);
      phaseSession = planSession.createPhaseSession(Optional.empty(), Optional.of(appFiles));

      // Perform aggregation with gradient
      if (accumulateIntermediateUpdates) {
        encryptedGradients.forEach(phaseSession::accumulateIntermediateUpdate);
      } else {
        encryptedGradients.forEach(phaseSession::accumulateClientUpdate);
      }

      // Finalize aggregation
      ByteString result = phaseSession.toIntermediateUpdate();
      return result.toByteArray();
    } finally {
      if (phaseSession != null) {
        phaseSession.close();
      }
    }
  }

  private BlobDescription getGradientFullPath(String bucket, String prefix, String gradient) {
    return BlobDescription.builder().host(bucket).resourceObject(prefix + gradient).build();
  }
}
