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

variable "project_id" {
  description = "GCP Project ID in which this module will be created."
  type        = string
}

variable "environment" {
  description = "Description for the environment, e.g. dev, staging, production"
  type        = string
}

variable "region" {
  description = "Region where all services will be created."
  type        = string
}

variable "task_assignment_image" {
  description = "The taskassignment container image."
  type        = string
}

variable "task_assignment_port" {
  description = "The taskassignment service port."
  type        = string
}

variable "task_scheduler_image" {
  description = "The taskscheduler container image."
  type        = string
}

variable "collector_image" {
  description = "The collector container image."
  type        = string
}

variable "static_ip_name" {
  description = "The static ip name"
  type        = string
}

variable "parent_domain_name" {
  description = "Custom domain name to register and use for external APIs."
  type        = string
}

variable "task_assignment_max_replicas" {
  description = "The maximum number of task assignment replicas that the autoscaler can scale up to."
  type        = number
}

variable "task_assignment_min_replicas" {
  description = "The minimum number of task assignment replicas that the autoscaler can scale down to."
  type        = number
}

variable "collector_max_replicas" {
  description = "The maximum number of collector replicas that the autoscaler can scale up to."
  type        = number
}

variable "collector_min_replicas" {
  description = "The minimum number of collector replicas that the autoscaler can scale down to."
  type        = number
}

variable "task_scheduler_max_replicas" {
  description = "The maximum number of task scheduler replicas that the autoscaler can scale up to."
  type        = number
}

variable "task_scheduler_min_replicas" {
  description = "The minimum number of task scheduler replicas that the autoscaler can scale down to."
  type        = number
}
