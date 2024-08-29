view: iteration_status_base {
  sql_table_name: iteration_status_history ;;

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

  dimension: created_time_epoch {
    type: number
    sql: UNIX_SECONDS(${TABLE}.CreatedTime) ;;
  }

  dimension_group: created_time {
    type: time
    timeframes: [time, date, week, month, raw]
    sql: ${TABLE}.CreatedTime ;;
  }

  dimension: duration_in_minutes {
    type: number
    sql:  (MAX(${created_time_epoch}) - MIN(${created_time_epoch})) / 60 ;;
  }

  measure: min_created_time {
    type: min
    sql: ${created_time_epoch} ;;
  }

}
