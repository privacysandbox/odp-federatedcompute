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

resource "kubernetes_manifest" "actuator-prometheus-monitoring" {
  manifest = {
    "apiVersion" = "monitoring.googleapis.com/v1"
    "kind"       = "PodMonitoring"
    "metadata" = {
      "name"      = "actuator-prometheus-monitoring"
      "namespace" = "default"
    }
    "spec" = {
      "endpoints" = [
        {
          "interval" = "10s"
          "metricRelabeling" = [
            { "action" = "drop"
              "regex"  = "jvm_.+"
              "sourceLabels" = [
                "__name__",
              ]
            },
            {
              "action" = "drop"
              "regex"  = "logback_events_total"
              "sourceLabels" = [
                "__name__",
              ]
            },
          ]
          "path" = "/actuator/prometheus"
          "port" = "http"
        },
      ]
      "selector" = {
        "matchExpressions" = [
          {
            "key"      = "app"
            "operator" = "In"
            "values" = [
              "taskassignment",
            ]
          },
        ]
      }
    }
  }
}
