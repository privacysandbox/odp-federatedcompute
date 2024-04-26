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

from collections.abc import Callable
import logging
from typing import Optional
import common
from fcp.artifact_building import data_spec
from fcp.artifact_building import federated_compute_plan_builder
from fcp.artifact_building import plan_utils
from fcp.protos import plan_pb2
from google.cloud import storage
from shuffler.proto import task_builder_pb2
from shuffler.proto import task_pb2
import tensorflow as tf
import tensorflow_checkpoints
import tensorflow_federated as tff


def build_artifacts(
    learning_process: tff.templates.IterativeProcess,
    preprocessing_fn: Callable[[tf.data.Dataset], tf.data.Dataset],
):
  """Returns artifacts built from a properly composed learning process and a preprocessing function.

  Args:
      learning_process: a properly composed and DP-noised
        `tff.learning.templates.LearningProcess` built from a TF/TFF model.
      preprocessing_fn: a callable function that consists of all preprocessing
        that needs to be done on the client dataset.

  Raises: TaskBuilderException if any step fails, with an error message.

  Returns:
      Server-side `Plan` that encapsulates all training instuctions for a task,
      including model information and other metadata, such as report goal.
      Client-side `ClientOnlyPlan`, consumed by devices, is a device-side
      subset of `Plan`.
      Initial model weights of the model in bytes.
  """
  logging.log(logging.INFO, 'Start building artifacts...')
  plan = _build_plan(learning_process.next, preprocessing_fn)
  logging.log(logging.INFO, 'Plan was built successfully.')
  client_plan = _build_client_plan(plan)
  logging.log(logging.INFO, 'ClientOnlyPlan was built successfully.')
  checkpoint_bytes = _build_initial_server_checkpoint(
      learning_process.initialize
  )
  logging.log(logging.INFO, 'Initial checkpoint was built successfully.')
  return plan, client_plan, checkpoint_bytes


def build_and_upload_artifacts(
    task: task_pb2.Task,
    learning_process: tff.templates.IterativeProcess,
    client: Optional[storage.Client] = None,
    preprocessing_fn: Optional[
        Callable[[tf.data.Dataset], tf.data.Dataset]
    ] = None,
) -> task_builder_pb2.BuildTaskResponse:
  try:
    # check if URIs are valid, otherwise do not start building artifacts.
    if not client:
      client = storage.Client(project=common.GCP_PROJECT_ID.value)
    logging.log(logging.INFO, 'Validating format of returned URIs...')
    checkpoint_blobs = _parse_gcs_uri(
        client=client, uris=task.init_checkpoint_url
    )
    plan_blobs = _parse_gcs_uri(client=client, uris=task.server_phase_url)
    client_blobs = _parse_gcs_uri(client=client, uris=task.client_only_plan_url)
    logging.log(logging.INFO, 'URIs are validated.')

    plan, client_plan, checkpoint_bytes = build_artifacts(
        learning_process=learning_process, preprocessing_fn=preprocessing_fn
    )
    for checkpoint_blob in checkpoint_blobs:
      _upload_content_to_gcs(
          client=client, blob=checkpoint_blob, data=checkpoint_bytes
      )
    for plan_blob in plan_blobs:
      _upload_content_to_gcs(
          client=client, blob=plan_blob, data=plan.SerializeToString()
      )
    for client_plan_blob in client_blobs:
      _upload_content_to_gcs(
          client=client,
          blob=client_plan_blob,
          data=client_plan.SerializeToString(),
      )
  except common.TaskBuilderException as e:
    return task_builder_pb2.BuildTaskResponse(
        error_info=task_builder_pb2.ErrorInfo(
            error_type=task_builder_pb2.ErrorType.Enum.ARTIFACT_BUILDING_ERROR,
            error_message=str(e),
        )
    )
  return task_builder_pb2.BuildTaskResponse(task_id=task.task_id)


def _build_plan(
    learning_process_comp: tff.Computation,
    preprocessing_fn: Optional[
        Callable[[tf.data.Dataset], tf.data.Dataset]
    ] = None,
) -> plan_pb2.Plan:
  logging.log(logging.INFO, 'Start building plan...')
  try:
    example_selector = plan_pb2.ExampleSelector(
        collection_uri=common.EXAMPLE_COLLECTION_URI.value
    )
    plan = federated_compute_plan_builder.build_plan(
        mrf=tff.backends.mapreduce.get_map_reduce_form_for_computation(
            learning_process_comp
        ),
        dataspec=data_spec.DataSpec(example_selector, preprocessing_fn),
    )
    logging.log(logging.INFO, 'Adding TFLite graph to the plan...')
    return plan_utils.generate_and_add_flat_buffer_to_plan(plan)
  except Exception as e:
    raise common.TaskBuilderException(
        common.PLAN_BUILDING_ERROR_MESSAGE + str(e)
    )


def _build_client_plan(plan: plan_pb2.Plan) -> plan_pb2.ClientOnlyPlan:
  logging.log(logging.INFO, 'Start building ClientOnlyPlan...')
  try:
    """The ClientOnlyPlan corresponding to the Plan proto."""
    client_only_plan = plan_pb2.ClientOnlyPlan(
        phase=plan.phase[0].client_phase,
        graph=plan.client_graph_bytes.value,
        tflite_graph=plan.client_tflite_graph_bytes,
    )
    if plan.HasField('tensorflow_config_proto'):
      client_only_plan.tensorflow_config_proto.CopyFrom(
          plan.tensorflow_config_proto
      )
    return client_only_plan
  except Exception as e:
    raise common.TaskBuilderException(
        common.CLIENT_PLAN_BUILDING_ERROR_MESSAGE + str(e)
    )


def _parse_gcs_uri(
    client: storage.Client, uris: list[str]
) -> list[common.BlobId]:
  # Check URI format
  blobs = []
  if not uris:
    raise common.TaskBuilderException(
        common.INVALID_URI_ERROR_MESSAGE + 'empty URIs.'
    )
  for uri in uris:
    if not uri.startswith(common.GCS_PREFIX):
      raise common.TaskBuilderException(common.INVALID_URI_ERROR_MESSAGE + uri)
    uri_without_prefix = uri[len(common.GCS_PREFIX) :]
    temp = uri_without_prefix.split('/', 1)
    if len(temp) != 2:
      raise common.TaskBuilderException(common.INVALID_URI_ERROR_MESSAGE + uri)
    bucket_name, object_name = temp[0], temp[1]
    # Assume the bucket exists.
    if not client.bucket(bucket_name).exists():
      raise common.TaskBuilderException(
          common.BUCKET_NOT_FOUND_ERROR_MESSAGE.format(bucket_name=bucket_name)
      )
    blobs.append(common.BlobId(bucket=bucket_name, name=object_name))
  return blobs


def _upload_content_to_gcs(
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


def _build_initial_server_checkpoint(
    initialize_comp: tff.Computation,
) -> bytes:
  logging.log(logging.INFO, 'Start building initial checkpoint...')
  try:
    return tensorflow_checkpoints.build_initial_checkpoint_bytes(
        initialize_comp=initialize_comp
    )
  except Exception as e:
    raise common.TaskBuilderException(
        common.CHECKPOINT_BUILDING_ERROR_MESSAGE + str(e)
    )
