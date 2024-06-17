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

module "storage" {
  source                                        = "../../modules/storage"
  aggregated_gradient_bucket_force_destroy      = var.aggregated_gradient_bucket_force_destroy
  aggregated_gradient_bucket_versioning         = var.aggregated_gradient_bucket_versioning
  client_gradient_bucket_force_destroy          = var.client_gradient_bucket_force_destroy
  client_gradient_bucket_versioning             = var.client_gradient_bucket_versioning
  environment                                   = var.environment
  model_bucket_force_destroy                    = var.model_bucket_force_destroy
  model_bucket_versioning                       = var.model_bucket_versioning
  aggregated_gradient_bucket_lifecycle_age_days = var.aggregated_gradient_bucket_lifecycle_age_days
  client_gradient_bucket_lifecycle_age_days     = var.client_gradient_bucket_lifecycle_age_days
  model_bucket_lifecycle_age_days               = var.model_bucket_lifecycle_age_days
  project_id                                    = var.project_id
  region                                        = var.region
  spanner_database_deletion_protection          = var.spanner_database_deletion_protection
  spanner_database_retention_period             = var.spanner_database_retention_period
  spanner_instance_config                       = var.spanner_instance_config
  spanner_processing_units                      = var.spanner_processing_units
  metric_spanner_processing_units               = var.metric_spanner_processing_units
}

module "pubsub" {
  source      = "../../modules/pubsub"
  environment = var.environment
  project_id  = var.project_id
  region      = var.region
}

######################
# Service parameters
######################
module "spanner_instance" {
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = "SPANNER_INSTANCE"
  parameter_value = module.storage.spanner_instance_name
}

module "task_database_name" {
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = "TASK_DATABASE_NAME"
  parameter_value = module.storage.spanner_database_name
}

module "lock_database_name" {
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = "LOCK_DATABASE_NAME"
  parameter_value = module.storage.spanner_lock_database_name
}

module "metrics_spanner_instance" {
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = "METRICS_SPANNER_INSTANCE"
  parameter_value = module.storage.metrics_spanner_instance_name
}

module "metrics_database_name" {
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = "METRICS_DATABASE_NAME"
  parameter_value = module.storage.spanner_metrics_database_name
}

module "client_gradient_bucket_template" {
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = "CLIENT_GRADIENT_BUCKET_TEMPLATE"
  parameter_value = "fcp-${var.environment}-g-%s"
}

module "aggregated_gradient_bucket_template" {
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = "AGGREGATED_GRADIENT_BUCKET_TEMPLATE"
  parameter_value = "fcp-${var.environment}-a-%s"
}

module "model_bucket_template" {
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = "MODEL_BUCKET_TEMPLATE"
  parameter_value = "fcp-${var.environment}-m-%s"
}

module "model_updater_pubsub_topic" {
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = "MODEL_UPDATER_PUBSUB_TOPIC"
  parameter_value = module.pubsub.model_updater_topic_name
}

module "aggregator_pubsub_subscription" {
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = "AGGREGATOR_PUBSUB_SUBSCRIPTION"
  parameter_value = module.pubsub.aggregator_subscription_name
}

module "aggregator_pubsub_topic" {
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = "AGGREGATOR_PUBSUB_TOPIC"
  parameter_value = module.pubsub.aggregator_topic_name
}

module "model_updater_pubsub_subscription" {
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = "MODEL_UPDATER_PUBSUB_SUBSCRIPTION"
  parameter_value = module.pubsub.model_updater_subscription_name
}

module "encryption_key_service_a_base_url" {
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = "ENCRYPTION_KEY_SERVICE_A_BASE_URL"
  parameter_value = var.encryption_key_service_a_base_url
}

module "encryption_key_service_b_base_url" {
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = "ENCRYPTION_KEY_SERVICE_B_BASE_URL"
  parameter_value = var.encryption_key_service_b_base_url
}

module "encryption_key_service_a_cloudfunction_url" {
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = "ENCRYPTION_KEY_SERVICE_A_CLOUDFUNCTION_URL"
  parameter_value = var.encryption_key_service_a_cloudfunction_url
}

module "encryption_key_service_b_cloudfunction_url" {
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = "ENCRYPTION_KEY_SERVICE_B_CLOUDFUNCTION_URL"
  parameter_value = var.encryption_key_service_b_cloudfunction_url
}

module "wip_provider_a" {
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = "WIP_PROVIDER_A"
  parameter_value = var.wip_provider_a
}

module "wip_provider_b" {
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = "WIP_PROVIDER_B"
  parameter_value = var.wip_provider_b
}

module "service_account_a" {
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = "SERVICE_ACCOUNT_A"
  parameter_value = var.service_account_a
}

module "service_account_b" {
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = "SERVICE_ACCOUNT_B"
  parameter_value = var.service_account_b
}

module "public_key_service_base_url" {
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = "PUBLIC_KEY_SERVICE_BASE_URL"
  parameter_value = var.public_key_service_base_url
}

module "key_attestation_validation_url" {
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = "KEY_ATTESTATION_VALIDATION_URL"
  parameter_value = var.key_attestation_validation_url
}

module "key_attestation_api_key" {
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = "KEY_ATTESTATION_API_KEY"
  parameter_value = var.key_attestation_api_key
}

module "is_authentication_enabled" {
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = "IS_AUTHENTICATION_ENABLED"
  parameter_value = var.is_authentication_enabled
}

module "allow_rooted_devices" {
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = "ALLOW_ROOTED_DEVICES"
  parameter_value = var.allow_rooted_devices
}

module "download_plan_token_duration" {
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = "DOWNLOAD_PLAN_TOKEN_DURATION"
  parameter_value = var.download_plan_token_duration
}

module "download_checkpoint_token_duration" {
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = "DOWNLOAD_CHECKPOINT_TOKEN_DURATION"
  parameter_value = var.download_checkpoint_token_duration
}

module "upload_gradient_token_duration" {
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = "UPLOAD_GRADIENT_TOKEN_DURATION"
  parameter_value = var.upload_gradient_token_duration
}

module "local_compute_timeout_minutes" {
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = "LOCAL_COMPUTE_TIMEOUT_MINUTES"
  parameter_value = var.local_compute_timeout_minutes
}

module "upload_timeout_minutes" {
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = "UPLOAD_TIMEOUT_MINUTES"
  parameter_value = var.upload_timeout_minutes
}

module "aggregator_subscriber_max_outstanding_element_count" {
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = "AGGREGATOR_SUBSCRIBER_MAX_OUTSTANDING_ELEMENT_COUNT"
  parameter_value = var.aggregator_subscriber_max_outstanding_element_count
}

module "model_updater_subscriber_max_outstanding_element_count" {
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = "MODEL_UPDATER_SUBSCRIBER_MAX_OUTSTANDING_ELEMENT_COUNT"
  parameter_value = var.model_updater_subscriber_max_outstanding_element_count
}

module "collector_batch_size" {
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = "COLLECTOR_BATCH_SIZE"
  parameter_value = var.collector_batch_size
}
