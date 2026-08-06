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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.security.validation;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Validates URLs against SSRF (CWE-918) for server-initiated outbound requests.
 *
 * <p>Decision order (issue #1205):
 *
 * <ol>
 *   <li>Scheme must be http or https
 *   <li>Hard-coded reserved/metadata hosts denied (defense in depth)
 *   <li>Block-list globs from {@code rxconfig/Server/blockedUrls.properties}
 *   <li>Baseline permit: loopback any port; non-private host on port -1/80/443
 *   <li>Allow-list globs from {@code rxconfig/Server/allowedUrls.properties} (additive; can unlock
 *       private hosts / nonstandard ports)
 *   <li>Otherwise deny
 * </ol>
 *
 * @see URLValidationConfig
 * @see URLListFileLoader
 */
public class URLValidation {

  private static final Set<String> SAFE_PROTOCOLS = new HashSet<>(Arrays.asList("http", "https"));

  private static final Set<String> HARD_BLOCKED_HOSTNAMES =
      new HashSet<>(
          Arrays.asList(
              "0.0.0.0", "169.254.169.254", "169.254.169.253", "metadata.google.internal"));

  private static final Set<String> LOOPBACK_HOSTNAMES =
      new HashSet<>(Arrays.asList("localhost", "127.0.0.1", "::1"));

  private URLValidation() {}

  /**
   * Convenience overload that validates {@code url} using the {@link
   * URLValidationConfig#getDefault() default configuration}.
   *
   * @param url the URL to validate, must not be {@code null}.
   * @throws IllegalArgumentException if {@code url} is {@code null}.
   * @throws SecurityException if the URL is rejected by the default validation configuration.
   */
  public static void validateURL(URL url) {
    validateURL(url, URLValidationConfig.getDefault());
  }

  /**
   * Validates a URL using the given configuration.
   *
   * @param url the URL to validate
   * @param config validation configuration (allow/block lists)
   * @throws IllegalArgumentException if URL is null
   * @throws SecurityException if URL is unsafe
   */
  public static void validateURL(URL url, URLValidationConfig config) {
    if (url == null) {
      throw new IllegalArgumentException("URL cannot be null");
    }
    if (config == null) {
      config = new URLValidationConfig();
    }

    String protocol = url.getProtocol();
    if (protocol == null || !SAFE_PROTOCOLS.contains(protocol.toLowerCase())) {
      throw new SecurityException(
          String.format(
              "Protocol '%s' is not allowed. Only http and https are permitted.", protocol));
    }

    String host = url.getHost();
    if (host == null || host.isEmpty()) {
      throw new SecurityException("URL host cannot be empty");
    }

    String normalizedHost = host.toLowerCase().replaceAll("[\\[\\]]", "");
    String normalized = URLGlobMatcher.normalize(url);

    // Hard-coded reserved / cloud metadata (defense in depth)
    if (HARD_BLOCKED_HOSTNAMES.contains(normalizedHost) || isCloudMetadataAddress(normalizedHost)) {
      throw new SecurityException(
          String.format("Cannot connect to reserved/metadata address: %s", host));
    }

    // Block list wins over baseline and allow
    if (config.matchesBlock(normalized)) {
      throw new SecurityException(
          String.format("URL is blocked by blockedUrls.properties pattern match: %s", host));
    }

    // Baseline: loopback any port
    if (LOOPBACK_HOSTNAMES.contains(normalizedHost)) {
      return;
    }

    // Baseline: non-private public host on default/80/443
    int port = url.getPort();
    boolean privateHost =
        isPrivateIPAddress(normalizedHost) || isCloudMetadataAddress(normalizedHost);
    if (!privateHost && isBaselinePublicPort(port)) {
      return;
    }

    // Additive allow list (may unlock private hosts / nonstandard ports)
    if (config.matchesAllow(normalized)) {
      return;
    }

    if (privateHost) {
      throw new SecurityException(
          String.format(
              "Cannot connect to private or restricted address: %s. Add an allow pattern in"
                  + " rxconfig/Server/allowedUrls.properties if this target is intentional.",
              host));
    }

    throw new SecurityException(
        String.format(
            "URL is not permitted by baseline rules or allowedUrls.properties: %s"
                + (port > 0 ? " (port " + port + ")" : ""),
            host));
  }

  /**
   * Convenience overload that parses {@code urlString} and validates it using the {@link
   * URLValidationConfig#getDefault() default configuration}.
   *
   * @param urlString the URL string to parse and validate, must not be {@code null} or empty.
   * @return the parsed {@link URL}.
   * @throws MalformedURLException if {@code urlString} is not a valid URL.
   * @throws IllegalArgumentException if {@code urlString} is {@code null} or empty.
   * @throws SecurityException if the parsed URL is rejected by the default validation
   *     configuration.
   */
  public static URL validateURLString(String urlString) throws MalformedURLException {
    return validateURLString(urlString, URLValidationConfig.getDefault());
  }

  /**
   * Parses {@code urlString} and validates it against {@code config}.
   *
   * @param urlString the URL string to parse and validate, must not be {@code null} or empty.
   * @param config validation configuration (allow/block lists); may be {@code null} to fall back to
   *     {@link URLValidationConfig#getDefault()}.
   * @return the parsed {@link URL}.
   * @throws MalformedURLException if {@code urlString} is not a valid URL.
   * @throws IllegalArgumentException if {@code urlString} is {@code null} or empty.
   * @throws SecurityException if the parsed URL is rejected by the supplied configuration.
   */
  public static URL validateURLString(String urlString, URLValidationConfig config)
      throws MalformedURLException {
    if (urlString == null || urlString.isEmpty()) {
      throw new IllegalArgumentException("URL string cannot be null or empty");
    }
    URL url = new URL(urlString);
    validateURL(url, config);
    return url;
  }

  private static boolean isBaselinePublicPort(int port) {
    // -1 = default port for scheme (80/443)
    return port < 0 || port == 80 || port == 443;
  }

  static boolean isPrivateIPAddress(String host) {
    String[] parts = host.split("\\.");
    if (parts.length == 4) {
      try {
        int[] octets = new int[4];
        for (int i = 0; i < 4; i++) {
          octets[i] = Integer.parseInt(parts[i]);
          if (octets[i] < 0 || octets[i] > 255) {
            return false;
          }
        }
        if (octets[0] == 10) {
          return true;
        }
        if (octets[0] == 172 && octets[1] >= 16 && octets[1] <= 31) {
          return true;
        }
        if (octets[0] == 192 && octets[1] == 168) {
          return true;
        }
        if (octets[0] == 0) {
          return true;
        }
        if (octets[0] >= 224 && octets[0] <= 239) {
          return true;
        }
        if (octets[0] >= 240) {
          return true;
        }
      } catch (NumberFormatException e) {
        return false;
      }
    }
    return false;
  }

  private static boolean isCloudMetadataAddress(String host) {
    String hostLower = host.toLowerCase();
    return hostLower.equals("169.254.169.254")
        || hostLower.equals("metadata.google.internal")
        || hostLower.equals("169.254.169.253");
  }
}
