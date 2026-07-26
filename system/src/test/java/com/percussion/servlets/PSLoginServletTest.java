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

package com.percussion.servlets;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class PSLoginServletTest {

  MockHttpServletRequest request = new MockHttpServletRequest();

  @Test
  public void testIsValidRedirectUri() throws Exception {
    request.setScheme("http");
    request.setServerPort(9992);
    request.setServerName("perc-test");
    request.setRequestURI("/stuff");
    assertEquals("http://perc-test:9992/stuff", request.getRequestURL().toString());
    assertTrue(PSLoginServlet.isValidRedirectUri(request, "http://perc-test:9992/logout"));
    assertFalse(PSLoginServlet.isValidRedirectUri(request, "http://badsite.com/login"));
    assertTrue(PSLoginServlet.isValidRedirectUri(request, "/login"));
  }

  @Test
  public void testSanitizeRedirectPathRemovesBackslashesAndDoubleSlashes() {
    // Jetty 12 UriCompliance: Ambiguous URI path separator
    assertEquals("/cm/app", PSLoginServlet.sanitizeRedirectPath("\\cm\\app"));
    assertEquals("/cm/app", PSLoginServlet.sanitizeRedirectPath("//cm//app"));
    assertEquals("index.jsp", PSLoginServlet.sanitizeRedirectPath("index.jsp"));
    assertNull(PSLoginServlet.sanitizeRedirectPath(null));
    // scheme:// must not collapse
    assertEquals(
        "http://localhost:9992/Rhythmyx",
        PSLoginServlet.sanitizeRedirectPath("http://localhost:9992/Rhythmyx"));
  }

  @Test
  public void testResolveSafePostLoginRedirectAcceptsInternalPathsAndDefaults() {
    request.setScheme("http");
    request.setServerPort(9992);
    request.setServerName("perc-test");

    assertEquals("/cm/app", PSLoginServlet.resolveSafePostLoginRedirect(request, "/cm/app"));
    assertEquals("/cm/app", PSLoginServlet.resolveSafePostLoginRedirect(request, "\\cm\\app"));
    // Default CMS index when blank / null
    assertEquals("index.jsp", PSLoginServlet.resolveSafePostLoginRedirect(request, null));
    assertEquals("index.jsp", PSLoginServlet.resolveSafePostLoginRedirect(request, "   "));
    // App-relative entry points
    assertEquals("index.jsp", PSLoginServlet.resolveSafePostLoginRedirect(request, "index.jsp"));
    assertEquals(
        "Rhythmyx/sys_cx/mainpage.html",
        PSLoginServlet.resolveSafePostLoginRedirect(request, "Rhythmyx/sys_cx/mainpage.html"));
  }

  @Test
  public void testResolveSafePostLoginRedirectRejectsOpenRedirects() {
    request.setScheme("http");
    request.setServerPort(9992);
    request.setServerName("perc-test");

    // External host → fall back to CMS index
    assertEquals(
        "index.jsp",
        PSLoginServlet.resolveSafePostLoginRedirect(request, "http://evil.example/phish"));
    // Path traversal
    assertEquals(
        "index.jsp", PSLoginServlet.resolveSafePostLoginRedirect(request, "/../../etc/passwd"));
    // javascript: scheme (relative form rejected by colon rule)
    assertEquals(
        "index.jsp", PSLoginServlet.resolveSafePostLoginRedirect(request, "javascript:alert(1)"));
    // Same-host absolute is allowed
    assertEquals(
        "http://perc-test:9992/logout",
        PSLoginServlet.resolveSafePostLoginRedirect(request, "http://perc-test:9992/logout"));
  }

  @Test
  public void testValidatePostLoginRedirectCandidate() {
    request.setServerName("perc-test");
    assertEquals("/admin", PSLoginServlet.validatePostLoginRedirectCandidate(request, "/admin"));
    assertNull(PSLoginServlet.validatePostLoginRedirectCandidate(request, "/../secret"));
    assertNull(
        PSLoginServlet.validatePostLoginRedirectCandidate(request, "http://evil.example/path"));
    assertEquals(
        "index.jsp", PSLoginServlet.validatePostLoginRedirectCandidate(request, "index.jsp"));
  }

  @Test
  public void testRebuildRedirectTargetDoesNotDoubleEncode() {
    // Jetty 12: Ambiguous URI path encoding when % is re-encoded to %25
    String withEncodedSpace = "http://perc-test:9992/path%20x";
    assertEquals(withEncodedSpace, PSLoginServlet.rebuildRedirectTarget(withEncodedSpace));
    assertFalse(PSLoginServlet.rebuildRedirectTarget(withEncodedSpace).contains("%25"));

    String loginSysRedirect =
        "/login?sys_redirect=http%3a%2f%2flocalhost%3a9992%2fRhythmyx%2fcm%2fapp";
    assertEquals(loginSysRedirect, PSLoginServlet.rebuildRedirectTarget(loginSysRedirect));
  }

  @Test
  public void testResolveSafePostLoginRedirectDecodesOverEncodedSysRedirect() {
    // Realistic session value after getParameter once-decodes a double-encoded Location query
    request.setScheme("http");
    request.setServerPort(9992);
    request.setServerName("localhost");

    String stillEncoded = "http%3a%2f%2flocalhost%3a9992%2fRhythmyx%2fcm%2fapp";
    assertEquals(
        "http://localhost:9992/Rhythmyx/cm/app",
        PSLoginServlet.resolveSafePostLoginRedirect(request, stillEncoded));
  }
}
