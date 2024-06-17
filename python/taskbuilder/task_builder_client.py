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
from typing import Optional
import absl
import common
import google.auth
from google.auth import impersonated_credentials
from google.cloud import storage
import http_utils
import io_utils
import requests
from shuffler.proto import task_builder_pb2


def task_builder_request_handler(
    request: task_builder_pb2.BuildTaskRequest,
    artifact_only: Optional[bool] = False,
) -> task_builder_pb2.BuildTaskResponse:
  endpoint = http_utils.get_task_builder_endpoint(
      request_type=common.RequestType.BUILD_TASK_GROUP
  )
  if artifact_only:
    endpoint = http_utils.get_task_builder_endpoint(
        request_type=common.RequestType.BUILD_ARTIFACT_ONLY
    )
  logging.log(logging.INFO, 'Send HTTP request to: ' + endpoint)
  try:
    headers = common.PROROBUF_HEADERS
    if common.IMPERSONATE_SERVICE_ACCOUNT.value is None:
      logging.log(logging.INFO, 'Retrieving id token from metadata server.')
      access_token = http_utils.get_idtoken_from_metadata_server(endpoint)
    else:
      logging.log(
          logging.INFO,
          'Retrieving id token from impersonated service account '
          + common.IMPERSONATE_SERVICE_ACCOUNT.value,
      )
      access_token = get_id_token_from_adc(
          common.IMPERSONATE_SERVICE_ACCOUNT.value, endpoint
      )
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
  # Propagate error from Task Builder APIs.
  if not response.ok:
    raise common.TaskBuilderException(
        '{code}: {msg}'.format(code=response.status_code, msg=response.reason)
    )
  try:
    build_task_response = task_builder_pb2.BuildTaskResponse()
    build_task_response.ParseFromString(response.content)
    return build_task_response
  except Exception as e:
    raise common.TaskBuilderException(common.DECODE_ERROR_MSG) from e


def main(argv):
  model_path = common.MODEL_PATH.value
  task_config_path = common.CONFIG_PATH.value
  e2e_population_name = common.E2E_TEST_POPULATION_NAME.value
  artifact_only = common.ARTIFACT_BUILDING_ONLY.value
  skip_flex_ops_check = common.SKIP_FLEX_OPS_CHECK.value
  skip_dp_check = common.SKIP_DP_CHECK.value
  if not model_path:
    raise ValueError('`--saved_model` is required but not set.')
  if not task_config_path:
    raise ValueError('`--task_config` is required but not set.')

  logging.info('Creating build task request from command-line options.')
  flags = task_builder_pb2.ExperimentFlags()
  flags.skip_flex_ops_check = skip_flex_ops_check
  flags.skip_dp_check = skip_dp_check
  task_builder_request = io_utils.create_build_task_request_from_resource_path(
      model_path=model_path,
      task_config_path=task_config_path,
      client=storage.Client(),
      flags=flags,
  )
  if e2e_population_name:
    task_builder_request.task_config.population_name = e2e_population_name
  task_builder_response = task_builder_request_handler(
      request=task_builder_request,
      artifact_only=artifact_only,
  )
  if task_builder_response.HasField('task_group'):
    logging.info(
        'Success! Tasks are built, and artifacts are uploaded to the cloud.'
    )
  else:
    logging.exception(
        'Failed to create task group. Error type:'
        f' {task_builder_response.error_info.error_type}; Error message:'
        f' {task_builder_response.error_info.error_message}'
    )


# FOR ADC local
# ADC account must have roles/iam.serviceAccountTokenCreator permission on target_principal
def get_id_token_from_adc(target_principal, target_audience):
  credentials, project_id = google.auth.default()
  target_scopes = ['https://www.googleapis.com/auth/cloud-platform']
  target_credentials = impersonated_credentials.Credentials(
      source_credentials=credentials,
      target_principal=target_principal,
      target_scopes=target_scopes,
      delegates=[],
      lifetime=5,
  )

  id_creds = impersonated_credentials.IDTokenCredentials(
      target_credentials, target_audience=target_audience, include_email=True
  )
  request = google.auth.transport.requests.Request()
  id_creds.refresh(request)
  token = id_creds.token
  return token


if __name__ == '__main__':
  absl.app.run(main)
