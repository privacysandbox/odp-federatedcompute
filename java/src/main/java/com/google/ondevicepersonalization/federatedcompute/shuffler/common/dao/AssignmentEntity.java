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

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/** The assignment data entity. */
@Getter
@EqualsAndHashCode
@Builder(toBuilder = true)
public class AssignmentEntity {
  private String populationName;
  private long taskId;
  private long iterationId;
  private long attemptId;
  private String sessionId;
  private String correlationId;
  private Instant createdTime;
  private Status status;
  private long statusId;
  private String batchId;
  long baseIterationId;
  long baseOnResultId;
  long resultId;

  /** The assignment status. */
  public enum Status {
    // Active status
    ASSIGNED(0L),
    LOCAL_COMPLETED(1L),
    UPLOAD_COMPLETED(2L),

    // Inactive status
    CANCELED(101L),
    LOCAL_FAILED(102L),
    LOCAL_NOT_ELIGIBLE(103L),
    REMOTE_FAILED(104L),
    LOCAL_FAILED_EXAMPLE_GENERATION(105L),
    LOCAL_FAILED_MODEL_COMPUTATION(106L),
    LOCAL_FAILED_OPS_ERROR(107L),

    // Inactive caused by timeout
    LOCAL_TIMEOUT(151L),
    UPLOAD_TIMEOUT(152L);

    private final long code;

    private static final Map<Long, Status> statusMap = new HashMap<>();

    static {
      Stream.of(Status.values()).forEach(status -> statusMap.put(status.code(), status));
    }

    Status(long code) {
      this.code = code;
    }

    /** Returns {@link long} code for the {@link Status} enum. */
    public long code() {
      return code;
    }

    /** Returns {@link Status} for the given {@code code}. */
    public static Status fromCode(long code) {
      if (statusMap.containsKey(code)) {
        return statusMap.get(code);
      }

      throw new IllegalArgumentException(
          String.format("Invalid Assignment status code : %s", code));
    }
  }
}
