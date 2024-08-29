include: "apm_base.view"
view: apm_local_not_eligible {
  extends: [apm_base]

  filter: assigned {
    type: yesno
    sql: ${TABLE}.Status = 103 ;;
  }

  measure: count_per_minute {
    type: count_distinct
    sql: ${TABLE}.SessionId ;;
    filters: {
      field: status_number
      value: "103"
    }
  }
}
