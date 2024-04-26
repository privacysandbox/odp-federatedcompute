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
from typing import Optional
from absl import flags
import tensorflow as tf

"""Constants for Task Management APIs."""
TASK_MANAGEMENT_V1 = '/taskmanagement/v1/population/'
CREATE_TASK_ACTION = ':create-task'
CANCEL_TASK_ACTION = ':cancel'
PROROBUF_HEADERS = {'Content-Type': 'application/x-protobuf'}

"""Constants for dataset processing"""
BATCH_SIZE = flags.DEFINE_integer(
    name='batch_size', default=3, help='The number of examples in one batch.'
)
MAX_TRAING_BATCHES_PER_CLIENT = flags.DEFINE_integer(
    name='max_training_batches_per_client',
    default=100,
    help='The maximum number of batches used for training in each round.',
)

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
EXAMPLE_COLLECTION_URI = flags.DEFINE_string(
    name='example_collection_uri',
    default='app://test_collection_train',
    help='Example url to retrieve training example inputs',
)
GCP_PROJECT_ID = flags.DEFINE_string(
    name='google_cloud_project',
    default=None,
    help='Google cloud project for artifact uploading/downloading.',
)
TASK_MANAGEMENT_SERVER = flags.DEFINE_string(
    name='task_management_server',
    default='http://localhost:8082/',
    help='Task management server endpoint url.',
)
GCP_TARGET_SCOPE = flags.DEFINE_string(
    name='gcp_target_scope',
    default='https://www.googleapis.com/auth/cloud-platform',
    help='GCP target scope for API authentication.',
)
GCP_SERVICE_ACCOUNT = flags.DEFINE_string(
    name='gcp_service_account',
    default=None,
    help='GCP service account for API authentication.',
)
ARTIFACT_BUILDING_ONLY = flags.DEFINE_boolean(
    name='build_artifact_only',
    default=False,
    help=(
        'Build artifacts only without tasks. Please provide artifact URIs in'
        ' task config.'
    ),
)
GCS_PREFIX = 'gs://'


"""Constants for error messages."""
CONNECTION_FAILURE_MSG = 'Fail to connect: '
DECODE_ERROR_MSG = 'Server returned unreadable response.'
INVALID_URI_ERROR_MESSAGE = (
    'Cannot build artifacts because createTask returns invalid URLs: '
)
BUCKET_NOT_FOUND_ERROR_MESSAGE = (
    'Cannot build artifacts because bucket {bucket_name} does not exist.'
)
ARTIFACT_UPLOADING_ERROR_MESSAGE = 'Cannot upload aritfact to: '
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
DEFAULT_DP_EPSILON = 6.0
DEFAULT_TOTAL_POPULATION = 10000
DEFAULT_DP_DELTA = 1 / DEFAULT_TOTAL_POPULATION
# Just a placeholder, `dp_utils` will calibrate a noise, if not set by adopters.
DEFAULT_DP_NOISE = 0.0

METRICS_ALLOWLIST = {
    'precision': tf.keras.metrics.Precision,
    'mean_squared_error': tf.keras.metrics.MeanSquaredError,
    'root_mean_squared_error': tf.keras.metrics.RootMeanSquaredError,
    'mean_absolute_error': tf.keras.metrics.MeanAbsoluteError,
    'mean_absolute_percentage_error': (
        tf.keras.metrics.MeanAbsolutePercentageError
    ),
    'mean_squared_logarithmic_error': (
        tf.keras.metrics.MeanSquaredLogarithmicError
    ),
    'recall': tf.keras.metrics.Recall,
}

"""Constants for creating tasks."""
DEFAULT_MIN_CLIENT_VERSION = '0'
DEFAULT_MAX_CLIENT_VERSION = '999999999'
DEFAULT_OVER_SELECTION_RATE = 0.3


class RequestType(Enum):
  CREATE_TASK = 1
  CANCEL_TASK = 2


@dataclass
class BlobId:
  bucket: str
  name: Optional[str] = ''


@dataclass
class DpParameter:
  noise_multiplier: float
  dp_clip_norm: float


class TaskBuilderException(Exception):
  pass
