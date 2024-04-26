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

import com.google.cloud.Timestamp;
import java.time.Instant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class TimestampInstantConverterTest {

  @Test
  public void testTimestampToInstantConversion() {
    Timestamp timestamp = Timestamp.ofTimeSecondsAndNanos(123, 456);

    Instant instant = TimestampInstantConverter.TO_INSTANT.convert(timestamp);

    assertThat(instant).isEqualTo(Instant.ofEpochSecond(123, 456));
  }

  @Test
  public void testInstantToTimestampConversion() {
    Instant instant = Instant.ofEpochSecond(222, 333);

    Timestamp timestamp = TimestampInstantConverter.TO_TIMESTAMP.convert(instant);

    assertThat(timestamp).isEqualTo(Timestamp.ofTimeSecondsAndNanos(222, 333));
  }

  @Test
  public void testTimestampToInstantConversion_back() {
    // arrange
    Timestamp timestamp = Timestamp.ofTimeSecondsAndNanos(123, 456);

    // act
    Timestamp back =
        TimestampInstantConverter.TO_TIMESTAMP.convert(
            TimestampInstantConverter.TO_INSTANT.convert(timestamp));

    // assert
    assertThat(back).isEqualTo(timestamp);
  }

  @Test
  public void testInstantToTimestampConversion_back() {
    Instant instant = Instant.ofEpochSecond(222, 333);

    Instant back =
        TimestampInstantConverter.TO_INSTANT.convert(
            TimestampInstantConverter.TO_TIMESTAMP.convert(instant));
    assertThat(back).isEqualTo(instant);
  }
}
