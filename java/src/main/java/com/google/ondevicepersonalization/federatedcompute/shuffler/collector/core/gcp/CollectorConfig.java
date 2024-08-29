/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ondevicepersonalization.federatedcompute.shuffler.collector.core.gcp;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configuration for collector. */
@Configuration
public class CollectorConfig {
  @Bean
  @Qualifier("pubSubSubscription")
  public String pubSubSubscription(String aggregatorNotificationPubsubSubscription) {
    // Set the pubSubSubscription bean to point to aggregatorNotificationPubsubSubscription
    return aggregatorNotificationPubsubSubscription;
  }

  @Bean
  @Qualifier("aggregatorNotificationEndpoint")
  public String aggregatorNotificationEndpoint(
      String projectId, String aggregatorNotificationPubsubTopic) {
    return String.format(
        "https://pubsub.googleapis.com/v1/projects/%s/topics/%s:publish",
        projectId, aggregatorNotificationPubsubTopic);
  }
}
