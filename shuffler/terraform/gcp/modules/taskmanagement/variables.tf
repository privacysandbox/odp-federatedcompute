/**
 * Copyright 2024 Google LLC
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

