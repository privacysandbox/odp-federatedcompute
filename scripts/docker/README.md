# Docker Compose

Services can be deployed locally using [Docker Compose](https://docs.docker.com/compose/).

## Pre-requisites

The local services will depend on GCP resources. The `environment` and `ENCRYPTION_OPTS` should be set in the [docker-compose.yml](docker-compose.yml) for each service to reference the deployed resources.

A resource-only stack can be deployed using [resources-only/shuffler.tf](../../shuffler/terraform/gcp/environments/resources-only/shuffler.tf). If using an existing deployment, note that there may be contention of resources between locally running services and cloud deployed services.

A key-service coordinator will be required for local deployment of the Aggregator and ModelUpdater. The key-service coordinator should allowlist the service account used by locally run services.

## Running Locally

To run using docker-compose, from the repository root run:

Set required environment variables:
```
# Set GOOGLE_CLOUD_PROJECT
export GOOGLE_CLOUD_PROJECT=<project-id>

# Set GOOGLE_APPLICATION_CREDENTIALS
export GOOGLE_APPLICATION_CREDENTIALS=<application_default_credentials.json>
```

To run all services:
```
./scripts/docker/run_all_services_docker.sh
```

To run individual services include a space-separated list of services to run:
```
./scripts/docker/run_all_services_docker.sh aggregator collector
```

The local server can then be reached using the [EndToEnd client](../../java/src/it/java/com/google/ondevicepersonalization/federatedcompute/endtoendtests/README.md).

```
bazel run java/src/it/java/com/google/ondevicepersonalization/federatedcompute/endtoendtests:end_to_end_test -- --server http://localhost:8080 --public_key_url <public-key-service> --task_management_server http://localhost:8080
```