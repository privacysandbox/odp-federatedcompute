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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.crypto.tink.HybridDecrypt;
import com.google.crypto.tink.subtle.Base64;
import com.google.fcp.plan.PhaseSession;
import com.google.fcp.plan.TensorflowPlanSession;
import com.google.fcp.tensorflow.AppFiles;
import com.google.gson.Gson;
import com.google.ondevicepersonalization.federatedcompute.shuffler.aggregator.core.message.AggregatorMessage;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.crypto.Payload;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.crypto.PublicKeyEncryptionService;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobDescription;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.IterationEntity;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.tensorflow.TensorflowPlanSessionFactory;
import com.google.ondevicepersonalization.federatedcompute.shuffler.modelupdater.core.message.ModelUpdaterMessage;
import com.google.protobuf.ByteString;
import com.google.scp.operator.cpio.cryptoclient.DecryptionKeyService;
import java.io.IOException;
import java.time.Instant;
import java.time.InstantSource;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(JUnit4.class)
public final class AggregatorCoreImplTest {

  private static final IterationEntity ITERATION1 =
      IterationEntity.builder()
          .populationName("us")
          .taskId(35)
          .iterationId(17)
          .attemptId(0)
          .status(IterationEntity.Status.AGGREGATING)
          .baseIterationId(16)
          .baseOnResultId(16)
          .reportGoal(3)
          .resultId(17)
          .build();
  private static final IterationEntity ITERATION2 =
      IterationEntity.builder()
          .populationName("us")
          .taskId(36)
          .iterationId(18)
          .attemptId(0)
          .status(IterationEntity.Status.AGGREGATING)
          .baseIterationId(17)
          .baseOnResultId(17)
          .reportGoal(3)
          .resultId(18)
          .build();
  private static final BlobDescription DIR1_1 =
      BlobDescription.builder()
          .host("test-1-1")
          .resourceObject("us/35/17/d/")
          .url("gs://test-1-1/us/35/17/d/")
          .build();
  private static final BlobDescription RESULT_1 =
      BlobDescription.builder()
          .host("test-1-1")
          .resourceObject("us/35/17/s/0/checkpoint")
          .url("gs://test-1-1/us/35/17/s/0/checkpoint")
          .build();
  private static final BlobDescription PLAN_1 =
      BlobDescription.builder()
          .host("test-1-1")
          .resourceObject("us/35/17/s/0/server_plan")
          .url("gs://test-1-1/us/35/17/s/0/server_plan")
          .build();
  private static final BlobDescription DIR2_1 =
      BlobDescription.builder()
          .host("test-2-1")
          .resourceObject("us/36/18/d/")
          .url("gs://test-2-1/us/36/18/d/")
          .build();
  private static final BlobDescription RESULT_2 =
      BlobDescription.builder()
          .host("test-1-1")
          .resourceObject("us/36/18/s/0/checkpoint")
          .url("gs://test-1-1/us/36/18/s/0/checkpoint")
          .build();
  private static final BlobDescription PLAN_2 =
      BlobDescription.builder()
          .host("test-1-1")
          .resourceObject("us/36/18/s/0/server_plan")
          .url("gs://test-1-1/us/36/18/s/0/server_plan")
          .build();

  private static final AggregatorMessage MESSAGE1 =
      AggregatorMessage.builder()
          .serverPlanBucket(PLAN_1.getHost())
          .serverPlanObject(PLAN_1.getResourceObject())
          .gradientBucket(DIR1_1.getHost())
          .gradientPrefix(DIR1_1.getResourceObject())
          .gradients(List.of("iter1_1/", "iter1_2/"))
          .aggregatedGradientOutputBucket(RESULT_1.getHost())
          .aggregatedGradientOutputObject(RESULT_1.getResourceObject())
          .requestId(ITERATION1.getId().toString())
          .accumulateIntermediateUpdates(false)
          .build();

