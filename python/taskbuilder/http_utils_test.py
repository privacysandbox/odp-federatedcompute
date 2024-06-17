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

import http
from unittest.mock import MagicMock, patch
from absl.testing import absltest
import common
from google.api_core import exceptions
import google_crc32c
import http_utils
import responses
from shuffler.proto import task_pb2

TASK_MANAGEMENT_SERVER = 'https://mock-server'
CREATE_TASK_ENDPOINT = (
    'https://mock-server/taskmanagement/v1/population/test:create-task'
)
CANCEL_TASK_ENDPOINT = (
    'https://mock-server/taskmanagement/v1/population/test:cancel'
)
VM_METADATA_ENDPOINT = (
    'http://metadata.google.internal/computeMetadata/v1/project/project-id'
)
BAD_ENDPOINT = 'http://bad-endpoint/'
EXPECTED_ERROR_MSG = '500: Internal Server Error'


class CreateTaskTest(absltest.TestCase):

  def setUp(self):
    super().setUp()
    self._patcher = patch('http_utils.secretmanager.SecretManagerServiceClient')
    self._mock_secret_manager = self._patcher.start()
    self._mock_secret_response = MagicMock()
    self._mock_secret_response.payload.data = b'mock_secret'
    self._mock_secret_manager.access_secret_version.return_value = (
        self._mock_secret_response
    )

  def test_get_create_task_endpoint(self):
    endpoint = http_utils.get_task_management_endpoint(
        tm_server=TASK_MANAGEMENT_SERVER,
        population_name='test',
        request_type=common.RequestType.CREATE_TASK,
    )
    self.assertEqual(CREATE_TASK_ENDPOINT, endpoint)

  def test_get_cancel_task_endpoint(self):
    endpoint = http_utils.get_task_management_endpoint(
        tm_server=TASK_MANAGEMENT_SERVER,
        population_name='test',
        request_type=common.RequestType.CANCEL_TASK,
    )
    self.assertEqual(CANCEL_TASK_ENDPOINT, endpoint)

  def test_endpoint_connection_error(self):
    request = task_pb2.CreateTaskRequest(task=task_pb2.Task())
    with self.assertRaisesWithLiteralMatch(
        common.TaskBuilderException,
        common.CONNECTION_FAILURE_MSG + BAD_ENDPOINT,
    ):
      http_utils.create_task(BAD_ENDPOINT, request)

  @responses.activate
  def test_create_task_non_ok_http_status(self):
    request = task_pb2.CreateTaskRequest(task=task_pb2.Task())
    responses.add(
        responses.POST,
        CREATE_TASK_ENDPOINT,
        status=http.HTTPStatus.INTERNAL_SERVER_ERROR,
    )
    with self.assertRaisesWithLiteralMatch(
        common.TaskBuilderException, EXPECTED_ERROR_MSG
    ):
      http_utils.create_task(CREATE_TASK_ENDPOINT, request)

  @responses.activate
  def test_create_task_bad_content(self):
    request = task_pb2.CreateTaskRequest(task=task_pb2.Task())
    responses.add(
        responses.POST,
        CREATE_TASK_ENDPOINT,
        body='bad-content',
        status=http.HTTPStatus.OK,
        adding_headers=common.PROROBUF_HEADERS,
    )
    with self.assertRaisesWithLiteralMatch(
        common.TaskBuilderException, common.DECODE_ERROR_MSG
    ):
      http_utils.create_task(CREATE_TASK_ENDPOINT, request)

  @responses.activate
  def test_create_task_server_success(self):
    request = task_pb2.CreateTaskRequest(task=task_pb2.Task())
    response_bytes = task_pb2.CreateTaskResponse(
        task=task_pb2.Task()
    ).SerializeToString()
    responses.add(
        responses.POST,
        CREATE_TASK_ENDPOINT,
        body=response_bytes,
        status=http.HTTPStatus.OK,
        adding_headers=common.PROROBUF_HEADERS,
    )
    result = http_utils.create_task(CREATE_TASK_ENDPOINT, request)
    self.assertIsInstance(task_pb2.CreateTaskResponse(), type(result))

  @responses.activate
  @patch('http_utils.get_idtoken_from_metadata_server')
  def test_create_task_group_training_and_eval_success(self, mock_auth):
    mock_auth.return_value = None
    response_bytes = task_pb2.CreateTaskResponse(
        task=task_pb2.Task()
    ).SerializeToString()
    responses.add(
        responses.POST,
        CREATE_TASK_ENDPOINT,
        body=response_bytes,
        status=http.HTTPStatus.OK,
        adding_headers=common.PROROBUF_HEADERS,
    )
    task_group = (
        task_pb2.Task(population_name='test'),
        task_pb2.Task(population_name='test'),
    )
    training_task, eval_task = http_utils.create_task_group(
        tm_server=TASK_MANAGEMENT_SERVER, tasks=task_group
    )
    self.assertIsNotNone(training_task)
    self.assertIsNotNone(eval_task)

  @responses.activate
  @patch('http_utils.get_idtoken_from_metadata_server')
  def test_create_task_group_single_task_success(self, mock_auth):
    mock_auth.return_value = None
    response_bytes = task_pb2.CreateTaskResponse(
        task=task_pb2.Task()
    ).SerializeToString()
    responses.add(
        responses.POST,
        CREATE_TASK_ENDPOINT,
        body=response_bytes,
        status=http.HTTPStatus.OK,
        adding_headers=common.PROROBUF_HEADERS,
    )
    task_group = (
        task_pb2.Task(population_name='test'),
        None,
    )
    only_mode_task, empty = http_utils.create_task_group(
        tm_server=TASK_MANAGEMENT_SERVER, tasks=task_group
    )
    self.assertIsNotNone(only_mode_task)
    self.assertIsNone(empty)

  @responses.activate
  def test_get_vm_metadata_success(self):
    responses.add(
        responses.GET,
        VM_METADATA_ENDPOINT,
        body=b'mock-project-id',
        status=http.HTTPStatus.OK,
        adding_headers=common.VM_METADATA_HEADERS,
    )
    project_id = http_utils.get_vm_metadata(
        common.COMPUTE_METADATA_SERVER, common.GCP_PROJECT_ID_METADATA_KEY
    )
    self.assertEqual('mock-project-id', project_id)

  def test_get_vm_metadata_connection_error(self):
    project_id = http_utils.get_vm_metadata(
        BAD_ENDPOINT, common.GCP_PROJECT_ID_METADATA_KEY
    )
    self.assertIsNone(project_id)

  def test_get_secret_config_success(self):
    with patch(
        'http_utils.secretmanager.SecretManagerServiceClient',
        return_value=self._mock_secret_manager,
    ):
      self._mock_secret_response.payload.data_crc32c = int(
          google_crc32c.Checksum(
              self._mock_secret_response.payload.data
          ).hexdigest(),
          16,
      )
      secret_value = http_utils.get_secret_config(
          project_id='mock-project', secret_id='mock-secret-key'
      )
      self.assertEqual('mock_secret', secret_value)

  def test_get_secret_config_not_found(self):
    with patch(
        'http_utils.secretmanager.SecretManagerServiceClient',
        return_value=self._mock_secret_manager,
    ):
      self._mock_secret_manager.access_secret_version.side_effect = (
          exceptions.NotFound('Secret Not Found.')
      )
      secret_value = http_utils.get_secret_config(
          project_id='mock-project', secret_id='mock-secret-key'
      )
      self.assertIsNone(secret_value)

  def test_get_secret_config_corrupt_data(self):
    with patch(
        'http_utils.secretmanager.SecretManagerServiceClient',
        return_value=self._mock_secret_manager,
    ):
      self._mock_secret_response.payload.data_crc32c = int(
          google_crc32c.Checksum(b'corrupt_secret').hexdigest(), 16
      )
      secret_value = http_utils.get_secret_config(
          project_id='mock-project', secret_id='mock-secret-key'
      )
      self.assertIsNone(secret_value)

  def tearDown(self):
    self._patcher.stop()


if __name__ == '__main__':
  absltest.main()
