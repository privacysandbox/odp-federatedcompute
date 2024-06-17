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
from google.api_core import exceptions
from google.auth import compute_engine, transport
from google.cloud import secretmanager
import google_crc32c
import requests
from shuffler.proto import task_pb2


def get_task_management_endpoint(
    tm_server: str, population_name: str, request_type: common.RequestType
) -> str:
  if tm_server.endswith('/'):
    tm_server = tm_server[:-1]
  server_endpoint = tm_server + common.TASK_MANAGEMENT_V1 + population_name
  if request_type == common.RequestType.CREATE_TASK:
    server_endpoint = server_endpoint + common.CREATE_TASK_ACTION
  elif request_type == common.RequestType.CANCEL_TASK:
    server_endpoint = server_endpoint + common.CANCEL_TASK_ACTION
  return server_endpoint


def get_task_builder_endpoint(request_type: common.RequestType) -> str:
  server_endpoint = common.TASK_BUILDER_SERVER.value
  if server_endpoint.endswith('/'):
    server_endpoint = server_endpoint[:-1]
  server_endpoint = server_endpoint + common.TASK_BUILDER_V1
  if request_type == common.RequestType.BUILD_TASK_GROUP:
    server_endpoint = server_endpoint + common.BUILD_TASK_GROUP_ACTION
  elif request_type == common.RequestType.BUILD_ARTIFACT_ONLY:
    server_endpoint = server_endpoint + common.BUILD_ARTIFACT_ONLY_ACTION
  return server_endpoint


def create_task(
    endpoint: str,
    request: task_pb2.CreateTaskRequest,
    access_token: Optional[str] = None,
) -> task_pb2.CreateTaskResponse:
  logging.log(logging.INFO, 'Send HTTP request to: ' + endpoint)
  try:
    headers = common.PROROBUF_HEADERS
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
    tm_server: str,
    tasks: Tuple[task_pb2.Task, Optional[task_pb2.Task]],
) -> Tuple[task_pb2.Task, Optional[task_pb2.Task]]:
  """Make create task request to Task Management APIs for a group of tasks."""
  main_task, optional_task = tasks
  create_task_endpoint = get_task_management_endpoint(
      tm_server=tm_server,
      population_name=main_task.population_name,
      request_type=common.RequestType.CREATE_TASK,
  )
  access_token = get_idtoken_from_metadata_server(url=tm_server)
  main_response = create_task(
      endpoint=create_task_endpoint,
      request=task_pb2.CreateTaskRequest(task=main_task),
      access_token=access_token,
  )
  if not optional_task:
    return main_response.task, None
  # Training and eval task mode
  eval_response = create_task(
      endpoint=create_task_endpoint,
      request=task_pb2.CreateTaskRequest(task=optional_task),
      access_token=access_token,
  )
  return main_response.task, eval_response.task


def get_vm_metadata(metadata_server: str, key: str) -> Optional[str]:
  if not metadata_server.endswith('/'):
    metadata_server = metadata_server + '/'
  url = metadata_server + key
  try:
    response = requests.get(url=url, headers=common.VM_METADATA_HEADERS)
  except Exception as e:
    logging.warning('Fail to connect to VM metadata server: ' + metadata_server)
    return None
  if not response.ok:
    return None
  return response.text


def get_secret_config(
    project_id: str, secret_id: str, version_id: Optional[str] = 'latest'
) -> Optional[str]:
  client = secretmanager.SecretManagerServiceClient()
  name = f'projects/{project_id}/secrets/{secret_id}/versions/{version_id}'
  try:
    response = client.access_secret_version(request={'name': name})
  except exceptions.NotFound:
    logging.warning(f'{secret_id} does not exist in {project_id}.')
    return None

  # Verify payload checksum.
  crc32c = google_crc32c.Checksum()
  crc32c.update(response.payload.data)
  if response.payload.data_crc32c != int(crc32c.hexdigest(), 16):
    logging.warning('Data corruption detected.')
    return None

  payload = response.payload.data.decode('UTF-8')
  return payload


def get_idtoken_from_metadata_server(url: str) -> Optional[str]:
  if 'localhost' in url:
    logging.info(
        'Local testing is enabled. No authorization token is generated.'
    )
    return None
  request = transport.requests.Request()
  credentials = compute_engine.IDTokenCredentials(
      request=request, target_audience=url, use_metadata_identity_endpoint=True
  )
  credentials.refresh(request)
  return credentials.token
