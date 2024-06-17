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

package com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao;

import com.google.ondevicepersonalization.federatedcompute.shuffler.common.CompressionUtils.CompressionFormat;

/**
 * The Blob storage URL generater. Each interface generates (a) file location(s) based on the input.
 */
public interface BlobManager {
  /** Generates the file location where a device uploads the encrypted gradient. */
  public BlobDescription generateUploadGradientDescription(
      AssignmentEntity assignment, CompressionFormat format);

  /** Geneates the file locations where the Collector scans for device uploaded gradients. */
  public BlobDescription[] generateDownloadGradientDescriptions(IterationEntity iteration);

  /** Generates the file location where Aggregator uploads the aggregated gradient. */
  public BlobDescription generateUploadAggregatedGradientDescription(IterationEntity iteration);

  /** Generates the file location where to download the aggregated gradient. */
  public BlobDescription generateDownloadAggregatedGradientDescription(IterationEntity iteration);

  /** Generates the file locations where to publish the updated checkpoints. */
  public BlobDescription[] generateUploadCheckpointDescriptions(IterationEntity iteration);

  /** Generates the file locations where to publish the updated model metrics. */
  public BlobDescription[] generateUploadMetricsDescriptions(IterationEntity iteration);

  /** Generates the file locations where to publish the updated client checkpoints. */
  public BlobDescription[] generateUploadClientCheckpointDescriptions(IterationEntity iteration);

  /** Generates the file location where a device downloads the checkpoint. */
  public BlobDescription generateDownloadCheckpointDescription(AssignmentEntity assignment);

  /**
   * Generates download checkpoint file location for an assignment with checkpoint of a specific
   * iteration.
   */
  public BlobDescription generateDownloadCheckpointDescription(
      AssignmentId assignmentId, IterationId iterationId);

  /** Generates the file location where a server downloads the checkpoint. */
  public BlobDescription generateDownloadCheckpointDescription(IterationEntity iteration);

  /** Generates the file location where a device downloads the client training plan. */
  public BlobDescription generateDownloadDevicePlanDescription(AssignmentEntity assignment);

  /** Generates the file locations where to publish the client training plan. */
  public BlobDescription[] generateUploadDevicePlanDescriptions(TaskEntity task);

  /** Generates the file location where a server downloads the training plan. */
  public BlobDescription generateDownloadServerPlanDescription(IterationEntity iteration);

  /** Generates the file locations where to upload the server training plan. */
  public BlobDescription[] generateUploadServerPlanDescription(TaskEntity task);

  /**
   * Get the full path of device uploaded gradients.
   *
   * <p>A helper function to translate the folder from generateDownloadGradientDescriptions to full
   * path.
   *
   * @param iterationFolder the result folder of an iteration.
   * @param assignmentFolder the sub folder of the assignment
   * @return the full path of the gradient.
   */
  public BlobDescription getDeviceUploadedGradientFullPath(
      BlobDescription iterationFolder, String assignmentFolder);
}
