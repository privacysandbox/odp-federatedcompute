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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.CompressionUtils.CompressionFormat;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AssignmentEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.AssignmentId;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobDescription;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.IterationEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.IterationId;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.Partitioner;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskEntity;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.google.testing.junit.testparameterinjector.TestParameters;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(TestParameterInjector.class)
public final class GCSBlobManagerTest {
  // default traing settings
  private static final String POPULATION_NAME = "us";
  private static final long TASK_ID = 35;
  private static final long ITERATION_ID = 15;
  private static final long ATTEMPT_ID = 2;
  private static final long BASE_ITERATION_ID = 11;
  private static final long BASE_ON_RESULT_ID = 13;
  private static final long RESULT_ID = 17;
  private static final String SESSION_ID = "xyz";
  private static final long AGGREGATION_LEVEL = 1;

  // default gcs setting
  private static final String GRADIENT_BUCKET = "gra-%d";
  private static final String AGGREGATED_GRADIENT_BUCKET = "agg-%d";
  private static final String MODEL_BUCKET = "mdl-%d";
  private static final long DOWNLOAD_PLAN_DURATION = 5;
  private static final long DOWNLOAD_CHECKPOINT_DURATION = 60;
  private static final long UPLOAD_CHECKPOINT_DURATION = 120;

  private final HashMap<String, String> putHeaders;
  private final HashMap<String, String> emptyHeaders;

  private static final TaskEntity TASK =
      TaskEntity.builder().populationName(POPULATION_NAME).taskId(TASK_ID).build();
  private static final IterationEntity ITERATION =
      IterationEntity.builder()
          .populationName(POPULATION_NAME)
          .taskId(TASK_ID)
          .iterationId(ITERATION_ID)
          .attemptId(ATTEMPT_ID)
          .baseOnResultId(BASE_ON_RESULT_ID)
          .baseIterationId(BASE_ITERATION_ID)
          .resultId(RESULT_ID)
          .aggregationLevel(AGGREGATION_LEVEL)
          .build();

  private static final AssignmentEntity ASSIGNMENT =
      AssignmentEntity.builder()
          .populationName(POPULATION_NAME)
          .taskId(TASK_ID)
          .iterationId(ITERATION_ID)
          .attemptId(ATTEMPT_ID)
          .baseOnResultId(BASE_ON_RESULT_ID)
          .baseIterationId(BASE_ITERATION_ID)
          .resultId(RESULT_ID)
          .sessionId(SESSION_ID)
          .build();

  private static final GCSConfig CONFIG =
      GCSConfig.builder()
          .gradientBucketTemplate(GRADIENT_BUCKET)
          .aggregatedGradientBucketTemplate(AGGREGATED_GRADIENT_BUCKET)
          .modelBucketTemplate(MODEL_BUCKET)
          .downloadPlanTokenDurationInSecond(DOWNLOAD_PLAN_DURATION)
          .downloadCheckpointTokenDurationInSecond(DOWNLOAD_CHECKPOINT_DURATION)
          .uploadGradientTokenDurationInSecond(UPLOAD_CHECKPOINT_DURATION)
          .build();

  private GCSBlobManager manager;
  @Mock Storage mockStorage;
  @Mock Partitioner mockPartitioner;
  @Mock IterationEntity mockIterationEntity;

