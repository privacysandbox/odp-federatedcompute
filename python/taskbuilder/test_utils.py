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
import example_model
import tensorflow as tf
import tensorflow_federated as tff

"""
Test utilities to build simple models.
"""


def _get_input_spec() -> Tuple[tf.TensorSpec, tf.TensorSpec]:
  """Builds the input spec for the example model."""
  return (
      tf.TensorSpec(shape=[None, 1024], dtype=tf.float32),
      tf.TensorSpec(shape=[None, 1], dtype=tf.float32),
  )


def get_functional_model_without_metrics() -> (
    tff.learning.models.FunctionalModel
):
  return tff.learning.models.functional_model_from_keras(
      keras_model=example_model.ExampleKerasModel(),
      loss_fn=tf.keras.losses.BinaryCrossentropy(from_logits=False),
      input_spec=_get_input_spec(),
  )
