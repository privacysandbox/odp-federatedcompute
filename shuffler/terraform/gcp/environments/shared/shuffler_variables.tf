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

variable "parent_domain_name" {
  description = "Custom domain name to register and use for external APIs."
  type        = string
}

variable "aggregator_image" {
  description = "The aggregator container image."
  type        = string
}

variable "aggregator_service_account" {
  description = "The service account to use for the aggregator"
  type        = string
  default     = ""
}

variable "model_updater_image" {
  description = "The model updater container image."
  type        = string
}

variable "model_updater_service_account" {
  description = "The service account to use for the model updater"
  type        = string
  default     = ""
}

variable "cluster_service_account" {
  description = "The service account to use for the cluster"
  type        = string
  default     = ""
}

variable "cluster_deletion_protection" {
  description = "Whether or not to allow Terraform to destroy the cluster."
  type        = bool
  default     = true
}

variable "model_bucket_force_destroy" {
  description = "Whether to force destroy the bucket even if it is not empty."
  type        = bool
  default     = false
}

variable "model_bucket_versioning" {
  description = "Enable bucket versioning."
  type        = bool
  default     = true
}

variable "client_gradient_bucket_force_destroy" {
  description = "Whether to force destroy the bucket even if it is not empty."
  type        = bool
  default     = false
}

variable "client_gradient_bucket_versioning" {
  description = "Enable bucket versioning."
  type        = bool
  default     = true
}

variable "aggregated_gradient_bucket_force_destroy" {
  description = "Whether to force destroy the bucket even if it is not empty."
  type        = bool
  default     = false
}

variable "aggregated_gradient_bucket_versioning" {
  description = "Enable bucket versioning."
  type        = bool
  default     = true
}

variable "model_bucket_lifecycle_age_days" {
  description = "Duration in days for objects in the model bucket before they are deleted."
  type        = number
  default     = 360
}

variable "client_gradient_bucket_lifecycle_age_days" {
  description = "Duration in days for objects in the client gradient bucket before they are deleted."
  type        = number
  default     = 60
}

variable "aggregated_gradient_bucket_lifecycle_age_days" {
  description = "Duration in days for objects in the aggregated gradient bucket before they are deleted."
  type        = number
  default     = 60
}

variable "model_bucket_location" {
  description = "GCS bucket location (https://cloud.google.com/storage/docs/locations) for the model bucket."
  type        = string
}

variable "client_gradient_bucket_location" {
  description = "GCS bucket location (https://cloud.google.com/storage/docs/locations) for the client gradient bucket."
  type        = string
}

# https://cloud.google.com/spanner/docs/pitr
# Must be between 1 hour and 7 days. Can be specified in days, hours, minutes, or seconds.
# eg: 1d, 24h, 1440m, and 86400s are equivalent.
variable "spanner_database_retention_period" {
  description = "Duration to maintain table versioning for point-in-time recovery."
  type        = string
  nullable    = false
  default     = "1h"
}

variable "spanner_instance_config" {
  type        = string
  description = "Multi region config value for the Spanner Instance. Example: 'nam10' for North America."
}

variable "spanner_processing_units" {
  description = "Spanner's compute capacity. 1000 processing units = 1 node and must be set as a multiple of 100."
  type        = number
  default     = 1000
}

variable "metric_spanner_processing_units" {
  description = "Spanner's compute capacity for Metric instance. 1000 processing units = 1 node and must be set as a multiple of 100."
  type        = number
  default     = 100
}

variable "spanner_database_deletion_protection" {
  description = "Prevents destruction of the Spanner database."
  type        = bool
  default     = true
}

# Service input variables
variable "encryption_key_service_a_base_url" {
  description = "The base url of the encryption key service A."
  type        = string
}

variable "encryption_key_service_b_base_url" {
  description = "The base url of the encryption key service B."
  type        = string
}

variable "encryption_key_service_a_cloudfunction_url" {
  description = "The cloudfunction url of the encryption key service A."
  type        = string
}

variable "encryption_key_service_b_cloudfunction_url" {
  description = "The cloudfunction url of the encryption key service B."
  type        = string
}

variable "wip_provider_a" {
  description = "The workload identity provider of the encryption key service A."
  type        = string
}

variable "wip_provider_b" {
  description = "The workload identity provider of the encryption key service B."
  type        = string
}

variable "service_account_a" {
  description = "The service account to impersonate of the encryption key service A."
  type        = string
}

variable "service_account_b" {
  description = "The service account to impersonate of the encryption key service B."
  type        = string
}

variable "allowed_operator_service_accounts" {
  description = "The service accounts provided by coordinator for the worker to impersonate."
  type        = string
}

variable "key_attestation_validation_url" {
  description = "The base url of key attestation validation service"
  type        = string
  default     = ""
}

variable "key_attestation_api_key" {
  description = "The api key of key attestation"
  type        = string
  default     = ""
}

variable "is_authentication_enabled" {
  description = "Whether to enable authentication"
  type        = bool
  default     = false
}

variable "allow_rooted_devices" {
  description = "Whether to allow rooted devices. This setting will have no effect when authentication is disabled. It is recommended to be set false for production environments."
  type        = bool
  default     = false
}

variable "download_plan_token_duration" {
  description = "Duration in seconds the download plan signed URL token is valid for"
  type        = number
  default     = 900
}

variable "download_checkpoint_token_duration" {
  description = "Duration in seconds the download checkpoint signed URL token is valid for"
  type        = number
  default     = 900
}

variable "upload_gradient_token_duration" {
  description = "Duration in seconds the upload gradient signed URL token is valid for"
  type        = number
  default     = 900
}

