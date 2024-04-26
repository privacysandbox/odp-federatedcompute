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

import com.google.common.collect.ImmutableList;
import com.google.ondevicepersonalization.federatedcompute.proto.TaskInfo;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.ProtoParser;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/** An entity class representing the Task from database. */
@Getter
@EqualsAndHashCode
@Builder(toBuilder = true)
public class TaskEntity {
  private String populationName;
  private long taskId;
  private long totalIteration;
  private long minAggregationSize;
  private long maxAggregationSize;
  private Status status;
  private Instant startTaskNoEarlierThan;
  private Instant doNotCreateIterationAfter;
  private long maxParallel;
  private String correlationId;
  private String minClientVersion;
  private String maxClientVersion;
  private Instant startedTime;
  private Instant stopTime;
  private Instant createdTime;
  private String info;

  public TaskId getId() {
    return TaskId.builder().populationName(populationName).taskId(taskId).build();
  }

  /**
   * Converts the internal 'info' string into a TaskInfo protocol buffer representation.
   *
   * @return A TaskInfo protocol buffer object representing the provided 'info'.
   * @throws IllegalStateException If the conversion from 'info' to TaskInfo fails.
   */
  public TaskInfo getProtoInfo() {
    return ProtoParser.toProto(info, TaskInfo.getDefaultInstance());
  }

  /** The task status. */
  public enum Status {
    // Active
    OPEN(0L),
    CREATED(2L),

    // Terminated
    COMPLETED(1L),
    CANCELED(101L),
    FAILED(102L);

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
          String.format("Invalid TaskEntity status code : %s", code));
    }

    /** Return all active {@link Status}. */
    public static ImmutableList<Status> getActiveStatus() {
      return ImmutableList.of(Status.CREATED, Status.OPEN);
    }
  }
}
