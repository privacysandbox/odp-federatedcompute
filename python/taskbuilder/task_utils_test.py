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
from shuffler.proto import task_builder_pb2
from shuffler.proto import task_pb2
import task_utils
import test_utils


class TaskUtilsTest(absltest.TestCase):

  def setUp(self):
    super().setUp()
    self._test_data = test_utils.get_good_training_and_eval_task_config()

  def test_create_tasks(self):
    training_task, eval_task = task_utils.create_tasks(self._test_data)
    # Test both training and eval tasks are built
    self.assertIsNotNone(training_task)
    self.assertIsNotNone(eval_task)
    self._validate_training_task(training_task=training_task)
    self._validate_eval_task(eval_task=eval_task)

  def test_create_training_only_task(self):
    self._test_data.mode = task_builder_pb2.TaskMode.Enum.TRAINING_ONLY
    training_task, eval_task = task_utils.create_tasks(self._test_data)
    self.assertIsNotNone(training_task)
    # Test no eval task is built
    self.assertIsNone(eval_task)
    self._validate_training_task(training_task=training_task)

  def test_create_eval_only_task(self):
    self._test_data.mode = task_builder_pb2.TaskMode.Enum.EVAL_ONLY
    eval_task, training_task = task_utils.create_tasks(self._test_data)
    self.assertIsNotNone(eval_task)
    # Test no training task is built
    self.assertIsNone(training_task)
    self._validate_eval_task(eval_task=eval_task)

    # Test zero division edge case in traffic weight calculation
    self._test_data.federated_learning.evaluation.evaluation_traffic = 1.0
    eval_task, _ = task_utils.create_tasks(task_config=self._test_data)
    self.assertEqual(10000, eval_task.info.traffic_weight)

  def test_create_tasks_for_artifact_only_request(self):
    training_task, eval_task = (
        task_utils.create_tasks_for_artifact_only_request(self._test_data)
    )
    self.assertIsNotNone(training_task)
    self.assertIsNotNone(eval_task)
    self._validate_task_uris(training_task)
    self._validate_task_uris(eval_task)

  def _validate_training_task(self, training_task: task_pb2.Task):
    self.assertEqual('my_new_population', training_task.population_name)
    self.assertEqual(2000, training_task.min_aggregation_size)
    self.assertEqual(2600, training_task.max_aggregation_size)
    self.assertEqual('0', training_task.min_client_version)
    self.assertEqual('999999999', training_task.max_client_version)
    self.assertEqual(
        common.TRAINING_TRAFFIC_WEIGHT, training_task.info.traffic_weight
    )
    self.assertTrue(training_task.info.HasField('training_info'))
    training_info = training_task.info.training_info
    eligibility_policies = (
        training_info.eligibility_task_info.eligibility_policies
    )
    self.assertEqual(2, len(eligibility_policies))
    policy_1 = eligibility_policies[0]
    policy_2 = eligibility_policies[1]
    self.assertTrue(policy_1.HasField('min_sep_policy'))
    self.assertTrue(policy_2.HasField('data_availability_policy'))
    self.assertEqual(3, policy_1.min_sep_policy.minimum_separation)
    self.assertEqual(100, policy_2.data_availability_policy.min_example_count)

  def _validate_eval_task(self, eval_task: task_pb2.Task):
    self.assertEqual('my_new_population', eval_task.population_name)
    self.assertEqual(200, eval_task.min_aggregation_size)
    self.assertEqual(260, eval_task.max_aggregation_size)
    self.assertEqual('0', eval_task.min_client_version)
    self.assertEqual('999999999', eval_task.max_client_version)
    self.assertEqual(11, eval_task.info.traffic_weight)
    self.assertTrue(eval_task.info.HasField('evaluation_info'))
    eval_info = eval_task.info.evaluation_info
    self.assertEqual('source_population', eval_info.training_population_name)
    checkpoint_selector = eval_info.check_point_selector
    self.assertTrue(checkpoint_selector.HasField('iteration_selector'))
    self.assertEqual(1, eval_info.training_task_id)
    iteration_selector = checkpoint_selector.iteration_selector
    self.assertEqual(2, iteration_selector.size)

  def _validate_task_uris(self, task):
    self.assertLen(task.client_only_plan_url, 1)
    self.assertEqual(common.TEST_BLOB_PATH, task.client_only_plan_url[0])
    self.assertLen(task.server_phase_url, 1)
    self.assertEqual(common.TEST_BLOB_PATH, task.server_phase_url[0])
    self.assertLen(task.init_checkpoint_url, 1)
    self.assertEqual(common.TEST_BLOB_PATH, task.init_checkpoint_url[0])


if __name__ == '__main__':
  absltest.main()
