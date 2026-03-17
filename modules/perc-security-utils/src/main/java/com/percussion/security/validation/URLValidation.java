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

package com.percussion.security.validation;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for validating URLs against SSRF (Server-Side Request Forgery) attacks (CWE-918).
 *
 * <p>Provides methods to validate URLs before making network requests, preventing attacks that
 * attempt to access internal services or private networks through application-initiated
 * connections.
 *
 * <p><strong>Default Behavior (Most Restrictive):</strong>
 *
 * <ul>
 *   <li>✅ Always allows: localhost, 127.0.0.1, ::1 (IPv6 loopback) on <strong>any</strong> port
 *   <li>✅ Allows standard ports: 80 (HTTP), 443 (HTTPS) for external hosts
 *   <li>❌ Blocks: Private IP ranges (10.x, 172.16-31.x, 192.168.x) by default
 *   <li>❌ Blocks: AWS metadata, reserved addresses (0.0.0.0, etc.)
 *   <li>❌ Blocks: Dangerous protocols (file, ftp, gopher, jar, netdoc)
 * </ul>
 *
 * <p><strong>Configuration for Different Deployments:</strong>
 *
 * <p>For <strong>CMS (Private Network Publishing)</strong>:
 *
 * <pre>
 * -Dpercussion.url.validation.allowed.ports=9992,8080
 * -Dpercussion.url.validation.allowed.ip.ranges=10.0.0.0/8
 * </pre>
 *
 * <p>For <strong>DTS (Reverse Proxy Setup)</strong>:
 *
 * <pre>
 * -Dpercussion.url.validation.allowed.ports=9980,8443
 * -Dpercussion.url.validation.allowed.hosts=internal-cms.local
 * </pre>
 *
 * <p>To allow all private networks (less secure):
 *
 * <pre>
 * -Dpercussion.url.validation.allow.private.networks=true
 * </pre>
 *
 * <p>Reference: OWASP SSRF Prevention Cheat Sheet
 *
 * @author Sunny Sal the Senior Java Developer
 * @since Java 21
 */
public class URLValidation {

  // Dangerous protocols that should not be allowed (blocked implicitly - only http/https allowed)
  // private static final Set<String> DANGEROUS_PROTOCOLS =
  //     new HashSet<>(Arrays.asList("file", "ftp", "gopher", "jar", "netdoc"));

  // Safe protocols for remote connections
  private static final Set<String> SAFE_PROTOCOLS = new HashSet<>(Arrays.asList("http", "https"));

  // Cloud metadata services and other reserved addresses that should be blocked
  private static final Set<String> BLOCKED_HOSTNAMES =
      new HashSet<>(
          Arrays.asList(
              "0.0.0.0", "169.254.169.254" // AWS metadata service
              ));

  // Always allow loopback addresses (localhost)
  private static final Set<String> LOOPBACK_HOSTNAMES =
      new HashSet<>(Arrays.asList("localhost", "127.0.0.1", "::1"));

  private URLValidation() {
    // Utility class - no instantiation
  }

  /**
   * Validates a URL to prevent SSRF attacks using the default configuration.
   *
   * <p>Checks:
   *
   * <ul>
   *   <li>Protocol is in the safe list (http, https only)
   *   <li>Host is not a dangerous address or blocked by configuration
   *   <li>Port is allowed (80/443 for non-localhost, or configured allowed ports)
   * </ul>
   *
   * <p><strong>Always allows localhost/127.0.0.1/::1 on any port</strong> (safe for inter-service
   * communication in same tier)
   *
   * @param url the URL to validate
   * @throws IllegalArgumentException if URL is null or malformed
   * @throws SecurityException if URL is unsafe (SSRF risk detected)
   */
  public static void validateURL(URL url) {
    validateURL(url, URLValidationConfig.getDefault());
  }

