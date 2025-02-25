/*
 * Copyright 2025 Google LLC
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

package com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class CheckInResultTest {

  @Test
  public void testAndEquals() {
    assertThat(CheckInResult.fromCode(1)).isEqualTo(CheckInResult.SUCCESS);
  }

  @Test
  public void testInvalidStatusCode() {

    // act
    IllegalArgumentException expected =
        assertThrows(IllegalArgumentException.class, () -> CheckInResult.fromCode(9999));

    // assert
    assertThat(expected).hasMessageThat().contains("9999");
  }
}
