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

locals {
  network_name           = "${var.environment}-network"
  subnet_name            = "${var.environment}-subnet"
  master_auth_subnetwork = "${var.environment}-master-subnet"
  pods_range_name        = "ip-range-pods-${var.environment}"
  svc_range_name         = "ip-range-svc-${var.environment}"
  subnet_names           = [for subnet_self_link in module.network.subnets_self_links : split("/", subnet_self_link)[length(split("/", subnet_self_link)) - 1]]
}

module "network" {
  source                 = "../../modules/network"
  environment            = var.environment
  project_id             = var.project_id
  region                 = var.region
  master_auth_subnetwork = local.master_auth_subnetwork
  network_name           = local.network_name
  pods_range_name        = local.pods_range_name
  subnet_name            = local.subnet_name
  svc_range_name         = local.svc_range_name
  parent_domain_name     = var.parent_domain_name
}

# https://cloud.google.com/kubernetes-engine/docs/how-to/hardening-your-cluster#use_least_privilege_sa
module "gke_sa" {
  source                = "../../modules/serviceaccount"
  environment           = var.environment
  project_id            = var.project_id
  roles                 = ["roles/logging.logWriter", "roles/monitoring.viewer", "roles/monitoring.metricWriter", "roles/stackdriver.resourceMetadata.writer", "roles/autoscaling.metricsWriter", "roles/artifactregistry.reader"]
  service_account_email = var.cluster_service_account
  service_account_name  = "gke"
}

module "gke_cluster" {
  source                      = "../../modules/gke"
  environment                 = var.environment
  project_id                  = var.project_id
  region                      = var.region
  cluster_service_account     = module.gke_sa.service_account_email
  network_name                = local.network_name
  pods_range_name             = local.pods_range_name
  subnet_name                 = local.subnet_name
  subnets_names               = local.subnet_names
  svc_range_name              = local.svc_range_name
  cluster_deletion_protection = var.cluster_deletion_protection
}

module "aggregator_sa" {
  source                = "../../modules/serviceaccount"
  environment           = var.environment
  project_id            = var.project_id
  roles                 = ["roles/iam.serviceAccountTokenCreator", "roles/iam.serviceAccountUser", "roles/logging.logWriter", "roles/storage.objectUser", "roles/pubsub.subscriber", "roles/pubsub.publisher", "roles/secretmanager.secretAccessor", "roles/confidentialcomputing.workloadUser", "roles/monitoring.editor", "roles/artifactregistry.reader"]
  service_account_email = var.aggregator_service_account
  service_account_name  = "ag"
}

module "aggregator" {
  source                            = "../../modules/confidentialspace"
  allowed_operator_service_accounts = var.allowed_operator_service_accounts
  autoscaling_jobs_per_instance     = var.aggregator_autoscaling_jobs_per_instance
  compute_service_account           = module.aggregator_sa.service_account_email
  cooldown_period                   = var.aggregator_cooldown_period
  environment                       = var.environment
  instance_source_image             = var.aggregator_instance_source_image
  jobqueue_subscription_name        = module.pubsub.aggregator_subscription_name
  machine_type                      = var.aggregator_machine_type
  max_replicas                      = var.aggregator_max_replicas
  min_replicas                      = var.aggregator_min_replicas
  name                              = "ag"
  network_name                      = local.network_name
  project_id                        = var.project_id
  region                            = var.region
  subnet_name                       = local.subnet_name
  workload_image                    = var.aggregator_image
  depends_on = [
    module.aggregator_pubsub_subscription,
    module.encryption_key_service_a_base_url,
    module.encryption_key_service_b_base_url,
    module.encryption_key_service_a_cloudfunction_url,
    module.encryption_key_service_b_cloudfunction_url,
    module.wip_provider_a,
    module.wip_provider_b,
    module.service_account_a,
    module.service_account_b,
    module.aggregator_subscriber_max_outstanding_element_count,
    module.enable_aggregation_success_notifications
  ]
}

module "model_updater_sa" {
  source                = "../../modules/serviceaccount"
  environment           = var.environment
  project_id            = var.project_id
  roles                 = ["roles/iam.serviceAccountTokenCreator", "roles/iam.serviceAccountUser", "roles/logging.logWriter", "roles/storage.objectUser", "roles/pubsub.subscriber", "roles/secretmanager.secretAccessor", "roles/confidentialcomputing.workloadUser", "roles/monitoring.editor", "roles/artifactregistry.reader"]
  service_account_email = var.model_updater_service_account
  service_account_name  = "mu"
}

