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
bazel-bin/shuffler/services/aggregator/aggregator_image/index.json:      "digest": "sha256:a8a66a800c23865604e9ab816e6c3fc24c29bbed34187b5e21c93819f3594533"
bazel-bin/shuffler/services/collector/collector_image/index.json:      "digest": "sha256:7be6b26c12d678614e6e92fc225985198afcfae6293abf84ddac7c6db217de25"
bazel-bin/shuffler/services/modelupdater/model_updater_image/index.json:      "digest": "sha256:9bc980a096865f4dffcfd44e956441a1be72319f214c1bb0198f5441e9e619db"
bazel-bin/shuffler/services/taskassignment/task_assignment_image/index.json:      "digest": "sha256:9169c5bc235022972f9cd1ce370716397314f1dcdd67b605426f99f9a0f46c54"
bazel-bin/shuffler/services/taskbuilder/task_builder_image/index.json:      "digest": "sha256:87dcaf4adeb728d67ba73c29e2997aac318335f172f9484986dbb45154ddd398"
bazel-bin/shuffler/services/taskmanagement/task_management_image/index.json:      "digest": "sha256:b00553e6648ded8fde0d773be566333dbb3c5eaef06074e54f1c7adea1e82962"
bazel-bin/shuffler/services/taskscheduler/task_scheduler_image/index.json:      "digest": "sha256:8f9669d23f8a76f6b22303016accd36809559218562289a2c4c0dfbadf36fb1a"
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
