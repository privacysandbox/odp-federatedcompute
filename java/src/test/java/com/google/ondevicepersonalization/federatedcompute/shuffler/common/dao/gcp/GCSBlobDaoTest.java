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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.gax.paging.Page;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageBatch;
import com.google.cloud.storage.StorageBatchResult;
import com.google.cloud.storage.StorageException;
import com.google.common.collect.ImmutableList;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.CompressionUtils;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobDescription;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.google.testing.junit.testparameterinjector.TestParameters;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** GCSBlob Dao test. */
@RunWith(TestParameterInjector.class)
public final class GCSBlobDaoTest {

  @Mock Storage mockStorage;
  @Mock Page<Blob> mockReturnedPage;
  @Mock Page<Blob> mockReturnedPage2;
  @Mock Blob mockBlob1;
  @Mock Blob mockBlob2;

  @Mock StorageBatch mockStorageBatch;
  @Mock StorageBatchResult<Boolean> mockStorageBatchResult;
  @Mock ReadChannel mockReadChannel;

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
  public void downloadAndDecompressIfNeeded_hasGzip_returnDecompressed() throws IOException {
    // arrange
    byte[] originalData = "HelloWorld11111111111111111111111111111111100000011".getBytes();
    byte[] compressedData = CompressionUtils.compressWithGzip(originalData);
    when(mockStorage.get(isA(BlobId.class))).thenReturn(mockBlob1);
    when(mockStorage.reader(isA(BlobId.class))).thenReturn(mockReadChannel);
    when(mockBlob1.getContentEncoding()).thenReturn("gzip");
    when(mockReadChannel.read(any(ByteBuffer.class)))
        .thenAnswer(
            invocation -> {
              ByteBuffer buffer = invocation.getArgument(0);
              buffer.put(compressedData, 0, compressedData.length);
              return compressedData.length;
            })
        .thenReturn(-1);

    // act
    byte[] content =
        blobDao.downloadAndDecompressIfNeeded(
            BlobDescription.builder().host("test-bucket").resourceObject("path/file").build());

    // assert
    assertThat(content).isEqualTo(originalData);
    verify(mockStorage, times(1)).get(BlobId.of("test-bucket", "path/file"));
    verify(mockBlob1, times(2)).getContentEncoding();
    verify(mockReadChannel, times(1)).setChunkSize(1024);
  }

  @Test
  public void downloadAndDecompressIfNeeded_emptyContentEncoding_returnOriginal() {
    // arrange
    when(mockStorage.get(isA(BlobId.class))).thenReturn(mockBlob1);
    when(mockBlob1.getContentEncoding()).thenReturn("");
    when(mockBlob1.getContent()).thenReturn(new byte[] {10, 20, 30});

    // act
    byte[] content =
        blobDao.downloadAndDecompressIfNeeded(
            BlobDescription.builder().host("test-bucket").resourceObject("path/file").build());

    // assert
    assertThat(content).isEqualTo(new byte[] {10, 20, 30});
    verify(mockStorage, times(1)).get(BlobId.of("test-bucket", "path/file"));
    verify(mockBlob1, times(1)).getContentEncoding();
    verify(mockBlob1, times(1)).getContent();
  }

  @Test
  public void downloadAndDecompressIfNeeded_nullContentEncoding_returnOriginal() {
    // arrange
    when(mockStorage.get(isA(BlobId.class))).thenReturn(mockBlob1);
    when(mockBlob1.getContentEncoding()).thenReturn(null);
    when(mockBlob1.getContent()).thenReturn(new byte[] {10, 20, 30});

    // act
    byte[] content =
        blobDao.downloadAndDecompressIfNeeded(
            BlobDescription.builder().host("test-bucket").resourceObject("path/file").build());

    // assert
    assertThat(content).isEqualTo(new byte[] {10, 20, 30});
    verify(mockStorage, times(1)).get(BlobId.of("test-bucket", "path/file"));
    verify(mockBlob1, times(1)).getContentEncoding();
    verify(mockBlob1, times(1)).getContent();
  }

