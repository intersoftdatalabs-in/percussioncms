/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
package com.percussion.delivery.feeds.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * SSRF regression tests for {@link PSFeedService#buildMetadataServiceUri} (CodeQL {@code
 * java/ssrf}, alert #797, T037 residual).
 *
 * <p>The metadata-service target must be built only from server-side values with a fixed path and a
 * scheme constrained to http/https literals.
 */
@DisplayName("PSFeedService.buildMetadataServiceUri — SSRF (CWE-918) regression tests")
class PSFeedServiceMetadataUriTest {

  @Nested
  @DisplayName("Scheme is constrained to safe literals")
  class Scheme {

    @Test
    @DisplayName("https request scheme yields https URI")
    void testHttpsScheme() throws Exception {
      URI uri = PSFeedService.buildMetadataServiceUri("https", "127.0.0.1", 9980);
      assertEquals("https", uri.getScheme());
      assertEquals("127.0.0.1", uri.getHost());
      assertEquals(9980, uri.getPort());
      assertEquals(PSFeedService.METADATA_SERVICE_PATH, uri.getPath());
    }

    @Test
    @DisplayName("http request scheme yields http URI")
    void testHttpScheme() throws Exception {
      URI uri = PSFeedService.buildMetadataServiceUri("http", "10.0.0.5", 8080);
      assertEquals("http", uri.getScheme());
    }

    @Test
    @DisplayName("unexpected scheme falls back to http")
    void testUnexpectedSchemeFallsBackToHttp() throws Exception {
      URI uri = PSFeedService.buildMetadataServiceUri("ftp", "127.0.0.1", 9980);
      assertEquals("http", uri.getScheme());
      assertNotEquals("ftp", uri.getScheme());
    }

    @Test
    @DisplayName("null scheme falls back to http")
    void testNullSchemeFallsBackToHttp() throws Exception {
      URI uri = PSFeedService.buildMetadataServiceUri(null, "127.0.0.1", 9980);
      assertEquals("http", uri.getScheme());
    }
  }

  @Nested
  @DisplayName("Path is fixed")
  class Path {

    @Test
    @DisplayName("path is always the metadata service constant")
    void testFixedPath() throws Exception {
      URI uri = PSFeedService.buildMetadataServiceUri("https", "192.0.2.1", 443);
      assertEquals(PSFeedService.METADATA_SERVICE_PATH, uri.getPath());
      assertEquals(null, uri.getQuery());
      assertEquals(null, uri.getFragment());
    }

    @Test
    @DisplayName("IPv6 host is bracketed in the URI string")
    void testIpv6HostIsBracketed() throws Exception {
      URI uri = PSFeedService.buildMetadataServiceUri("http", "2001:db8::1", 9980);
      // URI multi-arg constructor brackets IPv6; getHost() may include brackets
      // depending on JDK, so assert the authority form used for the request.
      assertTrue(
          uri.toString().contains("[2001:db8::1]"),
          "IPv6 host must be bracketed in the URI string: " + uri);
      assertEquals(9980, uri.getPort());
      assertEquals(PSFeedService.METADATA_SERVICE_PATH, uri.getPath());
    }
  }
}
