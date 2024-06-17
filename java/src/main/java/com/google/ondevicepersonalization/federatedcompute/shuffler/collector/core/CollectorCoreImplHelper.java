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

import static com.google.ondevicepersonalization.federatedcompute.shuffler.common.Constants.GRADIENT_FILE;

import com.google.ondevicepersonalization.federatedcompute.shuffler.aggregator.core.message.AggregatorMessage;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobDescription;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobManager;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.IterationEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.modelupdater.core.message.ModelUpdaterMessage;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** A helper class for CollectorCore. */
@Component
public class CollectorCoreImplHelper {
  @Autowired private BlobManager blobManager;

  /**
   * Creates a AggregatorMessage instance containing information required by the Aggregator
   * application.
   *
   * @return AggregatorMessage A message detailing resource locations and a request ID.
   */
  public AggregatorMessage createAggregatorMessage(
      IterationEntity iteration,
      Collection<String> contributions,
      Optional<String> batchId,
      boolean intermediate) {
    BlobDescription serverPlanBlob = blobManager.generateDownloadServerPlanDescription(iteration);

    List<String> gradients =
        contributions.stream().map(path -> path + "/" + GRADIENT_FILE).toList();
    BlobDescription gradientPath;
    if (intermediate) {
      gradientPath = blobManager.generateDownloadAggregatedGradientDescription(iteration);
    } else {
      gradientPath = blobManager.generateDownloadGradientDescriptions(iteration)[0];
    }

    BlobDescription aggregatedGradientBlob =
        blobManager.generateUploadAggregatedGradientDescription(iteration);
    String aggregatedGradientOutputSuffix =
        batchId.isPresent() ? batchId.get() + "/" + GRADIENT_FILE : GRADIENT_FILE;

    String requestId = iteration.getId().toString();

    return AggregatorMessage.builder()
        .serverPlanBucket(serverPlanBlob.getHost())
        .serverPlanObject(serverPlanBlob.getResourceObject())
        .gradientBucket(gradientPath.getHost())
        .gradientPrefix(gradientPath.getResourceObject())
        .gradients(gradients)
        .aggregatedGradientOutputBucket(aggregatedGradientBlob.getHost())
        .aggregatedGradientOutputObject(
            aggregatedGradientBlob.getResourceObject() + aggregatedGradientOutputSuffix)
        .requestId(requestId)
        .accumulateIntermediateUpdates(intermediate)
        .build();
  }

  /**
   * Creates a ModelUpdaterMessage instance containing information required by the ModelUpdater
   * application.
   *
   * @param iteration The IterationEntity containing data to construct the message.
   * @param intermediates Collection of intermediate gradients to apply during model updating
   * @return ModelUpdaterMessage A message detailing resource locations and a request ID.
   */
  public ModelUpdaterMessage createModelUpdaterMessage(
      IterationEntity iteration, Collection<String> intermediates) {
    String requestId = iteration.getId().toString();
    BlobDescription serverPlanBlob = blobManager.generateDownloadServerPlanDescription(iteration);
    BlobDescription checkpointBlob = blobManager.generateDownloadCheckpointDescription(iteration);
    BlobDescription metricsBlob = blobManager.generateUploadMetricsDescriptions(iteration)[0];

    List<String> gradients =
        intermediates.stream().map(path -> path + "/" + GRADIENT_FILE).toList();
    BlobDescription gradientPath =
        blobManager.generateDownloadAggregatedGradientDescription(iteration);

    // For evaluation task, skip setting new checkpoint related fields to avoid uploading
    // new checkpoints in the model updater.
    if (iteration.getIterationInfo().getTaskInfo().hasEvaluationInfo()) {
      return ModelUpdaterMessage.builder()
          .serverPlanBucket(serverPlanBlob.getHost())
          .serverPlanObject(serverPlanBlob.getResourceObject())
          .intermediateGradientBucket(gradientPath.getHost())
          .intermediateGradientPrefix(gradientPath.getResourceObject())
          .intermediateGradients(gradients)
          .checkpointBucket(checkpointBlob.getHost())
          .checkpointObject(checkpointBlob.getResourceObject())
          .metricsOutputBucket(metricsBlob.getHost())
          .metricsOutputObject(metricsBlob.getResourceObject())
          .requestId(requestId)
          .build();
    }

    BlobDescription newCheckpointBlob =
        blobManager.generateUploadCheckpointDescriptions(iteration)[0];
    BlobDescription newClientCheckpointBlob =
        blobManager.generateUploadClientCheckpointDescriptions(iteration)[0];

    return ModelUpdaterMessage.builder()
        .serverPlanBucket(serverPlanBlob.getHost())
        .serverPlanObject(serverPlanBlob.getResourceObject())
        .intermediateGradientBucket(gradientPath.getHost())
        .intermediateGradientPrefix(gradientPath.getResourceObject())
        .intermediateGradients(gradients)
        .checkpointBucket(checkpointBlob.getHost())
        .checkpointObject(checkpointBlob.getResourceObject())
        .newCheckpointOutputBucket(newCheckpointBlob.getHost())
        .newCheckpointOutputObject(newCheckpointBlob.getResourceObject())
        .newClientCheckpointOutputBucket(newClientCheckpointBlob.getHost())
        .newClientCheckpointOutputObject(newClientCheckpointBlob.getResourceObject())
        .metricsOutputBucket(metricsBlob.getHost())
        .metricsOutputObject(metricsBlob.getResourceObject())
        .requestId(requestId)
        .build();
  }
}
