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

import collections
import functools
from typing import Callable, Mapping, Optional, Tuple, Union
import common
from fcp.artifact_building import data_spec
from fcp.protos import plan_pb2
from shuffler.proto import task_builder_pb2
import tensorflow as tf
import tensorflow_federated as tff

TFIOFeature = Union[
    tf.io.FixedLenFeature,
    tf.io.VarLenFeature,
    tf.io.SparseFeature,
    tf.io.RaggedFeature,
]


def get_data_specs(
    task_config: task_builder_pb2.TaskConfig,
    preprocessing_fn: Callable[[tf.data.Dataset], tf.data.Dataset],
) -> Tuple[data_spec.DataSpec, Optional[data_spec.DataSpec]]:
  mode = task_config.mode
  training_example_selector = plan_pb2.ExampleSelector(
      collection_uri=task_config.federated_learning.learning_process.runtime_config.example_selector_uri
  )
  eval_example_selector = plan_pb2.ExampleSelector(
      collection_uri=task_config.federated_learning.evaluation.example_selector_uri
  )
  if mode == task_builder_pb2.TaskMode.Enum.TRAINING_AND_EVAL:
    return data_spec.DataSpec(
        training_example_selector, preprocessing_fn
    ), data_spec.DataSpec(eval_example_selector, preprocessing_fn)
  elif mode == task_builder_pb2.TaskMode.Enum.EVAL_ONLY:
    return data_spec.DataSpec(eval_example_selector, preprocessing_fn), None
  else:
    return data_spec.DataSpec(training_example_selector, preprocessing_fn), None


def compose_preprocessing_fn(
    model: tff.learning.models.FunctionalModel,
    dataset_policy: task_builder_pb2.DatasetPolicy,
    label_name: Optional[str] = None,
) -> Callable[[tf.data.Dataset], tf.data.Dataset]:
  input_spec = model.input_spec
  if not isinstance(input_spec, Tuple):
    raise TypeError('Input spec must be a tuple')
  feature_spec = input_spec[0]
  if isinstance(feature_spec, tf.TensorSpec):
    label_name = 'y'
  label_spec = input_spec[1]
  if not label_name:
    raise ValueError('Label name cannot be empty.')
  batch_size = dataset_policy.batch_size or common.DEFAULT_BATCH_SIZE
  max_batch_per_client = (
      dataset_policy.max_training_batches_per_client
      or common.DEFAULT_MAX_BATCH_PER_CLIENT
  )
  return functools.partial(
      _dataset_preprocessing_fn,
      feature_spec,
      label_spec,
      label_name,
      batch_size,
      max_batch_per_client,
  )


def parse_tf_example(
    serialized_example: Union[bytes, tf.Tensor],
    feature_decoders: Mapping[str, TFIOFeature],
    label_name: str,
    is_dtype_int32: Mapping[str, bool],
) -> Tuple[collections.OrderedDict[str, tf.Tensor], tf.Tensor]:
  """Parses a serialized `tf.train.Example`.

  Args:
    serialized_example: The serialized example to parse.
    feature_decoders: A mapping from feature names to the configuration for
      parsing the feature from the example.

  Returns:
    A ordered dictionary of parsed examples with feature names as key
    and feature tensors as value.
  """
  features = tf.io.parse_example(serialized_example, features=feature_decoders)
  for feature_name in features:
    if is_dtype_int32[feature_name]:
      features[feature_name] = tf.cast(features[feature_name], tf.int32)
  label = features[label_name]
  del features[label_name]
  if len(features) == 1 and 'x' in features:
    return features['x'], label
  return collections.OrderedDict(sorted(features.items())), label


def _convert_tensor_spec_to_io_spec(
    tensor_spec: tf.TensorSpec,
) -> Tuple[tf.io.FixedLenFeature, bool]:
  # tf.io.parse_example only supports tf.int64 instead of tf.int32.
  # Thus, we need to parse examples into tf.int64 and cast them int tf.int32.
  is_dtype_int32 = tensor_spec.dtype == tf.int32
  return (
      tf.io.FixedLenFeature(
          shape=tensor_spec.shape[1:],
          dtype=tf.int64 if is_dtype_int32 else tensor_spec.dtype,
      ),
      is_dtype_int32,
  )


def _dataset_preprocessing_fn(
    feature_spec: Union[
        tf.TensorSpec, collections.OrderedDict[str, tf.TensorSpec]
    ],
    label_spec: tf.TensorSpec,
    label_name: str,
    batch_size: int,
    max_batch_per_client: int,
    dataset: tf.data.Dataset,
) -> tf.data.Dataset:
  """Dataset preprocessing function to parse serialized examples.

  Args:
    feature_spec: feature tensor spec.
    label_spec: label tensor spec.
    dataset: the original dataset

  Returns:
    The new dataset.
  """
  io_specs_dict = collections.OrderedDict()
  is_dtype_int32 = collections.OrderedDict()
  if isinstance(feature_spec, tf.TensorSpec):
    io_specs_dict['x'], is_dtype_int32['x'] = _convert_tensor_spec_to_io_spec(
        feature_spec
    )
  else:
    for feature_name in feature_spec:
      io_specs_dict[feature_name], is_dtype_int32[feature_name] = (
          _convert_tensor_spec_to_io_spec(feature_spec[feature_name])
      )
  io_specs_dict[label_name], is_dtype_int32[label_name] = (
      _convert_tensor_spec_to_io_spec(label_spec)
  )

  parser = functools.partial(
      parse_tf_example,
      feature_decoders=io_specs_dict,
      label_name=label_name,
      is_dtype_int32=is_dtype_int32,
  )
  return dataset.batch(batch_size).map(parser).take(max_batch_per_client)
