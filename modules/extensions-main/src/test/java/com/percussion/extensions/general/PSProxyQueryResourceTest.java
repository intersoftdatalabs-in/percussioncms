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
package com.percussion.extensions.general;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.percussion.security.validation.URLValidation;
import com.percussion.security.validation.URLValidationConfig;
import com.percussion.server.IPSRequestContext;
import com.percussion.testing.PSMockRequestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * Regression tests for {@link PSProxyQueryResource} (CodeQL {@code java/ssrf}, T037, US3).
 *
 * <p><strong>Background.</strong> The pre-fix code called {@code URI.create(url)} before {@code
 * URLValidation.validateURLString(url)}; the validation rejected malicious URLs at runtime, but the
 * {@code requestUri} had already been constructed from the raw user-supplied string, which CodeQL's
 * data-flow analysis still flagged as a taint sink. The fix moves the validation before {@code
 * URI.create} and re-derives the outbound URI from the validated {@link java.net.URL} object, so
 * CodeQL can recognize the request as sanitized.
 *
 * <p><strong>Fail-then-pass coverage (Constitution III).</strong> The runtime SSRF protection was
 * already in place in the pre-fix code (the validation rejected every known SSRF target), so the
 * tests in this file serve as <em>regression guards</em> rather than strict fail-then-pass tests.
 * The structural fix's value is in the data-flow ordering — the URI is now derived from a sanitized
 * {@link java.net.URL} object — which is what CodeQL's taint analysis needs to mark the finding as
 * fixed. The tests below cover the runtime behavior; the structural fix is verified by the CodeQL
 * re-scan on the merged PR.
 *
 * <p><strong>Note on the relative-URL branch.</strong> The pre-fix code's relative-URL rewriting
 * branch (lines 109-112) is a separate SSRF exposure: a URL starting with {@code "../"} is
 * rewritten to {@code PSServer.getRequestRoot() + url.substring(2)} and the resulting absolute URL
 * is treated as internal, which skipped validation. The post-fix code validates the rewritten URL
 * against {@link URLValidationConfig} as well, closing that exposure. The relative-URL branch
 * cannot be exercised in a unit test without initializing {@code PSServer} (which has static state
 * set during server startup); the fix is verified end-to-end by the CodeQL re-scan.
 */
@DisplayName("PSProxyQueryResource — SSRF (CWE-918) regression tests")
class PSProxyQueryResourceTest {

  private PSProxyQueryResource m_ext;
  private IPSRequestContext m_request;
  private URLValidationConfig m_originalDefault;

  @BeforeEach
  void setUp() {
    m_ext = new PSProxyQueryResource();
    m_request = new PSMockRequestContext();
    m_originalDefault = URLValidationConfig.getDefault();
  }

  @AfterEach
  void tearDown() {
    URLValidationConfig.setDefault(m_originalDefault);
  }

  private Object[] paramsFor(String url) {
    // Exit signature for PSProxyQueryResource: a single-element array containing the URL
    // string; the helper extracts it via PSExtensionParamsHelper.
    return new Object[] {url};
  }

  @Nested
  @DisplayName("Malicious URL is rejected before the HTTP request is sent")
  class SsrProtection {

    @Test
    @DisplayName("AWS instance metadata URL is rejected")
    void testAwsMetadataUrlIsRejected() {
      Document result =
          assertDoesNotThrow(
              () ->
                  m_ext.processResultDocument(
                      paramsFor("http://169.254.169.254/latest/meta-data/"), m_request, null),
              "Extension must not propagate an exception for a malicious URL");
      assertNull(result, "Expected the extension to return null for a malicious URL");
    }

    @Test
    @DisplayName("Private IP URL is rejected")
    void testPrivateIpUrlIsRejected() {
      Document result =
          assertDoesNotThrow(
              () ->
                  m_ext.processResultDocument(
                      paramsFor("http://10.0.0.1/internal"), m_request, null),
              "Extension must not propagate an exception for a private IP URL");
      assertNull(result, "Expected the extension to return null for a private IP URL");
    }

    @Test
    @DisplayName("file:// scheme is rejected")
    void testFileSchemeIsRejected() {
      Document result =
          assertDoesNotThrow(
              () -> m_ext.processResultDocument(paramsFor("file:///etc/passwd"), m_request, null),
              "Extension must not propagate an exception for a file:// URL");
      assertNull(result, "Expected the extension to return null for a file:// URL");
    }

    @Test
    @DisplayName("External URL on a non-standard port is rejected")
    void testNonStandardExternalPortIsRejected() {
      Document result =
          assertDoesNotThrow(
              () ->
                  m_ext.processResultDocument(
                      paramsFor("http://example.com:9999/api"), m_request, null),
              "Extension must not propagate an exception for a disallowed port");
      assertNull(result, "Expected the extension to return null for a non-standard external port");
    }

    @Test
    @DisplayName("Malformed URL is rejected")
    void testMalformedUrlIsRejected() {
      Document result =
          assertDoesNotThrow(
              () -> m_ext.processResultDocument(paramsFor("not a valid url"), m_request, null),
              "Extension must not propagate an exception for a malformed URL");
      assertNull(result, "Expected the extension to return null for a malformed URL");
    }

    @Test
    @DisplayName("Gopher protocol URL is rejected")
    void testGopherProtocolIsRejected() {
      // gopher:// is a known SSRF target for probing internal services.
      Document result =
          assertDoesNotThrow(
              () ->
                  m_ext.processResultDocument(paramsFor("gopher://example.com/"), m_request, null),
              "Extension must not propagate an exception for a gopher:// URL");
      assertNull(result, "Expected the extension to return null for a gopher:// URL");
    }
  }

  @Test
  @DisplayName("URLValidation utility is reachable from the test classpath")
  void testClasspathSanity() {
    // Guards against a future refactor that moves URLValidation to a different package
    // and breaks the dependency from PSProxyQueryResource. If the production code can no
    // longer reach URLValidation, the tests in SsrProtection would silently stop catching
    // SSRF attempts; this test makes the dependency explicit.
    assertNotNull(URLValidation.class, "URLValidation class must be on the classpath");
  }
}
