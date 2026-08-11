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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intsof.percussioncms.auditlog.AuditEventType;
import com.intsof.percussioncms.auditlog.AuditModule;
import com.intsof.percussioncms.auditlog.AuditOutcome;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CloneErrorCodesTest {

  @Test
  void everyConstantHasExplicitModuleSysAndUniqueNumericCode() {
    Set<Integer> seen = new HashSet<>();
    for (CloneErrorCodes code : CloneErrorCodes.values()) {
      assertEquals(AuditModule.SYS, code.module());
      assertTrue(code.numericCode() >= 17501, code.name());
      assertTrue(seen.add(code.numericCode()), "duplicate numeric: " + code.numericCode());
      assertNotNull(code.userMessageTemplate());
      assertNotNull(code.logMessageTemplate());
      assertTrue(code.qualifiedCode().startsWith("SYS-"));
    }
    assertEquals(6, CloneErrorCodes.values().length);
  }

  @Test
  void onlyAuthzCodesAreAuditable() {
    for (CloneErrorCodes code : CloneErrorCodes.values()) {
      if (code == CloneErrorCodes.NOT_AUTHENTICACATED) {
        assertTrue(code.isAuditable());
        assertSame(AuditEventType.AUTH_FAILURE, code.eventType());
        assertSame(AuditOutcome.FAILURE, code.defaultOutcome());
      } else if (code == CloneErrorCodes.NOT_AUTHORIZED) {
        assertTrue(code.isAuditable());
        assertSame(AuditEventType.ACCESS_DENIED, code.eventType());
        assertSame(AuditOutcome.FAILURE, code.defaultOutcome());
      } else {
        assertFalse(code.isAuditable(), code.name());
        assertNull(code.eventType(), code.name());
      }
    }
  }

  @Test
  void preservesLegacyIpsCloneErrorsNumericValues() {
    assertEquals(17501, CloneErrorCodes.INVALID_CLONESOURCEID.numericCode());
    assertEquals(17502, CloneErrorCodes.NOT_AUTHENTICACATED.numericCode());
    assertEquals(17503, CloneErrorCodes.NOT_AUTHORIZED.numericCode());
    assertEquals(17506, CloneErrorCodes.ROLE_CREATION_ERROR.numericCode());
  }
}
