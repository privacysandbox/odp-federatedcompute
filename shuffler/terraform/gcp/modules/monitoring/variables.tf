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

variable "environment" {
  description = "Description for the environment, e.g. dev, staging, production"
  type        = string
}

variable "alarms_notification_email" {
  description = "The email to send alarm notifications to"
  type        = string
}

variable "aggregator_pub_sub_ack_latency_threshold_ms" {
  description = "Aggregator pub/sub ack latency threshold"
  type        = number
}

variable "model_updater_pub_sub_ack_latency_threshold_ms" {
  description = "Model Updater pub/sub ack latency threshold"
  type        = number
}

variable "aggregator_subscription_name" {
  description = "Name of Aggregator pub/sub subscription"
  type        = string
}

variable "model_updater_subscription_name" {
  description = "Name of Model Updater pub/sub subscription"
  type        = string
}

variable "spanner_instance_name" {
  description = "Name of the FCP task Spanner instance."
  type        = string
}

variable "spanner_task_database_name" {
  description = "Name of the FCP task Spanner database."
  type        = string
}
variable "spanner_lock_database_name" {
  description = "Name of the FCP lock Spanner database."
  type        = string
}


variable "metrics_spanner_instance_name" {
  description = "Name of the FCP metrics Spanner instance."
  type        = string
}

variable "task_assignment_report_result_failures" {
  description = "Num of Report Result failures to trigger alert"
  type        = number
  default     = 10
}

variable "task_assignment_no_task_available_failures" {
  description = "Num of No_Task_Available failures to trigger alert"
  type        = number
  default     = 10
}

variable "cluster_name" {
  description = "Name of the cluster"
  type        = string
}