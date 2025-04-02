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

import logging
import common
from flask import Flask, Response, request
from google.cloud import storage
import io_utils
import task_builder_core

app = Flask(__name__)
logging.getLogger().setLevel(logging.INFO)
logging.getLogger('werkzeug').setLevel(logging.WARNING)


@app.route('/taskbuilder/v1:build-task-group', methods=['POST'])
def build_task_group():
  gcp_project_id = io_utils.get_gcp_project_id()
  gcs_client = storage.Client(project=gcp_project_id)
  build_task_group_request = (
      io_utils.create_build_task_request_from_request_body(
          request_data=request.data, client=gcs_client
      )
  )
  build_task_group_response = (
      task_builder_core.build_task_group_request_handler(
          build_task_request=build_task_group_request
      )
  )
  return Response(
      build_task_group_response.SerializeToString(),
      content_type=common.PROTOBUF_HEADERS['Content-Type'],
  )


@app.route('/taskbuilder/v1:build-artifacts', methods=['POST'])
def build_artifacts():
  gcp_project_id = io_utils.get_gcp_project_id()
  gcs_client = storage.Client(project=gcp_project_id)
  build_task_group_request = (
      io_utils.create_build_task_request_from_request_body(
          request_data=request.data, client=gcs_client
      )
  )
  build_task_group_response = (
      task_builder_core.build_task_group_request_handler(
          build_task_request=build_task_group_request, artifact_only=True
      )
  )
  return Response(
      build_task_group_response.SerializeToString(),
      content_type=common.PROTOBUF_HEADERS['Content-Type'],
  )


@app.route('/ready')
def ready():
  return 'Greetings from Task Builder Flask! Ready check. \n'


@app.route('/healthz')
def healthz():
  return 'Greetings from Task Builder Flask! Health check. \n'


if __name__ == '__main__':
  app.run('0.0.0.0')
