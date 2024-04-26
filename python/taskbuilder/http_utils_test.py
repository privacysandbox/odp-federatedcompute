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
from absl.testing import absltest
import common
import http_utils
import responses
from shuffler.proto import task_pb2

CREATE_TASK_ENDPOINT = (
    'http://localhost:8082/taskmanagement/v1/population/test:create-task'
)
CANCEL_TASK_ENDPOINT = (
    'http://localhost:8082/taskmanagement/v1/population/test:cancel'
)
BAD_ENDPOINT = 'http://bad-endpoint/'
EXPECTED_ERROR_MSG = '500: Internal Server Error'


class CreateTaskTest(absltest.TestCase):

  def test_get_create_task_endpoint(self):
    endpoint = http_utils.get_task_management_endpoint(
        'test', common.RequestType.CREATE_TASK
    )
    self.assertEqual(CREATE_TASK_ENDPOINT, endpoint)

  def test_get_cancel_task_endpoint(self):
    endpoint = http_utils.get_task_management_endpoint(
        'test', common.RequestType.CANCEL_TASK
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
        adding_headers={'Content-Type': 'application/x-protobuf'},
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
        adding_headers={'Content-Type': 'application/x-protobuf'},
    )
    result = http_utils.create_task(CREATE_TASK_ENDPOINT, request)
    self.assertIsInstance(task_pb2.CreateTaskResponse(), type(result))

  @responses.activate
  def test_create_task_group_training_and_eval_success(self):
    response_bytes = task_pb2.CreateTaskResponse(
        task=task_pb2.Task()
    ).SerializeToString()
    responses.add(
        responses.POST,
        CREATE_TASK_ENDPOINT,
        body=response_bytes,
        status=http.HTTPStatus.OK,
        adding_headers={'Content-Type': 'application/x-protobuf'},
    )
    task_group = (
        task_pb2.Task(population_name='test'),
        task_pb2.Task(population_name='test'),
    )
    training_task, eval_task = http_utils.create_task_group(task_group)
    self.assertIsNotNone(training_task)
    self.assertIsNotNone(eval_task)

  @responses.activate
  def test_create_task_group_single_task_success(self):
    response_bytes = task_pb2.CreateTaskResponse(
        task=task_pb2.Task()
    ).SerializeToString()
    responses.add(
        responses.POST,
        CREATE_TASK_ENDPOINT,
        body=response_bytes,
        status=http.HTTPStatus.OK,
        adding_headers={'Content-Type': 'application/x-protobuf'},
    )
    task_group = (
        task_pb2.Task(population_name='test'),
        None,
    )
    only_mode_task, empty = http_utils.create_task_group(task_group)
    self.assertIsNotNone(only_mode_task)
    self.assertIsNone(empty)


if __name__ == '__main__':
  absltest.main()
