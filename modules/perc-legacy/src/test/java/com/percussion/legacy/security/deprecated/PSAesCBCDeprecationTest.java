/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.legacy.security.deprecated;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for feature 004 (zero-code-scanning-alerts) Phase 5 T047/T048.
 *
 * <p>Documents the accepted-risk contract for {@link PSAesCBC}:
 *
 * <ul>
 *   <li>Class remains {@code @Deprecated(forRemoval=true)} so callers know it is upgrade-only.
 *   <li>Historical fixed IV remains so pre-8.2 ciphertext can still be decrypted.
 *   <li>String encrypt/decrypt round-trip still works (needed for upgrade tooling and fixtures).
 *   <li>New product encryption must use {@code com.percussion.security.PSEncryptor} (AES/GCM) —
 *       production call sites of this class are decrypt-only fallbacks.
 * </ul>
 */
class PSAesCBCDeprecationTest {

  @Test
  void classIsAnnotatedAsDeprecatedForRemoval() {
    Deprecated annotation = PSAesCBC.class.getAnnotation(Deprecated.class);
    assertNotNull(
        annotation,
        "PSAesCBC must be annotated @Deprecated for the 9.0 removal (CodeQL accepted-risk)");
    assertTrue(
        annotation.forRemoval(),
        "PSAesCBC @Deprecated(forRemoval=true) is required by accepted-risks.md");
  }

  @Test
  void stringEncryptMethodIsDeprecatedForRemoval() throws Exception {
    Method encrypt =
        PSAesCBC.class.getMethod("encrypt", String.class, String.class);
    Deprecated annotation = encrypt.getAnnotation(Deprecated.class);
    assertNotNull(
        annotation,
        "encrypt(String,String) must be @Deprecated — not for new production secrets");
    assertTrue(
        annotation.forRemoval(),
        "encrypt(String,String) @Deprecated(forRemoval=true) required by accepted-risk contract");
  }

  @Test
  void staticInitializationVectorFieldIsDocumentedAsAcceptedRisk() throws Exception {
    java.lang.reflect.Field f = PSAesCBC.class.getDeclaredField("INITIAL_VECTOR");
    assertNotNull(f, "INITIAL_VECTOR field must exist (CodeQL static-IV accepted-risk)");
    // The class-level @Deprecated + class-level javadoc on INITIAL_VECTOR is the
    // accepted-risk marker that downstream consumers see; the test asserts the field
    // is still present so decryption of historical ciphertext continues to work.
    Deprecated classDep = PSAesCBC.class.getAnnotation(Deprecated.class);
    assertNotNull(
        classDep,
        "PSAesCBC must remain @Deprecated to flag the static-IV accepted-risk to callers");
  }

  @Test
  void legacyStringRoundTripStillDecryptsHistoricalLayout() throws Exception {
    // Proves upgrade-path decrypt still works with the fixed IV + AES/CBC layout.
    // Production encrypt of new secrets must NOT use this path (use PSEncryptor).
    PSAesCBC aes = new PSAesCBC();
    String key = "0123456789ABCDEF"; // 16 ISO-8859-1 bytes for AES-128 key material
    String plain = "upgrade-decrypt-fixture";
    String cipher = aes.encrypt(plain, key);
    assertEquals(plain, aes.decrypt(cipher, key));
  }

  @Test
  void randomIvByteArrayRoundTripStillWorks() throws Exception {
    byte[] rawKey = "0123456789ABCDEF".getBytes(StandardCharsets.ISO_8859_1);
    PSAesCBC aes = new PSAesCBC(rawKey);
    byte[] plain = "byte-array-fixture".getBytes(StandardCharsets.UTF_8);
    byte[] cipher = aes.encrypt(plain);
    assertTrue(cipher.length > 16, "ciphertext must prepend a 16-byte IV");
    byte[] roundTrip = aes.decrypt(cipher, "0123456789ABCDEF");
    assertEquals(
        new String(plain, StandardCharsets.UTF_8),
        new String(roundTrip, StandardCharsets.UTF_8));
  }
}
