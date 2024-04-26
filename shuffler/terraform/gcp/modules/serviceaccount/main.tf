/**
 * Copyright 2024 Google LLC
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
  service_account_email = var.service_account_email == "" ? google_service_account.service_account[0].email : var.service_account_email
}

resource "google_service_account" "service_account" {
  count = var.service_account_email == "" ? 1 : 0
  # Service account id has a 30 character limit
  account_id = "${var.environment}-${var.service_account_name}"
}

resource "google_project_iam_member" "worker_logging_iam" {
  for_each = toset(var.roles)
  role     = each.value
  member   = "serviceAccount:${local.service_account_email}"
  project  = var.project_id
}


