include: "apm_base.view"
view: apm_assigned_to_upload_completed {
  extends: [apm_base]

  dimension: created_time_epoch {
    type: number
    sql: UNIX_SECONDS(${TABLE}.CreatedTime) ;;
  }

  measure: duration_in_seconds {
    type: number
    sql:  (MAX(${created_time_epoch}) - MIN(${created_time_epoch})) ;;
  }

  measure: duration_in_minutes {
    type: number
    sql:  ${duration_in_seconds} / 60 ;;
  }

  measure: average_duration {
    type: number
    sql: ${duration_in_minutes}/${unique_sessions} ;;
  }

  measure: unique_sessions {
    type: count_distinct
    sql: ${TABLE}.SessionId ;;
  }
}

