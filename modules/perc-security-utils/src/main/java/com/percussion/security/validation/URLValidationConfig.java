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

import java.util.HashSet;
import java.util.Set;

/**
 * Configuration for URL validation rules. Allows customization of allowed networks and ports for
 * different deployment scenarios (CMS, DTS, etc.).
 *
 * <p>Default behavior:
 *
 * <ul>
 *   <li>Always allows: localhost, 127.0.0.1, ::1 on any port
 *   <li>Always blocks: private IP ranges (10.x, 172.16-31.x, 192.168.x), cloud metadata
 *   <li>Always blocks: dangerous protocols (file, ftp, gopher, jar, netdoc)
 *   <li>Uses only http/https protocols
 * </ul>
 *
 * <p>Customization via environment variables/system properties:
 *
 * <ul>
 *   <li>{@code percussion.url.validation.allow.private.networks=true|false}
 *   <li>{@code percussion.url.validation.allow.ports=8080,9080,9992,9980,8443} (comma-separated)
 *   <li>{@code percussion.url.validation.allowed.ip.ranges=10.0.0.0/8,172.16.0.0/12} (CIDR
 *       notation)
 *   <li>{@code percussion.url.validation.allowed.hosts=internal-api.local,cms-internal}
 *       (comma-separated)
 * </ul>
 *
 * @author Sunny Sal the Senior Java Developer
 */
public class URLValidationConfig {

  private static URLValidationConfig INSTANCE;

  private final Set<Integer> allowedPorts;
  private final Set<String> allowedHosts;
  private final Set<String> allowedIPRanges;
  private final boolean allowPrivateNetworks;
  private final boolean allowAllPorts;
  private final boolean allowAllHosts;

  /**
   * Creates default configuration that: - Allows localhost/loopback (127.0.0.1, ::1) on any port -
   * Blocks private IP ranges (10.x, 172.16.x, 192.168.x) - Allows only standard ports 80/443 for
   * non-localhost - Only allows http/https protocols
   */
  public URLValidationConfig() {
    this.allowedPorts = new HashSet<>();
    this.allowedHosts = new HashSet<>();
    this.allowedIPRanges = new HashSet<>();
    this.allowPrivateNetworks = false; // Default: block private networks
    this.allowAllPorts = false;
    this.allowAllHosts = false;

    // Load from system properties if configured
    loadFromProperties();
  }

  /**
   * Creates configuration with custom settings.
   *
   * @param allowedPorts specific ports to allow (in addition to 80, 443)
   * @param allowedHosts specific hostnames to allow
   * @param allowedIPRanges IP ranges in CIDR notation (e.g., "10.0.0.0/8")
   * @param allowPrivateNetworks whether to allow all RFC 1918 private networks
   */
  public URLValidationConfig(
      Set<Integer> allowedPorts,
      Set<String> allowedHosts,
      Set<String> allowedIPRanges,
      boolean allowPrivateNetworks) {
    this.allowedPorts = allowedPorts != null ? new HashSet<>(allowedPorts) : new HashSet<>();
    this.allowedHosts = allowedHosts != null ? new HashSet<>(allowedHosts) : new HashSet<>();
    this.allowedIPRanges =
        allowedIPRanges != null ? new HashSet<>(allowedIPRanges) : new HashSet<>();
    this.allowPrivateNetworks = allowPrivateNetworks;
    this.allowAllPorts = false;
    this.allowAllHosts = false;
  }

  /**
   * Gets the singleton default configuration.
   *
   * @return default URLValidationConfig instance
   */
  public static synchronized URLValidationConfig getDefault() {
    if (INSTANCE == null) {
      INSTANCE = new URLValidationConfig();
    }
    return INSTANCE;
  }

  /**
   * Sets a custom default configuration instance.
   *
   * @param config custom configuration to use as default
   */
  public static synchronized void setDefault(URLValidationConfig config) {
    INSTANCE = config;
  }

