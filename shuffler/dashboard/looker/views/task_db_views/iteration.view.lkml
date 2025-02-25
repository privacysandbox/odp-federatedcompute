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

  dimension: status_num {
    type: number
    sql: ${TABLE}.Status ;;
  }

  dimension: status {
    type: string
    sql: CASE
      WHEN ${TABLE}.Status = 0 THEN 'COLLECTING'
      WHEN ${TABLE}.Status = 1 THEN 'AGGREGATING'
      WHEN ${TABLE}.Status = 2 OR ${TABLE}.Status = 50 THEN 'COMPLETED'
      WHEN ${TABLE}.Status = 4 THEN 'APPLYING'
      WHEN ${TABLE}.Status = 5 OR ${TABLE}.Status = 51 THEN 'POST_PROCESSED'
      WHEN ${TABLE}.Status = 101 THEN 'CANCELED'
      WHEN ${TABLE}.Status = 102 THEN 'AGGREGATING_FAILED'
      WHEN ${TABLE}.Status = 103 THEN 'APPLYING_FAILED'
      ELSE 'Unknown'
    END ;;
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
