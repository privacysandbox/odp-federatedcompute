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

import com.google.common.collect.ImmutableList;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskEntity.Status;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(TestParameterInjector.class)
public final class TaskEntityStatusTest {

  @Test
  public void testFromCodeConversion(
      @TestParameter({"0", "1", "2", "101", "102"}) Long statusCode) {
    switch (statusCode.intValue()) {
      case 0:
      case 1:
      case 2:
      case 101:
      case 102:
        Status status = Status.fromCode(statusCode);
        assertThat(statusCode).isEqualTo(status.code());
        break;

      default:
        IllegalArgumentException exception =
            assertThrows(IllegalArgumentException.class, () -> Status.fromCode(statusCode));
        assertThat(exception)
            .hasMessageThat()
            .contains(String.format("Invalid TaskEntity status code : %s", statusCode));
    }
  }

  @Test
  public void getActiveTaskStatus_expected() {
    // act
    ImmutableList<Status> activeStatus = Status.getActiveStatus();

    // assert
    assertThat(activeStatus).containsExactly(Status.OPEN, Status.CREATED);
  }
}
