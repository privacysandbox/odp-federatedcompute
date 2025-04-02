# Changelog

## [v0.7.1]

- Fixed api_key based auth in TaskBuilder Client to no longer attempt to generate a Bearer token.
- Small cleanup and fixes to terraform and taskbuilder.
- Additional logging added to Collector.
- Add pointer to [OnDevicePersonalization E2E tutorial](BUILDING.md#L53).
- Add new [Keras model sample](python/taskbuilder/sample/keras) for Python TaskBuilder
- Update [Java sample artifacts](java/src/it/java/com/google/ondevicepersonalization/federatedcompute/endtoendtests/resources) to use the Keras model from TaskBuilder [sample](python/taskbuilder/sample/keras).

## [v0.7.0]

### Changes

- Moved IterationEntity COMPLETED and POST_PROCESSED states from 2 and 5 to 50 and 51 respectively. **This is not a backwards compatible change. We recommend either starting from a clean Spanner database or for migration updating Status within the Iteration table from 2 and 5 to 50 and 51 respectively.**
  - ```UPDATE Iteration SET Status=50 WHERE Status=2; UPDATE Iteration SET Status=51 WHERE Status=5;```
- Add more granular rejection info for CreateTaskAssignment rejections.
- Added flag to Aggregator to disable output encryption for testing without coordinators.
  - To enable add: `--should_encrypt_aggregator_output 'false'` to the [input params of the aggregator](shuffler/services/aggregator/BUILD#L42)
  - Resulting line should look like: `    env = {"ENCRYPTION_OPTS": "--public_key_service_base_url 'https://publickeyservice.odp.gcp.privacysandboxservices.com/.well-known/odp/v1/public-keys' --should_encrypt_aggregator_output 'false'"},`
- Add support for [rules_distroless](https://github.com/GoogleContainerTools/rules_distroless) to build images
- Add [cloudbuild.yaml](cloudbuild.yaml) to support building images remotely with GCP Cloud Build
- Added flag to enable/disable success notifications in the aggregator.
- Updated python requirements and bazel version

## [v0.6.0]

### Changes

- Moved public key url for aggregator into image environment from terraform configuration
- Upgrade to Bazel 7.3.2 and Spring 3.3.4
- Updated base java image to `gcr.io/distroless/java17-debian11:nonroot`
- Minor updates to terraform configuration for PubSub and Confidential Space
- Added API Gateway support to TaskBuilder to support API key-based auth
- Minor Looker dashboard improvements

## [v0.5.0]

### Changes

- Added support for Cloud CDN in GCP terraform.
- Added several GCP alarm policies in GCP terraform.
- Additional charts added to Looker project dashboard.
- Upgrade from Apache http4 to http5 client for KAVS and notification clients.
- Update GCP Confidential Space images and add support for in-memory tree parallelism for V1 tensorflow aggregation.

## [v0.4.0]

### Changes

- Added support for Dashboard creation for model metrics and task status.
  - [Looker project](shuffler/dashboard/looker) to generate dashboard
  - [SQL queries](shuffler/dashboard/lookerstudio) to generate charts for Looker Studio
- Added aggregation job error handling for non-retryable aggregation errors.