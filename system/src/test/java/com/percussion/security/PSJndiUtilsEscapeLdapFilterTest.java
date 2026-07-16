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
package com.percussion.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for {@link PSJndiUtils#escapeLdapFilter} (CodeQL
 * {@code java/ldap-injection}, T040, US3).
 *
 * <p><strong>Background.</strong> The pre-fix {@code PSJndiGroupProvider} concatenated DN
 * components into LDAP search filter strings without escaping the RFC 4515 metacharacters
 * {@code \}, {@code *}, {@code (}, {@code )} and the NUL byte. A malicious DN that
 * contained a parenthesised assertion (e.g. {@code *)(objectClass=*}) could break out
 * of the surrounding filter and inject arbitrary clauses.
 *
 * <p><strong>Fail-then-pass coverage (Constitution III).</strong> The escape is a pure
 * string function that takes a raw input and returns the escaped output. The pre-fix
 * code had no such helper, so a test that asserts the output contains the hex-escape
 * form of the metacharacters would fail on pre-fix code (no escape at all) and pass
 * on post-fix code. This is the strict fail-then-pass test.
 */
@DisplayName("PSJndiUtils.escapeLdapFilter — LDAP injection (CWE-90) regression tests")
class PSJndiUtilsEscapeLdapFilterTest {

  @Nested
  @DisplayName("Metacharacter escaping per RFC 4515")
  class Metacharacters {

    @Test
    @DisplayName("backslash is hex-escaped to \\5c")
    void testBackslashIsEscaped() {
      assertEquals("\\5c", PSJndiUtils.escapeLdapFilter("\\"));
    }

    @Test
    @DisplayName("asterisk is hex-escaped to \\2a")
    void testAsteriskIsEscaped() {
      assertEquals("\\2a", PSJndiUtils.escapeLdapFilter("*"));
    }

    @Test
    @DisplayName("opening parenthesis is hex-escaped to \\28")
    void testOpenParenIsEscaped() {
      assertEquals("\\28", PSJndiUtils.escapeLdapFilter("("));
    }

    @Test
    @DisplayName("closing parenthesis is hex-escaped to \\29")
    void testCloseParenIsEscaped() {
      assertEquals("\\29", PSJndiUtils.escapeLdapFilter(")"));
    }

    @Test
    @DisplayName("NUL byte is hex-escaped to \\00")
    void testNulIsEscaped() {
      assertEquals("\\00", PSJndiUtils.escapeLdapFilter("\0"));
    }
  }

  @Nested
  @DisplayName("Safe characters are not escaped")
  class SafeCharacters {

    @Test
    @DisplayName("alphanumerics pass through unchanged")
    void testAlphanumericsPassThrough() {
      assertEquals("admin", PSJndiUtils.escapeLdapFilter("admin"));
      assertEquals("OU=Users", PSJndiUtils.escapeLdapFilter("OU=Users"));
      assertEquals("user123", PSJndiUtils.escapeLdapFilter("user123"));
    }

    @Test
    @DisplayName("hyphens and dots pass through unchanged")
    void testHyphensAndDotsPassThrough() {
      assertEquals("first.last", PSJndiUtils.escapeLdapFilter("first.last"));
      assertEquals("user-name", PSJndiUtils.escapeLdapFilter("user-name"));
    }
  }

  @Nested
  @DisplayName("Mixed payloads")
  class Mixed {

    @Test
    @DisplayName("typical DN component is escaped correctly")
    void testTypicalDnComponent() {
      // Per RFC 4515 §3, the comma (`,`) is a DN SEPARATOR, not a
      // filter metacharacter, and is NOT escaped by escapeLdapFilter.
      // escapeLdapFilter is for interpolating a value into an LDAP
      // search filter; DN separator escaping is a separate concern
      // (handled by escapeDnComponent). The four RFC 4515 filter
      // metacharacters that escapeLdapFilter hex-escapes are: `\`,
      // `*`, `(`, `)`. This test exercises a DN value containing no
      // metacharacters — it should pass through unchanged.
      assertEquals("OU=Users,DC=example,DC=com",
          PSJndiUtils.escapeLdapFilter("OU=Users,DC=example,DC=com"));
    }

    @Test
    @DisplayName("classic LDAP injection payload is fully neutralized")
    void testClassicInjectionPayload() {
      // The canonical LDAP injection payload that would break out of
      // (cn=<value>) by closing it and adding (objectClass=*). With the
      // pre-fix code (no escape), the metacharacters would pass through
      // verbatim and the JNDI search would interpret the injected clause.
      // The post-fix escape converts every RFC 4515 metacharacter to its
      // hex-escape form, so the payload becomes a literal string and can
      // no longer break out of its surrounding parentheses.
      String malicious = "*)(objectClass=*";
      String escaped = PSJndiUtils.escapeLdapFilter(malicious);
      assertNotEquals(malicious, escaped, "the payload must be escaped, not returned verbatim");
      assertTrue(escaped.contains("\\2a"), "asterisk must be hex-escaped");
      assertTrue(escaped.contains("\\28"), "open paren must be hex-escaped");
      assertTrue(escaped.contains("\\29"), "close paren must be hex-escaped");
      // The escaped form is a single literal value, not a clause break.
      // `=` is the attribute-value separator and is NOT a metacharacter
      // per RFC 4515, so it passes through unchanged.
      assertEquals("\\2a\\29\\28objectClass=\\2a", escaped);
    }

    @Test
    @DisplayName("null and empty inputs are handled")
    void testNullAndEmpty() {
      assertEquals("", PSJndiUtils.escapeLdapFilter(null));
      assertEquals("", PSJndiUtils.escapeLdapFilter(""));
    }
  }
}
