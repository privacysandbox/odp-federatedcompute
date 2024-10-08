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

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.fcp.aggregation.AggregationSession;
import com.google.fcp.plan.PhaseSession;
import com.google.fcp.plan.PlanSession;
import com.google.fcp.tensorflow.AppFiles;
import com.google.gson.Gson;
import com.google.internal.federated.plan.Plan;
import com.google.ondevicepersonalization.federatedcompute.shuffler.aggregator.core.message.AggregatorMessage;
import com.google.ondevicepersonalization.federatedcompute.shuffler.aggregator.core.message.AggregatorNotification;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.CompressionUtils;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.Constants;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.Exceptions;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.NonRetryableException;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.crypto.Payload;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.crypto.PublicKeyEncryptionService;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobDescription;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.messaging.HttpMessageSender;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.tensorflow.TensorflowPlanSessionFactory;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
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
  private HttpMessageSender httpMessageSender;

  public AggregatorCoreImpl(
      BlobDao blobDao,
      InstantSource instantSource,
      TensorflowPlanSessionFactory tensorflowPlanSessionFactory,
      DecryptionKeyService decryptionKeyService,
      PublicKeyEncryptionService publicKeyEncryptionService,
      AppFiles appFiles,
      HttpMessageSender httpMessageSender) {
    this.blobDao = blobDao;
    this.instantSource = instantSource;
    this.tensorflowPlanSessionFactory = tensorflowPlanSessionFactory;
    this.decryptionKeyService = decryptionKeyService;
    this.publicKeyEncryptionService = publicKeyEncryptionService;
    this.appFiles = appFiles;
    this.httpMessageSender = httpMessageSender;
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
      // Retryable errors will throw an exception triggering no acknowledgement for the message.
      logger.atError().setCause(e).log("Failed processing iteration aggregation.");

      // If a notification endpoint is provided,
      // non-retryable errors will send a notification and acknowledge the message.
      if (!Exceptions.isRetryableException(e)
          && !Strings.isNullOrEmpty(message.getNotificationEndpoint())) {
        handleNonRetryableException(e, message);
        return;
      }
      throw e;
    } finally {
      Duration duration = Duration.between(startTime, instantSource.instant());
      logger.info("processing completed in {} second.", duration.getSeconds());
      MDC.remove(Constants.REQUEST_ID);
    }
  }

  private void handleNonRetryableException(Exception e, AggregatorMessage message) {
    logger.info("Processing non-retryable exception {}", Throwables.getRootCause(e));
    AggregatorNotification.ErrorReason errorReason =
        AggregatorNotification.ErrorReason.UNKNOWN_ERROR;
    if (Exceptions.isAggregationException(e) || Exceptions.isTensorflowException(e)) {
      errorReason = AggregatorNotification.ErrorReason.AGGREGATION_ERROR;
    } else if (Exceptions.isNonRetryableKeyFetchException(e)) {
      errorReason = AggregatorNotification.ErrorReason.DECRYPTION_ERROR;
    }
    AggregatorNotification notification =
        AggregatorNotification.builder()
            .messages(
                List.of(
                    AggregatorNotification.Message.builder()
                        .attributes(
                            AggregatorNotification.Attributes.builder()
                                .requestId(message.getRequestId())
                                .status(AggregatorNotification.Status.ERROR)
                                .errorReason(errorReason)
                                .build())
                        .build()))
            .build();
    httpMessageSender.sendMessage(notification, message.getNotificationEndpoint());
  }

  private void processMessageImpl(AggregatorMessage message) {
    List<byte[]> encryptedGradients =
        message.getGradients().stream()
            .parallel()
            .map(
                gradient ->
                    getGradientFullPath(
                        message.getGradientBucket(), message.getGradientPrefix(), gradient))
            .map(
                (gradient) ->
                    blobDao
                        .downloadAndDecompressIfNeeded(gradient)
                        .orElseThrow(
                            () ->
                                new NonRetryableException(
                                    String.format(
                                        "Downloaded gradient for bucket %s and object %s is null or"
                                            + " does not exist",
                                        gradient.getHost(), gradient.getResourceObject()))))
            .map((payload) -> Payload.parseAndDecryptPayload(payload, decryptionKeyService))
            .collect(Collectors.toList());

    byte[] plan =
        blobDao
            .downloadAndDecompressIfNeeded(
                BlobDescription.builder()
                    .host(message.getServerPlanBucket())
                    .resourceObject(message.getServerPlanObject())
                    .build())
            .orElseThrow(
                () ->
                    new NonRetryableException(
                        String.format(
                            "Downloaded plan for bucket %s and object %s is null or"
                                + " does not exist",
                            message.getServerPlanBucket(), message.getServerPlanObject())));

    byte[] aggregatedResult =
        aggregate(encryptedGradients, plan, message.isAccumulateIntermediateUpdates());
    byte[] packagedAggregatedResult = encryptAndPackage(aggregatedResult);

    BlobDescription aggregatedResultLocation =
        BlobDescription.builder()
            .host(message.getAggregatedGradientOutputBucket())
            .resourceObject(message.getAggregatedGradientOutputObject())
            .build();
    try {
      blobDao.compressAndUpload(aggregatedResultLocation, packagedAggregatedResult);
    } catch (IOException e) {
      logger.atError().setCause(e).log("failed to compressAndUpload aggregated result.");
      throw new RuntimeException("Failed to compressAndUpload aggregated result", e);
    }

    if (!Strings.isNullOrEmpty(message.getNotificationEndpoint())) {
      try {
        AggregatorNotification notification =
            AggregatorNotification.builder()
                .messages(
                    List.of(
                        AggregatorNotification.Message.builder()
                            .attributes(
                                AggregatorNotification.Attributes.builder()
                                    .requestId(message.getRequestId())
                                    .status(AggregatorNotification.Status.OK)
                                    .build())
                            .build()))
                .build();
        httpMessageSender.sendMessage(notification, message.getNotificationEndpoint());
      } catch (Exception e) {
        logger
            .atError()
            .setCause(e)
            .log("Failed to send message to provided notification endpoint.");
        throw new RuntimeException("Failed to send message to provided notification endpoint.", e);
      }
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
      List<byte[]> encryptedGradients, byte[] planBytes, boolean accumulateIntermediateUpdates) {
    Plan plan;
    try {
      plan = Plan.parseFrom(planBytes);
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalStateException("Failed to decode plan");
    }
    // Take the sqrt to enforce two layers of in-memory tree aggregation.
    int partitionSize = (int) Math.ceil(Math.sqrt(encryptedGradients.size()));

    // Layer 1
    List<List<byte[]>> partitionedGradients = Lists.partition(encryptedGradients, partitionSize);
    if (plan.getPhase(0).hasServerPhaseV2()) {
      if (partitionedGradients.size() > 1) {
        boolean finalAccumulateIntermediateUpdates = accumulateIntermediateUpdates;
        encryptedGradients =
            partitionedGradients.parallelStream()
                .map(
                    gradients ->
                        aggregateV2(gradients, planBytes, finalAccumulateIntermediateUpdates))
                .collect(Collectors.toList());
        accumulateIntermediateUpdates = true;
      }

      // Layer 2
      return aggregateV2(encryptedGradients, planBytes, accumulateIntermediateUpdates);
    } else {
      if (partitionedGradients.size() > 1) {
        boolean finalAccumulateIntermediateUpdates = accumulateIntermediateUpdates;
        encryptedGradients =
            partitionedGradients.parallelStream()
                .map(
                    gradients ->
                        aggregateV1(gradients, planBytes, finalAccumulateIntermediateUpdates))
                .collect(Collectors.toList());
        accumulateIntermediateUpdates = true;
      }

      // Layer 2
      return aggregateV1(encryptedGradients, planBytes, accumulateIntermediateUpdates);
    }
  }

  private byte[] aggregateV2(
      List<byte[]> encryptedGradients, byte[] plan, boolean accumulateIntermediateUpdates) {
    try (AggregationSession aggregationSession =
        tensorflowPlanSessionFactory.createAggregationSession(plan)) {
      if (accumulateIntermediateUpdates) {
        aggregationSession.mergeWith(encryptedGradients.toArray(byte[][]::new));
      } else {
        aggregationSession.accumulate(encryptedGradients.toArray(byte[][]::new));
      }
      return aggregationSession.serialize();
    }
  }

  private byte[] aggregateV1(
      List<byte[]> encryptedGradients, byte[] plan, boolean accumulateIntermediateUpdates) {
    PhaseSession phaseSession = null;
    try {
      // Create tensorflow session
      PlanSession planSession =
          tensorflowPlanSessionFactory.createPlanSession(ByteString.copyFrom(plan));
      phaseSession = planSession.createPhaseSession(Optional.empty(), Optional.of(appFiles));

      // Perform aggregation with gradient
      if (accumulateIntermediateUpdates) {
        encryptedGradients.stream()
            .map(ByteString::copyFrom)
            .forEach(phaseSession::accumulateIntermediateUpdate);
      } else {
        encryptedGradients.stream()
            .map(ByteString::copyFrom)
            .forEach(phaseSession::accumulateClientUpdate);
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
