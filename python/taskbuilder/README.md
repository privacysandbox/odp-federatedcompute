# Task Builder
Task builder consists of Python utilities to build tasks based on a TFF model and task configuration.

A [task](../../shuffler/proto/task.proto) encapulates instructions on how to train or evaluate the model, including training artifacts such as:

- a server-side training plan [`Plan`](https://github.com/privacysandbox/federatedcompute/blob/5a7fb889bf94f958fd7ce77292989c31c86d9adf/fcp/protos/plan.proto#L1220)
- a device-side training plan [`ClientOnlyPlan`](https://github.com/privacysandbox/federatedcompute/blob/5a7fb889bf94f958fd7ce77292989c31c86d9adf/fcp/protos/plan.proto#L1342)
- Initial model weights `checkpoint` in bytes

## Local Task Builder Client
For early experiments, we provide a [Python client](task_builder_client.py) that run the task building workflows locally, until a more secure cloud service of task builder is ready. The task builder client provides the following workflows based on different use cases.

### Workflow 1: create tasks and artifacts together
Run the Python client: `bazel run //python/taskbuilder:task_builder_client -- --saved_model=gs://<resource_uri> --task_config=gs://<resource_uri> --task_management_server=https://<tm_server> --gcp_service_account=<service_account>`
- `--saved_model`: a GCS path to a TensorFlow [`SavedModel`](https://www.tensorflow.org/guide/saved_model)
- `--task-config`: a GCS path to a [`TaskConfig`](../../shuffler/proto/task_builder.proto#L217) in text protobuf.
- `--task_management_server`: the task management server that create tasks.
- `--gcp_service_account`: a GCP service account to impersonate for TM service authorization.
- Note that you must have [`Service Account Token Creator`](https://cloud.google.com/docs/authentication/use-service-account-impersonation#required-roles) permission to impersonate the provided service account.
- For local testing, since authorization is not required, `--gcp_service_account` is ignored.

### Workflow 2: create artifacts only
Run the Python client: `bazel run //python/taskbuilder:task_builder_client -- --saved_model=gs://<resource_uri> --task_config=gs://<resource_uri> --build_artifact_only=true`
- `--build_artifact_only`: the option to skip task creation and build artifacts only. If the flag is true, [`ArtifactBuilding`](../../shuffler/proto/task_builder.proto#L170) has to be set in [`TaskConfig`](../../shuffler/proto/task_builder.proto#L217), which point to GCS paths where created artifacts should to uploaded to.
- `--saved_model` and `--task-config` are same as above.
- Since tasks are not created, options in `--task_management_server` and `--gcp_service_account` are ignored.

For all workflows, `--google_cloud_project` can be set to switch from the default GCP project.