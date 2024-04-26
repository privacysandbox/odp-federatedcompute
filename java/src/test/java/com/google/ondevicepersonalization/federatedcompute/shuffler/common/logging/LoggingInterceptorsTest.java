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

package com.google.ondevicepersonalization.federatedcompute.shuffler.common.logging;

import static com.google.common.truth.Truth.assertThat;

import com.google.internal.federatedcompute.v1.Resource;
import com.google.ondevicepersonalization.federatedcompute.proto.TaskAssignment;
import com.google.ondevicepersonalization.federatedcompute.proto.UploadInstruction;
import com.google.protobuf.util.JsonFormat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class LoggingInterceptorsTest {

  @Test
  public void testRedactUrl_oneMatch() {
    // act
    String result =
        LoggingInterceptors.redactUrl("abc https://abc?x=a b https://xyz/?p=q c https://xyz/");

    // assert
    assertThat(result).isEqualTo("abc https://abc b https://xyz/ c https://xyz/");
  }

  @Test
  public void testRedactUrl_withTaskAssignmentProto_toString() {
    // arrange
    TaskAssignment ta =
        TaskAssignment.newBuilder()
            .setPopulationName("a")
            .setTaskId("b")
            .setAggregationId("c")
            .setAssignmentId("d")
            .setTaskName("e")
            .setInitCheckpoint(
                Resource.newBuilder()
                    .setUri(
                        "https://storage.googleapis.com/fcp-test-us1-m-0/us/5/1/s/0/checkpoint?X-Goog-Algorithm=GOOG4-RSA-SHA256&X-Goog-Credential")
                    .build())
            .setPlan(
                Resource.newBuilder()
                    .setUri(
                        "https://storage.googleapis.com/fcp-test-us1-m-0/us/5/0/s/0/client_only_plan?X-Goog-Algorithm=GOOG4-RSA-SHA256&X-Goog-Credential=oda-fc")
                    .build())
            .setSelfUri(
                "/population/us/task/5/aggregation/2/task-assignment/b03c60d5-f73e-4338-b707-8f2e786fa249")
            .build();

    // act
    String result = LoggingInterceptors.redactUrl(ta.toString());

    // assert
    assertThat(result.contains("X-Goog-Algorithm")).isFalse();
    assertThat(
            result.contains(
                "https://storage.googleapis.com/fcp-test-us1-m-0/us/5/1/s/0/checkpoint"))
        .isTrue();
    assertThat(
            result.contains(
                "https://storage.googleapis.com/fcp-test-us1-m-0/us/5/0/s/0/client_only_plan"))
        .isTrue();
  }

  @Test
  public void testRedactUrl_withTaskAssignmentProto_toJson() throws Exception {
    // arrange
    TaskAssignment ta =
        TaskAssignment.newBuilder()
            .setPopulationName("a")
            .setTaskId("b")
            .setAggregationId("c")
            .setAssignmentId("d")
            .setTaskName("e")
            .setInitCheckpoint(
                Resource.newBuilder()
                    .setUri(
                        "https://storage.googleapis.com/fcp-test-us1-m-0/us/5/1/s/0/checkpoint?X-Goog-Algorithm=GOOG4-RSA-SHA256&X-Goog-Credential")
                    .build())
            .setPlan(
                Resource.newBuilder()
                    .setUri(
                        "https://storage.googleapis.com/fcp-test-us1-m-0/us/5/0/s/0/client_only_plan?X-Goog-Algorithm=GOOG4-RSA-SHA256&X-Goog-Credential=oda-fc")
                    .build())
            .setSelfUri(
                "/population/us/task/5/aggregation/2/task-assignment/b03c60d5-f73e-4338-b707-8f2e786fa249")
            .build();

    // act
    String result = LoggingInterceptors.redactUrl(JsonFormat.printer().print(ta));

    // assert
    assertThat(result.contains("X-Goog-Algorithm")).isFalse();
    assertThat(
            result.contains(
                "https://storage.googleapis.com/fcp-test-us1-m-0/us/5/1/s/0/checkpoint"))
        .isTrue();
    assertThat(
            result.contains(
                "https://storage.googleapis.com/fcp-test-us1-m-0/us/5/0/s/0/client_only_plan"))
        .isTrue();
  }

  @Test
  public void testRedactUrl_withUploadInstruction_toString() {
    // arrange
    UploadInstruction ui =
        UploadInstruction.newBuilder()
            .setUploadLocation(
                "https://storage.googleapis.com/fcp-test-us1-m-0/us/5/0/s/0/client_only_plan?X-Goog-Algorithm=GOOG4-RSA-SHA256&X-Goog-Credential=oda-fc")
            .build();

    // act
    String result = LoggingInterceptors.redactUrl(ui.toString());

    // assert
    assertThat(result.contains("X-Goog-Algorithm")).isFalse();
    assertThat(
            result.contains(
                "https://storage.googleapis.com/fcp-test-us1-m-0/us/5/0/s/0/client_only_plan"))
        .isTrue();
  }

  @Test
  public void testRedactUrl_withUploadInstruction_json() throws Exception {
    // arrange
    UploadInstruction ui =
        UploadInstruction.newBuilder()
            .setUploadLocation(
                "https://storage.googleapis.com/fcp-test-us1-m-0/us/5/0/s/0/client_only_plan?X-Goog-Algorithm=GOOG4-RSA-SHA256&X-Goog-Credential=oda-fc")
            .build();

    // act
    String result = LoggingInterceptors.redactUrl(JsonFormat.printer().print(ui));

    // assert
    assertThat(result.contains("X-Goog-Algorithm")).isFalse();
    assertThat(
            result.contains(
                "https://storage.googleapis.com/fcp-test-us1-m-0/us/5/0/s/0/client_only_plan"))
        .isTrue();
  }
}
