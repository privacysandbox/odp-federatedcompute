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
import os
from unittest import mock
from absl.testing import absltest
import common
import responses
from shuffler.proto import task_builder_pb2
from shuffler.proto import task_pb2
import task_builder_core
import test_utils

CREATE_TASK_ENDPOINT = 'https://mock-server/taskmanagement/v1/population/my_new_population:create-task'


class TaskBuilderCoreTest(absltest.TestCase):

  def setUp(self):
    super().setUp()
    self._model = test_utils.get_functional_model_without_metrics()
    self._task_config = test_utils.get_good_training_and_eval_task_config()
    self._patcher = mock.patch('artifact_utils.storage.Client')
    self._mock_client = self._patcher.start()
    self._mock_bucket = self._mock_client.bucket.return_value
    self._mock_bucket.exists.return_value = True
    self._mock_blob = self._mock_bucket.blob.return_value

  @responses.activate
  @mock.patch.dict(
      os.environ, {common.TASK_MANAGEMENT_SERVER_URL_KEY: 'https://mock-server'}
  )
  @mock.patch('http_utils.get_idtoken_from_metadata_server')
  def test_task_builder_core_success(self, mock_auth):
    mock_auth.return_value = None
    task = task_pb2.Task()
    task.client_only_plan_url.extend([common.TEST_BLOB_PATH])
    task.server_phase_url.extend([common.TEST_BLOB_PATH])
    task.init_checkpoint_url.extend([common.TEST_BLOB_PATH])
    response_body = task_pb2.CreateTaskResponse(task=task).SerializeToString()
    responses.add(
        responses.POST,
        CREATE_TASK_ENDPOINT,
        body=response_body,
        status=http.HTTPStatus.OK,
        adding_headers={'Content-Type': 'application/x-protobuf'},
    )

    build_task_response = task_builder_core.build_task_group_request_handler(
        build_task_request=common.BuildTaskRequest(
            model=self._model,
            task_config=self._task_config,
            flags=task_builder_pb2.ExperimentFlags(),
        )
    )
    self.assertTrue(build_task_response.HasField('task_group'))
    self.assertTrue(build_task_response.HasField('task_report'))
    training_task = build_task_response.task_group.training_task
    eval_task = build_task_response.task_group.eval_task
    self.assertEqual(
        task.SerializeToString(), training_task.SerializeToString()
    )
    self.assertEqual(task.SerializeToString(), eval_task.SerializeToString())
    self.assertFalse(build_task_response.HasField('error_info'))

  def test_task_builder_core_artifact_only_success(self):
    build_task_response = task_builder_core.build_task_group_request_handler(
        build_task_request=common.BuildTaskRequest(
            model=self._model,
            task_config=self._task_config,
            flags=task_builder_pb2.ExperimentFlags(),
        ),
        artifact_only=True,
    )
    self.assertTrue(build_task_response.HasField('task_group'))
    self.assertTrue(build_task_response.HasField('task_report'))
    training_task = build_task_response.task_group.training_task
    eval_task = build_task_response.task_group.eval_task
    self.assertLen(training_task.server_phase_url, 1)
    self.assertLen(training_task.client_only_plan_url, 1)
    self.assertLen(training_task.init_checkpoint_url, 1)
    self.assertEqual(common.TEST_BLOB_PATH, training_task.server_phase_url[0])
    self.assertEqual(
        common.TEST_BLOB_PATH, training_task.client_only_plan_url[0]
    )
    self.assertEqual(
        common.TEST_BLOB_PATH, training_task.init_checkpoint_url[0]
    )
    # Update task min_client_version when build artifacts.
    self.assertEqual(training_task.min_client_version, '341912000')
    self.assertEqual(eval_task.min_client_version, '341812000')
    # Clear different min_client_version to compare task
    training_task.min_client_version = ''
    eval_task.min_client_version = ''
    self.assertEqual(
        training_task.SerializeToString(), eval_task.SerializeToString()
    )
    self.assertFalse(build_task_response.HasField('error_info'))

  def test_task_builder_core_config_validation_failed(self):
    self._task_config.policies.min_separation_policy.minimum_separation = -1
    build_task_response = task_builder_core.build_task_group_request_handler(
        build_task_request=common.BuildTaskRequest(
            model=self._model,
            task_config=self._task_config,
            flags=task_builder_pb2.ExperimentFlags(),
        ),
        artifact_only=True,
    )
    self.assertTrue(build_task_response.HasField('error_info'))
    self.assertTrue(build_task_response.HasField('task_report'))

  def tearDown(self):
    self._patcher.stop()


if __name__ == '__main__':
  absltest.main()