  private static final AggregatorMessage MESSAGE2 =
      AggregatorMessage.builder()
          .serverPlanBucket(PLAN_2.getHost())
          .serverPlanObject(PLAN_2.getResourceObject())
          .gradientBucket(DIR2_1.getHost())
          .gradientPrefix(DIR2_1.getResourceObject())
          .gradients(List.of("iter2_1/", "iter2_2/"))
          .aggregatedGradientOutputBucket(RESULT_2.getHost())
          .aggregatedGradientOutputObject(RESULT_2.getResourceObject())
          .requestId(ITERATION2.getId().toString())
          .accumulateIntermediateUpdates(false)
          .build();

  // GMT time.
  private static Instant NOW = Instant.parse("2023-09-01T00:00:00Z");
  private static InstantSource instanceSource = InstantSource.fixed(NOW);

  // Base-64 encoded gzip of the string "HelloWorld"
  private static String GZIP_GRADIENT = "H4sIAAAAAAAAAPNIzcnJD88vykkBAHkMd3cKAAAA";
  @Mock BlobDao blobDao;
  @Mock TensorflowPlanSessionFactory tensorflowPlanSessionFactory;
  @Mock TensorflowPlanSession tensorflowPlanSession;
  @Mock PhaseSession phaseSession;

  @Mock DecryptionKeyService decryptionKeyService;
  @Mock HybridDecrypt hybridDecrypt;

  @Mock PublicKeyEncryptionService publicKeyEncryptionService;
  @Mock AppFiles appFiles;

