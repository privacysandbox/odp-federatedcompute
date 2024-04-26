/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ondevicepersonalization.federatedcompute.shuffler.common.config.gcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.gax.rpc.NotFoundException;
import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretPayload;
import com.google.protobuf.ByteString;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(JUnit4.class)
public class GcpParameterClientTest {

  private GcpParameterClient gcpParameterClient;

  @Mock private SecretManagerServiceClient mockSecretManagerServiceClient;

  @Mock private SecretPayload mockSecretPayload;

  @Mock private AccessSecretVersionResponse mockAccessSecretVersionResponse;

  @Mock private NotFoundException mockNotFoundException;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    gcpParameterClient = new GcpParameterClient(mockSecretManagerServiceClient, "projectId", "env");
  }

  @Test
  public void test_getParameter() {
    when(mockSecretManagerServiceClient.accessSecretVersion(anyString()))
        .thenReturn(mockAccessSecretVersionResponse);
    when(mockAccessSecretVersionResponse.getPayload()).thenReturn(mockSecretPayload);
    when(mockSecretPayload.getData()).thenReturn(ByteString.copyFrom("HelloWorld".getBytes()));

    assertEquals("HelloWorld", gcpParameterClient.getParameter("param").get());
    verify(mockSecretManagerServiceClient, times(1))
        .accessSecretVersion(eq("projects/projectId/secrets/fc-env-param/versions/latest"));
  }

  @Test
  public void test_getParameterNotFound() {
    when(mockSecretManagerServiceClient.accessSecretVersion(anyString()))
        .thenThrow(mockNotFoundException);

    assertTrue(gcpParameterClient.getParameter("param").isEmpty());
  }

  @Test
  public void test_getParameterError() {
    when(mockSecretManagerServiceClient.accessSecretVersion(anyString()))
        .thenThrow(new RuntimeException());

    assertThrows(
        IllegalStateException.class, () -> gcpParameterClient.getParameter("param").isEmpty());
  }
}
