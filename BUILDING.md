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
bazel-bin/shuffler/services/aggregator/aggregator_image/index.json:      "digest": "sha256:cdc593bc226729214d4525da592133a0e83595e92806f6fee53e372b6bf06f67"
bazel-bin/shuffler/services/collector/collector_image/index.json:      "digest": "sha256:6b9ac9ba7b7878ebbd2b107dfa15aff2c729840599df330d73a2de49e64bd6dc"
bazel-bin/shuffler/services/modelupdater/model_updater_image/index.json:      "digest": "sha256:05f73cbec7dea193ae81e51c99a3c02de6cd0e62c27e267b9a43e31e6c8d530a"
bazel-bin/shuffler/services/taskassignment/task_assignment_image/index.json:      "digest": "sha256:f33caf4e93846e8cf772a62d297b42fb2eaba60fb4d6d94d255752f652098ab3"
bazel-bin/shuffler/services/taskbuilder/task_builder_image/index.json:      "digest": "sha256:321706311e3773990aeff5074c38f8825ef2348fce75952222d5458336d42976"
bazel-bin/shuffler/services/taskmanagement/task_management_image/index.json:      "digest": "sha256:a4d8eb2ccfcc86cc2e05b73caea3c7423cedf45a6051285e5238c34a242042c5"
bazel-bin/shuffler/services/taskscheduler/task_scheduler_image/index.json:      "digest": "sha256:5f79eb45c3d52f07567cb8678c324ff9bddb502e6255b9f5b14713a6b53b6c15"
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
