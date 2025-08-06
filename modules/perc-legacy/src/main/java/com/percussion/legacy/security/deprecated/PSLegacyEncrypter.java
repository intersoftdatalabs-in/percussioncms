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

package com.percussion.legacy.security.deprecated;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

/**
 * Utility class to encrypt and decrypt strings using the Blowfish cipher with a 
 * secret shared key.
 * @deprecated Only use for upgrade from prior versions
 */
@Deprecated
public class PSLegacyEncrypter {
   private final PSAesCBC aes;
   
   @Deprecated
   public PSLegacyEncrypter(byte[] rawKey) {
      this.aes = new PSAesCBC(rawKey);
   }
   
   /**
    * Encrypts the given plain text and returns Base64-encoded ciphertext.
    * Legacy string-based API retained for backward compatibility.
    */
   public String encryptToBase64(String plain) {
      try {
         return Base64.getEncoder().encodeToString(aes.encrypt(plain.getBytes(StandardCharsets.UTF_8)));
      } catch (Exception e) {
         throw new IllegalStateException("Failed to encrypt to Base64", e);
      }
   }
   
   /**
    * Decrypts the given Base64-encoded ciphertext and returns the plain text.
    * Convenience counterpart to encryptToBase64 retained for legacy callers.
    */
   public String decryptFromBase64(String base64Cipher) {
      try {
         byte[] cipher = Base64.getDecoder().decode(base64Cipher);
         byte[] plain = aes.decrypt(cipher, "");
         return new String(plain, StandardCharsets.UTF_8);
      } catch (Exception e) {
         throw new IllegalStateException("Failed to decrypt from Base64", e);
      }
   }
   
   /**
    * Legacy convenience that mirrors historical string round-trip behavior.
    */
   @Deprecated
   public String encrypt(String plain) {
      return encryptToBase64(plain);
   }
   
   /**
    * Legacy convenience that mirrors historical string round-trip behavior.
    */
   @Deprecated
   public String decrypt(String cipher) {
      return decryptFromBase64(cipher);
   }
   
   /**
    * Byte-array encryption using random IV with IV prepended to ciphertext.
    */
   public byte[] encrypt(byte[] data) throws Exception {
      return aes.encrypt(data);
   }
   
   /**
    * Byte-array decryption that expects the IV to be prepended to the ciphertext.
    */
   public byte[] decrypt(byte[] cipher) throws Exception {
      return aes.decrypt(cipher, "");
   }
}
