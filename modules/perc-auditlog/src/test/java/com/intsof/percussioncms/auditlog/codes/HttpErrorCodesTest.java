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
package com.intsof.percussioncms.auditlog.codes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intsof.percussioncms.auditlog.AuditModule;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HttpErrorCodesTest {

  @Test
  void everyConstantHasExplicitModuleSysAndUniqueNumericCode() {
    Set<Integer> seen = new HashSet<>();
    for (HttpErrorCodes code : HttpErrorCodes.values()) {
      assertEquals(AuditModule.SYS, code.module());
      assertTrue(code.numericCode() > 0, code.name());
      assertTrue(seen.add(code.numericCode()), "duplicate numeric: " + code.numericCode());
      assertNotNull(code.userMessageTemplate());
      assertNotNull(code.logMessageTemplate());
      assertTrue(code.qualifiedCode().startsWith("SYS-"));
    }
    assertEquals(37, HttpErrorCodes.values().length);
  }

  @Test
  void allHttpStatusCodesAreNonAuditable() {
    for (HttpErrorCodes code : HttpErrorCodes.values()) {
      assertFalse(code.isAuditable(), code.name());
      assertNull(code.eventType(), code.name());
    }
  }

  @Test
  void preservesLegacyIpsHttpErrorsNumericValues() {
    assertEquals(100, HttpErrorCodes.HTTP_CONTINUE.numericCode());
    assertEquals(200, HttpErrorCodes.HTTP_OK.numericCode());
    assertEquals(301, HttpErrorCodes.HTTP_MOVED_PERMANENTLY.numericCode());
    assertEquals(302, HttpErrorCodes.HTTP_MOVED_TEMPORARILY.numericCode());
    assertEquals(401, HttpErrorCodes.HTTP_UNAUTHORIZED.numericCode());
    assertEquals(403, HttpErrorCodes.HTTP_FORBIDDEN.numericCode());
    assertEquals(404, HttpErrorCodes.HTTP_NOT_FOUND.numericCode());
    assertEquals(500, HttpErrorCodes.HTTP_INTERNAL_SERVER_ERROR.numericCode());
    assertEquals(505, HttpErrorCodes.HTTP_VERSION_NOT_SUPPORTED.numericCode());
  }
}
