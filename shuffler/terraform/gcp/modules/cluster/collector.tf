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

resource "kubernetes_deployment_v1" "collector" {
  lifecycle {
    ignore_changes = [
      metadata[0].annotations["autopilot.gke.io/resource-adjustment"],
      metadata[0].annotations["autopilot.gke.io/warden-version"]
    ]
  }
  metadata {
    name = "${var.environment}-collector"

    labels = {
      maintained_by = "terraform"
      app           = "collector"
    }
  }

  spec {
    selector {
      match_labels = {
        app = "collector"
      }
    }
    template {
      metadata {
        labels = {
          app = "collector"
        }
      }
      spec {
        service_account_name = module.gke-workload-identity.k8s_service_account_name
        node_selector = {
          "cloud.google.com/compute-class" : "Balanced"
        }
        container {
          image = var.collector_image
          name  = "${var.environment}-collector"
          env {
            name  = "FCP_OPTS"
            value = "--environment '${var.environment}'"
          }
          port {
            container_port = 8082
          }
          readiness_probe {
            http_get {
              path = "/ready"
              port = 8082
            }
          }
          liveness_probe {
            http_get {
              path = "/healthz"
              port = 8082
            }
          }
          startup_probe {
            http_get {
              path = "/healthz"
              port = 8082
            }
            success_threshold = 1
            failure_threshold = 20
            period_seconds    = 10
            timeout_seconds   = 5
          }
          security_context {
            allow_privilege_escalation = false
            privileged                 = false
            read_only_root_filesystem  = false
            run_as_non_root            = false
            capabilities {
              add  = []
              drop = ["NET_RAW"]
            }
          }
          resources {
            // Limits are not used for autopilot: https://cloud.google.com/kubernetes-engine/docs/concepts/autopilot-resource-requests#resource-limits
            requests = {
              cpu               = var.collector_cpu
              ephemeral-storage = "1Gi"
              memory            = "${var.collector_cpu}Gi" # Use 1:1 CPU-to-memory
            }
          }
        }
        security_context {
          run_as_non_root     = false
          supplemental_groups = []
          seccomp_profile {
            type = "RuntimeDefault"
          }
        }
        toleration {
          effect   = "NoSchedule"
          key      = "kubernetes.io/arch"
          operator = "Equal"
          value    = "amd64"
        }
        toleration {
          effect   = "NoSchedule"
          key      = "cloud.google.com/compute-class"
          operator = "Equal"
          value    = "Balanced"
        }
      }
    }
  }
}

// TODO(b/303323046): Update scaling metrics after determining best metric to scale on for the workload.
resource "kubernetes_horizontal_pod_autoscaler_v2" "collector-hpa" {
  metadata {
    name = "${var.environment}-collector-hpa"
  }

  spec {
    max_replicas = var.collector_max_replicas
    min_replicas = var.collector_min_replicas

    metric {
      type = "Resource"
      resource {
        name = "cpu"
        target {
          type                = "Utilization"
          average_utilization = "70"
        }
      }
    }

    scale_target_ref {
      api_version = "apps/v1"
      kind        = "Deployment"
      name        = kubernetes_deployment_v1.collector.metadata[0].name
    }
  }
}
