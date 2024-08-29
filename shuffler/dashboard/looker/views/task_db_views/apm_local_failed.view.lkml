include: "apm_base.view"
view: apm_local_failed {
  extends: [apm_base]

  filter: assigned {
    type: yesno
    sql: ${TABLE}.Status = 102 ;;
  }

  measure: count_per_minute {
    type: count_distinct
    sql: ${TABLE}.SessionId ;;
    filters: {
      field: status_number
      value: "102"
    }
  }
}
