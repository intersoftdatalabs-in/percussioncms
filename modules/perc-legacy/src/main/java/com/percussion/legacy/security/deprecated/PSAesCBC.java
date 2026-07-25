/*
 * Copyright 1999-2025 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 *
 * See the License for the specific language governing permissions and limitations under the
 * License.
 */

package com.percussion.legacy.security.deprecated;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.percussion.security.PSEncryptionException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Legacy AES/CBC helpers retained solely so upgrades can decrypt historical ciphertext written by
 * older Percussion CMS releases.
 *
 * <p><b>Do not use for new secrets.</b> All new encryption MUST go through {@code
 * com.percussion.security.PSEncryptor} (AES/GCM with a random IV). Production call sites of this
 * class (S3 delivery, pub-server service) invoke {@link #decrypt(String, String)} only as a
 * fallback after {@code PSEncryptor.decryptString} fails — never to mint new ciphertext.
 *
 * <p>ACCEPTED-RISK (CodeQL {@code java/weak-cryptographic-algorithm} alerts #757–#759 and {@code
 * java/static-initialization-vector} alerts #649–#650): AES/CBC and the historical fixed IV are
 * intentionally preserved for wire-compatible decryption of pre-8.2 payloads. Removing or changing
 * them would brick upgrades of customer installs that still hold legacy-encrypted properties.
 * Tracked for removal in release 9.0 once a one-shot migration has re-encrypted remaining legacy
 * stores; see {@code docs/ai-generated/tasks/gh-codeql-alerts/accepted-risks.md}.
 *
 * @deprecated for removal in 9.0; decrypt-only upgrade path. Use {@code PSEncryptor} for all new
 *     encryption.
 */
@Deprecated(forRemoval = true, since = "8.2")
public class PSAesCBC {
  private static final String AES_ALGORITHM = "AES";
  private static final String AES_CBC_PKCS5 = "AES/CBC/PKCS5Padding";
  private static final int IV_LENGTH = 16;

  /**
   * Legacy static IV required for wire-compatible decrypt of historical String/String payloads that
   * assumed a fixed IV named {@code InitialVector}. Zero-filled 16 bytes matches the pre-8.2
   * behavior.
   *
   * <p>ACCEPTED-RISK (CodeQL {@code java/static-initialization-vector}; alerts #649, #650):
   * retained only so {@link #decrypt(String, String)} (and the matching {@link #encrypt(String,
   * String)} test/fixture helper) can round-trip historical ciphertext. The newer {@link
   * #encrypt(byte[])} overload prepends a random IV; new product encryption must use {@code
   * PSEncryptor} (AES/GCM).
   */
  private static final byte[] INITIAL_VECTOR = new byte[IV_LENGTH];

  private final Key key;
  private final SecureRandom secureRandom;

  /**
   * Default constructor for compatibility with legacy callers that relied on a no-arg constructor.
   * Uses a zero-filled 16-byte key. Predictable for historical decrypt only — never for new
   * encryption.
   */
  public PSAesCBC() {
    this(new byte[16]);
  }

  public PSAesCBC(byte[] rawKey) {
    if (rawKey == null || rawKey.length == 0) {
      throw new IllegalArgumentException("Key must not be null or empty");
    }
    this.key = new SecretKeySpec(Arrays.copyOf(rawKey, 16), AES_ALGORITHM);
    this.secureRandom = new SecureRandom();
  }

  /**
   * Encrypt a plain-text String using the historical fixed-IV AES/CBC layout.
   *
   * <p><b>Not for production new secrets.</b> Kept so unit tests and migration tooling can recreate
   * legacy ciphertext. Runtime product code must encrypt with {@code PSEncryptor}.
   *
   * @param plainText String to encrypt. Not null.
   * @param encryptionKey String used for encryption. Not null.
   * @return The resultant String of encrypted text
   * @throws Exception if encryption fails
   * @deprecated decrypt-only upgrade path; do not encrypt new data with this method
   */
  @Deprecated(forRemoval = true, since = "8.2")
  public String encrypt(String plainText, String encryptionKey) throws Exception {
    if (isBlank(plainText)) plainText = "";
    if (isBlank(encryptionKey)) encryptionKey = "";

    Cipher cipher =
        Cipher.getInstance(
            "AES/CBC/PKCS5Padding",
            "SunJCE"); // codeql[java/weak-cryptographic-algorithm] justification: ACCEPTED-RISK
    // legacy upgrade path only; new secrets use PSEncryptor AES/GCM (alert #757)

    SecretKeySpec key =
        new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.ISO_8859_1), "AES");

    cipher.init(
        Cipher.ENCRYPT_MODE,
        key,
        new IvParameterSpec(
            INITIAL_VECTOR)); // codeql[java/static-initialization-vector] justification:
    // ACCEPTED-RISK fixed IV required for wire-compatible legacy
    // ciphertext (alert #649)

    final byte[] encrypted = cipher.doFinal(plainText.getBytes("ISO-8859-1"));

    return new String(encrypted, "ISO-8859-1");
  }

  /**
   * Decode a given ISO-8859-1 character encoded String. Decrypt resulting String using encryption
   * key String.
   *
   * <p>This is the method production upgrade/fallback paths call after modern {@code PSEncryptor}
   * decryption fails (older installs still hold AES/CBC ciphertext).
   *
   * @param secretText String to decrypt. May be null.
   * @param encryptionKey String used for decryption. Not null.
   * @return The resultant String of decrypted and decoded text.
   * @throws PSEncryptionException if decryption fails
   */
  public String decrypt(String secretText, String encryptionKey) throws PSEncryptionException {
    if (isBlank(secretText)) secretText = "";
    if (isBlank(encryptionKey)) encryptionKey = "";

    try {
      final byte[] cipherText = secretText.getBytes(StandardCharsets.ISO_8859_1);

      Cipher cipher =
          Cipher.getInstance(
              "AES/CBC/PKCS5Padding",
              "SunJCE"); // codeql[java/weak-cryptographic-algorithm] justification: ACCEPTED-RISK
      // decrypt pre-8.2 payloads during upgrade only (alert #758)

      SecretKeySpec key =
          new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.ISO_8859_1), "AES");

      cipher.init(
          Cipher.DECRYPT_MODE,
          key,
          new IvParameterSpec(
              INITIAL_VECTOR)); // codeql[java/static-initialization-vector] justification:
      // ACCEPTED-RISK fixed IV matches historical ciphertext layout
      // (alert #650)

      return new String(cipher.doFinal(cipherText), StandardCharsets.ISO_8859_1);
    } catch (InvalidAlgorithmParameterException
        | NoSuchAlgorithmException
        | BadPaddingException
        | NoSuchProviderException
        | InvalidKeyException
        | NoSuchPaddingException
        | IllegalBlockSizeException e) {
      throw new PSEncryptionException(e);
    }
  }

  /**
   * Decrypt a given byte array using a given encryption key. Expects layout {@code [16-byte
   * IV][ciphertext]} as produced by {@link #encrypt(byte[])}.
   *
   * @param cipherText Byte array to decrypt. Not null.
   * @param encryptionKey String used for decryption. Not null.
   * @return The resultant byte array of decrypted text.
   * @throws Exception if decryption fails
   */
  public byte[] decrypt(byte[] cipherText, String encryptionKey) throws Exception {
    if (cipherText == null || cipherText.length < IV_LENGTH) {
      throw new IllegalArgumentException("Cipher text must not be null or smaller than IV length");
    }
    if (isBlank(encryptionKey)) encryptionKey = "";

    Cipher cipher =
        Cipher.getInstance(
            "AES/CBC/PKCS5Padding",
            "SunJCE"); // codeql[java/weak-cryptographic-algorithm] justification: ACCEPTED-RISK
    // decrypt historical byte[] payloads only (alert #759)

    SecretKeySpec key =
        new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.ISO_8859_1), "AES");

    cipher.init(
        Cipher.DECRYPT_MODE,
        key,
        new IvParameterSpec(Arrays.copyOfRange(cipherText, 0, IV_LENGTH)));

    return cipher.doFinal(Arrays.copyOfRange(cipherText, IV_LENGTH, cipherText.length));
  }

  /**
   * Encrypt a UTF-8 plaintext to a byte[] with a random IV prepended. Layout: {@code [16-byte
   * IV][ciphertext bytes]}.
   *
   * <p>Still AES/CBC (weak relative to GCM). Prefer {@code PSEncryptor} for any new product
   * ciphertext. Kept for {@link PSLegacyEncrypter} compatibility during migration.
   */
  public byte[] encrypt(byte[] plaintextUtf8) throws GeneralSecurityException {
    if (plaintextUtf8 == null) {
      throw new IllegalArgumentException("plaintext must not be null");
    }
    // Generate random IV
    byte[] iv = new byte[IV_LENGTH];
    secureRandom.nextBytes(iv);

    // codeql[java/weak-cryptographic-algorithm] ACCEPTED-RISK: AES/CBC retained for
    // PSLegacyEncrypter migration; new secrets use PSEncryptor AES/GCM
    Cipher cipher = Cipher.getInstance(AES_CBC_PKCS5, "SunJCE");
    cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
    byte[] ct = cipher.doFinal(plaintextUtf8);

    // Prepend IV
    byte[] out = new byte[IV_LENGTH + ct.length];
    System.arraycopy(iv, 0, out, 0, IV_LENGTH);
    System.arraycopy(ct, 0, out, IV_LENGTH, ct.length);
    return out;
  }
}
