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

package com.google.ondevicepersonalization.federatedcompute.shuffler.common;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class CompressionUtilsTest {
  @Test
  public void compressAndDecompress_success() {
    byte[] inputData = "HelloWorld11111111111111111111111111111111100000011".getBytes();

    byte[] compressed = CompressionUtils.compressWithGzip(inputData);
    assertTrue(inputData.length > compressed.length);

    byte[] uncompressed = CompressionUtils.uncompressWithGzip(compressed);
    assertArrayEquals(inputData, uncompressed);
  }

  @Test
  public void decompress_fail() {
    byte[] inputData = "HelloWorld11111111".getBytes();
    assertThrows(
        IllegalArgumentException.class, () -> CompressionUtils.uncompressWithGzip(inputData));
  }
}
