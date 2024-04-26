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
  source                                                 = "../../applications/shuffler"
  cluster_service_account                                = var.cluster_service_account
  cluster_deletion_protection                            = var.cluster_deletion_protection
  environment                                            = var.environment
  project_id                                             = var.project_id
  region                                                 = var.region
  parent_domain_name                                     = var.parent_domain_name
  aggregator_image                                       = var.aggregator_image
  aggregator_service_account                             = var.aggregator_service_account
  model_updater_image                                    = var.model_updater_image
  model_updater_service_account                          = var.model_updater_service_account
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
  encryption_key_service_a_base_url                      = var.encryption_key_service_a_base_url
  encryption_key_service_b_base_url                      = var.encryption_key_service_b_base_url
  encryption_key_service_a_cloudfunction_url             = var.encryption_key_service_a_cloudfunction_url
  encryption_key_service_b_cloudfunction_url             = var.encryption_key_service_b_cloudfunction_url
  wip_provider_a                                         = var.wip_provider_a
  wip_provider_b                                         = var.wip_provider_b
  service_account_a                                      = var.service_account_a
  service_account_b                                      = var.service_account_b
  public_key_service_base_url                            = var.public_key_service_base_url
  allowed_operator_service_accounts                      = var.allowed_operator_service_accounts
  key_attestation_api_key                                = var.key_attestation_api_key
  key_attestation_validation_url                         = var.key_attestation_validation_url
  is_authentication_enabled                              = var.is_authentication_enabled
  download_plan_token_duration                           = var.download_plan_token_duration
  download_checkpoint_token_duration                     = var.download_checkpoint_token_duration
  upload_gradient_token_duration                         = var.upload_gradient_token_duration
  local_compute_timeout_minutes                          = var.local_compute_timeout_minutes
  upload_timeout_minutes                                 = var.upload_timeout_minutes
  aggregator_autoscaling_jobs_per_instance               = var.aggregator_autoscaling_jobs_per_instance
  aggregator_cooldown_period                             = var.aggregator_cooldown_period
  aggregator_instance_source_image                       = var.aggregator_instance_source_image
  aggregator_machine_type                                = var.aggregator_machine_type
  aggregator_max_replicas                                = var.aggregator_max_replicas
  aggregator_min_replicas                                = var.aggregator_min_replicas
  aggregator_subscriber_max_outstanding_element_count    = var.aggregator_subscriber_max_outstanding_element_count
  collector_batch_size                                   = var.collector_batch_size
  model_updater_autoscaling_jobs_per_instance            = var.model_updater_autoscaling_jobs_per_instance
  model_updater_cooldown_period                          = var.model_updater_cooldown_period
  model_updater_instance_source_image                    = var.model_updater_instance_source_image
  model_updater_machine_type                             = var.model_updater_machine_type
  model_updater_max_replicas                             = var.model_updater_max_replicas
  model_updater_min_replicas                             = var.model_updater_min_replicas
  model_updater_subscriber_max_outstanding_element_count = var.model_updater_subscriber_max_outstanding_element_count
  task_management_service_account                        = var.task_management_service_account
  task_management_image                                  = var.task_management_image
  task_management_max_replicas                           = var.task_management_max_replicas
  task_management_min_replicas                           = var.task_management_min_replicas
  task_management_port                                   = var.task_management_port
}