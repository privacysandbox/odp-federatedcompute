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
import static org.junit.Assert.assertThrows;

import com.google.ondevicepersonalization.federatedcompute.proto.TaskInfo;
import com.google.ondevicepersonalization.federatedcompute.proto.TrainingInfo;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.ProtoParser;
import java.time.Instant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TaskEntityTest {
  private static Instant NOW = Instant.parse("2023-09-01T00:00:00Z");

  private static final TaskInfo DEFAULT_TASK_INFO =
      TaskInfo.newBuilder()
          .setTrafficWeight(1)
          .setTrainingInfo(TrainingInfo.getDefaultInstance())
          .build();
  private static final String DEFAULT_TASK_INFO_STRING =
      ProtoParser.toJsonString(DEFAULT_TASK_INFO);

  private static final TaskEntity DEFAULT_TASK_ENTITY =
      TaskEntity.builder()
          .populationName("name")
          .taskId(123)
          .totalIteration(2)
          .minAggregationSize(3)
          .maxAggregationSize(4)
          .status(TaskEntity.Status.COMPLETED)
          .startTaskNoEarlierThan(NOW)
          .doNotCreateIterationAfter(NOW)
          .maxParallel(5)
          .correlationId("c")
          .minClientVersion("0")
          .maxClientVersion("9")
          .startedTime(NOW)
          .stopTime(NOW)
          .createdTime(NOW)
          .info(DEFAULT_TASK_INFO_STRING)
          .build();

  @Test
  public void testBuilderAndEquals() {
    // arrange
    TaskEntity task1 =
        TaskEntity.builder()
            .populationName("name")
            .taskId(123)
            .totalIteration(2)
            .minAggregationSize(3)
            .maxAggregationSize(4)
            .status(TaskEntity.Status.COMPLETED)
            .startTaskNoEarlierThan(NOW)
            .doNotCreateIterationAfter(NOW)
            .maxParallel(5)
            .correlationId("c")
            .minClientVersion("0")
            .maxClientVersion("9")
            .startedTime(NOW)
            .stopTime(NOW)
            .createdTime(NOW)
            .info(DEFAULT_TASK_INFO_STRING)
            .build();
    TaskEntity.TaskEntityBuilder builder = task1.toBuilder();

    // act
    TaskEntity task2 = builder.build();

    // assert
    assertThat(task1).isEqualTo(DEFAULT_TASK_ENTITY);
    assertThat(task2).isEqualTo(DEFAULT_TASK_ENTITY);
    assertThat(task1.hashCode()).isEqualTo(DEFAULT_TASK_ENTITY.hashCode());

    // for test coverage only.
    assertThat(builder.toString()).isNotEmpty();
  }

  @Test
  public void getProtoInfo_validInfoString_success() {
    // arrange and act
    TaskInfo taskInfo = DEFAULT_TASK_ENTITY.getProtoInfo();

    // assert
    assertThat(taskInfo)
        .isEqualTo(
            TaskInfo.newBuilder()
                .setTrafficWeight(1)
                .setTrainingInfo(TrainingInfo.getDefaultInstance())
                .build());
  }

  @Test
  public void getProtoInfo_inValidInfoString_throwException() {
    // arrange and act
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> DEFAULT_TASK_ENTITY.toBuilder().info("random_string").build().getProtoInfo());

    // assert
    assertThat(exception)
        .hasMessageThat()
        .contains("Expect message object but got: \"random_string\"");
  }

  @Test
  public void getId_success() {
    // arrange and act
    TaskId taskId = DEFAULT_TASK_ENTITY.getId();

    // assert
    assertThat(taskId).isEqualTo(TaskId.builder().populationName("name").taskId(123).build());
  }
}
