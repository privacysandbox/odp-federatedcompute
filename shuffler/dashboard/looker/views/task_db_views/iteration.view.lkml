view: iteration {
  sql_table_name: Iteration ;;

  dimension: population_name {
    type: string
    sql: ${TABLE}.PopulationName ;;
  }

  dimension: task_id {
    type: number
    sql: ${TABLE}.TaskId ;;
  }

  dimension: iteration_id {
    type: number
    sql: ${TABLE}.IterationId ;;
  }

  dimension: attempt_id {
    type: number
    sql: ${TABLE}.AttemptId ;;
  }

  dimension: report_goal {
    type: number
    sql: ${TABLE}.ReportGoal ;;
  }

  dimension: max_aggregation_size {
    type: number
    sql: ${TABLE}.MaxAggregationSize ;;
  }

  dimension: status {
    type: number
    sql: ${TABLE}.Status ;;
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
