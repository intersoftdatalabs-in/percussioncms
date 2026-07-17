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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for {@link PSJndiUtils#getFilterString} LDAP injection hardening (CodeQL {@code
 * java/ldap-injection}, alert #648 / T040 residual).
 *
 * <p>User-supplied filter values must be RFC 4515-escaped before interpolation so a payload such as
 * {@code *)(objectClass=*} cannot break out of the surrounding filter. Intentional application
 * wildcards use {@code %} and must still become LDAP {@code *}.
 */
@DisplayName("PSJndiUtils.getFilterString — LDAP injection (CWE-90) regression tests")
class PSJndiUtilsGetFilterStringTest {

  private static final String ATTR = "cn";

  @Nested
  @DisplayName("Injection payloads are neutralized")
  class Injection {

    @Test
    @DisplayName("classic break-out payload is hex-escaped inside the filter")
    void testClassicInjectionPayloadIsEscaped() throws PSSecurityException {
      String malicious = "*)(objectClass=*";
      String filter = PSJndiUtils.getFilterString(new String[] {malicious}, ATTR, null);

      // Must not contain a raw clause break: unescaped ")(" would allow injection.
      assertFalse(
          filter.contains(")("),
          "filter must not contain raw )( clause break; got: " + filter);
      assertTrue(filter.contains("\\2a"), "asterisk must be hex-escaped: " + filter);
      assertTrue(filter.contains("\\28"), "open paren must be hex-escaped: " + filter);
      assertTrue(filter.contains("\\29"), "close paren must be hex-escaped: " + filter);

      // Expected shape: (| (cn=<escaped>)) — space after OR operator is historical.
      assertEquals("(| (cn=\\2a\\29\\28objectClass=\\2a))", filter);
    }
  }

  @Nested
  @DisplayName("Intentional wildcards and safe values")
  class WildcardsAndSafe {

    @Test
    @DisplayName("application wildcard % becomes LDAP * after escape")
    void testPercentWildcardBecomesStar() throws PSSecurityException {
      String filter = PSJndiUtils.getFilterString(new String[] {"admin%"}, ATTR, null);
      assertEquals("(| (cn=admin*))", filter);
    }

    @Test
    @DisplayName("safe alphanumeric value passes through")
    void testSafeValue() throws PSSecurityException {
      String filter = PSJndiUtils.getFilterString(new String[] {"Editors"}, ATTR, null);
      assertEquals("(| (cn=Editors))", filter);
    }

    @Test
    @DisplayName("base filter is AND-combined without re-escaping config text")
    void testBaseFilterCombined() throws PSSecurityException {
      String filter =
          PSJndiUtils.getFilterString(
              new String[] {"Editors"}, ATTR, "(objectClass=groupOfNames)");
      assertEquals("(& (| (cn=Editors)) (objectClass=groupOfNames))", filter);
    }
  }

  @Nested
  @DisplayName("andLdapFilters")
  class AndFilters {

    @Test
    @DisplayName("combines two filters with AND")
    void testAndBoth() {
      assertEquals(
          "(& (objectClass=group) (| (cn=Editors)))",
          PSJndiUtils.andLdapFilters("(objectClass=group)", "(| (cn=Editors))"));
    }

    @Test
    @DisplayName("omits blank left operand")
    void testAndLeftBlank() {
      assertEquals("(| (cn=x))", PSJndiUtils.andLdapFilters(null, "(| (cn=x))"));
      assertEquals("(| (cn=x))", PSJndiUtils.andLdapFilters("  ", "(| (cn=x))"));
    }

    @Test
    @DisplayName("omits blank right operand")
    void testAndRightBlank() {
      assertEquals("(objectClass=group)", PSJndiUtils.andLdapFilters("(objectClass=group)", null));
    }
  }
}
