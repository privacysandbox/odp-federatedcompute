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

import collections
from typing import Any, Callable, Sequence, TypeVar
import common
from shuffler.proto import task_builder_pb2
import tensorflow as tf
import tensorflow_federated as tff

StateVar = TypeVar('StateVar')


def metric_outcomes(
    metrics: list[task_builder_pb2.Metric],
) -> dict[str, list[str]]:
  outcome = {'accepted_metrics': [], 'rejected_metrics': []}
  for metric in metrics:
    metric_name = metric.name
    if metric_name in common.METRICS_ALLOWLIST:
      outcome['accepted_metrics'].append(metric_name)
    else:
      outcome['rejected_metrics'].append(metric_name)
  return outcome


def build_metric_constructors_list(
    allowed_metric_names: Sequence[str],
) -> list[Callable[[], tf.keras.metrics.Metric]]:
  """Builds a no-arg callable that constructors specified metrics.

  Args:
    allowed_metric_names: A sequence of `str` metric names.

  Returns:
    A `list` of no-arg callables matching the order of `metrics_enum_list`. Each
    callable returns a `tf.keras.metrics.Metric` instance.
  """

  def _build_metric_constructor(
      metric_name: str,
  ) -> Callable[[], tf.keras.metrics.Metric]:
    # Names have to be specified manually because some keras metrics
    # generates globally unique names (e.g. precision, precision_1). This will
    # make some downstream checks in TFF side fail.
    return common.METRICS_ALLOWLIST[metric_name]

  return [
      _build_metric_constructor(metric_name)
      for metric_name in allowed_metric_names
  ]


def create_metrics_fns(
    metric_constructors: Sequence[Callable[[], tf.keras.metrics.Metric]] = (),
) -> tuple[
    Callable[[], StateVar],
    Callable[[StateVar, Any, tff.learning.models.BatchOutput, Any], StateVar],
    Callable[[StateVar], Any],
]:
  """Creates the three functions for metrics updates.

  Args:
    metric_constructors: A sequence of callables that construct Keras metrics.

  Returns:
    A 3-tuple of callables (initialize, update, finalize) for implementing
    metrics on a `tff.learning.models.FunctionalModel`. Also see
    `tff.learning.metrics.create_functional_metric_fns` documentation.
  """

  def metrics_constructor() -> (
      collections.OrderedDict[str, tf.keras.metrics.Metric]
  ):
    metrics: collections.OrderedDict[str, tf.keras.metrics.Metric] = (
        collections.OrderedDict()
    )
    for constructor in metric_constructors:
      metric = constructor()
      metrics[metric.name] = metric
    return metrics

  initialize_fn, update_fn, finalize_fn = (
      tff.learning.metrics.create_functional_metric_fns(metrics_constructor)
  )

  @tf.function
  def initialize():
    state = initialize_fn()
    # Add a state for the loss metric tracking.
    state['loss'] = (
        tf.zeros([], dtype=tf.float32),
        tf.zeros([], dtype=tf.float32),
    )
    return state

  @tf.function
  def update(
      state: StateVar,
      labels,
      batch_output: tff.learning.models.BatchOutput,
      sample_weight=None,
  ) -> StateVar:
    # Make a copy of state to modify, tf.function does not allow modifying
    # parameters.
    state = type(state)(state)
    # Must pop the 'loss' key out of the structure because the `update_fn`
    # method doesn't expect it and will otherwise return bogus results.
    loss, weight = state.pop('loss')
    if (
        batch_output.predictions is not None
        and isinstance(batch_output.predictions, tuple)
        and len(batch_output.predictions) == 3
    ):
      # We are assuming (possibly incorrectly) that this is an AutoPE model.
      # Consider using __repr__ or __name__ output to reduce the number of
      # false positives for this check.
      _, _, logits = batch_output.predictions
      batch_output = tff.learning.models.BatchOutput(
          loss=batch_output.loss,
          predictions=logits,
          num_examples=batch_output.num_examples,
      )

    state = update_fn(state, labels, batch_output, sample_weight)
    # Update the batch weighted loss sum.
    batch_weight = tf.cast(batch_output.num_examples, tf.float32)
    state['loss'] = (
        loss + (batch_output.loss * batch_weight),
        weight + batch_weight,
    )
    return state

  @tf.function
  def finalize(state):
    # Make a copy of state to modify, tf.function does not allow modifying
    # parameters.
    state = type(state)(state)
    # Must pop the 'loss' key out of the structure because the `finalize_fn`
    # method doesn't expect it and will otherwise return bogus results.
    loss, weight = state.pop('loss')
    metrics = finalize_fn(state)
    metrics['loss'] = loss / weight
    return metrics

  return initialize, update, finalize
