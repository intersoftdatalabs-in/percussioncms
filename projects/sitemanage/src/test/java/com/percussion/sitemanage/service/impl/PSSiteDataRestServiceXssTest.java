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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.sitemanage.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for {@link PSSiteDataRestService} (CodeQL
 * {@code java/xss}, T044, US3).
 *
 * <p><strong>Background.</strong> The pre-fix code passed the
 * {@code id} / {@code siteName} / {@code siteId} / {@code sitename}
 * path parameters through the service layer to the response without
 * validating their content. A path parameter containing an XSS
 * payload ({@code <script>alert(1)</script>}) could flow through
 * the service layer to the JSON/XML response, which is then
 * rendered in HTML by the browser. The fix:
 * <ol>
 *   <li>Validates path parameters against the {@code SAFE_ID_PATTERN}
 *       allow-list at the API boundary (alphanumeric, dot, dash,
 *       underscore, colon; length 1-128). Any input outside the
 *       allow-list is rejected with HTTP 400.
 *   <li>Documents the data flow for each REST method with a
 *       {@code // codeql[java/xss] justification: ...} comment
 *       per {@code contracts/C2}.
 * </ol>
 *
 * <p><strong>Fail-then-pass coverage (Constitution III).</strong>
 * The pre-fix code had no input validation; an XSS payload in the
 * path would have been accepted and passed to the service layer.
 * The post-fix code's {@code requireSafeId} is package-private
 * (testable directly) and rejects payloads with HTML/XML/JS
 * metacharacters.
 */
@DisplayName("PSSiteDataRestService — XSS (CWE-79) regression tests")
class PSSiteDataRestServiceXssTest {

  @Nested
  @DisplayName("Safe path parameters are accepted")
  class SafeParameters {

    @Test
    @DisplayName("alphanumeric id is accepted")
    void testAlphanumericId() {
      assertEquals("abc123", PSSiteDataRestService.requireSafeId("abc123", "id"));
    }

    @Test
    @DisplayName("id with dot, dash, underscore is accepted")
    void testSpecialCharsInId() {
      assertEquals("site.test-name_v2", PSSiteDataRestService.requireSafeId("site.test-name_v2", "id"));
    }

    @Test
    @DisplayName("id with colon is accepted (CMS site-name namespace separator)")
    void testColonInId() {
      assertEquals("namespace:site-name", PSSiteDataRestService.requireSafeId("namespace:site-name", "id"));
    }

    @Test
    @DisplayName("id at the 128-character boundary is accepted")
    void testMaxLengthId() {
      String maxId = "a".repeat(128);
      assertEquals(maxId, PSSiteDataRestService.requireSafeId(maxId, "id"));
    }
  }

  @Nested
  @DisplayName("XSS payloads are rejected with 400")
  class XssPayloads {

    @Test
    @DisplayName("script tag payload is rejected")
    void testScriptTagPayload() {
      assertThrows(
          WebApplicationException.class,
          () -> PSSiteDataRestService.requireSafeId("<script>alert(1)</script>", "id"),
          "XSS script-tag payload must be rejected");
    }

    @Test
    @DisplayName("img onerror payload is rejected")
    void testImgOnerrorPayload() {
      assertThrows(
          WebApplicationException.class,
          () ->
              PSSiteDataRestService.requireSafeId(
                  "<img src=x onerror=alert(1)>", "id"),
          "XSS img-onerror payload must be rejected");
    }

    @Test
    @DisplayName("javascript: URL payload is rejected")
    void testJavascriptUrlPayload() {
      assertThrows(
          WebApplicationException.class,
          () -> PSSiteDataRestService.requireSafeId("javascript:alert(1)", "id"),
          "XSS javascript: URL payload must be rejected");
    }

    @Test
    @DisplayName("single-quote injection payload is rejected")
    void testSingleQuoteInjection() {
      assertThrows(
          WebApplicationException.class,
          () -> PSSiteDataRestService.requireSafeId("'; DROP TABLE users; --", "id"),
          "single-quote injection payload must be rejected");
    }

    @Test
    @DisplayName("xml injection payload is rejected")
    void testXmlInjection() {
      assertThrows(
          WebApplicationException.class,
          () ->
              PSSiteDataRestService.requireSafeId(
                  "]]><script>alert(1)</script>", "id"),
          "XML injection payload must be rejected");
    }

    @Test
    @DisplayName("id with space is rejected")
    void testSpaceInId() {
      assertThrows(
          WebApplicationException.class,
          () -> PSSiteDataRestService.requireSafeId("site name", "id"),
          "id with space must be rejected");
    }

    @Test
    @DisplayName("id with slash is rejected")
    void testSlashInId() {
      assertThrows(
          WebApplicationException.class,
          () -> PSSiteDataRestService.requireSafeId("../etc/passwd", "id"),
          "id with slash must be rejected");
    }

    @Test
    @DisplayName("null id is rejected")
    void testNullId() {
      assertThrows(
          WebApplicationException.class,
          () -> PSSiteDataRestService.requireSafeId(null, "id"),
          "null id must be rejected");
    }
  }

  private static String escapeHtmlForResponse(String input) {
    return com.percussion.security.validation.XSSValidation.escapeHtml(input);
  }

  @Nested
  @DisplayName("escapeHtmlForResponse helper")
  class EscapeHelper {

    @Test
    @DisplayName("HTML-sensitive characters are escaped")
    void testEscape() {
      String result = escapeHtmlForResponse("<script>alert(1)</script>");
      assertTrue(
          !result.contains("<script>"),
          "the raw <script> tag must be escaped, got: " + result);
      assertTrue(
          result.contains("&lt;") || result.contains("&#"),
          "the result must contain an HTML entity, got: " + result);
    }

    @Test
    @DisplayName("null input returns null")
    void testEscapeNull() {
      assertNull(escapeHtmlForResponse(null));
    }

    @Test
    @DisplayName("plain text passes through unchanged")
    void testEscapePlainText() {
      assertDoesNotThrow(() -> {
        String result = escapeHtmlForResponse("hello world");
        assertTrue(result.contains("hello world"));
      });
    }
  }
}
