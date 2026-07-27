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
package com.percussion.servlets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Log-injection hygiene for {@link PSSecurityFilter#sanitizeForLog(String)} (PR #1344 review). */
@DisplayName("PSSecurityFilter sanitizeForLog (T051)")
class PSSecurityFilterSanitizeForLogTest {

  @Test
  void stripsCrLfAndOtherControlChars() {
    assertEquals("okInjected: value", PSSecurityFilter.sanitizeForLog("ok\r\nInjected: value"));
    assertFalse(PSSecurityFilter.sanitizeForLog("a\rb\nc").contains("\r"));
    assertFalse(PSSecurityFilter.sanitizeForLog("a\rb\nc").contains("\n"));
    assertEquals("ab", PSSecurityFilter.sanitizeForLog("a\u0007b"));
    assertEquals("ab", PSSecurityFilter.sanitizeForLog("a\tb"));
    assertEquals("", PSSecurityFilter.sanitizeForLog(null));
  }
}
