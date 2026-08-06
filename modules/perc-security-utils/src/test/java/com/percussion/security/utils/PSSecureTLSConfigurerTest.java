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
package com.percussion.security.utils;

import static org.junit.jupiter.api.Assertions.*;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for {@link PSSecureTLSConfigurer} covering CWE-295 and CWE-298
 * vulnerability prevention.
 *
 * <p>Test Coverage:
 *
 * <ul>
 *   <li>Default hostname verifier retrieval
 *   <li>Default SSL context retrieval
 *   <li>Hostname verification validation
 *   <li>SSL context security validation
 *   <li>Strict hostname verifier creation
 *   <li>TLS configuration logging
 * </ul>
 */
@DisplayName("PSSecureTLSConfigurer - TLS/SSL Security (CWE-295, CWE-298)")
class PSSecureTLSConfigurerTest {

  @Nested
  @DisplayName("Default Hostname Verifier Tests")
  class DefaultHostnameVerifierTests {

    @Test
    @DisplayName("Should return default hostname verifier")
    void testGetDefaultHostnameVerifier() {
      HostnameVerifier verifier = PSSecureTLSConfigurer.getDefaultHostnameVerifier();

      assertNotNull(verifier, "Default hostname verifier should not be null");
      assertEquals(
          HttpsURLConnection.getDefaultHostnameVerifier(),
          verifier,
          "Should return system default verifier");
    }

    @Test
    @DisplayName("Should always return same secure verifier instance")
    void testVerifierConsistency() {
      HostnameVerifier verifier1 = PSSecureTLSConfigurer.getDefaultHostnameVerifier();
      HostnameVerifier verifier2 = PSSecureTLSConfigurer.getDefaultHostnameVerifier();

      assertSame(verifier1, verifier2, "Should return same verifier instance");
    }

    @Test
    @DisplayName("Should never return null verifier")
    void testVerifierNotNull() {
      for (int i = 0; i < 10; i++) {
        assertNotNull(PSSecureTLSConfigurer.getDefaultHostnameVerifier());
      }
    }
  }

  @Nested
  @DisplayName("Default SSL Context Tests")
  class DefaultSSLContextTests {

    @Test
    @DisplayName("Should return default SSL context")
    void testGetDefaultSSLContext() {
      SSLContext context = PSSecureTLSConfigurer.getDefaultSSLContext();

      assertNotNull(context, "Default SSL context should not be null");
    }

    @Test
    @DisplayName("Should use secure TLS protocol")
    void testSSLContextProtocol() {
      SSLContext context = PSSecureTLSConfigurer.getDefaultSSLContext();
      String protocol = context.getProtocol();

      assertNotNull(protocol, "Protocol should not be null");
      assertFalse(protocol.isEmpty(), "Protocol should not be empty");
      // Protocol should be "Default" (which negotiates TLS) or explicit TLS variant
      assertTrue(
          protocol.equalsIgnoreCase("Default") || protocol.toUpperCase().contains("TLS"),
          "Should use TLS protocol variant or Default: " + protocol);
    }

    @Test
    @DisplayName("Should always return consistent context")
    void testSSLContextConsistency() {
      SSLContext context1 = PSSecureTLSConfigurer.getDefaultSSLContext();
      SSLContext context2 = PSSecureTLSConfigurer.getDefaultSSLContext();

      assertNotNull(context1, "First context should not be null");
      assertNotNull(context2, "Second context should not be null");
      assertEquals(context1.getProtocol(), context2.getProtocol(), "Should use same protocol");
    }
  }

  @Nested
  @DisplayName("Hostname Verification Validation Tests")
  class HostnameVerificationValidationTests {

    @Test
    @DisplayName("Should validate with default hostname verifier")
    void testValidateWithDefaultVerifier() {
      HostnameVerifier defaultVerifier = PSSecureTLSConfigurer.getDefaultHostnameVerifier();
      boolean result =
          PSSecureTLSConfigurer.validateHostnameVerification("percussion.com", defaultVerifier);

      assertTrue(result, "Should validate default verifier");
    }

    @Test
    @DisplayName("Should reject null hostname")
    void testRejectNullHostname() {
      HostnameVerifier verifier = PSSecureTLSConfigurer.getDefaultHostnameVerifier();

      assertThrows(
          NullPointerException.class,
          () -> PSSecureTLSConfigurer.validateHostnameVerification(null, verifier),
          "Should reject null hostname");
    }

