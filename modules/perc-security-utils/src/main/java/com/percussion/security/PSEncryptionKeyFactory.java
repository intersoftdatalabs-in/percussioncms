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

/** Factory for encryption key generation. */
public abstract class PSEncryptionKeyFactory {

  /**
   * @deprecated DES is no longer considered secure
   */
  @Deprecated public static final String DES_ALGORITHM = "DES";

  /** The AES-GCM algorithm identifier */
  public static final String AES_GCM_ALGORIYTHM = "AES";

  private PSEncryptionKeyFactory() {
    super();
  }

  /**
   * Get an instance of the key generator which can be used for the default encryption/decryption
   * algorithm. From the key type returned, the caller can determine what to use to generate the
   * key. The updated key can then be passed in to the encryptor/decryptor.
   */
  /**
   * Get an instance of the key generator which can be used for the specified encryption/decryption
   * algorithm. From the key type returned, the caller can determine what to use to generate the
   * key. The updated key can then be passed in to the encryptor/decryptor.
   *
   * @param algorithm the encryption algorithm to use (e.g., AES_GCM_ALGORIYTHM)
   * @return an IPSKey instance for the specified algorithm
   * @throws IllegalArgumentException if algorithm is null or not supported
   */
  public static IPSKey getKeyGenerator(String algorithm) {
    IPSKey key = null;

    if (algorithm == null) throw new IllegalArgumentException("Algorithm cannot be null.");

    if (!algorithm.equalsIgnoreCase(AES_GCM_ALGORIYTHM))
      throw new IllegalArgumentException("Algorithm not supported");

    key = new PSAESGCMKey();

    return key;
  }
}
