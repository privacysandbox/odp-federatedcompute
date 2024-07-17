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

provider "google" {
  project = var.project_id
  region  = var.region
}

module "shuffler" {
  source                                                 = "../../applications/resources"
  environment                                            = var.environment
  project_id                                             = var.project_id
  region                                                 = var.region
  aggregated_gradient_bucket_force_destroy               = var.aggregated_gradient_bucket_force_destroy
  aggregated_gradient_bucket_versioning                  = var.aggregated_gradient_bucket_versioning
  client_gradient_bucket_force_destroy                   = var.client_gradient_bucket_force_destroy
  client_gradient_bucket_versioning                      = var.client_gradient_bucket_versioning
  model_bucket_force_destroy                             = var.model_bucket_force_destroy
  model_bucket_versioning                                = var.model_bucket_versioning
  spanner_database_deletion_protection                   = var.spanner_database_deletion_protection
  spanner_database_retention_period                      = var.spanner_database_retention_period
  spanner_instance_config                                = var.spanner_instance_config
  spanner_processing_units                               = var.spanner_processing_units
  metric_spanner_processing_units                        = var.metric_spanner_processing_units
  encryption_key_service_a_base_url                      = var.encryption_key_service_a_base_url
  encryption_key_service_b_base_url                      = var.encryption_key_service_b_base_url
  encryption_key_service_a_cloudfunction_url             = var.encryption_key_service_a_cloudfunction_url
  encryption_key_service_b_cloudfunction_url             = var.encryption_key_service_b_cloudfunction_url
  wip_provider_a                                         = var.wip_provider_a
  wip_provider_b                                         = var.wip_provider_b
  service_account_a                                      = var.service_account_a
  service_account_b                                      = var.service_account_b
  public_key_service_base_url                            = var.public_key_service_base_url
  key_attestation_api_key                                = var.key_attestation_api_key
  key_attestation_validation_url                         = var.key_attestation_validation_url
  is_authentication_enabled                              = var.is_authentication_enabled
  download_plan_token_duration                           = var.download_plan_token_duration
  download_checkpoint_token_duration                     = var.download_checkpoint_token_duration
  upload_gradient_token_duration                         = var.upload_gradient_token_duration
  local_compute_timeout_minutes                          = var.local_compute_timeout_minutes
  upload_timeout_minutes                                 = var.upload_timeout_minutes
  aggregator_subscriber_max_outstanding_element_count    = var.aggregator_subscriber_max_outstanding_element_count
  collector_batch_size                                   = var.collector_batch_size
  model_updater_subscriber_max_outstanding_element_count = var.model_updater_subscriber_max_outstanding_element_count
  aggregated_gradient_bucket_lifecycle_age_days          = var.aggregated_gradient_bucket_lifecycle_age_days
  allow_rooted_devices                                   = var.allow_rooted_devices
  client_gradient_bucket_lifecycle_age_days              = var.client_gradient_bucket_lifecycle_age_days
  model_bucket_lifecycle_age_days                        = var.model_bucket_lifecycle_age_days
}