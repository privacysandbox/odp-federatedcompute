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

import com.google.common.base.Preconditions;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/** Iteration identifier. */
@Getter
@EqualsAndHashCode
@Builder(toBuilder = true)
public class IterationId {
  private String populationName;
  private long taskId;
  private long iterationId;
  private long attemptId;

  @Override
  public String toString() {
    return populationName + "/" + taskId + "/" + iterationId + "/" + attemptId;
  }

  /**
   * Converts an IterationId String to an instance of IterationId.
   *
   * <p>Example: - String: "pop_name/123/3/4" - IterationId: - populationName = "pop_name" - taskId
   * = 123 - iterationId = 3 - attemptId = 4
   */
  public static IterationId fromString(String iterationIdStr) {
    String[] iterArr = iterationIdStr.split("/");
    try {
      Preconditions.checkArgument(iterArr.length == 4 && !iterArr[0].isEmpty());
      return IterationId.builder()
          .populationName(iterArr[0])
          .taskId(Long.parseLong(iterArr[1]))
          .iterationId(Long.parseLong(iterArr[2]))
          .attemptId(Long.parseLong(iterArr[3]))
          .build();
    } catch (Exception e) {
      throw new IllegalArgumentException(
          "Failed to convert string " + iterationIdStr + " to IterationId.", e);
    }
  }
}
