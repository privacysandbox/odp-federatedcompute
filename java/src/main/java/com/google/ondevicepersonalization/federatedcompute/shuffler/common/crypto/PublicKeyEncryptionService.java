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

package com.google.ondevicepersonalization.federatedcompute.shuffler.common.crypto;

import com.google.crypto.tink.CleartextKeysetHandle;
import com.google.crypto.tink.HybridEncrypt;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.proto.HpkePublicKey;
import com.google.crypto.tink.proto.KeyData;
import com.google.crypto.tink.proto.KeyData.KeyMaterialType;
import com.google.crypto.tink.proto.KeyStatusType;
import com.google.crypto.tink.proto.Keyset;
import com.google.crypto.tink.proto.OutputPrefixType;
import com.google.protobuf.ByteString;
import com.google.scp.shared.util.KeyParams;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Class to handle encrypting payloads using public encryption keys */
@Component
public class PublicKeyEncryptionService {
  private static final Logger logger = LoggerFactory.getLogger(PublicKeyEncryptionService.class);
  private final PublicKeyFetchingService publicKeyFetchingService;
  private PublicKeys publicKeys;

  public PublicKeyEncryptionService(PublicKeyFetchingService publicKeyFetchingService) {
    this.publicKeyFetchingService = publicKeyFetchingService;
    this.publicKeys = PublicKeys.builder().keys(new ArrayList<>()).build();
  }

  public Payload encryptPayload(byte[] data, byte[] associatedData) {
    if (publicKeys.getKeys().size() == 0) {
      fetchNewPublicKeys();
      if (publicKeys.getKeys().size() == 0) {
        throw new IllegalStateException("No public keys available and failed to fetch");
      }
    }
    try {
      List<PublicKey> keyList = publicKeys.getKeys();
      PublicKey randomKey = keyList.get(new Random().nextInt(keyList.size()));
      ByteString keyProto =
          HpkePublicKey.newBuilder()
              .setPublicKey(ByteString.copyFrom(Base64.getDecoder().decode(randomKey.getKey())))
              .setParams(KeyParams.getHpkeParams())
              .build()
              .toByteString();
      KeysetHandle handle =
          CleartextKeysetHandle.fromKeyset(
              Keyset.newBuilder()
                  .addKey(
                      Keyset.Key.newBuilder()
                          .setStatus(KeyStatusType.ENABLED)
                          .setOutputPrefixType(OutputPrefixType.RAW)
                          .setKeyData(
                              KeyData.newBuilder()
                                  .setTypeUrl(
                                      "type.googleapis.com/google.crypto.tink.HpkePublicKey")
                                  .setKeyMaterialType(KeyMaterialType.ASYMMETRIC_PUBLIC)
                                  .setValue(keyProto)
                                  .build())
                          .build())
                  .build());
      HybridEncrypt encryptor = handle.getPrimitive(HybridEncrypt.class);

      byte[] ciphertext = encryptor.encrypt(data, associatedData);
      Payload finalPayload =
          Payload.builder()
              .keyId(randomKey.getId())
              .encryptedPayload(Base64.getEncoder().encodeToString(ciphertext))
              .associatedData(Base64.getEncoder().encodeToString(associatedData))
              .build();
      return finalPayload;
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Failed to encrypt payload", e);
    }
  }

  public void fetchNewPublicKeys() {
    PublicKeys newPublicKeys = publicKeyFetchingService.fetchPublicKeys();
    if (newPublicKeys.getKeys().size() > 0) {
      this.publicKeys = newPublicKeys;
    } else {
      logger.error("Failed to fetch new public keys.");
    }
  }
}
