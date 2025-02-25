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

from typing import Tuple
import common
import example_model
from shuffler.proto import common_pb2
from shuffler.proto import task_builder_pb2
import tensorflow as tf
import tensorflow_federated as tff

"""
Test utilities for building models and task configs.
"""


def _get_input_spec() -> Tuple[tf.TensorSpec, tf.TensorSpec]:
  """Builds the input spec for the example model."""
  return (
      tf.TensorSpec(shape=[None, 784], dtype=tf.float32),
      tf.TensorSpec(shape=[None, 1], dtype=tf.int64),
  )


def get_functional_model_without_metrics() -> (
    tff.learning.models.FunctionalModel
):
  return tff.learning.models.functional_model_from_keras(
      keras_model=example_model.ExampleKerasModel(),
      loss_fn=tf.keras.losses.BinaryCrossentropy(from_logits=False),
      input_spec=_get_input_spec(),
  )


def get_good_training_and_eval_task_config() -> task_builder_pb2.TaskConfig:
  """Builds a well-validated task config, which passes DP accounting check."""
  return task_builder_pb2.TaskConfig(
      mode=task_builder_pb2.TaskMode.Enum.TRAINING_AND_EVAL,
      population_name='my_new_population',
      label_name='y',
      policies=task_builder_pb2.Policies(
          min_separation_policy=common_pb2.MinimumSeparationPolicy(
              minimum_separation=3
          ),
          data_availability_policy=common_pb2.DataAvailabilityPolicy(
              min_example_count=100
          ),
          model_release_policy=task_builder_pb2.ModelReleaseManagementPolicy(
              dp_target_epsilon=6,
              dp_delta=0.000000001,
              num_max_training_rounds=1000,
          ),
          dataset_policy=task_builder_pb2.DatasetPolicy(
              batch_size=3,
              max_training_batches_per_client=100,
          ),
      ),
      federated_learning=task_builder_pb2.FederatedLearning(
          learning_process=task_builder_pb2.LearningProcess(
              client_learning_rate=0.01,
              server_learning_rate=1.0,
              runtime_config=task_builder_pb2.RuntimeConfig(
                  report_goal=2000,
                  over_selection_rate=0.3,
                  example_selector_uri='training_collection',
              ),
              artifact_building=task_builder_pb2.ArtifactBuilding(
                  plan_url=common.TEST_BLOB_PATH,
                  client_plan_url=common.TEST_BLOB_PATH,
                  checkpoint_url=common.TEST_BLOB_PATH,
              ),
          ),
          evaluation=task_builder_pb2.Evaluation(
              source_training_population='source_population',
              checkpoint_selector='every_2_round',
              evaluation_traffic=0.1,
              report_goal=200,
              over_selection_rate=0.3,
              example_selector_uri='eval_collection',
              artifact_building=task_builder_pb2.ArtifactBuilding(
                  plan_url=common.TEST_BLOB_PATH,
                  client_plan_url=common.TEST_BLOB_PATH,
                  checkpoint_url=common.TEST_BLOB_PATH,
              ),
              source_training_task_id=1,
          ),
      ),
      differential_privacy=task_builder_pb2.DifferentialPrivacy(
          noise_multiplier=6.0, clip_norm=0.1
      ),
  )
