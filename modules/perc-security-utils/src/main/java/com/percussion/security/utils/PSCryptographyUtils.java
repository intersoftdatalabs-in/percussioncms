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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import org.apache.commons.lang3.StringUtils;

/**
 * Cryptography utility providing secure alternatives to weak algorithms.
 *
 * <p>This utility class replaces deprecated and weak cryptographic algorithms with modern, secure
 * alternatives:
 *
 * <ul>
 *   <li><strong>MD5</strong> → SHA-256 (for integrity checking, not passwords)
 *   <li><strong>SHA-1</strong> → SHA-256 (for integrity checking, not passwords)
 *   <li><strong>DES</strong> → AES-256 (for symmetric encryption)
 * </ul>
 *
 * <p><strong>Security Notes:</strong>
 *
 * <ul>
 *   <li>For password hashing: Use bcrypt, Argon2, or PBKDF2 instead of simple digest functions
 *   <li>For message authentication: Use HMAC-SHA256 instead of plain MessageDigest
 *   <li>For encryption: Always use authenticated encryption (AES-GCM)
 * </ul>
 *
 * <p><strong>CWE-327 (Broken/Risky Cryptographic Algorithm) Mitigation:</strong>
 *
 * <p>This utility prevents the use of weak algorithms identified in CodeQL security scans: - MD5,
 * SHA-1, DES, RC4 are explicitly rejected - Only modern, NIST-approved algorithms are recommended -
 * All methods validate algorithm strength
 *
 * @see <a href="https://cwe.mitre.org/data/definitions/327.html">CWE-327</a>
 * @see <a href="https://owasp.org/www-community/attacks/Brute_force_attack">OWASP - Brute Force
 *     Attack</a>
 * @since 8.2.0
 */
public final class PSCryptographyUtils {

  private static final HexFormat HEX_FORMAT = HexFormat.of().withUpperCase();
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  // Weak algorithms that should NOT be used
  private static final String[] WEAK_ALGORITHMS = {"MD5", "SHA-1", "SHA1", "DES", "RC4"};

  // Strong algorithms for different use cases
  private static final String HASH_ALGORITHM = "SHA-256";
  private static final String HASH_ALGORITHM_STRONG = "SHA-512";

  private PSCryptographyUtils() {
    // Utility class - no instantiation
  }

  /**
   * Computes a SHA-256 hash of the given data.
   *
   * <p>Use this method for: - Integrity verification (NOT password storage) - File checksums -
   * General message digests - Replacing MD5 or SHA-1 usage
   *
   * <p><strong>DO NOT use for password hashing:</strong> Use bcrypt or Argon2 instead.
   *
   * @param data The data to hash (must not be null)
   * @return Hexadecimal representation of the SHA-256 hash
   * @throws IllegalArgumentException if data is null
   * @throws RuntimeException if SHA-256 is not available (should not happen)
   */
  public static String sha256Hex(byte[] data) {
    if (data == null) {
      throw new IllegalArgumentException("Data to hash must not be null");
    }
    return hashHex(data, HASH_ALGORITHM);
  }

