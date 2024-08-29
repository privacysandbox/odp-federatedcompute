# Changelog

## [v0.4.0]

### Changes

- Added support for Dashboard creation for model metrics and task status.
  - [Looker project](shuffler/dashboard/looker) to generate dashboard
  - [SQL queries](shuffler/dashboard/lookerstudio) to generate charts for Looker Studio
- Added aggregation job error handling for non-retryable aggregation errors.