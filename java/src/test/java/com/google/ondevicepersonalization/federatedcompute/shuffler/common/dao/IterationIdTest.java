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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class IterationIdTest {

  @Test
  public void testToAndFromString() {
    // arrange
    IterationId id =
        IterationId.builder()
            .populationName("name")
            .taskId(123)
            .iterationId(3)
            .attemptId(4)
            .build();

    // act
    String stringId = id.toString();

    // assert
    assertThat(stringId).isEqualTo("name/123/3/4");

    assertThat(IterationId.fromString(stringId)).isEqualTo(id);
  }

  @Test
  public void testToAndFromStringInvalid() {
    assertThrows(IllegalArgumentException.class, () -> IterationId.fromString("bad/string"));
  }

  @Test
  public void testBuilderAndEquals() {
    // arrange
    IterationId id1 =
        IterationId.builder().populationName("a").taskId(1).iterationId(2).attemptId(3).build();
    IterationId id2 =
        IterationId.builder().populationName("a").taskId(1).iterationId(2).attemptId(3).build();
    IterationId.IterationIdBuilder builder = id2.toBuilder();

    // act
    IterationId id3 = builder.build();

    // assert
    assertThat(id2).isEqualTo(id1);
    assertThat(id3).isEqualTo(id1);
    assertThat(id2.hashCode()).isEqualTo(id1.hashCode());

    // for test coverage only.
    assertThat(builder.toString()).isNotEmpty();
  }
}
