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
from dataclasses import dataclass
from enum import Enum
import functools
from typing import Optional
from absl import flags
from shuffler.proto import task_builder_pb2
import tensorflow as tf
import tensorflow_federated as tff

"""Constants for Task Management APIs."""
TASK_MANAGEMENT_V1 = '/taskmanagement/v1/population/'
TASK_BUILDER_V1 = '/taskbuilder/v1'
CREATE_TASK_ACTION = ':create-task'
CANCEL_TASK_ACTION = ':cancel'
BUILD_TASK_GROUP_ACTION = ':build-task-group'
BUILD_ARTIFACT_ONLY_ACTION = ':build-artifacts'
PROROBUF_HEADERS = {'Content-Type': 'application/x-protobuf'}
VM_METADATA_HEADERS = {'Metadata-Flavor': 'Google'}

COMPUTE_METADATA_SERVER = 'http://metadata.google.internal/computeMetadata/v1'
GCP_PROJECT_ID_KEY = 'GOOGLE_CLOUD_PROJECT'
DEFAULT_PARAM_PREFIX = 'fc'
ENV_KEY = 'ENV'
TASK_MANAGEMENT_SERVER_URL_KEY = 'TASK_MANAGEMENT_SERVER_URL'
GCP_PROJECT_ID_METADATA_KEY = 'project/project-id'

"""Constants for building and uploading training artifacts."""
MODEL_PATH = flags.DEFINE_string(
    name='saved_model',
    help='GCS URI to the SavedModel resource.',
    default=None,
)
CONFIG_PATH = flags.DEFINE_string(
    name='task_config',
    help='GCS URI to the task config resource in pbtxt.',
    default=None,
)
TASK_BUILDER_SERVER = flags.DEFINE_string(
    name='task_builder_server',
    default='http://localhost:5000/',
    help='Task builder server endpoint url.',
)
IMPERSONATE_SERVICE_ACCOUNT = flags.DEFINE_string(
    name='impersonate_service_account',
    default=None,
    help=(
        'Service account to impersonate for accessing the task builder server.'
        ' If not provided it will use the metadata identity endpoint'
    ),
)
ARTIFACT_BUILDING_ONLY = flags.DEFINE_boolean(
    name='build_artifact_only',
    default=False,
    help=(
        'Build artifacts only without tasks. Please provide artifact URIs in'
        ' task config.'
    ),
)
SKIP_FLEX_OPS_CHECK = flags.DEFINE_boolean(
    name='skip_flex_ops_check',
    default=False,
    help=(
        'Build task without checking flex ops availability in android TFLite'
        ' library.'
    ),
)
SKIP_DP_CHECK = flags.DEFINE_boolean(
    name='skip_dp_check',
    default=False,
    help=(
        'Build task without checking targeting differential privacy epsilon. '
        'This flag is temporarily enabled in the beta task builder server.'
    ),
)
SKIP_DP_AGGREGATOR = flags.DEFINE_boolean(
    name='skip_dp_aggregator',
    default=False,
    help=(
        'Build task without applying dp aggregator to the fl learning process. '
        'This flag is temporarily enabled in the beta task builder server.'
    ),
)
E2E_TEST_POPULATION_NAME = flags.DEFINE_string(
    name='population_name',
    default=None,
    help='Population name for task builder e2e test.',
)
API_KEY = flags.DEFINE_string(
    name='api_key',
    default=None,
    help='API Key for task builder access.',
)
GCS_PREFIX = 'gs://'
TEST_BLOB_PATH = 'gs://mock-bucket/mock_blob'

"""Constants for error messages."""
CONNECTION_FAILURE_MSG = 'Fail to connect: '
DECODE_ERROR_MSG = 'Server returned unreadable response.'
INVALID_URI_ERROR_MESSAGE = (
    'Cannot build artifacts because createTask returns invalid URLs: '
)
BUCKET_NOT_FOUND_ERROR_MESSAGE = (
    'Cannot build artifacts because bucket {bucket_name} does not exist.'
)
ARTIFACT_UPLOADING_ERROR_MESSAGE = 'Cannot upload artifact to: '
PLAN_BUILDING_ERROR_MESSAGE = 'Cannot build the plan: '
CLIENT_PLAN_BUILDING_ERROR_MESSAGE = 'Cannot build the ClientOnlyPlan: '
CHECKPOINT_BUILDING_ERROR_MESSAGE = 'Cannot build the initial checkpoint: '
CONFIG_VALIDATOR_ERROR_PREFIX = '[Invalid task config]: '
BAD_VALUE_ERROR_MSG = (
    'key `{key_name}` has a bad value: `{value_name}` in `{entity_name}`.'
    ' {debug_msg}'
)
DP_ACCOUNTING_CHECK_ERROR_MSG = (
    'differential_privacy setup is not private enough to pass DP accounting'
    ' check.'
)
LOADING_MODEL_ERROR_MESSAGE = 'Cannot load TFF functional model from {path}.'
LOADING_CONFIG_ERROR_MESSAGE = 'Cannot load task config from {path}.'

