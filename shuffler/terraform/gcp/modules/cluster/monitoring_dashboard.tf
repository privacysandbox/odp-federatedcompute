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
            "title": "Result: No_Task_Available rate per population",
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
                    }
                  },
                  "plotType": "LINE",
                  "minAlignmentPeriod": "60s",
                  "targetAxis": "Y1"
                }
              ],
              "yAxis": {
                "scale": "LINEAR"
              },
              "chartOptions": {
                "mode": "COLOR"
              }
            }
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
                    }
                  },
                  "plotType": "LINE",
                  "minAlignmentPeriod": "60s",
                  "targetAxis": "Y1"
                }
              ],
              "yAxis": {
                "scale": "LINEAR"
              },
              "chartOptions": {
                "mode": "COLOR"
              }
            }
          }
        },
        {
          "yPos": 46,
          "width": 48,
          "height": 23,
          "widget": {
            "title": "Result: Outcomes per population",
            "xyChart": {
              "chartOptions": {
                "mode": "COLOR"
              },
              "dataSets": [
                {
                  "plotType": "STACKED_AREA",
                  "targetAxis": "Y1",
                  "timeSeriesQuery": {
                    "prometheusQuery": "sum by (population,result)(increase(create_task_assignment_seconds_count[60s]))"
                  }
                }
              ],
              "yAxis": {
                "label": "Count",
                "scale": "LINEAR"
              }
            }
          }
        }
      ]
    },
    "dashboardFilters": [
        {
          "filterType": "RESOURCE_LABEL",
          "labelKey": "cluster",
          "stringValue": "${var.environment}-cluster"
        }
      ]
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
                    }
                  },
                  "plotType": "LINE",
                  "minAlignmentPeriod": "60s",
                  "targetAxis": "Y1"
                }
              ],
              "yAxis": {
                "scale": "LINEAR"
              },
              "chartOptions": {
                "mode": "COLOR"
              }
            }
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
                    }
                  },
                  "plotType": "LINE",
                  "minAlignmentPeriod": "60s",
                  "targetAxis": "Y1"
                }
              ],
              "yAxis": {
                "scale": "LINEAR"
              },
              "chartOptions": {
                "mode": "COLOR"
              }
            }
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
                    }
                  },
                  "plotType": "LINE",
                  "minAlignmentPeriod": "60s",
                  "targetAxis": "Y1"
                }
              ],
              "yAxis": {
                "scale": "LINEAR"
              },
              "chartOptions": {
                "mode": "COLOR"
              }
            }
          }
        },
        {
          "yPos": 69,
          "width": 48,
          "height": 23,
          "widget": {
            "title": "Result: FAILED_EXAMPLE_GENERATION rate per population",
            "xyChart": {
              "dataSets": [
                {
                  "timeSeriesQuery": {
                    "timeSeriesFilter": {
                      "filter": "metric.type=\"prometheus.googleapis.com/report_result_seconds_count/summary\" resource.type=\"prometheus_target\" metric.label.\"result\"=\"FAILED_EXAMPLE_GENERATION\"",
                      "aggregation": {
                        "alignmentPeriod": "60s",
                        "perSeriesAligner": "ALIGN_RATE",
                        "crossSeriesReducer": "REDUCE_SUM",
                        "groupByFields": [
                          "metric.label.\"population\""
                        ]
                      }
                    }
                  },
                  "plotType": "LINE",
                  "minAlignmentPeriod": "60s",
                  "targetAxis": "Y1"
                }
              ],
              "yAxis": {
                "scale": "LINEAR"
              },
              "chartOptions": {
                "mode": "COLOR"
              }
            }
          }
        },
        {
          "yPos": 92,
          "width": 48,
          "height": 23,
          "widget": {
            "title": "Result: FAILED_MODEL_COMPUTATION rate per population",
            "xyChart": {
              "dataSets": [
                {
                  "timeSeriesQuery": {
                    "timeSeriesFilter": {
                      "filter": "metric.type=\"prometheus.googleapis.com/report_result_seconds_count/summary\" resource.type=\"prometheus_target\" metric.label.\"result\"=\"FAILED_MODEL_COMPUTATION\"",
                      "aggregation": {
                        "alignmentPeriod": "60s",
                        "perSeriesAligner": "ALIGN_RATE",
                        "crossSeriesReducer": "REDUCE_SUM",
                        "groupByFields": [
                          "metric.label.\"population\""
                        ]
                      }
                    }
                  },
                  "plotType": "LINE",
                  "minAlignmentPeriod": "60s",
                  "targetAxis": "Y1"
                }
              ],
              "yAxis": {
                "scale": "LINEAR"
              },
              "chartOptions": {
                "mode": "COLOR"
              }
            }
          }
        },
        {
          "yPos": 115,
          "width": 48,
          "height": 23,
          "widget": {
            "title": "Result: FAILED_OPS_ERROR rate per population",
            "xyChart": {
              "dataSets": [
                {
                  "timeSeriesQuery": {
                    "timeSeriesFilter": {
                      "filter": "metric.type=\"prometheus.googleapis.com/report_result_seconds_count/summary\" resource.type=\"prometheus_target\" metric.label.\"result\"=\"FAILED_OPS_ERROR\"",
                      "aggregation": {
                        "alignmentPeriod": "60s",
                        "perSeriesAligner": "ALIGN_RATE",
                        "crossSeriesReducer": "REDUCE_SUM",
                        "groupByFields": [
                          "metric.label.\"population\""
                        ]
                      }
                    }
                  },
                  "plotType": "LINE",
                  "minAlignmentPeriod": "60s",
                  "targetAxis": "Y1"
                }
              ],
              "yAxis": {
                "scale": "LINEAR"
              },
              "chartOptions": {
                "mode": "COLOR"
              }
            }
          }
        },
        {
          "yPos": 138,
          "width": 48,
          "height": 23,
          "widget": {
            "title": "Result: Outcomes per population",
            "xyChart": {
              "dataSets": [
                {
                  "timeSeriesQuery": {
                    "prometheusQuery": "sum by (population,result)(increase(report_result_seconds_count[60s]))"
                  },
                  "plotType": "STACKED_AREA",
                  "targetAxis": "Y1"
                }
              ],
              "yAxis": {
                "label": "Count",
                "scale": "LINEAR"
              },
              "chartOptions": {
                "mode": "COLOR"
              }
            }
          }
        }
      ]
    },
    "dashboardFilters": [
        {
          "filterType": "RESOURCE_LABEL",
          "labelKey": "cluster",
          "stringValue": "${var.environment}-cluster"
        }
      ]
  }

