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
import artifact_utils
import common
import config_validator
import dataset_utils
from google.cloud import storage
import http_utils
import io_utils
import learning_process_utils
from shuffler.proto import task_builder_pb2
from shuffler.proto import task_pb2
import task_utils


def build_task_group_request_handler(
    build_task_request: common.BuildTaskRequest,
    artifact_only: Optional[bool] = False,
) -> task_builder_pb2.BuildTaskResponse:
  project_id = io_utils.get_gcp_project_id()
  gcs_client = storage.Client(project=project_id)
  model = build_task_request.model
  task_config = build_task_request.task_config
  flags = build_task_request.flags
  task_report = task_builder_pb2.TaskReport()

  population_name = task_config.population_name
  is_training_and_eval = (
      task_config.mode == task_builder_pb2.TaskMode.Enum.TRAINING_AND_EVAL
  )
  is_eval_only = task_config.mode == task_builder_pb2.TaskMode.Enum.EVAL_ONLY
  use_daf = task_config.use_daf
  logging.info(
      'Successfully loaded input. Start building task group under population'
      f' `{population_name}`...'
  )
  # Validate task config
  try:
    config_validator.validate_metadata(task_config=task_config)
    logging.info(
        'Basic config is valid. Start validating differential privacy setup...'
    )
    # DP accounting validation for training task
    dp_parameters = config_validator.validate_fcp_dp(task_config, flags)
    dp_parameters_proto = task_builder_pb2.TaskReport.DPHyperparameters(
        dp_delta=dp_parameters.dp_delta,
        dp_epsilon=dp_parameters.dp_epsilon,
        noise_multiplier=dp_parameters.noise_multiplier,
        dp_clip_norm=dp_parameters.dp_clip_norm,
        num_training_rounds=dp_parameters.num_training_rounds,
    )

    task_report.dp_hyperparameters.CopyFrom(dp_parameters_proto)
    logging.info('Task config is valid! Start building the task group.')
  except common.TaskBuilderException as e:
    return _pack_task_builder_error(
        task_builder_pb2.ErrorType.Enum.INVALID_REQUEST,
        f'Task config is invalid: {str(e)}',
        task_report=task_report,
    )

  # Compose learning algorithms based on `learning_process` config
  try:
    dataset_preprocessor = dataset_utils.compose_preprocessing_fn(
        model=model,
        dataset_policy=task_config.policies.dataset_policy,
        label_name=task_config.label_name,
    )
    training_iterative_process, evaluation_iterative_process, task_report = (
        learning_process_utils.compose_iterative_processes(
            model=model,
            learning_process=task_config.federated_learning.learning_process,
            dp_parameters=dp_parameters,
            training_and_eval=is_training_and_eval,
            eval_only=is_eval_only,
            flags=flags,
            task_report=task_report,
        )
    )
  except Exception as e:
    return _pack_task_builder_error(
        task_builder_pb2.ErrorType.Enum.INVALID_REQUEST,
        'Failed to build learning algorithm based on `learning_process`'
        f' config: {str(e)}',
        task_report=task_report,
    )

  if artifact_only:
    main_task, optional_task = (
        task_utils.create_tasks_for_artifact_only_request(
            task_config=task_config
        )
    )
  else:
    main_task, optional_task = task_utils.create_tasks(task_config=task_config)

  # Build artifacts for training and evaluation task if exists.
  try:
    main_data_spec, optional_data_spec = dataset_utils.get_data_specs(
        task_config=task_config, preprocessing_fn=dataset_preprocessor
    )
    main_task_plan, main_task_client_plan, main_task_checkpoint = (
        artifact_utils.build_artifacts(
            task=main_task,
            learning_process=evaluation_iterative_process
            if is_eval_only
            else training_iterative_process,
            dataspec=main_data_spec,
            use_daf=use_daf,
            flags=flags,
        )
    )
    logging.info(f'Build artifacts for task {main_task.task_id}.')
  except common.TaskBuilderException as e:
    return _pack_task_builder_error(
        task_builder_pb2.ErrorType.Enum.ARTIFACT_BUILDING_ERROR,
        f'Artifact building failed for task {main_task.task_id}: {str(e)}. Stop'
        ' building remaining artifacts.',
        task_report=task_report,
    )

  if is_training_and_eval:
    try:
      (
          optional_task_plan,
          optional_task_client_plan,
          optional_task_checkpoint,
      ) = artifact_utils.build_artifacts(
          task=optional_task,
          learning_process=evaluation_iterative_process,
          dataspec=optional_data_spec,
          use_daf=use_daf,
          flags=flags,
      )
      logging.info(f'Build artifacts for task {optional_task.task_id}.')
    except common.TaskBuilderException as e:
      return _pack_task_builder_error(
          task_builder_pb2.ErrorType.Enum.ARTIFACT_BUILDING_ERROR,
          f'Artifact building failed for task {optional_task.task_id}:'
          f' {str(e)}. Stop building remaining artifacts.',
          task_report=task_report,
      )

  # Create tasks in TM if task configuration is valid and artifact only mode
  # is disabled. If artifact only is enabled, only artifact URIs will be
  # attached on empty tasks. The created tasks should be a tuple, where the
  # second task is optional.
  try:
    if not artifact_only:
      task_management_server = io_utils.get_task_management_server(
          project_id=project_id
      )
      if not task_management_server:
        return _pack_task_builder_error(
            task_builder_pb2.ErrorType.Enum.TASK_MANAGEMENT_ERROR,
            'Cannot find task management server.',
            task_report=task_report,
        )
      logging.info(
          f'Connecting to task management server: {task_management_server}'
      )
      main_task, optional_task = http_utils.create_task_group(
          tm_server=task_management_server, tasks=(main_task, optional_task)
      )
  except common.TaskBuilderException as e:
    return _pack_task_builder_error(
        task_builder_pb2.ErrorType.Enum.TASK_MANAGEMENT_ERROR,
        f'Failed to create tasks by task management service: {str(e)}',
        task_report=task_report,
    )

  # build artifacts for the associated eval process, if exists
  try:
    artifact_utils.upload_artifacts(
        task=main_task,
        plan=main_task_plan,
        client_plan=main_task_client_plan,
        client=gcs_client,
        checkpoint_bytes=main_task_checkpoint,
    )
    logging.info(f'Artifacts are uploaded for task {main_task.task_id}.')
  except common.TaskBuilderException as e:
    return _pack_task_builder_error(
        task_builder_pb2.ErrorType.Enum.ARTIFACT_BUILDING_ERROR,
        f'Artifact failed to upload for task {main_task.task_id}:'
        f' {str(e)}. Stop building remaining artifacts.',
        task_report=task_report,
    )

  if is_training_and_eval:
    try:
      artifact_utils.upload_artifacts(
          task=optional_task,
          plan=optional_task_plan,
          client_plan=optional_task_client_plan,
          client=gcs_client,
          checkpoint_bytes=optional_task_checkpoint,
      )
      logging.info(f'Artifacts are uploaded for task {optional_task.task_id}.')
    except common.TaskBuilderException as e:
      return _pack_task_builder_error(
          task_builder_pb2.ErrorType.Enum.ARTIFACT_BUILDING_ERROR,
          f'Artifact failed to upload for task {main_task.task_id}:'
          f' {str(e)}. Stop building remaining artifacts.',
          task_report=task_report,
      )
  return _pack_task_builder_success(
      main_task=main_task,
      optional_task=optional_task,
      task_report=task_report,
      is_eval_only=is_eval_only,
      is_training_and_eval=is_training_and_eval,
  )