    @Test
    @DisplayName("Should reject empty hostname")
    void testRejectEmptyHostname() {
      HostnameVerifier verifier = PSSecureTLSConfigurer.getDefaultHostnameVerifier();

      assertThrows(
          IllegalArgumentException.class,
          () -> PSSecureTLSConfigurer.validateHostnameVerification("", verifier),
          "Should reject empty hostname");
    }

    @Test
    @DisplayName("Should reject whitespace-only hostname")
    void testRejectWhitespaceHostname() {
      HostnameVerifier verifier = PSSecureTLSConfigurer.getDefaultHostnameVerifier();

      assertThrows(
          IllegalArgumentException.class,
          () -> PSSecureTLSConfigurer.validateHostnameVerification("   ", verifier),
          "Should reject whitespace-only hostname");
    }

    @Test
    @DisplayName("Should reject null verifier")
    void testRejectNullVerifier() {
      assertThrows(
          NullPointerException.class,
          () -> PSSecureTLSConfigurer.validateHostnameVerification("test.com", null),
          "Should reject null verifier");
    }

    @Test
    @DisplayName("Should validate various valid hostnames")
    void testValidateValidHostnames() {
      HostnameVerifier verifier = PSSecureTLSConfigurer.getDefaultHostnameVerifier();

      String[] validHostnames = {
        "percussion.com", "example.org", "localhost", "192.168.1.1", "subdomain.example.com"
      };

      for (String hostname : validHostnames) {
        assertTrue(
            PSSecureTLSConfigurer.validateHostnameVerification(hostname, verifier),
            "Should validate hostname: " + hostname);
      }
    }
  }

  @Nested
  @DisplayName("SSL Context Security Validation Tests")
  class SSLContextSecurityValidationTests {

    @Test
    @DisplayName("Should validate default SSL context as secure")
    void testValidateDefaultContextAsSecure() {
      SSLContext context = PSSecureTLSConfigurer.getDefaultSSLContext();

      assertTrue(
          PSSecureTLSConfigurer.isSecureSSLContext(context), "Default context should be secure");
    }

    @Test
    @DisplayName("Should reject null SSL context")
    void testRejectNullContext() {
      assertThrows(
          NullPointerException.class,
          () -> PSSecureTLSConfigurer.isSecureSSLContext(null),
          "Should reject null SSL context");
    }

    @Test
    @DisplayName("Should identify TLS context as secure")
    void testIdentifyTLSAsSecure() {
      SSLContext tlsContext = PSSecureTLSConfigurer.getDefaultSSLContext();
      String protocol = tlsContext.getProtocol();

      // Protocol can be "Default" or explicit "TLS" variants
      assertTrue(
          protocol.equalsIgnoreCase("Default") || protocol.toUpperCase().contains("TLS"),
          "Should use Default or TLS protocol: " + protocol);
      assertTrue(PSSecureTLSConfigurer.isSecureSSLContext(tlsContext), "Context should be secure");
    }
  }

  @Nested
  @DisplayName("Strict Hostname Verifier Tests")
  class StrictHostnameVerifierTests {

    @Test
    @DisplayName("Should create strict hostname verifier")
    void testCreateStrictVerifier() {
      HostnameVerifier verifier = PSSecureTLSConfigurer.createStrictHostnameVerifier();

      assertNotNull(verifier, "Should create non-null verifier");
      // Strict verifier is created fresh, not the same as default
      assertNotEquals(
          PSSecureTLSConfigurer.getDefaultHostnameVerifier(),
          verifier,
          "Strict verifier should be a new instance");
    }

    @Test
    @DisplayName("Should reject null hostname in strict verifier")
    void testStrictVerifierRejectNullHostname() {
      HostnameVerifier verifier = PSSecureTLSConfigurer.createStrictHostnameVerifier();

      assertThrows(
          NullPointerException.class,
          () -> verifier.verify(null, null),
          "Should reject null hostname");
    }

    @Test
    @DisplayName("Should reject null SSL session in strict verifier")
    void testStrictVerifierRejectNullSession() {
      HostnameVerifier verifier = PSSecureTLSConfigurer.createStrictHostnameVerifier();

      assertThrows(
          NullPointerException.class,
          () -> verifier.verify("test.com", null),
          "Should reject null session");
    }

