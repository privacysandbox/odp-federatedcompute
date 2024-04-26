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
bazel-bin/shuffler/services/aggregator/aggregator_image/index.json:      "digest": "sha256:375a7525eb34a9cff17c0f21f572485a4f19318ede5dc85dd15fa8390800f203"
bazel-bin/shuffler/services/collector/collector_image/index.json:      "digest": "sha256:aa51fa96b69612eda957d2931cd04a570cf6deb9dbdff0cf87ee673b43bc6cbe"
bazel-bin/shuffler/services/modelupdater/model_updater_image/index.json:      "digest": "sha256:0c57e62a78cc112ecfa91fbb2eb6a16a484264cb65d2add500bcccbabae8e51c"
bazel-bin/shuffler/services/taskassignment/task_assignment_image/index.json:      "digest": "sha256:bcc328634272c591a5a58f3a8406d8849ecb393fe8cf0cb62091338a99e3d772"
bazel-bin/shuffler/services/taskmanagement/task_management_image/index.json:      "digest": "sha256:001060ec8a7fe40f761aa125474389021cd8404930739c8f8340240bfd21c221"
bazel-bin/shuffler/services/taskscheduler/task_scheduler_image/index.json:      "digest": "sha256:3a8a7bd66646aac0e55fbc510291f32a1e4b9b03f3e64ea72fb9ceace985750e"
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