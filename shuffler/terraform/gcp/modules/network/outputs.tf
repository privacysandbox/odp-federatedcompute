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

output "network_name" {
  description = "The name of the VPC being created"
  value       = module.gcp-network.network_name
}

output "subnets_names" {
  description = "The names of the subnet being created"
  value       = module.gcp-network.subnets_names
}

output "subnets_self_links" {
  description = "The self-links of subnets being created"
  value       = module.gcp-network.subnets_self_links
}

output "static_ip" {
  description = "The allocated static ip address"
  value       = google_compute_global_address.default.address
}

output "static_ip_name" {
  description = "The name of the allocated static ip address"
  value       = google_compute_global_address.default.name
}


