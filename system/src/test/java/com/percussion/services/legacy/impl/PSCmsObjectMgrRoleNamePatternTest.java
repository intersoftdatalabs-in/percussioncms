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
package com.percussion.services.legacy.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * {@code PSCmsObjectMgr.toRoleNamePattern} must translate SQL-LIKE {@code %} wildcards while
 * quoting every literal segment, so user-supplied regex metacharacters cannot change the meaning of
 * the expression (CodeQL {@code java/regex-injection}, alert #2091).
 */
class PSCmsObjectMgrRoleNamePatternTest {

  private static boolean matches(String filter, String roleName) {
    Pattern pattern = PSCmsObjectMgr.toRoleNamePattern(filter);
    return pattern.matcher(roleName).matches();
  }

  @Test
  void percentMatchesAnyRun() {
    assertTrue(matches("%", "Admin"));
    assertTrue(matches("Ad%", "Admin"));
    assertTrue(matches("%min", "Admin"));
    assertTrue(matches("A%n", "Admin"));
  }

  @Test
  void literalTextMatchesWholeNameCaseInsensitively() {
    assertTrue(matches("admin", "Admin"));
    assertTrue(matches("ADMIN", "admin"));
    assertFalse(matches("Adm", "Admin"));
    assertFalse(matches("AdminX", "Admin"));
  }

  @Test
  void regexMetacharactersAreQuoted() {
    // A filter of ".*" must match only a role literally named ".*", not everything.
    assertFalse(matches(".*", "Admin"));
    assertTrue(matches(".*", ".*"));
    // Parentheses, brackets, plus and backslash are literal too.
    assertFalse(matches("Ad(min)", "Admin"));
    assertTrue(matches("Ad(min)", "Ad(min)"));
    assertFalse(matches("Admin+", "Adminnnn"));
    assertTrue(matches("a.c", "a.c"));
    assertFalse(matches("a.c", "abc"));
  }

  @Test
  void catastrophicBacktrackingPayloadIsHarmless() {
    // Classic ReDoS probe: must not throw and must not match.
    assertFalse(matches("(a+)+$", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa!"));
    assertFalse(matches("^(a|a)*$", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa!"));
  }
}
