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

  dimension: status_num {
    type: number
    sql: ${TABLE}.Status ;;
  }

  dimension: status {
    type: string
    sql: CASE
      WHEN ${TABLE}.Status = 0 THEN 'OPEN'
      WHEN ${TABLE}.Status = 1 THEN 'COMPLETED'
      WHEN ${TABLE}.Status = 2 THEN 'CREATED'
      WHEN ${TABLE}.Status = 101 THEN 'CANCELED'
      WHEN ${TABLE}.Status = 102 THEN 'FAILED'
      ELSE 'Unknown'
    END ;;
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
