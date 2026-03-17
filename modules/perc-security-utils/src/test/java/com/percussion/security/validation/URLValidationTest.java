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

import static org.junit.jupiter.api.Assertions.*;

import java.net.MalformedURLException;
import java.net.URL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for URLValidation - SSRF prevention (CWE-918).
 *
 * <p>Tests validate that:
 *
 * <ul>
 *   <li>Localhost/loopback (127.0.0.1, ::1, localhost) are ALWAYS allowed on any port
 *   <li>Private IPs (10.x, 172.16.x, 192.168.x) are BLOCKED by default but CONFIGURABLE
 *   <li>External URLs are restricted to standard ports (80, 443) by default
 *   <li>Cloud metadata services and reserved addresses are always blocked
 * </ul>
 */
@DisplayName("URLValidation - SSRF Prevention")
class URLValidationTest {

  @Nested
  @DisplayName("Public URL Tests")
  class PublicURLTests {

    @Test
    @DisplayName("Should allow valid HTTPS URLs")
    void testAllowValidHttpsUrl() throws MalformedURLException {
      URL validUrl = new URL("https://example.com/api/resource");
      URLValidation.validateURL(validUrl);
    }

    @Test
    @DisplayName("Should allow valid HTTP URLs on port 80")
    void testAllowValidHttpUrl() throws MalformedURLException {
      URL validUrl = new URL("http://example.com/path");
      URLValidation.validateURL(validUrl);
    }

    @Test
    @DisplayName("Should reject non-standard port 8080 for external hosts")
    void testRejectNonStandardPort8080() throws MalformedURLException {
      URL highPortUrl = new URL("http://example.com:8080/api");
      assertThrows(
          SecurityException.class,
          () -> URLValidation.validateURL(highPortUrl),
          "Non-standard port 8080 should be blocked for external hosts");
    }
  }

  @Nested
  @DisplayName("Localhost/Loopback Tests (Always Allowed for Internal Services)")
  class LocalhostTests {
    @Test
    @DisplayName("Should allow localhost:8080 (CMS inter-service)")
    void testAllowLocalhostHighPort() throws MalformedURLException {
      URL localhostUrl = new URL("http://localhost:8080/api");
      URLValidation.validateURL(localhostUrl);
    }

    @Test
    @DisplayName("Should allow localhost:9992 (CMS default port)")
    void testAllowLocalhostCMSPort() throws MalformedURLException {
      URL cmsUrl = new URL("http://localhost:9992");
      URLValidation.validateURL(cmsUrl);
    }

    @Test
    @DisplayName("Should allow 127.0.0.1:9992 (CMS on loopback)")
    void testAllowLoopbackCMSPort() throws MalformedURLException {
      URL loopbackUrl = new URL("http://127.0.0.1:9992/admin");
      URLValidation.validateURL(loopbackUrl);
    }

    @Test
    @DisplayName("Should allow 127.0.0.1:9980 (DTS HTTP port)")
    void testAllowLoopbackDTSHTTP() throws MalformedURLException {
      URL dtsUrl = new URL("http://127.0.0.1:9980");
      URLValidation.validateURL(dtsUrl);
    }

    @Test
    @DisplayName("Should allow 127.0.0.1:8443 (DTS HTTPS port)")
    void testAllowLoopbackDTSHTTPS() throws MalformedURLException {
      URL dtsSecureUrl = new URL("https://127.0.0.1:8443");
      URLValidation.validateURL(dtsSecureUrl);
    }

    @Test
    @DisplayName("Should allow IPv6 loopback ::1 on any port")
    void testAllowIPv6Loopback() throws MalformedURLException {
      URL ipv6Url = new URL("http://[::1]:8080");
      URLValidation.validateURL(ipv6Url);
    }
  }

  @Nested
  @DisplayName("Private IP Tests (Blocked by Default, Configurable)")
  class PrivateIPTests {
    @Test
    @DisplayName("Should reject private IP 10.0.0.100 by default")
    void testRejectPrivateIP10() throws MalformedURLException {
      URL privateUrl = new URL("http://10.0.0.5/internal");
      assertThrows(
          SecurityException.class,
          () -> URLValidation.validateURL(privateUrl),
          "Private IP 10.x.x.x should be blocked by default");
    }

