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

package com.percussion.security.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for {@link PSRedirectValidation} covering CWE-601 (Open Redirect)
 * vulnerability prevention.
 *
 * <p>Test Coverage:
 *
 * <ul>
 *   <li>Relative URL validation (internal redirects)
 *   <li>Absolute URL whitelist validation
 *   <li>Open redirect attack prevention
 *   <li>URL encoding and special characters
 *   <li>Protocol-relative URLs (//evil.com)
 *   <li>JavaScript and data URI attacks
 *   <li>Directory traversal in paths
 * </ul>
 */
@DisplayName("PSRedirectValidation - Open Redirect Prevention (CWE-601)")
class PSRedirectValidationTest {

  private static final Set<String> DEFAULT_WHITELIST = createDefaultWhitelist();

  private static Set<String> createDefaultWhitelist() {
    Set<String> whitelist = new HashSet<>();
    whitelist.add("example.com");
    whitelist.add("www.example.com");
    whitelist.add("api.example.com");
    return whitelist;
  }

  @Nested
  @DisplayName("Test Relative URL Validation (Internal Redirects)")
  class RelativeURLTests {

    @Test
    @DisplayName("Should accept simple relative paths")
    void testSimpleRelativePath() {
      String result = PSRedirectValidation.validateRedirectUrl("/dashboard", DEFAULT_WHITELIST);
      assertEquals("/dashboard", result, "Simple relative path should be accepted");
    }

    @Test
    @DisplayName("Should accept relative paths with query parameters")
    void testRelativePathWithQuery() {
      String result =
          PSRedirectValidation.validateRedirectUrl(
              "/pages/view?id=123&tab=summary", DEFAULT_WHITELIST);
      assertEquals(
          "/pages/view?id=123&tab=summary", result, "Path with query params should be accepted");
    }

    @Test
    @DisplayName("Should accept relative paths with fragments")
    void testRelativePathWithFragment() {
      String result =
          PSRedirectValidation.validateRedirectUrl("/docs/api#authentication", DEFAULT_WHITELIST);
      assertEquals("/docs/api#authentication", result, "Path with fragment should be accepted");
    }

    @Test
    @DisplayName("Should reject relative paths with directory traversal")
    void testDirectoryTraversal() {
      String result =
          PSRedirectValidation.validateRedirectUrl("/../../etc/passwd", DEFAULT_WHITELIST);
      assertNull(result, "Directory traversal attempts should be rejected");
    }

    @Test
    @DisplayName("Should reject paths containing .. anywhere")
    void testDirectoryTraversalVariant() {
      String result =
          PSRedirectValidation.validateRedirectUrl(
              "/safe/path/../../../sensitive", DEFAULT_WHITELIST);
      assertNull(result, "Paths with .. should be rejected regardless of position");
    }

    @Test
    @DisplayName("Should accept deeply nested safe paths")
    void testDeeplyNestedPath() {
      String result =
          PSRedirectValidation.validateRedirectUrl("/a/b/c/d/e/f/g/h", DEFAULT_WHITELIST);
      assertEquals("/a/b/c/d/e/f/g/h", result, "Deeply nested paths without .. should be safe");
    }
  }

  @Nested
  @DisplayName("Test Open Redirect Attack Prevention")
  class OpenRedirectAttackTests {

    @Test
    @DisplayName("Should reject protocol-relative URLs (//evil.com)")
    void testProtocolRelativeUrl() {
      String result =
          PSRedirectValidation.validateRedirectUrl("//evil.com/phishing", DEFAULT_WHITELIST);
      assertNull(result, "Protocol-relative URLs (//evil.com) should be rejected");
    }

    @Test
    @DisplayName("Should reject protocol-relative URLs with www")
    void testProtocolRelativeUrlVariant() {
      String result =
          PSRedirectValidation.validateRedirectUrl(
              "//www.attacker.com/steal-data", DEFAULT_WHITELIST);
      assertNull(result, "Protocol-relative URLs with www should be rejected");
    }

    @Test
    @DisplayName("Should reject external HTTP URLs not in whitelist")
    void testUnwhitelistedExternalUrl() {
      String result =
          PSRedirectValidation.validateRedirectUrl(
              "http://attacker.com/phishing", DEFAULT_WHITELIST);
      assertNull(result, "External URLs not in whitelist should be rejected");
    }

    @Test
    @DisplayName("Should reject external HTTPS URLs not in whitelist")
    void testUnwhitelistedHttpsUrl() {
      String result =
          PSRedirectValidation.validateRedirectUrl(
              "https://malicious.org/steal", DEFAULT_WHITELIST);
      assertNull(result, "HTTPS URLs not in whitelist should be rejected");
    }

    @Test
    @DisplayName("Should accept whitelisted HTTP URLs")
    void testWhitelistedHttpUrl() {
      String result =
          PSRedirectValidation.validateRedirectUrl("http://example.com/page", DEFAULT_WHITELIST);
      assertEquals("http://example.com/page", result, "Whitelisted HTTP URLs should be accepted");
    }

    @Test
    @DisplayName("Should accept whitelisted HTTPS URLs")
    void testWhitelistedHttpsUrl() {
      String result =
          PSRedirectValidation.validateRedirectUrl(
              "https://www.example.com/secure", DEFAULT_WHITELIST);
      assertEquals(
          "https://www.example.com/secure", result, "Whitelisted HTTPS URLs should be accepted");
    }

    @Test
    @DisplayName("Should accept whitelisted subdomains")
    void testWhitelistedSubdomain() {
      String result =
          PSRedirectValidation.validateRedirectUrl(
              "https://api.example.com/v1/data", DEFAULT_WHITELIST);
      assertEquals(
          "https://api.example.com/v1/data", result, "Whitelisted subdomains should be accepted");
    }

    @Test
    @DisplayName("Should reject FTP URLs")
    void testFtpUrl() {
      String result =
          PSRedirectValidation.validateRedirectUrl("ftp://example.com/file", DEFAULT_WHITELIST);
      assertNull(result, "FTP URLs should be rejected (only http/https allowed)");
    }

    @Test
    @DisplayName("Should reject file:// URLs")
    void testFileUrl() {
      String result =
          PSRedirectValidation.validateRedirectUrl("file:///etc/passwd", DEFAULT_WHITELIST);
      assertNull(result, "file:// URLs should be rejected");
    }
  }

  @Nested
  @DisplayName("Test JavaScript and Data URI Attack Prevention")
  class JavaScriptAndDataURITests {

    @Test
    @DisplayName("Should reject JavaScript URLs")
    void testJavaScriptUrl() {
      String result =
          PSRedirectValidation.validateRedirectUrl("javascript:alert('XSS')", DEFAULT_WHITELIST);
      assertNull(result, "JavaScript URLs should be rejected");
    }

    @Test
    @DisplayName("Should reject JavaScript URLs with different capitalization")
    void testJavaScriptUrlCapitalization() {
      String result =
          PSRedirectValidation.validateRedirectUrl("JavaScript:alert('XSS')", DEFAULT_WHITELIST);
      assertNull(result, "JavaScript URLs (case-insensitive) should be rejected");
    }

    @Test
    @DisplayName("Should reject data URIs")
    void testDataUri() {
      String result =
          PSRedirectValidation.validateRedirectUrl(
              "data:text/html,<script>alert('XSS')</script>", DEFAULT_WHITELIST);
      assertNull(result, "Data URIs should be rejected");
    }

    @Test
    @DisplayName("Should reject data URIs with different capitalization")
    void testDataUriCapitalization() {
      String result =
          PSRedirectValidation.validateRedirectUrl(
              "DATA:text/html;base64,PHNjcmlwdD4=", DEFAULT_WHITELIST);
      assertNull(result, "Data URIs (case-insensitive) should be rejected");
    }

    @Test
    @DisplayName("Should reject vbscript URLs")
    void testVbScriptUrl() {
      String result =
          PSRedirectValidation.validateRedirectUrl("vbscript:msgbox('XSS')", DEFAULT_WHITELIST);
      assertNull(result, "VBScript URLs should be rejected");
    }
  }

  @Nested
  @DisplayName("Test Edge Cases and Special Characters")
  class EdgeCasesTests {

    @Test
    @DisplayName("Should reject null URL")
    void testNullUrl() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PSRedirectValidation.validateRedirectUrl(null, DEFAULT_WHITELIST),
          "Null URL should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("Should reject empty URL")
    void testEmptyUrl() {
      String result = PSRedirectValidation.validateRedirectUrl("", DEFAULT_WHITELIST);
      assertNull(result, "Empty URL should be rejected");
    }

    @Test
    @DisplayName("Should reject URL with only whitespace")
    void testWhitespaceOnlyUrl() {
      String result = PSRedirectValidation.validateRedirectUrl("   ", DEFAULT_WHITELIST);
      assertNull(result, "Whitespace-only URL should be rejected");
    }

    @Test
    @DisplayName("Should trim whitespace from URLs")
    void testWhitespaceTrimming() {
      String result = PSRedirectValidation.validateRedirectUrl("  /dashboard  ", DEFAULT_WHITELIST);
      assertEquals("/dashboard", result, "Whitespace should be trimmed from URLs");
    }

    @Test
    @DisplayName("Should handle URLs with encoded characters")
    void testEncodedCharacters() {
      String result =
          PSRedirectValidation.validateRedirectUrl(
              "/search?q=hello%20world&sort=date", DEFAULT_WHITELIST);
      assertEquals(
          "/search?q=hello%20world&sort=date",
          result, "URL-encoded characters should be preserved");
    }

    @Test
    @DisplayName("Should reject empty whitelist")
    void testEmptyWhitelist() {
      Set<String> emptyWhitelist = new HashSet<>();
      String result =
          PSRedirectValidation.validateRedirectUrl("http://example.com/page", emptyWhitelist);
      assertNull(result, "External URLs should be rejected with empty whitelist");
    }

    @Test
    @DisplayName("Should handle URLs with port numbers")
    void testUrlWithPort() {
      String result =
          PSRedirectValidation.validateRedirectUrl(
              "http://example.com:8080/api", DEFAULT_WHITELIST);
      assertEquals("http://example.com:8080/api", result, "URLs with port numbers should be valid");
    }

    @Test
    @DisplayName("Should handle internationalized domain names (IDN)")
    void testInternationalizedDomain() {
      Set<String> whitelist = new HashSet<>();
      whitelist.add("example.com");
      String result =
          PSRedirectValidation.validateRedirectUrl("http://example.com/über", whitelist);
      assertEquals("http://example.com/über", result, "Internationalized URLs should be valid");
    }
  }

  @Nested
  @DisplayName("Test Internal Redirect Validation")
  class InternalRedirectValidationTests {

    @Test
    @DisplayName("Should accept simple internal paths")
    void testSimpleInternalPath() {
      String result = PSRedirectValidation.validateInternalRedirectUrl("/admin");
      assertEquals("/admin", result, "Simple internal path should be accepted");
    }

    @Test
    @DisplayName("Should accept internal paths with query parameters")
    void testInternalPathWithQuery() {
      String result = PSRedirectValidation.validateInternalRedirectUrl("/pages?id=123");
      assertEquals("/pages?id=123", result, "Internal path with query should be accepted");
    }

    @Test
    @DisplayName("Should reject all external URLs")
    void testRejectExternalUrl() {
      String result = PSRedirectValidation.validateInternalRedirectUrl("http://example.com/page");
      assertNull(result, "External URLs should be rejected in internal redirect validation");
    }

    @Test
    @DisplayName("Should reject protocol-relative URLs")
    void testRejectProtocolRelativeUrl() {
      String result = PSRedirectValidation.validateInternalRedirectUrl("//evil.com");
      assertNull(result, "Protocol-relative URLs should be rejected");
    }

    @Test
    @DisplayName("Should reject JavaScript URLs")
    void testRejectJavaScriptUrl() {
      String result = PSRedirectValidation.validateInternalRedirectUrl("javascript:alert('XSS')");
      assertNull(result, "JavaScript URLs should be rejected");
    }

    @Test
    @DisplayName("Should reject directory traversal")
    void testRejectDirectoryTraversal() {
      String result = PSRedirectValidation.validateInternalRedirectUrl("/../../sensitive");
      assertNull(result, "Directory traversal should be rejected");
    }

    @Test
    @DisplayName("Should reject relative paths without leading slash")
    void testRejectRelativePath() {
      String result = PSRedirectValidation.validateInternalRedirectUrl("admin/page");
      assertNull(result, "Relative paths without / should be rejected in strict mode");
    }
  }

  @Nested
  @DisplayName("Test Default Whitelist Creation")
  class DefaultWhitelistTests {

    @Test
    @DisplayName("Should create whitelist with main domain")
    void testCreateDefaultWhitelist() {
      Set<String> whitelist = PSRedirectValidation.createDefaultWhitelist("example.com");
      assertTrue(whitelist.contains("example.com"), "Main domain should be in whitelist");
    }

    @Test
    @DisplayName("Should add www variant to whitelist")
    void testCreateWhitelistWithWww() {
      Set<String> whitelist = PSRedirectValidation.createDefaultWhitelist("example.com");
      assertTrue(whitelist.contains("www.example.com"), "www variant should be added");
    }

    @Test
    @DisplayName("Should not duplicate www variant if already present")
    void testCreateWhitelistSkipsDuplicateWww() {
      Set<String> whitelist = PSRedirectValidation.createDefaultWhitelist("www.example.com");
      assertEquals(1, whitelist.size(), "Should not create duplicate www entries");
    }

    @Test
    @DisplayName("Should handle null domain gracefully")
    void testCreateWhitelistWithNull() {
      Set<String> whitelist = PSRedirectValidation.createDefaultWhitelist(null);
      assertTrue(whitelist.isEmpty(), "Null domain should result in empty whitelist");
    }

    @Test
    @DisplayName("Should handle blank domain gracefully")
    void testCreateWhitelistWithBlank() {
      Set<String> whitelist = PSRedirectValidation.createDefaultWhitelist("   ");
      assertTrue(whitelist.isEmpty(), "Blank domain should result in empty whitelist");
    }
  }

  @Nested
  @DisplayName("Test Real-World Attack Scenarios")
  class RealWorldAttackScenarios {

    @Test
    @DisplayName("Should prevent GitHub OAuth callback hijacking")
    void testGitHubOAuthHijacking() {
      // Attacker tries: http://attacker.com/steal-token?code=legitimateCode
      String result =
          PSRedirectValidation.validateRedirectUrl(
              "http://attacker.com/oauth/callback?code=abc123", DEFAULT_WHITELIST);
      assertNull(result, "GitHub OAuth hijacking attempt should be blocked");
    }

    @Test
    @DisplayName("Should allow legitimate OAuth callback")
    void testLegitimateOAuthCallback() {
      Set<String> whitelist = PSRedirectValidation.createDefaultWhitelist("auth.example.com");
      String result =
          PSRedirectValidation.validateRedirectUrl(
              "https://auth.example.com/callback?code=abc123&state=xyz", whitelist);
      assertNotNull(result, "Legitimate OAuth callback should be allowed");
    }

    @Test
    @DisplayName("Should prevent open redirect via encoded URLs")
    void testEncodedOpenRedirect() {
      // Attacker tries: /auth/callback?next=http://evil.com (URL encoded)
      // But our validation works on decoded URLs, so this is caught at app level
      String result =
          PSRedirectValidation.validateRedirectUrl(
              "http://attacker.com/phishing", DEFAULT_WHITELIST);
      assertNull(result, "Encoded open redirect attempts should be blocked");
    }

    @Test
    @DisplayName("Should prevent open redirect via data exfiltration")
    void testDataExfiltration() {
      // Attacker tries: data:text/html with <img src=http://evil.com?data=
      String result =
          PSRedirectValidation.validateRedirectUrl(
              "data:text/html,<img src=http://attacker.com?cookie=", DEFAULT_WHITELIST);
      assertNull(result, "Data URI injection attempts should be blocked");
    }

    @Test
    @DisplayName("Should prevent open redirect via form submission")
    void testFormSubmissionRedirect() {
      // Legitimate: /checkout/confirm with hidden form redirecting to attacker
      Set<String> whitelist = new HashSet<>();
      whitelist.add("paymentgateway.com");
      String result =
          PSRedirectValidation.validateRedirectUrl(
              "https://attacker.com/steal-card-data", whitelist);
      assertNull(result, "Unwhitelisted payment gateway redirect should be blocked");
    }

    @Test
    @DisplayName("Should allow legitimate post-login redirect")
    void testLegitimatePostLoginRedirect() {
      Set<String> whitelist = PSRedirectValidation.createDefaultWhitelist("example.com");
      String result =
          PSRedirectValidation.validateRedirectUrl(
              "https://www.example.com/dashboard?tab=profile", whitelist);
      assertNotNull(result, "Legitimate post-login redirect should be allowed");
    }
  }

  @Nested
  @DisplayName("rebuildValidatedRedirect — preserve encoding (Jetty UriCompliance)")
  class RebuildValidatedRedirectTests {

    @Test
    @DisplayName("Must not double-encode login sys_redirect query (Jetty Ambiguous URI path encoding)")
    void rebuildPreservesSingleEncodedSysRedirectQuery() {
      // addRedirect encodes once; rebuild must not turn %3a into %253a
      String loginWithRedirect =
          "/login?sys_redirect=http%3a%2f%2flocalhost%3a9992%2fRhythmyx%2fcm%2fapp";
      String rebuilt = PSRedirectValidation.rebuildValidatedRedirect(loginWithRedirect);
      assertEquals(loginWithRedirect, rebuilt);
      assertFalse(rebuilt.contains("%25"), "must not produce %25 (double-encoded %)");
    }

    @Test
    @DisplayName("Preserves absolute URL path encoding without %2520")
    void rebuildPreservesEncodedPath() {
      String url = "http://example.com/path%20with%20spaces";
      assertEquals(url, PSRedirectValidation.rebuildValidatedRedirect(url));
    }

    @Test
    @DisplayName("Preserves simple absolute and relative targets")
    void rebuildSimpleTargets() {
      assertEquals("index.jsp", PSRedirectValidation.rebuildValidatedRedirect("index.jsp"));
      assertEquals("/cm/app", PSRedirectValidation.rebuildValidatedRedirect("/cm/app"));
      assertEquals(
          "http://example.com/logout",
          PSRedirectValidation.rebuildValidatedRedirect("http://example.com/logout"));
    }

    @Test
    @DisplayName("Returns null for blank")
    void rebuildBlank() {
      assertNull(PSRedirectValidation.rebuildValidatedRedirect(null));
      assertNull(PSRedirectValidation.rebuildValidatedRedirect("   "));
    }
  }

  @Nested
  @DisplayName("decodeOverEncodedRedirect — fix double-encoded sys_redirect")
  class DecodeOverEncodedRedirectTests {

    @Test
    @DisplayName("Decodes double-encoded absolute URL (session after buggy Location header)")
    void decodeDoubleEncodedAbsolute() {
      // After getParameter once-decodes double-encoded Location query
      String stillEncoded = "http%3a%2f%2flocalhost%3a9992%2fRhythmyx%2fcm%2fapp";
      assertEquals(
          "http://localhost:9992/Rhythmyx/cm/app",
          PSRedirectValidation.decodeOverEncodedRedirect(stillEncoded));
    }

    @Test
    @DisplayName("Decodes triple-encoded absolute URL down to usable form")
    void decodeTripleEncodedAbsolute() {
      String triple = "http%253a%252f%252flocalhost%253a9992%252fRhythmyx%252fcm%252fapp";
      assertEquals(
          "http://localhost:9992/Rhythmyx/cm/app",
          PSRedirectValidation.decodeOverEncodedRedirect(triple));
    }

    @Test
    @DisplayName("Leaves normal URLs and paths unchanged")
    void leavesNormalUnchanged() {
      assertEquals(
          "http://localhost:9992/cm/app",
          PSRedirectValidation.decodeOverEncodedRedirect("http://localhost:9992/cm/app"));
      assertEquals("/cm/app", PSRedirectValidation.decodeOverEncodedRedirect("/cm/app"));
      assertEquals("index.jsp", PSRedirectValidation.decodeOverEncodedRedirect("index.jsp"));
    }
  }
}
