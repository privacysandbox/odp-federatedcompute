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

package com.google.ondevicepersonalization.federatedcompute.shuffler.common.messaging.gcp;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.outbound.PubSubMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

/** PubSub publisher configuration. */
@Configuration
public class PubSubPublisherConfig {

  private static final Logger logger = LoggerFactory.getLogger(PubSubPublisherConfig.class);

  @Bean
  @ServiceActivator(inputChannel = "pubsubOutputChannel")
  public MessageHandler messageSender(PubSubTemplate pubsubTemplate) {
    // Default topic is unused. It should be overridden in the messagingGateway.
    PubSubMessageHandler adapter = new PubSubMessageHandler(pubsubTemplate, "topic");
    // Set synchronous so failures are not lost async.
    adapter.setSync(true);

    adapter.setFailureCallback(
        (exception, message) ->
            logger.info("There was an error sending the message: " + message.getPayload()));

    adapter.setSuccessCallback(
        (messageId, message) ->
            logger.info(
                "Message was sent successfully;\n\tpublish ID = "
                    + messageId
                    + "\n\tmessage="
                    + message.getPayload()));
    return adapter;
  }

  @MessagingGateway(defaultRequestChannel = "pubsubOutputChannel")
  public interface PubsubOutboundGateway {
    void sendToPubsub(Message<String> message);
  }
}
