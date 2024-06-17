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
import traceback
from typing import Optional
import common
from fcp.artifact_building import data_spec
from fcp.artifact_building import federated_compute_plan_builder
from fcp.artifact_building import plan_utils
from fcp.protos import plan_pb2
from google.cloud import storage
import io_utils
from shuffler.proto import task_builder_pb2
from shuffler.proto import task_pb2
import support_ops_utils
import tensorflow as tf
import tensorflow_checkpoints
import tensorflow_federated as tff


def build_artifacts(
    learning_process: tff.templates.IterativeProcess,
    dataspec: data_spec.DataSpec,
    flags: task_builder_pb2.ExperimentFlags,
    use_daf: Optional[bool] = False,
):
  """Returns artifacts built from a properly composed learning process and a preprocessing function.

  Args:
      learning_process: a properly composed and DP-noised
        `tff.learning.templates.LearningProcess` built from a TF/TFF model.
      dataspec: a `data_spec.DataSpec` that defines on-device dataset
        processing.

  Raises: TaskBuilderException if any step fails, with an error message.

  Returns:
      Server-side `Plan` that encapsulates all training instuctions for a task,
      including model information and other metadata, such as report goal.
      Client-side `ClientOnlyPlan`, consumed by devices, is a device-side
      subset of `Plan`.
      Initial model weights of the model in bytes.
  """
  logging.log(logging.INFO, 'Start building artifacts...')
  plan = _build_plan(
      learning_process_comp=learning_process.next,
      dataspec=dataspec,
      use_daf=use_daf,
  )
  logging.log(logging.INFO, 'Plan was built successfully.')
  client_plan = _build_client_plan(
      plan=plan, skip_flex_ops_check=flags.skip_flex_ops_check
  )
  logging.log(logging.INFO, 'ClientOnlyPlan was built successfully.')
  checkpoint_bytes = _build_initial_server_checkpoint(
      initialize_comp=learning_process.initialize
  )
  logging.log(logging.INFO, 'Initial checkpoint was built successfully.')
  return plan, client_plan, checkpoint_bytes


def build_and_upload_artifacts(
    task: task_pb2.Task,
    learning_process: tff.templates.IterativeProcess,
    dataspec: data_spec.DataSpec,
    client: storage.Client,
    flags: task_builder_pb2.ExperimentFlags,
    use_daf: Optional[bool] = False,
):
  # check if URIs are valid, otherwise do not start building artifacts.
  logging.log(logging.INFO, 'Validating format of returned URIs...')
  checkpoint_blobs = [
      io_utils.parse_gcs_uri(client=client, uri=checkpoint_url)
      for checkpoint_url in task.init_checkpoint_url
  ]
  plan_blobs = [
      io_utils.parse_gcs_uri(client=client, uri=checkpoint_url)
      for checkpoint_url in task.server_phase_url
  ]
  client_blobs = [
      io_utils.parse_gcs_uri(client=client, uri=checkpoint_url)
      for checkpoint_url in task.client_only_plan_url
  ]
  logging.log(logging.INFO, 'URIs are validated.')

  plan, client_plan, checkpoint_bytes = build_artifacts(
      learning_process=learning_process,
      dataspec=dataspec,
      use_daf=use_daf,
      flags=flags,
  )
  for checkpoint_blob in checkpoint_blobs:
    io_utils.upload_content_to_gcs(
        client=client, blob=checkpoint_blob, data=checkpoint_bytes
    )
  for plan_blob in plan_blobs:
    io_utils.upload_content_to_gcs(
        client=client, blob=plan_blob, data=plan.SerializeToString()
    )
  for client_plan_blob in client_blobs:
    io_utils.upload_content_to_gcs(
        client=client,
        blob=client_plan_blob,
        data=client_plan.SerializeToString(),
    )


def _build_plan(
    learning_process_comp: tff.Computation,
    dataspec: data_spec.DataSpec,
    use_daf: Optional[bool] = False,
) -> plan_pb2.Plan:
  logging.log(logging.INFO, 'Start building plan...')
  try:
    if use_daf:
      logging.log(
          logging.INFO, 'Distributed aggregated form is used for plan builder.'
      )
    plan = federated_compute_plan_builder.build_plan(
        mrf=tff.backends.mapreduce.get_map_reduce_form_for_computation(
            learning_process_comp
        )
        if not use_daf
        else None,
        daf=tff.backends.mapreduce.get_distribute_aggregate_form_for_computation(
            learning_process_comp
        )
        if use_daf
        else None,
        dataspec=dataspec,
        grappler_config=tf.compat.v1.ConfigProto() if use_daf else None,
    )
    logging.log(logging.INFO, 'Adding TFLite graph to the plan...')
    return plan_utils.generate_and_add_flat_buffer_to_plan(plan)
  except Exception as e:
    logging.error('Building plan failed with error: %s', e)
    raise common.TaskBuilderException(
        common.PLAN_BUILDING_ERROR_MESSAGE + traceback.format_exc()
    )


def _build_client_plan(
    plan: plan_pb2.Plan,
    skip_flex_ops_check: Optional[bool] = False,
) -> plan_pb2.ClientOnlyPlan:
  logging.log(logging.INFO, 'Start building ClientOnlyPlan...')
  try:
    """The ClientOnlyPlan corresponding to the Plan proto."""
    if not skip_flex_ops_check:
      support_ops_utils.validate_flex_ops(plan)

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
    logging.exception('Building client plan failed with error: %s', e)
    raise common.TaskBuilderException(
        common.CLIENT_PLAN_BUILDING_ERROR_MESSAGE + traceback.format_exc()
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
