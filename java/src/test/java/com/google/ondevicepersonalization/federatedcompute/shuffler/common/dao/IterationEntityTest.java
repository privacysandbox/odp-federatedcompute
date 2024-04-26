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

import com.google.ondevicepersonalization.federatedcompute.proto.IterationInfo;
import com.google.ondevicepersonalization.federatedcompute.proto.TaskInfo;
import com.google.ondevicepersonalization.federatedcompute.proto.TrainingInfo;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class IterationEntityTest {

  @Test
  public void testInvalidStatusCode() {

    // act
    IllegalArgumentException expected =
        assertThrows(IllegalArgumentException.class, () -> IterationEntity.Status.fromCode(9999));

    // assert
    assertThat(expected).hasMessageThat().contains("9999");
  }

  @Test
  public void testBuilderAndEquals() {
    // arrange
    IterationEntity entity1 =
        IterationEntity.builder()
            .populationName("a")
            .taskId(1)
            .iterationId(2)
            .attemptId(3)
            .reportGoal(4)
            .status(IterationEntity.Status.COMPLETED)
            .baseIterationId(5)
            .baseOnResultId(6)
            .resultId(7)
            .assigned(Optional.of(8L))
            .info("iteration_info")
            .aggregationLevel(1)
            .build();
    IterationEntity entity2 =
        IterationEntity.builder()
            .populationName("a")
            .taskId(1)
            .iterationId(2)
            .attemptId(3)
            .reportGoal(4)
            .status(IterationEntity.Status.COMPLETED)
            .baseIterationId(5)
            .baseOnResultId(6)
            .resultId(7)
            .assigned(Optional.of(8L))
            .info("iteration_info")
            .aggregationLevel(1)
            .build();

    // act
    IterationEntity.IterationEntityBuilder builder = entity2.toBuilder();
    IterationEntity entity3 = builder.build();

    // assert
    assertThat(entity2).isEqualTo(entity1);
    assertThat(entity3).isEqualTo(entity1);
    assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());

    // for test coverage only
    assertThat(builder.toString()).isNotEmpty();
  }

  @Test
  public void getTrainingCheckpointIterationId_hasInvalidInfo_throwException() {
    // arrange
    IterationEntity entity =
        IterationEntity.builder()
            .populationName("a")
            .taskId(1)
            .iterationId(2)
            .attemptId(3)
            .reportGoal(4)
            .status(IterationEntity.Status.COMPLETED)
            .baseIterationId(5)
            .baseOnResultId(6)
            .resultId(7)
            .assigned(Optional.of(8L))
            .info("iteration_info")
            .aggregationLevel(0)
            .build();

    // act and assert
    IllegalStateException expected =
        assertThrows(IllegalStateException.class, () -> entity.getTrainingCheckpointIterationId());
    assertThat(expected).hasMessageThat().contains("iteration_info");
  }

  @Test
  public void getTrainingCheckpointIterationId_hasInfoNull_throwException() {
    // arrange
    IterationEntity entity =
        IterationEntity.builder()
            .populationName("a")
            .taskId(1)
            .iterationId(2)
            .attemptId(3)
            .reportGoal(4)
            .status(IterationEntity.Status.COMPLETED)
            .baseIterationId(5)
            .baseOnResultId(6)
            .resultId(7)
            .assigned(Optional.of(8L))
            .aggregationLevel(0)
            .build();

    // act and assert
    IllegalStateException expected =
        assertThrows(IllegalStateException.class, () -> entity.getTrainingCheckpointIterationId());
    assertThat(expected).hasMessageThat().contains("is null");
  }

  @Test
  public void getTrainingCheckpointIterationId_hasValidTrainingInfo_returnTrainingIterationId() {
    // arrange
    IterationEntity entity =
        IterationEntity.builder()
            .populationName("a")
            .taskId(1)
            .iterationId(2)
            .attemptId(3)
            .reportGoal(4)
            .status(IterationEntity.Status.COMPLETED)
            .baseIterationId(5)
            .baseOnResultId(6)
            .resultId(7)
            .assigned(Optional.of(8L))
            .info("{\"taskInfo\":{\"trafficWeight\":\"1\",\"trainingInfo\":{}}}")
            .aggregationLevel(0)
            .build();

    // act and assert
    assertThat(entity.getTrainingCheckpointIterationId())
        .isEqualTo(IterationId.builder().populationName("a").taskId(1).iterationId(6).build());
  }

  @Test
  public void getTrainingCheckpointIterationId_hasValidEvaluationInfo_returnTrainingIterationId() {
    // arrange
    IterationEntity entity =
        IterationEntity.builder()
            .populationName("a")
            .taskId(1)
            .iterationId(2)
            .attemptId(3)
            .reportGoal(4)
            .status(IterationEntity.Status.COMPLETED)
            .baseIterationId(5)
            .baseOnResultId(6)
            .resultId(7)
            .assigned(Optional.of(8L))
            .info(
                "{\"evaluationTrainingIterationId\":\"222\",\"taskInfo\":{\"evaluationInfo\":"
                    + "{\"checkPointSelector\":{\"iterationSelector\":{\"size\":\"1\"}},"
                    + "\"trainingPopulationName\":\"b\",\"trainingTaskId\":\"111\"},"
                    + "\"trafficWeight\":\"1\"}}\n")
            .aggregationLevel(0)
            .build();

    // act and assert
    assertThat(entity.getTrainingCheckpointIterationId())
        .isEqualTo(IterationId.builder().populationName("b").taskId(111).iterationId(222).build());
  }

  @Test
  public void getIterationInfo_hasValidInfo_success() {
    // arrange
    IterationEntity entity =
        IterationEntity.builder()
            .populationName("a")
            .taskId(1)
            .iterationId(2)
            .attemptId(3)
            .reportGoal(4)
            .status(IterationEntity.Status.COMPLETED)
            .baseIterationId(5)
            .baseOnResultId(6)
            .resultId(7)
            .assigned(Optional.of(8L))
            .info("{\"taskInfo\":{\"trafficWeight\":\"1\",\"trainingInfo\":{}}}")
            .aggregationLevel(0)
            .build();

    // act
    IterationInfo iterationInfo = entity.getIterationInfo();

    // assert
    assertThat(iterationInfo)
        .isEqualTo(
            IterationInfo.newBuilder()
                .setTaskInfo(
                    TaskInfo.newBuilder()
                        .setTrafficWeight(1)
                        .setTrainingInfo(TrainingInfo.getDefaultInstance()))
                .build());
  }

  @Test
  public void getIterationInfo_hasInvalidInfo_throwException() {
    // arrange
    IterationEntity entity =
        IterationEntity.builder()
            .populationName("a")
            .taskId(1)
            .iterationId(2)
            .attemptId(3)
            .reportGoal(4)
            .status(IterationEntity.Status.COMPLETED)
            .baseIterationId(5)
            .baseOnResultId(6)
            .resultId(7)
            .assigned(Optional.of(8L))
            .info("iteration_info")
            .aggregationLevel(0)
            .build();

    // act and assert
    IllegalStateException expected =
        assertThrows(IllegalStateException.class, () -> entity.getIterationInfo());
    assertThat(expected).hasMessageThat().contains("iteration_info");
  }
}
