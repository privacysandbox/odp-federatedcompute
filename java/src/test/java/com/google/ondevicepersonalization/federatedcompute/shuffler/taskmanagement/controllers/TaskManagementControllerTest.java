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

package com.google.ondevicepersonalization.federatedcompute.shuffler.taskmanagement.controllers;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.ondevicepersonalization.federatedcompute.proto.CreateTaskRequest;
import com.google.ondevicepersonalization.federatedcompute.proto.CreateTaskResponse;
import com.google.ondevicepersonalization.federatedcompute.proto.GetTaskByIdResponse;
import com.google.ondevicepersonalization.federatedcompute.proto.Task;
import com.google.ondevicepersonalization.federatedcompute.proto.TaskStatus;
import com.google.ondevicepersonalization.federatedcompute.shuffler.taskmanagement.core.TaskManagementCore;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

@RunWith(JUnit4.class)
public final class TaskManagementControllerTest {

  private static final Timestamp TS1 = Timestamps.parseUnchecked("2023-07-21T11:19:42.12Z");
  private static final Timestamp TS2 = Timestamps.parseUnchecked("2023-07-22T11:19:42.12Z");
  private static final Timestamp TS3 = Timestamps.parseUnchecked("2023-07-21T11:30:42.12Z");
  private static final Timestamp TS4 = Timestamps.parseUnchecked("2023-07-21T11:35:42.12Z");

  private static final Task DEFAULT_TASK1 =
      Task.newBuilder()
          .setPopulationName("us")
          .setTaskId(15)
          .setTotalIteration(13)
          .setMinAggregationSize(100)
          .setMaxAggregationSize(200)
          .setStatus(TaskStatus.Enum.OPEN)
          .setMaxParallel(5)
          .setCorrelationId("abc")
          .setMinClientVersion("0.0.0.0")
          .setMaxClientVersion("0.0.0.1")
          .setStartTaskNoEarlierThan(TS1)
          .setDoNotCreateIterationAfter(TS2)
          .setStartedTime(TS3)
          .setStopTime(TS4)
          .build();

  private TaskManagementController controller;
  @Mock TaskManagementCore mockTaskCore;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    controller = new TaskManagementController(mockTaskCore);
  }

  @Test
  public void testGetTaskById_Succeeded() {
    // arrange
    when(mockTaskCore.getTaskById(anyString(), anyLong()))
        .thenReturn(
            Optional.of(
                Task.newBuilder()
                    .setPopulationName("jp")
                    .setTaskId(13)
                    .setCorrelationId("abc")
                    .build()));

    // act
    GetTaskByIdResponse response = controller.getTaskById("jp", 13);

    // asert
    assertNotNull(response);
    assertNotNull(response.getTask());
    assertThat(response.getTask())
        .isEqualTo(
            Task.newBuilder()
                .setPopulationName("jp")
                .setTaskId(13)
                .setCorrelationId("abc")
                .build());
    verify(mockTaskCore).getTaskById("jp", 13);
  }

  @Test
  public void testGetTaskById_NotFound() {
    // arrange
    when(mockTaskCore.getTaskById(anyString(), anyLong())).thenReturn(Optional.empty());
    ResponseStatusException expected =
        assertThrows(ResponseStatusException.class, () -> controller.getTaskById("jp", 13));
    assertThat(expected).hasMessageThat().contains("404 NOT_FOUND");
  }

  @Test
  public void testCreateTask() {
    // arrange
    when(mockTaskCore.createTask(any()))
        .thenReturn(DEFAULT_TASK1.newBuilder().setPopulationName("cn").setTaskId(999).build());

    // act
    CreateTaskResponse response =
        controller.createTask("cn", CreateTaskRequest.newBuilder().setTask(DEFAULT_TASK1).build());

    // assert
    assertThat(response)
        .isEqualTo(
            CreateTaskResponse.newBuilder()
                .setTask(DEFAULT_TASK1.newBuilder().setPopulationName("cn").setTaskId(999).build())
                .build());
    verify(mockTaskCore)
        .createTask(DEFAULT_TASK1.toBuilder().setPopulationName("cn").setTaskId(0).build());
  }

  @Test
  public void testReady() {
    // act and assert
    assertThat(controller.ready().contains("Management")).isTrue();
  }

  @Test
  public void testHealthz() {
    // act and assert
    assertThat(controller.healthz().contains("Management")).isTrue();
  }
}
