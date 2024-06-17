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


# Note: name max length = 63
resource "google_storage_bucket" "model_bucket" {
  name                        = "fcp-${var.environment}-m-0"
  location                    = var.region
  force_destroy               = var.model_bucket_force_destroy
  project                     = var.project_id
  public_access_prevention    = "enforced"
  uniform_bucket_level_access = true

  versioning {
    enabled = var.model_bucket_versioning
  }

  lifecycle_rule {
    action {
      type = "Delete"
    }
    condition {
      age = var.model_bucket_lifecycle_age_days
    }
  }

  lifecycle_rule {
    action {
      type = "Delete"
    }
    condition {
      days_since_noncurrent_time = 10
    }
  }
}

resource "google_storage_bucket" "client_gradient_bucket" {
  name                        = "fcp-${var.environment}-g-0"
  location                    = var.region
  project                     = var.project_id
  force_destroy               = var.client_gradient_bucket_force_destroy
  public_access_prevention    = "enforced"
  uniform_bucket_level_access = true

  versioning {
    enabled = var.client_gradient_bucket_versioning
  }

  lifecycle_rule {
    action {
      type = "Delete"
    }
    condition {
      age = var.client_gradient_bucket_lifecycle_age_days
    }
  }

  lifecycle_rule {
    action {
      type = "Delete"
    }
    condition {
      days_since_noncurrent_time = 10
    }
  }
}

resource "google_storage_bucket" "aggregated_gradient_bucket" {
  name                        = "fcp-${var.environment}-a-0"
  location                    = var.region
  project                     = var.project_id
  force_destroy               = var.aggregated_gradient_bucket_force_destroy
  public_access_prevention    = "enforced"
  uniform_bucket_level_access = true

  versioning {
    enabled = var.aggregated_gradient_bucket_versioning
  }

  lifecycle_rule {
    action {
      type = "Delete"
    }
    condition {
      age = var.aggregated_gradient_bucket_lifecycle_age_days
    }
  }

  lifecycle_rule {
    action {
      type = "Delete"
    }
    condition {
      days_since_noncurrent_time = 10
    }
  }
}

# Note: display_name max length = 30
resource "google_spanner_instance" "fcp_task_spanner_instance" {
  name             = "fcp-task-${var.environment}"
  display_name     = "fcp-task-${var.environment}"
  project          = var.project_id
  config           = var.spanner_instance_config
  processing_units = var.spanner_processing_units
}

resource "google_spanner_database" "fcp_task_spanner_database" {
  instance                 = google_spanner_instance.fcp_task_spanner_instance.name
  name                     = "fcp-task-db-${var.environment}"
  project                  = var.project_id
  version_retention_period = var.spanner_database_retention_period
  deletion_protection      = var.spanner_database_deletion_protection
}

resource "google_spanner_database" "fcp_lock_spanner_database" {
  instance                 = google_spanner_instance.fcp_task_spanner_instance.name
  name                     = "fcp-lock-db-${var.environment}"
  project                  = var.project_id
  version_retention_period = var.spanner_database_retention_period
  deletion_protection      = var.spanner_database_deletion_protection
  // Spring JDBC Lock Registry DDL
  // https://docs.spring.io/spring-integration/reference/jdbc/lock-registry.html
  database_dialect = "POSTGRESQL"
  ddl = [
    <<-EOT
    CREATE TABLE INT_LOCK (
      LOCK_KEY VARCHAR(36),
      REGION VARCHAR(100),
      CLIENT_ID VARCHAR(36),
      CREATED_DATE TIMESTAMPTZ NOT NULL,
      PRIMARY KEY (LOCK_KEY, REGION)
    )
    EOT
  ]
}

resource "google_spanner_instance" "fcp_metrics_spanner_instance" {
  name             = "fcp-metric-${var.environment}"
  display_name     = "fcp-metric-${var.environment}"
  project          = var.project_id
  config           = var.spanner_instance_config
  processing_units = var.metric_spanner_processing_units
}

resource "google_spanner_database" "fcp_metrics_spanner_database" {
  instance                 = google_spanner_instance.fcp_metrics_spanner_instance.name
  name                     = "fcp-metric-db-${var.environment}"
  project                  = var.project_id
  version_retention_period = var.spanner_database_retention_period
  deletion_protection      = var.spanner_database_deletion_protection
}