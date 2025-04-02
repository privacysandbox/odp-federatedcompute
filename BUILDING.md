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
bazel-bin/shuffler/services/collector/collector_image/index.json:      "digest": "sha256:51e50b4984af0f06143f92d62e70ca2d68af50747d26649d0e492f0070765440"
bazel-bin/shuffler/services/modelupdater/model_updater_image/index.json:      "digest": "sha256:b0bc213e4cb34c99525345b1d544371ce5c9d647ec08405a9ca96d61e0b272fa"
bazel-bin/shuffler/services/taskassignment/task_assignment_image/index.json:      "digest": "sha256:6b5d42272b07a62dd42eca1f7d36f2da4a4b483cafef142c607a7002abaa0cda"
bazel-bin/shuffler/services/taskbuilder/task_builder_image/index.json:      "digest": "sha256:30d687c11d143efa2ad48d1080f5b5d6f7eed7d119501304ab95c19026cb8bc1"
bazel-bin/shuffler/services/taskmanagement/task_management_image/index.json:      "digest": "sha256:feff80bace0d49effe07e0277e56a90a2d958b06859414988e82239023957a14"
bazel-bin/shuffler/services/taskscheduler/task_scheduler_image/index.json:      "digest": "sha256:d66aa68a48386243fac930d1d583ea00d693ff4f07e175c66fa021e70615d3f1"
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

## Android Device E2E
A tutorial on running a full E2E with an Android device is provided [here](https://github.com/privacysandbox/ondevicepersonalization/OdpSamples/federated-learning.md).
