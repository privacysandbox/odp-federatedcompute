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

import com.google.ondevicepersonalization.federatedcompute.proto.IterationInfo;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.ProtoParser;

/** A helper class of Task Entity */
public class TaskEntities {
  /** Create a base iteration that is not stored in database. * */
  public static IterationEntity createBaseIteration(TaskEntity task) {
    return IterationEntity.builder()
        .populationName(task.getPopulationName())
        .taskId(task.getTaskId())
        .iterationId(0)
        .attemptId(0)
        .reportGoal(task.getMinAggregationSize())
        .status(IterationEntity.Status.COLLECTING)
        .baseIterationId(0)
        .baseOnResultId(0)
        .resultId(0)
        .info(createBaseIterationInfoString(task))
        .aggregationLevel(0)
        .build();
  }

  private static String createBaseIterationInfoString(TaskEntity taskEntity) {
    return ProtoParser.toJsonString(
        IterationInfo.newBuilder().setTaskInfo(taskEntity.getProtoInfo()).build());
  }

  private TaskEntities() {}
}
