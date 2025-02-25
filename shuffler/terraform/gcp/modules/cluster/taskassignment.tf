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

resource "kubernetes_deployment_v1" "taskassignment" {
  lifecycle {
    ignore_changes = [
      metadata[0].annotations["autopilot.gke.io/resource-adjustment"],
      metadata[0].annotations["autopilot.gke.io/warden-version"]
    ]
  }
  metadata {
    name = "${var.environment}-taskassignment"

    labels = {
      maintained_by = "terraform"
      app           = "taskassignment"
    }
  }

  spec {
    strategy {
      type = "RollingUpdate"
      rolling_update {
        max_surge       = "50%"
        max_unavailable = "50%"
      }
    }
    selector {
      match_labels = {
        app = "taskassignment"
      }
    }
    template {
      metadata {
        labels = {
          app = "taskassignment"
        }
      }
      spec {
        service_account_name             = module.gke-workload-identity.k8s_service_account_name
        termination_grace_period_seconds = 210
        container {
          image = var.task_assignment_image
          name  = "${var.environment}-taskassignment"
          env {
            name  = "FCP_OPTS"
            value = "--environment '${var.environment}'"
          }
          port {
            name           = "http"
            container_port = var.task_assignment_port
          }
          readiness_probe {
            http_get {
              path = "/ready"
              port = var.task_assignment_port
            }
          }
          liveness_probe {
            http_get {
              path = "/healthz"
              port = var.task_assignment_port
            }
          }
          startup_probe {
            http_get {
              path = "/healthz"
              port = var.task_assignment_port
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
          lifecycle {
            pre_stop {
              exec {
                command = ["sleep", "60"]
              }
            }
          }
          resources {
            // Limits are not used for autopilot: https://cloud.google.com/kubernetes-engine/docs/concepts/autopilot-resource-requests#resource-limits
            requests = {
              cpu               = var.task_assignment_cpu
              ephemeral-storage = "1Gi"
              memory            = "${var.task_assignment_cpu * 2}Gi" # Use 1:2 CPU-to-memory
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
      }
    }
  }
}

resource "kubernetes_manifest" "taskassignment-backend" {
  manifest = {
    "apiVersion" = "cloud.google.com/v1"
    "kind"       = "BackendConfig"
    "metadata" = {
      "name"      = "${var.environment}-taskassignment-backend-config"
      "namespace" = "default"
    }
    "spec" = {
      "healthCheck" = {
        requestPath = "/healthz"
      }
      "connectionDraining" = {
        drainingTimeoutSec = 60
      }
    }
  }
}

resource "kubernetes_service_v1" "taskassignment" {
  lifecycle {
    ignore_changes = [
      metadata[0].annotations["cloud.google.com/neg-status"]
    ]
  }
  metadata {
    name = "${var.environment}-taskassignment"
    labels = {
      maintained_by = "terraform"
      app           = kubernetes_deployment_v1.taskassignment.metadata[0].labels.app
    }
    annotations = {
      "cloud.google.com/backend-config"         = "{\"default\": \"${kubernetes_manifest.taskassignment-backend.manifest.metadata.name}\"}"
      "cloud.google.com/neg"                    = "{\"ingress\": true}"
      "networking.gke.io/max-rate-per-endpoint" = "100"
    }
  }

  spec {
    selector = {
      app = kubernetes_deployment_v1.taskassignment.metadata[0].labels.app
    }

    session_affinity = "ClientIP"

    port {
      port        = var.task_assignment_port
      target_port = var.task_assignment_port
    }

    type = "NodePort"
  }
}

resource "kubernetes_horizontal_pod_autoscaler_v2" "taskassignment-hpa" {
  metadata {
    name = "${var.environment}-taskassignment-hpa"
  }

  spec {
    max_replicas = var.task_assignment_max_replicas
    min_replicas = var.task_assignment_min_replicas

    metric {
      type = "Object"
      object {
        described_object {
          kind        = "Service"
          name        = kubernetes_service_v1.taskassignment.metadata[0].name
          api_version = "v1"
        }
        metric {
          name = "autoscaling.googleapis.com|gclb-capacity-utilization"
        }
        target {
          type          = "AverageValue"
          average_value = "70"
        }
      }
    }

    scale_target_ref {
      api_version = "apps/v1"
      kind        = "Deployment"
      name        = kubernetes_deployment_v1.taskassignment.metadata[0].name
    }
  }
}
