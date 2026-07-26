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

import com.percussion.security.SecureStringUtils;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for {@link PSSiteDataRestService} (CodeQL {@code java/xss}, T044, US3).
 *
 * <p><strong>Background.</strong> The pre-fix code passed the {@code id} / {@code siteName} /
 * {@code siteId} / {@code sitename} path parameters through the service layer to the response
 * without validating their content. A path parameter containing an XSS payload ({@code
 * <script>alert(1)</script>}) could flow through the service layer to the JSON/XML response, which
 * is then rendered in HTML by the browser. The fix:
 *
 * <ol>
 *   <li>Validates path parameters against the {@code SAFE_ID_PATTERN} allow-list at the API
 *       boundary (alphanumeric, dot, dash, underscore, colon; length 1-128). Any input outside the
 *       allow-list is rejected with HTTP 400.
 *   <li>Documents the data flow for each REST method with a <strong>same-line</strong> {@code //
 *       codeql[java/xss] justification: ...} annotation on the {@code return} sink (CodeQL ignores
 *       multi-line justification blocks several lines above the sink) per {@code contracts/C2} and
 *       the CodeQL PR playbook.
 * </ol>
 *
 * <p><strong>Fail-then-pass coverage (Constitution III).</strong> The pre-fix code had no input
 * validation; an XSS payload in the path would have been accepted and passed to the service layer.
 * The post-fix code's {@code requireSafeId} is package-private (testable directly) and rejects
 * payloads with HTML/XML/JS metacharacters.
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
      assertEquals(
          "site.test-name_v2", PSSiteDataRestService.requireSafeId("site.test-name_v2", "id"));
    }

    @Test
    @DisplayName("id with colon is accepted (CMS site-name namespace separator)")
    void testColonInId() {
      assertEquals(
          "namespace:site-name", PSSiteDataRestService.requireSafeId("namespace:site-name", "id"));
    }

    @Test
    @DisplayName("id at the 100-character boundary is accepted")
    void testMaxLengthId() {
      String maxId = "a".repeat(100);
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
          () -> PSSiteDataRestService.requireSafeId("<img src=x onerror=alert(1)>", "id"),
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
          () -> PSSiteDataRestService.requireSafeId("]]><script>alert(1)</script>", "id"),
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

    @Test
    @DisplayName("id over 100 characters is rejected")
    void testOverLengthId() {
      String tooLong = "a".repeat(101);
      assertThrows(
          WebApplicationException.class,
          () -> PSSiteDataRestService.requireSafeId(tooLong, "id"),
          "id over 100 chars must be rejected");
    }
  }

  @Nested
  @DisplayName("Same-line CodeQL java/xss annotations on REST sinks")
  class SinkLineAnnotations {

    /**
     * Pins that the open CodeQL sinks use same-line annotations. Multi-line justification blocks
     * several lines above {@code return} are ignored by CodeQL and left alerts #750/#751/#752/#1063
     * open despite correct {@code justification:} format.
     */
    @Test
    @DisplayName("open XSS sinks carry same-line codeql[java/xss] annotations")
    void openSinksHaveSameLineAnnotations() throws Exception {
      // Source is not on the test classpath as a resource; read from module tree
      // via a portable relative path from user.dir when tests run under Maven.
      java.nio.file.Path src =
          java.nio.file.Path.of(
              "src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataRestService.java");
      if (!java.nio.file.Files.isRegularFile(src)) {
        src =
            java.nio.file.Path.of(
                "projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataRestService.java");
      }
      assertTrue(java.nio.file.Files.isRegularFile(src), "source file must exist: " + src);
      String text = java.nio.file.Files.readString(src);
      String[] required =
          new String[] {
            "return siteDataService.save(site); // codeql[java/xss]",
            "return siteDataService.createSiteFromUrl(request, site); // codeql[java/xss]",
            "return siteDataService.updateSiteProperties(props); // codeql[java/xss]",
            "return siteDataService.updateSitePublishProperties(publishProps); // codeql[java/xss]",
          };
      for (String needle : required) {
        assertTrue(text.contains(needle), "missing same-line CodeQL annotation: " + needle);
      }
    }
  }

  @Nested
  @DisplayName("SecureStringUtils HTML sanitization")
  class EscapeHelper {

    @Test
    @DisplayName("HTML-sensitive characters are escaped")
    void testEscape() {
      String result = SecureStringUtils.sanitizeStringForHTML("<script>alert(1)</script>");
      assertTrue(
          !result.contains("<script>"), "the raw <script> tag must be escaped, got: " + result);
      assertTrue(
          result.contains("&lt;") || result.contains("&#"),
          "the result must contain an HTML entity, got: " + result);
    }

    @Test
    @DisplayName("null input returns null")
    void testEscapeNull() {
      assertNull(SecureStringUtils.sanitizeStringForHTML(null));
    }

    @Test
    @DisplayName("plain text passes through unchanged")
    void testEscapePlainText() {
      assertDoesNotThrow(
          () -> {
            String result = SecureStringUtils.sanitizeStringForHTML("hello world");
            assertTrue(result.contains("hello world"));
          });
    }
  }
}
