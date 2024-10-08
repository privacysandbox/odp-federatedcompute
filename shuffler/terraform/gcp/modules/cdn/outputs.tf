# Copyright 2024 Google LLC
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

output "cdn_domain" {
  value = local.domain
}

output "key_value_a" {
  value = google_compute_backend_bucket_signed_url_key.backend_key_a.key_value
}

output "key_value_b" {
  value = google_compute_backend_bucket_signed_url_key.backend_key_b.key_value
}

output "key_name" {
  value = toggles_leapfrog.toggle.alpha ? google_compute_backend_bucket_signed_url_key.backend_key_a.name : google_compute_backend_bucket_signed_url_key.backend_key_b.name
}