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

import com.google.cloud.Timestamp;
import com.google.common.base.Converter;
import java.time.Instant;

/** Converter between {@link Timestamp} and {@link Instant}. */
public final class TimestampInstantConverter extends Converter<Timestamp, Instant> {

  private TimestampInstantConverter() {}

  public static final Converter<Timestamp, Instant> TO_INSTANT = new TimestampInstantConverter();
  public static final Converter<Instant, Timestamp> TO_TIMESTAMP = TO_INSTANT.reverse();

  @Override
  protected Instant doForward(Timestamp timestamp) {
    return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
  }

  @Override
  protected Timestamp doBackward(Instant instant) {
    return Timestamp.ofTimeSecondsAndNanos(instant.getEpochSecond(), instant.getNano());
  }
}
