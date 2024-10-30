/**
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
data "google_project" "project" {}

resource "google_pubsub_topic" "aggregator-topic" {
  name     = "aggregator-${var.environment}-topic"
  provider = google
}

resource "google_pubsub_topic" "aggregator-dead-letter" {
  name     = "aggregator-${var.environment}-topic-dead-letter"
  provider = google
}

resource "google_pubsub_subscription" "aggregator-subscription" {
  name  = "aggregator-${var.environment}-subscription"
  topic = google_pubsub_topic.aggregator-topic.name

  # 7 days
  message_retention_duration = "604800s"
  retain_acked_messages      = true

  ack_deadline_seconds = 10

  expiration_policy {
    # Dont expire
    ttl = ""
  }

  dead_letter_policy {
    dead_letter_topic     = google_pubsub_topic.aggregator-dead-letter.id
    max_delivery_attempts = 10
  }

  enable_exactly_once_delivery = var.enable_exactly_once_delivery
  provider                     = google
}

resource "google_pubsub_subscription" "aggregator-dlq-subscription" {
  name  = "aggregator-dlq-${var.environment}-subscription"
  topic = google_pubsub_topic.aggregator-dead-letter.name

  # 7 days
  message_retention_duration = "604800s"
  retain_acked_messages      = true

  ack_deadline_seconds = 10

  expiration_policy {
    # Dont expire
    ttl = ""
  }

  enable_exactly_once_delivery = var.enable_exactly_once_delivery
  provider                     = google
}

resource "google_pubsub_topic" "aggregator-notifications-topic" {
  name     = "aggregator-notifications-${var.environment}-topic"
  provider = google
}

resource "google_pubsub_topic" "aggregator-notifications-dead-letter" {
  name     = "aggregator-notifications-${var.environment}-topic-dead-letter"
  provider = google
}

resource "google_pubsub_subscription" "aggregator-notifications-subscription" {
  name  = "aggregator-notifications-${var.environment}-subscription"
  topic = google_pubsub_topic.aggregator-notifications-topic.name

  # 7 days
  message_retention_duration = "604800s"
  retain_acked_messages      = true

  ack_deadline_seconds = 10

  expiration_policy {
    # Dont expire
    ttl = ""
  }

  dead_letter_policy {
    dead_letter_topic     = google_pubsub_topic.aggregator-notifications-dead-letter.id
    max_delivery_attempts = 10
  }

  enable_exactly_once_delivery = var.enable_exactly_once_delivery
  provider                     = google
}

resource "google_pubsub_subscription" "aggregator-notifications-dlq-subscription" {
  name  = "aggregator-notifications-dlq-${var.environment}-subscription"
  topic = google_pubsub_topic.aggregator-notifications-dead-letter.name

  # 7 days
  message_retention_duration = "604800s"
  retain_acked_messages      = true

  ack_deadline_seconds = 10

  expiration_policy {
    # Dont expire
    ttl = ""
  }

  enable_exactly_once_delivery = var.enable_exactly_once_delivery
  provider                     = google
}

resource "google_pubsub_topic" "model-updater-topic" {
  name     = "model-updater-${var.environment}-topic"
  provider = google
}

resource "google_pubsub_topic" "model-updater-dead-letter" {
  name     = "model-updater-${var.environment}-topic-dead-letter"
  provider = google
}

resource "google_pubsub_subscription" "model-updater-subscription" {
  name  = "model-updater-${var.environment}-subscription"
  topic = google_pubsub_topic.model-updater-topic.name

  # 7 days
  message_retention_duration = "604800s"
  retain_acked_messages      = true

  ack_deadline_seconds = 10

  expiration_policy {
    # Dont expire
    ttl = ""
  }

  dead_letter_policy {
    dead_letter_topic     = google_pubsub_topic.model-updater-dead-letter.id
    max_delivery_attempts = 10
  }

  enable_exactly_once_delivery = var.enable_exactly_once_delivery
  provider                     = google
}

resource "google_pubsub_subscription" "model-updater-dlq-subscription" {
  name  = "model-updater-dlq-${var.environment}-subscription"
  topic = google_pubsub_topic.model-updater-dead-letter.name

  # 7 days
  message_retention_duration = "604800s"
  retain_acked_messages      = true

  ack_deadline_seconds = 10

  expiration_policy {
    # Dont expire
    ttl = ""
  }

  enable_exactly_once_delivery = var.enable_exactly_once_delivery
  provider                     = google
}

resource "google_pubsub_topic_iam_member" "pubsub_sa_publish_aggregator_deadletter_topic" {
  topic  = google_pubsub_topic.aggregator-dead-letter.name
  role   = "roles/pubsub.publisher"
  member = "serviceAccount:service-${data.google_project.project.number}@gcp-sa-pubsub.iam.gserviceaccount.com"
}

resource "google_pubsub_subscription_iam_member" "pubsub_sa_pull_aggregator_topic_sub" {
  subscription = google_pubsub_subscription.aggregator-subscription.name
  role         = "roles/pubsub.subscriber"
  member       = "serviceAccount:service-${data.google_project.project.number}@gcp-sa-pubsub.iam.gserviceaccount.com"
}

resource "google_pubsub_topic_iam_member" "pubsub_sa_publish_aggregator_notifications_deadletter_topic" {
  topic  = google_pubsub_topic.aggregator-notifications-dead-letter.name
  role   = "roles/pubsub.publisher"
  member = "serviceAccount:service-${data.google_project.project.number}@gcp-sa-pubsub.iam.gserviceaccount.com"
}

resource "google_pubsub_subscription_iam_member" "pubsub_sa_pull_aggregator_notifications_topic_sub" {
  subscription = google_pubsub_subscription.aggregator-notifications-subscription.name
  role         = "roles/pubsub.subscriber"
  member       = "serviceAccount:service-${data.google_project.project.number}@gcp-sa-pubsub.iam.gserviceaccount.com"
}

resource "google_pubsub_topic_iam_member" "pubsub_sa_publish_model_updater_deadletter_topic" {
  topic  = google_pubsub_topic.model-updater-dead-letter.name
  role   = "roles/pubsub.publisher"
  member = "serviceAccount:service-${data.google_project.project.number}@gcp-sa-pubsub.iam.gserviceaccount.com"
}

resource "google_pubsub_subscription_iam_member" "pubsub_sa_pull_model_updater_topic_sub" {
  subscription = google_pubsub_subscription.model-updater-subscription.name
  role         = "roles/pubsub.subscriber"
  member       = "serviceAccount:service-${data.google_project.project.number}@gcp-sa-pubsub.iam.gserviceaccount.com"
}