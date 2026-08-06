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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.security.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for {@link PSCryptographyUtils} covering CWE-327 (Weak Cryptography)
 * vulnerability prevention.
 *
 * <p>Test Coverage:
 *
 * <ul>
 *   <li>SHA-256 hashing (replaces MD5)
 *   <li>SHA-512 hashing (replaces SHA-1)
 *   <li>Weak algorithm detection
 *   <li>Random salt generation
 *   <li>Algorithm validation
 *   <li>Migration recommendations
 * </ul>
 */
@DisplayName("PSCryptographyUtils - Weak Cryptography Prevention (CWE-327)")
class PSCryptographyUtilsTest {

  @Nested
  @DisplayName("SHA-256 Hashing Tests")
  class SHA256HashingTests {

    @Test
    @DisplayName("Should compute SHA-256 hash of byte array")
    void testSHA256HashBytes() {
      byte[] data = "test data".getBytes(java.nio.charset.StandardCharsets.UTF_8);
      String hash = PSCryptographyUtils.sha256Hex(data);

      assertNotNull(hash, "Hash should not be null");
      assertEquals(64, hash.length(), "SHA-256 hash should be 64 hex characters");
      assertTrue(hash.matches("[0-9A-F]+"), "Hash should be valid hexadecimal (uppercase)");
    }

    @Test
    @DisplayName("Should compute SHA-256 hash of string")
    void testSHA256HashString() {
      String data = "test data";
      String hash = PSCryptographyUtils.sha256Hex(data);

      assertEquals(64, hash.length(), "SHA-256 hash should be 64 hex characters");
      assertTrue(hash.matches("[0-9A-F]+"), "Hash should be valid hexadecimal");
    }

    @Test
    @DisplayName("Should produce consistent hash for same input")
    void testSHA256Consistency() {
      String data = "consistent test";
      String hash1 = PSCryptographyUtils.sha256Hex(data);
      String hash2 = PSCryptographyUtils.sha256Hex(data);

      assertEquals(hash1, hash2, "Same input should produce same hash");
    }

    @Test
    @DisplayName("Should produce different hash for different input")
    void testSHA256Different() {
      String hash1 = PSCryptographyUtils.sha256Hex("data1");
      String hash2 = PSCryptographyUtils.sha256Hex("data2");

      assertNotEquals(hash1, hash2, "Different inputs should produce different hashes");
    }

    @Test
    @DisplayName("Should handle empty string")
    void testSHA256EmptyString() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PSCryptographyUtils.sha256Hex(""),
          "Empty string should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("Should handle null input")
    void testSHA256NullInput() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PSCryptographyUtils.sha256Hex((String) null),
          "Null input should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("Should handle whitespace-only string")
    void testSHA256WhitespaceOnly() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PSCryptographyUtils.sha256Hex("   "),
          "Whitespace-only string should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("Should handle binary data correctly")
    void testSHA256BinaryData() {
      byte[] binaryData = new byte[] {0x00, 0x01, 0x02, -1, -2, -3};
      String hash = PSCryptographyUtils.sha256Hex(binaryData);

      assertEquals(64, hash.length(), "Should handle binary data with all byte values");
      assertNotNull(hash, "Hash should be computed for binary data");
    }
  }

  @Nested
  @DisplayName("SHA-512 Hashing Tests")
  class SHA512HashingTests {

    @Test
    @DisplayName("Should compute SHA-512 hash of byte array")
    void testSHA512HashBytes() {
      byte[] data = "test data".getBytes(java.nio.charset.StandardCharsets.UTF_8);
      String hash = PSCryptographyUtils.sha512Hex(data);

      assertNotNull(hash, "Hash should not be null");
      assertEquals(128, hash.length(), "SHA-512 hash should be 128 hex characters");
      assertTrue(hash.matches("[0-9A-F]+"), "Hash should be valid hexadecimal");
    }

    @Test
    @DisplayName("Should compute SHA-512 hash of string")
    void testSHA512HashString() {
      String data = "test data";
      String hash = PSCryptographyUtils.sha512Hex(data);

      assertEquals(128, hash.length(), "SHA-512 hash should be 128 hex characters");
      assertTrue(hash.matches("[0-9A-F]+"), "Hash should be valid hexadecimal");
    }

    @Test
    @DisplayName("Should produce different hash than SHA-256")
    void testSHA512DifferentFromSHA256() {
      String data = "test";
      String hash256 = PSCryptographyUtils.sha256Hex(data);
      String hash512 = PSCryptographyUtils.sha512Hex(data);

      assertNotEquals(
          hash256, hash512, "SHA-256 and SHA-512 should produce different length hashes");
      assertEquals(64, hash256.length(), "SHA-256 should be 64 chars");
      assertEquals(128, hash512.length(), "SHA-512 should be 128 chars");
    }

