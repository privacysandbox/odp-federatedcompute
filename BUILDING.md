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
bazel-bin/shuffler/services/aggregator/aggregator_image/index.json:      "digest": "sha256:47d794ec4653a135d434e5e5095145c8f91442d0f3fde972fdc367b8aaf13703"
bazel-bin/shuffler/services/collector/collector_image/index.json:      "digest": "sha256:08ba57f255f6c6b19cd986aa2b643cfb571fdeb508824215e661798053f39180"
bazel-bin/shuffler/services/modelupdater/model_updater_image/index.json:      "digest": "sha256:9f6fb2566681f57aa7ddb30f06ca0086eccfa871e1bae7fa8952611b7c7ddc41"
bazel-bin/shuffler/services/taskassignment/task_assignment_image/index.json:      "digest": "sha256:a626d7b680cf5314b663a879a22ebd58799cc0b7b902bda400f158e9153f2843"
bazel-bin/shuffler/services/taskbuilder/task_builder_image/index.json:      "digest": "sha256:cee0f537b9332a35c9044aec9497a31cc5c9ad3dfdd5230435d7b76b2b43ffcd"
bazel-bin/shuffler/services/taskmanagement/task_management_image/index.json:      "digest": "sha256:516a9311d1de4466401975cde870b6f2acf4a49e05c4d63f988a7ea2c2610803"
bazel-bin/shuffler/services/taskscheduler/task_scheduler_image/index.json:      "digest": "sha256:cacfb2b70d7553147f3ec4dff2c7d275281e354c87155e66cabf822d2e81d34b"
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
