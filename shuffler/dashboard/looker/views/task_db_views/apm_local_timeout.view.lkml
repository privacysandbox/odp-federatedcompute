include: "apm_base.view"
view: apm_local_timeout {
  extends: [apm_base]

  filter: assigned {
    type: yesno
    sql: ${TABLE}.Status = 151 ;;
  }

  measure: count_per_minute {
    type: count_distinct
    sql: ${TABLE}.SessionId ;;
    filters: {
      field: status_number
      value: "151"
    }
  }
}
