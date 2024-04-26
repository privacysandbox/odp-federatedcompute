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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import java.time.Instant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AggregationBatchEntityTest {
  private static Instant NOW = Instant.parse("2023-09-01T00:00:00Z");

  @Test
  public void testBuilderAndEquals() {

    // act
    AggregationBatchEntity entity1 =
        AggregationBatchEntity.builder()
            .populationName("a")
            .taskId(1)
            .iterationId(2)
            .attemptId(3)
            .batchId("b")
            .aggregationLevel(1)
            .batchSize(200)
            .createdTime(NOW)
            .status(AggregationBatchEntity.Status.PUBLISH_COMPLETED)
            .aggregatedBy("batch")
            .createdByPartition("1")
            .build();
    AggregationBatchEntity entity2 =
        AggregationBatchEntity.builder()
            .populationName("a")
            .taskId(1)
            .iterationId(2)
            .attemptId(3)
            .batchId("b")
            .aggregationLevel(1)
            .batchSize(200)
            .createdTime(NOW)
            .status(AggregationBatchEntity.Status.PUBLISH_COMPLETED)
            .aggregatedBy("batch")
            .createdByPartition("1")
            .build();

    AggregationBatchEntity.AggregationBatchEntityBuilder builder = entity2.toBuilder();
    AggregationBatchEntity entity3 = builder.build();

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
        assertThrows(
            IllegalArgumentException.class, () -> AggregationBatchEntity.Status.fromCode(9999));

    // assert
    assertThat(expected).hasMessageThat().contains("9999");
  }
}
