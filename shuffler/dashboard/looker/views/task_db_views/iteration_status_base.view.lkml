view: iteration_status_base {
  sql_table_name: IterationStatusHistory ;;

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
    timeframes: [time, date, week, month, minute, second, raw]
    sql: ${TABLE}.CreatedTime ;;
  }

  measure: duration_in_seconds {
    type: number
    sql:  (MAX(${created_time_epoch}) - MIN(${created_time_epoch})) ;;
  }

  measure: duration_in_minutes {
    type: number
    sql:  ${duration_in_seconds} / 60 ;;
  }

  measure: min_created_time {
    type: min
    sql: ${created_time_epoch} ;;
  }

}
