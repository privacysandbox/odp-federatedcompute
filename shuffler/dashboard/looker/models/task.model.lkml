connection: "task_db"

# include all the views
include: "/views/task_db_views/*.view.lkml"
include: "/dashboards/*.dashboard.lookml"

datagroup: fcp_default_datagroup {
  # sql_trigger: SELECT MAX(id) FROM etl_log;;
  max_cache_age: "1 hour"
}

persist_with: fcp_default_datagroup

explore: apm_assigned {
  sql_always_where: Status = 0 ;;
}

explore: apm_local_completed {
  sql_always_where: Status = 1 ;;
}

explore: apm_upload_completed {
  sql_always_where: Status = 2 ;;
}

explore: apm_canceled {
  sql_always_where: Status = 101 ;;
}

explore: apm_local_failed {
  sql_always_where: Status = 102 ;;
}

explore: apm_local_not_eligible {
  sql_always_where: Status = 103 ;;
}

explore: apm_remote_failed{
  sql_always_where: Status = 104 ;;
}

explore: apm_local_failed_example_generation {
  sql_always_where: Status = 105 ;;
}

explore: apm_local_failed_model_computation {
  sql_always_where: Status = 106 ;;
}

explore: apm_local_failed_ops_error {
  sql_always_where: Status = 107 ;;
}

explore: apm_local_timeout {
  sql_always_where: Status = 151 ;;
}

explore: apm_upload_timeout {
  sql_always_where: Status = 152 ;;
}

explore: apm_assigned_to_upload_completed{
  sql_always_where: apm_assigned_to_upload_completed.Status = 0 or apm_assigned_to_upload_completed.Status = 2 ;;
  join: iteration_completion {
    type: inner
    relationship: one_to_one
    sql_on: ${apm_assigned_to_upload_completed.population_name} = ${iteration_completion.population_name} AND ${apm_assigned_to_upload_completed.task_id} = ${iteration_completion.task_id} AND ${apm_assigned_to_upload_completed.iteration_id} = ${iteration_completion.iteration_id};;
  }
}

explore: task {
}

explore: iteration{
  join: iteration_open {
    type: inner
    relationship: one_to_one
    sql_on: ${iteration.population_name} = ${iteration_open.population_name} AND ${iteration.task_id} = ${iteration_open.task_id} AND ${iteration.iteration_id} = ${iteration_open.iteration_id} AND ${iteration_open.status} = 0;;
  }
}

explore: iteration_open {
  sql_always_where: Status = 0;;
}

explore: iteration_completion{
  sql_always_where: Status = 0 or Status = 2 or Status = 50;;
}

explore: iteration_aggregating_to_complete{
  sql_always_where: Status = 1 or Status = 2 or Status = 50;;
}