  /**
   * Validates a URL using a custom configuration.
   *
   * @param url the URL to validate
   * @param config custom validation configuration
   * @throws IllegalArgumentException if URL is null
   * @throws SecurityException if URL is unsafe (SSRF risk detected)
   */
  public static void validateURL(URL url, URLValidationConfig config) {
    if (url == null) {
      throw new IllegalArgumentException("URL cannot be null");
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

    String hostLower = host.toLowerCase();
    int port = url.getPort(); // Returns -1 if port not explicitly specified

    // ✅ ALWAYS ALLOW: Localhost/loopback on any port (safe for internal services)
    // Handle IPv6 addresses that come with brackets [::1]
    String normalizedHost = hostLower.replaceAll("[\\[\\]]", "");
    if (LOOPBACK_HOSTNAMES.contains(normalizedHost)) {
      // Localhost is always safe - CMS at localhost:9992, DTS at localhost:9980, etc.
      return;
    }

    // ❌ ALWAYS BLOCK: Dangerous reserved addresses
    if (BLOCKED_HOSTNAMES.contains(normalizedHost)) {
      throw new SecurityException(
          String.format("Cannot connect to reserved/metadata address: %s", host));
    }

    // ✅ Check if host is explicitly allowed in config
    if (config.isHostAllowed(host)) {
      // Host is in allow-list from config, permit it
      return;
    }

    // ❌ BLOCK: Private IPs and cloud metadata
    if (!config.arePrivateNetworksAllowed()) {
      if (isPrivateIPAddress(normalizedHost) || isCloudMetadataAddress(normalizedHost)) {
        // Check if it's in an allowed IP range
        if (!config.isIPRangeAllowed(normalizedHost)) {
          throw new SecurityException(
              String.format(
                  "Cannot connect to private IP address: %s. Configure"
                      + " percussion.url.validation.allowed.ip.ranges to allow specific ranges.",
                  host));
        }
      }
    } else {
      // Private networks are allowed - check if explicit port is allowed
      if (isPrivateIPAddress(normalizedHost) && port > 0 && !config.isPortAllowed(port, false)) {
        throw new SecurityException(
            String.format(
                "Port %d is not allowed for private network. Configure"
                    + " percussion.url.validation.allowed.ports to allow additional ports.",
                port));
      }
    }

    // ❌ DEFAULT: For non-loopback external hosts, validate port
    // Port -1 means no port specified, so use protocol default: 80 for http, 443 for https
    // Only validate if an explicit port was specified (port > 0)
    if (port > 0 && !config.isPortAllowed(port, false)) {
      throw new SecurityException(
          String.format(
              "Port %d is not allowed. "
                  + "Configure percussion.url.validation.allowed.ports to allow additional ports.",
              port));
    }
  }

  /**
   * Validates a URL string before creating a URL object. Uses default configuration.
   *
   * @param urlString the URL string to validate
   * @return the validated URL
   * @throws MalformedURLException if URL is malformed
   * @throws SecurityException if URL is unsafe (SSRF risk detected)
   */
  public static URL validateURLString(String urlString) throws MalformedURLException {
    return validateURLString(urlString, URLValidationConfig.getDefault());
  }

  /**
   * Validates a URL string using a custom configuration.
   *
   * @param urlString the URL string to validate
   * @param config custom validation configuration
   * @return the validated URL
   * @throws MalformedURLException if URL is malformed
   * @throws SecurityException if URL is unsafe (SSRF risk detected)
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

  /**
   * Checks if a hostname is a private IP address (RFC 1918 ranges and other reserved ranges).
   *
   * <p>Blocks: 10.0.0.0/8 | 172.16.0.0/12 | 192.168.0.0/16 | Multicast (224-239) | Reserved (240+)
   *
   * @param host hostname or IP address
   * @return true if the host appears to be a private IP address
   */
  private static boolean isPrivateIPAddress(String host) {
    // Try to parse as IPv4
    String[] parts = host.split("\\.");
    if (parts.length == 4) {
      try {
        int[] octets = new int[4];
        for (int i = 0; i < 4; i++) {
          octets[i] = Integer.parseInt(parts[i]);
          if (octets[i] < 0 || octets[i] > 255) {
            return false; // Not a valid IPv4 address
          }
        }

        // Check RFC 1918 private ranges
        if (octets[0] == 10) {
          return true; // 10.0.0.0/8
        }
        if (octets[0] == 172 && octets[1] >= 16 && octets[1] <= 31) {
          return true; // 172.16.0.0/12
        }
        if (octets[0] == 192 && octets[1] == 168) {
          return true; // 192.168.0.0/16
        }

        // Check other reserved ranges
        if (octets[0] == 0) {
          return true; // 0.0.0.0/8 (This network)
        }
        if (octets[0] >= 224 && octets[0] <= 239) {
          return true; // 224.0.0.0/4 (Multicast)
        }
        if (octets[0] >= 240) {
          return true; // 240.0.0.0/4 (Reserved)
        }
      } catch (NumberFormatException e) {
        // Not a valid IPv4 address, might be hostname
        return false;
      }
    }

    return false;
  }

  /**
   * Checks if a host is a cloud metadata service or other sensitive internal address.
   *
   * @param host hostname or IP address
   * @return true if host appears to be a cloud metadata service
   */
  private static boolean isCloudMetadataAddress(String host) {
    String hostLower = host.toLowerCase();
    return hostLower.equals("169.254.169.254") // AWS metadata
        || hostLower.equals("metadata.google.internal") // GCP metadata
        || hostLower.equals("169.254.169.253"); // Azure metadata variant
  }
}
