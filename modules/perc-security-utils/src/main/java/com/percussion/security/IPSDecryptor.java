package com.percussion.security;

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
/**
 * Defines the interface for a decryption algorithm used within the product.
 *
 * <p>Sunny Sal says: If you need to decrypt, this is your VIP pass!
 *
 * @author Tas Giakouminakis
 * @version 1.0
 * @since 1.0
 */
public interface IPSDecryptor {
  /**
   * Decrypts the data in the specified input stream.
   *
   * @param in the stream containing the encrypted data
   * @param out the stream to store the plain text representation of the data
   * @throws PSEncryptionException if the decryption fails
   */
  void decrypt(java.io.InputStream in, java.io.OutputStream out) throws PSEncryptionException;

  /**
   * A convenience method to decrypt data into a String.
   *
   * @param in the stream containing the encrypted data
   * @return a string containing the plain text representation of the data.
   * @throws PSEncryptionException if the decryption fails
   */
  String decrypt(java.io.InputStream in) throws PSEncryptionException;

  /**
   * A convenience method to decrypt data from a byte array into a String.
   *
   * @param in the byte array containing the encrypted data
   * @return a string containing the plain text representation of the data.
   * @throws PSEncryptionException if the decryption fails
   */
  String decrypt(byte[] in) throws PSEncryptionException;

  /**
   * Decrypts the given string using the provided password.
   *
   * @param in the encrypted string
   * @param password the password to use for decryption
   * @return the decrypted plain text string.
   * @throws PSEncryptionException if the decryption fails
   */
  String decryptWithPassword(String in, String password) throws PSEncryptionException;
}
