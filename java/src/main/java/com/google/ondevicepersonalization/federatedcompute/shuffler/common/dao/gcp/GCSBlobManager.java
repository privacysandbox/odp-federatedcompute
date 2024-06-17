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

package com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.gcp;

import static com.google.ondevicepersonalization.federatedcompute.shuffler.common.Constants.GRADIENT_FILE;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.CompressionUtils.CompressionFormat;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AssignmentEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AssignmentId;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobDescription;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobManager;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.IterationEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.IterationId;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.Partitioner;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskEntity;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/** Google Cloud Storage Blob Manager */
@Component
public class GCSBlobManager implements BlobManager {
  private static final String CLIENT_PLAN_FILE = "client_only_plan";
  private static final String SERVER_PLAN_FILE = "server_phase";
  private static final String CHECKPOINT_FILE = "checkpoint";
  private static final String METRICS_FILE = "metrics";
  private static final String CLIENT_CHECKPOINT_FILE = "client_checkpoint";
  private static final String UPLOAD_CONTENT_TYPE_NAME = "content-type";
  private static final String UPLOAD_CONTENT_TYPE_VALUE = "application/octet-stream";
  private static final String UPLOAD_CONTENT_ENCODING_NAME = "content-encoding";
  private static final String UPLOAD_CONTENT_ENCODING_GZIP_VALUE = "gzip";
  private static final Map<String, String> EMPTY_HEADER = Collections.emptyMap();
  private static final Map<String, String> GRADIENT_HEADERS =
      Map.of(UPLOAD_CONTENT_TYPE_NAME, UPLOAD_CONTENT_TYPE_VALUE);
  private static final Map<String, String> GRADIENT_GZIP_HEADERS =
      Map.of(
          UPLOAD_CONTENT_TYPE_NAME,
          UPLOAD_CONTENT_TYPE_VALUE,
          UPLOAD_CONTENT_ENCODING_NAME,
          UPLOAD_CONTENT_ENCODING_GZIP_VALUE);

  private Storage storage;
  private GCSConfig config;
  private Partitioner partitioner;

  public GCSBlobManager(Storage storage, GCSConfig config, Partitioner partitioner) {
    this.storage = storage;
    this.config = config;
    this.partitioner = partitioner;
  }

  public BlobDescription generateUploadGradientDescription(
      AssignmentEntity assignment, CompressionFormat format) {
    Map<String, String> headers =
        format == CompressionFormat.GZIP ? GRADIENT_GZIP_HEADERS : GRADIENT_HEADERS;
    String bucketName =
        String.format(
            config.getGradientBucketTemplate(),
            partitioner.getDeviceGradientPartition(assignment.getSessionId()));
    String objectName =
        createDeviceUploadGradientObjectName(
            assignment.getPopulationName(),
            assignment.getTaskId(),
            assignment.getResultId(),
            assignment.getSessionId(),
            GRADIENT_FILE);
    return BlobDescription.builder()
        .host(bucketName)
        .resourceObject(objectName)
        .url(
            generateV4PutObjectSignedUrl(
                bucketName, objectName, config.getUploadGradientTokenDurationInSecond(), headers))
        .headers(headers)
        .build();
  }

  public BlobDescription[] generateDownloadGradientDescriptions(IterationEntity iteration) {
    int partition = partitioner.getDeviceGradientPartitionCount();
    BlobDescription[] descriptions = new BlobDescription[partition];
    String objectName =
        createGradientUploadedDir(
            iteration.getPopulationName(), iteration.getTaskId(), iteration.getResultId());
    for (int i = 0; i < partition; ++i) {
      String bucketName = String.format(config.getGradientBucketTemplate(), i);
      descriptions[i] =
          BlobDescription.builder()
              .host(bucketName)
              .resourceObject(objectName)
              .url(createGcsPath(bucketName, objectName))
              .headers(EMPTY_HEADER)
              .build();
    }
    return descriptions;
  }

  public BlobDescription generateUploadAggregatedGradientDescription(IterationEntity iteration) {
    return generateAggregatedGradientDescription(iteration);
  }

  public BlobDescription generateDownloadAggregatedGradientDescription(IterationEntity iteration) {
    return generateAggregatedGradientDescription(
        iteration.toBuilder().aggregationLevel(iteration.getAggregationLevel() - 1).build());
  }

