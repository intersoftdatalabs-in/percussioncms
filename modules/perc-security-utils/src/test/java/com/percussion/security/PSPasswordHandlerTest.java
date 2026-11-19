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
package com.percussion.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class PSPasswordHandlerTest {

  @Test
  void checkHashedPasswordMatchesValidAndRejectsTamperedOrMalformed() throws Exception {
    String pw = "CorrectHorseBattery12!";
    String hash1 = PSPasswordHandler.getHashedPassword(pw);
    String hash2 = PSPasswordHandler.getHashedPassword(pw);

    assertNotEquals(hash1, hash2, "fresh salts must produce distinct digests");
    assertTrue(PSPasswordHandler.checkHashedPassword(pw, hash1));
    assertTrue(PSPasswordHandler.checkHashedPassword(pw, hash2));
    assertFalse(PSPasswordHandler.checkHashedPassword("WrongHorseBattery12!", hash1));

    byte[] decoded = Base64.getDecoder().decode(hash1);
    decoded[0] ^= 0x01;
    String tampered = Base64.getEncoder().encodeToString(decoded);
    assertFalse(PSPasswordHandler.checkHashedPassword(pw, tampered));

    assertFalse(PSPasswordHandler.checkHashedPassword(pw, "!!!not-base64!!!"));
    String tooShort = Base64.getEncoder().encodeToString("short".getBytes(StandardCharsets.UTF_8));
    assertFalse(PSPasswordHandler.checkHashedPassword(pw, tooShort));
  }

  @Test
  void checkHashedPasswordRejectsNullArguments() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSPasswordHandler.checkHashedPassword(null, "abc"));
    assertThrows(
        IllegalArgumentException.class,
        () -> PSPasswordHandler.checkHashedPassword("abc", null));
  }
}
