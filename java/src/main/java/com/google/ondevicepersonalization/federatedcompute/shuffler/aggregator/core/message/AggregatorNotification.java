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

package com.google.ondevicepersonalization.federatedcompute.shuffler.aggregator.core.message;

import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/** Notification sent for a completed aggregation job. */
@Getter
@EqualsAndHashCode
@Builder(toBuilder = true)
public class AggregatorNotification {
  public enum Status {
    OK,
    ERROR
  }

  public enum ErrorReason {
    AGGREGATION_ERROR,
    DECRYPTION_ERROR,
    UNKNOWN_ERROR
  }

  /** List of messages for the job result. */
  private List<Message> messages;

  @Getter
  @EqualsAndHashCode
  @Builder(toBuilder = true)
  public static class Message {
    /** Attributes of the message. */
    private Attributes attributes;
  }

  @Getter
  @EqualsAndHashCode
  @Builder(toBuilder = true)
  public static class Attributes {
    /** Status specifying the result. */
    private Status status;

    /** Id of the request. */
    private String requestId;

    /** Reason for error. */
    private ErrorReason errorReason;
  }
}
