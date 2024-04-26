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

package(default_visibility = ["//visibility:public"])

java_library(
    name = "key_attestation_manager",
    srcs = ["KeyAttestationManager.java"],
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:constants",
        "@coordinator-services-and-shared-libraries//:error_utils",
        "@coordinator-services-and-shared-libraries//:http_client_wrapper",
        "@maven//:com_google_code_gson_gson",
        "@maven//:com_google_guava_guava",
        "@maven//:org_apache_httpcomponents_httpclient",
        "@maven//:org_apache_httpcomponents_httpcore",
        "@maven//:org_slf4j_slf4j_api",
        "@maven//:org_springframework_spring_beans",
        "@maven//:org_springframework_spring_context",
    ],
)

java_library(
    name = "authentication_filter",
    srcs = ["AuthenticationFilter.java"],
    deps = [
        ":key_attestation_manager",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:constants",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:unique_id_generator",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao:authorization_token_dao",
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common/dao/gcp:authorization_token_spanner_dao",
        "//shuffler/proto:task_assignments_java_proto",
        "@federatedcompute//fcp/protos/federatedcompute:federated_compute_java_proto",
        "@maven//:com_google_protobuf_protobuf_java",
        "@maven//:jakarta_servlet_jakarta_servlet_api",
        "@maven//:org_slf4j_slf4j_api",
        "@maven//:org_springframework_spring_beans",
        "@maven//:org_springframework_spring_context",
        "@maven//:org_springframework_spring_web",
        "@maven//:org_springframework_spring_webmvc",
    ],
)

java_library(
    name = "authentication_filter_registration_bean",
    srcs = ["AuthenticationFilterRegistrationBean.java"],
    deps = [
        ":authentication_filter",
        "@maven//:org_springframework_boot_spring_boot",
        "@maven//:org_springframework_boot_spring_boot_autoconfigure",
        "@maven//:org_springframework_boot_spring_boot_starter_web",
        "@maven//:org_springframework_spring_beans",
        "@maven//:org_springframework_spring_context",
        "@maven//:org_springframework_spring_web",
        "@maven//:org_springframework_spring_webmvc",
    ],
)

java_library(
    name = "key_attestation_config",
    srcs = ["AndroidKeyAttestationConfig.java"],
    deps = [
        "//java/src/main/java/com/google/ondevicepersonalization/federatedcompute/shuffler/common:constants",
        "@maven//:com_google_auth_google_auth_library_oauth2_http",
        "@maven//:com_google_guava_guava",
        "@maven//:org_apache_httpcomponents_httpclient",
        "@maven//:org_apache_httpcomponents_httpcore",
        "@maven//:org_springframework_boot_spring_boot_autoconfigure",
        "@maven//:org_springframework_spring_beans",
        "@maven//:org_springframework_spring_context",
    ],
)
