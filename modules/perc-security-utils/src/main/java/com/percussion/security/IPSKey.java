package com.percussion.security;

import javax.crypto.SecretKey;

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
 * Represents a key for encryption/decryption algorithms used within the product.
 *
 * @author Tas Giakouminakis
 * @version 1.0
 * @since 1.0
 */
public interface IPSKey {
  /**
   * Generates an IPSEncryptor object which can make use of this key.
   *
   * @return the associated encryptor.
   */
  IPSEncryptor getEncryptor();

  /**
   * Generates an IPSDecryptor object which can make use of this key.
   *
   * @return the associated decryptor.
   */
  IPSDecryptor getDecryptor();

  /**
   * Returns a byte array containing the secret key.
   *
   * @return the secret key as a byte array.
   */
  byte[] getSecret();

  /**
   * Sets the secret to the specified byte array.
   *
   * @param secret the secret key as a byte array.
   */
  void setSecret(byte[] secret);

  /**
   * Generates a new key.
   *
   * @return a byte array containing the new encryption key
   */
  public SecretKey generateKey();
}
