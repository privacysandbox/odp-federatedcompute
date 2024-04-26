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

package com.google.ondevicepersonalization.federatedcompute.shuffler.common.crypto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.scp.shared.api.util.HttpClientResponse;
import com.google.scp.shared.api.util.HttpClientWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(JUnit4.class)
public class PublicKeyFetchingServiceTest {

  private PublicKeyFetchingService publicKeyFetchingService;
  private static String URI = "https://publickeyservice.com";
  private static String RESPONSE =
      "{\n"
          + "  \"keys\": [{\n"
          + "    \"id\": \"2b9a33a7-895f-4b60-98f2-0502adea3afa\",\n"
          + "    \"key\": \"2AFfcfrLWhdW3EEIvM1FulwH7JBCEHAaWUyO+8t0RGg\\u003d\"\n"
          + "  }, {\n"
          + "    \"id\": \"a748012a-bff4-4d53-903c-e9a6a121d40f\",\n"
          + "    \"key\": \"Y9VubbmpqMQhzt5QgUH5odZcqZ7tWobz7e36bQkrDWc\\u003d\"\n"
          + "  }, {\n"
          + "    \"id\": \"caf979c5-3cd7-4814-9f50-c613351f0be3\",\n"
          + "    \"key\": \"UHmxhQGcSMNUY0viNgCoGImY7umng9SWiGbcDEqkym0\\u003d\"\n"
          + "  }, {\n"
          + "    \"id\": \"ce6d600f-f2cc-49ff-b90a-8f1b9febf480\",\n"
          + "    \"key\": \"s7fyer3n6utITHhwds+310F+HNiJkIi/n9OFkxmR+kA\\u003d\"\n"
          + "  }, {\n"
          + "    \"id\": \"f840a3e5-1353-4e63-a502-a26063f31be8\",\n"
          + "    \"key\": \"9j83cLx4115wkde9pYXqRiYOGdHCAnDiZwBDviW6Rj8\\u003d\"\n"
          + "  }]\n"
          + "}";

  @Mock HttpClientWrapper httpClientWrapper;

  @Mock HttpClientResponse httpClientResponse;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    publicKeyFetchingService = new PublicKeyFetchingService(URI, httpClientWrapper);
  }

  @Test
  public void fetch_success() throws Exception {
    when(httpClientWrapper.execute(any())).thenReturn(httpClientResponse);
    when(httpClientResponse.statusCode()).thenReturn(200);
    when(httpClientResponse.responseBody()).thenReturn(RESPONSE);

    PublicKeys publicKeys = publicKeyFetchingService.fetchPublicKeys();
    assertEquals(publicKeys.getKeys().size(), 5);
    verify(httpClientWrapper, times(1)).execute(any());
  }

  @Test
  public void fetch_fail() throws Exception {
    when(httpClientWrapper.execute(any())).thenReturn(httpClientResponse);
    when(httpClientResponse.statusCode()).thenReturn(404);
    when(httpClientResponse.responseBody()).thenReturn("");

    assertThrows(IllegalStateException.class, () -> publicKeyFetchingService.fetchPublicKeys());
    verify(httpClientWrapper, times(1)).execute(any());
  }

  @Test
  public void parse_fail() throws Exception {
    when(httpClientWrapper.execute(any())).thenReturn(httpClientResponse);
    when(httpClientResponse.statusCode()).thenReturn(200);
    when(httpClientResponse.responseBody()).thenReturn("bad string");

    assertThrows(IllegalStateException.class, () -> publicKeyFetchingService.fetchPublicKeys());
    verify(httpClientWrapper, times(1)).execute(any());
  }
}
