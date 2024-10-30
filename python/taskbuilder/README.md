# Task Builder
Task builder consists of Python utilities to build tasks based on a TFF model and task configuration.

A [task](../../shuffler/proto/task.proto) encapulates instructions on how to train or evaluate the model, including training artifacts such as:

- a server-side training plan [`Plan`](https://github.com/privacysandbox/federatedcompute/blob/5a7fb889bf94f958fd7ce77292989c31c86d9adf/fcp/protos/plan.proto#L1220)
- a device-side training plan [`ClientOnlyPlan`](https://github.com/privacysandbox/federatedcompute/blob/5a7fb889bf94f958fd7ce77292989c31c86d9adf/fcp/protos/plan.proto#L1342)
- Initial model weights `checkpoint` in bytes

## Task Builder Service
The task builder service provides two APIs for different use cases:
### Build Task Group API
- **URL:** `/taskbuilder/v1:build-task-group`
- **Method:** `POST`
- **Description:** Creates tasks and artifacts based on [`SavedModel`](https://www.tensorflow.org/guide/saved_model) and [`TaskConfig`](../../shuffler/proto/task_builder.proto#L237) input in [BuildTaskRequest](../../shuffler/proto/task_builder.proto#L80).
- **Request:** The request body should contain binary data in protobuf format (`application/x-protobuf`) of [BuildTaskRequest](../../shuffler/proto/task_builder.proto#L80)
- **Response:** The API returns responses in protobuf format (`application/x-protobuf`) of [BuildTaskResponse](../../shuffler/proto/task_builder.proto#L95)

### Build Artifacts API
- **URL:** `/taskbuilder/v1:build-artifacts`
- **Method:** `POST`
- **Description:** Creates artifacts only without tasks.
- Request and response align with data format of `:build-task-group`.
- [`ArtifactBuilding`](../../shuffler/proto/task_builder.proto#L189) has to be set in [`TaskConfig`](../../shuffler/proto/task_builder.proto#L237) of the request, which point to GCS paths where created artifacts should to uploaded to.

### Start a local Task Builder Service for Test
`bazel run //python/taskbuilder:task_builder`

## Task Builder Client
For early testing, we provide a [simple client](task_builder_client.py) that interacts with task builder services.

### Workflow 1: create tasks and artifacts together
Run the Python client: `bazel run //python/taskbuilder:task_builder_client -- --saved_model=gs://<resource_uri> --task_config=gs://<resource_uri> --task_builder_server=https://<task_builder_server>`
- `--saved_model`: a GCS path to a TensorFlow [`SavedModel`](https://www.tensorflow.org/guide/saved_model). You can find a sample [here](sample/input/mnist_model)
- `--task_config`: a GCS path to a [`TaskConfig`](../../shuffler/proto/task_builder.proto#L237) in text protobuf. You can find a sample [here](sample/input/mnist_cnn_task_config.pbtxt)
- `--task_builder_server`: the task builder server endpoint. Local host (`http://localhost:5000`) will be used as default if not provided.
- `--population_name`: an updated population name for the task. If provided, the original [`population_name` in `TaskConfig`](../../shuffler/proto/task_builder.proto#L239) will be overriden. This is helpful if you want to reuse the same `--task_config` but with a different population name for testing purpose.
- `--impersonate_service_account`: a service account to impersonate which has permission to access the task builder server endpoint.
- `--api_key`: an API key which has permission to access the task builder API.

### Workflow 2: create artifacts only
Run the Python client: `bazel run //python/taskbuilder:task_builder_client -- --saved_model=gs://<resource_uri> --task_config=gs://<resource_uri> --task_builder_server=https://<task_builder_server> --build_artifact_only=true`
- `--build_artifact_only`: the option to skip task creation and build artifacts only. You can find a sample [here](sample/input/mnist_cnn_task_config_build_artifact_only.pbtxt)
- `--skip_flex_ops_check`: the option to skip flex ops check in Android TensorflowLite library.
- All other options are same as above. The generated task will uploaded to the GCS path you specified [here](sample/input/mnist_cnn_task_config_build_artifact_only.pbtxt#L32). You can find some output samples [here](sample/output)