EOF
}

resource "google_monitoring_dashboard" "ta_apis_status" {
  dashboard_json = <<EOF
  {
    "displayName": "Task Assignment API Status - ${var.environment}",
    "mosaicLayout": {
      "columns": 48,
      "tiles": [
        {
          "width": 48,
          "height": 16,
          "widget": {
            "xyChart": {
              "dataSets": [
                {
                  "timeSeriesQuery": {
                    "prometheusQuery": "sum by (uri)(rate(http_server_requests_seconds_count{uri=\"/taskassignment/v1/population/{populationName}:create-task-assignment\"}[$${__interval}]))"
                  },
                  "plotType": "LINE",
                  "targetAxis": "Y1"
                }
              ],
              "yAxis": {
                "scale": "LINEAR"
              },
              "chartOptions": {
                "mode": "COLOR"
              }
            },
            "title": "CreateTaskAssignment QPS"
          }
        },
        {
          "yPos": 16,
          "width": 48,
          "height": 15,
          "widget": {
            "xyChart": {
              "dataSets": [
                {
                  "timeSeriesQuery": {
                    "timeSeriesFilter": {
                      "filter": "metric.type=\"prometheus.googleapis.com/http_server_requests_seconds/histogram\" resource.type=\"prometheus_target\" metric.label.\"uri\"=\"/taskassignment/v1/population/{populationName}:create-task-assignment\"",
                      "aggregation": {
                        "alignmentPeriod": "60s",
                        "perSeriesAligner": "ALIGN_DELTA",
                        "crossSeriesReducer": "REDUCE_PERCENTILE_50"
                      }
                    }
                  },
                  "plotType": "LINE",
                  "minAlignmentPeriod": "60s",
                  "targetAxis": "Y1"
                }
              ],
              "yAxis": {
                "scale": "LINEAR"
              },
              "chartOptions": {
                "mode": "COLOR"
              }
            },
            "title": "CreateTaskAssignment Latency 50th percentile"
          }
        },
        {
          "yPos": 31,
          "width": 48,
          "height": 15,
          "widget": {
            "xyChart": {
              "dataSets": [
                {
                  "timeSeriesQuery": {
                    "timeSeriesFilter": {
                      "filter": "metric.type=\"prometheus.googleapis.com/http_server_requests_seconds/histogram\" resource.type=\"prometheus_target\" metric.label.\"uri\"=\"/taskassignment/v1/population/{populationName}:create-task-assignment\"",
                      "aggregation": {
                        "alignmentPeriod": "60s",
                        "perSeriesAligner": "ALIGN_DELTA",
                        "crossSeriesReducer": "REDUCE_PERCENTILE_99"
                      }
                    }
                  },
                  "plotType": "LINE",
                  "minAlignmentPeriod": "60s",
                  "targetAxis": "Y1"
                }
              ],
              "yAxis": {
                "scale": "LINEAR"
              },
              "chartOptions": {
                "mode": "COLOR"
              }
            },
            "title": "CreateTaskAssignment Latency 99th percentile"
          }
        },
        {
          "yPos": 46,
          "width": 48,
          "height": 15,
          "widget": {
            "title": "ReportResult QPS",
            "xyChart": {
              "chartOptions": {
                "mode": "COLOR"
              },
              "dataSets": [
                {
                  "plotType": "LINE",
                  "targetAxis": "Y1",
                  "timeSeriesQuery": {
                    "prometheusQuery": "sum by (uri)(rate(http_server_requests_seconds_count{uri=\"/taskassignment/v1/population/{populationName}/task/{taskId}/aggregation/{aggregationId}/task-assignment/{assignmentId}:report-result\"}[$${__interval}]))"
                  }
                }
              ],
              "yAxis": {
                "scale": "LINEAR"
              }
            }
          }
        },
        {
          "yPos": 61,
          "width": 48,
          "height": 15,
          "widget": {
            "xyChart": {
              "dataSets": [
                {
                  "timeSeriesQuery": {
                    "timeSeriesFilter": {
                      "filter": "metric.type=\"prometheus.googleapis.com/http_server_requests_seconds/histogram\" resource.type=\"prometheus_target\" metric.label.\"uri\"=\"/taskassignment/v1/population/{populationName}/task/{taskId}/aggregation/{aggregationId}/task-assignment/{assignmentId}:report-result\"",
                      "aggregation": {
                        "alignmentPeriod": "60s",
                        "perSeriesAligner": "ALIGN_DELTA",
                        "crossSeriesReducer": "REDUCE_PERCENTILE_50"
                      }
                    }
                  },
                  "plotType": "LINE",
                  "minAlignmentPeriod": "60s",
                  "targetAxis": "Y1"
                }
              ],
              "yAxis": {
                "scale": "LINEAR"
              },
              "chartOptions": {
                "mode": "COLOR"
              }
            },
            "title": "ReportResult Latency 50th percentile"
          }
        },
        {
          "yPos": 76,
          "width": 48,
          "height": 15,
          "widget": {
            "xyChart": {
              "dataSets": [
                {
                  "timeSeriesQuery": {
                    "timeSeriesFilter": {
                      "filter": "metric.type=\"prometheus.googleapis.com/http_server_requests_seconds/histogram\" resource.type=\"prometheus_target\" metric.label.\"uri\"=\"/taskassignment/v1/population/{populationName}/task/{taskId}/aggregation/{aggregationId}/task-assignment/{assignmentId}:report-result\"",
                      "aggregation": {
                        "alignmentPeriod": "60s",
                        "perSeriesAligner": "ALIGN_DELTA",
                        "crossSeriesReducer": "REDUCE_PERCENTILE_99"
                      }
                    }
                  },
                  "plotType": "LINE",
                  "minAlignmentPeriod": "60s",
                  "targetAxis": "Y1"
                }
              ],
              "yAxis": {
                "scale": "LINEAR"
              },
              "chartOptions": {
                "mode": "COLOR"
              }
            },
            "title": "ReportResult Latency 99th percentile"
          }
        }
      ]
    }
  }
EOF
}