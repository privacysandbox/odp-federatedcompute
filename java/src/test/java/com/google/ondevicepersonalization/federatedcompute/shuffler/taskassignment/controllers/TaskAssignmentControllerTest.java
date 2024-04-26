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

package com.google.ondevicepersonalization.federatedcompute.shuffler.taskassignment.controllers;

import static com.google.common.truth.Truth.assertThat;
import static com.google.ondevicepersonalization.federatedcompute.shuffler.taskassignment.controllers.TaskAssignmentController.CREATE_TASK_ASSIGNMENT_TIMER_NAME;
import static com.google.ondevicepersonalization.federatedcompute.shuffler.taskassignment.controllers.TaskAssignmentController.POPULATION_TAG;
import static com.google.ondevicepersonalization.federatedcompute.shuffler.taskassignment.controllers.TaskAssignmentController.REPORT_RESULT_TIMER_NAME;
import static com.google.ondevicepersonalization.federatedcompute.shuffler.taskassignment.controllers.TaskAssignmentController.RESULT_CREATED;
import static com.google.ondevicepersonalization.federatedcompute.shuffler.taskassignment.controllers.TaskAssignmentController.RESULT_TAG;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.internal.federatedcompute.v1.ClientVersion;
import com.google.internal.federatedcompute.v1.RejectionInfo;
import com.google.internal.federatedcompute.v1.RejectionReason;
import com.google.internal.federatedcompute.v1.RetryWindow;
import com.google.ondevicepersonalization.federatedcompute.proto.CreateTaskAssignmentRequest;
import com.google.ondevicepersonalization.federatedcompute.proto.CreateTaskAssignmentResponse;
import com.google.ondevicepersonalization.federatedcompute.proto.ReportResultRequest;
import com.google.ondevicepersonalization.federatedcompute.proto.ReportResultRequest.Result;
import com.google.ondevicepersonalization.federatedcompute.proto.ReportResultResponse;
import com.google.ondevicepersonalization.federatedcompute.proto.TaskAssignment;
import com.google.ondevicepersonalization.federatedcompute.proto.UploadInstruction;
import com.google.ondevicepersonalization.federatedcompute.shuffler.taskassignment.core.TaskAssignmentCore;
import com.google.protobuf.Duration;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

@RunWith(JUnit4.class)
public final class TaskAssignmentControllerTest {

