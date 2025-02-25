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

import com.google.ondevicepersonalization.federatedcompute.proto.CreateTaskAssignmentRequest;
import com.google.ondevicepersonalization.federatedcompute.proto.CreateTaskAssignmentResponse;
import com.google.ondevicepersonalization.federatedcompute.proto.ReportResultRequest;
import com.google.ondevicepersonalization.federatedcompute.proto.ReportResultResponse;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.CompressionUtils.CompressionFormat;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.logging.ResponseProto;
import com.google.ondevicepersonalization.federatedcompute.shuffler.taskassignment.core.TaskAssignmentCore;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** The task assignment controller. */
@RestController
public class TaskAssignmentController {

  static final String CREATE_TASK_ASSIGNMENT_TIMER_NAME = "create-task-assignment";
  static final String REPORT_RESULT_TIMER_NAME = "report-result";
  static final String POPULATION_TAG = "population";
  static final String RESULT_TAG = "result";
  static final String RESULT_CREATED = "CREATED";
  private static final Logger logger = LoggerFactory.getLogger(TaskAssignmentController.class);
  private final TaskAssignmentCore taskAssignment;
  private final MeterRegistry meterRegistry;

  public TaskAssignmentController(TaskAssignmentCore taskAssignment, MeterRegistry meterRegistry) {
    this.taskAssignment = taskAssignment;
    this.meterRegistry = meterRegistry;
  }

  @ResponseProto(CreateTaskAssignmentResponse.class)
  @PostMapping("/taskassignment/v1/population/{populationName}:create-task-assignment")
  public ResponseEntity<CreateTaskAssignmentResponse> createTaskAssignment(
      @PathVariable String populationName, @RequestBody CreateTaskAssignmentRequest request) {

    final Timer.Sample sample = Timer.start();
    // All clients versions support uploading gzip gradient, and gzip is the only
    // compression format supported by server, hard code it to gzip for now
    CreateTaskAssignmentResponse response =
        taskAssignment.createTaskAssignment(
            populationName,
            request.getClientVersion().getVersionCode(),
            "",
            CompressionFormat.GZIP);
    if (response.hasRejectionInfo()) {
      sample.stop(
          buildTimerWithPopulationAndResult(
              meterRegistry,
              CREATE_TASK_ASSIGNMENT_TIMER_NAME,
              populationName,
              response.getRejectionInfo().getReason().getValueDescriptor().getName()));
      return ResponseEntity.status(HttpStatus.OK).body(response);
    } else {
      sample.stop(
          buildTimerWithPopulationAndResult(
              meterRegistry, CREATE_TASK_ASSIGNMENT_TIMER_NAME, populationName, RESULT_CREATED));
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
  }

  private Timer buildTimerWithPopulationAndResult(
      MeterRegistry meterRegistry, String timerName, String population, String result) {
    return Timer.builder(timerName)
        .tags(POPULATION_TAG, population, RESULT_TAG, result)
        .register(meterRegistry);
  }

  @PutMapping(
      "/taskassignment/v1/population/{populationName}/task/{taskId}/aggregation/{aggregationId}/task-assignment/{assignmentId}:report-result")
  public ReportResultResponse reportResult(
      @PathVariable String populationName,
      @PathVariable long taskId,
      @PathVariable String aggregationId,
      @PathVariable String assignmentId,
      @RequestBody ReportResultRequest request) {

    final Timer.Sample sample = Timer.start();
    ReportResultResponse response = ReportResultResponse.getDefaultInstance();
    switch (request.getResult()) {
      case COMPLETED:
        taskAssignment.reportLocalCompleted(populationName, taskId, aggregationId, assignmentId);
        response =
            // All clients versions support uploading gzip gradient, and gzip is the only
            // compression format supported by server, hard code it to gzip for now
            taskAssignment
                .getUploadInstruction(
                    populationName, taskId, aggregationId, assignmentId, CompressionFormat.GZIP)
                .map(
                    uploadInstruction ->
                        ReportResultResponse.newBuilder()
                            .setUploadInstruction(uploadInstruction)
                            .build())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        break;
      case FAILED:
        taskAssignment.reportLocalFailed(populationName, taskId, aggregationId, assignmentId);
        break;
      case NOT_ELIGIBLE:
        taskAssignment.reportLocalNotEligible(populationName, taskId, aggregationId, assignmentId);
        break;
      case FAILED_EXAMPLE_GENERATION:
        taskAssignment.reportLocalFailedExampleGeneration(
            populationName, taskId, aggregationId, assignmentId);
        break;
      case FAILED_MODEL_COMPUTATION:
        taskAssignment.reportLocalFailedModelComputation(
            populationName, taskId, aggregationId, assignmentId);
        break;
      case FAILED_OPS_ERROR:
        taskAssignment.reportLocalFailedOpsError(
            populationName, taskId, aggregationId, assignmentId);
        break;
      default:
        // do nothing
    }

    sample.stop(
        buildTimerWithPopulationAndResult(
            meterRegistry, REPORT_RESULT_TIMER_NAME, populationName, request.getResult().name()));
    return response;
  }

  @GetMapping("/ready")
  public String ready() {
    // TODO(291594777): Implement readiness check.
    return "Greetings from Task Assignment Spring Boot! Ready check. \n";
  }

  @GetMapping("/healthz")
  public String healthz() {
    // TODO(291594777): Implement health check.
    return "Greetings from Task Assignment Spring Boot! Health check. \n";
  }
}
