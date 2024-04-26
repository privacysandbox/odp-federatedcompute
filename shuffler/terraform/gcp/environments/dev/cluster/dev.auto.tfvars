/**
 * Copyright 2022 Google LLC
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

# Example values required by cluster.tf
#
# These values should be modified for each of your environments.

environment = "demo-env"
project_id  = "demo-project"
region      = "us-central1"

# Workload Images
task_assignment_image = "<location>/<project>/<repository>/<image>:<tag or digest>"
task_scheduler_image  = "<location>/<project>/<repository>/<image>:<tag or digest>"
collector_image       = "<location>/<project>/<repository>/<image>:<tag or digest>"

# Output from shuffler
parent_domain_name         = "demo-domain-xyz.com"
static_ip_name             = "<name of static-ip allocated for shuffler>"
gke_cluster_ca_certificate = "<base64 encoded ca cert for gke cluster>"
gke_host                   = "<host endpoint for allocated cluster>"