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
  notification_channel_id = google_monitoring_notification_channel.alarm_email.id
}

resource "google_monitoring_alert_policy" "model_updater_pub_sub_ack_latency_alert_policy" {
  display_name = "${var.environment} Model Updater Pub/Sub Subscription - Ack latency"
  conditions {
    display_name = "${var.environment} Model Updater Pub/Sub ack latency > ${var.model_updater_pub_sub_ack_latency_threshold_ms}ms"
    condition_threshold {
      filter = "resource.type = \"pubsub_subscription\" AND resource.labels.subscription_id = \"${var.model_updater_subscription_name}\" AND metric.type = \"pubsub.googleapis.com/subscription/ack_latencies\""
      aggregations {
        alignment_period     = "300s" # 5 min
        cross_series_reducer = "REDUCE_NONE"
        per_series_aligner   = "ALIGN_PERCENTILE_99"
      }

      comparison = "COMPARISON_GT"
      duration   = "0s"
      trigger {
        count = 1
      }
      threshold_value = var.model_updater_pub_sub_ack_latency_threshold_ms
    }
  }
  combiner              = "OR"
  notification_channels = [local.notification_channel_id]
  severity              = "WARNING"
}

resource "google_monitoring_alert_policy" "aggregator_pub_sub_ack_latency_alert_policy" {
  display_name = "${var.environment} Aggregator Pub/Sub Subscription - Ack latency"
  conditions {
    display_name = "${var.environment} Aggregator Pub/Sub ack latency > ${var.aggregator_pub_sub_ack_latency_threshold_ms}ms"
    condition_threshold {
      filter = "resource.type = \"pubsub_subscription\" AND resource.labels.subscription_id = \"${var.aggregator_subscription_name}\" AND metric.type = \"pubsub.googleapis.com/subscription/ack_latencies\""
      aggregations {
        alignment_period     = "120s" # 2 min
        cross_series_reducer = "REDUCE_NONE"
        per_series_aligner   = "ALIGN_PERCENTILE_99"
      }

      comparison = "COMPARISON_GT"
      duration   = "0s"
      trigger {
        count = 3
      }
      threshold_value = var.aggregator_pub_sub_ack_latency_threshold_ms
    }
  }
  combiner              = "OR"
  notification_channels = [local.notification_channel_id]
  severity              = "WARNING"
}

resource "google_monitoring_alert_policy" "pub_sub_dlq_message_alert_policy" {
  display_name = "${var.environment} Pub/Sub Subscription - Dead letter queue message count"
  conditions {
    display_name = "${var.environment} Pub/Sub Subscription - Dead letter queue message count > 0"
    condition_threshold {
      filter = "resource.type = \"pubsub_subscription\" AND metric.type = \"pubsub.googleapis.com/subscription/num_unacked_messages_by_region\" AND resource.labels.subscription_id = monitoring.regex.full_match(\".*dlq.*${var.environment}.*\")"
      aggregations {
        alignment_period     = "300s" # 5 min
        cross_series_reducer = "REDUCE_NONE"
        per_series_aligner   = "ALIGN_MAX"
      }
      comparison = "COMPARISON_GT"
      duration   = "0s"
      trigger {
        count = 1
      }
      threshold_value = 0 # messages in dlq
    }
  }
  combiner              = "OR"
  notification_channels = [local.notification_channel_id]
  severity              = "WARNING"
}

resource "google_monitoring_alert_policy" "spanner_instance_storage_utilization_alert_policy" {

  display_name = "${var.environment} Spanner Instance - Storage Utilization Warning"
  conditions {
    display_name = "${var.environment} Spanner Instance Storage > 70%"
    condition_threshold {
      filter = "resource.type = \"spanner_instance\" AND resource.labels.instance_id = one_of(\"${var.spanner_instance_name}\", \"${var.metrics_spanner_instance_name}\") AND metric.type = \"spanner.googleapis.com/instance/storage/utilization\""
      aggregations {
        alignment_period     = "600s" # 10 min
        cross_series_reducer = "REDUCE_SUM"
        group_by_fields = [
          "resource.labels.instance_id"
        ]
        per_series_aligner = "ALIGN_MAX"
      }

      comparison = "COMPARISON_GT"
      duration   = "600s"
      trigger {
        count = 1
      }
      threshold_value = 0.7 # 70% of storage utilization
    }
  }
  combiner              = "OR"
  notification_channels = [local.notification_channel_id]
  severity              = "WARNING"
}

