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

module "cluster" {
  source                       = "../../../applications/cluster"
  environment                  = var.environment
  project_id                   = var.project_id
  region                       = var.region
  task_assignment_image        = var.task_assignment_image
  task_assignment_port         = var.task_assignment_port
  task_scheduler_image         = var.task_scheduler_image
  collector_image              = var.collector_image
  parent_domain_name           = var.parent_domain_name
  static_ip_name               = var.static_ip_name
  task_assignment_max_replicas = var.task_assignment_max_replicas
  task_assignment_min_replicas = var.task_assignment_min_replicas
  task_assignment_cpu          = var.task_assignment_cpu
  collector_min_replicas       = var.collector_min_replicas
  collector_max_replicas       = var.collector_max_replicas
  collector_cpu                = var.collector_cpu
  task_scheduler_min_replicas  = var.task_scheduler_min_replicas
  task_scheduler_max_replicas  = var.task_scheduler_max_replicas
  task_scheduler_cpu           = var.task_scheduler_cpu
}