"""Constants for config validation and DP accounting."""
TRAFFIC_WEIGHT_SCALE = 10000
TRAINING_TRAFFIC_WEIGHT = 100
DEFAULT_DP_EPSILON = 10
DEFAULT_TOTAL_POPULATION = 3000000
DEFAULT_DP_DELTA = 1 / DEFAULT_TOTAL_POPULATION
# Just a placeholder, `dp_utils` will calibrate a noise, if not set by adopters.
DEFAULT_DP_NOISE = 0.0
DEFAULT_BATCH_SIZE = 3
DEFAULT_MAX_BATCH_PER_CLIENT = -1
DEFAULT_EXAMPLE_SELECTOR = 'app://test_collection_train'

METRICS_ALLOWLIST = {
    'auc-roc': functools.partial(tf.keras.metrics.AUC, name='auc-roc'),
    'auc-pr': functools.partial(
        tf.keras.metrics.AUC, name='auc-pr', curve='PR'
    ),
    'accuracy': functools.partial(tf.keras.metrics.Accuracy, name='accuracy'),
    'binary_accuracy': functools.partial(
        tf.keras.metrics.BinaryAccuracy, name='binary_accuracy'
    ),
    'binary_crossentropy': functools.partial(
        tf.keras.metrics.BinaryCrossentropy,
        name='binary_crossentropy',
        from_logits=False,
    ),
    'binary_crossentropy_from_logits': functools.partial(
        tf.keras.metrics.BinaryCrossentropy,
        name='binary_crossentropy_from_logits',
        from_logits=True,
    ),
    'categorical_accuracy': functools.partial(
        tf.keras.metrics.CategoricalAccuracy, name='categorical_accuracy'
    ),
    'categorical_crossentropy': functools.partial(
        tf.keras.metrics.CategoricalCrossentropy,
        name='categorical_crossentropy',
        from_logits=False,
    ),
    'categorical_crossentropy_from_logits': functools.partial(
        tf.keras.metrics.CategoricalCrossentropy,
        name='categorical_crossentropy_from_logits',
        from_logits=True,
    ),
    'mean': functools.partial(tf.keras.metrics.Mean, name='mean'),
    'mean_absolute_error': functools.partial(
        tf.keras.metrics.MeanAbsoluteError, name='mean_absolute_error'
    ),
    'mean_absolute_percentage_error': functools.partial(
        tf.keras.metrics.MeanAbsolutePercentageError,
        name='mean_absolute_percentage_error',
    ),
    'mean_squared_error': functools.partial(
        tf.keras.metrics.MeanSquaredError, name='mean_squared_error'
    ),
    'mean_squared_logarithmic_error': functools.partial(
        tf.keras.metrics.MeanSquaredLogarithmicError,
        name='mean_squared_logarithmic_error',
    ),
    'precision': functools.partial(
        tf.keras.metrics.Precision, name='precision'
    ),
    'recall': functools.partial(tf.keras.metrics.Recall, name='recall'),
    'root_mean_squared_error': functools.partial(
        tf.keras.metrics.RootMeanSquaredError, name='root_mean_squared_error'
    ),
    'sparse_categorical_accuracy': functools.partial(
        tf.keras.metrics.SparseCategoricalAccuracy,
        name='sparse_categorical_accuracy',
    ),
    'sparse_categorical_crossentropy': functools.partial(
        tf.keras.metrics.SparseCategoricalCrossentropy,
        name='sparse_categorical_crossentropy',
        from_logits=False,
    ),
    'sparse_categorical_crossentropy_from_logits': functools.partial(
        tf.keras.metrics.SparseCategoricalCrossentropy,
        name='sparse_categorical_crossentropy_from_logits',
        from_logits=True,
    ),
}

"""Constants for creating tasks."""
DEFAULT_MIN_CLIENT_VERSION = '0'
DEFAULT_MAX_CLIENT_VERSION = '999999999'
DEFAULT_OVER_SELECTION_RATE = 0.3


class RequestType(Enum):
  CREATE_TASK = 1
  CANCEL_TASK = 2
  BUILD_TASK_GROUP = 3
  BUILD_ARTIFACT_ONLY = 4


@dataclass
class BlobId:
  bucket: str
  name: Optional[str] = ''


@dataclass
class DpParameter:
  noise_multiplier: float
  dp_clip_norm: float
  dp_delta: float
  dp_epsilon: float
  num_training_rounds: int


@dataclass
class BuildTaskRequest:
  model: tff.learning.models.FunctionalModel
  task_config: task_builder_pb2.TaskConfig
  flags: task_builder_pb2.ExperimentFlags


class TaskBuilderException(Exception):
  pass
