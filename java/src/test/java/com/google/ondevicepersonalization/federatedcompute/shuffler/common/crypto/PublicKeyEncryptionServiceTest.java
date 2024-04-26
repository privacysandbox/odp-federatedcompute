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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

import com.google.crypto.tink.hybrid.HybridConfig;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Base64;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(JUnit4.class)
public class PublicKeyEncryptionServiceTest {
  static {
    try {
      HybridConfig.register();
    } catch (GeneralSecurityException e) {
      throw new RuntimeException("Error initializing tink.");
    }
  }

  private PublicKeyEncryptionService publicKeyEncryptionService;

  private PublicKeys keys;

  @Mock PublicKeyFetchingService publicKeyFetchingService;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    publicKeyEncryptionService = new PublicKeyEncryptionService(publicKeyFetchingService);
    keys = PublicKeys.builder().keys(new ArrayList<>()).build();
    keys.getKeys()
        .add(
            PublicKey.builder()
                .id("2b9a33a7-895f-4b60-98f2-0502adea3afa")
                .key("2AFfcfrLWhdW3EEIvM1FulwH7JBCEHAaWUyO+8t0RGg=")
                .build());
    keys.getKeys()
        .add(
            PublicKey.builder()
                .id("a748012a-bff4-4d53-903c-e9a6a121d40f")
                .key("Y9VubbmpqMQhzt5QgUH5odZcqZ7tWobz7e36bQkrDWc=")
                .build());
  }

  @Test
  public void encrypt_success() throws Exception {
    when(publicKeyFetchingService.fetchPublicKeys()).thenReturn(keys);
    Payload payload =
        publicKeyEncryptionService.encryptPayload("HelloWorld".getBytes(), new byte[0]);
    assertThat(
        payload.getKeyId(),
        anyOf(
            is("2b9a33a7-895f-4b60-98f2-0502adea3afa"),
            is("a748012a-bff4-4d53-903c-e9a6a121d40f")));
    assertArrayEquals(new byte[0], Base64.getDecoder().decode(payload.getAssociatedData()));
  }

  @Test
  public void encrypt_no_keys() {
    when(publicKeyFetchingService.fetchPublicKeys())
        .thenReturn(PublicKeys.builder().keys(new ArrayList<>()).build());
    assertThrows(
        IllegalStateException.class,
        () -> publicKeyEncryptionService.encryptPayload("HelloWorld".getBytes(), new byte[0]));
  }
}
