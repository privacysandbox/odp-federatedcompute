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

variable "model_bucket_force_destroy" {
  description = "Whether to force destroy the bucket even if it is not empty."
  type        = bool
  nullable    = false
}

variable "model_bucket_versioning" {
  description = "Enable bucket versioning."
  type        = bool
  nullable    = false
}

variable "client_gradient_bucket_force_destroy" {
  description = "Whether to force destroy the bucket even if it is not empty."
  type        = bool
  nullable    = false
}

variable "client_gradient_bucket_versioning" {
  description = "Enable bucket versioning."
  type        = bool
  nullable    = false
}

variable "aggregated_gradient_bucket_force_destroy" {
  description = "Whether to force destroy the bucket even if it is not empty."
  type        = bool
  nullable    = false
}

variable "aggregated_gradient_bucket_versioning" {
  description = "Enable bucket versioning."
  type        = bool
  nullable    = false
}

variable "model_bucket_lifecycle_age_days" {
  description = "Duration in days for objects in the model bucket before they are deleted."
  type        = number
}

variable "client_gradient_bucket_lifecycle_age_days" {
  description = "Duration in days for objects in the client gradient bucket before they are deleted."
  type        = number
}

variable "aggregated_gradient_bucket_lifecycle_age_days" {
  description = "Duration in days for objects in the aggregated gradient bucket before they are deleted."
  type        = number
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
}

variable "spanner_instance_config" {
  type        = string
  description = "Multi region config value for the Spanner Instance. Example: 'nam10' for North America."
}

variable "spanner_processing_units" {
  description = "Spanner's compute capacity. 1000 processing units = 1 node and must be set as a multiple of 100."
  type        = number
}

variable "metric_spanner_processing_units" {
  description = "Spanner's compute capacity for Metric instance. 1000 processing units = 1 node and must be set as a multiple of 100."
  type        = number
}

variable "spanner_database_deletion_protection" {
  description = "Prevents destruction of the Spanner database."
  type        = bool
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

variable "key_attestation_validation_url" {
  description = "The base url of key attestation validation service"
  type        = string
}

variable "key_attestation_api_key" {
  description = "The api key of key attestation"
  type        = string
}

variable "is_authentication_enabled" {
  description = "Whether to enable authentication"
  type        = bool
}

variable "allow_rooted_devices" {
  description = "Whether to allow rooted devices. This is recommended to be set false for production environments."
  type        = bool
}

variable "download_plan_token_duration" {
  description = "Duration in seconds the download plan signed URL token is valid for"
  type        = number
}

variable "download_checkpoint_token_duration" {
  description = "Duration in seconds the download checkpoint signed URL token is valid for"
  type        = number
}

variable "upload_gradient_token_duration" {
  description = "Duration in seconds the upload gradient signed URL token is valid for"
  type        = number
}

variable "local_compute_timeout_minutes" {
  description = "The duration an assignment will remain in ASSIGNED status before timing out in minutes."
  type        = number
}

variable "upload_timeout_minutes" {
  description = "The duration an assignment will remain in LOCAL_COMPLETED status before timing out in minutes."
  type        = number
}

# Aggregator parameters
variable "aggregator_subscriber_max_outstanding_element_count" {
  description = "The maximum number of messages for the aggregator which have not received acknowledgments or negative acknowledgments before pausing the stream."
  type        = number
  default     = 2
}

# ModelUpdater parameters
variable "model_updater_subscriber_max_outstanding_element_count" {
  description = "The maximum number of messages for the model updater which have not received acknowledgments or negative acknowledgments before pausing the stream."
  type        = number
}

# Collector parameters
variable "collector_batch_size" {
  description = "The size of aggregation batches created by the collector"
  type        = number
}

# Domain name
variable "parent_domain_name" {
  description = "Custom domain name to register and use for external APIs."
  type        = string
}

variable "aggregation_batch_failure_threshold" {
  description = "The number of aggregation batches failed for an iteration before moving the iteration to a failure state."
  type        = number
}

variable "enable_exactly_once_delivery" {
  description = "Enable exactly once delivery on pubsub subscriptions. Consider disabling for improved performance at the cost of potentially redelivered messages. https://cloud.google.com/pubsub/docs/exactly-once-delivery"
  type        = bool
}
