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

package(default_visibility = ["//visibility:public"])

java_library(
    name = "tensorflow_plan_session_factory",
    srcs = [
        "TensorflowPlanSessionFactory.java",
    ],
    deps = [
        "@maven//:com_google_protobuf_protobuf_java",
        "@maven//:org_springframework_spring_context",
        # Import fcp last since it tries to pull in an older version of protobuf.
        "@federatedcompute//fcp/java_src/main/java/com/google/fcp/plan",
    ],
)
