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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.security;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class PSAESGCMDecryptor implements IPSDecryptor {

  private PSAESGCMKey m_key = null;

  /**
   * Construct a AES decryptor using the specified DES key.
   *
   * @param key the AES key to use for decryption
   * @throws IllegalArgumentException if <code>key</code> is <code>null</code>
   */
  public PSAESGCMDecryptor(PSAESGCMKey key) throws IllegalArgumentException {
    if (key == null) throw new IllegalArgumentException("key cannot be null");

    // store key for later use
    m_key = key;
  }

  /**
   * Decrypt the data in the specified input stream.
   *
   * @param in the stream containing the encrypted data
   * @param out the stream to store the plain text representation of the data
   */
  @Override
  public void decrypt(InputStream in, OutputStream out) {}

  /**
   * A convenience method to decrypt data into a String.
   *
   * @param in the stream containing the encrypted data
   * @return a string containing the plain text representation of the data
   */
  @Override
  public String decrypt(InputStream in) {
    return null;
  }

  /**
   * A convenience method to decrypt data from a byte array into a String.
   *
   * @param in the byte array containing the encrypted data
   * @return a string containing the plain text representation of the data
   * @throws PSEncryptionException if decryption fails
   */
  @Override
  public String decrypt(byte[] in) throws PSEncryptionException {
    final int ivLength = 12;
    if (in == null || in.length <= ivLength) {
      throw new PSEncryptionException("Input too short for AES-GCM decryption");
    }
    byte[] iv = new byte[ivLength];
    System.arraycopy(in, 0, iv, 0, ivLength);
    byte[] cipherText = new byte[in.length - ivLength];
    System.arraycopy(in, ivLength, cipherText, 0, cipherText.length);
    try {
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      PSAESGCMKey aesKey = m_key;
      cipher.init(Cipher.DECRYPT_MODE, aesKey.getSecretKey(), new GCMParameterSpec(128, iv));
      byte[] plainText = cipher.doFinal(cipherText);
      return new String(plainText, StandardCharsets.UTF_8);
    } catch (NoSuchAlgorithmException
        | NoSuchPaddingException
        | BadPaddingException
        | IllegalBlockSizeException
        | InvalidAlgorithmParameterException
        | InvalidKeyException e) {
      throw new PSEncryptionException("AES-GCM decryption failed: " + e.getMessage(), e);
    }
  }

  @Override
  public String decryptWithPassword(String in, String password) throws PSEncryptionException {
    try {
      byte[] decoded = Base64.getDecoder().decode(in.getBytes(StandardCharsets.UTF_8));
      final int ivLength = 12;
      final int saltLength = 16;
      if (decoded.length <= ivLength + saltLength) {
        throw new PSEncryptionException("Input too short for AES-GCM decryption with password");
      }
      byte[] iv = new byte[ivLength];
      System.arraycopy(decoded, 0, iv, 0, ivLength);
      byte[] salt = new byte[saltLength];
      System.arraycopy(decoded, ivLength, salt, 0, saltLength);
      byte[] encryptedText = new byte[decoded.length - ivLength - saltLength];
      System.arraycopy(decoded, ivLength + saltLength, encryptedText, 0, encryptedText.length);
      SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
      KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 1000, 256);
      SecretKey secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, secret, new GCMParameterSpec(128, iv));
      return new String(cipher.doFinal(encryptedText), StandardCharsets.UTF_8);
    } catch (InvalidKeySpecException
        | NoSuchAlgorithmException
        | BadPaddingException
        | InvalidKeyException
        | InvalidAlgorithmParameterException
        | NoSuchPaddingException
        | IllegalBlockSizeException e) {
      throw new PSEncryptionException(e.getMessage(), e);
    }
  }
}
