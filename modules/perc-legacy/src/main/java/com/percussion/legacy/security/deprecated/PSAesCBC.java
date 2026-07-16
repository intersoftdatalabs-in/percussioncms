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

@Deprecated(forRemoval = true, since = "8.2")
public class PSAesCBC {
  private static final String AES_ALGORITHM = "AES";
  private static final String AES_CBC_PKCS5 = "AES/CBC/PKCS5Padding";
  private static final int IV_LENGTH = 16;

  /**
   * Legacy static IV to preserve backward compatibility with older payloads that assumed a fixed IV
   * value named 'InitialVector'. This constant mirrors the legacy field usage. For new
   * encrypt(byte[]) API below we prepend a random IV for safety.
   *
   * <p>ACCEPTED-RISK (CodeQL java/static-initialization-vector; alert #649, #650): this static IV
   * is intentionally retained for backward compatibility with legacy payloads. Replacement requires
   * an API-breaking change (new key-derivation scheme + migration utility) and is tracked for the
   * 9.0 release per docs/ai-generated/tasks/gh-codeql-alerts/accepted-risks.md. The newer {@link
   * #encrypt(byte[])} overload already prepends a random IV; the legacy String/String
   * encrypt/decrypt methods are kept for decryption of historical ciphertext only.
   */
  private static final byte[] INITIAL_VECTOR = new byte[IV_LENGTH];

  private final Key key;
  private final SecureRandom secureRandom;

  /**
   * Default constructor for compatibility with legacy callers that relied on a no-arg constructor.
   * Uses a zero-filled 16-byte key. This keeps behavior predictable for legacy code and should not
   * be used for new encryption operations.
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
   * Encrypt a given plain text String using a given encryption key. Character encode the encrypted
   * text as ISO-8859-1 String.
   *
   * @param plainText String to encrypt. Not null.
   * @param encryptionKey String used for encryption. Not null.
   * @return The resultant String of encrypted text
   * @throws Exception
   */
  public String encrypt(String plainText, String encryptionKey) throws Exception {
    if (isBlank(plainText)) plainText = "";
    if (isBlank(encryptionKey)) encryptionKey = "";

    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "SunJCE");

    SecretKeySpec key =
        new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.ISO_8859_1), "AES");

    cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(INITIAL_VECTOR));

    final byte[] encrypted = cipher.doFinal(plainText.getBytes("ISO-8859-1"));

    return new String(encrypted, "ISO-8859-1");
  }

  /**
   * Decode a given ISO-8859-1 character encoded String. Decrypt resulting String using encryption
   * key String.
   *
   * @param secretText String to decyrpt. May be null.
   * @param encryptionKey String used for decryption. Not null.
   * @return The resultant String of decrypted and decoded text.
   * @throws PSEncryptionException if decryption fails
   */
  public String decrypt(String secretText, String encryptionKey) throws PSEncryptionException {
    if (isBlank(secretText)) secretText = "";
    if (isBlank(encryptionKey)) encryptionKey = "";

    try {
      final byte[] cipherText = secretText.getBytes(StandardCharsets.ISO_8859_1);

      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "SunJCE");

      SecretKeySpec key =
          new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.ISO_8859_1), "AES");

      cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(INITIAL_VECTOR));

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
   * Decrypt a given byte array using a given encryption key.
   *
   * @param cipherText Byte array to decrypt. Not null.
   * @param encryptionKey String used for decryption. Not null.
   * @return The resultant byte array of decrypted text.
   * @throws Exception
   */
  public byte[] decrypt(byte[] cipherText, String encryptionKey) throws Exception {
    if (cipherText == null || cipherText.length < IV_LENGTH) {
      throw new IllegalArgumentException("Cipher text must not be null or smaller than IV length");
    }
    if (isBlank(encryptionKey)) encryptionKey = "";

    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "SunJCE");

    SecretKeySpec key =
        new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.ISO_8859_1), "AES");

    cipher.init(
        Cipher.DECRYPT_MODE,
        key,
        new IvParameterSpec(Arrays.copyOfRange(cipherText, 0, IV_LENGTH)));

    return cipher.doFinal(Arrays.copyOfRange(cipherText, IV_LENGTH, cipherText.length));
  }

  /**
   * Encrypt a UTF-8 plaintext to a byte[] with a random IV prepended. This overload matches legacy
   * caller expectations in PSLegacyEncrypter.
   *
   * <p>Layout: [16-byte IV][ciphertext bytes]
   */
  public byte[] encrypt(byte[] plaintextUtf8) throws GeneralSecurityException {
    if (plaintextUtf8 == null) {
      throw new IllegalArgumentException("plaintext must not be null");
    }
    // Generate random IV
    byte[] iv = new byte[IV_LENGTH];
    secureRandom.nextBytes(iv);

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