module "model_updater" {
  source                            = "../../modules/confidentialspace"
  allowed_operator_service_accounts = var.allowed_operator_service_accounts
  autoscaling_jobs_per_instance     = var.model_updater_autoscaling_jobs_per_instance
  compute_service_account           = module.model_updater_sa.service_account_email
  cooldown_period                   = var.model_updater_cooldown_period
  environment                       = var.environment
  instance_source_image             = var.model_updater_instance_source_image
  jobqueue_subscription_name        = module.pubsub.model_updater_subscription_name
  machine_type                      = var.model_updater_machine_type
  max_replicas                      = var.model_updater_max_replicas
  min_replicas                      = var.model_updater_min_replicas
  name                              = "mu"
  network_name                      = local.network_name
  project_id                        = var.project_id
  region                            = var.region
  subnet_name                       = local.subnet_name
  workload_image                    = var.model_updater_image
  depends_on = [
    module.model_updater_pubsub_subscription,
    module.encryption_key_service_a_base_url,
    module.encryption_key_service_b_base_url,
    module.encryption_key_service_a_cloudfunction_url,
    module.encryption_key_service_b_cloudfunction_url,
    module.wip_provider_a,
    module.wip_provider_b,
    module.service_account_a,
    module.service_account_b,
    module.model_updater_subscriber_max_outstanding_element_count
  ]
}

module "task_management_sa" {
  source                = "../../modules/serviceaccount"
  environment           = var.environment
  project_id            = var.project_id
  roles                 = ["roles/spanner.databaseUser", "roles/logging.logWriter", "roles/storage.objectUser", "roles/secretmanager.secretAccessor", "roles/monitoring.editor"]
  service_account_email = var.task_management_service_account
  service_account_name  = "tm"
}

module "task_management" {
  source                          = "../../modules/taskmanagement"
  environment                     = var.environment
  project_id                      = var.project_id
  region                          = var.region
  task_management_service_account = module.task_management_sa.service_account_email
  task_management_image           = var.task_management_image
  task_management_max_replicas    = var.task_management_max_replicas
  task_management_min_replicas    = var.task_management_min_replicas
  task_management_port            = var.task_management_port
  depends_on = [
    module.spanner_instance,
    module.task_database_name,
    module.lock_database_name,
    module.metrics_spanner_instance,
    module.metrics_database_name,
    module.client_gradient_bucket_template,
    module.model_bucket_template,
    module.model_cdn_endpoint,
    module.model_cdn_signing_key_name
  ]
}

module "task_builder_sa" {
  source                = "../../modules/serviceaccount"
  environment           = var.environment
  project_id            = var.project_id
  roles                 = ["roles/run.invoker", "roles/logging.logWriter", "roles/storage.objectUser", "roles/secretmanager.secretAccessor", "roles/monitoring.editor"]
  service_account_email = var.task_builder_service_account
  service_account_name  = "tb"
}

module "task_builder" {
  source                            = "../../modules/taskbuilder"
  environment                       = var.environment
  project_id                        = var.project_id
  region                            = var.region
  task_builder_service_account      = module.task_builder_sa.service_account_email
  task_builder_image                = var.task_builder_image
  task_builder_max_replicas         = var.task_builder_max_replicas
  task_builder_min_replicas         = var.task_builder_min_replicas
  task_builder_port                 = var.task_builder_port
  task_management_server_url_secret = module.task_management_server_url.secret_name
}


module "task_management_server_url" {
  source          = "../../modules/parameters"
  environment     = var.environment
  parameter_name  = "TASK_MANAGEMENT_SERVER_URL"
  parameter_value = var.initial_deployment ? "undefined" : module.task_management.task_management_url
}

module "monitoring_alerts" {
  source                                         = "../../modules/monitoring"
  count                                          = var.enable_notification_alerts ? 1 : 0
  environment                                    = var.environment
  alarms_notification_email                      = var.alarms_notification_email
  aggregator_pub_sub_ack_latency_threshold_ms    = var.aggregator_pub_sub_ack_latency_threshold_ms
  model_updater_pub_sub_ack_latency_threshold_ms = var.model_updater_pub_sub_ack_latency_threshold_ms
  aggregator_subscription_name                   = module.pubsub.aggregator_subscription_name
  model_updater_subscription_name                = module.pubsub.model_updater_subscription_name
  metrics_spanner_instance_name                  = module.storage.metrics_spanner_instance_name
  spanner_instance_name                          = module.storage.spanner_instance_name
  spanner_task_database_name                     = module.storage.spanner_database_name
  spanner_lock_database_name                     = module.storage.spanner_lock_database_name
  task_assignment_report_result_failures         = var.task_assignment_report_result_failures
  task_assignment_no_task_available_failures     = var.task_assignment_no_task_available_failures
  cluster_name                                   = module.gke_cluster.cluster_name
}
