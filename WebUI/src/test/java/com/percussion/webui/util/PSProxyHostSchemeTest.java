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
package com.percussion.webui.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Behavioral tests for proxy public-scheme resolution used by dashboard JSPs (issue #1160).
 *
 * <p>Primary regression: when {@code proxyScheme} is unset, inherit {@code request.getScheme()}
 * instead of hardcoding {@code "http"} (mixed content on HTTPS).
 */
public class PSProxyHostSchemeTest {

  @Test
  public void inheritsHttpsRequestSchemeWhenProxySchemeUnset() {
    assertEquals("https", PSProxyHostScheme.resolveBehindProxy("https", null));
    assertEquals("https", PSProxyHostScheme.resolveBehindProxy("https", ""));
    assertEquals("https", PSProxyHostScheme.resolveBehindProxy("https", "   "));
  }

  @Test
  public void inheritsHttpRequestSchemeWhenProxySchemeUnset() {
    assertEquals("http", PSProxyHostScheme.resolveBehindProxy("http", null));
  }

  @Test
  public void prefersConfiguredProxySchemeWhenSet() {
    assertEquals("https", PSProxyHostScheme.resolveBehindProxy("http", "https"));
    assertEquals("http", PSProxyHostScheme.resolveBehindProxy("https", "http"));
  }

  @Test
  public void normalizesConfiguredSchemeCaseAndWhitespace() {
    assertEquals("https", PSProxyHostScheme.resolveBehindProxy("http", " HTTPS "));
    assertEquals("http", PSProxyHostScheme.resolveBehindProxy("https", "HTTP"));
  }

  @Test
  public void fallsBackToHttpOnlyWhenBothMissing() {
    assertEquals("http", PSProxyHostScheme.resolveBehindProxy(null, null));
    assertEquals("http", PSProxyHostScheme.resolveBehindProxy("", null));
  }

  @Test
  public void normalizesRequestSchemeWhenConfiguredMissing() {
    assertEquals("https", PSProxyHostScheme.resolveBehindProxy(" HTTPS ", null));
  }

  @Test
  public void normalizeSchemeTrimsAndLowerCases() {
    assertEquals("https", PSProxyHostScheme.normalizeScheme(" HTTPS "));
    assertNull(PSProxyHostScheme.normalizeScheme(null));
    assertNull(PSProxyHostScheme.normalizeScheme(""));
    assertNull(PSProxyHostScheme.normalizeScheme("  "));
  }
}
