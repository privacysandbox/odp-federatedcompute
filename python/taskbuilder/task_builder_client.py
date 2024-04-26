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
from typing import Tuple
import absl
import artifact_utils
import common
import config_validator
import dataset_utils
import http_utils
import learning_process_utils
from shuffler.proto import task_builder_pb2
import task_utils
import tensorflow_federated as tff


class TaskBuilderClient:

  def __init__(self):
    self._model = None
    self._task_config = None
    self._dp_parameters = None
    self._dataset_preprocessor = None
    self._main_learning_process = None
    self._optional_learning_process = None
    self._main_task = None
    self._optional_task = None
    self._artifact_only = common.ARTIFACT_BUILDING_ONLY.value

  def build_task_group(self, model_path: str, task_config_path: str):
    try:
      logging.info(f'Loading model from {model_path}...')
      self._model = task_utils.load_functional_model(model_path=model_path)
      logging.info(f'Loading task config from {task_config_path}...')
      self._task_config = task_utils.load_task_config(
          task_config_path=task_config_path
      )
    except Exception as e:
      logging.exception('Failed to load resources from input: ')
      raise e

    population_name = self._task_config.population_name
    is_training_and_eval = (
        self._task_config.mode
        == task_builder_pb2.TaskMode.Enum.TRAINING_AND_EVAL
    )
    is_eval_only = (
        self._task_config.mode == task_builder_pb2.TaskMode.Enum.EVAL_ONLY
    )
    logging.info(
        'Successfully loaded input. Start building task group under population'
        f' `{population_name}`...'
    )
    # Validate task config
    try:
      config_validator.validate_metadata(task_config=self._task_config)
      logging.info(
          'Basic config is valid. Start validating differential privacy'
          ' setup...'
      )
      # DP accounting validation for training task
      self._dp_parameters = config_validator.validate_fcp_dp(
          task_config=self._task_config
      )
      logging.info('Task config is valid! Start building the task group.')
    except Exception as e:
      logging.exception('Task config is invalid: ')
      raise e

    # Compose learning algorithms based on `learning_process` config
    try:
      self._dataset_preprocessor = dataset_utils.compose_preprocessing_fn(
          model=self._model, label_name=self._task_config.label_name
      )
      self._main_learning_process, self._optional_learning_process = (
          learning_process_utils.compose_iterative_processes(
              model=self._model,
              learning_process=self._task_config.federated_learning.learning_process,
              dp_parameters=self._dp_parameters,
              training_and_eval=is_training_and_eval,
              eval_only=is_eval_only,
          )
      )
    except Exception as e:
      logging.exception(
          'Failed to build learning algorithm based on `learning_process`'
          ' config: '
      )
      raise e

    # Create tasks in TM if task configuration is valid and artifact only mode is disabled.
    # If artifact only is enabled, only artifact URIs will be attached on empty tasks.
    # The created tasks should be a tuple, where the second task is optional.
    try:
      if self._artifact_only:
        logging.info(
            'Artifact only mode is enabled. Skipped the task creation in TM'
            ' server.'
        )
        self._main_task, self._optional_task = (
            task_utils.create_tasks_for_artifact_only_request(
                task_config=self._task_config
            )
        )
      else:
        tasks = task_utils.create_tasks(task_config=self._task_config)
        logging.info(
            'Connecting to task management server: '
            + common.TASK_MANAGEMENT_SERVER.value
        )
        self._main_task, self._optional_task = http_utils.create_task_group(
            tasks=tasks
        )
    except Exception as e:
      logging.exception('Failed to create tasks by task management service: ')
      raise e

    # Build and upload artifacts to designated GCS paths
    build_task_response = artifact_utils.build_and_upload_artifacts(
        task=self._main_task,
        learning_process=self._main_learning_process,
        preprocessing_fn=self._dataset_preprocessor,
    )
    if build_task_response.HasField('task_id'):
      logging.info(
          f'Training artifacts are uploaded for task {self._main_task.task_id}.'
      )
    else:
      logging.exception(
          'Artifact building failed for task'
          f' {population_name}:'
          f' {build_task_response.error_info.error_message}. Stop building'
          ' remaining artifacts.'
      )
      return

    # build artifacts for the associated eval process
    if self._optional_task and self._optional_learning_process:
      build_eval_task_response = artifact_utils.build_and_upload_artifacts(
          task=self._optional_task,
          learning_process=self._optional_learning_process,
          preprocessing_fn=self._dataset_preprocessor,
      )
      if build_eval_task_response.HasField('task_id'):
        logging.info(
            'Training artifacts are uploaded for task'
            f' {build_eval_task_response.task_id}.'
        )
      else:
        logging.exception(
            'Artifact building failed for task'
            f' {population_name}:'
            f' {build_eval_task_response.error_info.error_message}'
        )


def main(argv):
  model_path = common.MODEL_PATH.value
  task_config_path = common.CONFIG_PATH.value
  if not model_path:
    raise ValueError('`--saved_model` is required but not set.')
  if not task_config_path:
    raise ValueError('`--task_config` is required but not set.')
  try:
    task_builder_client = TaskBuilderClient()
    logging.info('Start creating task.')
    task_builder_client.build_task_group(
        model_path=model_path, task_config_path=task_config_path
    )
    logging.info(
        'Success! Tasks are built, and training artifacts are uploaded to the'
        ' cloud.'
    )
  except Exception as e:
    logging.exception('A runtime error occur when building task: ')
    raise e


if __name__ == '__main__':
  absl.app.run(main)
