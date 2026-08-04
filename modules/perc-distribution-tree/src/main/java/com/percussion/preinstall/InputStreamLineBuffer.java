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

package com.percussion.preinstall;

import com.percussion.security.error.PSExceptionUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Buffers lines read from an {@link InputStream} on a background thread so callers can poll for
 * output without blocking.
 */
public class InputStreamLineBuffer {

  private static final Logger log = LogManager.getLogger(InputStreamLineBuffer.class);

  private InputStream inputStream;
  private ConcurrentLinkedQueue<String> lines;
  private long lastTimeModified;
  private Thread inputCatcher;
  private boolean isAlive;

  /**
   * Captures lines from the supplied input stream. Reading starts when {@link #start()} is called.
   *
   * @param is source input stream; must not be null
   */
  public InputStreamLineBuffer(InputStream is) {
    inputStream = is;
    lines = new ConcurrentLinkedQueue<>();
    lastTimeModified = System.currentTimeMillis();
    isAlive = false;
    inputCatcher =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                StringBuilder sb = new StringBuilder(100);
                int b;
                try {
                  while ((b = inputStream.read()) != -1) {
                    // read one char
                    if ((char) b == '\n') {
                      // new Line -> add to queue
                      lines.offer(sb.toString());
                      sb.setLength(0); // reset StringBuilder
                      lastTimeModified = System.currentTimeMillis();
                    } else sb.append((char) b); // append char to stringbuilder
                  }
                } catch (IOException e) {
                  log.error(PSExceptionUtils.getMessageForLog(e));
                  log.debug(PSExceptionUtils.getDebugMessageForLog(e));
                } finally {
                  isAlive = false;
                }
              }
            });
  }

  /**
   * Returns true while the reader thread is still alive.
   *
   * @return {@code true} while the background reader thread is still running.
   */
  public boolean isAlive() {
    return isAlive;
  }

  /** Starts the background reader thread. */
  public void start() {
    isAlive = true;
    inputCatcher.start();
  }

  /**
   * Returns true when at least one buffered line is available.
   *
   * @return {@code true} when one or more lines are ready to be consumed via {@link #getNext()}.
   */
  public boolean hasNext() {
    return lines.size() > 0;
  }

  /**
   * Returns and removes the next buffered line, or null if none are available.
   *
   * @return the next buffered line, or {@code null} when the buffer is empty.
   */
  public String getNext() {
    return lines.poll();
  }

  /**
   * Returns milliseconds elapsed since the most recent line was read.
   *
   * @return milliseconds elapsed since the last buffered line was read.
   */
  public long timeElapsed() {
    return (System.currentTimeMillis() - lastTimeModified);
  }
}
