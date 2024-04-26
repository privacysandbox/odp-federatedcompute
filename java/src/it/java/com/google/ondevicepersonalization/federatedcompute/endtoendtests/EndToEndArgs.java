package com.google.ondevicepersonalization.federatedcompute.endtoendtests;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.validators.PositiveInteger;
import lombok.Getter;

/** Arguments for running end-to-end test. */
@Getter
public class EndToEndArgs {

  @Parameter(names = "--help", help = true)
  private boolean help = false;

  @Parameter(names = "--server", description = "Federated compute server endpoint url")
  private String server;

  @Parameter(
      names = "--task_management_server",
      description = "Task management server endpoint url",
      required = true)
  private String taskManagementServer;

  @Parameter(names = "--public_key_url", description = "Public key fetching endpoint url")
  private String publicKeyUrl;

  @Parameter(
      names = "--client_plan",
      description = "GCS client plan location. Formatted as gs://<resource>")
  private String clientPlan;

  @Parameter(
      names = "--server_plan",
      description = "GCS server plan location. Formatted as gs://<resource>")
  private String serverPlan;

  @Parameter(
      names = "--init_checkpoint",
      description = "GCS initial checkpoint location. Formatted as gs://<resource>")
  private String initCheckpoint;

  @Parameter(
      names = "--gradient",
      description = "GCS device gradient location. Formatted as gs://<resource>")
  private String gradient;

  @Parameter(
      names = "--use_local_resources",
      arity = 1,
      description =
          "Boolean flag specifying to use local plans and checkpoints instead. Local resources are"
              + " located in the java resources folder.")
  private boolean useLocalResources = true;

  @Parameter(names = "--population_name", description = "Population name to use for the task")
  private String populationName;

  @Parameter(
      names = "--min_client_version",
      description = "Minimum client version to use for the task")
  private String minClientVersion = "0";

  @Parameter(
      names = "--max_client_version",
      description = "Minimum client version to use for the task")
  private String maxClientVersion = "999999999";

  @Parameter(
      names = "--total_iteration",
      description = "Total iteration count to use for the task",
      validateWith = PositiveInteger.class)
  private int totalIteration = 1;

  @Parameter(
      names = "--min_aggregation_size",
      description = "Minimum aggregation size to use for the task",
      validateWith = PositiveInteger.class)
  private int minAggregationSize = 2;

  @Parameter(
      names = "--max_aggregation_size",
      description = "Maximum aggregation size to use for the task",
      validateWith = PositiveInteger.class)
  private int maxAggregationSize = 3;

  @Parameter(
      names = "--max_parallel",
      description = "Max parallel count to use for the task",
      validateWith = PositiveInteger.class)
  private int maxParallel = 1;

  @Parameter(names = "--operation", description = "Operation to run against the FC server.")
  private Operation operation = Operation.CREATE_AND_COMPLETE_TRAINING_TASK;

  @Parameter(
      names = "--run_count",
      description = "Number of times to run task contributions for RUN_TASK operation.")
  private int runCount = 1;

  @Parameter(
      names = "--task_id",
      description = "Task id of the task",
      validateWith = PositiveInteger.class)
  private int taskId = 1;

  @Parameter(
      names = "--iteration_waiting_interval",
      description = "The time interval (in millisecond) to wait between iterations of a task.",
      validateWith = PositiveInteger.class)
  private int iterationWaitingInterval = 60000;

  @Parameter(
      names = "--encrypt",
      arity = 1,
      description = "Boolean flag specifying whether or not to encrypt the provided gradient.")
  private boolean encrypt = true;

  enum Operation {
    CREATE_AND_COMPLETE_TRAINING_TASK,
    CREATE_AND_COMPLETE_EVALUATION_TASK,
    CREATE_TASK,
    RUN_TASK,
    GET_TASK
  }
}
