/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

package com.percussion.servlet_utils.servlet;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Production-safe mock {@link HttpServletResponse} for internal request synthesis and servlet
 * dispatch. Replaces Spring {@code MockHttpServletResponse} so {@code spring-test} stays test-only.
 */
public class PSMockHttpServletResponse implements HttpServletResponse {

  private final ByteArrayOutputStream content = new ByteArrayOutputStream();
  private final Map<String, List<String>> headers = new LinkedHashMap<>();
  private final List<Cookie> cookies = new ArrayList<>();

  private int status = SC_OK;
  private String characterEncoding = StandardCharsets.UTF_8.name();
  private String contentType;
  private Locale locale = Locale.getDefault();
  private boolean committed;
  private PrintWriter writer;
  private ServletOutputStream outputStream;

  /**
   * @return response body using the configured character encoding
   */
  public String getContentAsString() {
    Charset charset;
    try {
      charset = Charset.forName(characterEncoding);
    } catch (Exception e) {
      charset = StandardCharsets.UTF_8;
    }
    return content.toString(charset);
  }

  /**
   * @return raw response bytes
   */
  public byte[] getContentAsByteArray() {
    return content.toByteArray();
  }

  @Override
  public void addCookie(Cookie cookie) {
    if (cookie != null) {
      cookies.add(cookie);
    }
  }

  @Override
  public boolean containsHeader(String name) {
    return headers.containsKey(normalize(name));
  }

  @Override
  public String encodeURL(String url) {
    return url;
  }

  @Override
  public String encodeRedirectURL(String url) {
    return url;
  }

  @Override
  public void sendError(int sc, String msg) throws IOException {
    setStatus(sc);
    if (msg != null) {
      getWriter().write(msg);
    }
    committed = true;
  }

  @Override
  public void sendError(int sc) throws IOException {
    sendError(sc, null);
  }

  @Override
  public void sendRedirect(String location) throws IOException {
    setStatus(SC_FOUND);
    setHeader("Location", location);
    committed = true;
  }

  @Override
  public void sendRedirect(String location, int sc, boolean clearBuffer) throws IOException {
    if (clearBuffer) {
      resetBuffer();
    }
    setStatus(sc);
    setHeader("Location", location);
    committed = true;
  }

  @Override
  public void setDateHeader(String name, long date) {
    setHeader(name, Long.toString(date));
  }

  @Override
  public void addDateHeader(String name, long date) {
    addHeader(name, Long.toString(date));
  }

  @Override
  public void setHeader(String name, String value) {
    List<String> values = new ArrayList<>();
    if (value != null) {
      values.add(value);
    }
    headers.put(normalize(name), values);
  }

  @Override
  public void addHeader(String name, String value) {
    headers.computeIfAbsent(normalize(name), k -> new ArrayList<>()).add(value);
  }

  @Override
  public void setIntHeader(String name, int value) {
    setHeader(name, Integer.toString(value));
  }

  @Override
  public void addIntHeader(String name, int value) {
    addHeader(name, Integer.toString(value));
  }

  @Override
  public void setStatus(int sc) {
    this.status = sc;
  }

  @Override
  public int getStatus() {
    return status;
  }

  @Override
  public String getHeader(String name) {
    List<String> values = headers.get(normalize(name));
    return values == null || values.isEmpty() ? null : values.get(0);
  }

  @Override
  public Collection<String> getHeaders(String name) {
    List<String> values = headers.get(normalize(name));
    return values == null ? List.of() : Collections.unmodifiableList(values);
  }

  @Override
  public Collection<String> getHeaderNames() {
    return Collections.unmodifiableSet(headers.keySet());
  }

  @Override
  public String getCharacterEncoding() {
    return characterEncoding;
  }

  @Override
  public String getContentType() {
    return contentType;
  }

  @Override
  public ServletOutputStream getOutputStream() {
    if (writer != null) {
      throw new IllegalStateException("getWriter() already called");
    }
    if (outputStream == null) {
      outputStream =
          new ServletOutputStream() {
            @Override
            public boolean isReady() {
              return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {
              // no-op for in-memory capture
            }

            @Override
            public void write(int b) {
              content.write(b);
            }
          };
    }
    return outputStream;
  }

  @Override
  public PrintWriter getWriter() throws IOException {
    if (outputStream != null) {
      throw new IllegalStateException("getOutputStream() already called");
    }
    if (writer == null) {
      writer =
          new PrintWriter(
              new OutputStreamWriter(content, Charset.forName(characterEncoding)), true);
    }
    return writer;
  }

  @Override
  public void setCharacterEncoding(String charset) {
    this.characterEncoding = charset;
  }

  @Override
  public void setContentLength(int len) {
    setIntHeader("Content-Length", len);
  }

  @Override
  public void setContentLengthLong(long len) {
    setHeader("Content-Length", Long.toString(len));
  }

  @Override
  public void setContentType(String type) {
    this.contentType = type;
    setHeader("Content-Type", type);
  }

  @Override
  public void setBufferSize(int size) {
    // no-op
  }

  @Override
  public int getBufferSize() {
    return content.size();
  }

  @Override
  public void flushBuffer() throws IOException {
    if (writer != null) {
      writer.flush();
    }
    committed = true;
  }

  @Override
  public void resetBuffer() {
    if (committed) {
      throw new IllegalStateException("Response already committed");
    }
    content.reset();
  }

  @Override
  public boolean isCommitted() {
    return committed;
  }

  @Override
  public void reset() {
    if (committed) {
      throw new IllegalStateException("Response already committed");
    }
    content.reset();
    headers.clear();
    cookies.clear();
    status = SC_OK;
    contentType = null;
    writer = null;
    outputStream = null;
  }

  @Override
  public void setLocale(Locale loc) {
    if (loc != null) {
      this.locale = loc;
    }
  }

  @Override
  public Locale getLocale() {
    return locale;
  }

  private static String normalize(String name) {
    return name == null ? "" : name.toLowerCase(Locale.ROOT);
  }
}
