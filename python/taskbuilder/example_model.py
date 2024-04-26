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

"""A binary classification NLP keras model using pre-computed embeddings.

_________________________________________________________________
=================================================================
Layer (type) Output Shape Param #
first_dense_layer (Dense) multiple 262400
second_dense_layer (Dense) multiple 514
softmax (Softmax) multiple 0

Total params: 262914 (1.00 MB)
Trainable params: 262914 (1.00 MB)
Non-trainable params: 0 (0.00 Byte)
=================================================================
"""

import tensorflow as tf

_INPUT_DIM = 1024
_FIRST_DENSE_LAYER_DIM = 256
_SECOND_DENSE_LAYER_DIM = 2


class ExampleKerasModel(tf.keras.Model):
  """A binary classification NLP model using pre-computed embeddings."""

  def __init__(self):
    super().__init__()
    self.dense1_layer = tf.keras.layers.Dense(
        _FIRST_DENSE_LAYER_DIM, activation=tf.nn.relu, name='first_dense_layer'
    )
    self.dense2_layer = tf.keras.layers.Dense(
        units=_SECOND_DENSE_LAYER_DIM,
        activation=tf.nn.sigmoid,
        name='second_dense_layer',
    )
    self.probability_layer = tf.keras.layers.Softmax()

    # We explicitly create model variables by calling self#build here to ensure
    # all necessary information is defined before constructing the federated
    # functional model
    self.build(tf.TensorShape([None, _INPUT_DIM]))
    # NOMUTANTS -- used for logging
    self.summary()

  def call(self, inputs, training=None, mask=None):
    x = self.dense1_layer(inputs)
    x = self.dense2_layer(x)
    return self.probability_layer(x)[:, 0]
