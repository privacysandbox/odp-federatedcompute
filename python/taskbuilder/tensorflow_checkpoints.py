# Copyright 2024 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import os
import tempfile

from fcp.artifact_building import checkpoint_utils
from fcp.artifact_building import graph_helpers
from fcp.artifact_building import plan_utils
from fcp.protos import plan_pb2
import tensorflow as tf
import tensorflow_federated as tff


def build_initial_checkpoint_bytes(
    initialize_comp: tff.Computation,
) -> bytes:
  initialize_tf_comp = (
      tff.backends.mapreduce.get_state_initialization_computation(
          initialize_comp
      )
  )

  with tf.Graph().as_default():
    # Creates the variables to hold the server state and the associated saver.
    server_state_vars, _, _, savepoint = (
        checkpoint_utils.create_server_checkpoint_vars_and_savepoint(
            server_state_type=initialize_comp.type_signature.result,
            write_metrics_to_checkpoint=False,
        )
    )

    # Embeds the `initialize` logic.
    initial_values = graph_helpers.import_tensorflow(
        'initialize', initialize_tf_comp
    )

    # Executes all ops and creates the checkpoint.
    assign_ops = [
        tf.nest.map_structure(
            lambda variable, value: variable.assign(value),
            server_state_vars,
            initial_values,
        )
    ]
    initialize = tf.compat.v1.global_variables_initializer()
    with tf.compat.v1.Session() as sess:
      sess.run(initialize)
      sess.run(assign_ops)
      result = _create_checkpoint(sess, savepoint)
    return result


def _create_checkpoint(
    sess: tf.compat.v1.Session, checkpoint_op: plan_pb2.CheckpointOp
) -> bytes:
  # TODO (b/308453073): consider using a memory-based file system to avoid disk I/O.
  temp_file = tempfile.mktemp()
  try:
    plan_utils.write_checkpoint(sess, checkpoint_op, temp_file)
    with tf.io.gfile.GFile(temp_file, 'rb') as f:
      return f.read()
  finally:
    if os.path.exists(temp_file):
      os.remove(temp_file)