  private static final String DEFAULT_SESSION_ID = "session-123";
  private static final TaskAssignment DEFAULT_TASK_ASSIGNMENT =
      TaskAssignment.newBuilder()
          .setAssignmentId(DEFAULT_SESSION_ID)
          .setAggregationId("/population/us/task/13/iteration/9/attempt/2")
          .setTaskName("/population/us/task/13")
          .build();
  private static final CreateTaskAssignmentRequest REQUEST =
      CreateTaskAssignmentRequest.newBuilder()
          .setClientVersion(ClientVersion.newBuilder().setVersionCode("1.2.3.4"))
          .build();
  @Mock TaskAssignmentCore mockCore;
  private TaskAssignmentController controller;
  private MeterRegistry meterRegistry;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    meterRegistry = new SimpleMeterRegistry();
    controller = new TaskAssignmentController(mockCore, meterRegistry);
  }

  @Test
  public void testTimerMetrics_Success() {
    // arrange
    when(mockCore.createTaskAssignment(anyString(), anyString(), anyString()))
        .thenReturn(Optional.of(DEFAULT_TASK_ASSIGNMENT));

    // act
    ResponseEntity<CreateTaskAssignmentResponse> response =
        controller.createTaskAssignment("us", REQUEST);

    // assert
    Timer timer = meterRegistry.find(CREATE_TASK_ASSIGNMENT_TIMER_NAME).timer();
    assertThat(timer.count()).isEqualTo(1);
    assertThat(timer.getId().getTag(POPULATION_TAG)).isEqualTo("us");
    assertThat(timer.getId().getTag(RESULT_TAG)).isEqualTo(RESULT_CREATED);
  }

  @Test
  public void testCreateTaskAssignment_Success() {
    // arrange
    when(mockCore.createTaskAssignment(anyString(), anyString(), anyString()))
        .thenReturn(Optional.of(DEFAULT_TASK_ASSIGNMENT));

    // act
    ResponseEntity<CreateTaskAssignmentResponse> response =
        controller.createTaskAssignment("us", REQUEST);

    // assert
    assertThat(response.getBody())
        .isEqualTo(
            CreateTaskAssignmentResponse.newBuilder()
                .setTaskAssignment(DEFAULT_TASK_ASSIGNMENT)
                .build());
    assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(201));
    verify(mockCore)
        .createTaskAssignment(
            /* populationName= */ "us", /* clientVersion= */ "1.2.3.4", /* correlationId= */ "");
  }

  @Test
  public void testCreateTaskAssignment_NoAssignment() {
    // arrange
    when(mockCore.createTaskAssignment(anyString(), anyString(), anyString()))
        .thenReturn(Optional.empty());

    // act
    ResponseEntity<CreateTaskAssignmentResponse> response =
        controller.createTaskAssignment("us", REQUEST);

    // assert
    assertThat(response.getBody())
        .isEqualTo(
            CreateTaskAssignmentResponse.newBuilder()
                .setRejectionInfo(
                    RejectionInfo.newBuilder()
                        .setReason(RejectionReason.Enum.NO_TASK_AVAILABLE)
                        .setRetryWindow(
                            RetryWindow.newBuilder()
                                .setDelayMin(Duration.newBuilder().setSeconds(60))
                                .setDelayMax(Duration.newBuilder().setSeconds(300))))
                .build());
    assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
    verify(mockCore)
        .createTaskAssignment(
            /* populationName= */ "us", /* clientVersion= */ "1.2.3.4", /* correlationId= */ "");
  }

  @Test
  public void testReportResult_COMPLETED_Succeeded() {
    // arrange
    when(mockCore.getUploadInstruction(anyString(), anyLong(), anyString(), anyString()))
        .thenReturn(
            Optional.of(
                UploadInstruction.newBuilder().setUploadLocation("https://upload").build()));

    // act
    ReportResultResponse response =
        controller.reportResult(
            "us",
            13,
            "9",
            "assignment-1",
            ReportResultRequest.newBuilder().setResult(Result.COMPLETED).build());

    // assert
    assertThat(response)
        .isEqualTo(
            ReportResultResponse.newBuilder()
                .setUploadInstruction(
                    UploadInstruction.newBuilder().setUploadLocation("https://upload"))
                .build());
    assertThat(response.getUploadInstruction().getExtraRequestHeaders()).isNotNull();
    assertThat(response.getUploadInstruction().getExtraRequestHeaders().size()).isEqualTo(0);
    verify(mockCore).getUploadInstruction("us", 13, "9", "assignment-1");
    verify(mockCore).reportLocalCompleted("us", 13, "9", "assignment-1");

    // assert metrics
    Timer timer = meterRegistry.find(REPORT_RESULT_TIMER_NAME).timer();
    assertThat(timer.count()).isEqualTo(1);
    assertThat(timer.getId().getTag(POPULATION_TAG)).isEqualTo("us");
    assertThat(timer.getId().getTag(RESULT_TAG)).isEqualTo("COMPLETED");
  }

  @Test
  public void testReportResult_COMPLETED_Succeeded_ExtraHeaders() {
    // arrange
    Map<String, String> headers = new HashMap<>();
    headers.put("header1", "value1");
    headers.put("header2", "value2");
    when(mockCore.getUploadInstruction(anyString(), anyLong(), anyString(), anyString()))
        .thenReturn(
            Optional.of(
                UploadInstruction.newBuilder()
                    .setUploadLocation("https://upload")
                    .putAllExtraRequestHeaders(headers)
                    .build()));

    // act
    ReportResultResponse response =
        controller.reportResult(
            "us",
            13,
            "9",
            "assignment-1",
            ReportResultRequest.newBuilder().setResult(Result.COMPLETED).build());

    // assert
    assertThat(response)
        .isEqualTo(
            ReportResultResponse.newBuilder()
                .setUploadInstruction(
                    UploadInstruction.newBuilder()
                        .putAllExtraRequestHeaders(headers)
                        .setUploadLocation("https://upload"))
                .build());
    verify(mockCore).getUploadInstruction("us", 13, "9", "assignment-1");
    verify(mockCore).reportLocalCompleted("us", 13, "9", "assignment-1");

    // assert metrics
    Timer timer = meterRegistry.find(REPORT_RESULT_TIMER_NAME).timer();
    assertThat(timer.count()).isEqualTo(1);
    assertThat(timer.getId().getTag(POPULATION_TAG)).isEqualTo("us");
    assertThat(timer.getId().getTag(RESULT_TAG)).isEqualTo("COMPLETED");
  }

  @Test
  public void testReportResult_COMPLETED_NotFound() {
    // arrange
    when(mockCore.getUploadInstruction(anyString(), anyLong(), anyString(), anyString()))
        .thenReturn(Optional.empty());

    // act
    ResponseStatusException expected =
        assertThrows(
            ResponseStatusException.class,
            () ->
                controller.reportResult(
                    "us",
                    13,
                    "9",
                    "assignment-1",
                    ReportResultRequest.newBuilder().setResult(Result.COMPLETED).build()));

    // assert
    assertThat(expected).hasMessageThat().contains("404 NOT_FOUND");
    verify(mockCore).getUploadInstruction("us", 13, "9", "assignment-1");
    verify(mockCore).reportLocalCompleted("us", 13, "9", "assignment-1");
    verifyNoMoreInteractions(mockCore);

    // assert metrics
    Timer timer = meterRegistry.find(REPORT_RESULT_TIMER_NAME).timer();
    assertNull(timer);
  }

  @Test
  public void testReportResult_FAILED_Succeeded() {

    // act
    ReportResultResponse response =
        controller.reportResult(
            "us",
            13,
            "9",
            "assignment-1",
            ReportResultRequest.newBuilder().setResult(Result.FAILED).build());

    // assert
    assertThat(response).isEqualTo(ReportResultResponse.newBuilder().build());
    verify(mockCore).reportLocalFailed("us", 13, "9", "assignment-1");
    verifyNoMoreInteractions(mockCore);

    // assert metrics
    Timer timer = meterRegistry.find(REPORT_RESULT_TIMER_NAME).timer();
    assertThat(timer.count()).isEqualTo(1);
    assertThat(timer.getId().getTag(POPULATION_TAG)).isEqualTo("us");
    assertThat(timer.getId().getTag(RESULT_TAG)).isEqualTo("FAILED");
  }

  @Test
  public void testReportResult_NotEligible_Succeeded() {

    // act
    ReportResultResponse response =
        controller.reportResult(
            "us",
            13,
            "9",
            "assignment-1",
            ReportResultRequest.newBuilder().setResult(Result.NOT_ELIGIBLE).build());

    // assert
    assertThat(response).isEqualTo(ReportResultResponse.newBuilder().build());
    verify(mockCore).reportLocalNotEligible("us", 13, "9", "assignment-1");
    verifyNoMoreInteractions(mockCore);

    // assert metrics
    Timer timer = meterRegistry.find(REPORT_RESULT_TIMER_NAME).timer();
    assertThat(timer.count()).isEqualTo(1);
    assertThat(timer.getId().getTag(POPULATION_TAG)).isEqualTo("us");
    assertThat(timer.getId().getTag(RESULT_TAG)).isEqualTo("NOT_ELIGIBLE");
  }

  @Test
  public void testReportResult_Unknown_Succeeded() {

    // act
    ReportResultResponse response =
        controller.reportResult(
            "us",
            13,
            "9",
            "assignment-1",
            ReportResultRequest.newBuilder().setResult(Result.UNKNOWN).build());

    // assert
    assertThat(response).isEqualTo(ReportResultResponse.newBuilder().build());
    verifyNoInteractions(mockCore);
  }

  @Test
  public void testReady() {
    // act and assert
    assertThat(controller.ready().contains("Assignment")).isTrue();
  }

  @Test
  public void testHealthz() {
    // act and assert
    assertThat(controller.healthz().contains("Assignment")).isTrue();
  }
}
