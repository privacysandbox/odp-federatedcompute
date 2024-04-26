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

package com.google.ondevicepersonalization.federatedcompute.endtoendtests;

import static com.google.common.truth.Truth.assertThat;
import static java.lang.String.format;

import com.beust.jcommander.JCommander;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.ondevicepersonalization.federatedcompute.endtoendtests.clients.Device;
import com.google.ondevicepersonalization.federatedcompute.endtoendtests.clients.Partner;
import com.google.ondevicepersonalization.federatedcompute.proto.CheckPointSelector;
import com.google.ondevicepersonalization.federatedcompute.proto.EvaluationInfo;
import com.google.ondevicepersonalization.federatedcompute.proto.EveryKIterationsCheckpointSelector;
import com.google.ondevicepersonalization.federatedcompute.proto.Task;
import com.google.ondevicepersonalization.federatedcompute.proto.TaskInfo;
import com.google.ondevicepersonalization.federatedcompute.proto.TaskStatus;
import com.google.ondevicepersonalization.federatedcompute.proto.TrainingInfo;
import java.time.InstantSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** End-to-end test for FCP server. */
public final class EndToEndTest {

  private static final InstantSource INSTANT_SOURCE = InstantSource.system();
  private static final String POPULATION_NAME_PREFIX = "e2e-test";
  private static final long MAX_RUN = 20;
  private static final long WAIT_INTERVAL_LONG = 5000;
  private static final String WAITING_INTERVAL_STRING = "5 seconds";
  private static final String RESOURCE_PREFIX = "resources/";

  private final String server;
  private final String tmServer;
  private final String publicKeyUrl;
  private final boolean useLocalResources;
  private final boolean encrypt;
  private String clientPlan;
  private String serverPlan;
  private String initCheckpoint;
  private String gradient;
  private String populationName;
  private int totalIteration;
  private int minAggregationSize;
  private int maxAggregationSize;
  private int maxParallel;
  private String minClientVersion;
  private String maxClientVersion;

  private Partner partner;
  private Device device;
  private long iterationWaitingInterval;

  /**
   * Constructs a new EndToEndTest object, initializing it with parameters from the provided
   * EndToEndArgs.
   *
   * @param endToEndArgs An EndToEndArgs object containing configuration parameters for the
   *     end-to-end test.
   */
  public EndToEndTest(EndToEndArgs endToEndArgs) {
    validateInput(endToEndArgs);
    this.server = endToEndArgs.getServer();
    this.tmServer = endToEndArgs.getTaskManagementServer();
    this.publicKeyUrl = endToEndArgs.getPublicKeyUrl();

    this.populationName = endToEndArgs.getPopulationName();
    this.totalIteration = endToEndArgs.getTotalIteration();
    this.minAggregationSize = endToEndArgs.getMinAggregationSize();
    this.maxAggregationSize = endToEndArgs.getMaxAggregationSize();

    this.maxParallel = endToEndArgs.getMaxParallel();
    this.minClientVersion = endToEndArgs.getMinClientVersion();
    this.maxClientVersion = endToEndArgs.getMaxClientVersion();

    this.useLocalResources = endToEndArgs.isUseLocalResources();
    this.clientPlan = endToEndArgs.getClientPlan();
    this.serverPlan = endToEndArgs.getServerPlan();
    this.initCheckpoint = endToEndArgs.getInitCheckpoint();
    this.gradient = endToEndArgs.getGradient();

    this.iterationWaitingInterval = endToEndArgs.getIterationWaitingInterval();
    this.encrypt = endToEndArgs.isEncrypt();

    partner = new Partner(tmServer);
    device = new Device(server);
  }

