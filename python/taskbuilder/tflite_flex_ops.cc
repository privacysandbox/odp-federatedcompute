// Copyright 2024 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

#include <pybind11/pybind11.h>
#include <pybind11/stl.h>  // For converting C++ containers

#include "tensorflow/lite/model.h"
#include "tensorflow/lite/tools/list_flex_ops.h"

    namespace py = pybind11;

PYBIND11_MODULE(tflite_flex_ops, m) {
  m.def(
      "AddFlexOpsFromModel",
      [](const std::string& model_path) {
        // Load the model
        std::unique_ptr<tflite::FlatBufferModel> model =
            tflite::FlatBufferModel::BuildFromFile(model_path.c_str());
        if (!model) {
            throw std::runtime_error("Failed to load model");
          }

        // Create OpKernelSet and call the function
          tflite::flex::OpKernelSet flex_ops;
          tflite::flex::AddFlexOpsFromModel(model->GetModel(), &flex_ops);
          std::map<std::string, std::string> ops_kernel_map;
          for (const tflite::flex::OpKernel& op : flex_ops) {
            ops_kernel_map[op.op_name] = op.kernel_name;
          }
          return ops_kernel_map;
      },
      py::arg("model_path"));
}