variable "local_compute_timeout_minutes" {
  description = "The duration an assignment will remain in ASSIGNED status before timing out in minutes."
  type        = number
  default     = 15
}

variable "upload_timeout_minutes" {
  description = "The duration an assignment will remain in LOCAL_COMPLETED status before timing out in minutes."
  type        = number
  default     = 15
}

# Aggregator parameters
variable "aggregator_instance_source_image" {
  description = "The aggregator OS source container image to run."
  type        = string
  default     = "projects/confidential-space-images/global/images/confidential-space-240900"
}

variable "aggregator_machine_type" {
  description = "The aggregator machine type of the VM."
  type        = string
  default     = "n2d-standard-8"
}

variable "aggregator_autoscaling_jobs_per_instance" {
  description = "The ratio of jobs to aggregator worker instances to scale by."
  type        = number
  default     = 2
}

variable "aggregator_max_replicas" {
  description = "The maximum number of aggregator instances that the autoscaler can scale up to. "
  type        = number
  default     = 5
}

variable "aggregator_min_replicas" {
  description = "The minimum number of aggregator replicas that the autoscaler can scale down to."
  type        = number
  default     = 2
}

variable "aggregator_cooldown_period" {
  description = "The number of seconds that the autoscaler should wait before it starts collecting information from a aggregator new instance."
  type        = number
  default     = 180
}

variable "aggregator_subscriber_max_outstanding_element_count" {
  description = "The maximum number of messages for the aggregator which have not received acknowledgments or negative acknowledgments before pausing the stream."
  type        = number
  default     = 2
}

# ModelUpdater parameters
variable "model_updater_instance_source_image" {
  description = "The model_updater OS source container image to run."
  type        = string
  default     = "projects/confidential-space-images/global/images/confidential-space-240900"
}

variable "model_updater_machine_type" {
  description = "The model_updater machine type of the VM."
  type        = string
  default     = "n2d-standard-8"
}

variable "model_updater_autoscaling_jobs_per_instance" {
  description = "The ratio of jobs to model_updater worker instances to scale by."
  type        = number
  default     = 2
}

variable "model_updater_max_replicas" {
  description = "The maximum number of model_updater instances that the autoscaler can scale up to. "
  type        = number
  default     = 5
}

variable "model_updater_min_replicas" {
  description = "The minimum number of model_updater replicas that the autoscaler can scale down to."
  type        = number
  default     = 2
}

variable "model_updater_cooldown_period" {
  description = "The number of seconds that the autoscaler should wait before it starts collecting information from a model_updater new instance."
  type        = number
  default     = 120
}

variable "model_updater_subscriber_max_outstanding_element_count" {
  description = "The maximum number of messages for the model updater which have not received acknowledgments or negative acknowledgments before pausing the stream."
  type        = number
  default     = 2
}

# Task management parameters
variable "task_management_service_account" {
  description = "The service account to use for task management"
  type        = string
  default     = ""
}

variable "task_management_image" {
  description = "The task management container image."
  type        = string
}

variable "task_management_port" {
  description = "The task management service port."
  type        = string
  default     = "8082"
}

variable "task_management_max_replicas" {
  description = "The maximum number of task management replicas that the autoscaler can scale up to."
  type        = number
  default     = 5
}

variable "task_management_min_replicas" {
  description = "The minimum number of task management replicas that the autoscaler can scale down to."
  type        = number
  default     = 2
}

# Task builder parameters
variable "task_builder_service_account" {
  description = "The service account to use for task builder"
  type        = string
  default     = ""
}

variable "task_builder_image" {
  description = "The task builder container image."
  type        = string
}

variable "task_builder_port" {
  description = "The task builder service port."
  type        = string
  default     = "5000"
}

variable "task_builder_max_replicas" {
  description = "The maximum number of task builder replicas that the autoscaler can scale up to."
  type        = number
  default     = 5
}

variable "task_builder_min_replicas" {
  description = "The minimum number of task builder replicas that the autoscaler can scale down to."
  type        = number
  default     = 2
}

# Collector parameters
variable "collector_batch_size" {
  description = "The size of aggregation batches created by the collector"
  type        = number
  default     = 50
}

variable "initial_deployment" {
  description = "Set true for first deployment to handle dependencies which rely on features that require dependencies determined after apply"
  type        = bool
  default     = false
}

# Monitoring alerts parameters
variable "alarms_notification_email" {
  description = "The email to send alarm notifications to"
  type        = string
  default     = ""
}

variable "aggregator_pub_sub_ack_latency_threshold_ms" {
  description = "Aggregator pub/sub ack latency threshold"
  type        = number
  default     = 10000
}

variable "model_updater_pub_sub_ack_latency_threshold_ms" {
  description = "Model Updater pub/sub ack latency threshold"
  type        = number
  default     = 25000
}

variable "enable_notification_alerts" {
  description = "Whether or not to enable notification alerts."
  type        = bool
  default     = false
}

variable "task_assignment_report_result_failures" {
  description = "Num of Report Result failures to trigger alert"
  type        = number
  default     = 100
}

variable "task_assignment_no_task_available_failures" {
  description = "Num of No_Task_Available failures to trigger alert"
  type        = number
  default     = 100
}

variable "aggregation_batch_failure_threshold" {
  description = "The number of aggregation batches failed for an iteration before moving the iteration to a failure state."
  type        = number
  default     = null
}

variable "enable_exactly_once_delivery" {
  description = "Enable exactly once delivery on pubsub subscriptions. Consider disabling for improved performance at the cost of potentially redelivered messages. https://cloud.google.com/pubsub/docs/exactly-once-delivery"
  type        = bool
  default     = false
}