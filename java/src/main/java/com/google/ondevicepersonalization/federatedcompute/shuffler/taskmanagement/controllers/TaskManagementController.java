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

import com.google.ondevicepersonalization.federatedcompute.proto.CreateTaskRequest;
import com.google.ondevicepersonalization.federatedcompute.proto.CreateTaskResponse;
import com.google.ondevicepersonalization.federatedcompute.proto.GetTaskByIdResponse;
import com.google.ondevicepersonalization.federatedcompute.shuffler.taskmanagement.core.TaskManagementCore;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** A Task management controller. */
@RestController
public final class TaskManagementController {

  private TaskManagementCore taskCore;

  public TaskManagementController(TaskManagementCore taskCore) {
    this.taskCore = taskCore;
  }

  @GetMapping("/taskmanagement/v1/population/{populationName}/tasks/{taskId}:get")
  GetTaskByIdResponse getTaskById(@PathVariable String populationName, @PathVariable long taskId) {
    // TODO(b/292228181): Improve http code return and mappings.
    // TODO(b/292562860): Validate input.
    return GetTaskByIdResponse.newBuilder()
        .setTask(
            taskCore
                .getTaskById(populationName, taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)))
        .build();
  }

  @PostMapping("/taskmanagement/v1/population/{populationName}:create-task")
  CreateTaskResponse createTask(
      @PathVariable String populationName, @RequestBody CreateTaskRequest request) {
    // TODO(b/292228181): Improve http code return and mappings.
    // TODO(b/292562860): Validate input.
    return CreateTaskResponse.newBuilder()
        .setTask(
            taskCore.createTask(
                request.getTask().toBuilder()
                    .setPopulationName(populationName)
                    .setTaskId(0)
                    .build()))
        .build();
  }

  @GetMapping("/ready")
  public String ready() {
    // TODO(291594777): Implement readiness check.
    return "Greetings from Task Management Spring Boot! Ready check. \n";
  }

  @GetMapping("/healthz")
  public String healthz() {
    // TODO(291594777): Implement health check.
    return "Greetings from Task Management Spring Boot! Health check. \n";
  }
}
