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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TaskIdTest {

  @Test
  public void testToString() {
    // arrange
    TaskId taskId = TaskId.builder().populationName("name").taskId(123).build();

    // act
    String stringId = taskId.toString();

    // assert
    assertThat(stringId).isEqualTo("name/123");
  }

  @Test
  public void testBuilderAndEquals() {
    // arrange
    TaskId taskId1 = TaskId.builder().populationName("name").taskId(123).build();
    TaskId taskId2 = TaskId.builder().populationName("name").taskId(123).build();
    TaskId.TaskIdBuilder builder = taskId2.toBuilder();

    // act
    TaskId taskId3 = builder.build();

    // assert
    assertThat(taskId2).isEqualTo(taskId1);
    assertThat(taskId3).isEqualTo(taskId1);
    assertThat(taskId2.hashCode()).isEqualTo(taskId1.hashCode());

    // for test coverage only.
    assertThat(builder.toString()).isNotEmpty();
  }
}
