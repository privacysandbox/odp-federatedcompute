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

package com.google.ondevicepersonalization.federatedcompute.shuffler.common.config.gcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(JUnit4.class)
public class GcpVMMetadataServiceClientTest {

  private GcpVMMetadataServiceClient gcpVMMetadataServiceClient;
  @Mock HttpClient mockHttpClient;

  @Mock HttpResponse mockHttpResponse;

  @Mock HttpEntity mockHttpEntity;

  @Mock StatusLine mockStatusLine;

  ArgumentCaptor<HttpGet> httpGetCaptor;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    gcpVMMetadataServiceClient = new GcpVMMetadataServiceClient(mockHttpClient);
    httpGetCaptor = ArgumentCaptor.forClass(HttpGet.class);
  }

  @Test
  public void test_getGcpProjectId() throws IOException {
    InputStream is = new ByteArrayInputStream("HelloWorld".getBytes());
    when(mockHttpClient.execute(any())).thenReturn(mockHttpResponse);
    when(mockHttpResponse.getEntity()).thenReturn(mockHttpEntity);
    when(mockHttpEntity.getContent()).thenReturn(is);

    assertEquals("HelloWorld", gcpVMMetadataServiceClient.getGcpProjectId());
    verify(mockHttpClient, times(1)).execute(httpGetCaptor.capture());
    List<HttpGet> captorList = httpGetCaptor.getAllValues();
    HttpGet httpGet = captorList.get(0);
    assertEquals("Google", httpGet.getHeaders("Metadata-Flavor")[0].getValue());
    assertEquals(
        "http://metadata.google.internal/computeMetadata/v1/project/project-id",
        httpGet.getURI().toString());
  }

  @Test
  public void test_getMetadata() throws IOException {
    InputStream is = new ByteArrayInputStream("HelloWorld".getBytes());

    when(mockHttpClient.execute(any())).thenReturn(mockHttpResponse);
    when(mockHttpResponse.getEntity()).thenReturn(mockHttpEntity);
    when(mockHttpEntity.getContent()).thenReturn(is);
    when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
    when(mockStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);

    assertEquals("HelloWorld", gcpVMMetadataServiceClient.getMetadata("key").get());
    verify(mockHttpClient, times(1)).execute(httpGetCaptor.capture());
    List<HttpGet> captorList = httpGetCaptor.getAllValues();
    HttpGet httpGet = captorList.get(0);
    assertEquals("Google", httpGet.getHeaders("Metadata-Flavor")[0].getValue());
    assertEquals(
        "http://metadata.google.internal/computeMetadata/v1/instance/attributes/key",
        httpGet.getURI().toString());
  }

  @Test
  public void test_getMetadataNotFound() throws IOException {
    InputStream is = new ByteArrayInputStream("HelloWorld".getBytes());

    when(mockHttpClient.execute(any())).thenReturn(mockHttpResponse);
    when(mockHttpResponse.getEntity()).thenReturn(mockHttpEntity);
    when(mockHttpEntity.getContent()).thenReturn(is);
    when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
    when(mockStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_NOT_FOUND);

    assertTrue(gcpVMMetadataServiceClient.getMetadata("key").isEmpty());
  }

  @Test
  public void test_getMetadataBadStatus() throws IOException {
    InputStream is = new ByteArrayInputStream("HelloWorld".getBytes());

    when(mockHttpClient.execute(any())).thenReturn(mockHttpResponse);
    when(mockHttpResponse.getEntity()).thenReturn(mockHttpEntity);
    when(mockHttpEntity.getContent()).thenReturn(is);
    when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
    when(mockStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_BAD_REQUEST);

    assertThrows(IOException.class, () -> gcpVMMetadataServiceClient.getMetadata("key"));
  }
}
