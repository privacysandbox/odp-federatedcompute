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

package com.google.ondevicepersonalization.federatedcompute.shuffler.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class CompressionUtils {
  private static final int BUFFER_SIZE = 1024;

  private CompressionUtils() {}

  /** Uncompresses the input data using Gzip. */
  public static byte[] uncompressWithGzip(byte[] data) {
    try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
        GZIPInputStream gzip = new GZIPInputStream(inputStream);
        ByteArrayOutputStream result = new ByteArrayOutputStream()) {
      int length;
      byte[] buffer = new byte[BUFFER_SIZE];
      while ((length = gzip.read(buffer, 0, BUFFER_SIZE)) > 0) {
        result.write(buffer, 0, length);
      }
      return result.toByteArray();
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to uncompress using Gzip", e);
    }
  }

  /** Compresses the input data using Gzip. */
  public static byte[] compressWithGzip(byte[] data) {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream)) {
      gzipOutputStream.write(data);
      gzipOutputStream.finish();
      return outputStream.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to compress using Gzip", e);
    }
  }

  public enum CompressionFormat {
    NONE,
    GZIP,
  }
}
