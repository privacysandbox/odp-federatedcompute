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

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobDao;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.BlobDescription;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Google Cloud Storage Blob Dao */
@Component
public class GCSBlobDao implements BlobDao {
  private static final Logger logger = LoggerFactory.getLogger(GCSBlobDao.class);
  private Storage storage;

  private ListeningExecutorService executorService;

  public GCSBlobDao(Storage storage, Optional<ListeningExecutorService> executorService) {
    this.storage = storage;
    this.executorService = executorService.orElse(MoreExecutors.newDirectExecutorService());
  }

  public List<String> listByPartition(BlobDescription folder, List<String> partitionPrefixes) {
    try {
      List<ListenableFuture<List<String>>> futures = new ArrayList<>();
      for (String prefix : partitionPrefixes) {
        futures.add(
            executorService.submit(
                () ->
                    list(
                            folder.toBuilder()
                                .resourceObject(folder.getResourceObject() + prefix)
                                .build())
                        .stream()
                        .map((fileName) -> prefix + fileName)
                        .collect(Collectors.toList())));
      }
      return Futures.successfulAsList(futures).get().stream()
          .flatMap(List::stream)
          .collect(Collectors.toList());
    } catch (Exception e) {
      logger.error("Failed to list blobs");
      throw new RuntimeException("Failed to list blobs", e);
    }
  }

  public List<String> list(BlobDescription folder) {
    Page<Blob> blobs =
        storage.list(
            folder.getHost(),
            Storage.BlobListOption.prefix(folder.getResourceObject()),
            Storage.BlobListOption.currentDirectory());
    ImmutableList.Builder<String> result = ImmutableList.builder();

    for (Blob blob : blobs.iterateAll()) {
      result.add(blob.getName().substring(folder.getResourceObject().length()));
    }
    return result.build();
  }

  public byte[] download(BlobDescription file) {
    Blob blob = storage.get(getBlobId(file));
    return blob.getContent();
  }

  public void upload(BlobDescription file, byte[] content) throws IOException {
    BlobId blobId = getBlobId(file);
    BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
    storage.createFrom(blobInfo, new ByteArrayInputStream(content));
  }

  private BlobId getBlobId(BlobDescription file) {
    BlobId blobId;
    // Try to use URL first if available
    if (Strings.isNullOrEmpty(file.getUrl())) {
      blobId = BlobId.of(file.getHost(), file.getResourceObject());
    } else {
      blobId = BlobId.fromGsUtilUri(file.getUrl());
    }
    return blobId;
  }

  public boolean exists(BlobDescription[] files) {
    return Arrays.stream(files)
        .allMatch(file -> storage.get(BlobId.of(file.getHost(), file.getResourceObject())) != null);
  }
}
