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
package com.percussion.utils.jsr170;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.jcr.Binary;
import javax.jcr.RepositoryException;

/**
 * JCR 2.0 {@link Binary} adapter over an {@link InputStream}. Bytes are buffered on construction so
 * {@link #getStream()} may be called more than once and {@link #getSize()} is available.
 *
 * @author percussion
 */
public class PSBinary implements Binary {

  private final byte[] m_data;
  private boolean m_disposed;

  /**
   * @param stream source stream, never {@code null}; fully read and closed by this constructor
   * @throws RepositoryException if the stream cannot be read
   */
  public PSBinary(InputStream stream) throws RepositoryException {
    if (stream == null) {
      throw new IllegalArgumentException("stream may not be null");
    }
    try (InputStream in = stream;
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      byte[] buf = new byte[8192];
      int n;
      while ((n = in.read(buf)) >= 0) {
        out.write(buf, 0, n);
      }
      m_data = out.toByteArray();
    } catch (IOException e) {
      throw new RepositoryException("Unable to read binary stream", e);
    }
  }

  /**
   * @param data binary payload, never {@code null}
   */
  public PSBinary(byte[] data) {
    if (data == null) {
      throw new IllegalArgumentException("data may not be null");
    }
    m_data = data.clone();
  }

  @Override
  public InputStream getStream() throws RepositoryException {
    checkDisposed();
    return new ByteArrayInputStream(m_data);
  }

  @Override
  public int read(byte[] b, long position) throws IOException, RepositoryException {
    checkDisposed();
    if (b == null) {
      throw new IllegalArgumentException("b may not be null");
    }
    if (position < 0) {
      throw new IllegalArgumentException("position may not be negative");
    }
    if (position >= m_data.length) {
      return -1;
    }
    int pos = (int) position;
    int len = Math.min(b.length, m_data.length - pos);
    System.arraycopy(m_data, pos, b, 0, len);
    return len;
  }

  @Override
  public long getSize() throws RepositoryException {
    checkDisposed();
    return m_data.length;
  }

  @Override
  public void dispose() {
    m_disposed = true;
  }

  private void checkDisposed() throws RepositoryException {
    if (m_disposed) {
      throw new RepositoryException("Binary has been disposed");
    }
  }
}
