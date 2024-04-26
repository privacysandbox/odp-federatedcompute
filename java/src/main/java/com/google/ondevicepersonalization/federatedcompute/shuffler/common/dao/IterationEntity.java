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

import com.google.ondevicepersonalization.federatedcompute.proto.EvaluationInfo;
import com.google.ondevicepersonalization.federatedcompute.proto.IterationInfo;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.ProtoParser;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/** An iteration entity */
@Getter
@EqualsAndHashCode
@Builder(toBuilder = true)
public class IterationEntity {

  private String populationName;
  private long taskId;
  private long iterationId;
  private long attemptId;
  private long reportGoal;
  private Status status;
  private long baseIterationId;
  private long baseOnResultId;
  private long resultId;
  private Optional<Long> assigned;
  private String info;
  private long aggregationLevel;

  public IterationId getId() {
    return IterationId.builder()
        .populationName(populationName)
        .taskId(taskId)
        .iterationId(iterationId)
        .attemptId(attemptId)
        .build();
  }

  /**
   * Returns an IterationId representing the training checkpoint associated with this iteration.
   *
   * @return an IterationId representing the training checkpoint
   * @throws IllegalStateException if evaluation information cannot be extracted from the info
   */
  public IterationId getTrainingCheckpointIterationId() {
    IterationInfo iterationInfo = getIterationInfo();
    if (iterationInfo.getTaskInfo().hasEvaluationInfo()) {
      EvaluationInfo evaluationInfo = iterationInfo.getTaskInfo().getEvaluationInfo();
      return IterationId.builder()
          .populationName(evaluationInfo.getTrainingPopulationName())
          .taskId(evaluationInfo.getTrainingTaskId())
          .iterationId(iterationInfo.getEvaluationTrainingIterationId())
          .build();
    }

    return IterationId.builder()
        .populationName(populationName)
        .taskId(taskId)
        .iterationId(baseOnResultId)
        .build();
  }

  /**
   * Returns an IterationInfo proto parsed from info field.
   *
   * @return an IterationInfo proto
   * @throws IllegalStateException if IterationInfo information cannot be extracted from the info
   */
  public IterationInfo getIterationInfo() {
    return ProtoParser.toProto(info, IterationInfo.getDefaultInstance());
  }

  /** The iteration status. */
  public enum Status {
    // active
    COLLECTING(0L),
    AGGREGATING(1L),
    COMPLETED(2L),
    STOPPED(3L),
    APPLYING(4L),

    // inactive
    CANCELED(101L),
    AGGREGATING_FAILED(102L),
    APPLYING_FAILED(103L);

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
          String.format("Invalid IterationEntity status code : %s", code));
    }
  }
}
