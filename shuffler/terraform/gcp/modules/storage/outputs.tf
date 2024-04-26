# Copyright 2023 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

output "model_bucket_name" {
  value = google_storage_bucket.model_bucket.name
}

output "client_gradient_bucket_name" {
  value = google_storage_bucket.client_gradient_bucket.name
}

output "aggregated_gradient_bucket_name" {
  value = google_storage_bucket.aggregated_gradient_bucket.name
}

output "spanner_database_name" {
  value       = google_spanner_database.fcp_task_spanner_database.name
  description = "Name of the FCP task Spanner database."
}

output "spanner_lock_database_name" {
  value       = google_spanner_database.fcp_lock_spanner_database.name
  description = "Name of the FCP lock Spanner database."
}

output "spanner_instance_name" {
  value       = google_spanner_instance.fcp_task_spanner_instance.name
  description = "Name of the FCP task Spanner instance."
}