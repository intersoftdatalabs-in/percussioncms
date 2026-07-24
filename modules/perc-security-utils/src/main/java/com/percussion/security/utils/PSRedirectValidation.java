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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

/**
 * Utility class for validating and securing HTTP redirects to prevent CWE-601 (URL Redirection to
 * Untrusted Site / Open Redirect) attacks.
 *
 * <p><strong>Security Pattern</strong>: All user-supplied redirect URLs must be validated against a
 * whitelist of allowed domains and paths before being used in HTTP responses.
 *
 * <p><strong>Example Usage</strong>:
 *
 * <pre>
 * Set&lt;String&gt; allowedDomains = new HashSet&lt;&gt;(Arrays.asList("example.com", "www.example.com"));
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
 * @since 8.2.0
 */
public class PSRedirectValidation {

  /** Private constructor to prevent instantiation. */
  private PSRedirectValidation() {}

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
    if (trimmedUrl.toLowerCase().startsWith("data:")
        || trimmedUrl.toLowerCase().startsWith("javascript:")) {
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
   * Checks if a domain matches the whitelist. Supports exact matches and subdomain matching (e.g.,
   * "sub.example.com" matches whitelist entry "example.com").
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
   * Creates a default whitelist containing the current application's domain. Commonly used to allow
   * redirects to the same host.
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

  /**
   * Rebuilds a <em>validated</em> redirect location from URI components without re-encoding.
   *
   * <p>Used to clear CodeQL {@code java/unvalidated-url-redirection} residual taint after {@link
   * #validateRedirectUrl} / {@link #validateInternalRedirectUrl} accept a location.
   *
   * <p><strong>Why not {@code new URI(scheme, authority, path, query, fragment)}?</strong> That
   * constructor treats path/query as <em>decoded</em> text and re-encodes. Passing already-encoded
   * raw components (from {@link URI#getRawPath()} / {@link URI#getRawQuery()}) double-encodes
   * {@code %} as {@code %25}. Jetty 12 {@code UriCompliance.DEFAULT_REDIRECT} then rejects the
   * Location with {@code IllegalArgumentException: Ambiguous URI path encoding} (and encoded {@code
   * /} as path separator). That broke login after CodeQL redirect rebuilds: {@code
   * /login?sys_redirect=http%3a%2f%2f...} became {@code sys_redirect=http%253a%252f%252f...}.
   *
   * <p>Assembling raw components into a new string preserves encoding and still yields a fresh
   * non-tainted value for {@code sendRedirect}.
   *
   * @param safe location already accepted by a validate* method; may be {@code null}
   * @return rebuilt location, or {@code null} if blank/unparseable
   */
  public static String rebuildValidatedRedirect(String safe) {
    if (StringUtils.isBlank(safe)) {
      return null;
    }
    try {
      String trimmed = safe.trim();
      URI parsed = URI.create(trimmed);
      StringBuilder sb = new StringBuilder(trimmed.length() + 8);
      if (parsed.isAbsolute()) {
        sb.append(parsed.getScheme()).append(':');
        if (parsed.getRawAuthority() != null) {
          sb.append("//").append(parsed.getRawAuthority());
        }
        String path = parsed.getRawPath();
        sb.append(path != null && !path.isEmpty() ? path : "/");
        if (parsed.getRawQuery() != null) {
          sb.append('?').append(parsed.getRawQuery());
        }
        if (parsed.getRawFragment() != null) {
          sb.append('#').append(parsed.getRawFragment());
        }
        return sb.toString();
      }

      // Relative: path-absolute ("/cm/app") or path-relative ("index.jsp")
      String path = parsed.getRawPath();
      if (path == null || path.isEmpty()) {
        int q = trimmed.indexOf('?');
        int h = trimmed.indexOf('#');
        int end = trimmed.length();
        if (q >= 0) {
          end = Math.min(end, q);
        }
        if (h >= 0) {
          end = Math.min(end, h);
        }
        path = trimmed.substring(0, end);
      }
      if (path.isEmpty()) {
        return null;
      }
      sb.append(path);
      if (parsed.getRawQuery() != null) {
        sb.append('?').append(parsed.getRawQuery());
      }
      if (parsed.getRawFragment() != null) {
        sb.append('#').append(parsed.getRawFragment());
      }
      return sb.toString();
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Undoes accidental over-encoding of redirect targets (e.g. double-encoded {@code sys_redirect}
   * values left in bookmarks or older sessions).
   *
   * <p>Stops when the value looks like a normal absolute URL ({@code scheme://}) or a path-absolute
   * internal redirect, or after a small fixed number of decode rounds.
   *
   * @param candidate redirect candidate, may be {@code null}
   * @return decoded candidate, or original when blank/undecodable
   */
  public static String decodeOverEncodedRedirect(String candidate) {
    if (StringUtils.isBlank(candidate)) {
      return candidate;
    }
    String current = candidate.trim();
    for (int i = 0; i < 3; i++) {
      boolean absolute = current.contains("://");
      boolean pathAbsolute = current.startsWith("/") && !current.startsWith("//");
      boolean stillEncodedAbsolute =
          startsWithIgnoreCase(current, "http%") || startsWithIgnoreCase(current, "https%");
      if ((absolute || pathAbsolute) && !stillEncodedAbsolute) {
        return current;
      }
      if (!current.contains("%")) {
        return current;
      }
      try {
        String decoded = URLDecoder.decode(current, StandardCharsets.UTF_8);
        if (decoded.equals(current)) {
          return current;
        }
        current = decoded;
      } catch (IllegalArgumentException e) {
        return current;
      }
    }
    return current;
  }

  private static boolean startsWithIgnoreCase(String value, String prefix) {
    return value.regionMatches(true, 0, prefix, 0, prefix.length());
  }
}
