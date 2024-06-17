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

import csv
import logging
import os
import tempfile
import common
from fcp.protos import plan_pb2
import pkg_resources
import tflite_flex_ops


def validate_flex_ops(plan: plan_pb2.Plan):
  logging.log(logging.INFO, "Validating flex ops in client tflite graph...")
  supported_ops = load_supported_ops()
  unsupported_ops = {}
  model_temp_file = tempfile.mktemp()
  try:
    with open(model_temp_file, "wb") as temp_file:
      temp_file.write(plan.client_tflite_graph_bytes)

    ops_kernel_map = tflite_flex_ops.AddFlexOpsFromModel(model_temp_file)
    for op_name, op_kernel in ops_kernel_map.items():
      if op_name not in supported_ops:
        unsupported_ops[op_name] = op_kernel
  finally:
    os.remove(model_temp_file)
  if len(unsupported_ops) > 0:
    raise common.TaskBuilderException(
        common.CLIENT_PLAN_BUILDING_ERROR_MESSAGE
        + "Please contact Google to register these ops: "
        + str(unsupported_ops)
    )


def load_supported_ops():
  logging.log(logging.INFO, "Loading supported flex ops from file...")
  result_map = {}
  # support_ops.csv stores {OnDevicePersonalization mainline release version, TF ops} that
  # android Tensorflow Lite library supports. The list will keep up to date with android
  # mainline release.
  op_path = pkg_resources.resource_filename(
      "support_ops_utils", "support_ops.csv"
  )
  with open(op_path, "r", newline="") as csvfile:
    reader = csv.reader(csvfile)
    for row in reader:
      version, op = row[0], row[1]
      result_map[op] = version
  return result_map