  ArgumentCaptor<byte[]> uploadResultCaptor;
  ArgumentCaptor<ModelUpdaterMessage> messageCaptor;
  ArgumentCaptor<ByteString> gradientCaptor;
  private AggregatorCoreImpl core;
  private byte[] gradient;
  private Payload payload;
  private byte[] plan;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    core =
        new AggregatorCoreImpl(
            blobDao,
            instanceSource,
            tensorflowPlanSessionFactory,
            decryptionKeyService,
            publicKeyEncryptionService,
            appFiles);
    gradient =
        ("{\n"
             + "  \"encryptedPayload\":\"EqHpPLNug0sxrO+C/khNBauZxBFaBDAnm9YsriaW5FIUduNy6JRpSuwVTRu41tMjxA8uuRL5nbyqvKgd7qAKC2PpcmjnrQ4WpO/++a0Z\",\n"
             + "  \"keyId\":\"596cd2c2-5aee-4d29-858f-a26a7d62de1b\",\n"
             + "  \"associatedData\":\"\"\n"
             + "}")
            .getBytes();
    payload =
        Payload.builder()
            .keyId("596cd2c2-5aee-4d29-858f-a26a7d62de1b")
            .encryptedPayload(
                "EqHpPLNug0sxrO+C/khNBauZxBFaBDAnm9YsriaW5FIUduNy6JRpSuwVTRu41tMjxA8uuRL5nbyqvKgd7qAKC2PpcmjnrQ4WpO/++a0Z")
            .associatedData("")
            .build();
    plan = new byte[] {1, 2};
    uploadResultCaptor = ArgumentCaptor.forClass(byte[].class);
    messageCaptor = ArgumentCaptor.forClass(ModelUpdaterMessage.class);
    gradientCaptor = ArgumentCaptor.forClass(ByteString.class);
  }

  @Test
  public void testProcess_Succeeded() throws Exception {
    // arange
    when(blobDao.download(any())).thenReturn(gradient);
    when(blobDao.download(PLAN_1)).thenReturn(plan);
    when(blobDao.download(PLAN_2)).thenReturn(plan);
    when(tensorflowPlanSessionFactory.createPlanSession(any())).thenReturn(tensorflowPlanSession);
    when(tensorflowPlanSession.createPhaseSession(any(), any())).thenReturn(phaseSession);
    doNothing().when(phaseSession).accumulateClientUpdate(any());
    when(phaseSession.toIntermediateUpdate()).thenReturn(ByteString.copyFrom(new byte[] {10}));
    when(decryptionKeyService.getDecrypter(any())).thenReturn(hybridDecrypt);
    when(hybridDecrypt.decrypt(any(), any())).thenReturn(Base64.decode(GZIP_GRADIENT));
    when(publicKeyEncryptionService.encryptPayload(any(), any())).thenReturn(payload);

    // act
    core.process(MESSAGE1);
    core.process(MESSAGE2);

    // assert
    verify(blobDao, times(6)).download(any());
    verify(publicKeyEncryptionService, times(2)).encryptPayload(any(), any());
    verify(blobDao, times(2)).upload(any(), uploadResultCaptor.capture());
    List<byte[]> capturedResults = uploadResultCaptor.getAllValues();
    Gson gson = new Gson();
    assertArrayEquals(capturedResults.get(0), gson.toJson(payload).getBytes());
    assertArrayEquals(capturedResults.get(1), gson.toJson(payload).getBytes());
    verify(decryptionKeyService, times(4)).getDecrypter("596cd2c2-5aee-4d29-858f-a26a7d62de1b");
    verify(hybridDecrypt, times(4)).decrypt(any(), any());
    verify(tensorflowPlanSessionFactory, times(2)).createPlanSession(any());
    verify(tensorflowPlanSession, times(2)).createPhaseSession(any(), eq(Optional.of(appFiles)));
    verify(phaseSession, times(4)).accumulateClientUpdate(gradientCaptor.capture());
    List<ByteString> capturedGradientResults = gradientCaptor.getAllValues();
    assertArrayEquals("HelloWorld".getBytes(), capturedGradientResults.get(0).toByteArray());
    verify(phaseSession, times(2)).toIntermediateUpdate();
  }

  @Test
  public void testProcess_SucceededIntermediateUpdates() throws Exception {
    // arange
    when(blobDao.download(any())).thenReturn(gradient);
    when(blobDao.download(PLAN_1)).thenReturn(plan);
    when(blobDao.download(PLAN_2)).thenReturn(plan);
    when(tensorflowPlanSessionFactory.createPlanSession(any())).thenReturn(tensorflowPlanSession);
    when(tensorflowPlanSession.createPhaseSession(any(), any())).thenReturn(phaseSession);
    doNothing().when(phaseSession).accumulateIntermediateUpdate(any());
    when(phaseSession.toIntermediateUpdate()).thenReturn(ByteString.copyFrom(new byte[] {10}));
    when(decryptionKeyService.getDecrypter(any())).thenReturn(hybridDecrypt);
    when(hybridDecrypt.decrypt(any(), any())).thenReturn(Base64.decode(GZIP_GRADIENT));
    when(publicKeyEncryptionService.encryptPayload(any(), any())).thenReturn(payload);

    // act
    core.process(MESSAGE1.toBuilder().accumulateIntermediateUpdates(true).build());
    core.process(MESSAGE2.toBuilder().accumulateIntermediateUpdates(true).build());

    // assert
    verify(blobDao, times(6)).download(any());
    verify(publicKeyEncryptionService, times(2)).encryptPayload(any(), any());
    verify(blobDao, times(2)).upload(any(), uploadResultCaptor.capture());
    List<byte[]> capturedResults = uploadResultCaptor.getAllValues();
    Gson gson = new Gson();
    assertArrayEquals(capturedResults.get(0), gson.toJson(payload).getBytes());
    assertArrayEquals(capturedResults.get(1), gson.toJson(payload).getBytes());
    verify(decryptionKeyService, times(4)).getDecrypter("596cd2c2-5aee-4d29-858f-a26a7d62de1b");
    verify(hybridDecrypt, times(4)).decrypt(any(), any());
    verify(tensorflowPlanSessionFactory, times(2)).createPlanSession(any());
    verify(tensorflowPlanSession, times(2)).createPhaseSession(any(), eq(Optional.of(appFiles)));
    verify(phaseSession, times(0)).accumulateClientUpdate(any());
    verify(phaseSession, times(4)).accumulateIntermediateUpdate(gradientCaptor.capture());
    List<ByteString> capturedGradientResults = gradientCaptor.getAllValues();
    assertArrayEquals("HelloWorld".getBytes(), capturedGradientResults.get(0).toByteArray());
    verify(phaseSession, times(2)).toIntermediateUpdate();
  }

  @Test
  public void testProcess_ExceptionWhenUploadResult() throws Exception {
    // arange
    when(blobDao.download(any())).thenReturn(gradient);
    when(blobDao.download(PLAN_1)).thenReturn(plan);
    when(blobDao.download(PLAN_2)).thenReturn(plan);
    doThrow(new IOException()).when(blobDao).upload(any(), any());
    when(tensorflowPlanSessionFactory.createPlanSession(any())).thenReturn(tensorflowPlanSession);
    when(tensorflowPlanSession.createPhaseSession(any(), any())).thenReturn(phaseSession);
    doNothing().when(phaseSession).accumulateClientUpdate(any());
    when(phaseSession.toIntermediateUpdate()).thenReturn(ByteString.copyFrom(new byte[] {10}));
    when(decryptionKeyService.getDecrypter(any())).thenReturn(hybridDecrypt);
    when(hybridDecrypt.decrypt(any(), any())).thenReturn(Base64.decode(GZIP_GRADIENT));
    when(publicKeyEncryptionService.encryptPayload(any(), any())).thenReturn(payload);

    // act
    assertThrows(RuntimeException.class, () -> core.process(MESSAGE1));
    assertThrows(RuntimeException.class, () -> core.process(MESSAGE2));

    // assert
    verify(blobDao, times(6)).download(any());
    verify(blobDao, times(2)).upload(any(), uploadResultCaptor.capture());
    List<byte[]> capturedResults = uploadResultCaptor.getAllValues();
    Gson gson = new Gson();
    assertArrayEquals(capturedResults.get(0), gson.toJson(payload).getBytes());
    assertArrayEquals(capturedResults.get(1), gson.toJson(payload).getBytes());
    verify(decryptionKeyService, times(4)).getDecrypter("596cd2c2-5aee-4d29-858f-a26a7d62de1b");
    verify(hybridDecrypt, times(4)).decrypt(any(), any());
    verify(tensorflowPlanSessionFactory, times(2)).createPlanSession(any());
    verify(tensorflowPlanSession, times(2)).createPhaseSession(any(), eq(Optional.of(appFiles)));
    verify(phaseSession, times(4)).accumulateClientUpdate(any());
    verify(phaseSession, times(2)).toIntermediateUpdate();
  }

  @Test
  public void testProcess_NotFailOnOtherIllegalException() throws Exception {
    // arange
    when(blobDao.download(any())).thenReturn(gradient);
    when(blobDao.download(PLAN_1)).thenReturn(plan);
    when(tensorflowPlanSessionFactory.createPlanSession(any())).thenReturn(tensorflowPlanSession);
    when(tensorflowPlanSession.createPhaseSession(any(), any())).thenReturn(phaseSession);
    doThrow(new IllegalStateException("error.")).when(phaseSession).accumulateClientUpdate(any());
    when(phaseSession.toIntermediateUpdate()).thenReturn(ByteString.copyFrom(new byte[] {10}));
    when(decryptionKeyService.getDecrypter(any())).thenReturn(hybridDecrypt);
    when(hybridDecrypt.decrypt(any(), any())).thenReturn(Base64.decode(GZIP_GRADIENT));
    when(publicKeyEncryptionService.encryptPayload(any(), any())).thenReturn(payload);

    // act
    assertThrows(IllegalStateException.class, () -> core.process(MESSAGE1));
  }
}
