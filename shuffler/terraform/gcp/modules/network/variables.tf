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

################################################################################
# Global Variables.
################################################################################

variable "project_id" {
  description = "GCP Project ID in which this module will be created."
  type        = string
}

variable "environment" {
  description = "Description for the environment, e.g. dev, staging, production"
  type        = string
}

variable "region" {
  description = "Region where all services will be created."
  type        = string
}

variable "network_name" {
  description = "The name of the network being created."
  type        = string
}

variable "subnet_name" {
  description = "The name of the subnet being created."
  type        = string
}

variable "master_auth_subnetwork" {
  description = "The name of the master auth subnetwork being created."
  type        = string
}

variable "pods_range_name" {
  description = "The name of the pods range subnet being created."
  type        = string
}

variable "svc_range_name" {
  description = "The name of the svc range subnet being created."
  type        = string
}

variable "parent_domain_name" {
  description = "Custom domain name to register and use for external APIs."
  type        = string
}

