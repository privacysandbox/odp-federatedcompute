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

import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import com.google.gson.Gson;
import com.google.ondevicepersonalization.federatedcompute.shuffler.aggregator.core.message.AggregatorNotification;
import com.google.ondevicepersonalization.federatedcompute.shuffler.collector.core.CollectorCore;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

/** PubSub configuration for collector */
@Configuration
public class PubSubMessageReceiver {

  private static final Logger logger = LoggerFactory.getLogger(PubSubMessageReceiver.class);

  private CollectorCore collector;

  public PubSubMessageReceiver(CollectorCore collector) {
    this.collector = collector;
  }

  @ServiceActivator(inputChannel = "pubsubInputChannel")
  public void messageReceiver(
      @Payload String payload,
      @Header(GcpPubSubHeaders.ORIGINAL_MESSAGE) BasicAcknowledgeablePubsubMessage message) {
    Map<String, String> messageAttributes = message.getPubsubMessage().getAttributesMap();
    logger.info(
        "Message arrived via an inbound channel adapter! Payload: {}, Attributes {}",
        payload,
        messageAttributes);
    try {
      Gson gson = new Gson();
      AggregatorNotification.Attributes notificationAttributes =
          gson.fromJson(
              gson.toJson(message.getPubsubMessage().getAttributesMap()),
              AggregatorNotification.Attributes.class);
      collector.processAggregatorNotifications(notificationAttributes);
    } catch (Exception e) {
      logger.error("Failed to process message.", e);
      message.nack();
      return;
    }
    message.ack();
  }
}
