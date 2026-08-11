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

class AssemblyErrorCodesTest {

  @Test
  void everyConstantHasExplicitModulePubAndUniqueNumericCode() {
    Set<Integer> seen = new HashSet<>();
    for (AssemblyErrorCodes code : AssemblyErrorCodes.values()) {
      assertEquals(AuditModule.PUB, code.module());
      assertTrue(code.numericCode() > 0, code.name());
      assertTrue(seen.add(code.numericCode()), "duplicate numeric: " + code.numericCode());
      assertNotNull(code.userMessageTemplate());
      assertNotNull(code.logMessageTemplate());
      assertTrue(code.qualifiedCode().startsWith("PUB-"));
    }
    assertEquals(27, AssemblyErrorCodes.values().length);
  }

  @Test
  void allAssemblyCodesAreNonAuditable() {
    for (AssemblyErrorCodes code : AssemblyErrorCodes.values()) {
      assertFalse(code.isAuditable(), code.name());
      assertNull(code.eventType(), code.name());
    }
  }

  @Test
  void preservesLegacyIpsAssemblyErrorsNumericValues() {
    assertEquals(1, AssemblyErrorCodes.TEMPLATE_MISSING.numericCode());
    assertEquals(5, AssemblyErrorCodes.UNKNOWN_ERROR.numericCode());
    assertEquals(12, AssemblyErrorCodes.MISSING_FINDER.numericCode());
    assertEquals(27, AssemblyErrorCodes.INLINE_LINK_ERROR.numericCode());
  }
}
