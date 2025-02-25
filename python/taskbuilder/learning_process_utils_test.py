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

from dataclasses import asdict
from absl.testing import absltest
import common
import learning_process_utils
from shuffler.proto import task_builder_pb2
import test_utils

METRICS = [('precision', ''), ('auc', '')]
TEST_FL_SETUP = task_builder_pb2.LearningProcess(
    runtime_config=task_builder_pb2.RuntimeConfig(report_goal=2000)
)
TEST_FL_SETUP.metrics.extend([
    task_builder_pb2.Metric(name=name, parameter=param)
    for name, param in METRICS
])
DEFAULT_APPLIED_ALGORITHMS = task_builder_pb2.TaskReport.AppliedAlgorithms(
    learning_algo=task_builder_pb2.LearningAlgo.Enum.FED_AVG,
    dp_aggregator=task_builder_pb2.DpAggregator.Enum.FIXED_GAUSSIAN,
    client_optimizer=task_builder_pb2.Optimizer.Enum.SGD,
    server_optimizer=task_builder_pb2.Optimizer.Enum.SGD,
)
TEST_DP_PARAMETERS = common.DpParameter(
    noise_multiplier=1.0,
    dp_clip_norm=0.1,
    dp_delta=6.0,
    dp_epsilon=0.1,
    num_training_rounds=10,
    dp_aggregator_type=task_builder_pb2.DpAggregator.FIXED_GAUSSIAN,
)


