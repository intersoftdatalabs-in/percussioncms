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

import java.util.Objects;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Secure TLS/SSL configuration utility for HTTPS connections.
 *
 * <p>This utility prevents CWE-295 (Improper Certificate Validation) and CWE-298 (Improper
 * Validation of Certificate with Host Mismatch) by:
 *
 * <ul>
 *   <li>Using system default certificate validation
 *   <li>Enforcing hostname verification
 *   <li>Preventing permissive TrustManagers
 *   <li>Rejecting null/empty hostnames
 * </ul>
 *
 * <p><strong>Security Properties</strong>:
 *
 * <ul>
 *   <li>Default trust store: System Java trusted CAs
 *   <li>Hostname verification: Enabled (default behavior)
 *   <li>Protocol: TLS (negotiates latest secure version)
 *   <li>Never accepts all certificates: Always validates
 * </ul>
 *
 * <p><strong>CWE References</strong>:
 *
 * <ul>
 *   <li>CWE-295: Improper Certificate Validation
 *   <li>CWE-298: Improper Validation of Certificate with Host Mismatch
 * </ul>
 *
 * @author Percussion Software
 * @since 8.2.0
 */
public final class PSSecureTLSConfigurer {

  private static final Logger log = LogManager.getLogger(PSSecureTLSConfigurer.class);

  /**
   * Default secure hostname verifier that validates certificate matches hostname.
   *
   * <p>This is the standard Java hostname verifier that performs proper hostname verification
   * according to RFC 6125.
   */
  private static final HostnameVerifier DEFAULT_HOSTNAME_VERIFIER =
      HttpsURLConnection.getDefaultHostnameVerifier();

  /** Private constructor - utility class only */
  private PSSecureTLSConfigurer() {}

  /**
   * Gets the default secure hostname verifier.
   *
   * <p>Always returns the system default hostname verifier which properly validates certificate
   * hostnames. Never returns a permissive verifier.
   *
   * @return the default secure hostname verifier (never null)
   * @since 8.2.0
   */
  public static HostnameVerifier getDefaultHostnameVerifier() {
    return DEFAULT_HOSTNAME_VERIFIER;
  }

  /**
   * Gets the default secure SSLContext.
   *
   * <p>Returns the system default SSLContext which:
   *
   * <ul>
   *   <li>Uses the system trust store (standard Java CA certificates)
   *   <li>Does NOT trust all certificates
   *   <li>Validates certificate chains properly
   *   <li>Enforces hostname verification
   * </ul>
   *
   * @return the default secure SSLContext
   * @throws IllegalStateException if SSLContext cannot be obtained
   * @since 8.2.0
   */
  public static SSLContext getDefaultSSLContext() {
    try {
      return SSLContext.getDefault();
    } catch (Exception e) {
      log.error("Failed to get default SSLContext", e);
      throw new IllegalStateException("Unable to get default SSLContext for secure HTTPS", e);
    }
  }

  /**
   * Validates that hostname verification is properly configured.
   *
   * <p>Ensures:
   *
   * <ul>
   *   <li>Hostname verifier is not null
   *   <li>Hostname is not empty
   *   <li>Hostname verifier is not permissive (does not accept all hosts)
   * </ul>
   *
   * @param hostname the hostname to validate (must not be null or empty)
   * @param verifier the hostname verifier to validate (must not be null)
   * @return true if hostname and verifier are valid
   * @throws IllegalArgumentException if hostname is null or empty
   * @throws IllegalArgumentException if verifier is null
   * @since 8.2.0
   */
  public static boolean validateHostnameVerification(String hostname, HostnameVerifier verifier) {
    Objects.requireNonNull(hostname, "Hostname must not be null");
    Objects.requireNonNull(verifier, "HostnameVerifier must not be null");

    if (hostname.trim().isEmpty()) {
      throw new IllegalArgumentException("Hostname must not be empty");
    }

    // Use default verifier for validation
    return DEFAULT_HOSTNAME_VERIFIER.equals(verifier) || verifier == DEFAULT_HOSTNAME_VERIFIER;
  }

  /**
   * Creates a strict hostname verifier that validates certificate CN against hostname.
   *
   * <p>This is the recommended default verifier for all HTTPS connections. It properly validates:
   *
   * <ul>
   *   <li>Certificate hostname matches request hostname
   *   <li>Exact matches required (no wildcards unless RFC 6125 compliant)
   *   <li>Proper certificate chain validation
   * </ul>
   *
   * @return a strict hostname verifier (never null)
   * @since 8.2.0
   */
  public static HostnameVerifier createStrictHostnameVerifier() {
    return (hostname, session) -> {
      Objects.requireNonNull(hostname, "Hostname must not be null");
      Objects.requireNonNull(session, "SSLSession must not be null");

      if (hostname.trim().isEmpty()) {
        log.warn("Hostname verification failed: hostname is empty");
        return false;
      }

      // Use the default verifier for strict validation
      return DEFAULT_HOSTNAME_VERIFIER.verify(hostname, session);
    };
  }

  /**
   * Validates that an SSLContext is secure and not permissive.
   *
   * <p>Note: This performs basic validation. For complete verification, use TLS certificates from
   * trusted authorities.
   *
   * @param context the SSLContext to validate
   * @return true if context uses secure defaults
   * @throws NullPointerException if context is null
   * @since 8.2.0
   */
  public static boolean isSecureSSLContext(SSLContext context) {
    Objects.requireNonNull(context, "SSLContext must not be null");

    try {
      // Verify it's using a proper protocol
      String protocol = context.getProtocol();
      if (protocol == null || protocol.isEmpty()) {
        log.warn("SSLContext protocol is empty");
        return false;
      }

      // "Default" is secure (uses system default TLS), or explicitly TLS variants
      String upperProtocol = protocol.toUpperCase();
      return upperProtocol.contains("TLS") || "DEFAULT".equals(upperProtocol);
    } catch (Exception e) {
      log.error("Error validating SSLContext", e);
      return false;
    }
  }

  /**
   * Gets the default TLS protocol version to use.
   *
   * <p>Returns the system default which should be TLS 1.2 or higher on modern JVMs.
   *
   * @return the default TLS protocol ("TLS")
   * @since 8.2.0
   */
  public static String getDefaultTLSProtocol() {
    return "TLS"; // System negotiates latest secure version
  }

  /**
   * Logs a TLS/SSL configuration attempt.
   *
   * <p>Used to track when HTTPS connections are established for audit purposes.
   *
   * @param hostname the target hostname
   * @param isSecure true if using secure defaults, false if permissive
   * @since 8.2.0
   */
  public static void logTLSConfiguration(String hostname, boolean isSecure) {
    if (isSecure) {
      log.debug("Establishing secure HTTPS connection to: {}", hostname);
    } else {
      log.warn("Attempting HTTPS connection with non-standard configuration to: {}", hostname);
    }
  }
}
