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

package com.google.ondevicepersonalization.federatedcompute.shuffler.common.messaging.gcp;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import com.google.gson.Gson;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.messaging.gcp.PubSubPublisherConfig.PubsubOutboundGateway;
import com.google.ondevicepersonalization.federatedcompute.shuffler.modelupdater.core.message.ModelUpdaterMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.Message;

@RunWith(JUnit4.class)
public class PubSubMessageSenderTest {

  ModelUpdaterMessage MESSAGE =
      ModelUpdaterMessage.builder()
          .serverPlanBucket("serverPlanBucket")
          .serverPlanObject("server_phase")
          .aggregatedGradientBucket("aggregatedGradientBucket")
          .aggregatedGradientObject("gradient")
          .checkpointBucket("checkpointBucket")
          .checkpointObject("checkpoint")
          .newCheckpointOutputBucket("newCheckpointOutputBucket")
          .newCheckpointOutputObject("checkpoint")
          .newClientCheckpointOutputBucket("newClientCheckpointOutputBucket")
          .newClientCheckpointOutputObject("client_checkpoint")
          .metricsOutputBucket("metricsOutputBucket")
          .metricsOutputObject("metrics")
          .requestId("pop/1/1/0")
          .build();
  ArgumentCaptor<Message<String>> messageCaptor;
  @Mock private PubsubOutboundGateway messagingGateway;
  private PubSubMessageSender messageSender;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    messageSender = new PubSubMessageSender(messagingGateway);
    messageCaptor = ArgumentCaptor.forClass(Message.class);
  }

  @Test
  public void sendMessageTest_success() {
    Gson gson = new Gson();
    messageSender.sendMessage(MESSAGE, "topic");
    verify(messagingGateway, times(1)).sendToPubsub(messageCaptor.capture());
    assertEquals(
        gson.fromJson(messageCaptor.getValue().getPayload(), ModelUpdaterMessage.class), MESSAGE);
    assertEquals(messageCaptor.getValue().getHeaders().get(GcpPubSubHeaders.TOPIC), "topic");
  }
}
