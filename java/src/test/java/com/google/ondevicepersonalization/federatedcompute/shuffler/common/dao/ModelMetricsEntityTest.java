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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ModelMetricsEntityTest {

  @Test
  public void testBuilderAndEquals() {
    // act
    ModelMetricsEntity entity1 =
        ModelMetricsEntity.builder()
            .populationName("us")
            .taskId(111)
            .iterationId(222)
            .metricName("loss")
            .metricValue(1.0)
            .build();
    ModelMetricsEntity entity2 =
        ModelMetricsEntity.builder()
            .populationName("us")
            .taskId(111)
            .iterationId(222)
            .metricName("loss")
            .metricValue(1.0)
            .build();
    ModelMetricsEntity.ModelMetricsEntityBuilder builder = entity2.toBuilder();
    ModelMetricsEntity entity3 = builder.build();

    // assert
    assertThat(entity1).isEqualTo(entity2);
    assertThat(entity1).isEqualTo(entity3);
    assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());

    // for test coverage only.
    assertThat(builder.toString()).isNotEmpty();
  }
}
