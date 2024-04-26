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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.common.collect.ImmutableList;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobDescription;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** GCSBlob Dao test. */
@RunWith(JUnit4.class)
public final class GCSBlobDaoTest {

  @Mock Storage mockStorage;
  @Mock Page<Blob> mockReturnedPage;
  @Mock Page<Blob> mockReturnedPage2;
  @Mock Blob mockBlob1;
  @Mock Blob mockBlob2;
  GCSBlobDao blobDao;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    blobDao = new GCSBlobDao(mockStorage, Optional.empty());
  }

  @Test
  public void testList_Succeeded() {
    // arrange
    when(mockBlob1.getName()).thenReturn("us/35/17/d/session_1/");
    when(mockBlob2.getName()).thenReturn("us/35/17/d/session_2/");
    when(mockReturnedPage.iterateAll()).thenReturn(ImmutableList.of(mockBlob1, mockBlob2));
    when(mockStorage.list(anyString(), /* BlobListOption= */ any(), /* BlobListOption= */ any()))
        .thenReturn(mockReturnedPage);

    // act
    List<String> result =
        blobDao.list(
            BlobDescription.builder().host("test-bucket-1").resourceObject("us/35/17/d/").build());

    // assert
    assertThat(result).isEqualTo(ImmutableList.of("session_1/", "session_2/"));
    verify(mockStorage, times(1))
        .list(
            "test-bucket-1",
            Storage.BlobListOption.prefix("us/35/17/d/"),
            Storage.BlobListOption.currentDirectory());
  }

  @Test
  public void testListByPartition_Succeeded() {
    // arrange
    when(mockBlob1.getName()).thenReturn("us/35/17/d/tession_1/");
    when(mockBlob2.getName()).thenReturn("us/35/17/d/session_2/");
    when(mockReturnedPage.iterateAll()).thenReturn(ImmutableList.of(mockBlob2));
    when(mockReturnedPage2.iterateAll()).thenReturn(ImmutableList.of(mockBlob1));
    when(mockStorage.list(
            "test-bucket-1",
            Storage.BlobListOption.prefix("us/35/17/d/s"),
            Storage.BlobListOption.currentDirectory()))
        .thenReturn(mockReturnedPage);
    when(mockStorage.list(
            "test-bucket-1",
            Storage.BlobListOption.prefix("us/35/17/d/t"),
            Storage.BlobListOption.currentDirectory()))
        .thenReturn(mockReturnedPage2);

    // act
    List<String> result =
        blobDao.listByPartition(
            BlobDescription.builder().host("test-bucket-1").resourceObject("us/35/17/d/").build(),
            List.of("s", "t"));

    // assert
    assertThat(result).isEqualTo(ImmutableList.of("session_2/", "tession_1/"));
    verify(mockStorage, times(1))
        .list(
            "test-bucket-1",
            Storage.BlobListOption.prefix("us/35/17/d/s"),
            Storage.BlobListOption.currentDirectory());
    verify(mockStorage, times(1))
        .list(
            "test-bucket-1",
            Storage.BlobListOption.prefix("us/35/17/d/t"),
            Storage.BlobListOption.currentDirectory());
  }

  @Test
  public void testDownload_Succeeded() {
    // arrange
    when(mockStorage.get(isA(BlobId.class))).thenReturn(mockBlob1);
    when(mockBlob1.exists()).thenReturn(true);
    when(mockBlob1.getContent()).thenReturn(new byte[] {10, 20, 30});

    // act
    byte[] content =
        blobDao.download(
            BlobDescription.builder().host("test-bucket").resourceObject("path/file").build());

    // assert
    assertThat(content).isEqualTo(new byte[] {10, 20, 30});
    verify(mockStorage, times(1)).get(BlobId.of("test-bucket", "path/file"));
    verify(mockBlob1, times(1)).getContent();
  }

  @Test
  public void testDownloadUri_Succeeded() {
    // arrange
    when(mockStorage.get(isA(BlobId.class))).thenReturn(mockBlob1);
    when(mockBlob1.exists()).thenReturn(true);
    when(mockBlob1.getContent()).thenReturn(new byte[] {10, 20, 30});

    // act
    byte[] content =
        blobDao.download(BlobDescription.builder().url("gs://test-bucket/path/file").build());

    // assert
    assertThat(content).isEqualTo(new byte[] {10, 20, 30});
    verify(mockStorage, times(1)).get(BlobId.fromGsUtilUri("gs://test-bucket/path/file"));
    verify(mockBlob1, times(1)).getContent();
  }

  @Test
  public void testUpload_Succeeded() throws IOException {
    // arrange
    when(mockStorage.createFrom(isA(BlobInfo.class), isA(ByteArrayInputStream.class)))
        .thenReturn(mockBlob1);

    // act
    blobDao.upload(
        BlobDescription.builder().host("test-bucket").resourceObject("path/file").build(),
        new byte[] {10, 20, 30});

    // assert
    verify(mockStorage, times(1)).createFrom(isA(BlobInfo.class), isA(ByteArrayInputStream.class));
  }

  @Test
  public void testUploadUri_Succeeded() throws IOException {
    // arrange
    when(mockStorage.createFrom(isA(BlobInfo.class), isA(ByteArrayInputStream.class)))
        .thenReturn(mockBlob1);

    // act
    blobDao.upload(
        BlobDescription.builder().url("gs://test-bucket/path/file").build(),
        new byte[] {10, 20, 30});

    // assert
    verify(mockStorage, times(1)).createFrom(isA(BlobInfo.class), isA(ByteArrayInputStream.class));
  }

  @Test
  public void testExists_Succeeded() throws IOException {
    // arrange
    when(mockStorage.get(isA(BlobId.class))).thenReturn(mockBlob1);
    when(mockBlob1.exists()).thenReturn(true);

    // act
    boolean fileExists =
        blobDao.exists(
            new BlobDescription[] {
              BlobDescription.builder().host("test-bucket").resourceObject("path/file").build()
            });

    // assert
    assertThat(fileExists).isTrue();
    verify(mockStorage, times(1)).get(BlobId.of("test-bucket", "path/file"));
  }

  @Test
  public void testExists_Failed() {
    // arrange
    when(mockStorage.get(isA(BlobId.class))).thenReturn(null);

    // act
    boolean fileExists =
        blobDao.exists(
            new BlobDescription[] {
              BlobDescription.builder().host("test-bucket").resourceObject("path/file").build()
            });

    // assert
    assertThat(fileExists).isFalse();
    verify(mockStorage, times(1)).get(BlobId.of("test-bucket", "path/file"));
  }

  @Test
  public void testExists_MultiplePartitions_Succeeded() throws IOException {
    // arrange
    when(mockStorage.get(isA(BlobId.class))).thenReturn(mockBlob1);
    when(mockBlob1.exists()).thenReturn(true);

    // act
    boolean filesExist =
        blobDao.exists(
            new BlobDescription[] {
              BlobDescription.builder()
                  .host("test-bucket-1")
                  .resourceObject("path-1/file-1")
                  .build(),
              BlobDescription.builder()
                  .host("test-bucket-2")
                  .resourceObject("path-2/file-2")
                  .build()
            });
    // assert
    assertThat(filesExist).isTrue();
    verify(mockStorage, times(1)).get(BlobId.of("test-bucket-1", "path-1/file-1"));
    verify(mockStorage, times(1)).get(BlobId.of("test-bucket-2", "path-2/file-2"));
  }

  @Test
  public void testExists_MultiplePartitions_Failed() throws IOException {
    // arrange
    when(mockStorage.get(BlobId.of("test-bucket-1", "path-1/file-1"))).thenReturn(mockBlob1);
    when(mockStorage.get(BlobId.of("test-bucket-2", "path-2/file-2"))).thenReturn(null);
    when(mockStorage.get(BlobId.of("test-bucket-3", "path-3/file-3"))).thenReturn(mockBlob1);
    when(mockBlob1.exists()).thenReturn(true);

    // act
    boolean filesExist =
        blobDao.exists(
            new BlobDescription[] {
              BlobDescription.builder()
                  .host("test-bucket-1")
                  .resourceObject("path-1/file-1")
                  .build(),
              BlobDescription.builder()
                  .host("test-bucket-2")
                  .resourceObject("path-2/file-2")
                  .build(),
              BlobDescription.builder()
                  .host("test-bucket-3")
                  .resourceObject("path-3/file-3")
                  .build()
            });
    // assert
    assertThat(filesExist).isFalse();
    verify(mockStorage, times(1)).get(BlobId.of("test-bucket-1", "path-1/file-1"));
    verify(mockStorage, times(1)).get(BlobId.of("test-bucket-2", "path-2/file-2"));
    verify(mockStorage, times(0)).get(BlobId.of("test-bucket-3", "path-3/file-3"));
  }
}
