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

resource "google_monitoring_dashboard" "create_ta_api_result_by_population" {
  dashboard_json = <<EOF
  {
    "displayName": "CreateTaskAssignment Result By Population - ${var.environment}",
    "mosaicLayout": {
      "columns": 48,
      "tiles": [
        {
          "yPos": 23,
          "width": 48,
          "height": 23,
          "widget": {
            "title": "Result: NO_Task_Available rate per population",
            "xyChart": {
              "dataSets": [
                {
                  "timeSeriesQuery": {
                    "timeSeriesFilter": {
                      "filter": "metric.type=\"prometheus.googleapis.com/create_task_assignment_seconds_count/summary\" resource.type=\"prometheus_target\" metric.label.\"result\"=\"NO_TASK_AVAILABLE\"",
                      "aggregation": {
                        "alignmentPeriod": "60s",
                        "perSeriesAligner": "ALIGN_RATE",
                        "crossSeriesReducer": "REDUCE_SUM",
                        "groupByFields": [
                          "metric.label.\"population\""
                        ]
                      }
                    },
                    "unitOverride": "",
                    "outputFullDuration": false
                  },
                  "plotType": "LINE",
                  "legendTemplate": "",
                  "minAlignmentPeriod": "60s",
                  "targetAxis": "Y1",
                  "dimensions": [],
                  "measures": [],
                  "breakdowns": []
                }
              ],
              "thresholds": [],
              "yAxis": {
                "label": "",
                "scale": "LINEAR"
              },
              "chartOptions": {
                "mode": "COLOR",
                "showLegend": false,
                "displayHorizontal": false
              }
            },
            "id": ""
          }
        },
        {
          "width": 48,
          "height": 23,
          "widget": {
            "title": "Result: Created rate per population",
            "xyChart": {
              "dataSets": [
                {
                  "timeSeriesQuery": {
                    "timeSeriesFilter": {
                      "filter": "metric.type=\"prometheus.googleapis.com/create_task_assignment_seconds_count/summary\" resource.type=\"prometheus_target\" metric.label.\"result\"=\"CREATED\"",
                      "aggregation": {
                        "alignmentPeriod": "60s",
                        "perSeriesAligner": "ALIGN_RATE",
                        "crossSeriesReducer": "REDUCE_SUM",
                        "groupByFields": [
                          "metric.label.\"population\""
                        ]
                      }
                    },
                    "unitOverride": "",
                    "outputFullDuration": false
                  },
                  "plotType": "LINE",
                  "legendTemplate": "",
                  "minAlignmentPeriod": "60s",
                  "targetAxis": "Y1",
                  "dimensions": [],
                  "measures": [],
                  "breakdowns": []
                }
              ],
              "thresholds": [],
              "yAxis": {
                "label": "",
                "scale": "LINEAR"
              },
              "chartOptions": {
                "mode": "COLOR",
                "showLegend": false,
                "displayHorizontal": false
              }
            },
            "id": ""
          }
        }
      ]
    },
    "dashboardFilters": [
      {
          "filterType": "RESOURCE_LABEL",
          "labelKey": "cluster",
          "stringValue": "${var.environment}-cluster",
          "templateVariable": ""
        }
      ],
    "labels": {}
  }

EOF
}

