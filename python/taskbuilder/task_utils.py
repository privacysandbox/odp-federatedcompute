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

import os
import tempfile
from typing import Optional, Tuple
import common
from google.cloud import storage
from google.protobuf import text_format
from shuffler.proto import task_builder_pb2
from shuffler.proto import task_pb2
import tensorflow_federated as tff


def create_tasks(
    task_config: task_builder_pb2.TaskConfig,
) -> Tuple[task_pb2.Task, Optional[task_pb2.Task]]:
  """Create tasks given well-validated task configurations.

  Args:
    task_config: A well-validated task configuration.

  Returns:
    A tuple of tasks, the second can be None, if the task mode
    is training or evaluation only.
  """
  mode = task_config.mode
  if mode == task_builder_pb2.TaskMode.Enum.TRAINING_AND_EVAL:
    return _create_training_task(task_config=task_config), _create_eval_task(
        task_config=task_config
    )
  elif mode == task_builder_pb2.TaskMode.Enum.EVAL_ONLY:
    return _create_eval_task(task_config=task_config), None
  else:
    return _create_training_task(task_config=task_config), None


def create_tasks_for_artifact_only_request(
    task_config: task_builder_pb2.TaskConfig,
) -> Tuple[task_pb2.Task, Optional[task_pb2.Task]]:
  """Create tasks with only artifact URIs.

  Args:
    task_config: A well-validated task configuration.

  Returns:
    A tuple of tasks, the second can be None, if the task mode
    is training or evaluation only.
  """
  mode = task_config.mode
  training_artifact_uris = (
      task_config.federated_learning.learning_process.artifact_building
  )
  eval_artifact_uris = (
      task_config.federated_learning.evaluation.artifact_building
  )
  if mode == task_builder_pb2.TaskMode.Enum.TRAINING_AND_EVAL:
    return _attach_uris_to_task(
        uris=training_artifact_uris
    ), _attach_uris_to_task(uris=eval_artifact_uris)
  elif mode == task_builder_pb2.TaskMode.Enum.EVAL_ONLY:
    return _attach_uris_to_task(uris=eval_artifact_uris), None
  else:
    return _attach_uris_to_task(uris=training_artifact_uris), None


def load_functional_model(
    model_path: str, client: Optional[storage.Client] = None
) -> Optional[tff.learning.models.FunctionalModel]:
  """Load a TFF functional model as `tff.learning.models.FunctionalModel` from a GCS `model_path`."""
  functional_model = None
  try:
    if not client:
      client = storage.Client(project=common.GCP_PROJECT_ID.value)
    blob_id = _parse_gcs_uri(uri=model_path, allow_empty_blob=True)
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
    task_config_path: str, client: Optional[storage.Client] = None
) -> Optional[task_builder_pb2.TaskConfig]:
  """Load task configurations as `task_builder_pb2.TaskConfig` from a pbtxt file on GCS."""
  task_config = None
  try:
    if not client:
      client = storage.Client(project=common.GCP_PROJECT_ID.value)
    blob_id = _parse_gcs_uri(uri=task_config_path)
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


def _parse_gcs_uri(uri: str, allow_empty_blob: bool = False) -> common.BlobId:
  """Parse a GCS URI to a `BlobId` that represents a GCS blob or directory."""
  uri_without_prefix = uri[len(common.GCS_PREFIX) :]
  temp = uri_without_prefix.split('/', 1)
  # support bucket-only scenario for model path
  if allow_empty_blob and len(temp) == 1:
    return common.BlobId(bucket=temp[0])
  bucket_name, object_name = temp[0], temp[1]
  return common.BlobId(bucket=bucket_name, name=object_name)


