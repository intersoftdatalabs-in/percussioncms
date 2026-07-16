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
package com.percussion.services.assembly.jexl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * SSRF regression tests for {@link PSDocumentUtils} (CodeQL {@code java/ssrf}, alerts #1066 /
 * #1067, T037 residual).
 *
 * <p>Outbound requests must be built from {@code URLValidation.validateURLString}'s return value,
 * not the raw user string. Malicious targets must fail closed before any HTTP client is created.
 */
@DisplayName("PSDocumentUtils — SSRF (CWE-918) regression tests")
class PSDocumentUtilsSsrfTest {

  private final PSDocumentUtils utils = new PSDocumentUtils();

  @Nested
  @DisplayName("Malicious URLs are rejected")
  class Rejection {

    @Test
    @DisplayName("AWS instance metadata URL is rejected")
    void testAwsMetadataRejected() {
      IOException ex =
          assertThrows(
              IOException.class,
              () -> utils.buildValidatedExternalRequestUri("http://169.254.169.254/latest/meta-data/"));
      assertTrue(
          ex.getMessage().contains("SSRF") || ex.getCause() instanceof SecurityException,
          "expected SSRF validation failure, got: " + ex.getMessage());
    }

    @Test
    @DisplayName("private IP URL is rejected")
    void testPrivateIpRejected() {
      assertThrows(
          IOException.class,
          () -> utils.buildValidatedExternalRequestUri("http://192.168.1.1/secret"));
    }

    @Test
    @DisplayName("file scheme is rejected")
    void testFileSchemeRejected() {
      assertThrows(
          IOException.class,
          () -> utils.buildValidatedExternalRequestUri("file:///etc/passwd"));
    }
  }

  @Nested
  @DisplayName("Safe public URLs produce a validated URI")
  class Acceptance {

    @Test
    @DisplayName("public https URL is accepted and URI host matches")
    void testPublicHttpsAccepted() throws Exception {
      URI uri = utils.buildValidatedExternalRequestUri("https://example.com/path?q=1");
      assertEquals("https", uri.getScheme());
      assertEquals("example.com", uri.getHost());
      assertEquals("/path", uri.getPath());
      assertEquals("q=1", uri.getQuery());
    }
  }
}