resource "google_monitoring_alert_policy" "spanner_metrics_query_latency_alert_policy" {
  display_name = "${var.environment} Spanner Metrics DB - Query latencies"
  conditions {
    display_name = "${var.environment} Spanner Metrics Query Latency > 100ms"
    condition_threshold {
      filter = "resource.type = \"spanner_instance\" AND resource.labels.instance_id = \"${var.metrics_spanner_instance_name}\" AND metric.type = \"spanner.googleapis.com/query_stat/total/query_latencies\""
      aggregations {
        alignment_period     = "300s" # 5 min
        cross_series_reducer = "REDUCE_NONE"
        per_series_aligner   = "ALIGN_PERCENTILE_99"
      }
      comparison = "COMPARISON_GT"
      duration   = "0s"
      trigger {
        count = 2
      }
      threshold_value = 0.3 # seconds
    }
  }
  combiner              = "OR"
  notification_channels = [local.notification_channel_id]
  severity              = "WARNING"
}

resource "google_monitoring_alert_policy" "spanner_lock_query_latency_alert_policy" {
  display_name = "${var.environment} Spanner Lock DB - Query latencies"
  conditions {
    display_name = "${var.environment} Spanner Lock Query Latency > 300ms"
    condition_threshold {
      filter = "resource.type = \"spanner_instance\" AND resource.labels.instance_id = \"${var.spanner_instance_name}\" AND metric.labels.database = \"${var.spanner_lock_database_name}\" AND metric.type = \"spanner.googleapis.com/query_stat/total/query_latencies\""
      aggregations {
        alignment_period     = "120s" # 2 min
        cross_series_reducer = "REDUCE_NONE"
        per_series_aligner   = "ALIGN_PERCENTILE_99"
      }
      comparison = "COMPARISON_GT"
      duration   = "0s"
      trigger {
        count = 2
      }
      threshold_value = 0.3 # seconds
    }
  }
  combiner              = "OR"
  notification_channels = [local.notification_channel_id]
  severity              = "WARNING"
}

resource "google_monitoring_alert_policy" "spanner_task_query_latency_alert_policy" {
  display_name = "${var.environment} Spanner Task DB - Query latencies"
  conditions {
    display_name = "${var.environment} Spanner Task Query Latency > 100ms"
    condition_threshold {
      filter = "resource.type = \"spanner_instance\" AND resource.labels.instance_id = \"${var.spanner_instance_name}\" AND metric.labels.database = \"${var.spanner_task_database_name}\" AND metric.type = \"spanner.googleapis.com/query_stat/total/query_latencies\""
      aggregations {
        alignment_period     = "120s" # 2 min
        cross_series_reducer = "REDUCE_NONE"
        per_series_aligner   = "ALIGN_PERCENTILE_99"
      }
      comparison = "COMPARISON_GT"
      duration   = "0s"
      trigger {
        count = 2
      }
      threshold_value = 0.1 # seconds
    }
  }
  combiner              = "OR"
  notification_channels = [local.notification_channel_id]
  severity              = "WARNING"
}

resource "google_monitoring_alert_policy" "spanner_api_latency_alert_policy" {
  display_name = "${var.environment} Spanner Instance - Request latencies"
  conditions {
    display_name = "${var.environment} Spanner API Latency > 500ms"
    condition_threshold {
      filter = "resource.type = \"spanner_instance\" AND resource.labels.instance_id = one_of(\"${var.spanner_instance_name}\", \"${var.metrics_spanner_instance_name}\") AND metric.type = \"spanner.googleapis.com/api/request_latencies\""
      aggregations {
        alignment_period     = "300s" # 5 min
        cross_series_reducer = "REDUCE_NONE"
        per_series_aligner   = "ALIGN_PERCENTILE_99"
      }
      comparison = "COMPARISON_GT"
      duration   = "300s"
      trigger {
        count = 1
      }
      threshold_value = 0.5 # seconds
    }
  }
  combiner              = "OR"
  notification_channels = [local.notification_channel_id]
  # TODO: Reduce API scope before enabling.
  enabled  = false
  severity = "WARNING"
}

