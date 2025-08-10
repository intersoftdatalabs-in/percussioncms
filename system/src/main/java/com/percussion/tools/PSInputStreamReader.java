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

package com.percussion.tools;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.PushbackInputStream;

/**
 * Allows the readLine method to be called on an input stream.
 */
public class PSInputStreamReader extends PushbackInputStream {
  /**
   * Constructs an input stream reader for the specified stream. Optionally wraps in a buffered stream.
   * The underlying pushback stream buffer defaults to 1 byte.
   *
   * @param in the input stream to wrap
   * @param noBuffer true to avoid wrapping in BufferedInputStream
   */
  public PSInputStreamReader(InputStream in, boolean noBuffer) {
    this(in, noBuffer, 1);
  }

  /**
   * Constructs an input stream reader for the specified stream. Always wraps in BufferedInputStream.
   * The underlying pushback stream buffer defaults to 1 byte.
   *
   * @param in the input stream to wrap
   */
  public PSInputStreamReader(InputStream in) {
    this(in, false, 1);
  }

  /**
   * Constructs an input stream reader for the specified stream. Optionally wraps in a buffered stream.
   * The underlying pushback stream buffer size is configurable.
   *
   * @param in the input stream to wrap
   * @param noBuffer true to avoid wrapping in BufferedInputStream
   * @param pushbackBufSize number of bytes in the pushback buffer
   */
  public PSInputStreamReader(InputStream in, boolean noBuffer, int pushbackBufSize) {
    super((noBuffer ? in : new BufferedInputStream(in)), pushbackBufSize);
    //      System.out.println("Creating pushback stream w/ " + pushbackBufSize);
  }

  /**
   * Reads a line from this stream.
   * @return the next line or null if no more lines exist
   */
  public String readLine() throws java.io.IOException {
    return readLine(null);
  }

  /**
   * Reads a line from this stream using the specified character encoding.
   * @param encoding character encoding to use for decoding bytes
   * @return the next line or null if no more lines exist
   */
  public String readLine(String encoding) throws java.io.IOException {
    java.io.ByteArrayOutputStream bout = new java.io.ByteArrayOutputStream();
    int c;
    for (c = read(); c > 0; c = read()) {
      if (c == '\n') {
        break;
      } else if (c == '\r') {
        // if this is '\r', does '\n' follow (which should be skipped)
        c = read();
        if (c != '\n') unread(c);
        break;
      } else {
        bout.write(c);
      }
    }

    // was end of stream reached?
    if ((bout.size() == 0) && (c < 0)) return null;

    if (encoding == null) return bout.toString();
    else return bout.toString(encoding);
  }
}