  private void loadFromProperties() {
    // Load allowed ports from properties
    String portsStr = System.getProperty("percussion.url.validation.allowed.ports");
    if (portsStr != null && !portsStr.trim().isEmpty()) {
      for (String port : portsStr.split(",")) {
        try {
          allowedPorts.add(Integer.parseInt(port.trim()));
        } catch (NumberFormatException e) {
          // Log warning but continue
          System.err.println("Invalid port in percussion.url.validation.allowed.ports: " + port);
        }
      }
    }

    // Load allowed hosts from properties
    String hostsStr = System.getProperty("percussion.url.validation.allowed.hosts");
    if (hostsStr != null && !hostsStr.trim().isEmpty()) {
      for (String host : hostsStr.split(",")) {
        allowedHosts.add(host.trim().toLowerCase());
      }
    }

    // Load allowed IP ranges from properties
    String rangesStr = System.getProperty("percussion.url.validation.allowed.ip.ranges");
    if (rangesStr != null && !rangesStr.trim().isEmpty()) {
      for (String range : rangesStr.split(",")) {
        allowedIPRanges.add(range.trim());
      }
    }
  }

  /**
   * Checks if a port is allowed.
   *
   * @param port the port number
   * @param isLoopback true if the host is localhost/127.0.0.1/::1
   * @return true if port is allowed
   */
  public boolean isPortAllowed(int port, boolean isLoopback) {
    if (allowAllPorts) {
      return true;
    }

    // Loopback always allows any port
    if (isLoopback) {
      return true;
    }

    // Standard web ports are always allowed
    if (port == 80 || port == 443) {
      return true;
    }

    // Check configured allowed ports
    return allowedPorts.contains(port);
  }

  /**
   * Checks if a host is explicitly allowed.
   *
   * @param host hostname to check
   * @return true if host is in the allowed hosts list
   */
  public boolean isHostAllowed(String host) {
    if (allowAllHosts || host == null) {
      return false;
    }
    return allowedHosts.contains(host.toLowerCase());
  }

  /**
   * Checks if private networks are allowed by configuration.
   *
   * @return true if private networks (10.x, 172.16.x, 192.168.x) are allowed
   */
  public boolean arePrivateNetworksAllowed() {
    return allowPrivateNetworks;
  }

  /**
   * Checks if an IP range is allowed.
   *
   * @param host IP address to check
   * @return true if host matches an allowed IP range (CIDR)
   */
  public boolean isIPRangeAllowed(String host) {
    if (allowedIPRanges.isEmpty()) {
      return false;
    }

    // Simple CIDR range checking for configured ranges
    for (String range : allowedIPRanges) {
      if (isIPInRange(host, range)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if an IP address falls within a CIDR range.
   *
   * @param ip IP address to check
   * @param cidrRange CIDR notation range (e.g., "10.0.0.0/8")
   * @return true if IP is in range
   */
  private boolean isIPInRange(String ip, String cidrRange) {
    try {
      String[] rangeParts = cidrRange.split("/");
      if (rangeParts.length != 2) {
        return false;
      }

      String networkStr = rangeParts[0];
      int prefixLength = Integer.parseInt(rangeParts[1]);

      String[] networkParts = networkStr.split("\\.");
      String[] ipParts = ip.split("\\.");

      if (networkParts.length != 4 || ipParts.length != 4) {
        return false;
      }

      long networkAddr = 0;
      long ipAddr = 0;

      for (int i = 0; i < 4; i++) {
        networkAddr = (networkAddr << 8) | (Integer.parseInt(networkParts[i]) & 0xFF);
        ipAddr = (ipAddr << 8) | (Integer.parseInt(ipParts[i]) & 0xFF);
      }

      long mask = (0xFFFFFFFFL << (32 - prefixLength)) & 0xFFFFFFFFL;
      return (networkAddr & mask) == (ipAddr & mask);
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Get a builder for creating custom configurations.
   *
   * @return URLValidationConfigBuilder
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for creating URLValidationConfig instances. */
  public static class Builder {
    private final Set<Integer> ports = new HashSet<>();
    private final Set<String> hosts = new HashSet<>();
    private final Set<String> ipRanges = new HashSet<>();
    private boolean allowPrivateNetworks = false;

    public Builder addPort(int port) {
      ports.add(port);
      return this;
    }

    public Builder addPorts(int... ports) {
      for (int port : ports) {
        this.ports.add(port);
      }
      return this;
    }

    public Builder addHost(String host) {
      hosts.add(host.toLowerCase());
      return this;
    }

    public Builder addIPRange(String cidrRange) {
      ipRanges.add(cidrRange);
      return this;
    }

    public Builder allowPrivateNetworks(boolean allow) {
      this.allowPrivateNetworks = allow;
      return this;
    }

    public URLValidationConfig build() {
      return new URLValidationConfig(ports, hosts, ipRanges, allowPrivateNetworks);
    }
  }
}
