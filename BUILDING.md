# Building Workload Images
## Pre-requisites
- [Bazel](https://bazel.build/install) 7.4.0
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
bazel-bin/shuffler/services/aggregator/aggregator_image/index.json:      "digest": "sha256:a9cdcadff8b1c5c4d4225a129428dd86b65e75be80df48c6b3aecc8b68b46e30"
bazel-bin/shuffler/services/collector/collector_image/index.json:      "digest": "sha256:d5eca9963f3ca1d722fd1d9c6f689badf1bbb9d06a020b14e21a80458b3a8b68"
bazel-bin/shuffler/services/modelupdater/model_updater_image/index.json:      "digest": "sha256:b0bc213e4cb34c99525345b1d544371ce5c9d647ec08405a9ca96d61e0b272fa"
bazel-bin/shuffler/services/taskassignment/task_assignment_image/index.json:      "digest": "sha256:6cabefa2a45c8a8240b7a813bd799ff2979ee97ff18831a083c383da114d6fee"
bazel-bin/shuffler/services/taskbuilder/task_builder_image/index.json:      "digest": "sha256:5d6424e86e07e8f38d015959a5157f662e5d229e6c901849105b798b666af38e"
bazel-bin/shuffler/services/taskmanagement/task_management_image/index.json:      "digest": "sha256:dc5405620fcc2299bbd9e6efed217ffbd46a58f0a7365e8e31b7b5d0f5698932"
bazel-bin/shuffler/services/taskscheduler/task_scheduler_image/index.json:      "digest": "sha256:9a475248ddd5f8498b300047f6a6f2955b0ccb099472869ebebbfc6795efa96b"
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