    @Test
    @DisplayName("Should handle null input")
    void testSHA512NullInput() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PSCryptographyUtils.sha512Hex((String) null),
          "Null input should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("Weak Algorithm Detection Tests")
  class WeakAlgorithmDetectionTests {

    @Test
    @DisplayName("Should reject MD5 algorithm")
    void testRejectMD5() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PSCryptographyUtils.isAlgorithmAllowed("MD5"),
          "MD5 should be rejected as weak");
    }

    @Test
    @DisplayName("Should reject MD5 case-insensitive")
    void testRejectMD5CaseInsensitive() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PSCryptographyUtils.isAlgorithmAllowed("md5"),
          "MD5 (lowercase) should be rejected");

      assertThrows(
          IllegalArgumentException.class,
          () -> PSCryptographyUtils.isAlgorithmAllowed("Md5"),
          "MD5 (mixed case) should be rejected");
    }

    @Test
    @DisplayName("Should reject SHA-1 algorithm")
    void testRejectSHA1() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PSCryptographyUtils.isAlgorithmAllowed("SHA-1"),
          "SHA-1 should be rejected as weak");

      assertThrows(
          IllegalArgumentException.class,
          () -> PSCryptographyUtils.isAlgorithmAllowed("SHA1"),
          "SHA1 (without dash) should be rejected");
    }

    @Test
    @DisplayName("Should reject DES algorithm")
    void testRejectDES() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PSCryptographyUtils.isAlgorithmAllowed("DES"),
          "DES should be rejected (too weak)");

      assertThrows(
          IllegalArgumentException.class,
          () -> PSCryptographyUtils.isAlgorithmAllowed("DES/ECB/PKCS5Padding"),
          "DES/ECB should be rejected");
    }

    @Test
    @DisplayName("Should reject RC4 algorithm")
    void testRejectRC4() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PSCryptographyUtils.isAlgorithmAllowed("RC4"),
          "RC4 should be rejected (biased output)");
    }

    @Test
    @DisplayName("Should allow SHA-256")
    void testAllowSHA256() {
      assertTrue(PSCryptographyUtils.isAlgorithmAllowed("SHA-256"), "SHA-256 should be allowed");
      assertTrue(
          PSCryptographyUtils.isAlgorithmAllowed("SHA-256/RSA"),
          "SHA-256 with other components should be allowed");
    }

    @Test
    @DisplayName("Should allow SHA-512")
    void testAllowSHA512() {
      assertTrue(PSCryptographyUtils.isAlgorithmAllowed("SHA-512"), "SHA-512 should be allowed");
    }

    @Test
    @DisplayName("Should allow AES")
    void testAllowAES() {
      assertTrue(PSCryptographyUtils.isAlgorithmAllowed("AES"), "AES should be allowed");
      assertTrue(
          PSCryptographyUtils.isAlgorithmAllowed("AES/GCM/NoPadding"), "AES/GCM should be allowed");
    }

    @Test
    @DisplayName("Should reject null algorithm")
    void testRejectNullAlgorithm() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PSCryptographyUtils.isAlgorithmAllowed(null),
          "Null algorithm should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("Should reject blank algorithm")
    void testRejectBlankAlgorithm() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PSCryptographyUtils.isAlgorithmAllowed(""),
          "Blank algorithm should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("Random Salt Generation Tests")
  class RandomSaltGenerationTests {

    @Test
    @DisplayName("Should generate random salt of requested length")
    void testGenerateSalt() {
      byte[] salt = PSCryptographyUtils.generateRandomSalt(16);

      assertNotNull(salt, "Salt should not be null");
      assertEquals(16, salt.length, "Salt should be 16 bytes");
    }

    @Test
    @DisplayName("Should generate different salts each time")
    void testGenerateDifferentSalts() {
      byte[] salt1 = PSCryptographyUtils.generateRandomSalt(16);
      byte[] salt2 = PSCryptographyUtils.generateRandomSalt(16);

      assertNotEquals(
          java.util.Arrays.toString(salt1),
          java.util.Arrays.toString(salt2),
          "Each salt should be different (statistically)");
    }

    @Test
    @DisplayName("Should generate various salt lengths")
    void testGenerateVariousSaltLengths() {
      for (int length = 8; length <= 32; length += 4) {
        byte[] salt = PSCryptographyUtils.generateRandomSalt(length);
        assertEquals(length, salt.length, "Salt length should match requested length");
      }
    }

    @Test
    @DisplayName("Should reject zero or negative length")
    void testRejectInvalidSaltLength() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PSCryptographyUtils.generateRandomSalt(0),
          "Zero length should be rejected");

      assertThrows(
          IllegalArgumentException.class,
          () -> PSCryptographyUtils.generateRandomSalt(-1),
          "Negative length should be rejected");
    }
  }

  @Nested
  @DisplayName("Algorithm Replacement Migration Tests")
  class AlgorithmReplacementTests {

    @Test
    @DisplayName("Should recommend SHA-256 for MD5")
    void testReplaceMD5() {
      String replacement = PSCryptographyUtils.getReplacementAlgorithm("MD5");
      assertEquals("SHA-256", replacement, "MD5 should be replaced with SHA-256");
    }

    @Test
    @DisplayName("Should recommend SHA-256 for SHA-1")
    void testReplaceSHA1() {
      String replacement1 = PSCryptographyUtils.getReplacementAlgorithm("SHA-1");
      assertEquals("SHA-256", replacement1, "SHA-1 should be replaced with SHA-256");

      String replacement2 = PSCryptographyUtils.getReplacementAlgorithm("SHA1");
      assertEquals("SHA-256", replacement2, "SHA1 should be replaced with SHA-256");
    }

    @Test
    @DisplayName("Should recommend AES for DES")
    void testReplaceDES() {
      String replacement = PSCryptographyUtils.getReplacementAlgorithm("DES");
      assertEquals("AES", replacement, "DES should be replaced with AES");
    }

    @Test
    @DisplayName("Should recommend AES for RC4")
    void testReplaceRC4() {
      String replacement = PSCryptographyUtils.getReplacementAlgorithm("RC4");
      assertEquals("AES", replacement, "RC4 should be replaced with AES");
    }

    @Test
    @DisplayName("Should default to SHA-256 for unknown algorithms")
    void testDefaultToSHA256() {
      String replacement = PSCryptographyUtils.getReplacementAlgorithm("UNKNOWN");
      assertEquals("SHA-256", replacement, "Unknown algorithm should default to SHA-256");
    }

    @Test
    @DisplayName("Should handle null input gracefully")
    void testNullAlgorithmName() {
      String replacement = PSCryptographyUtils.getReplacementAlgorithm(null);
      assertEquals("SHA-256", replacement, "Null algorithm should default to SHA-256");
    }

    @Test
    @DisplayName("Should handle blank input gracefully")
    void testBlankAlgorithmName() {
      String replacement = PSCryptographyUtils.getReplacementAlgorithm("   ");
      assertEquals("SHA-256", replacement, "Blank algorithm should default to SHA-256");
    }
  }

  @Nested
  @DisplayName("Real-World Cryptography Scenarios")
  class RealWorldScenarios {

    @Test
    @DisplayName("Should hash passwords securely (SHA-256 as fallback)")
    void testPasswordHashingFallback() {
      String password = "MySecurePassword123!";
      String passwordHash = PSCryptographyUtils.sha256Hex(password);

      // Verify hash is computed
      assertNotNull(passwordHash, "Password hash should be computed");
      assertEquals(64, passwordHash.length(), "Hash should be 64 hex characters");

      // Verify same password produces same hash
      String passwordHash2 = PSCryptographyUtils.sha256Hex(password);
      assertEquals(passwordHash, passwordHash2, "Same password should produce same hash");
    }

    @Test
    @DisplayName("Should verify file integrity with SHA-256")
    void testFileIntegrityVerification() {
      byte[] fileContent =
          "File content to verify".getBytes(java.nio.charset.StandardCharsets.UTF_8);
      String originalHash = PSCryptographyUtils.sha256Hex(fileContent);

      // Verify file hasn't changed
      String currentHash = PSCryptographyUtils.sha256Hex(fileContent);
      assertEquals(originalHash, currentHash, "File should have same hash if content unchanged");

      // Verify file changed is detected
      byte[] modifiedContent = "Modified content".getBytes(java.nio.charset.StandardCharsets.UTF_8);
      String modifiedHash = PSCryptographyUtils.sha256Hex(modifiedContent);
      assertNotEquals(originalHash, modifiedHash, "Modified file should have different hash");
    }

    @Test
    @DisplayName("Should generate secure salts for bcrypt")
    void testSecureSaltForPasswordHashing() {
      byte[] salt = PSCryptographyUtils.generateRandomSalt(16);

      assertNotNull(salt, "Salt should be generated");
      assertEquals(16, salt.length, "Salt should be 16 bytes");

      // Verify randomness
      byte[] salt2 = PSCryptographyUtils.generateRandomSalt(16);
      assertNotEquals(
          java.util.Arrays.toString(salt),
          java.util.Arrays.toString(salt2),
          "Salts should be different");
    }

    @Test
    @DisplayName("Should prevent MD5 usage in code validation")
    void testPreventMD5InCodeValidation() {
      // Simulate code review checking for weak algorithms
      String[] legacyCode = {
        "MessageDigest.getInstance(\"MD5\")",
        "MessageDigest.getInstance(\"SHA-1\")",
        "Cipher.getInstance(\"DES\")"
      };

      for (String line : legacyCode) {
        if (line.contains("MD5")) {
          assertThrows(
              IllegalArgumentException.class,
              () -> PSCryptographyUtils.isAlgorithmAllowed("MD5"),
              "MD5 in code should be detected as weak");
        }
      }
    }
  }
}
