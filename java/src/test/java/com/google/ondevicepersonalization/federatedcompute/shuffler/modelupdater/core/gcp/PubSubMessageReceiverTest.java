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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.ondevicepersonalization.federatedcompute.shuffler.modelupdater.core.ModelUpdaterCore;
import com.google.ondevicepersonalization.federatedcompute.shuffler.modelupdater.core.message.ModelUpdaterMessage;
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

  @Mock ModelUpdaterCore modelUpdaterCore;
  @Mock BasicAcknowledgeablePubsubMessage message;

  PubSubMessageReceiver pubSubMessageReceiver;
  ArgumentCaptor<ModelUpdaterMessage> messageCaptor;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    pubSubMessageReceiver = new PubSubMessageReceiver(modelUpdaterCore);
    messageCaptor = ArgumentCaptor.forClass(ModelUpdaterMessage.class);
  }

  @Test
  public void messageReceiver_success() {
    pubSubMessageReceiver.messageReceiver(
        "{\"serverPlanBucket\":\"serverPlanBucket\",\"serverPlanObject\":\"server_phase\",\"intermediateGradientBucket\":\"aggregatedGradientBucket\",\"intermediateGradientPrefix\":\"gradient\",\"outputTopic\":\"topic\",\"requestId\":\"pop/1/1/0\",\"checkpointBucket\":\"checkpointBucket\",\"checkpointObject\":\"checkpoint\",\"newCheckpointOutputBucket\":\"newCheckpointOutputBucket\",\"newCheckpointOutputObject\":\"checkpoint\",\"newClientCheckpointOutputBucket\":\"newClientCheckpointOutputBucket\",\"newClientCheckpointOutputObject\":\"client_checkpoint\",\"metricsOutputBucket\":\"metricsOutputBucket\",\"metricsOutputObject\":\"metrics\"}",
        message);
    verify(message, times(1)).ack();
    verify(modelUpdaterCore, times(1)).process(messageCaptor.capture());
    ModelUpdaterMessage expected =
        ModelUpdaterMessage.builder()
            .serverPlanBucket("serverPlanBucket")
            .serverPlanObject("server_phase")
            .intermediateGradientBucket("aggregatedGradientBucket")
            .intermediateGradientPrefix("gradient")
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
    List<ModelUpdaterMessage> capturedResults = messageCaptor.getAllValues();
    assertThat(expected).isEqualTo(capturedResults.get(0));
  }
}
