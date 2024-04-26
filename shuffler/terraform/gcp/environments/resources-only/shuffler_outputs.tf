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

output "model_bucket_name" {
  value = module.shuffler.model_bucket_name
}

output "client_gradient_bucket_name" {
  value = module.shuffler.client_gradient_bucket_name
}

output "aggregated_gradient_bucket_name" {
  value = module.shuffler.aggregated_gradient_bucket_name
}

output "spanner_database_name" {
  value       = module.shuffler.spanner_database_name
  description = "Name of the FCP task Spanner database."
}

output "spanner_lock_database_name" {
  value       = module.shuffler.spanner_lock_database_name
  description = "Name of the FCP lock Spanner database."
}

output "spanner_instance_name" {
  value       = module.shuffler.spanner_instance_name
  description = "Name of the FCP task Spanner instance."
}

output "aggregator_topic_name" {
  value       = module.shuffler.aggregator_topic_name
  description = "Name of the aggregator pubsub topic."
}

output "aggregator_subscription_name" {
  value       = module.shuffler.aggregator_subscription_name
  description = "Name of the aggregator pubsub subscription."
}

output "model_updater_topic_name" {
  value       = module.shuffler.model_updater_topic_name
  description = "Name of the model_updater pubsub topic."
}

output "model_updater_subscription_name" {
  value       = module.shuffler.model_updater_subscription_name
  description = "Name of the model_updater pubsub subscription."
}