    @Test
    @DisplayName("Should reject private IP range 10.0.0.0/8")
    void testRejectPrivateIpRange10() throws MalformedURLException {
      URL privateUrl = new URL("http://10.0.0.5/internal");
      assertThrows(
          SecurityException.class,
          () -> URLValidation.validateURL(privateUrl),
          "Should reject 10.x.x.x range");
    }

    @Test
    @DisplayName("Should reject private IP 172.16.0.100 by default")
    void testRejectPrivateIP172() throws MalformedURLException {
      URL privateUrl = new URL("http://172.20.0.1/service");
      assertThrows(
          SecurityException.class,
          () -> URLValidation.validateURL(privateUrl),
          "Private IP 172.16-31.x.x should be blocked by default");
    }

    @Test
    @DisplayName("Should reject private IP 192.168.1.100 by default")
    void testRejectPrivateIP192() throws MalformedURLException {
      URL privateUrl = new URL("http://192.168.1.1/router");
      assertThrows(
          SecurityException.class,
          () -> URLValidation.validateURL(privateUrl),
          "Private IP 192.168.x.x should be blocked by default");
    }

    @Test
    @DisplayName("Should allow configured private IP range (10.0.0.0/8)")
    void testAllowConfiguredPrivateIPRange() throws MalformedURLException {
      URLValidationConfig config = URLValidationConfig.builder().addIPRange("10.0.0.0/8").build();
      URL privateUrl = new URL("http://10.0.0.100:80");
      URLValidation.validateURL(privateUrl, config);
    }

    @Test
    @DisplayName("Should allow configured ports for private IPs")
    void testAllowConfiguredPortForPrivateIP() throws MalformedURLException {
      URLValidationConfig config =
          URLValidationConfig.builder().addIPRange("192.168.0.0/16").addPort(9992).build();
      URL cmsInternalUrl = new URL("http://192.168.1.100:9992");
      URLValidation.validateURL(cmsInternalUrl, config);
    }
  }

  @Nested
  @DisplayName("Cloud Metadata & Reserved Address Tests")
  class MetadataTests {
    @Test
    @DisplayName("Should reject AWS metadata service 169.254.169.254")
    void testRejectAwsMetadata() throws MalformedURLException {
      URL awsMetadataUrl = new URL("http://169.254.169.254/latest/meta-data/");
      assertThrows(
          SecurityException.class,
          () -> URLValidation.validateURL(awsMetadataUrl),
          "AWS metadata service should always be blocked");
    }

    @Test
    @DisplayName("Should reject GCP metadata service")
    void testRejectGcpMetadata() throws MalformedURLException {
      URL gcpMetadataUrl = new URL("http://metadata.google.internal/");
      assertThrows(
          SecurityException.class,
          () -> URLValidation.validateURL(gcpMetadataUrl),
          "GCP metadata service should always be blocked");
    }
  }

  @Nested
  @DisplayName("Protocol Tests")
  class ProtocolTests {
    @Test
    @DisplayName("Should reject file:// protocol")
    void testRejectFileProtocol() throws MalformedURLException {
      URL fileUrl = new URL("file:///etc/passwd");
      assertThrows(
          SecurityException.class,
          () -> URLValidation.validateURL(fileUrl),
          "file:// protocol should be blocked");
    }

    @Test
    @DisplayName("Should reject ftp:// protocol")
    void testRejectFtpProtocol() throws MalformedURLException {
      URL ftpUrl = new URL("ftp://ftp.example.com/file.txt");
      assertThrows(
          SecurityException.class,
          () -> URLValidation.validateURL(ftpUrl),
          "ftp:// protocol should be blocked");
    }
  }

  @Nested
  @DisplayName("Input Validation Tests")
  class InputValidationTests {
    @Test
    @DisplayName("Should reject null URL")
    void testRejectNullUrl() {
      assertThrows(
          IllegalArgumentException.class,
          () -> URLValidation.validateURL(null),
          "Null URL should be rejected");
    }

    @Test
    @DisplayName("Should reject null URL string")
    void testRejectNullUrlString() {
      assertThrows(
          IllegalArgumentException.class,
          () -> URLValidation.validateURLString(null),
          "Null URL string should be rejected");
    }

    @Test
    @DisplayName("Should reject empty URL string")
    void testRejectEmptyUrlString() {
      assertThrows(
          IllegalArgumentException.class,
          () -> URLValidation.validateURLString(""),
          "Empty URL string should be rejected");
    }
  }
}