resource "google_monitoring_alert_policy" "task_assignment_report_result_alert_policy" {
  display_name = "${var.environment} Task Assignment Report Result Failures"
  conditions {
    display_name = "${var.environment} Report Result Failures > ${var.task_assignment_report_result_failures}"
    condition_threshold {
      filter = "resource.type = \"prometheus_target\" AND metric.type = \"prometheus.googleapis.com/report_result_seconds_count/summary\" AND metric.labels.result != \"COMPLETED\" AND resource.labels.cluster = \"${var.cluster_name}\""
      aggregations {
        alignment_period     = "3600s" # 1 hour
        cross_series_reducer = "REDUCE_MAX"
        group_by_fields = [
          "metric.label.result",
          "metric.label.population"
        ]
        per_series_aligner = "ALIGN_DELTA"
      }
      comparison = "COMPARISON_GT"
      duration   = "0s"
      trigger {
        count = 1
      }
      threshold_value = var.task_assignment_report_result_failures
    }
  }
  combiner              = "OR"
  notification_channels = [local.notification_channel_id]
  severity              = "WARNING"
}

resource "google_monitoring_alert_policy" "task_assignment_no_task_available_alert_policy" {
  display_name = "${var.environment} Task Assignment No Task Available Failures"
  conditions {
    display_name = "${var.environment} No_Task_Available Failures > ${var.task_assignment_no_task_available_failures}"
    condition_threshold {
      filter = "resource.type = \"prometheus_target\" AND metric.type = \"prometheus.googleapis.com/create_task_assignment_seconds_count/summary\" AND metric.labels.result = \"NO_TASK_AVAILABLE\" AND resource.labels.cluster = \"${var.cluster_name}\""
      aggregations {
        alignment_period     = "3600s" # 1 hour
        cross_series_reducer = "REDUCE_MAX"
        group_by_fields = [
          "metric.label.result",
          "metric.label.population"
        ]
        per_series_aligner = "ALIGN_DELTA"
      }
      comparison = "COMPARISON_GT"
      duration   = "0s"
      trigger {
        count = 1
      }
      threshold_value = var.task_assignment_no_task_available_failures
    }
  }
  combiner              = "OR"
  notification_channels = [local.notification_channel_id]
  severity              = "WARNING"
}

resource "google_monitoring_alert_policy" "iteration_failures_log_alert_policy" {

  display_name = "${var.environment} Iteration Failures"
  conditions {
    display_name = "${var.environment} Found iteration failure"
    condition_matched_log {
      filter = "resource.labels.container_name=\"${var.environment}-collector\"\ntextPayload=~\"Iteration ([a-zA-Z0-9_\\.\\-]+/[0-9]+/[0-9]+/[0-9]+) in (APPLYING|AGGREGATING)_FAILED status\\.\"\nseverity=\"WARNING\""
    }
  }
  alert_strategy {
    notification_rate_limit {
      period = "300s" # 5 min
    }
    auto_close = "172800s" # 2 days
  }
  combiner              = "OR"
  notification_channels = [local.notification_channel_id]
  severity              = "WARNING"
}

resource "google_monitoring_notification_channel" "alarm_email" {
  display_name = "${var.environment} FCP Alarm Notification Email"
  type         = "email"
  labels = {
    email_address = var.alarms_notification_email
  }

  lifecycle {
    # Email should not be empty
    precondition {
      condition     = var.alarms_notification_email != ""
      error_message = "var.enable_notification_alerts is true with an empty var.alarms_notification_email."
    }
  }
}
