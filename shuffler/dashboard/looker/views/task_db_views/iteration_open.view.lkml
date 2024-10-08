view: iteration_open {
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

  dimension: status {
    type: number
    sql:  ${TABLE}.Status ;;
  }

  dimension_group: created_time {
    type: time
    timeframes: [time, date, week, month, minute, raw]
    sql: ${TABLE}.CreatedTime ;;
  }
}
