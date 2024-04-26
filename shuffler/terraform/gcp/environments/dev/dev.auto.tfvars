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

environment = "demo-env"
project_id  = "demo-project"
region      = "us-central1"

parent_domain_name      = "demo-domain-xyz.com"
spanner_instance_config = "regional-us-central1"

# Workload Images
aggregator_image      = "<location>/<project>/<repository>/<image>:<tag or digest>"
model_updater_image   = "<location>/<project>/<repository>/<image>:<tag or digest>"
task_management_image = "<location>/<project>/<repository>/<image>:<tag or digest>"

# Coordinator Configuration
allowed_operator_service_accounts = "ca-opallowedusr@dev-env.iam.gserviceaccount.com,cb-opallowedusr@dev-env.iam.gserviceaccount.com"

encryption_key_service_a_base_url          = "https://coordinator-a.com"
encryption_key_service_a_cloudfunction_url = "https://coordinator-a-xyz.a.run.app"
wip_provider_a                             = "operator-a@dev-env.iam.gserviceaccount.com"
service_account_a                          = "projects/1234567890/locations/global/workloadIdentityPools/opwip-a/providers/"
encryption_key_service_b_base_url          = "https://coordinator-b.com"
encryption_key_service_b_cloudfunction_url = "https://coordinator-b-xyz.a.run.app"
wip_provider_b                             = "operator-b@dev-env.iam.gserviceaccount.com"
service_account_b                          = "projects/1234567890/locations/global/workloadIdentityPools/opwip-b/providers/"