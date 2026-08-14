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

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This class de-chunks an input stream.
 *
 * @version 0.3-3 06/05/2001
 * @author Ronald Tschalär
 */
@Deprecated
class ChunkedInputStream extends FilterInputStream {
  /**
   * @param is the input stream to dechunk
   */
  ChunkedInputStream(InputStream is) {
    super(is);
  }

  byte[] one = new byte[1];

  public synchronized int read() throws IOException {
    int b = read(one, 0, 1);
    if (b == 1) return (one[0] & 0xff);
    else return -1;
  }

  private long chunk_len = -1;
  private boolean eof = false;

  public synchronized int read(byte[] buf, int off, int len) throws IOException {
    if (eof) return -1;

    if (chunk_len == -1) // it's a new chunk
    {
      try {
        chunk_len = Codecs.getChunkLength(in);
      } catch (ParseException pe) {
        throw new IOException(pe.toString());
      }
    }

    if (chunk_len > 0) // it's data
    {
      len = Codecs.clampToChunkLength(len, chunk_len);
      int rcvd = in.read(buf, off, len);
      if (rcvd == -1) throw new EOFException("Premature EOF encountered");

      chunk_len -= rcvd;
      if (chunk_len == 0) // got the whole chunk
      {
        in.read(); // CR
        in.read(); // LF
        chunk_len = -1;
      }

      return rcvd;
    } else // the footers (trailers)
    {
      // discard
      Request dummy = new Request(null, null, null, null, null, null, false);
      new Response(dummy, null).readTrailers(in);

      eof = true;
      return -1;
    }
  }

  /** Bounded skip buffer so a huge {@code num} cannot allocate a user-sized array. */
  private static final int SKIP_BUFFER_SIZE = 8192;

  public synchronized long skip(long num) throws IOException {
    if (num <= 0L) {
      return 0L;
    }
    int bufSize = (int) Math.min(num, SKIP_BUFFER_SIZE);
    byte[] tmp = new byte[bufSize];
    long remaining = num;
    long skipped = 0L;
    while (remaining > 0L) {
      int toRead = (int) Math.min(remaining, tmp.length);
      int got = read(tmp, 0, toRead);
      if (got <= 0) {
        break;
      }
      skipped += got;
      remaining -= got;
    }
    return skipped;
  }

  public synchronized int available() throws IOException {
    if (eof) {
      return 0;
    }

    if (chunk_len != -1) {
      return Codecs.saturateAvailable(chunk_len, in.available());
    }
    return in.available();
  }
}
