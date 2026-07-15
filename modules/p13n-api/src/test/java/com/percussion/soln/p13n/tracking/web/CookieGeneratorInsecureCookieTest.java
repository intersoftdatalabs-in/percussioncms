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

package com.percussion.soln.p13n.tracking.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for CookieGenerator that verify the {@link jakarta.servlet.http.Cookie} created
 * for an HTTP response carries both the {@code Secure} and {@code HttpOnly} flags by default
 * (CodeQL {@code java/insecure-cookie}; CWE-614 / CWE-1004).
 *
 * <p>Follows the Constitution III fail-then-pass contract: on pre-fix code the cookie returned
 * by {@link CookieGenerator#createCookie(String)} is missing the {@code Secure} flag (the default
 * was {@code false}) and there is no notion of {@code HttpOnly}. On post-fix code the defaults
 * are both {@code true} so the cookie carries both flags; explicit {@code false} on either setter
 * is honored.
 */
@DisplayName("CookieGenerator Secure/HttpOnly Flag Tests")
class CookieGeneratorInsecureCookieTest {

  /**
   * Test-only subclass that exposes the protected {@link CookieGenerator#createCookie(String)}
   * method so the produced {@link Cookie} can be inspected directly.
   */
  private static final class TestableCookieGenerator extends CookieGenerator {
    Cookie createCookieExposed(String value) {
      return createCookie(value);
    }
  }

  @Test
  @DisplayName("createCookie returns a Secure cookie by default (CWE-614)")
  void testSecureFlagIsOnByDefault() {
    var generator = new TestableCookieGenerator();
    generator.setCookieName("p13n-test");
    generator.setCookiePath("/");

    var cookie = generator.createCookieExposed("value");

    assertTrue(cookie.getSecure(), "Cookie must be Secure by default (CWE-614)");
  }

  @Test
  @DisplayName("createCookie returns an HttpOnly cookie by default (CWE-1004)")
  void testHttpOnlyFlagIsOnByDefault() {
    var generator = new TestableCookieGenerator();
    generator.setCookieName("p13n-test");
    generator.setCookiePath("/");

    var cookie = generator.createCookieExposed("value");

    assertTrue(cookie.isHttpOnly(), "Cookie must be HttpOnly by default (CWE-1004)");
  }

  @Test
  @DisplayName("setCookieSecure(false) is honored: secure flag is dropped when explicitly disabled")
  void testSecureFlagCanBeDisabledExplicitly() {
    var generator = new TestableCookieGenerator();
    generator.setCookieName("p13n-test");
    generator.setCookiePath("/");
    generator.setCookieSecure(false);

    var cookie = generator.createCookieExposed("value");

    assertFalse(cookie.getSecure(), "Explicit setCookieSecure(false) must be honored");
  }

  @Test
  @DisplayName("setCookieHttpOnly(false) is honored: HttpOnly flag is dropped when explicitly disabled")
  void testHttpOnlyFlagCanBeDisabledExplicitly() {
    var generator = new TestableCookieGenerator();
    generator.setCookieName("p13n-test");
    generator.setCookiePath("/");
    generator.setCookieHttpOnly(false);

    var cookie = generator.createCookieExposed("value");

    assertFalse(cookie.isHttpOnly(), "Explicit setCookieHttpOnly(false) must be honored");
  }

  @Test
  @DisplayName("isCookieSecure() and isCookieHttpOnly() report the current flags")
  void testIsMethodsReportDefaults() {
    var generator = new TestableCookieGenerator();
    assertTrue(generator.isCookieSecure(), "Default cookieSecure must be true");
    assertTrue(generator.isCookieHttpOnly(), "Default cookieHttpOnly must be true");
  }

  @Test
  @DisplayName("setCookieSecure(true) is idempotent (default remains true)")
  void testSettingSecureToTrueIsIdempotent() {
    var generator = new TestableCookieGenerator();
    generator.setCookieSecure(true);
    assertTrue(generator.isCookieSecure());
    assertTrue(generator.createCookieExposed("v").getSecure());
  }

  @Test
  @DisplayName("setCookieHttpOnly(true) is idempotent (default remains true)")
  void testSettingHttpOnlyToTrueIsIdempotent() {
    var generator = new TestableCookieGenerator();
    generator.setCookieHttpOnly(true);
    assertTrue(generator.isCookieHttpOnly());
    assertTrue(generator.createCookieExposed("v").isHttpOnly());
  }
}