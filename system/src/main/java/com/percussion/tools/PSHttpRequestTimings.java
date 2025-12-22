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

/** Timing statistics for an HTTP request. */
public class PSHttpRequestTimings implements Cloneable {
  public PSHttpRequestTimings() {}

  /**
   * Returns the time before the socket is opened.
   *
   * @return time in milliseconds since epoch
   */
  public long beforeConnect() {
    return beforeConnectTime;
  }

  /**
   * Sets the time before the socket is opened.
   *
   * @param time time in milliseconds since epoch
   */
  public void beforeConnect(long time) {
    beforeConnectTime = time;
  }

  /**
   * Gets the after connect time.
   *
   * @author chad loder
   * @version 1.0 1999/10/25
   * @return long The after connect time, in milliseconds elapsed since midnight, January 1, 1970
   *     UTC.
   */
  public long afterConnect() {
    return afterConnectTime;
  }

  /**
   * Sets the after connect time. The after connect time is the time after the socket has been
   * opened but before any data has been sent or received.
   *
   * @author chad loder
   * @version 1.0 1999/10/25
   * @param time The after connect time in milliseconds elapsed since midnight, January 1, 1970 UTC.
   */
  public void afterConnect(long time) {
    afterConnectTime = time;
  }

  public long afterRequest() {
    return afterRequestTime;
  }

  public void afterRequest(long time) {
    afterRequestTime = time;
  }

  public long afterHeaders() {
    return afterHeadersTime;
  }

  public void afterHeaders(long time) {
    afterHeadersTime = time;
  }

  public long afterContent() {
    return afterContentTime;
  }

  public void afterContent(long time) {
    afterContentTime = time;
  }

  public long headerBytes() {
    return headerBytesCount;
  }

  public void headerBytes(long bytes) {
    headerBytesCount = bytes;
  }

  public long contentBytes() {
    return contentBytesCount;
  }

  public void contentBytes(long bytes) {
    contentBytesCount = bytes;
  }

  public Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

  /** Set before the socket is opened. */
  private long beforeConnectTime = 0L;

  /** Set after the socket is opened, before any data has been sent or received. */
  private long afterConnectTime = 0L;

  /** Set after the request has been sent, before any data has been received. */
  private long afterRequestTime = 0L;

  /**
   * Set after all the response headers have been received, before any body data has been received.
   */
  private long afterHeadersTime = 0L;

  /** Set after all of the response content has been read. */
  private long afterContentTime = 0L;

  /** Number of header bytes returned, including the HTTP status line. */
  private long headerBytesCount = 0L;

  /** Number of content bytes returned after the last header. */
  private long contentBytesCount = 0L;
}
