## Test Client
This EndToEnd client is used to perform a set of operations (CREATE_AND_COMPLETE_TASK, CREATE_TASK, RUN_TASK, GET_TASK) against the federated compute task assignment and management servers.

Before running, the Google Cloud credentials (https://cloud.google.com/docs/authentication/provide-credentials-adc) should be set to allow access to the related GCS resources and Task Management server.

## Operations
### CREATE_AND_COMPLETE_TRAINING_TASK
Creates a training task and runs through it until it reaches completion
```
bazel run //java/src/it/java/com/google/ondevicepersonalization/federatedcompute/endtoendtests:end_to_end_test -- --task_management_server <tm_server_url> --server <fc_server_url> --public_key_url <public_key_url>
```

### CREATE_AND_COMPLETE_EVALUATION_TASK
Creates a pair of training task and evaluation task and runs through them until they reach completion
```
bazel run //java/src/it/java/com/google/ondevicepersonalization/federatedcompute/endtoendtests:end_to_end_test -- --task_management_server <tm_server_url> --server <fc_server_url> --public_key_url <public_key_url> --operation CREATE_AND_COMPLETE_EVALUATION_TASK
```

### CREATE_TASK
Creates a task
```
bazel run //java/src/it/java/com/google/ondevicepersonalization/federatedcompute/endtoendtests:end_to_end_test -- --task_management_server <tm_server_url> --operation CREATE_TASK
```

### RUN_TASK
Runs `run_count` contributions against a given task.
```
bazel run //java/src/it/java/com/google/ondevicepersonalization/federatedcompute/endtoendtests:end_to_end_test -- --task_management_server <tm_server_url> --server <fc_server_url> --public_key_url <public_key_url> --operation RUN_TASK --population_name <popluation_name>
```

### GET_TASK
Gets a task for the given population and task id.
```
bazel run //java/src/it/java/com/google/ondevicepersonalization/federatedcompute/endtoendtests:end_to_end_test -- --task_management_server <tm_server_url> --operation GET_TASK --population_name <popluation_name> --task_id <task_id>
```

### More Info

```
bazel run //java/src/it/java/com/google/ondevicepersonalization/federatedcompute/endtoendtests:end_to_end_test -- --help
```