  /**
   * Computes a SHA-256 hash of the given string data (UTF-8 encoded).
   *
   * <p>Convenience method that automatically encodes the string as UTF-8.
   *
   * @param data The string to hash (must not be null)
   * @return Hexadecimal representation of the SHA-256 hash
   * @throws IllegalArgumentException if data is null
   */
  public static String sha256Hex(String data) {
    if (StringUtils.isBlank(data)) {
      throw new IllegalArgumentException("Data to hash must not be blank");
    }
    return sha256Hex(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  /**
   * Computes a SHA-512 hash of the given data (stronger than SHA-256).
   *
   * <p>Use this method when you need extra security margin: - Long-term security requirements -
   * Cryptographic proof time stamping - Performance is not critical
   *
   * @param data The data to hash (must not be null)
   * @return Hexadecimal representation of the SHA-512 hash
   * @throws IllegalArgumentException if data is null
   */
  public static String sha512Hex(byte[] data) {
    if (data == null) {
      throw new IllegalArgumentException("Data to hash must not be null");
    }
    return hashHex(data, HASH_ALGORITHM_STRONG);
  }

  /**
   * Computes a SHA-512 hash of the given string data (UTF-8 encoded).
   *
   * @param data The string to hash (must not be null)
   * @return Hexadecimal representation of the SHA-512 hash
   * @throws IllegalArgumentException if data is null
   */
  public static String sha512Hex(String data) {
    if (StringUtils.isBlank(data)) {
      throw new IllegalArgumentException("Data to hash must not be blank");
    }
    return sha512Hex(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  /**
   * Validates that an algorithm name is NOT a weak/deprecated algorithm.
   *
   * <p>Throws an exception if the algorithm is known to be weak: - MD5: Broken, collisions found -
   * SHA-1: Deprecated, SHAttered collision attack - DES: Too small key space (56-bit effective) -
   * RC4: Biased output, not secure for encryption
   *
   * @param algorithmName The name of the algorithm to validate
   * @return true if algorithm is allowed
   * @throws IllegalArgumentException if algorithm is weak or deprecated
   */
  public static boolean isAlgorithmAllowed(String algorithmName) {
    if (StringUtils.isBlank(algorithmName)) {
      throw new IllegalArgumentException("Algorithm name must not be blank");
    }

    String normalized = algorithmName.trim().toUpperCase();

    for (String weakAlgo : WEAK_ALGORITHMS) {
      if (normalized.contains(weakAlgo)) {
        throw new IllegalArgumentException(
            "Algorithm '"
                + algorithmName
                + "' is weak and not allowed. "
                + "Use SHA-256 (or SHA-512) instead of "
                + weakAlgo);
      }
    }

    return true;
  }

  /**
   * Generates a cryptographically random salt for password hashing.
   *
   * <p><strong>Note:</strong> This is for initializing password hashing libraries like bcrypt or
   * PBKDF2, NOT for direct use in password hashing.
   *
   * @param length Length of the salt in bytes (typically 16)
   * @return Random bytes suitable for use as a salt
   */
  public static byte[] generateRandomSalt(int length) {
    if (length <= 0) {
      throw new IllegalArgumentException("Salt length must be positive");
    }
    byte[] salt = new byte[length];
    SECURE_RANDOM.nextBytes(salt);
    return salt;
  }

  /**
   * Internal method to compute a hash using a specified algorithm.
   *
   * @param data The data to hash
   * @param algorithm The algorithm to use (must be allowed)
   * @return Hexadecimal representation of the hash
   */
  private static String hashHex(byte[] data, String algorithm) {
    isAlgorithmAllowed(algorithm); // Validate algorithm
    try {
      MessageDigest digest = MessageDigest.getInstance(algorithm);
      byte[] hash = digest.digest(data);
      return HEX_FORMAT.formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Algorithm " + algorithm + " not available", e);
    }
  }

  /**
   * Migration helper: Test if code is trying to use a weak algorithm.
   *
   * <p>Use this method during code review to identify legacy calls that need migration.
   *
   * @param legacyAlgorithm The algorithm name from legacy code
   * @return Recommended replacement algorithm
   */
  public static String getReplacementAlgorithm(String legacyAlgorithm) {
    if (StringUtils.isBlank(legacyAlgorithm)) {
      return HASH_ALGORITHM;
    }

    String normalized = legacyAlgorithm.trim().toUpperCase();

    if (normalized.contains("MD5")) {
      return HASH_ALGORITHM; // MD5 → SHA-256
    } else if (normalized.contains("SHA-1") || normalized.contains("SHA1")) {
      return HASH_ALGORITHM; // SHA-1 → SHA-256
    } else if (normalized.contains("DES")) {
      return "AES"; // DES → AES
    } else if (normalized.contains("RC4")) {
      return "AES"; // RC4 → AES
    }

    return HASH_ALGORITHM; // Default to SHA-256
  }
}