  private BlobDescription generateAggregatedGradientDescription(IterationEntity iteration) {
    String bucketName =
        String.format(
            config.getAggregatedGradientBucketTemplate(),
            partitioner.getAggregatedResultPartition(
                iteration.getPopulationName(),
                iteration.getTaskId(),
                iteration.getResultId())); // current iteration's result id.
    String objectName =
        createPerIterationPathWithAggregationLevel(
            iteration.getPopulationName(),
            iteration.getTaskId(),
            iteration.getResultId(), // current iteration's result id.
            iteration.getAggregationLevel());
    return BlobDescription.builder()
        .host(bucketName)
        .resourceObject(objectName)
        .url(createGcsPath(bucketName, objectName))
        .headers(EMPTY_HEADER)
        .build();
  }

  public BlobDescription[] generateUploadCheckpointDescriptions(IterationEntity iteration) {
    int partition = partitioner.getCheckpointStoragePartitionCount();
    BlobDescription[] descriptions = new BlobDescription[partition];
    String objectName =
        createPerIterationPath(
            iteration.getPopulationName(),
            iteration.getTaskId(),
            iteration.getResultId(),
            CHECKPOINT_FILE);
    for (int i = 0; i < partition; ++i) {
      String bucketName = String.format(config.getModelBucketTemplate(), i);
      descriptions[i] =
          BlobDescription.builder()
              .host(bucketName)
              .resourceObject(objectName)
              .url(createGcsPath(bucketName, objectName))
              .headers(EMPTY_HEADER)
              .build();
    }

    return descriptions;
  }

  public BlobDescription[] generateUploadMetricsDescriptions(IterationEntity iteration) {
    int partitionCount = partitioner.getCheckpointStoragePartitionCount();
    BlobDescription[] descriptions = new BlobDescription[partitionCount];
    String objectName =
        createPerIterationPath(
            iteration.getPopulationName(),
            iteration.getTaskId(),
            iteration.getResultId(),
            METRICS_FILE);
    for (int i = 0; i < partitionCount; ++i) {
      String bucketName = String.format(config.getModelBucketTemplate(), i);
      descriptions[i] =
          BlobDescription.builder()
              .host(bucketName)
              .resourceObject(objectName)
              .url(createGcsPath(bucketName, objectName))
              .headers(EMPTY_HEADER)
              .build();
    }

    return descriptions;
  }

  public BlobDescription[] generateUploadClientCheckpointDescriptions(IterationEntity iteration) {
    int partition = partitioner.getCheckpointStoragePartitionCount();
    BlobDescription[] descriptions = new BlobDescription[partition];
    String objectName =
        createClientPerIterationPath(
            iteration.getPopulationName(),
            iteration.getTaskId(),
            iteration.getResultId(),
            CLIENT_CHECKPOINT_FILE);
    for (int i = 0; i < partition; ++i) {
      String bucketName = String.format(config.getModelBucketTemplate(), i);
      descriptions[i] =
          BlobDescription.builder()
              .host(bucketName)
              .resourceObject(objectName)
              .url(createGcsPath(bucketName, objectName))
              .headers(EMPTY_HEADER)
              .build();
    }

    return descriptions;
  }

  public BlobDescription generateDownloadCheckpointDescription(AssignmentEntity assignment) {
    return generateDownloadCheckpointDescription(
        assignment.getPopulationName(),
        assignment.getTaskId(),
        assignment.getBaseOnResultId(),
        assignment.getSessionId());
  }

  public BlobDescription generateDownloadCheckpointDescription(
      AssignmentId assignmentId, IterationId iterationId) {
    return generateDownloadCheckpointDescription(
        iterationId.getPopulationName(),
        iterationId.getTaskId(),
        iterationId.getIterationId(),
        assignmentId.getAssignmentId());
  }

  private BlobDescription generateDownloadCheckpointDescription(
      String populationName, long taskId, long iterationId, String assignmentId) {
    String bucketName =
        String.format(
            config.getModelBucketTemplate(),
            partitioner.getCheckpointStoagePartition(populationName, taskId, iterationId));
    String objectName =
        createClientPerIterationPath(populationName, taskId, iterationId, CLIENT_CHECKPOINT_FILE);
    return BlobDescription.builder()
        .host(bucketName)
        .resourceObject(objectName)
        .url(
            generateV4GetObjectSignedUrl(
                bucketName, objectName, config.getDownloadCheckpointTokenDurationInSecond()))
        .headers(EMPTY_HEADER)
        .build();
  }

