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

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/** Device check-in result for creating an Assignment. */
public enum CheckInResult {
  // Leave room between all results in-case new ones are added.
  // Successful check-in
  SUCCESS(1L),

  // Check-in failures
  ITERATION_FULL(101L),
  ITERATION_NOT_OPEN(110L),
  CLIENT_VERSION_MISMATCH(120L),
  ITERATION_NOT_ACTIVE(130L);

  private final long code;

  private static final Map<Long, CheckInResult> statusMap = new HashMap<>();

  static {
    Stream.of(CheckInResult.values()).forEach(status -> statusMap.put(status.code(), status));
  }

  CheckInResult(long code) {
    this.code = code;
  }

  /** Returns {@link long} code for the {@link CheckInResult} enum. */
  public long code() {
    return code;
  }

  /** Returns {@link CheckInResult} for the given {@code code}. */
  public static CheckInResult fromCode(long code) {
    if (statusMap.containsKey(code)) {
      return statusMap.get(code);
    }

    throw new IllegalArgumentException(
        String.format("Invalid CheckInResult status code : %s", code));
  }
}
