include: "apm_base.view"
view: apm_assigned {
  extends: [apm_base]

  filter: assigned {
    type: yesno
    sql: ${TABLE}.Status = 0 ;;
  }

  measure: count_per_minute {
    type: count_distinct
    sql: ${TABLE}.SessionId ;;
    filters: {
      field: status_number
      value: "0"
    }
  }
}
