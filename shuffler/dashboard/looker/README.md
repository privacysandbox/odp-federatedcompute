# Looker (Google Cloud core) Dashboard

This directory contains the [Looker](https://cloud.google.com/looker/docs) dashboards for odp-federatedcompute.

## Instructions

1. [Create a Looker instance](https://cloud.google.com/looker/docs/looker-core-create-oauth)
2. [Setup database connections](https://cloud.google.com/looker/docs/looker-core-dialects#set_up_a_database_connection) for the fcp-task-db-\<environment> and fcp-metrics-db-\<environment> Spanner databases
3. Create a new GitHub repository and copy the [current folder](.) into the new repository.
4. Update the [task_db](models/task.model.lkml#L1) and [model_db](models/metric.model.lkml#L1) database connection names to the created database connections.
5. [Connect to the GitHub repository to the Looker instance](https://cloud.google.com/looker/docs/setting-up-git-connection#integrating-with-git).
6. [Import the Looker project from the GitHub repository](https://cloud.google.com/looker/docs/setting-up-git-connection#connecting_a_new_lookml_project_to_a_non-empty_git_repository)
7. Two new Looker dashboards "Model Metrics" and "Task Status" will be created.