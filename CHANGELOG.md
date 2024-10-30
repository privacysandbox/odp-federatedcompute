# Changelog

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