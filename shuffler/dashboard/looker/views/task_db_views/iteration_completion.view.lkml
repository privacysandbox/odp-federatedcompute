view: iteration_completion {
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

  measure: duration_in_minutes {
    type: number
    sql: ((MAX(UNIX_SECONDS(${TABLE}.CreatedTime)) - MIN(UNIX_SECONDS(${TABLE}.CreatedTime)))) / 60 ;;
  }
}
