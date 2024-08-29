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

package com.google.ondevicepersonalization.federatedcompute.shuffler.collector.core.gcp;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.ondevicepersonalization.federatedcompute.shuffler.aggregator.core.message.AggregatorNotification;
import com.google.ondevicepersonalization.federatedcompute.shuffler.collector.core.CollectorCore;
import com.google.pubsub.v1.PubsubMessage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(JUnit4.class)
public class PubSubMessageReceiverTest {

  @Mock CollectorCore collector;
  @Mock BasicAcknowledgeablePubsubMessage message;
  @Mock PubsubMessage pubSubMessage;

  PubSubMessageReceiver pubSubMessageReceiver;
  ArgumentCaptor<AggregatorNotification.Attributes> messageCaptor;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    pubSubMessageReceiver = new PubSubMessageReceiver(collector);
    messageCaptor = ArgumentCaptor.forClass(AggregatorNotification.Attributes.class);
  }

  @Test
  public void messageReceiver_success() {
    when(message.getPubsubMessage()).thenReturn(pubSubMessage);
    Map<String, String> attributes = new HashMap<>();
    attributes.put("requestId", "HelloWorld");
    attributes.put("status", "OK");
    when(pubSubMessage.getAttributesMap()).thenReturn(attributes);
    pubSubMessageReceiver.messageReceiver("", message);
    verify(message, times(1)).ack();
    verify(collector, times(1)).processAggregatorNotifications(messageCaptor.capture());
    AggregatorNotification.Attributes expected =
        AggregatorNotification.Attributes.builder()
            .status(AggregatorNotification.Status.OK)
            .requestId("HelloWorld")
            .build();
    List<AggregatorNotification.Attributes> capturedResults = messageCaptor.getAllValues();
    assertThat(expected).isEqualTo(capturedResults.get(0));
  }
}
