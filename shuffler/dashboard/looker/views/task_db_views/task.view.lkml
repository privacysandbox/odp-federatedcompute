view: task {
  sql_table_name: Task ;;

  dimension: population_name {
    type: string
    sql: ${TABLE}.PopulationName ;;
  }

  dimension: task_id {
    type: number
    sql: ${TABLE}.TaskId ;;
  }

  dimension: total_iteration {
    type: number
    sql: ${TABLE}.TotalIteration ;;
  }

  dimension: min_aggregation_size {
    type: number
    sql: ${TABLE}.MinAggregationSize ;;
  }

  dimension: max_aggregation_size {
    type: number
    sql: ${TABLE}.MaxAggregationSize ;;
  }

  dimension: status {
    type: number
    sql: ${TABLE}.Status ;;
  }

  dimension_group: created_time {
    type: time
    timeframes: [time, date, week, month, raw]
    sql: ${TABLE}.CreatedTime ;;
  }


  dimension: min_client_version {
    type: string
    sql: ${TABLE}.MinClientVersion ;;
  }

  dimension: max_client_version {
    type: string
    sql: ${TABLE}.MinClientVersion ;;
  }

}