  public BlobDescription generateDownloadCheckpointDescription(IterationEntity iteration) {
    return generateDownloadCheckpointDescription(iteration.getTrainingCheckpointIterationId());
  }

  private BlobDescription generateDownloadCheckpointDescription(IterationId iterationId) {
    String bucketName =
        String.format(
            config.getModelBucketTemplate(),
            partitioner.getCheckpointStoagePartition(
                iterationId.getPopulationName(),
                iterationId.getTaskId(),
                iterationId.getIterationId()));
    String objectName =
        createPerIterationPath(
            iterationId.getPopulationName(),
            iterationId.getTaskId(),
            iterationId.getIterationId(),
            CHECKPOINT_FILE);
    return BlobDescription.builder()
        .host(bucketName)
        .resourceObject(objectName)
        .url(createGcsPath(bucketName, objectName))
        .headers(EMPTY_HEADER)
        .build();
  }

  public BlobDescription generateDownloadDevicePlanDescription(AssignmentEntity assignment) {
    String bucketName =
        String.format(
            config.getModelBucketTemplate(),
            partitioner.getPlanStoagePartition(
                assignment.getPopulationName(),
                assignment.getTaskId(),
                assignment.getBaseOnResultId())); // last iteration.
    String objectName =
        createPerTaskPath(assignment.getPopulationName(), assignment.getTaskId(), CLIENT_PLAN_FILE);
    return BlobDescription.builder()
        .host(bucketName)
        .resourceObject(objectName)
        .url(
            generateV4GetObjectSignedUrl(
                bucketName, objectName, config.getDownloadPlanTokenDurationInSecond()))
        .headers(EMPTY_HEADER)
        .build();
  }

  public BlobDescription[] generateUploadDevicePlanDescriptions(TaskEntity task) {
    int partition = partitioner.getPlanStoragePartitionCount();
    BlobDescription[] descriptions = new BlobDescription[partition];
    String objectName =
        createPerTaskPath(task.getPopulationName(), task.getTaskId(), CLIENT_PLAN_FILE);
    for (int i = 0; i < partition; ++i) {
      String bucketName = String.format(config.getModelBucketTemplate(), i);
      descriptions[i] =
          BlobDescription.builder()
              .host(bucketName)
              .resourceObject(objectName)
              .url(createGcsPath(bucketName, objectName))
              .headers(EMPTY_HEADER)
              .build();
    }

    return descriptions;
  }

  public BlobDescription generateDownloadServerPlanDescription(IterationEntity iteration) {
    String bucketName =
        String.format(
            config.getModelBucketTemplate(),
            partitioner.getPlanStoagePartition(
                iteration.getPopulationName(),
                iteration.getTaskId(),
                iteration.getBaseOnResultId())); // last iteration.
    String objectName =
        createPerTaskPath(iteration.getPopulationName(), iteration.getTaskId(), SERVER_PLAN_FILE);
    return BlobDescription.builder()
        .host(bucketName)
        .resourceObject(objectName)
        .url(createGcsPath(bucketName, objectName))
        .headers(EMPTY_HEADER)
        .build();
  }

  public BlobDescription[] generateUploadServerPlanDescription(TaskEntity task) {

    int partition = partitioner.getPlanStoragePartitionCount();
    BlobDescription[] descriptions = new BlobDescription[partition];
    String objectName =
        createPerTaskPath(task.getPopulationName(), task.getTaskId(), SERVER_PLAN_FILE);
    for (int i = 0; i < partition; ++i) {
      String bucketName = String.format(config.getModelBucketTemplate(), i);
      descriptions[i] =
          BlobDescription.builder()
              .host(bucketName)
              .resourceObject(objectName)
              .url(createGcsPath(bucketName, objectName))
              .headers(EMPTY_HEADER)
              .build();
    }

    return descriptions;
  }

  public BlobDescription getDeviceUploadedGradientFullPath(
      BlobDescription iterationFolder, String assignmentFolder) {
    validateFolderPath(iterationFolder.getResourceObject());
    validateFolderPath(assignmentFolder);

    // remove the '/' at end.
    String sessionId = assignmentFolder.substring(0, assignmentFolder.length() - 1);
    String fullPath =
        createDeviceUploadGradientObjectNameFromIterationResultFolder(
            iterationFolder.getResourceObject(), sessionId, GRADIENT_FILE);
    return BlobDescription.builder()
        .host(iterationFolder.getHost())
        .resourceObject(fullPath)
        .url(createGcsPath(iterationFolder.getHost(), fullPath))
        .build();
  }

