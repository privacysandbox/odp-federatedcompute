# Building Workload Images
## Pre-requisites
- [Bazel](https://bazel.build/install) 7.3.2
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
bazel-bin/shuffler/services/aggregator/aggregator_image/index.json:      "digest": "sha256:28b60095fe040c8b230df053729e582af5fb39402ddd7e0fc8f975e4d7ca27a8"
bazel-bin/shuffler/services/collector/collector_image/index.json:      "digest": "sha256:a81847c43078be120ab0fd8041f490e714e5ab607b967d468a4b5447f3a14d58"
bazel-bin/shuffler/services/modelupdater/model_updater_image/index.json:      "digest": "sha256:55683b7f22ac97d35d489ed66e9351be4a1eb2d6aee493a50fa662009fef90bf"
bazel-bin/shuffler/services/taskassignment/task_assignment_image/index.json:      "digest": "sha256:c4c3029333bcd355dc7c861980f26df155cef50629d1c3dd952f36e1625b050b"
bazel-bin/shuffler/services/taskbuilder/task_builder_image/index.json:      "digest": "sha256:d617d293f0e85b4fa46db6b46e25c9256dd0ceda484c04b49f77c54cb7f90521"
bazel-bin/shuffler/services/taskmanagement/task_management_image/index.json:      "digest": "sha256:c6220e7c6b3a50c53a8e258f21e8dcd573508d32578e5638beeecaf69d9d95dc"
bazel-bin/shuffler/services/taskscheduler/task_scheduler_image/index.json:      "digest": "sha256:00b57b6f7fdebf96a0f65ff2f868ab132ad3cb08cbd25e390f4f6932e2b6d619"
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
