/**
 * Copyright 2022 Google LLC
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

module "gke" {
  source                     = "terraform-google-modules/kubernetes-engine/google//modules/beta-autopilot-public-cluster"
  project_id                 = var.project_id
  name                       = "${var.environment}-cluster"
  regional                   = true
  region                     = var.region
  network                    = var.network_name
  subnetwork                 = var.subnets_names[index(var.subnets_names, var.subnet_name)]
  ip_range_pods              = var.pods_range_name
  ip_range_services          = var.svc_range_name
  release_channel            = "REGULAR"
  horizontal_pod_autoscaling = true
  http_load_balancing        = true
  network_tags               = [var.environment]
  service_account            = var.cluster_service_account
  cluster_resource_labels = {
    environment = var.environment
  }
  deletion_protection = var.cluster_deletion_protection
}