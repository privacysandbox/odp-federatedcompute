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

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

/**
 * Utility methods for converting between proto and json formats. Uses the {@link JsonFormat}
 * serialization tool that converts proto -> json and back.
 */
public final class ProtoParser {

  /**
   * Converts a JSON string representation of a message into a Protocol Buffer message of the
   * specified type.
   *
   * @param jsonString The JSON string containing the message data.
   * @param targetProto An instance of the target Protocol Buffer message type, providing a builder
   *     for the conversion.
   * @param <T> The specific type of the Protocol Buffer message, extending the 'Message' class.
   * @return The constructed Protocol Buffer message object.
   * @throws IllegalStateException If a parsing error occurs during the JSON to Protocol Buffer
   *     conversion.
   */
  public static <T extends Message> T toProto(String jsonString, T targetProto) {
    try {
      Message.Builder protoBuilder = targetProto.newBuilderForType();
      JsonFormat.parser().merge(jsonString, protoBuilder);
      @SuppressWarnings("unchecked") // safe by contract of newBuilderForType()
      T proto = (T) protoBuilder.build();
      return proto;
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Serializes a Protocol Buffer message into a JSON string representation.
   *
   * @param message The Protocol Buffer message to be serialized.
   * @return A JSON string representation of the provided message.
   * @throws IllegalStateException If an error occurs during the serialization process.
   */
  public static String toJsonString(Message message) {
    try {
      return JsonFormat.printer().print(message);
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalStateException(e);
    }
  }
}
