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
import tensorflow as tf
import tensorflow_federated as tff

TFIOFeature = Union[
    tf.io.FixedLenFeature,
    tf.io.VarLenFeature,
    tf.io.SparseFeature,
    tf.io.RaggedFeature,
]


def compose_preprocessing_fn(
    model: tff.learning.models.FunctionalModel, label_name: Optional[str] = None
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
  return functools.partial(
      _dataset_preprocessing_fn,
      feature_spec,
      label_spec,
      label_name,
  )


def parse_tf_example(
    serialized_example: Union[bytes, tf.Tensor],
    feature_decoders: Mapping[str, TFIOFeature],
    label_name: str,
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
  label = features[label_name]
  del features[label_name]
  if len(features) == 1 and 'x' in features:
    return features['x'], label
  return collections.OrderedDict(sorted(features.items())), label


def _convert_tensor_spec_to_io_spec(
    tensor_spec: tf.TensorSpec,
) -> tf.io.FixedLenFeature:
  return tf.io.FixedLenFeature(
      shape=[tensor_spec.shape[1]], dtype=tensor_spec.dtype
  )


def _dataset_preprocessing_fn(
    feature_spec: Union[
        tf.TensorSpec, collections.OrderedDict[str, tf.TensorSpec]
    ],
    label_spec: tf.TensorSpec,
    label_name: str,
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
  if isinstance(feature_spec, tf.TensorSpec):
    io_specs_dict['x'] = _convert_tensor_spec_to_io_spec(feature_spec)
  else:
    for feature_name in feature_spec:
      io_specs_dict[feature_name] = _convert_tensor_spec_to_io_spec(
          feature_spec[feature_name]
      )
  io_specs_dict[label_name] = _convert_tensor_spec_to_io_spec(label_spec)

  parser = functools.partial(
      parse_tf_example,
      feature_decoders=io_specs_dict,
      label_name=label_name,
  )
  return (
      dataset.batch(common.BATCH_SIZE.value)
      .map(parser)
      .take(common.MAX_TRAING_BATCHES_PER_CLIENT.value)
  )
