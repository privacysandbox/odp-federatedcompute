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
  value       = kubernetes_deployment_v1.taskscheduler.metadata[0].name
}

output "taskassignment_name" {
  description = "Name of taskassignment deployment"
  value       = kubernetes_deployment_v1.taskassignment.metadata[0].name
}

output "collector_name" {
  description = "Name of collector deployment"
  value       = kubernetes_deployment_v1.collector.metadata[0].name
}

output "ingress_name" {
  description = "Name of cluster ingress"
  value       = kubernetes_ingress_v1.ingress.metadata[0].name
}

output "gcp_service_account_email" {
  description = "The email address of GCP service account for workloads"
  value       = module.gke-workload-identity.gcp_service_account_email
}