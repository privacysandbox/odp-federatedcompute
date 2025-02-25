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

from unittest.mock import Mock, patch
from absl.testing import absltest
import artifact_utils
import common
import dataset_utils
from fcp.artifact_building import data_spec
from fcp.artifact_building import federated_compute_plan_builder
from fcp.protos import plan_pb2
import learning_process_utils
from shuffler.proto import task_builder_pb2
from shuffler.proto import task_pb2
import tensorflow_checkpoints
import test_utils

TEST_FL_SETUP = task_builder_pb2.LearningProcess(
    runtime_config=task_builder_pb2.RuntimeConfig(
        report_goal=2000, example_selector_uri='app://test_collection_train'
    )
)
TEST_DP_PARAMETERS = common.DpParameter(
    noise_multiplier=1.0,
    dp_clip_norm=0.1,
    dp_epsilon=3.0,
    dp_delta=0.00001,
    num_training_rounds=100,
    dp_aggregator_type=task_builder_pb2.DpAggregator.FIXED_GAUSSIAN,
)
TEST_DATASET_POLICY = task_builder_pb2.DatasetPolicy(
    batch_size=3,
    max_training_batches_per_client=100,
)


class ArtifactUtilsTest(absltest.TestCase):

  def setUp(self):
    super().setUp()
    self._task = task_pb2.Task()
    self._task.client_only_plan_url.extend([common.TEST_BLOB_PATH])
    self._task.server_phase_url.extend([common.TEST_BLOB_PATH])
    self._task.init_checkpoint_url.extend([common.TEST_BLOB_PATH])
    self._model = test_utils.get_functional_model_without_metrics()
    training_process, _, _ = learning_process_utils.compose_iterative_processes(
        model=self._model,
        learning_process=TEST_FL_SETUP,
        dp_parameters=TEST_DP_PARAMETERS,
        training_and_eval=False,
        eval_only=False,
        flags=task_builder_pb2.ExperimentFlags(),
        task_report=task_builder_pb2.TaskReport(),
    )
    self._learning_process = training_process
    test_example_selector = plan_pb2.ExampleSelector(
        collection_uri=common.DEFAULT_EXAMPLE_SELECTOR
    )
    test_preprocessing_fn = dataset_utils.compose_preprocessing_fn(
        model=self._model, dataset_policy=TEST_DATASET_POLICY
    )
    self._data_spec = data_spec.DataSpec(
        test_example_selector, test_preprocessing_fn
    )
    self._real_plan_builder = federated_compute_plan_builder.build_plan
    self._real_checkpoint_builder = (
        tensorflow_checkpoints.build_initial_checkpoint_bytes
    )
    self._patcher = patch('artifact_utils.storage.Client')
    self._mock_client = self._patcher.start()
    self._mock_bucket = self._mock_client.bucket.return_value
    self._mock_bucket.exists.return_value = True
    self._mock_blob = self._mock_bucket.blob.return_value

  def test_build_artifacts_failed_plan(self):
    federated_compute_plan_builder.build_plan = Mock(side_effect=ValueError)
    with self.assertRaisesWithPredicateMatch(
        common.TaskBuilderException,
        lambda e: str(e).startswith(
            common.PLAN_BUILDING_ERROR_MESSAGE + 'Traceback '
        ),
    ):
      plan, client_plan, checkpoint = artifact_utils.build_artifacts(
          task=self._task,
          learning_process=self._learning_process,
          dataspec=self._data_spec,
          flags=task_builder_pb2.ExperimentFlags(),
      )
      artifact_utils.upload_artifacts(
          self._task, plan, client_plan, self._mock_client, checkpoint
      )

  def test_build_artifacts_failed_checkpoint(self):
    tensorflow_checkpoints.build_initial_checkpoint_bytes = Mock(
        side_effect=ValueError
    )
    with self.assertRaisesWithLiteralMatch(
        common.TaskBuilderException,
        common.CHECKPOINT_BUILDING_ERROR_MESSAGE,
    ):
      plan, client_plan, checkpoint = artifact_utils.build_artifacts(
          task=self._task,
          learning_process=self._learning_process,
          dataspec=self._data_spec,
          flags=task_builder_pb2.ExperimentFlags(),
      )
      artifact_utils.upload_artifacts(
          self._task, plan, client_plan, self._mock_client, checkpoint
      )

  def test_build_artifacts_success(self):
    try:
      plan, client_plan, checkpoint = artifact_utils.build_artifacts(
          task=self._task,
          learning_process=self._learning_process,
          dataspec=self._data_spec,
          flags=task_builder_pb2.ExperimentFlags(),
      )
      artifact_utils.upload_artifacts(
          self._task, plan, client_plan, self._mock_client, checkpoint
      )
    except:
      self.fail('Unexpected exception is raised when success is expected.')

  def test_build_eval_artifacts_success(self):
    _, eval_process, _ = learning_process_utils.compose_iterative_processes(
        model=self._model,
        learning_process=TEST_FL_SETUP,
        dp_parameters=TEST_DP_PARAMETERS,
        training_and_eval=False,
        eval_only=True,
        flags=task_builder_pb2.ExperimentFlags(),
        task_report=task_builder_pb2.TaskReport(),
    )

    try:
      plan, client_plan, checkpoint = artifact_utils.build_artifacts(
          task=self._task,
          learning_process=eval_process,
          dataspec=self._data_spec,
          flags=task_builder_pb2.ExperimentFlags(),
      )
      artifact_utils.upload_artifacts(
          self._task, plan, client_plan, self._mock_client, checkpoint
      )
    except:
      self.fail('Unexpected exception is raised when success is expected.')

  def test_build_plan_with_daf(self):
    plan, _, _ = artifact_utils.build_artifacts(
        task=self._task,
        learning_process=self._learning_process,
        dataspec=self._data_spec,
        use_daf=True,
        flags=task_builder_pb2.ExperimentFlags(),
    )
    self.assertIsNotNone(plan)

  def tearDown(self):
    federated_compute_plan_builder.build_plan = self._real_plan_builder
    tensorflow_checkpoints.build_initial_checkpoint_bytes = (
        self._real_checkpoint_builder
    )
    self._patcher.stop()


if __name__ == '__main__':
  absltest.main()
