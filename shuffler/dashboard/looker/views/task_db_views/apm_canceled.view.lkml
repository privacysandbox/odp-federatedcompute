include: "apm_base.view"
view: apm_canceled {
  extends: [apm_base]

  filter: assigned {
    type: yesno
    sql: ${TABLE}.Status = 101 ;;
  }

  measure: count_per_minute {
    type: count_distinct
    sql: ${TABLE}.SessionId ;;
    filters: {
      field: status_number
      value: "101"
    }
  }
}