resource "google_monitoring_dashboard" "report_result_api_result_by_population" {
  dashboard_json = <<EOF
  {
    "displayName": "ReportResult Result By Population - ${var.environment}",
    "mosaicLayout": {
      "columns": 48,
      "tiles": [
        {
          "yPos": 23,
          "width": 48,
          "height": 23,
          "widget": {
            "title": "Result: Failed rate per population",
            "xyChart": {
              "dataSets": [
                {
                  "timeSeriesQuery": {
                    "timeSeriesFilter": {
                      "filter": "metric.type=\"prometheus.googleapis.com/report_result_seconds_count/summary\" resource.type=\"prometheus_target\" metric.label.\"result\"=\"FAILED\"",
                      "aggregation": {
                        "alignmentPeriod": "60s",
                        "perSeriesAligner": "ALIGN_RATE",
                        "crossSeriesReducer": "REDUCE_SUM",
                        "groupByFields": [
                          "metric.label.\"population\""
                        ]
                      }
                    },
                    "unitOverride": "",
                    "outputFullDuration": false
                  },
                  "plotType": "LINE",
                  "legendTemplate": "",
                  "minAlignmentPeriod": "60s",
                  "targetAxis": "Y1",
                  "dimensions": [],
                  "measures": [],
                  "breakdowns": []
                }
              ],
              "thresholds": [],
              "yAxis": {
                "label": "",
                "scale": "LINEAR"
              },
              "chartOptions": {
                "mode": "COLOR",
                "showLegend": false,
                "displayHorizontal": false
              }
            },
            "id": ""
          }
        },
        {
          "width": 48,
          "height": 23,
          "widget": {
            "title": "Result: Completed rate per population",
            "xyChart": {
              "dataSets": [
                {
                  "timeSeriesQuery": {
                    "timeSeriesFilter": {
                      "filter": "metric.type=\"prometheus.googleapis.com/report_result_seconds_count/summary\" resource.type=\"prometheus_target\" metric.label.\"result\"=\"COMPLETED\"",
                      "aggregation": {
                        "alignmentPeriod": "60s",
                        "perSeriesAligner": "ALIGN_RATE",
                        "crossSeriesReducer": "REDUCE_SUM",
                        "groupByFields": [
                          "metric.label.\"population\""
                        ]
                      }
                    },
                    "unitOverride": "",
                    "outputFullDuration": false
                  },
                  "plotType": "LINE",
                  "legendTemplate": "",
                  "minAlignmentPeriod": "60s",
                  "targetAxis": "Y1",
                  "dimensions": [],
                  "measures": [],
                  "breakdowns": []
                }
              ],
              "thresholds": [],
              "yAxis": {
                "label": "",
                "scale": "LINEAR"
              },
              "chartOptions": {
                "mode": "COLOR",
                "showLegend": false,
                "displayHorizontal": false
              }
            },
            "id": ""
          }
        },
        {
          "yPos": 46,
          "width": 48,
          "height": 23,
          "widget": {
            "title": "Result: NOT_ELIGIBLE rate per population",
            "xyChart": {
              "dataSets": [
                {
                  "timeSeriesQuery": {
                    "timeSeriesFilter": {
                      "filter": "metric.type=\"prometheus.googleapis.com/report_result_seconds_count/summary\" resource.type=\"prometheus_target\" metric.label.\"result\"=\"NOT_ELIGIBLE\"",
                      "aggregation": {
                        "alignmentPeriod": "60s",
                        "perSeriesAligner": "ALIGN_RATE",
                        "crossSeriesReducer": "REDUCE_SUM",
                        "groupByFields": [
                          "metric.label.\"population\""
                        ]
                      }
                    },
                    "unitOverride": "",
                    "outputFullDuration": false
                  },
                  "plotType": "LINE",
                  "legendTemplate": "",
                  "minAlignmentPeriod": "60s",
                  "targetAxis": "Y1",
                  "dimensions": [],
                  "measures": [],
                  "breakdowns": []
                }
              ],
              "thresholds": [],
              "yAxis": {
                "label": "",
                "scale": "LINEAR"
              },
              "chartOptions": {
                "mode": "COLOR",
                "showLegend": false,
                "displayHorizontal": false
              }
            },
            "id": ""
          }
        }
      ]
    },
    "dashboardFilters": [
        {
          "filterType": "RESOURCE_LABEL",
          "labelKey": "cluster",
          "stringValue": "${var.environment}-cluster",
          "templateVariable": ""
        }
      ],
    "labels": {}
  }

EOF
}

