view: apm_base {
  sql_table_name: AssignmentStatusHistory ;;

  dimension: population_name {
    type: string
    sql: ${TABLE}.PopulationName ;;
  }

  dimension: task_id {
    type: number
    sql: ${TABLE}.TaskId ;;
  }

  dimension: status_number {
    type: number
    sql: ${TABLE}.Status ;;
  }

  dimension: status_id_number {
    type: number
    sql: ${TABLE}.StatusId ;;
  }

  dimension: status {
    type: string
    sql: CASE
        WHEN ${TABLE}.Status = 0 THEN 'ASSIGNED'
        WHEN ${TABLE}.Status = 1 THEN 'LOCAL_COMPLETED'
        WHEN ${TABLE}.Status = 2 THEN 'UPLOAD_COMPLETED'
        WHEN ${TABLE}.Status = 101 THEN 'CANCELED'
        WHEN ${TABLE}.Status = 102 THEN 'LOCAL_FAILED'
        WHEN ${TABLE}.Status = 103 THEN 'LOCAL_NOT_ELIGIBLE'
        WHEN ${TABLE}.Status = 151 THEN 'LOCAL_TIMEOUT'
        WHEN ${TABLE}.Status = 152 THEN 'UPLOAD_TIMEOUT'
        ELSE 'Unknown'
    END ;;
  }

  dimension_group: created_time_minute {
    type: time
    timeframes: [raw, minute]
    sql: TIMESTAMP_TRUNC(${TABLE}.CreatedTime, MINUTE) ;;
  }

}
