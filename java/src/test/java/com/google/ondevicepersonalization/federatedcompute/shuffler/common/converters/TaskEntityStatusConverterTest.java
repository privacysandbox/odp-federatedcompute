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
package com.google.ondevicepersonalization.federatedcompute.shuffler.common.converters;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

import com.google.ondevicepersonalization.federatedcompute.proto.TaskStatus;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.dao.TaskEntity.Status;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(TestParameterInjector.class)
public final class TaskEntityStatusConverterTest {

  @Test
  public void testBiDirectionalConversionFromTaskEntityStatusEnum(@TestParameter Status status) {
    switch (status) {
      case OPEN:
      case COMPLETED:
      case CREATED:
      case CANCELED:
      case FAILED:
        assertThat(
                TaskEntityStatusConverter.TO_TASK_ENTITY_STATUS.convert(
                    TaskEntityStatusConverter.TO_TASK_STATUS.convert(status)))
            .isEqualTo(status);
        break;
      default:
        fail("unknow status:" + status);
    }
  }

  @Test
  public void testBiDirectionalConversionFromTaskStatusEnum(
      @TestParameter TaskStatus.Enum taskStatus) {
    switch (taskStatus) {
      case OPEN:
      case COMPLETED:
      case CREATED:
      case CANCELED:
      case FAILED:
        assertThat(
                TaskEntityStatusConverter.TO_TASK_STATUS.convert(
                    TaskEntityStatusConverter.TO_TASK_ENTITY_STATUS.convert(taskStatus)))
            .isEqualTo(taskStatus);
        break;
      case UNKNOWN:
      case UNRECOGNIZED:
        IllegalArgumentException exception =
            assertThrows(
                IllegalArgumentException.class,
                () -> TaskEntityStatusConverter.TO_TASK_ENTITY_STATUS.convert(taskStatus));
        assertThat(exception)
            .hasMessageThat()
            .contains(String.format("Invalid TaskStatus proto enum : %s", taskStatus));
        break;
      default:
        fail("unknow status:" + taskStatus);
    }
  }
}
