# Copyright 2023 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from typing import Optional, Tuple
import common
import metrics_utils
from shuffler.proto import task_builder_pb2
import tensorflow as tf
import tensorflow_federated as tff


def compose_iterative_processes(
    model: tff.learning.models.FunctionalModel,
    learning_process: task_builder_pb2.LearningProcess,
    dp_parameters: common.DpParameter,
    training_and_eval: bool,
    eval_only: bool,
    flags: task_builder_pb2.ExperimentFlags,
    task_report: task_builder_pb2.TaskReport,
) -> Tuple[
    Optional[tff.templates.IterativeProcess],
    Optional[tff.templates.IterativeProcess],
    task_builder_pb2.TaskReport,
]:
  """Composes the training iterative process and the evaluation iterative process..

  Args:
    model: A 'tff.learning.models.FunctionalModel' model.
    learning_process: The learning process setups from the configuration.
    dp_parameters: The DP setups from the configuration and preprocessing.
    training_and_eval: If generating both training learning process and
      evaluation learning process together.
    eval_only: If generating the evaluation learning process only.

  Returns: A tuple of (training learning process, evaluation learning
  process, task report).
  """
  # Unpack configurations and set default if not provided
  learning_algo = (
      learning_process.type or task_builder_pb2.LearningAlgo.Enum.FED_AVG
  )
  client_optimizer = (
      learning_process.client_optimizer or task_builder_pb2.Optimizer.Enum.SGD
  )
  server_optimizer = (
      learning_process.server_optimizer or task_builder_pb2.Optimizer.Enum.SGD
  )
  client_learning_rate = learning_process.client_learning_rate or 0.1
  server_learning_rate = learning_process.server_learning_rate or 1.0
  training_report_goal = learning_process.runtime_config.report_goal

  task_report.applied_algorithms.client_optimizer = client_optimizer
  task_report.applied_algorithms.server_optimizer = server_optimizer
  task_report.applied_algorithms.learning_algo = learning_algo

  metrics = learning_process.metrics

  # Find accepted and rejected metrics
  metric_outcomes = metrics_utils.metric_outcomes(metrics)
  metric_results = task_builder_pb2.TaskReport.MetricResults(
      accepted_metrics=", ".join(metric_outcomes["accepted_metrics"]).lstrip(),
      rejected_metrics=", ".join(metric_outcomes["rejected_metrics"]).lstrip(),
  )
  task_report.metric_results.CopyFrom(metric_results)
  # Add metrics into model.
  model = _compose_model_with_metrics(
      model, metric_outcomes["accepted_metrics"]
  )

  def compose_eval_iterative_process(
      training_iterative_process: tff.templates.IterativeProcess,
  ) -> tff.templates.IterativeProcess:
    get_model_weights_computation = training_iterative_process.get_model_weights
    state_type = get_model_weights_computation.type_signature.parameter
    evaluation_computation = tff.learning.build_federated_evaluation(
        model_fn=model
    )
    batch_type = evaluation_computation.type_signature.parameter[1]

    @tff.tf_computation
    def create_all_zero_state():
      return tff.types.structure_from_tensor_type_tree(
          lambda t: tf.zeros(shape=t.shape, dtype=t.dtype), state_type
      )

    @tff.federated_computation
    def initialize():
      return tff.federated_eval(create_all_zero_state, tff.SERVER)

    @tff.tf_computation(state_type)
    def get_flatted_model_weights_computation(state):
      # Switch to the tuple expected by evaluation_computation.
      model_weights = get_model_weights_computation(state)
      return (model_weights.trainable, model_weights.non_trainable)

    @tff.federated_computation(
        tff.FederatedType(state_type, tff.SERVER), batch_type
    )
    def eval_next(state, data):
      model_weights = tff.federated_map(
          get_flatted_model_weights_computation, state
      )
      raw_metrics = evaluation_computation(model_weights, data)
      if isinstance(raw_metrics.type_signature, tff.StructType):
        metrics = tff.federated_zip(raw_metrics)
      else:
        metrics = raw_metrics
      return state, metrics

    return tff.templates.IterativeProcess(initialize, eval_next)

  # Construct TFF optimizers
  tff_client_optimizer = _get_optimizer(
      optimizer_type=client_optimizer, learning_rate=client_learning_rate
  )
  tff_server_optimizer = _get_optimizer(
      optimizer_type=server_optimizer, learning_rate=server_learning_rate
  )

  # Inject DP parameters to the learning process.
  # Only Fixed Gaussian is supported.
  fixed_gaussian_dp_aggregator = None
  if not flags.skip_dp_aggregator:
    fixed_gaussian_dp_aggregator = (
        tff.aggregators.DifferentiallyPrivateFactory.gaussian_fixed(
            noise_multiplier=dp_parameters.noise_multiplier,
            clip=dp_parameters.dp_clip_norm,
            clients_per_round=training_report_goal,
        )
    )
    task_report.applied_algorithms.dp_aggregator = (
        task_builder_pb2.DpAggregator.FIXED_GAUSSIAN
    )

  def compose_training_iterative_process() -> tff.templates.IterativeProcess:
    if learning_algo == task_builder_pb2.LearningAlgo.Enum.FED_SGD:
      return tff.learning.algorithms.build_fed_sgd(
          model_fn=model,
          server_optimizer_fn=tff_server_optimizer,
          model_aggregator=fixed_gaussian_dp_aggregator,
      )
    return tff.learning.algorithms.build_unweighted_fed_avg(
        model_fn=model,
        client_optimizer_fn=tff_client_optimizer,
        server_optimizer_fn=tff_server_optimizer,
        model_aggregator=fixed_gaussian_dp_aggregator,
    )

  training_iterative_process = compose_training_iterative_process()
  if eval_only:
    return (
        None,
        compose_eval_iterative_process(training_iterative_process),
        task_report,
    )
  elif training_and_eval:
    return (
        training_iterative_process,
        compose_eval_iterative_process(training_iterative_process),
        task_report,
    )
  else:
    return training_iterative_process, None, task_report


def _get_optimizer(
    optimizer_type: task_builder_pb2.Optimizer.Enum, learning_rate: float
) -> tff.learning.optimizers.Optimizer:
  if optimizer_type == task_builder_pb2.Optimizer.Enum.ADAM:
    return tff.learning.optimizers.build_adam(
        learning_rate=learning_rate,
    )
  # Support SGD optimizer by default.
  return tff.learning.optimizers.build_sgdm(learning_rate=learning_rate)


def _compose_model_with_metrics(
    model: tff.learning.models.FunctionalModel,
    metrics: list[str],
) -> tff.learning.models.FunctionalModel:
  metrics_constructors = metrics_utils.build_metric_constructors_list(
      allowed_metric_names=metrics
  )
  (
      model.initialize_metrics_state,
      model.update_metrics_state,
      model.finalize_metrics,
  ) = metrics_utils.create_metrics_fns(metric_constructors=metrics_constructors)

  return model
