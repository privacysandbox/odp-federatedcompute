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

################################################################################
# Global Variables.
################################################################################

variable "aggregator_image" {
  description = "The aggregator container image."
  type        = string
}

variable "aggregator_service_account" {
  description = "The service account to use for the aggregator"
  type        = string
}

variable "model_updater_image" {
  description = "The model updater container image."
  type        = string
}

variable "model_updater_service_account" {
  description = "The service account to use for the model updater"
  type        = string
}

variable "cluster_service_account" {
  description = "The service account to use for the cluster"
  type        = string
}

variable "cluster_deletion_protection" {
  description = "Whether or not to allow Terraform to destroy the cluster."
  type        = bool
}

variable "parent_domain_name" {
  description = "Custom domain name to register and use for external APIs."
  type        = string
}

# Service input variables
variable "allowed_operator_service_accounts" {
  description = "The service accounts provided by coordinator for the worker to impersonate."
  type        = string
}

# Aggregator parameters
variable "aggregator_instance_source_image" {
  description = "The aggregator OS source container image to run."
  type        = string
}

variable "aggregator_machine_type" {
  description = "The aggregator machine type of the VM."
  type        = string
}

variable "aggregator_autoscaling_jobs_per_instance" {
  description = "The ratio of jobs to aggregator worker instances to scale by."
  type        = number
}

variable "aggregator_max_replicas" {
  description = "The maximum number of aggregator instances that the autoscaler can scale up to. "
  type        = number
}

variable "aggregator_min_replicas" {
  description = "The minimum number of aggregator replicas that the autoscaler can scale down to."
  type        = number
}

variable "aggregator_cooldown_period" {
  description = "The number of seconds that the autoscaler should wait before it starts collecting information from a aggregator new instance."
  type        = number
}

# ModelUpdater parameters
variable "model_updater_instance_source_image" {
  description = "The model_updater OS source container image to run."
  type        = string
}

variable "model_updater_machine_type" {
  description = "The model_updater machine type of the VM."
  type        = string
}

variable "model_updater_autoscaling_jobs_per_instance" {
  description = "The ratio of jobs to model_updater worker instances to scale by."
  type        = number
}

variable "model_updater_max_replicas" {
  description = "The maximum number of model_updater instances that the autoscaler can scale up to. "
  type        = number
}

variable "model_updater_min_replicas" {
  description = "The minimum number of model_updater replicas that the autoscaler can scale down to."
  type        = number
}

variable "model_updater_cooldown_period" {
  description = "The number of seconds that the autoscaler should wait before it starts collecting information from a model_updater new instance."
  type        = number
}

# Task management parameters
variable "task_management_service_account" {
  description = "The service account to use for the task management cloud run"
  type        = string
}

variable "task_management_image" {
  description = "The task management container image."
  type        = string
}

variable "task_management_port" {
  description = "The task management service port."
  type        = string
}

variable "task_management_max_replicas" {
  description = "The maximum number of task management replicas that the autoscaler can scale up to."
  type        = number
}

variable "task_management_min_replicas" {
  description = "The minimum number of task management replicas that the autoscaler can scale down to."
  type        = number
}
