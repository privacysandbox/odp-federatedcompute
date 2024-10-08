include: "apm_base.view"
view: apm_upload_completed {
  extends: [apm_base]

 filter: assigned {
    type: yesno
    sql: ${TABLE}.Status = 2;;
 }

  measure: count_per_minute {
    type: count_distinct
    sql: ${TABLE}.SessionId ;;
    filters: [
      status_number: "2",
      status_id_number: "3"
    ]
  }
}
