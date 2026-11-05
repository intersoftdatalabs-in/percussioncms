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
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Deprecated: Use {@link PSCryptographyUtils} for secure cryptographic hashing.
 *
 * <p>This class previously calculated a running MD5 digest but has been refactored to verify using
 * SHA-256 for improved security (CWE-327: Weak Cryptography).
 *
 * <p>This class calculates a running SHA-256 digest of the data read. When the stream is closed the
 * calculated digest is passed to a HashVerifier which is expected to verify this digest and to
 * throw an Exception if it fails.
 *
 * @version 0.3-3 06/05/2001
 * @author Ronald Tschalär
 * @deprecated Use {@link PSCryptographyUtils} instead
 */
@Deprecated(forRemoval = true)
class MD5InputStream extends FilterInputStream {
  private HashVerifier verifier;
  private ByteArrayOutputStream buffer;
  private long rcvd = 0;
  private boolean closed = false;

  /**
   * @param is the input stream over which the SHA-256 hash is to be calculated
   * @param verifier the HashVerifier to invoke when the stream is closed
   * @deprecated Use {@link PSCryptographyUtils} instead
   */
  public MD5InputStream(InputStream is, HashVerifier verifier) {
    super(is);
    this.verifier = verifier;
    this.buffer = new ByteArrayOutputStream();
  }

  public synchronized int read() throws IOException {
    int b = in.read();
    if (b != -1) {
      buffer.write(b);
    } else {
      real_close();
    }
    rcvd++;
    return b;
  }

  public synchronized int read(byte[] buf, int off, int len) throws IOException {
    int num = in.read(buf, off, len);
    if (num > 0) {
      buffer.write(buf, off, num);
    } else {
      real_close();
    }
    rcvd += num;
    return num;
  }

  public synchronized long skip(long num) throws IOException {
    byte[] tmp = new byte[(int) num];
    int got = read(tmp, 0, (int) num);

    if (got > 0) return (long) got;
    else return 0L;
  }

  /**
   * Close the stream and check the digest. If the stream has not been fully read then the rest of
   * the data will first be read (and discarded) to complete the digest calculation.
   *
   * @exception IOException if the close()'ing the underlying stream throws an IOException, or if
   *     the expected digest and the calculated digest don't match.
   */
  public synchronized void close() throws IOException {
    while (skip(10000) > 0)
      ;
    real_close();
  }

  /**
   * Close the stream and check the digest. Computes SHA-256 hash of buffered data.
   *
   * @exception IOException if the close()'ing the underlying stream throws an IOException, or if
   *     the expected digest and the calculated digest don't match.
   */
  private void real_close() throws IOException {
    if (closed) return;
    closed = true;

    in.close();
    // Compute SHA-256 hash of buffered data and convert to bytes
    byte[] data = buffer.toByteArray();
    String hashHex = PSCryptographyUtils.sha256Hex(data);
    byte[] hashBytes = hexStringToByteArray(hashHex);
    verifier.verifyHash(hashBytes, rcvd);
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
}
