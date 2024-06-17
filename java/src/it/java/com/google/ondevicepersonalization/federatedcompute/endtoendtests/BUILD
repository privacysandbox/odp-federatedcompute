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

java_library(
    name = "end_to_end_args",
    srcs = [
        "EndToEndArgs.java",
    ],
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:lombok_java",
        "@maven//:com_beust_jcommander",
    ],
)

java_binary(
    name = "end_to_end_test",
    srcs = ["EndToEndTest.java"],
    main_class = "com.google.ondevicepersonalization.federatedcompute.endtoendtests.EndToEndTest",
    resource_strip_prefix = "java/src/it/java/",
    resources = ["//java/src/it/java/com/google/ondevicepersonalization/federatedcompute/endtoendtests/resources"],
    deps = [
        ":end_to_end_args",
        "//java/src/it/java/com/google/ondevicepersonalization/federatedcompute/endtoendtests/clients:device",
        "//java/src/it/java/com/google/ondevicepersonalization/federatedcompute/endtoendtests/clients:partner",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:compression_utils",
        "//shuffler/proto:task_java_proto",
        "@maven//:com_beust_jcommander",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_truth_truth",
        "@maven//:junit_junit",
    ],
)
