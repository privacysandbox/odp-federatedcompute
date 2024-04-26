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

resource "kubernetes_deployment_v1" "taskscheduler" {
  lifecycle {
    ignore_changes = [
      metadata[0].annotations["autopilot.gke.io/resource-adjustment"],
      metadata[0].annotations["autopilot.gke.io/warden-version"]
    ]
  }
  metadata {
    name = "${var.environment}-taskscheduler"

    labels = {
      maintained_by = "terraform"
      app           = "taskscheduler"
    }
  }

  spec {
    selector {
      match_labels = {
        app = "taskscheduler"
      }
    }
    template {
      metadata {
        labels = {
          app = "taskscheduler"
        }
      }
      // TODO(b/331826774): Configure support for health checks/probes.
      spec {
        service_account_name = module.gke-workload-identity.k8s_service_account_name
        container {
          image = var.task_scheduler_image
          name  = "${var.environment}-taskscheduler"
          env {
            name  = "FCP_OPTS"
            value = "--environment '${var.environment}'"
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
              cpu               = "1500m"
              ephemeral-storage = "1Gi"
              memory            = "8Gi"
            }
          }
          volume_mount {
            mount_path        = "/tmp"
            name              = "tmp-volume"
            read_only         = false
            mount_propagation = "None"
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
        volume {
          name = "tmp-volume"
          empty_dir {
            medium     = "Memory"
            size_limit = "500Mi"
          }
        }
      }
    }
  }
}

// TODO(b/303323046): Update scaling metrics after determining best metric to scale on for the workload.
resource "kubernetes_horizontal_pod_autoscaler_v2" "task-scheduler-hpa" {
  metadata {
    name = "${var.environment}-taskscheduler-hpa"
  }

  spec {
    max_replicas = var.task_scheduler_max_replicas
    min_replicas = var.task_scheduler_min_replicas

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
      name        = kubernetes_deployment_v1.taskscheduler.metadata[0].name
    }
  }
}
