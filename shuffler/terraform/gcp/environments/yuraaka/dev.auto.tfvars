/**
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

# Example values required by shuffler.tf
#
# These values should be modified for each of your environments.

environment = "yuraaka"
project_id  = "jetbrains-fed-compute"
region      = "europe-west2"

client_gradient_bucket_location = "eu"
model_bucket_location           = "eu"

parent_domain_name      = "jbf.app"
spanner_instance_config = "regional-europe-west4"

# Workload Images
aggregator_image      = "europe-west2-docker.pkg.dev/jetbrains-fed-compute/odp-w2/aggregator_image:latest"
model_updater_image   = "europe-west2-docker.pkg.dev/jetbrains-fed-compute/odp-w2/model_updater_image:latest"
task_management_image = "europe-west4-docker.pkg.dev/jetbrains-fed-compute/odp/task_management_image:latest"
task_builder_image    = "europe-west4-docker.pkg.dev/jetbrains-fed-compute/odp/task_builder_image:latest"

# Coordinator Configuration
allowed_operator_service_accounts = "coordinated-fedor@jetbrains-fed-compute.iam.gserviceaccount.com"

encryption_key_service_a_base_url          = "https://coordinator-a.com"
encryption_key_service_a_cloudfunction_url = "https://coordinator-a-xyz.a.run.app"
wip_provider_a                             = "projects/291466669625/locations/global/workloadIdentityPools/odp-pool/provider/tee-provider-a"
service_account_a                          = "tee-operator-a@jetbrains-fed-compute.iam.gserviceaccount.com"
encryption_key_service_b_base_url          = "https://coordinator-b.com"
encryption_key_service_b_cloudfunction_url = "https://coordinator-b-xyz.a.run.app"
wip_provider_b                             = "projects/291466669625/locations/global/workloadIdentityPools/odp-pool/provider/tee-provider-b"
service_account_b                          = "tee-operator-b@jetbrains-fed-compute.iam.gserviceaccount.com"

deletion_protection=false