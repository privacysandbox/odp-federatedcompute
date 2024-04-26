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
from absl.testing import absltest
from fcp.artifact_building import artifact_constants
import tensorflow as tf
import tensorflow_checkpoints
import tensorflow_federated as tff


@tff.federated_computation()
def example():
  @tff.tf_computation
  def initialize_tf():
    return collections.OrderedDict(num_rounds=tf.constant(0))

  return tff.federated_value(initialize_tf(), tff.SERVER)


class BuildCheckpointTest(absltest.TestCase):

  def test_build_initial_checkpoint_bytes(self):
    checkpoint_bytes = tensorflow_checkpoints.build_initial_checkpoint_bytes(
        example
    )
    self.assertIsNotNone(checkpoint_bytes)
    checkpoint_filename = self.create_tempfile(
        content=checkpoint_bytes
    ).full_path
    reader = tf.compat.v1.train.NewCheckpointReader(checkpoint_filename)
    tensor_names = []
    expected_tensor_name = (
        f'{artifact_constants.SERVER_STATE_VAR_PREFIX}/num_rounds'
    )
    for tensor_name in reader.get_variable_to_shape_map().keys():
      tensor_names.append(tensor_name)
    self.assertLen(tensor_names, 1)
    self.assertEqual(expected_tensor_name, tensor_names[0])
    self.assertEqual(tf.constant(0), reader.get_tensor(expected_tensor_name))


if __name__ == '__main__':
  absltest.main()
