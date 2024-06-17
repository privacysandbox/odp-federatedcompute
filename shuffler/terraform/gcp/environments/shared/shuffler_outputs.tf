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

output "gke_host" {
  description = "The cluster endpoint"
  sensitive   = true
  value       = module.shuffler.kubernetes_endpoint
}

output "cluster_name" {
  description = "Cluster name"
  value       = module.shuffler.cluster_name
}

output "location" {
  value = module.shuffler.location
}

output "master_kubernetes_version" {
  description = "Kubernetes version of the master"
  value       = module.shuffler.master_kubernetes_version
}

output "gke_cluster_ca_certificate" {
  description = "The cluster ca certificate (base64 encoded)"
  value       = module.shuffler.ca_certificate
  sensitive   = true
}

output "gke_service_account" {
  description = "The service account to default running nodes"
  value       = module.shuffler.gke_service_account
}

output "network_name" {
  description = "The name of the VPC being created"
  value       = module.shuffler.network_name
}

output "subnet_names" {
  description = "The names of the subnet being created"
  value       = module.shuffler.subnet_names
}

output "region" {
  description = "The region in which the cluster resides"
  value       = module.shuffler.region
}

output "cluster_zones" {
  description = "List of zones in which the cluster resides"
  value       = module.shuffler.zones
}

output "project_id" {
  description = "The project ID the cluster is in"
  value       = var.project_id
}

output "static_ip" {
  description = "Allocated static ip address"
  value       = module.shuffler.static_ip
}

output "static_ip_name" {
  description = "Allocated static ip address name"
  value       = module.shuffler.static_ip_name
}

output "parent_domain_name" {
  description = "Custom domain name to register and use for external APIs."
  value       = var.parent_domain_name
}

output "model_bucket_name" {
  value = module.shuffler.model_bucket_name
}

output "client_gradient_bucket_name" {
  value = module.shuffler.client_gradient_bucket_name
}

output "aggregated_gradient_bucket_name" {
  value = module.shuffler.aggregated_gradient_bucket_name
}

output "spanner_database_name" {
  value       = module.shuffler.spanner_database_name
  description = "Name of the FCP task Spanner database."
}

output "spanner_lock_database_name" {
  value       = module.shuffler.spanner_lock_database_name
  description = "Name of the FCP lock Spanner database."
}

output "spanner_instance_name" {
  value       = module.shuffler.spanner_instance_name
  description = "Name of the FCP task Spanner instance."
}

output "metrics_spanner_instance_name" {
  value       = module.shuffler.metrics_spanner_instance_name
  description = "Name of the FCP metrics Spanner instance."
}

output "metrics_spanner_database_name" {
  value       = module.shuffler.spanner_metrics_database_name
  description = "Name of the FCP metrics Spanner database."
}

output "aggregator_service_account_email" {
  description = "The service account of the aggregator"
  value       = module.shuffler.aggregator_service_account_email
}

output "aggregator_topic_name" {
  value       = module.shuffler.aggregator_topic_name
  description = "Name of the aggregator pubsub topic."
}

output "aggregator_subscription_name" {
  value       = module.shuffler.aggregator_subscription_name
  description = "Name of the aggregator pubsub subscription."
}

output "model_updater_service_account_email" {
  description = "The service account of the model updater"
  value       = module.shuffler.model_updater_service_account_email
}

output "model_updater_topic_name" {
  value       = module.shuffler.model_updater_topic_name
  description = "Name of the model_updater pubsub topic."
}

output "model_updater_subscription_name" {
  value       = module.shuffler.model_updater_subscription_name
  description = "Name of the model_updater pubsub subscription."
}

output "task_management_name" {
  description = "Name of task management cloud run deployment"
  value       = module.shuffler.task_management_name
}

output "task_management_service_account_email" {
  description = "The service account of the task management"
  value       = module.shuffler.task_management_service_account_email
}

output "task_management_url" {
  # https://github.com/hashicorp/terraform-provider-google/issues/15119
  description = "URL of the task management service. The terraform may need to be re-run if blank to populate."
  value       = module.shuffler.task_management_url
}

output "task_builder_name" {
  description = "Name of task builder cloud run deployment"
  value       = module.shuffler.task_builder_name
}

output "task_builder_service_account_email" {
  value       = module.shuffler.task_builder_service_account_email
  description = "Email of the task builder service account"
}

output "task_builder_url" {
  # https://github.com/hashicorp/terraform-provider-google/issues/15119
  description = "URL of the task builder service. The terraform may need to be re-run if blank to populate."
  value       = module.shuffler.task_builder_url
}