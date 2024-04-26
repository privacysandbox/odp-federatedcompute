// Copyright 2023 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
////////////////////////////////////////////////////////////////////////////////

package com.google.ondevicepersonalization.federatedcompute.shuffler.modelupdater.core.gcp;

import com.google.api.gax.batching.FlowControlSettings;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import com.google.gson.Gson;
import com.google.ondevicepersonalization.federatedcompute.shuffler.modelupdater.core.ModelUpdaterCore;
import com.google.ondevicepersonalization.federatedcompute.shuffler.modelupdater.core.message.ModelUpdaterMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

/** PubSub configuration for model updater. */
@Configuration
public class PubSubMessageReceiver {

  private static final Logger logger = LoggerFactory.getLogger(PubSubMessageReceiver.class);

  private ModelUpdaterCore modelupdater;

  public PubSubMessageReceiver(ModelUpdaterCore modelupdater) {
    this.modelupdater = modelupdater;
  }

  /**
   * Creates a FlowControlSettings bean to be used within the Google Cloud Pub/Sub
   * DefaultSubscriberFactory configuration.
   *
   * @param modelUpdaterSubscriberMaxOutstandingElementCount The maximum number of unacknowledged
   *     messages allowed before the subscriber pauses receiving new messages.
   * @return A FlowControlSettings object configured with the specified limit.
   */
  @Bean
  @Qualifier("subscriberFlowControlSettings")
  public FlowControlSettings subscriberFlowControlSettings(
      long modelUpdaterSubscriberMaxOutstandingElementCount) {
    // Controls the maximum number of messages the subscriber receives before pausing the message
    // stream.
    return FlowControlSettings.newBuilder()
        .setMaxOutstandingElementCount(modelUpdaterSubscriberMaxOutstandingElementCount)
        .build();
  }

  /**
   * Processes incoming messages from a Google Cloud Pub/Sub input channel, responsible for parsing
   * the payload, triggering model updates, and handling message acknowledgments. This method acts
   * as a Spring Integration Service Activator.
   *
   * @param payload The raw message payload as a String.
   * @param message The original Google Cloud Pub/Sub message object, providing access to
   *     acknowledgement functionality.
   */
  @ServiceActivator(inputChannel = "pubsubInputChannel")
  public void messageReceiver(
      @Payload String payload,
      @Header(GcpPubSubHeaders.ORIGINAL_MESSAGE) BasicAcknowledgeablePubsubMessage message) {
    logger.info("Message arrived via an inbound channel adapter! Payload: " + payload);
    Gson gson = new Gson();
    try {
      ModelUpdaterMessage modelUpdaterMessage = gson.fromJson(payload, ModelUpdaterMessage.class);
      modelupdater.process(modelUpdaterMessage);
    } catch (Exception e) {
      logger.error("Failed to process message.", e);
      message.nack();
      return;
    }
    message.ack();
  }
}
