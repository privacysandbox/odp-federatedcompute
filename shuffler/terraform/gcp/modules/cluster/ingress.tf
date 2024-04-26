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

resource "kubernetes_ingress_v1" "ingress" {
  metadata {
    name = "${var.environment}-ingress"
    annotations = {
      "kubernetes.io/ingress.allow-http"            = "false"
      "kubernetes.io/ingress.global-static-ip-name" = var.static_ip_name
      "ingress.gcp.kubernetes.io/pre-shared-cert"   = google_compute_managed_ssl_certificate.default.name
    }
    labels = {
      maintained_by = "terraform"
    }
  }

  spec {
    rule {
      http {
        path {
          backend {
            service {
              name = kubernetes_service_v1.taskassignment.metadata[0].name
              port {
                number = var.task_assignment_port
              }
            }
          }

          path = "/taskassignment/*"
        }
      }
    }
  }
}

resource "google_compute_managed_ssl_certificate" "default" {
  name     = "${var.environment}-cert"
  provider = google
  managed {
    domains = [var.parent_domain_name]
  }
}