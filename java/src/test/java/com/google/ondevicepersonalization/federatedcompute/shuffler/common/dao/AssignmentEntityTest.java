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

import java.time.Instant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AssignmentEntityTest {
  private static Instant NOW = Instant.parse("2023-09-01T00:00:00Z");

  @Test
  public void testBuilderAndEquals() {

    // act
    AssignmentEntity entity1 =
        AssignmentEntity.builder()
            .populationName("a")
            .taskId(1)
            .iterationId(2)
            .attemptId(3)
            .sessionId("b")
            .correlationId("c")
            .createdTime(NOW)
            .status(AssignmentEntity.Status.CANCELED)
            .statusId(4)
            .baseIterationId(5)
            .baseOnResultId(6)
            .resultId(7)
            .batchId("batch")
            .build();
    AssignmentEntity entity2 =
        AssignmentEntity.builder()
            .populationName("a")
            .taskId(1)
            .iterationId(2)
            .attemptId(3)
            .sessionId("b")
            .correlationId("c")
            .createdTime(NOW)
            .status(AssignmentEntity.Status.CANCELED)
            .statusId(4)
            .baseIterationId(5)
            .baseOnResultId(6)
            .resultId(7)
            .batchId("batch")
            .build();

    AssignmentEntity.AssignmentEntityBuilder builder = entity2.toBuilder();
    AssignmentEntity entity3 = builder.build();

    // assert
    assertThat(entity1).isEqualTo(entity2);
    assertThat(entity1).isEqualTo(entity3);
    assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());

    // for test coverage only.
    assertThat(builder.toString()).isNotEmpty();
  }

  @Test
  public void testInvalidStatusCode() {

    // act
    IllegalArgumentException expected =
        assertThrows(IllegalArgumentException.class, () -> AssignmentEntity.Status.fromCode(9999));

    // assert
    assertThat(expected).hasMessageThat().contains("9999");
  }
}
