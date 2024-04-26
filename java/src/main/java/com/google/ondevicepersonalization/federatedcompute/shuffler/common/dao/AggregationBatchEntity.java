// Copyright 2024 Google LLC
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

/** The aggregation batch data entity. */
@Getter
@EqualsAndHashCode
@Builder(toBuilder = true)
public class AggregationBatchEntity {
  private String populationName;
  private long taskId;
  private long iterationId;
  private long attemptId;
  private String batchId;
  private long aggregationLevel;
  private long batchSize;
  private String createdByPartition;
  private Instant createdTime;
  private Status status;
  private String aggregatedBy;

  public AggregationBatchEntity getId() {
    return AggregationBatchEntity.builder()
        .populationName(populationName)
        .taskId(taskId)
        .iterationId(iterationId)
        .attemptId(attemptId)
        .batchId(batchId)
        .build();
  }

  /** The AggregationBatch status. */
  public enum Status {
    // Active status
    FULL(1L),
    PUBLISH_COMPLETED(2L),
    UPLOAD_COMPLETED(3L),

    // Inactive status
    FAILED(101L);
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
          String.format("Invalid AggregationBatch status code : %s", code));
    }
  }
}