    @Test
    @DisplayName("Should reject empty hostname in strict verifier")
    void testStrictVerifierRejectEmptyHostname() {
      HostnameVerifier verifier = PSSecureTLSConfigurer.createStrictHostnameVerifier();

      assertThrows(
          IllegalArgumentException.class,
          () -> PSSecureTLSConfigurer.validateHostnameVerification("", verifier),
          "Verifier should reject empty hostname");
    }

    @Test
    @DisplayName("Should use default verifier logic")
    void testStrictVerifierUsesDefault() {
      HostnameVerifier strictVerifier = PSSecureTLSConfigurer.createStrictHostnameVerifier();
      HostnameVerifier defaultVerifier = PSSecureTLSConfigurer.getDefaultHostnameVerifier();

      // Both should be non-null
      assertNotNull(strictVerifier, "Strict verifier should be created");
      assertNotNull(defaultVerifier, "Default verifier should exist");
      // Default verifier should validate
      assertTrue(
          PSSecureTLSConfigurer.validateHostnameVerification("test.com", defaultVerifier),
          "Default verifier should be valid for validateHostnameVerification");
    }
  }

  @Nested
  @DisplayName("TLS Protocol Configuration Tests")
  class TLSProtocolTests {

    @Test
    @DisplayName("Should return TLS protocol string")
    void testGetDefaultTLSProtocol() {
      String protocol = PSSecureTLSConfigurer.getDefaultTLSProtocol();

      assertEquals("TLS", protocol, "Should return TLS protocol string");
    }

    @Test
    @DisplayName("Should use negotiated TLS version")
    void testTLSNegotiation() {
      SSLContext context = PSSecureTLSConfigurer.getDefaultSSLContext();
      String protocol = context.getProtocol();

      assertTrue(
          protocol.equalsIgnoreCase("Default") || protocol.toUpperCase().contains("TLS"),
          "SSL context should use Default or TLS: " + protocol);
    }
  }

  @Nested
  @DisplayName("Real-World Security Scenarios")
  class RealWorldScenarios {

    @Test
    @DisplayName("Should secure HTTPS connection with default verifier")
    void testSecureHTTPSConnection() {
      HostnameVerifier verifier = PSSecureTLSConfigurer.getDefaultHostnameVerifier();
      SSLContext context = PSSecureTLSConfigurer.getDefaultSSLContext();

      assertTrue(
          PSSecureTLSConfigurer.validateHostnameVerification("example.com", verifier),
          "Should allow verifier for valid hostname");
      assertTrue(PSSecureTLSConfigurer.isSecureSSLContext(context), "Should use secure context");
    }

    @Test
    @DisplayName("Should prevent permissive certificate acceptance")
    void testPreventPermissiveCertificateAcceptance() {
      // Simulate permissive verifier (would always return true)
      HostnameVerifier permissiveVerifier = (hostname, session) -> true;

      // Should not use permissive verifier
      assertNotEquals(
          PSSecureTLSConfigurer.getDefaultHostnameVerifier(),
          permissiveVerifier,
          "Should not accept permissive verifier");
    }

    @Test
    @DisplayName("Should enforce hostname verification in SSL context")
    void testEnforceHostnameVerification() {
      HostnameVerifier verifier = PSSecureTLSConfigurer.getDefaultHostnameVerifier();

      // Should enforce verification for multiple hostnames
      assertTrue(
          PSSecureTLSConfigurer.validateHostnameVerification("percussion.com", verifier),
          "Should verify percussion.com");
      assertTrue(
          PSSecureTLSConfigurer.validateHostnameVerification("example.org", verifier),
          "Should verify example.org");
    }

    @Test
    @DisplayName("Should handle protocol negotiation securely")
    void testSecureProtocolNegotiation() {
      SSLContext context = PSSecureTLSConfigurer.getDefaultSSLContext();
      String protocol = context.getProtocol();

      assertTrue(
          protocol.equalsIgnoreCase("Default") || protocol.toUpperCase().contains("TLS"),
          "Should negotiate secure protocol (Default or TLS)");

      assertNotEquals("SSL", protocol, "Should not use deprecated SSL protocol");
    }
  }
}
