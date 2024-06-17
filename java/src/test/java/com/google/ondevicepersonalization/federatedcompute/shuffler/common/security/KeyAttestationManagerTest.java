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

package com.google.ondevicepersonalization.federatedcompute.shuffler.common.security;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(JUnit4.class)
public class KeyAttestationManagerTest {

  private static final String CHALLENGE =
      "AHXUDhoSEFikqOefmo8xE7kGp/xjVMRDYBecBiHGxCN8rTv9W0Z4L/14d0OLB"
          + "vC1VVzXBAnjgHoKLZzuJifTOaBJwGNIQ2ejnx3n6ayoRchDNCgpK29T+EAhBWzH";

  private static String CHALLENGE_RESPONSE =
      "{\n"
          + " \"challenge\": \""
          + CHALLENGE
          + "\",\n"
          + " \"expireTime\": \"2023-08-04T11:14:59.257698Z\"}";

  private static String SUCCESS_VERIFICATION_RESPONSE;
  private static String FAILED_VERIRICATION_RESPONSE1;
  private static String FAILED_VERIRICATION_RESPONSE2;
  private static String FAILED_VERIRICATION_RESPONSE3;

  private static String URI = "test-url";

  private static String API_KEY = "<API KEY>";

  public static String ATTESTATION_RECORD = "[\"MIIDcjCCAxegAwIBAgIBAT\"]";

  @Mock HttpClientBuilder mockClientBuilder;

  @Mock CloseableHttpClient mockClient;

  @Mock CloseableHttpResponse mockResponse;

  @Mock StatusLine mockStatus;

  KeyAttestationManager kaMgr;

  private static final String FILE_SUFFIX = "java/src/test/java/resources/";

  static {
    try {
      SUCCESS_VERIFICATION_RESPONSE =
          readResponseJson(FILE_SUFFIX + "KeyAttestationSuccessVerification.json");
      FAILED_VERIRICATION_RESPONSE1 =
          readResponseJson(FILE_SUFFIX + "KeyAttestationFailVerification1.json");
      FAILED_VERIRICATION_RESPONSE2 =
          readResponseJson(FILE_SUFFIX + "KeyAttestationFailVerification2.json");
      FAILED_VERIRICATION_RESPONSE3 =
          readResponseJson(FILE_SUFFIX + "KeyAttestationFailVerification3.json");

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(mockResponse.getStatusLine()).thenReturn(mockStatus);
    kaMgr =
        new KeyAttestationManager(Optional.of(URI), Optional.of(API_KEY), mockClient, true, false);
  }

  @Test
  public void fetchChallenge_success() throws Exception {
    when(mockClient.execute(any())).thenReturn(mockResponse);
    when(mockResponse.getEntity()).thenReturn(new StringEntity(CHALLENGE_RESPONSE));
    when(mockStatus.getStatusCode()).thenReturn(200);

    byte[] challenge = kaMgr.fetchChallenge();
    assertArrayEquals(challenge, Base64.getDecoder().decode(CHALLENGE));
    verify(mockClient, times(1)).execute(any());
  }

  @Test
  public void fetchChallenge_throws() throws Exception {
    when(mockClient.execute(any())).thenReturn(mockResponse);
    when(mockResponse.getEntity()).thenReturn(new StringEntity(""));
    when(mockStatus.getStatusCode()).thenReturn(500);

    assertThrows(IllegalStateException.class, () -> kaMgr.fetchChallenge());
    verify(mockClient, times(1)).execute(any());
  }

  @Test
  public void verifyAttestationRecord_success() throws IOException {
    when(mockClient.execute(any())).thenReturn(mockResponse);
    when(mockResponse.getEntity()).thenReturn(new StringEntity(SUCCESS_VERIFICATION_RESPONSE));
    when(mockStatus.getStatusCode()).thenReturn(200);

    boolean isVerified = kaMgr.isAttestationRecordVerified(ATTESTATION_RECORD);

    assertTrue(isVerified);
    verify(mockClient, times(1)).execute(any());
  }

  @Test
  public void verifyAttestationRecord_fail() throws IOException {
    var responses =
        List.of(
            FAILED_VERIRICATION_RESPONSE1,
            FAILED_VERIRICATION_RESPONSE2,
            FAILED_VERIRICATION_RESPONSE3);
    for (var response : responses) {
      when(mockClient.execute(any())).thenReturn(mockResponse);
      when(mockResponse.getEntity()).thenReturn(new StringEntity(response));
      when(mockStatus.getStatusCode()).thenReturn(200);

      boolean isVerified = kaMgr.isAttestationRecordVerified(ATTESTATION_RECORD);

      assertFalse(isVerified);
    }
  }

  @Test
  public void verifyAttestationRecord_throws() throws IOException {
    when(mockClient.execute(any())).thenReturn(mockResponse);
    when(mockResponse.getEntity()).thenReturn(new StringEntity(""));
    when(mockStatus.getStatusCode()).thenReturn(500);

    assertThrows(
        IllegalStateException.class, () -> kaMgr.isAttestationRecordVerified(ATTESTATION_RECORD));
    verify(mockClient, times(1)).execute(any());
  }

  private static String readResponseJson(String path) throws Exception {
    return new String(Files.readAllBytes(Paths.get(path)));
  }
}