  /**
   * The main entry point for the End-to-End test application. It executes specified operation based
   * on arguments.
   *
   * @param args Command-line arguments provided to the application.
   */
  public static void main(String[] args) {
    EndToEndArgs endToEndArgs = new EndToEndArgs();
    JCommander jcommander = JCommander.newBuilder().addObject(endToEndArgs).build();
    jcommander.parse(args);

    if (endToEndArgs.isHelp()) {
      jcommander.usage();
      return;
    }

    try {
      EndToEndTest endToEndTest = new EndToEndTest(endToEndArgs);
      EndToEndArgs.Operation op = endToEndArgs.getOperation();
      switch (op) {
        case CREATE_AND_COMPLETE_TRAINING_TASK:
          endToEndTest.completeTask(endToEndTest.createTrainingTask());
          break;
        case CREATE_AND_COMPLETE_EVALUATION_TASK:
          // create training and evaluation task
          Task trainingTask = endToEndTest.createTrainingTask();
          EvaluationInfo evaluationInfo =
              EvaluationInfo.newBuilder()
                  .setTrainingPopulationName(trainingTask.getPopulationName())
                  .setTrainingTaskId(trainingTask.getTaskId())
                  .setCheckPointSelector(
                      CheckPointSelector.newBuilder()
                          .setIterationSelector(
                              EveryKIterationsCheckpointSelector.newBuilder().setSize(1)))
                  .build();
          // create an evaluation task under same population with training task
          Task evaluationTask =
              endToEndTest.createEvaluationTask(trainingTask.getPopulationName(), evaluationInfo);
          endToEndTest.completeTasksInSamePopulation(
              ImmutableList.of(trainingTask, evaluationTask));
          break;
        case CREATE_TASK:
          endToEndTest.createTrainingTask();
          break;
        case RUN_TASK:
          endToEndTest.runTask(endToEndArgs.getRunCount());
          break;
        case GET_TASK:
          endToEndTest.getTask(endToEndArgs.getTaskId());
          break;
        default:
          throw new IllegalArgumentException("Invalid operation.");
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static void validateInput(EndToEndArgs args) {
    EndToEndArgs.Operation op = args.getOperation();
    switch (op) {
      case CREATE_AND_COMPLETE_TRAINING_TASK:
      case CREATE_AND_COMPLETE_EVALUATION_TASK:
        if (args.isEncrypt()) {
          Objects.requireNonNull(args.getPublicKeyUrl());
        }
        Objects.requireNonNull(args.getServer());
        if (!args.isUseLocalResources()) {
          Objects.requireNonNull(args.getClientPlan());
          Objects.requireNonNull(args.getServerPlan());
          Objects.requireNonNull(args.getInitCheckpoint());
          Objects.requireNonNull(args.getGradient());
        }
        break;
      case CREATE_TASK:
        if (!args.isUseLocalResources()) {
          Objects.requireNonNull(args.getClientPlan());
          Objects.requireNonNull(args.getServerPlan());
          Objects.requireNonNull(args.getInitCheckpoint());
        }
        break;
      case RUN_TASK:
        if (args.isEncrypt()) {
          Objects.requireNonNull(args.getPublicKeyUrl());
        }
        Objects.requireNonNull(args.getPopulationName());
        Objects.requireNonNull(args.getServer());
        if (!args.isUseLocalResources()) {
          Objects.requireNonNull(args.getGradient());
        }
        break;
      case GET_TASK:
        Objects.requireNonNull(args.getPopulationName());
        break;
      default:
        throw new IllegalArgumentException("Invalid operation.");
    }
    Objects.requireNonNull(args.getTaskManagementServer());
    if (args.isUseLocalResources()) {
      Preconditions.checkArgument(
          args.getClientPlan() == null, "Local resources enabled, but remote resources provided.");
      Preconditions.checkArgument(
          args.getServerPlan() == null, "Local resources enabled, but remote resources provided.");
      Preconditions.checkArgument(
          args.getInitCheckpoint() == null,
          "Local resources enabled, but remote resources provided.");
      Preconditions.checkArgument(
          args.getGradient() == null, "Local resources enabled, but remote resources provided.");
    }
  }

  private Task createTrainingTask() throws Exception {
    if (populationName == null) {
      populationName = POPULATION_NAME_PREFIX + "-" + INSTANT_SOURCE.instant().toString();
    }
    Task trainingTask =
        Task.newBuilder()
            .setPopulationName(populationName)
            .setTotalIteration(totalIteration)
            .setMinAggregationSize(minAggregationSize)
            .setMaxAggregationSize(maxAggregationSize)
            .setMaxParallel(maxParallel)
            .setMinClientVersion(minClientVersion)
            .setMaxClientVersion(maxClientVersion)
            .setInfo(
                TaskInfo.newBuilder()
                    .setTrafficWeight(1)
                    .setTrainingInfo(TrainingInfo.getDefaultInstance()))
            .build();

    return createTask(trainingTask, true);
  }

  private Task createEvaluationTask(String populationName, EvaluationInfo evaluationInfo)
      throws Exception {
    Task evaluationTask =
        Task.newBuilder()
            .setPopulationName(populationName)
            .setTotalIteration(totalIteration)
            .setMinAggregationSize(minAggregationSize)
            .setMaxAggregationSize(maxAggregationSize)
            .setMaxParallel(maxParallel)
            .setMinClientVersion(minClientVersion)
            .setMaxClientVersion(maxClientVersion)
            .setInfo(TaskInfo.newBuilder().setTrafficWeight(1).setEvaluationInfo(evaluationInfo))
            .build();

    return createTask(evaluationTask, false);
  }

  private Task createTask(Task basicTask, boolean uploadInitCheckpoint) throws Exception {
    // create task
    System.out.println(format("Creating task at server with basic task: \n%s", basicTask));
    Task createdTask = partner.createTask(basicTask);
    System.out.println(
        format(
            "Created task at server, population name: %s, task id: %s",
            createdTask.getPopulationName(), createdTask.getTaskId()));

    // upload task metadata
    System.out.println("Uploading metadata for the task...");
    if (useLocalResources) {
      partner.uploadClientPlan(
          this.getClass().getResourceAsStream(RESOURCE_PREFIX + "client_only_plan").readAllBytes());
      partner.uploadServerPlan(
          this.getClass().getResourceAsStream(RESOURCE_PREFIX + "server_plan").readAllBytes());
      if (uploadInitCheckpoint) {
        partner.uploadCheckpoint(
            this.getClass().getResourceAsStream(RESOURCE_PREFIX + "checkpoint").readAllBytes());
      }
    } else {
      partner.uploadClientPlan(clientPlan);
      partner.uploadServerPlan(serverPlan);
      if (uploadInitCheckpoint) {
        partner.uploadCheckpoint(initCheckpoint);
      }
    }

    // wait for task scheduler to kick off
    Task existingTask = Task.getDefaultInstance();
    for (int i = 0; i <= MAX_RUN; i++) {
      existingTask = partner.getTask(createdTask.getPopulationName(), createdTask.getTaskId());
      if (existingTask.getStatus() == TaskStatus.Enum.CREATED) {
        System.out.println(
            format("Task status is CREATED, wait %s and retry...", WAITING_INTERVAL_STRING));
        Thread.sleep(WAIT_INTERVAL_LONG);
      }
    }

    // verify the task status is OPEN now
    System.out.println(format("After waiting, verify the task status is OPEN..."));
    assertThat(existingTask.getStatus()).isEqualTo(TaskStatus.Enum.OPEN);
    return createdTask;
  }

  private void getTask(long taskId) throws Exception {
    Task task = partner.getTask(populationName, taskId);
    System.out.println(task.toString());
  }

  private void runTask(int runCount) throws Exception {
    long contributorCount = 0;
    for (int i = 0; i < maxAggregationSize + MAX_RUN && contributorCount < runCount; i++) {
      boolean created = device.createTaskAssignment(minClientVersion, populationName);

      if (!created) {
        System.out.println(
            format(
                "Task assignment not created successfully, wait %s and check...",
                WAITING_INTERVAL_STRING));
        Thread.sleep(WAIT_INTERVAL_LONG);
        continue;
      }

      processTaskAssignment();
      contributorCount++;
    }
  }

  private void completeTask(Task basicTask) throws Exception {
    for (int i = 1; i <= basicTask.getTotalIteration(); i++) {
      System.out.println(
          format(
              "Processing population %s, task id %s, iteration %s ...",
              basicTask.getPopulationName(), basicTask.getTaskId(), i));
      createTaskAssignments(
          basicTask.getPopulationName(),
          basicTask.getMinAggregationSize(),
          basicTask.getMinAggregationSize());
      System.out.println(
          format(
              "Processed population %s, task id %s, iteration %s ...",
              basicTask.getPopulationName(), basicTask.getTaskId(), i));

      if (i == basicTask.getTotalIteration()) {
        break;
      }
      System.out.println(
          format("Waiting %s seconds for next iteration", this.iterationWaitingInterval / 1000));
      Thread.sleep(this.iterationWaitingInterval);
    }

    // wait for task scheduler to complete the task.
    System.out.println("Waiting for task to be complete...");
    Task updatedTask = Task.getDefaultInstance();
    for (int i = 0; i <= MAX_RUN; i++) {
      updatedTask = partner.getTask(basicTask.getPopulationName(), basicTask.getTaskId());
      if (updatedTask.getStatus() == TaskStatus.Enum.OPEN) {
        Thread.sleep(WAIT_INTERVAL_LONG * 2);
      }
    }
    System.out.println(format("After waiting, the task:\n %s", updatedTask));
    assertThat(updatedTask.getStatus().toString()).isEqualTo("COMPLETED");
  }

  private void completeTasksInSamePopulation(List<Task> tasks) throws Exception {
    String populationName = tasks.stream().findAny().get().getPopulationName();
    long deviceCount =
        tasks.stream()
            .mapToLong(task -> task.getTotalIteration() * task.getMaxAggregationSize())
            .sum();
    createTaskAssignments(populationName, deviceCount, deviceCount + 1);

    // wait for task scheduler to complete the task.
    System.out.println("Waiting for task to be complete...");
    List<TaskStatus.Enum> updatedTaskStatus = new ArrayList<>();
    for (int i = 0; i <= MAX_RUN; i++) {
      updatedTaskStatus =
          tasks.stream()
              .map(
                  task -> {
                    try {
                      return partner.getTask(task.getPopulationName(), task.getTaskId());
                    } catch (Exception e) {
                      throw new RuntimeException(e);
                    }
                  })
              .map(updatedTask -> updatedTask.getStatus())
              .collect(Collectors.toList());
      if (updatedTaskStatus.stream().anyMatch(status -> status == TaskStatus.Enum.OPEN)) {
        System.out.println("Some tasks are still open, waiting...");
        Thread.sleep(WAIT_INTERVAL_LONG * 2);
      }
    }
    assertThat(updatedTaskStatus.stream().anyMatch(status -> status == TaskStatus.Enum.COMPLETED))
        .isTrue();
    System.out.println(
        format(
            "All the tasks are completed now, the population name is %s, task ids are %s.",
            populationName, tasks.stream().map(task -> task.getTaskId()).toList()));
  }

  private void createTaskAssignments(
      String populationName, long requiredAssignments, long failureInterval) throws Exception {
    System.out.println(format("Devices start to create task assignment..."));
    long contributorCount = 0;
    for (int i = 0;
        i <= requiredAssignments + MAX_RUN && contributorCount < requiredAssignments;
        i++) {
      boolean created = device.createTaskAssignment(minClientVersion, populationName);

      if (!created) {
        System.out.println(
            format(
                "Task assignment not created successfully, wait %s and check...",
                WAITING_INTERVAL_STRING));
        Thread.sleep(WAIT_INTERVAL_LONG);
        continue;
      }

      if (i % failureInterval == 0) {
        System.out.println("reportLocalFailed");
        device.reportLocalFailed();
        continue;
      }

      processTaskAssignment();
      contributorCount++;
      System.out.println(
          format(
              "Need %s contributor(s), %s contributor(s) are submitted.",
              requiredAssignments, contributorCount));
    }
  }

  private void processTaskAssignment() throws Exception {
    device.downloadModel();
    device.downloadPlan();
    byte[] downloadedGradient;
    if (useLocalResources) {
      downloadedGradient =
          this.getClass().getResourceAsStream(RESOURCE_PREFIX + "gradient").readAllBytes();
    } else {
      downloadedGradient = partner.download(gradient);
    }
    if (encrypt) {
      downloadedGradient = device.encryptAndCompressGradient(downloadedGradient, publicKeyUrl);
    }
    device.submitResult(downloadedGradient);
  }
}
