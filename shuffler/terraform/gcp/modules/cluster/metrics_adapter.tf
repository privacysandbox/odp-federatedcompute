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

# Terraformed-version of https://raw.githubusercontent.com/GoogleCloudPlatform/k8s-stackdriver/master/custom-metrics-stackdriver-adapter/deploy/production/adapter_new_resource_model.yaml

resource "kubernetes_namespace_v1" "custom-metrics" {
  metadata {
    name = "custom-metrics"
  }
}

module "gke-monitoring-workload-identity" {
  source     = "terraform-google-modules/kubernetes-engine/google//modules/workload-identity"
  name       = "${var.environment}-gke-m-wi"
  namespace  = "custom-metrics"
  project_id = var.project_id
  roles      = ["roles/monitoring.admin"]
}

resource "kubernetes_cluster_role_binding_v1" "cluster_role_binding_system" {
  metadata {
    name = "custom-metrics:system:auth-delegator"
  }
  role_ref {
    api_group = "rbac.authorization.k8s.io"
    kind      = "ClusterRole"
    name      = "system:auth-delegator"
  }
  subject {
    kind      = "ServiceAccount"
    name      = module.gke-monitoring-workload-identity.k8s_service_account_name
    namespace = kubernetes_namespace_v1.custom-metrics.metadata[0].name
  }
}

resource "kubernetes_role_binding_v1" "role_binding" {
  metadata {
    name      = "custom-metrics-auth-reader"
    namespace = "kube-system"
  }
  role_ref {
    api_group = "rbac.authorization.k8s.io"
    kind      = "Role"
    name      = "extension-apiserver-authentication-reader"
  }
  subject {
    kind      = "ServiceAccount"
    name      = module.gke-monitoring-workload-identity.k8s_service_account_name
    namespace = kubernetes_namespace_v1.custom-metrics.metadata[0].name
  }
}

resource "kubernetes_cluster_role_v1" "custom-metrics-reader" {
  metadata {
    name = "custom-metrics-resource-reader"
  }
  rule {
    api_groups = [""]
    resources  = ["pods", "nodes", "nodes/stats"]
    verbs      = ["get", "list", "watch"]
  }
}

resource "kubernetes_cluster_role_binding_v1" "custom-metrics-resource-reader" {
  metadata {
    name = "custom-metrics-resource-reader"
  }
  role_ref {
    api_group = "rbac.authorization.k8s.io"
    kind      = "ClusterRole"
    name      = "custom-metrics-resource-reader"
  }
  subject {
    kind      = "ServiceAccount"
    name      = module.gke-monitoring-workload-identity.k8s_service_account_name
    namespace = kubernetes_namespace_v1.custom-metrics.metadata[0].name
  }
}

resource "kubernetes_deployment_v1" "custom-metrics-stackdriver-adapter" {
  lifecycle {
    ignore_changes = [
      metadata[0].annotations["autopilot.gke.io/resource-adjustment"],
      metadata[0].annotations["autopilot.gke.io/warden-version"]
    ]
  }
  metadata {
    labels = {
      k8s-app = "custom-metrics-stackdriver-adapter"
      run     = "custom-metrics-stackdriver-adapter"
    }
    name      = "custom-metrics-stackdriver-adapter"
    namespace = kubernetes_namespace_v1.custom-metrics.metadata[0].name
  }

  spec {
    replicas = 1
    selector {
      match_labels = {
        k8s-app = "custom-metrics-stackdriver-adapter"
        run     = "custom-metrics-stackdriver-adapter"
      }
    }
    template {
      metadata {
        labels = {
          k8s-app                         = "custom-metrics-stackdriver-adapter"
          "kubernetes.io/cluster-service" = "true"
          run                             = "custom-metrics-stackdriver-adapter"
        }
      }
      spec {
        container {
          command = [
            "/adapter",
            "--use-new-resource-model=true",
            "--fallback-for-container-metrics=true",
          ]
          image             = "gcr.io/gke-release/custom-metrics-stackdriver-adapter:v0.13.1-gke.0"
          image_pull_policy = "Always"
          name              = "pod-custom-metrics-stackdriver-adapter"
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
            requests = {
              cpu               = "500m"
              memory            = "1Gi"
              ephemeral-storage = "1Gi"
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
        service_account_name = module.gke-monitoring-workload-identity.k8s_service_account_name
      }
    }
  }
}

resource "kubernetes_service_v1" "custom-metrics-stackdriver-adapter" {
  lifecycle {
    ignore_changes = [
      metadata[0].annotations["cloud.google.com/neg-status"]
    ]
  }
  metadata {
    labels = {
      k8s-app                         = kubernetes_deployment_v1.custom-metrics-stackdriver-adapter.metadata[0].labels.k8s-app
      "kubernetes.io/cluster-service" = "true"
      "kubernetes.io/name"            = "Adapter"
      run                             = kubernetes_deployment_v1.custom-metrics-stackdriver-adapter.metadata[0].labels.run
    }
    annotations = {
      "cloud.google.com/neg" = "{\"ingress\": true}"
    }
    name      = "custom-metrics-stackdriver-adapter"
    namespace = kubernetes_namespace_v1.custom-metrics.metadata[0].name
  }
  spec {
    port {
      port        = 443
      protocol    = "TCP"
      target_port = 443
    }
    selector = {
      k8s-app = kubernetes_deployment_v1.custom-metrics-stackdriver-adapter.metadata[0].labels.k8s-app
      run     = kubernetes_deployment_v1.custom-metrics-stackdriver-adapter.metadata[0].labels.run
    }
    type = "ClusterIP"
  }
}

resource "kubernetes_api_service_v1" "custom-metrics-v1beta1" {
  metadata {
    name = "v1beta1.custom.metrics.k8s.io"
  }
  spec {
    group                    = "custom.metrics.k8s.io"
    group_priority_minimum   = 100
    insecure_skip_tls_verify = true
    service {
      name      = kubernetes_service_v1.custom-metrics-stackdriver-adapter.metadata[0].name
      namespace = kubernetes_namespace_v1.custom-metrics.metadata[0].name
    }
    version          = "v1beta1"
    version_priority = 100
  }
}

resource "kubernetes_api_service_v1" "custom-metrics-beta2" {
  metadata {
    name = "v1beta2.custom.metrics.k8s.io"
  }
  spec {
    group                    = "custom.metrics.k8s.io"
    group_priority_minimum   = 100
    insecure_skip_tls_verify = true
    service {
      name      = kubernetes_service_v1.custom-metrics-stackdriver-adapter.metadata[0].name
      namespace = kubernetes_namespace_v1.custom-metrics.metadata[0].name
    }
    version          = "v1beta2"
    version_priority = 200
  }
}

resource "kubernetes_api_service_v1" "external-metrics-v1beta1" {
  metadata {
    name = "v1beta1.external.metrics.k8s.io"
  }
  spec {
    group                    = "external.metrics.k8s.io"
    group_priority_minimum   = 100
    insecure_skip_tls_verify = true
    service {
      name      = kubernetes_service_v1.custom-metrics-stackdriver-adapter.metadata[0].name
      namespace = kubernetes_namespace_v1.custom-metrics.metadata[0].name
    }
    version          = "v1beta1"
    version_priority = 100
  }
}