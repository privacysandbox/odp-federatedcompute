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

provider "google-beta" {
  project = var.project_id
  region  = var.region
}

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

resource "google_api_gateway_api" "api_gw" {
  provider = google-beta
  api_id   = "${var.environment}-tb-api-gw"
}

resource "google_api_gateway_api_config" "api_gw" {
  provider             = google-beta
  api                  = google_api_gateway_api.api_gw.api_id
  api_config_id_prefix = "${var.environment}-tb-api-config"

  gateway_config {
    backend_config {
      google_service_account = var.task_builder_service_account
    }
  }

  openapi_documents {
    document {
      path = "spec.yaml"
      contents = base64encode(<<-EOF
        # openapi2-functions.yaml
        swagger: '2.0'
        info:
          title: ${var.environment}-tb-api
          description: API for Task Builder
          version: 1.0.0
        schemes:
          - https
        produces:
          - application/x-protobuf
        paths:
          "/taskbuilder/v1:build-task-group":
            post:
              summary: Creates tasks and artifacts based on SavedModel and TaskConfig input in BuildTaskRequest.
              operationId: build-task-group
              x-google-backend:
                address: "${google_cloud_run_v2_service.taskbuilder.uri}/taskbuilder/v1:build-task-group"
                deadline: 600.0
              security:
              - api_key: []
              responses:
                '200':
                  description: A successful response
                  schema:
                    type: string
          "/taskbuilder/v1:build-artifacts":
            post:
              summary: Creates artifacts only without tasks.
              operationId: build-artifacts
              x-google-backend:
                address: "${google_cloud_run_v2_service.taskbuilder.uri}/taskbuilder/v1:build-artifacts"
                deadline: 600.0
              security:
              - api_key: []
              responses:
                '200':
                  description: A successful response
                  schema:
                    type: string
        securityDefinitions:
          # This section configures basic authentication with an API key.
          api_key:
            type: "apiKey"
            name: "key"
            in: "query"
    EOF
      )
    }
  }
  lifecycle {
    create_before_destroy = true
  }
}

resource "google_api_gateway_gateway" "api_gw" {
  provider   = google-beta
  api_config = google_api_gateway_api_config.api_gw.id
  gateway_id = "${var.environment}-tb-api-gw"
}