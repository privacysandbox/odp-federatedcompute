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

import os
from unittest.mock import MagicMock, patch
from absl.testing import absltest
import common
from google.cloud import storage
import io_utils

TEST_INVALID_URI = '/path/not/found'
TEST_INVALID_URI_ERROR_MSG = (
    'Cannot build artifacts because createTask returns invalid URLs:'
    ' /path/not/found'
)
TEST_GCS_UPLOADING_ERROR_MSG = (
    'Cannot upload aritfact to: gs://mock-bucket/mock_blob'
)


class TaskUtilsTest(absltest.TestCase):

  def setUp(self):
    super().setUp()
    self._patcher = patch('io_utils.storage.Client')
    self._mock_client = self._patcher.start()
    self._mock_bucket = self._mock_client.bucket.return_value
    self._mock_bucket.exists.return_value = True
    self._mock_blob = self._mock_bucket.blob.return_value

  def test_invalid_gcs_uri(self):
    with self.assertRaisesWithLiteralMatch(
        common.TaskBuilderException,
        TEST_INVALID_URI_ERROR_MSG,
    ):
      io_utils.parse_gcs_uri(client=self._mock_client, uri=TEST_INVALID_URI)

  def test_gcs_upload_content_fail(self):
    self._mock_blob.upload_from_string.side_effect = Exception(
        'Simulated Error'
    )
    with self.assertRaisesWithLiteralMatch(
        common.TaskBuilderException,
        TEST_GCS_UPLOADING_ERROR_MSG,
    ):
      io_utils.upload_content_to_gcs(
          client=self._mock_client,
          blob=common.BlobId(bucket='mock-bucket', name='mock_blob'),
          data=b'test_data',
      )

  def test_load_functional_model_fail(self):
    self._mock_blob = MagicMock(spec=storage.Blob)
    self._mock_blob.download_to_file.side_effect = Exception(
        'Simulated exception.'
    )
    self._mock_client.bucket.return_value.list_blobs.return_value = [
        self._mock_blob
    ]

    with self.assertRaisesWithLiteralMatch(
        common.TaskBuilderException,
        'Cannot load TFF functional model from gs://mock-model.',
    ):
      io_utils.load_functional_model(
          model_path='gs://mock-model', client=self._mock_client
      )

  def test_load_task_config_success(self):
    self._mock_blob.download_as_text.return_value = (
        'population_name: "my_new_population" '
    )
    task_config = io_utils.load_task_config(
        task_config_path='gs://mock-task-config/mock-task-config.pbtxt',
        client=self._mock_client,
    )
    self.assertEqual('my_new_population', task_config.population_name)

  def test_load_task_config_fail(self):
    self._mock_blob.download_as_text.side_effect = Exception(
        'Simulated exception.'
    )
    with self.assertRaisesWithLiteralMatch(
        common.TaskBuilderException,
        'Cannot load task config from'
        ' gs://mock-task-config/mock-task-config.pbtxt.',
    ):
      io_utils.load_task_config(
          task_config_path='gs://mock-task-config/mock-task-config.pbtxt',
          client=self._mock_client,
      )

  def test_get_full_secret_id(self):
    with patch.dict(os.environ, {common.ENV_KEY: 'mock-env'}):
      full_secret_id = io_utils.get_full_secret_id(param='MOCK_PARAM')
      self.assertEqual('fc-mock-env-MOCK_PARAM', full_secret_id)

  def test_get_env_variables(self):
    with patch.dict(
        os.environ,
        {
            common.GCP_PROJECT_ID_KEY: 'mock_project',
            common.TASK_MANAGEMENT_SERVER_URL_KEY: 'https://mock-server',
        },
    ):
      project_id = io_utils.get_gcp_project_id()
      self.assertEqual('mock_project', project_id)
      tm_server = io_utils.get_task_management_server(project_id=project_id)
      self.assertEqual('https://mock-server', tm_server)

  def tearDown(self):
    self._patcher.stop()


if __name__ == '__main__':
  absltest.main()
