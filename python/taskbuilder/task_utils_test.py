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

import copy
from unittest.mock import MagicMock, patch
from absl.testing import absltest
import common
from google.cloud import storage
from shuffler.proto import task_builder_pb2
from shuffler.proto import task_pb2
import task_utils

TEST_DATA = task_builder_pb2.TaskConfig(
    mode=task_builder_pb2.TaskMode.Enum.TRAINING_AND_EVAL,
    population_name='my_new_population',
    label_name='y',
    policies=task_builder_pb2.Policies(
        min_separation_policy=task_pb2.MinimumSeparationPolicy(
            minimum_separation=3
        ),
        data_availability_policy=task_pb2.DataAvailabilityPolicy(
            min_example_count=100
        ),
        model_release_policy=task_builder_pb2.ModelReleaseManagementPolicy(
            dp_target_epsilon=6,
            dp_delta=0.000001,
            num_max_training_rounds=10000,
        ),
    ),
    federated_learning=task_builder_pb2.FederatedLearning(
        learning_process=task_builder_pb2.LearningProcess(
            client_learning_rate=0.01,
            server_learning_rate=1.0,
            runtime_config=task_builder_pb2.RuntimeConfig(report_goal=2000),
            artifact_building=task_builder_pb2.ArtifactBuilding(
                plan_url='gs://plan',
                client_plan_url='gs://client_only_plan',
                checkpoint_url='gs://checkpoint',
            ),
        ),
        evaluation=task_builder_pb2.Evaluation(
            source_training_population='source_population',
            checkpoint_selector='every_10_round',
            evaluation_traffic=0.1,
            report_goal=200,
            artifact_building=task_builder_pb2.ArtifactBuilding(
                plan_url='gs://plan',
                client_plan_url='gs://client_only_plan',
                checkpoint_url='gs://checkpoint',
            ),
        ),
    ),
    differential_privacy=task_builder_pb2.DifferentialPrivacy(
        noise_multiplier=0.1, clip_norm=0.1
    ),
)


class TaskUtilsTest(absltest.TestCase):

  def setUp(self):
    super().setUp()
    self._patcher = patch('task_utils.storage.Client')
    self._mock_client = self._patcher.start()
    self._test_data = copy.deepcopy(TEST_DATA)

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
    iteration_selector = checkpoint_selector.iteration_selector
    self.assertEqual(10, iteration_selector.size)

  def _validate_task_uris(self, task):
    self.assertLen(task.client_only_plan_url, 1)
    self.assertEqual('gs://client_only_plan', task.client_only_plan_url[0])
    self.assertLen(task.server_phase_url, 1)
    self.assertEqual('gs://plan', task.server_phase_url[0])
    self.assertLen(task.init_checkpoint_url, 1)
    self.assertEqual('gs://checkpoint', task.init_checkpoint_url[0])

  def test_load_functional_model_fail(self):
    self._saved_model_blob = MagicMock(spec=storage.Blob)
    self._saved_model_blob.download_to_file.side_effect = Exception(
        'Simulated exception.'
    )
    self._mock_client.bucket.return_value.list_blobs.return_value = [
        self._saved_model_blob
    ]

    with self.assertRaisesWithLiteralMatch(
        common.TaskBuilderException,
        'Cannot load TFF functional model from gs://mock-model.',
    ):
      functional_model = task_utils.load_functional_model(
          model_path='gs://mock-model',
          client=self._mock_client,
      )

  def test_load_task_config_success(self):
    self._mock_task_config_blob = (
        self._mock_client.bucket.return_value.blob.return_value
    )
    self._mock_task_config_blob.download_as_text.return_value = (
        'population_name: "my_new_population" '
    )
    task_config = task_utils.load_task_config(
        task_config_path='gs://mock-task-config/mock-task-config.pbtxt',
        client=self._mock_client,
    )
    self.assertEqual('my_new_population', task_config.population_name)

  def test_load_task_config_fail(self):
    self._mock_task_config_blob = (
        self._mock_client.bucket.return_value.blob.return_value
    )
    self._mock_task_config_blob.download_as_text.side_effect = Exception(
        'Simulated exception.'
    )
    with self.assertRaisesWithLiteralMatch(
        common.TaskBuilderException,
        'Cannot load task config from'
        ' gs://mock-task-config/mock-task-config.pbtxt.',
    ):
      task_utils.load_task_config(
          task_config_path='gs://mock-task-config/mock-task-config.pbtxt',
          client=self._mock_client,
      )

  def tearDown(self):
    self._patcher.stop()


if __name__ == '__main__':
  absltest.main()
