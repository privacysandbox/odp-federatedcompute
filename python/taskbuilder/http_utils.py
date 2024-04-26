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
from typing import Optional, Tuple
import common
import google.auth
from google.auth import impersonated_credentials
import google.auth.transport.requests
import requests
from shuffler.proto import task_pb2


def get_task_management_endpoint(
    population_name: str, request_type: common.RequestType
) -> str:
  server_endpoint = common.TASK_MANAGEMENT_SERVER.value
  if server_endpoint.endswith('/'):
    server_endpoint = server_endpoint[:-1]
  # Partners are expected to interact with task management APIs only.
  server_endpoint = (
      server_endpoint + common.TASK_MANAGEMENT_V1 + population_name
  )
  if request_type == common.RequestType.CREATE_TASK:
    server_endpoint = server_endpoint + common.CREATE_TASK_ACTION
  elif request_type == common.RequestType.CANCEL_TASK:
    server_endpoint = server_endpoint + common.CANCEL_TASK_ACTION
  return server_endpoint


def create_task(
    endpoint: str, request: task_pb2.CreateTaskRequest
) -> task_pb2.CreateTaskResponse:
  logging.log(logging.INFO, 'Send HTTP request to: ' + endpoint)
  try:
    headers = common.PROROBUF_HEADERS
    task_management_server = common.TASK_MANAGEMENT_SERVER.value
    if _is_authorization_required(host=task_management_server):
      access_token = _get_id_token_from_adc(host=task_management_server)
      if access_token:
        headers['Authorization'] = f'Bearer {access_token}'
    response = requests.post(
        url=endpoint,
        data=request.SerializeToString(),
        headers=headers,
    )
  except Exception as e:
    raise common.TaskBuilderException(
        common.CONNECTION_FAILURE_MSG + endpoint
    ) from e
  # Propagate error from Task Management APIs.
  if not response.ok:
    raise common.TaskBuilderException(
        '{code}: {msg}'.format(code=response.status_code, msg=response.reason)
    )
  try:
    create_task_response = task_pb2.CreateTaskResponse()
    create_task_response.ParseFromString(response.content)
    return create_task_response
  except Exception as e:
    raise common.TaskBuilderException(common.DECODE_ERROR_MSG) from e


def create_task_group(
    tasks: Tuple[task_pb2.Task, Optional[task_pb2.Task]],
) -> Tuple[task_pb2.Task, Optional[task_pb2.Task]]:
  """Make create task request to Task Management APIs for a group of tasks."""
  main_task, optional_task = tasks
  create_task_endpoint = get_task_management_endpoint(
      population_name=main_task.population_name,
      request_type=common.RequestType.CREATE_TASK,
  )
  main_response = create_task(
      endpoint=create_task_endpoint,
      request=task_pb2.CreateTaskRequest(task=main_task),
  )
  if not optional_task:
    return main_response.task, None
  # Training and eval task mode
  eval_response = create_task(
      endpoint=create_task_endpoint,
      request=task_pb2.CreateTaskRequest(task=optional_task),
  )
  return main_response.task, eval_response.task


def _get_id_token_from_adc(host: str) -> str:
  credentials, _ = google.auth.default()
  target_credentials = impersonated_credentials.Credentials(
      source_credentials=credentials,
      target_principal=common.GCP_SERVICE_ACCOUNT.value,
      target_scopes=[common.GCP_TARGET_SCOPE.value],
      delegates=[],
      lifetime=5,
  )

  id_creds = impersonated_credentials.IDTokenCredentials(
      target_credentials, target_audience=host, include_email=True
  )
  request = google.auth.transport.requests.Request()
  id_creds.refresh(request)
  token = id_creds.token
  return token


def _is_authorization_required(host: str) -> bool:
  if not common.GCP_TARGET_SCOPE.value:
    logging.info('Target scope is not set')
    return False
  if not common.GCP_SERVICE_ACCOUNT.value:
    logging.info('Service account is not set.')
    return False
  if 'localhost' in host:
    logging.info('localhost is used for task management server.')
    return False
  return True
