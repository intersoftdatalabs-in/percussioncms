/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
package com.percussion.services.auth.mfa.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class PSTotpSecretCryptoTest {

  private final PSTotpSecretCrypto crypto =
      new PSTotpSecretCrypto(() -> "unit-test-instance-master-key-bytes".getBytes(StandardCharsets.UTF_8));

  @Test
  void roundTripsPerUserWithRandomIvAndEnforcesAadBinding() {
    byte[] secret = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);
    String enc = crypto.encrypt("Alice", secret);

    assertTrue(enc.startsWith("v1:"));
    assertArrayEquals(secret, crypto.decrypt("alice", enc));
    assertThrows(IllegalStateException.class, () -> crypto.decrypt("bob", enc));
    String tampered = enc.substring(0, enc.length() - 3) + (enc.charAt(enc.length() - 3) == 'A' ? 'B' : 'A') + enc.substring(enc.length() - 2);
    assertThrows(IllegalStateException.class, () -> crypto.decrypt("alice", tampered));
    assertThrows(IllegalStateException.class, () -> crypto.decrypt("alice", enc + "A"));
  }
}
