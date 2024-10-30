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

output "kubernetes_endpoint" {
  description = "The cluster endpoint"
  sensitive   = true
  value       = module.gke_cluster.kubernetes_endpoint
}

output "cluster_name" {
  description = "Cluster name"
  value       = module.gke_cluster.cluster_name
}

output "location" {
  value = module.gke_cluster.location
}

output "master_kubernetes_version" {
  description = "Kubernetes version of the master"
  value       = module.gke_cluster.master_kubernetes_version
}

output "ca_certificate" {
  description = "The cluster ca certificate (base64 encoded)"
  value       = module.gke_cluster.ca_certificate
  sensitive   = true
}

output "gke_service_account" {
  description = "The service account to default running nodes as if not overridden in `node_pools`."
  value       = module.gke_sa.service_account_email
}

output "network_name" {
  description = "The name of the VPC being created"
  value       = module.network.network_name
}

output "subnet_names" {
  description = "The names of the subnet being created"
  value       = module.network.subnets_names
}

output "region" {
  description = "The region in which the cluster resides"
  value       = module.gke_cluster.region
}

output "zones" {
  description = "List of zones in which the cluster resides"
  value       = module.gke_cluster.zones
}

output "project_id" {
  description = "The project ID the cluster is in"
  value       = var.project_id
}

output "static_ip" {
  description = "Allocated static ip address"
  value       = module.network.static_ip
}

output "static_ip_name" {
  description = "Allocated static ip address name"
  value       = module.network.static_ip_name
}

output "aggregator_service_account_email" {
  value       = module.aggregator_sa.service_account_email
  description = "Email of the aggregator service account"
}

output "model_updater_service_account_email" {
  value       = module.model_updater_sa.service_account_email
  description = "Email of the model updater service account"
}

output "task_management_name" {
  description = "Name of task management cloud run deployment"
  value       = module.task_management.task_management_name
}

output "task_management_service_account_email" {
  value       = module.task_management_sa.service_account_email
  description = "Email of the task management service account"
}

output "task_management_url" {
  description = "URL of the task management service. The terraform may need to be re-run if blank to populate."
  value       = module.task_management.task_management_url
}

output "task_builder_name" {
  description = "Name of task builder cloud run deployment"
  value       = module.task_builder.task_builder_name
}

output "task_builder_service_account_email" {
  value       = module.task_builder_sa.service_account_email
  description = "Email of the task builder service account"
}

output "task_builder_url" {
  description = "URL of the task builder service. The terraform may need to be re-run if blank to populate."
  value       = module.task_builder.task_builder_url
}

output "task_builder_managed_service_name" {
  description = "Task builder API managed service name"
  value       = module.task_builder.task_builder_managed_service_name
}