def _pack_task_builder_error(
    error_type: task_builder_pb2.ErrorType.Enum,
    error_msg: str,
    task_report: task_builder_pb2.TaskReport,
) -> task_builder_pb2.BuildTaskResponse:
  return task_builder_pb2.BuildTaskResponse(
      error_info=task_builder_pb2.ErrorInfo(
          error_type=error_type,
          error_message=error_msg,
      ),
      task_report=task_report,
  )


def _pack_task_builder_success(
    main_task: task_pb2.Task,
    optional_task: Optional[task_pb2.Task],
    task_report: task_builder_pb2.TaskReport,
    is_eval_only: Optional[bool] = False,
    is_training_and_eval: Optional[bool] = False,
) -> task_builder_pb2.BuildTaskResponse:
  if is_eval_only:
    return task_builder_pb2.BuildTaskResponse(
        task_group=task_builder_pb2.TaskGroup(eval_task=main_task),
        task_report=task_report,
    )
  if is_training_and_eval:
    return task_builder_pb2.BuildTaskResponse(
        task_group=task_builder_pb2.TaskGroup(
            training_task=main_task, eval_task=optional_task
        ),
        task_report=task_report,
    )
  return task_builder_pb2.BuildTaskResponse(
      task_group=task_builder_pb2.TaskGroup(training_task=main_task),
      task_report=task_report,
  )
