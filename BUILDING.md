# Building Workload Images
## Pre-requisites
- [Bazel](https://bazel.build/install) 6.4.0
- (Optional) Docker
  - Required for deterministic builds

## (Optional) Building from Docker
It is recommended to build from within the provided [Dockerfile](Dockerfile). The Dockerfile provides a fixed set of dependencies to ensure a deterministic build for workload images.

```./scripts/docker/docker_sh.sh```

## Building
Images can be built using the provided script

```./scripts/build_images.sh```

The expected output for images built within the provided [Dockerfile](Dockerfile):
```
bazel-bin/shuffler/services/aggregator/aggregator_image/index.json:      "digest": "sha256:c04b35b41fc2990e47f4c6c1e575f4dc06eadfef5add6498959977b012c008d4"
bazel-bin/shuffler/services/collector/collector_image/index.json:      "digest": "sha256:4f6bef12c33bac516448d44e423ffc9a7a6d273fe9c2de327c45ec0bad74295a"
bazel-bin/shuffler/services/modelupdater/model_updater_image/index.json:      "digest": "sha256:05f73cbec7dea193ae81e51c99a3c02de6cd0e62c27e267b9a43e31e6c8d530a"
bazel-bin/shuffler/services/taskassignment/task_assignment_image/index.json:      "digest": "sha256:80439bfca81d86d69010540c26bb5e7371f7fe98d5538a1ac0813f6f8c69f35a"
bazel-bin/shuffler/services/taskbuilder/task_builder_image/index.json:      "digest": "sha256:3959610c3a6896e0f77358d94a85900b088290951f0cad53557c6298a4d86bf2"
bazel-bin/shuffler/services/taskmanagement/task_management_image/index.json:      "digest": "sha256:4f9239def6a6ee004aec5a55dbed47b255e83705250d749923d96b6d1e27b2b0"
bazel-bin/shuffler/services/taskscheduler/task_scheduler_image/index.json:      "digest": "sha256:a15ef0b318a133c7e5d6112ae458fca0f344e2646bb2094323d6d27a8c721049"
```

## Publishing
Publishing is configured via oci_push `<service>_image_publish` bazel target. For each service, the target `repository` should be configured:
- [aggregator](shuffler/services/aggregator/BUILD#L74)
- [collector](shuffler/services/collector/BUILD#L39)
- [model_updater](shuffler/services/modelupdater/BUILD#L72)
- [task_assignment](shuffler/services/taskassignment/BUILD#L40)
- [task_management](shuffler/services/taskmanagement/BUILD#L39)
- [task_scheduler](shuffler/services/taskscheduler/BUILD#L68)

The images can then be built and published:

```
"./scripts/upload_images.sh"
```

Note: Before running the above publish command, the proper repository credentials should be configured either within the docker container or mounted to the container.

## Building artifacts
Instructions on building artifacts can be found [here](python/taskbuilder/README.md)

## Deployment & Testing
GCP Infrastructure deployment instructions are provided [here](shuffler/terraform/gcp/README.md).

The server can then be reached using the [EndToEnd client](java/src/it/java/com/google/ondevicepersonalization/federatedcompute/endtoendtests/README.md).