  @Test
  public void downloadAndDecompressIfNeeded_unsupportedContentEncoding_returnOriginal() {
    // arrange
    when(mockStorage.get(isA(BlobId.class))).thenReturn(mockBlob1);
    when(mockBlob1.getContentEncoding()).thenReturn("unsupported");
    when(mockBlob1.getContent()).thenReturn(new byte[] {10, 20, 30});

    // act
    byte[] content =
        blobDao.downloadAndDecompressIfNeeded(
            BlobDescription.builder().host("test-bucket").resourceObject("path/file").build());

    // assert
    assertThat(content).isEqualTo(new byte[] {10, 20, 30});
    verify(mockStorage, times(1)).get(BlobId.of("test-bucket", "path/file"));
    verify(mockBlob1, times(3)).getContentEncoding();
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
  public void compressAndUpload_hasHostAndResourceObject_succeeded() throws IOException {
    // arrange
    byte[] content = new byte[] {10, 20, 30};
    BlobDescription blobDescription =
        BlobDescription.builder().host("test-bucket").resourceObject("path/file").build();
    BlobId blobId = BlobId.of("test-bucket", "path/file");
    when(mockStorage.createFrom(isA(BlobInfo.class), isA(ByteArrayInputStream.class)))
        .thenReturn(mockBlob1);

    // act
    blobDao.compressAndUpload(blobDescription, content);

    // assert
    ArgumentCaptor<ByteArrayInputStream> contentCaptor =
        ArgumentCaptor.forClass(ByteArrayInputStream.class);
    ArgumentCaptor<BlobInfo> blobInfoCaptor = ArgumentCaptor.forClass(BlobInfo.class);
    verify(mockStorage).createFrom(blobInfoCaptor.capture(), contentCaptor.capture());
    assertThat(blobInfoCaptor.getValue())
        .isEqualTo(BlobInfo.newBuilder(blobId).setContentEncoding("gzip").build());
    assertThat(contentCaptor.getValue().readAllBytes())
        .isEqualTo(CompressionUtils.compressWithGzip(content));
  }

  @Test
  public void checkExistsAndGzipContentIfNeeded_notExists_returnFalse() throws IOException {
    // arrange
    BlobDescription[] files =
        new BlobDescription[] {
          BlobDescription.builder().host("test-bucket").resourceObject("path/file").build()
        };
    when(mockStorage.get(isA(BlobId.class))).thenReturn(null);

    // act
    boolean result = blobDao.checkExistsAndGzipContentIfNeeded(files);

    // assert
    assertFalse(result);
  }

  @Test
  @TestParameters("{encoding: }")
  @TestParameters("{encoding: null}")
  public void checkExistsAndGzipContentIfNeeded_emptyOrNullEncoding_compressed(String encoding)
      throws IOException {
    // arrange
    byte[] content = new byte[] {10, 20, 30};
    BlobDescription[] files =
        new BlobDescription[] {
          BlobDescription.builder().host("test-bucket").resourceObject("path/file").build()
        };
    BlobId blobId = BlobId.of("test-bucket", "path/file");
    when(mockStorage.get(isA(BlobId.class))).thenReturn(mockBlob1);
    when(mockBlob1.getContent()).thenReturn(new byte[] {10, 20, 30});
    when(mockBlob1.getContentEncoding()).thenReturn(encoding);
    when(mockStorage.createFrom(isA(BlobInfo.class), isA(ByteArrayInputStream.class)))
        .thenReturn(mockBlob1);

    // act
    boolean result = blobDao.checkExistsAndGzipContentIfNeeded(files);

    // assert
    assertTrue(result);
    ArgumentCaptor<ByteArrayInputStream> contentCaptor =
        ArgumentCaptor.forClass(ByteArrayInputStream.class);
    ArgumentCaptor<BlobInfo> blobInfoCaptor = ArgumentCaptor.forClass(BlobInfo.class);
    verify(mockStorage).createFrom(blobInfoCaptor.capture(), contentCaptor.capture());
    assertThat(blobInfoCaptor.getValue())
        .isEqualTo(BlobInfo.newBuilder(blobId).setContentEncoding("gzip").build());
    assertThat(contentCaptor.getValue().readAllBytes())
        .isEqualTo(CompressionUtils.compressWithGzip(content));
  }

  @Test
  public void checkExistsAndGzipContentIfNeeded_hasException_returnFalse() throws IOException {
    // arrange
    byte[] content = new byte[] {10, 20, 30};
    BlobDescription[] files =
        new BlobDescription[] {
          BlobDescription.builder().host("test-bucket").resourceObject("path/file").build()
        };
    BlobId blobId = BlobId.of("test-bucket", "path/file");
    when(mockStorage.get(isA(BlobId.class))).thenReturn(mockBlob1);
    when(mockBlob1.getContent()).thenReturn(new byte[] {10, 20, 30});
    when(mockBlob1.getContentEncoding()).thenReturn(null);
    when(mockStorage.createFrom(isA(BlobInfo.class), isA(ByteArrayInputStream.class)))
        .thenThrow(new IOException("Failed to upload blob"));

    // act
    boolean result = blobDao.checkExistsAndGzipContentIfNeeded(files);

    // assert
    assertFalse(result);
    verify(mockStorage, times(1)).createFrom(isA(BlobInfo.class), isA(ByteArrayInputStream.class));
  }

  @Test
  public void checkExistsAndGzipContentIfNeeded_gzipEncoding_doNothingReturnTrue()
      throws IOException {
    // arrange
    BlobDescription[] files =
        new BlobDescription[] {
          BlobDescription.builder().host("test-bucket").resourceObject("path/file").build()
        };
    when(mockStorage.get(isA(BlobId.class))).thenReturn(mockBlob1);
    when(mockBlob1.getContent()).thenReturn(new byte[] {10, 20, 30});
    when(mockBlob1.getContentEncoding()).thenReturn("gzip");
    when(mockStorage.createFrom(isA(BlobInfo.class), isA(ByteArrayInputStream.class)))
        .thenReturn(mockBlob1);

    // act
    boolean result = blobDao.checkExistsAndGzipContentIfNeeded(files);

    // assert
    assertTrue(result);
    verify(mockBlob1, times(0)).getContent();
    verify(mockStorage, times(0)).createFrom(isA(BlobInfo.class), isA(ByteArrayInputStream.class));
  }

  @Test
  public void checkExistsAndGzipContentIfNeeded_unSupportedEncoding_returnFalse()
      throws IOException {
    // arrange
    BlobDescription[] files =
        new BlobDescription[] {
          BlobDescription.builder().host("test-bucket").resourceObject("path/file").build()
        };
    when(mockStorage.get(isA(BlobId.class))).thenReturn(mockBlob1);
    when(mockBlob1.getContent()).thenReturn(new byte[] {10, 20, 30});
    when(mockBlob1.getContentEncoding()).thenReturn("abc");
    when(mockStorage.createFrom(isA(BlobInfo.class), isA(ByteArrayInputStream.class)))
        .thenReturn(mockBlob1);

    // act
    boolean result = blobDao.checkExistsAndGzipContentIfNeeded(files);

    // assert
    assertFalse(result);
    verify(mockBlob1, times(0)).getContent();
    verify(mockStorage, times(0)).createFrom(isA(BlobInfo.class), isA(ByteArrayInputStream.class));
  }

  @Test
  public void compressAndUpload_hasUrl_Succeeded() throws IOException {
    // arrange
    byte[] content = new byte[] {10, 20, 30};
    BlobDescription blobDescription =
        BlobDescription.builder().url("gs://test-bucket/path/file").build();
    BlobId blobId = BlobId.fromGsUtilUri("gs://test-bucket/path/file");
    when(mockStorage.createFrom(isA(BlobInfo.class), isA(ByteArrayInputStream.class)))
        .thenReturn(mockBlob1);

    // act
    blobDao.compressAndUpload(blobDescription, content);

    // assert
    ArgumentCaptor<ByteArrayInputStream> contentCaptor =
        ArgumentCaptor.forClass(ByteArrayInputStream.class);
    ArgumentCaptor<BlobInfo> blobInfoCaptor = ArgumentCaptor.forClass(BlobInfo.class);
    verify(mockStorage).createFrom(blobInfoCaptor.capture(), contentCaptor.capture());
    assertThat(blobInfoCaptor.getValue())
        .isEqualTo(BlobInfo.newBuilder(blobId).setContentEncoding("gzip").build());
    assertThat(contentCaptor.getValue().readAllBytes())
        .isEqualTo(CompressionUtils.compressWithGzip(content));
  }

  @Test
  public void testExists_Succeeded() throws IOException {
    // arrange
    when(mockStorage.get(isA(BlobId.class))).thenReturn(mockBlob1);

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

  @Test
  public void delete_success_returnTrue() {
    // arrange
    when(mockBlob1.getBlobId()).thenReturn(BlobId.of("test-bucket-1", "us/35/17/d/session_1"));
    when(mockBlob2.getBlobId()).thenReturn(BlobId.of("test-bucket-1", "us/35/17/d/session_2/"));
    when(mockReturnedPage.iterateAll()).thenReturn(ImmutableList.of(mockBlob1, mockBlob2));
    when(mockStorage.list(anyString(), /* BlobListOption= */ any(), /* BlobListOption= */ any()))
        .thenReturn(mockReturnedPage);
    when(mockStorage.batch()).thenReturn(mockStorageBatch);
    when(mockStorageBatch.delete(any())).thenReturn(mockStorageBatchResult);
    doNothing().when(mockStorageBatch).submit();

    // act
    boolean result =
        blobDao.delete(
            BlobDescription.builder().host("test-bucket-1").resourceObject("us/35/17/d/").build());

    // assert
    assertTrue(result);
    verify(mockStorageBatch, times(1)).submit();
  }

  @Test
  public void delete_hasException_returnFalse() {
    // arrange
    when(mockBlob1.getBlobId()).thenReturn(BlobId.of("test-bucket-1", "us/35/17/d/session_1"));
    when(mockBlob2.getBlobId()).thenReturn(BlobId.of("test-bucket-1", "us/35/17/d/session_2/"));
    when(mockReturnedPage.iterateAll()).thenReturn(ImmutableList.of(mockBlob1, mockBlob2));
    when(mockStorage.list(anyString(), /* BlobListOption= */ any(), /* BlobListOption= */ any()))
        .thenReturn(mockReturnedPage);
    when(mockStorage.batch()).thenReturn(mockStorageBatch);
    when(mockStorageBatch.delete(any())).thenReturn(mockStorageBatchResult);
    doThrow(new StorageException(new IOException("Batch submit throws a storage exception")))
        .when(mockStorageBatch)
        .submit();

    // act
    boolean result =
        blobDao.delete(
            BlobDescription.builder().host("test-bucket-1").resourceObject("us/35/17/d/").build());

    // assert
    assertFalse(result);
    verify(mockStorageBatch, times(1)).submit();
  }
}
