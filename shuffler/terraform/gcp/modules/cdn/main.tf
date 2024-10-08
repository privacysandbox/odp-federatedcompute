/**
 * Copyright 2024 Google LLC
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

terraform {
  required_providers {
    toggles = {
      source  = "reinoudk/toggles"
      version = "0.3.0"
    }
  }
}

locals {
  domain = "cdn.${var.parent_domain_name}"
}

data "google_project" "project" {
  project_id = var.project_id
}

# reserve IP address
resource "google_compute_global_address" "default" {
  name = "cdn-${var.environment}-ip"
}

# backend bucket with CDN policy with default ttl settings
resource "google_compute_backend_bucket" "default" {
  name        = "cdn-${var.environment}-backend-bucket"
  bucket_name = var.backend_bucket_name
  enable_cdn  = true
  cdn_policy {
    cache_mode                   = "CACHE_ALL_STATIC"
    signed_url_cache_max_age_sec = 3600
    client_ttl                   = 3600
    default_ttl                  = 3600
    max_ttl                      = 86400
    negative_caching             = true
    serve_while_stale            = 86400
  }
}

# url map
resource "google_compute_url_map" "default" {
  name            = "${var.environment}-https-cdn-lb"
  default_service = google_compute_backend_bucket.default.id
}

data "google_dns_managed_zone" "dns_zone" {
  name = replace(var.parent_domain_name, ".", "-")
}

# Add A record for loadbalancer IPs.
resource "google_dns_record_set" "a" {
  # Name must end with period
  name         = "${local.domain}."
  managed_zone = data.google_dns_managed_zone.dns_zone.name
  type         = "A"
  ttl          = 300

  rrdatas = [google_compute_global_address.default.address]
}

resource "google_compute_managed_ssl_certificate" "default" {
  name     = "${var.environment}-cdn-cert"
  provider = google
  managed {
    domains = [local.domain]
  }
  depends_on = [google_dns_record_set.a]
}

# https proxy
resource "google_compute_target_https_proxy" "default" {
  name             = "${var.environment}-https-cdn-lb-proxy"
  url_map          = google_compute_url_map.default.id
  ssl_certificates = [google_compute_managed_ssl_certificate.default.id]
}

# forwarding rule
resource "google_compute_global_forwarding_rule" "default" {
  name                  = "${var.environment}-http-lb-forwarding-rule"
  ip_protocol           = "TCP"
  load_balancing_scheme = "EXTERNAL"
  port_range            = "443"
  target                = google_compute_target_https_proxy.default.id
  ip_address            = google_compute_global_address.default.id
}

resource "time_rotating" "toggle_interval" {
  rotation_days = 90
}

resource "toggles_leapfrog" "toggle" {
  // Maintain 2 active keys at a time. Use the newest one and replace the oldest one.
  provider = toggles
  trigger  = time_rotating.toggle_interval.rotation_rfc3339
}

resource "random_bytes" "url_signature_a" {
  length = 16
  keepers = {
    alpha = toggles_leapfrog.toggle.alpha_timestamp
  }
}

resource "google_compute_backend_bucket_signed_url_key" "backend_key_a" {
  name           = "${var.environment}-signed-url-key-a"
  key_value      = replace(replace(random_bytes.url_signature_a.base64, "+", "-"), "/", "_")
  backend_bucket = google_compute_backend_bucket.default.name
}

resource "random_bytes" "url_signature_b" {
  length = 16
  keepers = {
    beta = toggles_leapfrog.toggle.beta_timestamp
  }
}

resource "google_compute_backend_bucket_signed_url_key" "backend_key_b" {
  name           = "${var.environment}-signed-url-key-b"
  key_value      = replace(replace(random_bytes.url_signature_b.base64, "+", "-"), "/", "_")
  backend_bucket = google_compute_backend_bucket.default.name
}

resource "google_storage_bucket_iam_binding" "binding" {
  bucket = var.backend_bucket_name
  role   = "roles/storage.objectViewer"
  members = [
    "serviceAccount:service-${data.google_project.project.number}@cloud-cdn-fill.iam.gserviceaccount.com",
  ]
  depends_on = [google_compute_backend_bucket_signed_url_key.backend_key_a, google_compute_backend_bucket_signed_url_key.backend_key_b]
}