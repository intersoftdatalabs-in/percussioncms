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

package com.percussion.HTTPClient;

import com.percussion.security.utils.PSCryptographyUtils;
import java.io.UnsupportedEncodingException;

/**
 * Deprecated: Use {@link PSCryptographyUtils} for secure cryptographic hashing.
 *
 * <p>This class previously provided MD5 hashing but has been refactored to use SHA-256 for
 * improved security (CWE-327: Weak Cryptography).
 *
 * Some utility methods for digesting info using SHA-256 (replaces legacy MD5).
 *
 * @version 0.3-3 06/05/2001
 * @author Ronald Tschalär
 * @since V0.3-3
 * @deprecated Use {@link PSCryptographyUtils} instead
 */
@Deprecated(forRemoval = true)
class MD5 {
  private static final char[] hex = {
    '0', '1', '2', '3', '4', '5', '6', '7',
    '8', '9', 'a', 'b', 'c', 'd', 'e', 'f',
  };

  /**
   * Turns array of bytes into string representing each byte as unsigned hex number.
   *
   * @param hash array of bytes to convert to hex-string
   * @return generated hex string
   */
  public static final String toHex(byte hash[]) {
    StringBuilder buf = new StringBuilder(hash.length * 2);

    for (int idx = 0; idx < hash.length; idx++)
      buf.append(hex[(hash[idx] >> 4) & 0x0f]).append(hex[hash[idx] & 0x0f]);

    return buf.toString();
  }

  /**
   * Digest the input using SHA-256 (replaces legacy MD5).
   *
   * @param input the data to be digested.
   * @return the SHA-256-digested input as bytes
   * @deprecated Use {@link PSCryptographyUtils#sha256Hex(byte[])} instead
   */
  @Deprecated(forRemoval = true)
  public static final byte[] digest(byte[] input) {
    // Convert to SHA-256 hash and return as bytes
    String hashHex = PSCryptographyUtils.sha256Hex(input);
    return hexStringToByteArray(hashHex);
  }

  /**
   * Converts a hex string to a byte array.
   *
   * @param hex the hex string
   * @return the byte array
   */
  private static byte[] hexStringToByteArray(String hex) {
    byte[] bytes = new byte[hex.length() / 2];
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
    }
    return bytes;
  }

  /**
   * Digest the input using SHA-256 (replaces legacy MD5).
   *
   * @param input1 the first part of the data to be digested.
   * @param input2 the second part of the data to be digested.
   * @return the SHA-256-digested input as bytes
   * @deprecated Use {@link PSCryptographyUtils#sha256Hex(byte[])} instead
   */
  @Deprecated(forRemoval = true)
  public static final byte[] digest(byte[] input1, byte[] input2) {
    // Combine inputs and hash with SHA-256
    byte[] combined = new byte[input1.length + input2.length];
    System.arraycopy(input1, 0, combined, 0, input1.length);
    System.arraycopy(input2, 0, combined, input1.length, input2.length);
    String hashHex = PSCryptographyUtils.sha256Hex(combined);
    return hexStringToByteArray(hashHex);
  }

  /**
   * Digest the input.
   *
   * @param input the data to be digested.
   * @return the md5-digested input as a hex string
   */
  public static final String hexDigest(byte[] input) {
    return toHex(digest(input));
  }

  /**
   * Digest the input.
   *
   * @param input1 the first part of the data to be digested.
   * @param input2 the second part of the data to be digested.
   * @return the md5-digested input as a hex string
   */
  public static final String hexDigest(byte[] input1, byte[] input2) {
    return toHex(digest(input1, input2));
  }

  /**
   * Digest the input.
   *
   * @param input the data to be digested.
   * @return the md5-digested input as a hex string
   */
  public static final byte[] digest(String input) {
    try {
      return digest(input.getBytes("8859_1"));
    } catch (UnsupportedEncodingException uee) {
      throw new Error(uee.toString());
    }
  }

  /**
   * Digest the input.
   *
   * @param input the data to be digested.
   * @return the md5-digested input as a hex string
   */
  public static final String hexDigest(String input) {
    try {
      return toHex(digest(input.getBytes("8859_1")));
    } catch (UnsupportedEncodingException uee) {
      throw new Error(uee.toString());
    }
  }
}