  public GCSBlobManagerTest() {
    putHeaders = new HashMap<String, String>();
    putHeaders.put("content-type", "application/octet-stream");
    emptyHeaders = new HashMap<String, String>();
  }

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    manager = new GCSBlobManager(mockStorage, CONFIG, mockPartitioner);
  }

  @Test
  @TestParameters("{format: GZIP}")
  @TestParameters("{format: null}")
  public void generateUploadGradientDescription_success(CompressionFormat format)
      throws MalformedURLException {
    // arrange
    Map<String, String> headers =
        format == CompressionFormat.GZIP
            ? Map.of("content-type", "application/octet-stream", "content-encoding", "gzip")
            : Map.of("content-type", "application/octet-stream");
    when(mockPartitioner.getDeviceGradientPartition(anyString())).thenReturn(1);
    when(mockStorage.signUrl(
            /* blobInfo= */ any(),
            /* duration= */ anyLong(),
            /* unit= */ any(),
            any(), // Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
            any(), // Storage.SignUrlOption.withExtHeaders(resultGradientHeaders),
            any()) // Storage.SignUrlOption.withV4Signature()
        )
        .thenReturn(new URL("https://gcs?token=123"));

    // act
    BlobDescription description = manager.generateUploadGradientDescription(ASSIGNMENT, format);

    // assert
    String bucketName = "gra-1";
    String objectName = "us/35/17/d/xyz/gradient";
    assertThat(description)
        .isEqualTo(
            BlobDescription.builder()
                .host(bucketName)
                .resourceObject(objectName)
                .url("https://gcs?token=123")
                .headers(headers)
                .build());
    verify(mockPartitioner, times(1)).getDeviceGradientPartition("xyz");
    verify(mockStorage, times(1))
        .signUrl(
            eq(BlobInfo.newBuilder(BlobId.of(bucketName, objectName)).build()),
            eq(UPLOAD_CHECKPOINT_DURATION),
            eq(TimeUnit.SECONDS),
            refEq(Storage.SignUrlOption.httpMethod(HttpMethod.PUT)),
            refEq(Storage.SignUrlOption.withExtHeaders(headers)),
            refEq(Storage.SignUrlOption.withV4Signature()));
  }

  @Test
  public void testGenerateDownloadGradientDescriptions() throws MalformedURLException {
    // arrange
    when(mockPartitioner.getDeviceGradientPartitionCount()).thenReturn(2);

    // act
    BlobDescription[] descriptions = manager.generateDownloadGradientDescriptions(ITERATION);

    // assert
    String objectName = "us/35/17/d/";
    assertThat(descriptions)
        .isEqualTo(
            new BlobDescription[] {
              BlobDescription.builder()
                  .host("gra-0")
                  .resourceObject(objectName)
                  .url("gs://gra-0/us/35/17/d/")
                  .headers(emptyHeaders)
                  .build(),
              BlobDescription.builder()
                  .host("gra-1")
                  .resourceObject(objectName)
                  .url("gs://gra-1/us/35/17/d/")
                  .headers(emptyHeaders)
                  .build()
            });
    verify(mockPartitioner, times(1)).getDeviceGradientPartitionCount();
  }

  @Test
  public void testGenerateUploadAggregatedGradientDescription() throws MalformedURLException {
    // arrange
    when(mockPartitioner.getAggregatedResultPartition(anyString(), anyLong(), anyLong()))
        .thenReturn(1);

    // act
    BlobDescription description = manager.generateUploadAggregatedGradientDescription(ITERATION);

    // assert
    String bucketName = "agg-1";
    String objectName = "us/35/17/s/0/1/";
    assertThat(description)
        .isEqualTo(
            BlobDescription.builder()
                .host(bucketName)
                .resourceObject(objectName)
                .url("gs://agg-1/us/35/17/s/0/1/")
                .headers(emptyHeaders)
                .build());
    verify(mockPartitioner, times(1)).getAggregatedResultPartition("us", 35, 17);
  }

  @Test
  public void testGenerateDownloadAggregatedGradientDescription() throws MalformedURLException {
    // arrange
    when(mockPartitioner.getAggregatedResultPartition(anyString(), anyLong(), anyLong()))
        .thenReturn(3);

    // act
    BlobDescription description = manager.generateDownloadAggregatedGradientDescription(ITERATION);

    // assert
    String bucketName = "agg-3";
    String objectName = "us/35/17/s/0/0/";
    assertThat(description)
        .isEqualTo(
            BlobDescription.builder()
                .host(bucketName)
                .resourceObject(objectName)
                .url("gs://agg-3/us/35/17/s/0/0/")
                .headers(emptyHeaders)
                .build());
    verify(mockPartitioner, times(1)).getAggregatedResultPartition("us", 35, 17);
  }

  @Test
  public void testGenerateUploadMetricsDescriptions() {
    // arrange
    when(mockPartitioner.getCheckpointStoragePartitionCount()).thenReturn(3);

    // act
    BlobDescription[] descriptions = manager.generateUploadMetricsDescriptions(ITERATION);

    // assert
    String objectName = "us/35/17/s/0/metrics";
    assertThat(descriptions)
        .isEqualTo(
            new BlobDescription[] {
              BlobDescription.builder()
                  .host("mdl-0")
                  .resourceObject(objectName)
                  .url("gs://mdl-0/us/35/17/s/0/metrics")
                  .headers(emptyHeaders)
                  .build(),
              BlobDescription.builder()
                  .host("mdl-1")
                  .resourceObject(objectName)
                  .url("gs://mdl-1/us/35/17/s/0/metrics")
                  .headers(emptyHeaders)
                  .build(),
              BlobDescription.builder()
                  .host("mdl-2")
                  .resourceObject(objectName)
                  .url("gs://mdl-2/us/35/17/s/0/metrics")
                  .headers(emptyHeaders)
                  .build()
            });
    verify(mockPartitioner, times(1)).getCheckpointStoragePartitionCount();
  }

  @Test
  public void testGenerateUploadCheckpointDescriptions() {
    // arrange
    when(mockPartitioner.getCheckpointStoragePartitionCount()).thenReturn(3);

    // act
    BlobDescription[] descriptions = manager.generateUploadCheckpointDescriptions(ITERATION);

    // assert
    String objectName = "us/35/17/s/0/checkpoint";
    assertThat(descriptions)
        .isEqualTo(
            new BlobDescription[] {
              BlobDescription.builder()
                  .host("mdl-0")
                  .resourceObject(objectName)
                  .url("gs://mdl-0/us/35/17/s/0/checkpoint")
                  .headers(emptyHeaders)
                  .build(),
              BlobDescription.builder()
                  .host("mdl-1")
                  .resourceObject(objectName)
                  .url("gs://mdl-1/us/35/17/s/0/checkpoint")
                  .headers(emptyHeaders)
                  .build(),
              BlobDescription.builder()
                  .host("mdl-2")
                  .resourceObject(objectName)
                  .url("gs://mdl-2/us/35/17/s/0/checkpoint")
                  .headers(emptyHeaders)
                  .build()
            });
    verify(mockPartitioner, times(1)).getCheckpointStoragePartitionCount();
  }

  @Test
  public void testGenerateUploadClientCheckpointDescriptions() {
    // arrange
    when(mockPartitioner.getCheckpointStoragePartitionCount()).thenReturn(3);

    // act
    BlobDescription[] descriptions = manager.generateUploadClientCheckpointDescriptions(ITERATION);

    // assert
    String objectName = "us/35/17/d/0/client_checkpoint";
    assertThat(descriptions)
        .isEqualTo(
            new BlobDescription[] {
              BlobDescription.builder()
                  .host("mdl-0")
                  .resourceObject(objectName)
                  .url("gs://mdl-0/us/35/17/d/0/client_checkpoint")
                  .headers(emptyHeaders)
                  .build(),
              BlobDescription.builder()
                  .host("mdl-1")
                  .resourceObject(objectName)
                  .url("gs://mdl-1/us/35/17/d/0/client_checkpoint")
                  .headers(emptyHeaders)
                  .build(),
              BlobDescription.builder()
                  .host("mdl-2")
                  .resourceObject(objectName)
                  .url("gs://mdl-2/us/35/17/d/0/client_checkpoint")
                  .headers(emptyHeaders)
                  .build()
            });
    verify(mockPartitioner, times(1)).getCheckpointStoragePartitionCount();
  }

  @Test
  public void testGenerateDownloadCheckpointDescription_fromIterationIdAndAssignmentId()
      throws MalformedURLException {
    // arrange
    when(mockPartitioner.getCheckpointStoagePartition(anyString(), anyLong(), anyLong()))
        .thenReturn(4);
    when(mockStorage.signUrl(
            /* blobInfo= */ any(),
            /* duration= */ anyLong(),
            /* unit= */ any(),
            /* signUrlOptions= */ any()))
        .thenReturn(new URL("https://gcs?token=123"));
    IterationId trainingIterationId =
        IterationId.builder()
            .populationName(POPULATION_NAME)
            .taskId(TASK_ID)
            .iterationId(BASE_ON_RESULT_ID)
            .build();

    AssignmentId assignmentId =
        AssignmentId.builder()
            .populationName(POPULATION_NAME)
            .taskId(TASK_ID)
            .iterationId(BASE_ON_RESULT_ID)
            .assignmentId(SESSION_ID)
            .build();

    // act
    BlobDescription description =
        manager.generateDownloadCheckpointDescription(assignmentId, trainingIterationId);

    // assert
    String bucketName = "mdl-4";
    String objectName = "us/35/13/d/0/client_checkpoint";
    assertThat(description)
        .isEqualTo(
            BlobDescription.builder()
                .host(bucketName)
                .resourceObject(objectName)
                .url("https://gcs?token=123")
                .headers(emptyHeaders)
                .build());
    verify(mockPartitioner, times(1)).getCheckpointStoagePartition("us", 35, 13);
    verify(mockStorage, times(1))
        .signUrl(
            eq(BlobInfo.newBuilder(BlobId.of(bucketName, objectName)).build()),
            eq(DOWNLOAD_CHECKPOINT_DURATION),
            eq(TimeUnit.SECONDS),
            refEq(Storage.SignUrlOption.withV4Signature()));
  }

  @Test
  public void testGenerateDownloadCheckpointDescription_fromAssignmentEntity()
      throws MalformedURLException {
    // arrange
    when(mockPartitioner.getCheckpointStoagePartition(anyString(), anyLong(), anyLong()))
        .thenReturn(4);
    when(mockStorage.signUrl(
            /* blobInfo= */ any(),
            /* duration= */ anyLong(),
            /* unit= */ any(),
            /* signUrlOptions= */ any()))
        .thenReturn(new URL("https://gcs?token=123"));

    // act
    BlobDescription description = manager.generateDownloadCheckpointDescription(ASSIGNMENT);

    // assert
    String bucketName = "mdl-4";
    String objectName = "us/35/13/d/0/client_checkpoint";
    assertThat(description)
        .isEqualTo(
            BlobDescription.builder()
                .host(bucketName)
                .resourceObject(objectName)
                .url("https://gcs?token=123")
                .headers(emptyHeaders)
                .build());
    verify(mockPartitioner, times(1)).getCheckpointStoagePartition("us", 35, 13);
    verify(mockStorage, times(1))
        .signUrl(
            eq(BlobInfo.newBuilder(BlobId.of(bucketName, objectName)).build()),
            eq(DOWNLOAD_CHECKPOINT_DURATION),
            eq(TimeUnit.SECONDS),
            refEq(Storage.SignUrlOption.withV4Signature()));
  }

  @Test
  public void testGenerateDownloadCheckpointDescription_Server() throws MalformedURLException {
    // arrange
    when(mockPartitioner.getCheckpointStoagePartition(anyString(), anyLong(), anyLong()))
        .thenReturn(3);
    when(mockIterationEntity.getTrainingCheckpointIterationId())
        .thenReturn(IterationId.builder().populationName("us").taskId(35).iterationId(13).build());

    // act
    BlobDescription description =
        manager.generateDownloadCheckpointDescription(mockIterationEntity);

    // assert
    String bucketName = "mdl-3";
    String objectName = "us/35/13/s/0/checkpoint";

    assertThat(description)
        .isEqualTo(
            BlobDescription.builder()
                .host(bucketName)
                .resourceObject(objectName)
                .url("gs://mdl-3/us/35/13/s/0/checkpoint")
                .headers(emptyHeaders)
                .build());
    verify(mockPartitioner, times(1)).getCheckpointStoagePartition("us", 35, 13);
  }

  @Test
  public void testGenerateDownloadDevicePlanDescription() throws MalformedURLException {
    // arrange
    when(mockPartitioner.getPlanStoagePartition(anyString(), anyLong(), anyLong())).thenReturn(4);
    when(mockStorage.signUrl(
            /* blobInfo= */ any(),
            /* duration= */ anyLong(),
            /* unit= */ any(),
            /* signUrlOptions= */ any()))
        .thenReturn(new URL("https://gcs?token=123"));

    // act
    BlobDescription description = manager.generateDownloadDevicePlanDescription(ASSIGNMENT);

    // assert
    String bucketName = "mdl-4";
    String objectName = "us/35/0/s/0/client_only_plan";
    assertThat(description)
        .isEqualTo(
            BlobDescription.builder()
                .host(bucketName)
                .resourceObject(objectName)
                .url("https://gcs?token=123")
                .headers(emptyHeaders)
                .build());
    verify(mockPartitioner, times(1)).getPlanStoagePartition("us", 35, 13);
    verify(mockStorage, times(1))
        .signUrl(
            eq(BlobInfo.newBuilder(BlobId.of(bucketName, objectName)).build()),
            eq(DOWNLOAD_PLAN_DURATION),
            eq(TimeUnit.SECONDS),
            refEq(Storage.SignUrlOption.withV4Signature()));
  }

  @Test
  public void testGenerateUploadDevicePlanDescriptions() {
    // arrange
    when(mockPartitioner.getPlanStoragePartitionCount()).thenReturn(2);

    // act
    BlobDescription[] descriptions = manager.generateUploadDevicePlanDescriptions(TASK);

    // assert
    String objectName = "us/35/0/s/0/client_only_plan";
    assertThat(descriptions)
        .isEqualTo(
            new BlobDescription[] {
              BlobDescription.builder()
                  .host("mdl-0")
                  .resourceObject(objectName)
                  .url("gs://mdl-0/us/35/0/s/0/client_only_plan")
                  .headers(emptyHeaders)
                  .build(),
              BlobDescription.builder()
                  .host("mdl-1")
                  .resourceObject(objectName)
                  .url("gs://mdl-1/us/35/0/s/0/client_only_plan")
                  .headers(emptyHeaders)
                  .build()
            });
    verify(mockPartitioner, times(1)).getPlanStoragePartitionCount();
  }

  @Test
  public void testGenerateDownloadServerPlanDescription() throws MalformedURLException {
    // arrange
    when(mockPartitioner.getPlanStoagePartition(anyString(), anyLong(), anyLong())).thenReturn(9);

    // act
    BlobDescription description = manager.generateDownloadServerPlanDescription(ITERATION);

    // assert
    String bucketName = "mdl-9";
    String objectName = "us/35/0/s/0/server_phase";

    assertThat(description)
        .isEqualTo(
            BlobDescription.builder()
                .host(bucketName)
                .resourceObject(objectName)
                .url("gs://mdl-9/us/35/0/s/0/server_phase")
                .headers(emptyHeaders)
                .build());
    verify(mockPartitioner, times(1)).getPlanStoagePartition("us", 35, 13);
  }

  @Test
  public void testGenerateUploadServerPlanDescription() {
    // arrange
    when(mockPartitioner.getPlanStoragePartitionCount()).thenReturn(2);

    // act
    BlobDescription[] descriptions = manager.generateUploadServerPlanDescription(TASK);

    // assert
    String objectName = "us/35/0/s/0/server_phase";
    assertThat(descriptions)
        .isEqualTo(
            new BlobDescription[] {
              BlobDescription.builder()
                  .host("mdl-0")
                  .resourceObject(objectName)
                  .url("gs://mdl-0/us/35/0/s/0/server_phase")
                  .headers(emptyHeaders)
                  .build(),
              BlobDescription.builder()
                  .host("mdl-1")
                  .resourceObject(objectName)
                  .url("gs://mdl-1/us/35/0/s/0/server_phase")
                  .headers(emptyHeaders)
                  .build()
            });
    verify(mockPartitioner, times(1)).getPlanStoragePartitionCount();
  }

  @Test
  public void testGetDeviceUploadedGradientFullPath_Succeeded() {
    // act
    BlobDescription result =
        manager.getDeviceUploadedGradientFullPath(
            BlobDescription.builder().host("test-bucket-1").resourceObject("a/b/c/d/").build(),
            "session/");

    // assert
    assertThat(result)
        .isEqualTo(
            BlobDescription.builder()
                .host("test-bucket-1")
                .resourceObject("a/b/c/d/session/gradient")
                .url("gs://test-bucket-1/a/b/c/d/session/gradient")
                .build());
  }

  @Test
  public void testGetDeviceUploadedGradientFullPath_InvalidFolder() {
    // act
    IllegalArgumentException expected =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                manager.getDeviceUploadedGradientFullPath(
                    BlobDescription.builder().host("test-bucket-1").resourceObject("a/b/c").build(),
                    "d/"));

    // assert
    assertThat(expected).hasMessageThat().contains("a/b/c");
  }

  @Test
  public void testGetDeviceUploadedGradientFullPath_InvalidAssignmentFolder() {
    // act
    IllegalArgumentException expected =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                manager.getDeviceUploadedGradientFullPath(
                    BlobDescription.builder()
                        .host("test-bucket-1")
                        .resourceObject("a/b/c/")
                        .build(),
                    "session"));

    // assert
    assertThat(expected).hasMessageThat().contains("session");
  }

  @Test
  public void testBuilderAndEqual() {
    // arrange
    BlobDescription bd1 =
        BlobDescription.builder()
            .url("a")
            .host("b")
            .resourceObject("c")
            .headers(putHeaders)
            .build();
    BlobDescription bd2 =
        BlobDescription.builder()
            .url("a")
            .host("b")
            .resourceObject("c")
            .headers(putHeaders)
            .build();

    // act
    BlobDescription.BlobDescriptionBuilder builder = bd2.toBuilder();
    BlobDescription bd3 = builder.build();

    // assert
    assertThat(bd2).isEqualTo(bd1);
    assertThat(bd3).isEqualTo(bd1);
    assertThat(bd2.hashCode()).isEqualTo(bd1.hashCode());

    // for test coverage only.
    assertThat(builder.toString()).isNotEmpty();
  }
}
