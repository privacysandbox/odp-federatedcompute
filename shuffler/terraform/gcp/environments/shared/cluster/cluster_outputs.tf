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

output "taskscheduler_name" {
  description = "Name of taskscheduler deployment"
  value       = module.cluster.taskscheduler_name
}

output "taskassignment_name" {
  description = "Name of taskassignment deployment"
  value       = module.cluster.taskassignment_name
}

output "collector_name" {
  description = "Name of collector deployment"
  value       = module.cluster.collector_name
}

output "ingress_name" {
  description = "Name of cluster ingress"
  value       = module.cluster.ingress_name
}

output "gcp_service_account_email" {
  description = "The email address of GCP service account for workloads"
  value       = module.cluster.gcp_service_account_email
}