  private String generateV4PutObjectSignedUrl(
      String bucketName, String objectName, long durationInSecond, Map<String, String> headers) {

    // Define Resource
    BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, objectName)).build();

    URL url =
        storage.signUrl(
            blobInfo,
            durationInSecond,
            TimeUnit.SECONDS,
            Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
            Storage.SignUrlOption.withExtHeaders(headers),
            Storage.SignUrlOption.withV4Signature());
    return url.toString();
  }

  private String generateV4GetObjectSignedUrl(
      String bucketName, String objectName, long durationInSecond) throws StorageException {

    // Define resource
    BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, objectName)).build();

    URL url =
        storage.signUrl(
            blobInfo, durationInSecond, TimeUnit.SECONDS, Storage.SignUrlOption.withV4Signature());

    return url.toString();
  }

  private String createGradientUploadedDir(String populationName, long taskId, long iterationId) {
    // {population_name}/{task_id}/{iteration_id-or_result_id}/{server/device}/{server-or-worker-id}/{file_name}
    // NOTE: don't add "/" at beginning other wise, the url becomes double '/' as
    // gs://bucket//us/....
    return String.format("%s/%s/%s/%s", populationName, taskId, iterationId, "d/");
  }

  private static String createPerIterationPath(
      String populationName, long taskId, long iterationId, String fileName) {
    // {population_name}/{task_id}/{iteration_id-or_result_id}/{server/device}/{server-or-worker-id}/{file_name}
    // NOTE: don't add "/" at beginning other wise, the url becomes double '/' as
    // gs://bucket//us/....
    return String.format(
        "%s/%s/%s/%s/%s/%s", populationName, taskId, iterationId, "s", "0", fileName);
  }

  private static String createPerIterationPathWithAggregationLevel(
      String populationName, long taskId, long iterationId, long aggregationLevel) {
    // {population_name}/{task_id}/{iteration_id-or_result_id}/{server/device}/{server-or-worker-id}/{aggregation-level}/{batch_id}/{file_name}
    // NOTE: don't add "/" at beginning other wise, the url becomes double '/' as
    // gs://bucket//us/....
    return String.format(
        "%s/%s/%s/%s/%s/%s/", populationName, taskId, iterationId, "s", "0", aggregationLevel);
  }

  private static String createClientPerIterationPath(
      String populationName, long taskId, long iterationId, String fileName) {
    // {population_name}/{task_id}/{iteration_id-or_result_id}/{server/device}/{server-or-worker-id}/{file_name}
    // NOTE: don't add "/" at beginning other wise, the url becomes double '/' as
    // gs://bucket//us/....
    return String.format(
        "%s/%s/%s/%s/%s/%s", populationName, taskId, iterationId, "d", "0", fileName);
  }

  private static String createGcsPath(String bucketName, String objectName) {
    return "gs://" + bucketName + "/" + objectName;
  }

  private static String createPerTaskPath(String populationName, long taskId, String fileName) {
    // {population_name}/{task_id}/{iteration_id-or_result_id}/{server/device}/{server-or-worker-id}/{file_name}
    // NOTE: don't add "/" at beginning otherwise, the url becomes double '/' as
    // gs://bucket//us/....
    return String.format("%s/%s/%s/%s/%s/%s", populationName, taskId, 0, "s", "0", fileName);
  }

  private static String createDeviceUploadGradientObjectName(
      String populationName, long taskId, long iterationId, String sessionId, String fileName) {
    // {population_name}/{task_id}/{iteration_id-or_result_id}/{server/device}/{server-or-worker-id}/{file_name}
    // NOTE: don't add "/" at beginning otherwise, the url becomes double '/' as
    // gs://bucket//us/....
    String iterationFolder =
        String.format("%s/%s/%s/%s/", populationName, taskId, iterationId, "d");
    return createDeviceUploadGradientObjectNameFromIterationResultFolder(
        iterationFolder, sessionId, fileName);
  }

  private static String createDeviceUploadGradientObjectNameFromIterationResultFolder(
      String iterationResultFolder, String sessionId, String fileName) {
    validateFolderPath(iterationResultFolder);
    return String.format("%s%s/%s", iterationResultFolder, sessionId, fileName);
  }

  private static void validateFolderPath(String folder) {
    if (!folder.endsWith("/")) {
      throw new IllegalArgumentException(
          String.format("Invalid folder (%s). folder must ends with '/'.", folder));
    }
  }
}
