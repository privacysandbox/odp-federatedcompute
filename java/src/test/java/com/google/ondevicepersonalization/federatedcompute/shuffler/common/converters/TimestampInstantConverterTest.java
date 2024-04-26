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
package com.google.ondevicepersonalization.federatedcompute.shuffler.common.converters;

import static com.google.common.truth.Truth.assertThat;

import com.google.protobuf.Timestamp;
import java.time.Instant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class TimestampInstantConverterTest {

  @Test
  public void testTimestampToInstantConversion() {
    Timestamp timestamp = Timestamp.newBuilder().setSeconds(1).setNanos(1).build();

    Instant instant = TimestampInstantConverter.TO_INSTANT.convert(timestamp);

    assertThat(instant).isEqualTo(Instant.ofEpochSecond(1, 1));
  }

  @Test
  public void testInstantToTimestampConversion() {
    Instant instant = Instant.ofEpochSecond(1, 1);

    Timestamp timestamp = TimestampInstantConverter.TO_TIMESTAMP.convert(instant);

    assertThat(timestamp).isEqualTo(Timestamp.newBuilder().setSeconds(1).setNanos(1).build());
  }
}
