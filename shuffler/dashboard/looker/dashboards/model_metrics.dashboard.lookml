---
- dashboard: model_metrics
  title: Model Metrics
  layout: newspaper
  preferred_viewer: dashboards-next
  description: ''
  preferred_slug: gWQbwgdEQtKLmcauUJiCxA
  elements:
  - title: Model Metrics Chart
    name: Model Metrics Chart
    model: metric
    explore: metric
    type: looker_line
    fields: [metric.metric_value, metric.iteration_id, metric.population_task_metric]
    pivots: [metric.population_task_metric]
    sorts: [metric.population_task_metric, metric.metric_value asc 0]
    limit: 5000
    column_limit: 50
    x_axis_gridlines: false
    y_axis_gridlines: true
    show_view_names: false
    show_y_axis_labels: true
    show_y_axis_ticks: true
    y_axis_tick_density: default
    y_axis_tick_density_custom: 5
    show_x_axis_label: true
    show_x_axis_ticks: true
    y_axis_scale_mode: linear
    x_axis_reversed: false
    y_axis_reversed: false
    plot_size_by_field: false
    trellis: ''
    stacking: ''
    limit_displayed_rows: false
    legend_position: center
    point_style: none
    show_value_labels: false
    label_density: 25
    x_axis_scale: linear
    y_axis_combined: true
    show_null_points: true
    interpolation: linear
    x_axis_zoom: true
    y_axis_zoom: true
    limit_displayed_rows_values:
      show_hide: hide
      first_last: first
      num_rows: 0
    hidden_pivots: {}
    show_row_numbers: true
    transpose: false
    truncate_text: true
    hide_totals: false
    hide_row_totals: false
    size_to_fit: true
    table_theme: white
    enable_conditional_formatting: false
    header_text_alignment: left
    header_font_size: 12
    rows_font_size: 12
    conditional_formatting_include_totals: false
    conditional_formatting_include_nulls: false
    defaults_version: 1
    value_labels: legend
    label_type: labPer
    listen:
      Population Task Metric: metric.population_task_metric
    row: 0
    col: 0
    width: 24
    height: 12
  filters:
  - name: Population Task Metric
    title: Population Task Metric
    type: field_filter
    default_value: 'null'
    allow_multiple_values: true
    required: false
    ui_config:
      type: tag_list
      display: popover
    model: metric
    explore: metric
    listens_to_filters: []
    field: metric.population_task_metric
