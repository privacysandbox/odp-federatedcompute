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

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/** Aggregation batch identifier. */
@Getter
@EqualsAndHashCode
@Builder(toBuilder = true)
public class AggregationBatchId {
  private String populationName;
  private long taskId;
  private long iterationId;
  private long attemptId;
  private String batchId;

  @Override
  public String toString() {
    return populationName + "/" + taskId + "/" + iterationId + "/" + attemptId + "/" + batchId;
  }
}
