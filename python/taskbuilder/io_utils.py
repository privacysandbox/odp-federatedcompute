# Copyright 2024 Google LLC
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
import os
import tempfile
from typing import Optional
import common
from fcp.protos.federatedcompute import common_pb2
from google.cloud import storage
from google.protobuf import text_format
from google.protobuf.message import DecodeError
import http_utils
from shuffler.proto import task_builder_pb2
import tensorflow_federated as tff


def load_functional_model(
    model_path: str, client: storage.Client
) -> Optional[tff.learning.models.FunctionalModel]:
  """Load a TFF functional model as `tff.learning.models.FunctionalModel` from a GCS `model_path`."""
  functional_model = None
  try:
    blob_id = parse_gcs_uri(
        client=client, uri=model_path, allow_empty_blob=True
    )
    bucket_name = blob_id.bucket
    model_dir = blob_id.name
    bucket = client.bucket(bucket_name)
    with tempfile.TemporaryDirectory() as temp_path:
      blobs = bucket.list_blobs(prefix=model_dir)
      for blob in blobs:
        if blob.name.endswith('/'):
          continue
        file_path = os.path.join(temp_path, blob.name)
        local_dir = os.path.dirname(file_path)
        # create intermediate directories to preserve original structures
        os.makedirs(local_dir, exist_ok=True)
        with open(file_path, 'wb+') as f:
          blob.download_to_file(f)
      functional_model = tff.learning.models.load_functional_model(
          os.path.join(temp_path, model_dir)
      )
  except Exception as e:
    raise common.TaskBuilderException(
        common.LOADING_MODEL_ERROR_MESSAGE.format(path=model_path)
    ) from e
  return functional_model


def load_task_config(
    task_config_path: str, client: storage.Client
) -> Optional[task_builder_pb2.TaskConfig]:
  """Load task configurations as `task_builder_pb2.TaskConfig` from a pbtxt file on GCS."""
  task_config = None
  try:
    blob_id = parse_gcs_uri(client=client, uri=task_config_path)
    bucket_name = blob_id.bucket
    object_name = blob_id.name
    bucket = client.bucket(bucket_name)
    blob = bucket.blob(object_name)
    task_config_str = blob.download_as_text()
    task_config = task_builder_pb2.TaskConfig()
    text_format.Parse(task_config_str, task_config)
  except Exception as e:
    raise common.TaskBuilderException(
        common.LOADING_CONFIG_ERROR_MESSAGE.format(path=task_config_path)
    ) from e
  return task_config


def parse_gcs_uri(
    client: storage.Client, uri: str, allow_empty_blob: Optional[bool] = False
) -> common.BlobId:
  # Check URI format
  if not uri:
    raise common.TaskBuilderException(
        common.INVALID_URI_ERROR_MESSAGE + 'empty URI.'
    )
  if not uri.startswith(common.GCS_PREFIX):
    raise common.TaskBuilderException(common.INVALID_URI_ERROR_MESSAGE + uri)
  uri_without_prefix = uri[len(common.GCS_PREFIX) :]
  temp = uri_without_prefix.split('/', 1)
  if not temp:
    raise common.TaskBuilderException(common.INVALID_URI_ERROR_MESSAGE + uri)
  bucket_name = temp[0]
  # support bucket-only scenario for model path
  if allow_empty_blob and len(temp) == 1:
    return common.BlobId(bucket=temp[0])
  if len(temp) != 2:
    raise common.TaskBuilderException(common.INVALID_URI_ERROR_MESSAGE + uri)
  object_name = temp[1]
  return common.BlobId(bucket=bucket_name, name=object_name)


def upload_content_to_gcs(
    client: storage.Client,
    blob: common.BlobId,
    data: Optional[bytes] = None,
    filename: Optional[str] = None,
):
  if not data and not filename:
    raise common.TaskBuilderException(
        common.ARTIFACT_UPLOADING_ERROR_MESSAGE
        + 'Exactly one of `data` or `filename` must be provided.'
    )
  if data and filename:
    raise common.TaskBuilderException(
        common.ARTIFACT_UPLOADING_ERROR_MESSAGE
        + '`data` and `filename` cannot both be specified.'
    )

  bucket_name = blob.bucket
  object_name = blob.name
  logging.log(
      logging.INFO,
      'Start uploading artifact to: '
      + common.GCS_PREFIX
      + bucket_name
      + '/'
      + object_name,
  )
  try:
    bucket = client.bucket(bucket_name)
    blob = bucket.blob(object_name)
    if data:
      blob.upload_from_string(data)
    if filename:
      blob.upload_from_filename(filename)
    logging.log(logging.INFO, 'Upload complete!')
  except:
    raise common.TaskBuilderException(
        common.ARTIFACT_UPLOADING_ERROR_MESSAGE
        + common.GCS_PREFIX
        + bucket_name
        + '/'
        + object_name
    )


def create_build_task_request_from_resource_path(
    model_path: str,
    task_config_path: str,
    client: storage.Client,
    flags: task_builder_pb2.ExperimentFlags,
) -> task_builder_pb2.BuildTaskRequest:
  """Create a BuildTaskRequest from provided resource path."""
  task_config = None
  try:
    logging.info(f'Loading task config from {task_config_path}...')
    task_config = load_task_config(
        task_config_path=task_config_path, client=client
    )
  except common.TaskBuilderException as e:
    raise e
  return task_builder_pb2.BuildTaskRequest(
      saved_model=common_pb2.Resource(uri=model_path),
      task_config=task_config,
      flags=flags,
  )


def create_build_task_request_from_request_body(
    request_data: bytes, client: storage.Client
) -> common.BuildTaskRequest:
  """Create a BuildTaskRequest from the request body in the service."""
  build_task_request = task_builder_pb2.BuildTaskRequest()
  try:
    build_task_request.ParseFromString(request_data)
  except DecodeError as e:
    raise common.TaskBuilderException(
        'Unable to decode request body: ' + str(e)
    )
  saved_model_request = build_task_request.saved_model
  task_config_request = build_task_request.task_config
  flags_request = build_task_request.flags

  functional_model = None

  if saved_model_request.HasField('uri'):
    try:
      functional_model = load_functional_model(
          model_path=saved_model_request.uri, client=client
      )
    except common.TaskBuilderException as e:
      raise e

  return common.BuildTaskRequest(
      model=functional_model,
      task_config=task_config_request,
      flags=flags_request,
  )


def get_gcp_project_id() -> Optional[str]:
  project_id = os.environ.get(common.GCP_PROJECT_ID_KEY)
  if project_id:
    return project_id
  project_id = http_utils.get_vm_metadata(
      metadata_server=common.COMPUTE_METADATA_SERVER,
      key=common.GCP_PROJECT_ID_METADATA_KEY,
  )
  return project_id


def get_task_management_server(project_id: str) -> Optional[str]:
  task_management_server = os.environ.get(common.TASK_MANAGEMENT_SERVER_URL_KEY)
  if task_management_server:
    return task_management_server
  if not project_id:
    return None
  full_secret_id = get_full_secret_id(common.TASK_MANAGEMENT_SERVER_URL_KEY)
  task_management_server = http_utils.get_secret_config(
      project_id=project_id, secret_id=full_secret_id
  )
  return task_management_server


def get_full_secret_id(param: str) -> str:
  env = os.environ.get(common.ENV_KEY)
  if not env:
    return f'{common.DEFAULT_PARAM_PREFIX}-{param}'
  return f'{common.DEFAULT_PARAM_PREFIX}-{env}-{param}'
