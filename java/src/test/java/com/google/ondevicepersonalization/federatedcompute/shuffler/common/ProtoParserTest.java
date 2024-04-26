// Copyright 2024 Google LLC
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

package com.google.ondevicepersonalization.federatedcompute.shuffler.common;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.ondevicepersonalization.federatedcompute.proto.TaskInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ProtoParserTest {

  private static final TaskInfo TASK_INFO = TaskInfo.newBuilder().setTrafficWeight(1).build();
  private static final String INFO_STRING = "{\n  \"trafficWeight\": \"1\"\n}";

  @Test
  public void validJsonStringInput_toProto_success() {
    TaskInfo taskInfo = ProtoParser.toProto(INFO_STRING, TaskInfo.getDefaultInstance());

    assertThat(taskInfo).isEqualTo(TASK_INFO);
  }

  @Test
  public void invalidJsonStringInput_toProto_throwException() {
    String json = "{\n  \"invalid_field\": \"1\"\n}";

    assertThrows(
        IllegalStateException.class,
        () -> ProtoParser.toProto(json, TaskInfo.getDefaultInstance()));
  }

  @Test
  public void protoInput_toJsonString_success() {
    String json = ProtoParser.toJsonString(TASK_INFO);

    assertThat(json).isEqualTo(INFO_STRING);
  }
}
