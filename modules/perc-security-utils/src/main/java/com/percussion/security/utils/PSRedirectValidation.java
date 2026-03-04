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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

/**
 * Utility class for validating and securing HTTP redirects to prevent CWE-601 (URL Redirection
 * to Untrusted Site / Open Redirect) attacks.
 *
 * <p><strong>Security Pattern</strong>: All user-supplied redirect URLs must be validated against
 * a whitelist of allowed domains and paths before being used in HTTP responses.
 *
 * <p><strong>Example Usage</strong>:
 *
 * <pre>
 * Set<String> allowedDomains = new HashSet<>(Arrays.asList("example.com", "www.example.com"));
 * String redirectUrl = request.getParameter("redirect");
 * String safeUrl = PSRedirectValidation.validateRedirectUrl(redirectUrl, allowedDomains);
 * if (safeUrl != null) {
 *     response.sendRedirect(safeUrl);
 * } else {
 *     // Log attack attempt and redirect to safe default
 *     log.warn("Attempted open redirect with URL: {}", redirectUrl);
 *     response.sendRedirect("/default-page");
 * }
 * </pre>
 *
 * @see <a href="https://owasp.org/www-community/attacks/Open_Redirect">OWASP Open Redirect</a>
 * @see <a href="https://cwe.mitre.org/data/definitions/601.html">CWE-601: URL Redirection to
 *     Untrusted Site</a>
 * @author Sunny Sal (GitHub Copilot)
 */
public class PSRedirectValidation {

  /**
   * Validates a redirect URL against a whitelist of allowed domains. Only relative URLs or URLs
   * pointing to allowed domains are accepted.
   *
   * <p><strong>Security Rationale</strong>:
   *
   * <ul>
   *   <li>Relative URLs (starting with `/`) are always safe (internal redirects)
   *   <li>Absolute URLs must have a host that matches the whitelist
   *   <li>Protocol-relative URLs (starting with `//`) are rejected (CWE-601 vector)
   *   <li>Data URIs and JavaScript URIs are rejected
   * </ul>
   *
   * @param redirectUrl the user-supplied redirect URL to validate
   * @param allowedDomains set of allowed domains (e.g., "example.com", "www.example.com")
   * @return the validated URL if safe, or null if malicious/invalid
   * @throws IllegalArgumentException if redirectUrl is null
   */
  public static String validateRedirectUrl(String redirectUrl, Set<String> allowedDomains) {
    if (redirectUrl == null) {
      throw new IllegalArgumentException("Redirect URL cannot be null");
    }

    String trimmedUrl = redirectUrl.trim();

    // Empty URL is invalid
    if (trimmedUrl.isEmpty()) {
      return null;
    }

    // Reject protocol-relative URLs (CWE-601 vector: //evil.com)
    if (trimmedUrl.startsWith("//")) {
      return null;
    }

    // Reject data URIs and JavaScript URIs
    if (trimmedUrl.toLowerCase().startsWith("data:") || trimmedUrl.toLowerCase().startsWith("javascript:")) {
      return null;
    }

    // Relative URLs (internal redirects) are always safe
    if (trimmedUrl.startsWith("/")) {
      // Verify no directory traversal attempts in path
      if (trimmedUrl.contains("..")) {
        return null;
      }
      return trimmedUrl;
    }

    // Absolute URLs must match a whitelisted domain
    try {
      URI uri = new URI(trimmedUrl);
      String host = uri.getHost();

      // Validate the host matches whitelist
      if (host != null && isAllowedDomain(host, allowedDomains)) {
        // Only allow http and https schemes
        String scheme = uri.getScheme();
        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
          return trimmedUrl;
        }
      }
    } catch (URISyntaxException e) {
      // Invalid URL format
      return null;
    }

    // URL is not whitelisted
    return null;
  }

  /**
   * Validates a redirect URL as a safe internal path-only redirect. Rejects any absolute URLs.
   *
   * <p><strong>Security Rationale</strong>: When only internal paths are expected, reject all
   * absolute URLs regardless of domain.
   *
   * @param redirectUrl the user-supplied redirect URL
   * @return the validated URL if safe (and relative), or null if invalid/absolute
   * @throws IllegalArgumentException if redirectUrl is null
   */
  public static String validateInternalRedirectUrl(String redirectUrl) {
    if (redirectUrl == null) {
      throw new IllegalArgumentException("Redirect URL cannot be null");
    }

    String trimmedUrl = redirectUrl.trim();

    // Empty URL is invalid
    if (trimmedUrl.isEmpty()) {
      return null;
    }

    // Reject any absolute URLs or special URIs
    if (trimmedUrl.startsWith("http://")
        || trimmedUrl.startsWith("https://")
        || trimmedUrl.startsWith("//")
        || trimmedUrl.toLowerCase().startsWith("data:")
        || trimmedUrl.toLowerCase().startsWith("javascript:")) {
      return null;
    }

    // Must be a relative path starting with /
    if (!trimmedUrl.startsWith("/")) {
      return null;
    }

    // Reject directory traversal attempts
    if (trimmedUrl.contains("..")) {
      return null;
    }

    return trimmedUrl;
  }

  /**
   * Checks if a domain matches the whitelist. Supports exact matches and subdomain matching
   * (e.g., "sub.example.com" matches whitelist entry "example.com").
   *
   * @param host the host to check
   * @param allowedDomains set of allowed domains
   * @return true if the host is in the whitelist or is a subdomain of a whitelisted domain
   */
  private static boolean isAllowedDomain(String host, Set<String> allowedDomains) {
    if (host == null || allowedDomains == null || allowedDomains.isEmpty()) {
      return false;
    }

    String hostLower = host.toLowerCase();

    for (String allowedDomain : allowedDomains) {
      String domainLower = allowedDomain.toLowerCase();

      // Exact match
      if (hostLower.equals(domainLower)) {
        return true;
      }

      // Subdomain match (host must end with .domain)
      if (hostLower.endsWith("." + domainLower)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Creates a default whitelist containing the current application's domain. Commonly used to
   * allow redirects to the same host.
   *
   * @param currentDomain the application's domain (e.g., "example.com")
   * @return a whitelist set containing the provided domain
   */
  public static Set<String> createDefaultWhitelist(String currentDomain) {
    Set<String> whitelist = new HashSet<>();
    if (StringUtils.isNotBlank(currentDomain)) {
      whitelist.add(currentDomain);
      // Also allow www variant
      if (!currentDomain.startsWith("www.")) {
        whitelist.add("www." + currentDomain);
      }
    }
    return whitelist;
  }
}
