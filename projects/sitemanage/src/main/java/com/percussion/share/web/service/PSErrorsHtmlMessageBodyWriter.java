/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
package com.percussion.share.web.service;

import com.percussion.share.validation.PSErrors;
import com.percussion.share.validation.PSErrors.PSObjectError;
import com.percussion.system.utils.PSSiteManageBean;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/**
 * Writes {@link PSErrors} as {@code text/html} (and {@code text/plain}) so Preview and other HTML
 * GET resources do not fail with {@code No message body writer has been found for class
 * com.percussion.share.validation.PSErrors, ContentType: text/html} (#3809).
 *
 * <p>JSON/XML continue to use Jackson/JAXB. This provider is selected only for HTML/plain.
 */
@Provider
@Produces({MediaType.TEXT_HTML, MediaType.TEXT_PLAIN})
@PSSiteManageBean("psErrorsHtmlMessageBodyWriter")
public class PSErrorsHtmlMessageBodyWriter implements MessageBodyWriter<PSErrors> {

  @Override
  public boolean isWriteable(
      Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    if (type == null || !PSErrors.class.isAssignableFrom(type)) {
      return false;
    }
    return isHtmlOrPlain(mediaType);
  }

  @Override
  public void writeTo(
      PSErrors errors,
      Class<?> type,
      Type genericType,
      Annotation[] annotations,
      MediaType mediaType,
      MultivaluedMap<String, Object> httpHeaders,
      OutputStream entityStream)
      throws IOException, WebApplicationException {
    if (entityStream == null) {
      throw new IOException("entityStream is required");
    }
    byte[] bytes = toHtml(errors).getBytes(StandardCharsets.UTF_8);
    entityStream.write(bytes);
    entityStream.flush();
  }

  /**
   * Minimal HTML document for a {@link PSErrors} payload. Used by Preview error mapping when the
   * client Accept is {@code text/html}.
   *
   * @param errors may be {@code null}
   * @return HTML document, never blank
   */
  public static String toHtml(PSErrors errors) {
    String message = messageOf(errors);
    StringBuilder sb = new StringBuilder(256);
    sb.append("<!DOCTYPE html>\n");
    sb.append("<html><head><meta charset=\"UTF-8\"/><title>Preview error</title></head>");
    sb.append("<body><p>");
    sb.append(escapeHtml(message));
    sb.append("</p></body></html>");
    return sb.toString();
  }

  static boolean isHtmlOrPlain(MediaType mediaType) {
    if (mediaType == null) {
      return false;
    }
    return MediaType.TEXT_HTML_TYPE.isCompatible(mediaType)
        || MediaType.TEXT_PLAIN_TYPE.isCompatible(mediaType);
  }

  static String messageOf(PSErrors errors) {
    if (errors == null) {
      return "Request failed";
    }
    PSObjectError global = errors.getGlobalError();
    if (global != null) {
      String msg = global.getDefaultMessage();
      if (msg != null && !msg.isBlank()) {
        return msg.trim();
      }
      String code = global.getCode();
      if (code != null && !code.isBlank()) {
        return code.trim();
      }
    }
    return "Request failed";
  }

  static String escapeHtml(String raw) {
    if (raw == null || raw.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder(raw.length());
    for (int i = 0; i < raw.length(); i++) {
      char c = raw.charAt(i);
      switch (c) {
        case '&' -> sb.append("&amp;");
        case '<' -> sb.append("&lt;");
        case '>' -> sb.append("&gt;");
        case '"' -> sb.append("&quot;");
        case '\'' -> sb.append("&#39;");
        default -> sb.append(c);
      }
    }
    return sb.toString();
  }
}
