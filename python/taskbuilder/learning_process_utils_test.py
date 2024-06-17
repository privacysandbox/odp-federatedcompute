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


class LearningProcessUtilsTest(absltest.TestCase):

  def test_compose_iterative_processes(self):
    dp_parameters = common.DpParameter(
        noise_multiplier=1.0,
        dp_clip_norm=0.1,
    )
    training_process, eval_process = (
        learning_process_utils.compose_iterative_processes(
            model=test_utils.get_functional_model_without_metrics(),
            learning_process=TEST_FL_SETUP,
            dp_parameters=dp_parameters,
            training_and_eval=True,
        )
    )
    self.assertIsNotNone(training_process)
    self.assertIsNotNone(training_process.initialize)
    self.assertIsNotNone(training_process.next)
    self.assertIsNotNone(training_process.get_model_weights)

    self.assertIsNotNone(eval_process)
    self.assertIsNotNone(eval_process.initialize)
    self.assertIsNotNone(eval_process.next)

  def test_compose_iterative_process_training_only(self):
    dp_parameters = common.DpParameter(
        noise_multiplier=1.0,
        dp_clip_norm=0.1,
    )
    training_process, empty = (
        learning_process_utils.compose_iterative_processes(
            model=test_utils.get_functional_model_without_metrics(),
            learning_process=TEST_FL_SETUP,
            dp_parameters=dp_parameters,
        )
    )
    self.assertIsNotNone(training_process)
    self.assertIsNotNone(training_process.initialize)
    self.assertIsNotNone(training_process.next)
    self.assertIsNotNone(training_process.get_model_weights)

    self.assertIsNone(empty)

  def test_compose_iterative_process_eval_only(self):
    dp_parameters = common.DpParameter(
        noise_multiplier=1.0,
        dp_clip_norm=0.1,
    )
    empty, eval_process = learning_process_utils.compose_iterative_processes(
        model=test_utils.get_functional_model_without_metrics(),
        learning_process=TEST_FL_SETUP,
        dp_parameters=dp_parameters,
        eval_only=True,
    )
    self.assertIsNotNone(eval_process)
    self.assertIsNotNone(eval_process.initialize)
    self.assertIsNotNone(eval_process.next)

    self.assertIsNone(empty)


if __name__ == '__main__':
  absltest.main()
