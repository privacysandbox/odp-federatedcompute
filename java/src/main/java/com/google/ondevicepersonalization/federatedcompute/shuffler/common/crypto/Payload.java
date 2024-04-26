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

package com.google.ondevicepersonalization.federatedcompute.shuffler.common.crypto;

import com.google.crypto.tink.HybridDecrypt;
import com.google.crypto.tink.subtle.Base64;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.ondevicepersonalization.federatedcompute.shuffler.common.CompressionUtils;
import com.google.scp.operator.cpio.cryptoclient.DecryptionKeyService;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
@Builder(toBuilder = true)
/** POJO representing encrypted payload downloaded from storage. */
public class Payload {
  // ID of the key used to encrypt the payload
  private String keyId;

  // The authenticated encrypted payload
  private String encryptedPayload;

  // The associated data of the authenticated encryption
  private String associatedData;

  public static byte[] parseAndDecryptPayload(
      byte[] payloadJson, DecryptionKeyService decryptionKeyService) {
    try {
      String jsonPayload = new String(payloadJson);
      Gson gson = new Gson();
      Payload payload = gson.fromJson(jsonPayload, Payload.class);
      byte[] associatedData = Base64.decode(payload.getAssociatedData(), Base64.NO_WRAP);
      // Decode
      byte[] encryptedPayload = Base64.decode(payload.getEncryptedPayload(), Base64.NO_WRAP);
      // Decrypt
      HybridDecrypt hybridDecrypt = decryptionKeyService.getDecrypter(payload.getKeyId());
      byte[] decryptedPayload = hybridDecrypt.decrypt(encryptedPayload, associatedData);
      return CompressionUtils.uncompressWithGzip(decryptedPayload);
    } catch (JsonSyntaxException jse) {
      // Support unencrypted payloads in the aggregator for testing. Unencrypted payloads uploaded
      // are considered to be non-private.
      // All data uploaded from devices containing private data are expected to be encrypted.
      return payloadJson;
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }
}
