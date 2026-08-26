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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.share.service.exception.PSErrorUtils;
import com.percussion.share.validation.PSErrors;
import com.percussion.share.validation.PSErrors.PSObjectError;
import jakarta.ws.rs.core.MediaType;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** HTML writer for {@link PSErrors} so Preview does not fail with no message-body-writer (#3809). */
@Tag("UnitTest")
class PSErrorsHtmlMessageBodyWriterTest {

  private final PSErrorsHtmlMessageBodyWriter writer = new PSErrorsHtmlMessageBodyWriter();

  @Test
  @DisplayName("writable only for PSErrors + text/html or text/plain")
  void isWriteable() {
    assertTrue(
        writer.isWriteable(PSErrors.class, PSErrors.class, null, MediaType.TEXT_HTML_TYPE));
    assertTrue(
        writer.isWriteable(PSErrors.class, PSErrors.class, null, MediaType.TEXT_PLAIN_TYPE));
    assertFalse(
        writer.isWriteable(PSErrors.class, PSErrors.class, null, MediaType.APPLICATION_JSON_TYPE));
    assertFalse(writer.isWriteable(String.class, String.class, null, MediaType.TEXT_HTML_TYPE));
  }

  @Test
  @DisplayName("writeTo emits HTML document, not message-body-writer text")
  void writeToHtml() throws Exception {
    PSErrors errors = new PSErrors();
    PSObjectError global = new PSObjectError();
    global.setDefaultMessage("Failed to preview page: 16777215-101-551");
    errors.setGlobalError(global);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    writer.writeTo(
        errors,
        PSErrors.class,
        PSErrors.class,
        null,
        MediaType.TEXT_HTML_TYPE,
        null,
        out);
    String html = out.toString(StandardCharsets.UTF_8);
    assertTrue(html.contains("<html"));
    assertTrue(html.contains("Failed to preview page: 16777215-101-551"));
    assertFalse(html.toLowerCase().contains("no message body writer"));
  }

  @Test
  @DisplayName("HTML escapes markup in the error message")
  void escapeHtml() {
    assertEquals("&lt;script&gt;", PSErrorsHtmlMessageBodyWriter.escapeHtml("<script>"));
    assertEquals("ok", PSErrorsHtmlMessageBodyWriter.escapeHtml("ok"));
    String html = PSErrorsHtmlMessageBodyWriter.toHtml(null);
    assertTrue(html.contains("<html"));
    assertTrue(html.contains("Request failed"));
  }

  @Test
  @DisplayName("createErrorsFromException payload writes as HTML")
  void fromException() throws Exception {
    PSErrors errors =
        PSErrorUtils.createErrorsFromException(new IllegalStateException("assembly failed"));
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    writer.writeTo(
        errors, PSErrors.class, PSErrors.class, null, MediaType.TEXT_HTML_TYPE, null, out);
    String html = out.toString(StandardCharsets.UTF_8);
    assertTrue(html.contains("assembly failed"));
    assertTrue(html.contains("<html"));
  }
}