class LearningProcessUtilsTest(absltest.TestCase):

  def test_compose_iterative_processes(self):
    training_process, eval_process, task_report = (
        learning_process_utils.compose_iterative_processes(
            model=test_utils.get_functional_model_without_metrics(),
            learning_process=TEST_FL_SETUP,
            dp_parameters=TEST_DP_PARAMETERS,
            training_and_eval=True,
            eval_only=False,
            flags=task_builder_pb2.ExperimentFlags(),
            task_report=task_builder_pb2.TaskReport(),
        )
    )
    self.assertIsNotNone(training_process)
    self.assertIsNotNone(training_process.initialize)
    self.assertIsNotNone(training_process.next)
    self.assertIsNotNone(training_process.get_model_weights)

    self.assertIsNotNone(eval_process)
    self.assertIsNotNone(eval_process.initialize)
    self.assertIsNotNone(eval_process.next)

    self.assertIsNotNone(task_report)
    self.assertEqual(task_report.applied_algorithms, DEFAULT_APPLIED_ALGORITHMS)

  def test_compose_iterative_process_training_only(self):
    training_process, empty, task_report = (
        learning_process_utils.compose_iterative_processes(
            model=test_utils.get_functional_model_without_metrics(),
            learning_process=TEST_FL_SETUP,
            dp_parameters=TEST_DP_PARAMETERS,
            training_and_eval=False,
            eval_only=False,
            flags=task_builder_pb2.ExperimentFlags(),
            task_report=task_builder_pb2.TaskReport(),
        )
    )
    self.assertIsNotNone(training_process)
    self.assertIsNotNone(training_process.initialize)
    self.assertIsNotNone(training_process.next)
    self.assertIsNotNone(training_process.get_model_weights)

    self.assertIsNone(empty)
    self.assertIsNotNone(task_report)
    self.assertEqual(task_report.applied_algorithms, DEFAULT_APPLIED_ALGORITHMS)

  def test_compose_iterative_process_eval_only(self):
    empty, eval_process, task_report = (
        learning_process_utils.compose_iterative_processes(
            model=test_utils.get_functional_model_without_metrics(),
            learning_process=TEST_FL_SETUP,
            dp_parameters=TEST_DP_PARAMETERS,
            training_and_eval=False,
            eval_only=True,
            flags=task_builder_pb2.ExperimentFlags(),
            task_report=task_builder_pb2.TaskReport(),
        )
    )
    self.assertIsNotNone(eval_process)
    self.assertIsNotNone(eval_process.initialize)
    self.assertIsNotNone(eval_process.next)

    self.assertIsNone(empty)

    self.assertIsNotNone(task_report)
    self.assertEqual(task_report.applied_algorithms, DEFAULT_APPLIED_ALGORITHMS)

  def test_compose_iterative_process_training_only_with_tree_aggregation(self):
    dp_parameters = common.DpParameter(
        noise_multiplier=1.0,
        dp_clip_norm=0.1,
        dp_delta=6.0,
        dp_epsilon=0.1,
        num_training_rounds=10,
        dp_aggregator_type=task_builder_pb2.DpAggregator.TREE_AGGREGATION,
    )

    training_process, eval_process, task_report = (
        learning_process_utils.compose_iterative_processes(
            model=test_utils.get_functional_model_without_metrics(),
            learning_process=TEST_FL_SETUP,
            dp_parameters=dp_parameters,
            training_and_eval=False,
            eval_only=False,
            flags=task_builder_pb2.ExperimentFlags(),
            task_report=task_builder_pb2.TaskReport(),
        )
    )
    self.assertIsNotNone(training_process)
    self.assertIsNotNone(training_process.initialize)
    self.assertIsNotNone(training_process.next)
    self.assertIsNotNone(training_process.get_model_weights)

    self.assertIsNotNone(task_report)
    self.assertEqual(
        task_report.applied_algorithms.dp_aggregator,
        task_builder_pb2.DpAggregator.TREE_AGGREGATION,
    )

  def test_compose_iterative_process_training_only_with_adaptive_tree_aggregation(
      self,
  ):
    dp_parameters = common.DpParameter(
        noise_multiplier=1.0,
        dp_clip_norm=0.1,
        dp_delta=6.0,
        dp_epsilon=0.1,
        num_training_rounds=10,
        dp_aggregator_type=task_builder_pb2.DpAggregator.ADAPTIVE_TREE,
    )

    training_process, eval_process, task_report = (
        learning_process_utils.compose_iterative_processes(
            model=test_utils.get_functional_model_without_metrics(),
            learning_process=TEST_FL_SETUP,
            dp_parameters=dp_parameters,
            training_and_eval=False,
            eval_only=False,
            flags=task_builder_pb2.ExperimentFlags(),
            task_report=task_builder_pb2.TaskReport(),
        )
    )
    self.assertIsNotNone(training_process)
    self.assertIsNotNone(training_process.initialize)
    self.assertIsNotNone(training_process.next)
    self.assertIsNotNone(training_process.get_model_weights)

    self.assertIsNotNone(task_report)
    self.assertEqual(
        task_report.applied_algorithms.dp_aggregator,
        task_builder_pb2.DpAggregator.ADAPTIVE_TREE,
    )

  def test_compose_iterative_process_training_only_with_adaptive_gaussian_aggregation(
      self,
  ):
    dp_parameters = common.DpParameter(
        noise_multiplier=1.0,
        dp_clip_norm=0.1,
        dp_delta=6.0,
        dp_epsilon=0.1,
        num_training_rounds=10,
        dp_aggregator_type=task_builder_pb2.DpAggregator.ADAPTIVE_GAUSSIAN,
    )

    training_process, eval_process, task_report = (
        learning_process_utils.compose_iterative_processes(
            model=test_utils.get_functional_model_without_metrics(),
            learning_process=TEST_FL_SETUP,
            dp_parameters=dp_parameters,
            training_and_eval=False,
            eval_only=False,
            flags=task_builder_pb2.ExperimentFlags(),
            task_report=task_builder_pb2.TaskReport(),
        )
    )
    self.assertIsNotNone(training_process)
    self.assertIsNotNone(training_process.initialize)
    self.assertIsNotNone(training_process.next)
    self.assertIsNotNone(training_process.get_model_weights)

    self.assertIsNotNone(task_report)
    self.assertEqual(
        task_report.applied_algorithms.dp_aggregator,
        task_builder_pb2.DpAggregator.ADAPTIVE_GAUSSIAN,
    )


if __name__ == '__main__':
  absltest.main()
