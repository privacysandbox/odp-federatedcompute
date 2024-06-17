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

resource "google_cloud_run_v2_service" "taskbuilder" {
  name     = "${var.environment}-task-builder-service"
  location = var.region
  ingress  = "INGRESS_TRAFFIC_ALL"
  template {
    service_account = var.task_builder_service_account
    scaling {
      min_instance_count = var.task_builder_min_replicas
      max_instance_count = var.task_builder_max_replicas
    }
    containers {
      image = var.task_builder_image
      env {
        name  = "ENV"
        value = var.environment
      }
      env {
        name  = "PYTHONUNBUFFERED"
        value = 1
      }
      env {
        name = "TASK_MANAGEMENT_SERVER_URL"
        value_source {
          secret_key_ref {
            secret  = var.task_management_server_url_secret
            version = "latest"
          }
        }
      }
      ports {
        name           = "http1"
        container_port = var.task_builder_port
      }
      resources {
        limits = {
          cpu    = "2"
          memory = "8Gi"
        }
        startup_cpu_boost = true
      }
      startup_probe {
        failure_threshold = 20
        period_seconds    = 10
        timeout_seconds   = 5
        http_get {
          path = "/ready"
          port = var.task_builder_port
        }
      }
      liveness_probe {
        http_get {
          path = "/healthz"
          port = var.task_builder_port
        }
      }
    }
  }
  traffic {
    type    = "TRAFFIC_TARGET_ALLOCATION_TYPE_LATEST"
    percent = 100
  }
}