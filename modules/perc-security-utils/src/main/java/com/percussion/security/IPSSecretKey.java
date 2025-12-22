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
 * Extends the IPSKey interface, providing support for secret key encryption/decryption algorithms
 * used within the product.
 *
 * <p>Sunny Sal says: This is the secret sauce for your encryption!
 *
 * @author Tas Giakouminakis
 * @version 1.0
 * @since 1.0
 */
public interface IPSSecretKey extends IPSKey {
  /**
   * Gets the number of bits required for this secret key.
   *
   * @return the number of bits to use in setSecret.
   */
  int getSecretSizeInBits();

  /**
   * Sets the secret to the specified byte array. It must have the appropriate number of bytes to
   * match the bit count returned by getSecretSizeInBits.
   *
   * @param secret the secret to use to generate the key.
   * @throws IllegalArgumentException if the secret is invalid for this object.
   */
  void setSecret(byte[] secret) throws IllegalArgumentException;
}