def _create_training_task(
    task_config: task_builder_pb2.TaskConfig,
) -> task_pb2.Task:
  population_name = task_config.population_name
  num_training_rounds = (
      task_config.policies.model_release_policy.num_max_training_rounds
  )
  training_reporting_goal = (
      task_config.federated_learning.learning_process.runtime_config.report_goal
  )
  training_over_selection_rate = (
      task_config.federated_learning.learning_process.runtime_config.over_selection_rate
  )
  min_separation_policy = task_config.policies.min_separation_policy
  data_availability_policy = task_config.policies.data_availability_policy

  # build training task based on configs
  eligibility_info = task_pb2.EligibilityTaskInfo()
  eligibility_info.eligibility_policies.append(
      task_pb2.EligibilityPolicyEvalSpec(min_sep_policy=min_separation_policy)
  )
  eligibility_info.eligibility_policies.append(
      task_pb2.EligibilityPolicyEvalSpec(
          data_availability_policy=data_availability_policy
      )
  )
  training_info = task_pb2.TrainingInfo(eligibility_task_info=eligibility_info)
  return task_pb2.Task(
      population_name=population_name,
      total_iteration=num_training_rounds,
      min_aggregation_size=training_reporting_goal,
      max_aggregation_size=_get_max_aggregation_size(
          min_aggregation_size=training_reporting_goal,
          over_selection_rate=training_over_selection_rate,
      ),
      min_client_version=common.DEFAULT_MIN_CLIENT_VERSION,
      max_client_version=common.DEFAULT_MAX_CLIENT_VERSION,
      info=task_pb2.TaskInfo(
          traffic_weight=common.TRAINING_TRAFFIC_WEIGHT,
          training_info=training_info,
      ),
  )


def _create_eval_task(
    task_config: task_builder_pb2.TaskConfig,
) -> task_pb2.Task:
  population_name = task_config.population_name
  num_training_rounds = (
      task_config.policies.model_release_policy.num_max_training_rounds
  )
  evalution_config = task_config.federated_learning.evaluation
  eval_reporting_goal = evalution_config.report_goal
  source_training_population = evalution_config.source_training_population
  if not source_training_population:
    source_training_population = population_name

  eval_over_selection_rate = evalution_config.over_selection_rate
  checkpoint_selector_config = evalution_config.checkpoint_selector
  if not checkpoint_selector_config:
    checkpoint_selector_config = 'every_1_round'
  temp = checkpoint_selector_config.split('_')
  k = int(temp[1])
  selector_type = temp[2]

  # build evaluation task based on configs
  checkpoint_selector = task_pb2.CheckPointSelector()
  if selector_type == 'round':
    checkpoint_selector.iteration_selector.CopyFrom(
        task_pb2.EveryKIterationsCheckpointSelector(size=k)
    )
  elif selector_type == 'hour':
    checkpoint_selector.duration_selector.CopyFrom(
        task_pb2.EveryKHoursCheckpointSelector(hours=k)
    )

  eval_info = task_pb2.EvaluationInfo(
      check_point_selector=checkpoint_selector,
      training_population_name=source_training_population,
  )
  return task_pb2.Task(
      population_name=population_name,
      total_iteration=num_training_rounds,
      min_aggregation_size=eval_reporting_goal,
      max_aggregation_size=_get_max_aggregation_size(
          min_aggregation_size=eval_reporting_goal,
          over_selection_rate=eval_over_selection_rate,
      ),
      min_client_version=common.DEFAULT_MIN_CLIENT_VERSION,
      max_client_version=common.DEFAULT_MAX_CLIENT_VERSION,
      info=task_pb2.TaskInfo(
          traffic_weight=_get_eval_traffic_weight(
              evalution_config.evaluation_traffic
          ),
          evaluation_info=eval_info,
      ),
  )


# Rescale the percentage of evaluation traffic to the evaluation traffic weight,
# an integer in the domain space [0, 10000], relative to a fixed traffic weight
# of the associated training task, which is set to 100.
def _get_eval_traffic_weight(evaluation_traffic: float) -> int:
  if evaluation_traffic == 1.0:
    return common.TRAFFIC_WEIGHT_SCALE
  rescaled_eval_traffic = evaluation_traffic * common.TRAINING_TRAFFIC_WEIGHT
  return (int)(rescaled_eval_traffic / (1.0 - evaluation_traffic))


# min_aggregation_size = report_goal
# max_aggregation_size = min_aggregation_size * (1.0 + over_selection_rate)
# Default over selection rate is 30% if not provided.
def _get_max_aggregation_size(
    min_aggregation_size: int, over_selection_rate: float
) -> int:
  if not over_selection_rate:
    over_selection_rate = common.DEFAULT_OVER_SELECTION_RATE
  return int(min_aggregation_size * (1.0 + over_selection_rate))


def _attach_uris_to_task(
    uris: task_builder_pb2.ArtifactBuilding,
) -> task_pb2.Task:
  task = task_pb2.Task()
  task.client_only_plan_url.extend([uris.client_plan_url])
  task.server_phase_url.extend([uris.plan_url])
  task.init_checkpoint_url.extend([uris.checkpoint_url])
  return task
