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

package com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao;

import java.io.IOException;
import java.util.List;

/** The Dao of Blob storage */
public interface BlobDao {
  /**
   * List the objects under folder.
   *
   * @return The relative paths of the files under the folder.
   */
  public List<String> list(BlobDescription folder);

  /**
   * List the objects under folder appending the given partitions.
   *
   * @return The relative paths of the files under the folder that start with a String in
   *     partitions.
   */
  public List<String> listByPartition(BlobDescription folder, List<String> partitions);

  /** Download content from file. */
  public byte[] download(BlobDescription file);

  /** Upload content to file. */
  public void upload(BlobDescription file, byte[] content) throws IOException;

  /** Check if file exists from server given all uploaded replica. */
  public boolean exists(BlobDescription[] files);
}
