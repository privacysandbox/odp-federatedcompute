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

import static com.google.common.truth.Truth.assertThat;

import com.google.ondevicepersonalization.federatedcompute.proto.IterationInfo;
import com.google.ondevicepersonalization.federatedcompute.proto.TaskInfo;
import com.google.ondevicepersonalization.federatedcompute.proto.TrainingInfo;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.ProtoParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TaskEntitiesTest {

  private static final TaskInfo DEFAULT_TASK_INFO =
      TaskInfo.newBuilder()
          .setTrafficWeight(1)
          .setTrainingInfo(TrainingInfo.getDefaultInstance())
          .build();
  private static final String DEFAULT_TASK_INFO_STRING =
      ProtoParser.toJsonString(DEFAULT_TASK_INFO);
  private static final IterationInfo DEFAULT_ITERATION_INFO =
      IterationInfo.newBuilder().setTaskInfo(DEFAULT_TASK_INFO).build();
  private static final String DEFAULT_ITERATION_INFO_STRING =
      ProtoParser.toJsonString(DEFAULT_ITERATION_INFO);

  @Test
  public void testCreateBaseIteration() {
    // act and assert
    assertThat(
            TaskEntities.createBaseIteration(
                TaskEntity.builder()
                    .populationName("some name")
                    .taskId(123)
                    .totalIteration(999)
                    .minAggregationSize(100)
                    .maxAggregationSize(1000)
                    .minClientVersion("0")
                    .maxClientVersion("9")
                    .status(TaskEntity.Status.CREATED)
                    .info(DEFAULT_TASK_INFO_STRING)
                    .build()))
        .isEqualTo(
            IterationEntity.builder()
                .populationName("some name")
                .taskId(123)
                .iterationId(0)
                .attemptId(0)
                .reportGoal(100)
                .baseIterationId(0)
                .baseOnResultId(0)
                .status(IterationEntity.Status.COLLECTING)
                .info(DEFAULT_ITERATION_INFO_STRING)
                .aggregationLevel(0)
                .maxAggregationSize(1000)
                .minClientVersion("0")
                .maxClientVersion("9")
                .build());
  }
}
