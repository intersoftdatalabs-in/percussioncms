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

class ConnectionErrorCodesTest {

  @Test
  void everyConstantHasExplicitModuleSysAndUniqueNumericCode() {
    Set<Integer> seen = new HashSet<>();
    for (ConnectionErrorCodes code : ConnectionErrorCodes.values()) {
      assertEquals(AuditModule.SYS, code.module());
      assertTrue(code.numericCode() >= 3001, code.name());
      assertTrue(seen.add(code.numericCode()), "duplicate numeric: " + code.numericCode());
      assertNotNull(code.userMessageTemplate());
      assertNotNull(code.logMessageTemplate());
      assertTrue(code.qualifiedCode().startsWith("SYS-"));
    }
    assertEquals(19, ConnectionErrorCodes.values().length);
  }

  @Test
  void onlyUnauthorizedIsAuditable() {
    for (ConnectionErrorCodes code : ConnectionErrorCodes.values()) {
      if (code == ConnectionErrorCodes.UNAUTHORIZED) {
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
  void preservesLegacyIpsConnectionErrorsNumericValues() {
    assertEquals(3001, ConnectionErrorCodes.PORT_NUMBER_INVALID.numericCode());
    assertEquals(3005, ConnectionErrorCodes.QUEUE_LIMIT_INVALID.numericCode());
    assertEquals(3101, ConnectionErrorCodes.UNKNOWN_SERVER_EXCEPTION.numericCode());
    assertEquals(3107, ConnectionErrorCodes.UNAUTHORIZED.numericCode());
  }

  @Test
  void documentsNumericOverlapWithUserManagementPackageLocal() {
    // Phase-2a USER lifecycle codes reuse 3001–3005 package-locally but are not flat-registered.
    assertEquals(3001, UserManagementErrorCodes.CREATE.numericCode());
    assertEquals(3001, ConnectionErrorCodes.PORT_NUMBER_INVALID.numericCode());
    assertEquals(3005, UserManagementErrorCodes.REVOKE.numericCode());
    assertEquals(3005, ConnectionErrorCodes.QUEUE_LIMIT_INVALID.numericCode());
  }
}
