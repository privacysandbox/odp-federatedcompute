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
from fcp.artifact_building import federated_compute_plan_builder
import learning_process_utils
from shuffler.proto import task_builder_pb2
from shuffler.proto import task_pb2
import tensorflow_checkpoints
import test_utils

TEST_INVALID_URI = '/path/not/found'
TEST_INVALID_URI_ERROR_MSG = (
    'Cannot build artifacts because createTask returns invalid URLs:'
    ' /path/not/found'
)
TEST_BLOB_PATH = 'gs://mock-bucket/mock_blob'
TEST_BUCKET_NOT_FOUND_ERROR_MSG = (
    'Cannot build artifacts because bucket mock-bucket does not exist.'
)
TEST_GCS_UPLOADING_ERROR_MSG = (
    'Cannot upload aritfact to: gs://mock-bucket/mock_blob'
)
TEST_FL_SETUP = task_builder_pb2.LearningProcess(
    runtime_config=task_builder_pb2.RuntimeConfig(report_goal=2000)
)
TEST_DP_PARAMETERS = common.DpParameter(
    noise_multiplier=1.0,
    dp_clip_norm=0.1,
)


class ArtifactUtilsTest(absltest.TestCase):

  def setUp(self):
    super().setUp()
    self._task = task_pb2.Task()
    self._task.client_only_plan_url.extend([TEST_BLOB_PATH])
    self._task.server_phase_url.extend([TEST_BLOB_PATH])
    self._task.init_checkpoint_url.extend([TEST_BLOB_PATH])
    self._model = test_utils.get_functional_model_without_metrics()
    training_process, _ = learning_process_utils.compose_iterative_processes(
        model=self._model,
        learning_process=TEST_FL_SETUP,
        dp_parameters=TEST_DP_PARAMETERS,
    )
    self._learning_process = training_process
    self._preprocessing_fn = dataset_utils.compose_preprocessing_fn(
        model=self._model
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

  def test_build_artifacts_invalid_uri(self):
    self._task.client_only_plan_url.extend([TEST_INVALID_URI])
    build_task_response = artifact_utils.build_and_upload_artifacts(
        self._task,
        self._learning_process,
        self._mock_client,
        self._preprocessing_fn,
    )
    self.assertFalse(build_task_response.HasField('task_id'))
    self.assertTrue(build_task_response.HasField('error_info'))
    self.assertEqual(
        task_builder_pb2.ErrorType.Enum.ARTIFACT_BUILDING_ERROR,
        build_task_response.error_info.error_type,
    )
    self.assertEqual(
        TEST_INVALID_URI_ERROR_MSG,
        build_task_response.error_info.error_message,
    )

  def test_build_artifacts_nonexist_bucket(self):
    self._mock_bucket.exists.return_value = False
    build_task_response = artifact_utils.build_and_upload_artifacts(
        self._task,
        self._learning_process,
        self._mock_client,
        self._preprocessing_fn,
    )
    self.assertFalse(build_task_response.HasField('task_id'))
    self.assertTrue(build_task_response.HasField('error_info'))
    self.assertEqual(
        task_builder_pb2.ErrorType.Enum.ARTIFACT_BUILDING_ERROR,
        build_task_response.error_info.error_type,
    )
    self.assertEqual(
        TEST_BUCKET_NOT_FOUND_ERROR_MSG,
        build_task_response.error_info.error_message,
    )

  def test_build_artifacts_failed_plan(self):
    federated_compute_plan_builder.build_plan = Mock(side_effect=ValueError)
    build_task_response = artifact_utils.build_and_upload_artifacts(
        self._task,
        self._learning_process,
        self._mock_client,
        self._preprocessing_fn,
    )
    self.assertFalse(build_task_response.HasField('task_id'))
    self.assertTrue(build_task_response.HasField('error_info'))
    self.assertEqual(
        task_builder_pb2.ErrorType.Enum.ARTIFACT_BUILDING_ERROR,
        build_task_response.error_info.error_type,
    )
    self.assertStartsWith(
        build_task_response.error_info.error_message,
        common.PLAN_BUILDING_ERROR_MESSAGE,
    )

  def test_build_artifacts_failed_checkpoint(self):
    tensorflow_checkpoints.build_initial_checkpoint_bytes = Mock(
        side_effect=ValueError
    )
    build_task_response = artifact_utils.build_and_upload_artifacts(
        self._task,
        self._learning_process,
        self._mock_client,
        self._preprocessing_fn,
    )
    self.assertFalse(build_task_response.HasField('task_id'))
    self.assertTrue(build_task_response.HasField('error_info'))
    self.assertEqual(
        task_builder_pb2.ErrorType.Enum.ARTIFACT_BUILDING_ERROR,
        build_task_response.error_info.error_type,
    )
    self.assertStartsWith(
        build_task_response.error_info.error_message,
        common.CHECKPOINT_BUILDING_ERROR_MESSAGE,
    )

  def test_build_artifacts_success(self):
    build_task_response = artifact_utils.build_and_upload_artifacts(
        self._task,
        self._learning_process,
        self._mock_client,
        self._preprocessing_fn,
    )
    self.assertTrue(build_task_response.HasField('task_id'))
    self.assertFalse(build_task_response.HasField('error_info'))

  def test_gcs_upload_content_failed(self):
    self._mock_blob.upload_from_string.side_effect = Exception(
        'Simulated Error'
    )
    build_task_response = artifact_utils.build_and_upload_artifacts(
        self._task,
        self._learning_process,
        self._mock_client,
        self._preprocessing_fn,
    )
    self.assertFalse(build_task_response.HasField('task_id'))
    self.assertTrue(build_task_response.HasField('error_info'))
    self.assertEqual(
        task_builder_pb2.ErrorType.Enum.ARTIFACT_BUILDING_ERROR,
        build_task_response.error_info.error_type,
    )
    self.assertEqual(
        build_task_response.error_info.error_message,
        TEST_GCS_UPLOADING_ERROR_MSG,
    )

  def test_build_eval_artifacts_success(self):
    eval_process, _ = learning_process_utils.compose_iterative_processes(
        model=self._model,
        learning_process=TEST_FL_SETUP,
        dp_parameters=TEST_DP_PARAMETERS,
        eval_only=True,
    )
    build_task_response = artifact_utils.build_and_upload_artifacts(
        self._task,
        eval_process,
        self._mock_client,
        self._preprocessing_fn,
    )
    self.assertTrue(build_task_response.HasField('task_id'))
    self.assertFalse(build_task_response.HasField('error_info'))

  def tearDown(self):
    federated_compute_plan_builder.build_plan = self._real_plan_builder
    tensorflow_checkpoints.build_initial_checkpoint_bytes = (
        self._real_checkpoint_builder
    )
    self._patcher.stop()


if __name__ == '__main__':
  absltest.main()
