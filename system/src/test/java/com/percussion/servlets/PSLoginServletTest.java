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
    // SPA entry is accepted
    assertEquals(
        "/cm/app/spa.jsp?entry=home",
        PSLoginServlet.resolveSafePostLoginRedirect(request, "/cm/app/spa.jsp?entry=home"));
    // Default CMS index when blank / null — modern SPA landing
    assertEquals(
        "/cm/app/spa.jsp?entry=home", PSLoginServlet.resolveSafePostLoginRedirect(request, null));
    assertEquals(
        "/cm/app/spa.jsp?entry=home", PSLoginServlet.resolveSafePostLoginRedirect(request, "   "));
    // App-relative entry points still allowed
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

    // External host → fall back to CMS SPA index
    assertEquals(
        "/cm/app/spa.jsp?entry=home",
        PSLoginServlet.resolveSafePostLoginRedirect(request, "http://evil.example/phish"));
    // Path traversal
    assertEquals(
        "/cm/app/spa.jsp?entry=home",
        PSLoginServlet.resolveSafePostLoginRedirect(request, "/../../etc/passwd"));
    // javascript: scheme (relative form rejected by colon rule)
    assertEquals(
        "/cm/app/spa.jsp?entry=home",
        PSLoginServlet.resolveSafePostLoginRedirect(request, "javascript:alert(1)"));
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

  @Test
  public void testFirstNonBlankLocaleNormalizesAndRejects() {
    assertEquals("fr-fr", PSLoginServlet.firstNonBlankLocale("FR_FR"));
    assertEquals("en-us", PSLoginServlet.firstNonBlankLocale(" en-us "));
    assertEquals("hi", PSLoginServlet.firstNonBlankLocale("hi"));
    assertNull(PSLoginServlet.firstNonBlankLocale(null));
    assertNull(PSLoginServlet.firstNonBlankLocale(""));
    assertNull(PSLoginServlet.firstNonBlankLocale("   "));
    assertNull(PSLoginServlet.firstNonBlankLocale("../etc"));
    assertNull(PSLoginServlet.firstNonBlankLocale("en us"));
    assertNull(PSLoginServlet.firstNonBlankLocale("<script>"));
  }

  @Test
  public void testBuildLogoutLoginHrefCarriesJLocale() {
    assertEquals("login?j_locale=fr-fr", PSLoginServlet.buildLogoutLoginHref("fr-fr"));
    assertEquals("login?j_locale=en-us", PSLoginServlet.buildLogoutLoginHref("EN_US"));
    assertEquals("login", PSLoginServlet.buildLogoutLoginHref(null));
    assertEquals("login", PSLoginServlet.buildLogoutLoginHref(""));
    assertEquals("login", PSLoginServlet.buildLogoutLoginHref("not a locale"));
  }

  @Test
  public void testResolveLogoutLocalePrefersQueryThenSession() {
    // Query j_locale wins over session
    request.setParameter("j_locale", "es-es");
    request.getSession().setAttribute(
        com.percussion.i18n.PSI18nUtils.USER_SESSION_OBJECT_SYS_LANG, "de-de");
    assertEquals("es-es", PSLoginServlet.resolveLogoutLocale(request));

    // sys_lang preferred over j_locale
    MockHttpServletRequest req2 = new MockHttpServletRequest();
    req2.setParameter("sys_lang", "fr-fr");
    req2.setParameter("j_locale", "es-es");
    assertEquals("fr-fr", PSLoginServlet.resolveLogoutLocale(req2));

    // Session when no query
    MockHttpServletRequest req3 = new MockHttpServletRequest();
    req3.getSession()
        .setAttribute(
            com.percussion.i18n.PSI18nUtils.USER_SESSION_OBJECT_SYS_LANG, "hi-in");
    assertEquals("hi-in", PSLoginServlet.resolveLogoutLocale(req3));

    // Invalid query falls through to session
    MockHttpServletRequest req4 = new MockHttpServletRequest();
    req4.setParameter("j_locale", "javascript:alert(1)");
    req4.getSession()
        .setAttribute(
            com.percussion.i18n.PSI18nUtils.USER_SESSION_OBJECT_SYS_LANG, "nl-nl");
    assertEquals("nl-nl", PSLoginServlet.resolveLogoutLocale(req4));

    // No session / no params → system language (never null/empty)
    MockHttpServletRequest req5 = new MockHttpServletRequest();
    String system = PSLoginServlet.resolveLogoutLocale(req5);
    assertNotNull(system);
    assertFalse(system.isEmpty());
  }
}
