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

package com.google.ondevicepersonalization.federatedcompute.shuffler.aggregator.core.gcp;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.ondevicepersonalization.federatedcompute.shuffler.aggregator.core.AggregatorCore;
import com.google.ondevicepersonalization.federatedcompute.shuffler.aggregator.core.message.AggregatorMessage;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(JUnit4.class)
public class PubSubMessageReceiverTest {

  @Mock AggregatorCore aggregator;
  @Mock BasicAcknowledgeablePubsubMessage message;

  PubSubMessageReceiver pubSubMessageReceiver;
  ArgumentCaptor<AggregatorMessage> messageCaptor;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    pubSubMessageReceiver = new PubSubMessageReceiver(aggregator);
    messageCaptor = ArgumentCaptor.forClass(AggregatorMessage.class);
  }

  @Test
  public void messageReceiver_success() {
    pubSubMessageReceiver.messageReceiver(
        "{\"serverPlanBucket\":\"serverPlanBucket\",\"serverPlanObject\":\"server_phase\",\"gradientBucket\":\"gradientBucket\",\"gradientPrefix\":\"\",\"gradients\":[\"66ee8bd0-17fe-42ca-aef5-380d0132fabd/gradient\",\"c0cf2876-49da-418b-a123-d0e4aed1ec14/gradient\"],\"aggregatedGradientOutputBucket\":\"aggregatedGradientOutputBucket\",\"aggregatedGradientOutputObject\":\"gradient\",\"requestId\":\"pop/1/1/0\"}",
        message);
    verify(message, times(1)).ack();
    verify(aggregator, times(1)).process(messageCaptor.capture());
    List<String> gradients = new ArrayList<>();
    gradients.add("66ee8bd0-17fe-42ca-aef5-380d0132fabd/gradient");
    gradients.add("c0cf2876-49da-418b-a123-d0e4aed1ec14/gradient");
    AggregatorMessage expected =
        AggregatorMessage.builder()
            .serverPlanBucket("serverPlanBucket")
            .serverPlanObject("server_phase")
            .gradientBucket("gradientBucket")
            .gradientPrefix("")
            .gradients(gradients)
            .aggregatedGradientOutputBucket("aggregatedGradientOutputBucket")
            .aggregatedGradientOutputObject("gradient")
            .requestId("pop/1/1/0")
            .build();
    List<AggregatorMessage> capturedResults = messageCaptor.getAllValues();
    assertThat(expected).isEqualTo(capturedResults.get(0));
  }
}
