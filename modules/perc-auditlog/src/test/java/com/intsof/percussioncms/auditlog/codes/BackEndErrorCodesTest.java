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

class BackEndErrorCodesTest {

  @Test
  void everyConstantHasExplicitModuleSysAndUniqueNumericCode() {
    Set<Integer> seen = new HashSet<>();
    for (BackEndErrorCodes code : BackEndErrorCodes.values()) {
      assertEquals(AuditModule.SYS, code.module());
      assertTrue(code.numericCode() >= 5001, code.name());
      assertTrue(seen.add(code.numericCode()), "duplicate numeric: " + code.numericCode());
      assertNotNull(code.userMessageTemplate());
      assertNotNull(code.logMessageTemplate());
      assertTrue(code.qualifiedCode().startsWith("SYS-"));
    }
    assertEquals(60, BackEndErrorCodes.values().length);
  }

  @Test
  void onlyAuthorizationErrorIsAuditable() {
    for (BackEndErrorCodes code : BackEndErrorCodes.values()) {
      if (code == BackEndErrorCodes.AUTHORIZATION_ERROR) {
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
  void preservesLegacyIpsBackEndErrorsNumericValues() {
    assertEquals(5001, BackEndErrorCodes.AUTHORIZATION_ERROR.numericCode());
    assertEquals(5008, BackEndErrorCodes.JDBC_DRIVER_LOAD_FAILED.numericCode());
    assertEquals(5401, BackEndErrorCodes.LOG_PREPARED_STMT.numericCode());
    assertEquals(5999, BackEndErrorCodes.NOT_YET_SUPPORTED.numericCode());
  }
}
