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

import com.intsof.percussioncms.auditlog.AuditEventType;
import com.intsof.percussioncms.auditlog.AuditModule;
import com.intsof.percussioncms.auditlog.AuditOutcome;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SearchErrorCodesTest {

  @Test
  void everyConstantHasExplicitModuleSysAndUniqueNumericCode() {
    Set<Integer> seen = new HashSet<>();
    for (SearchErrorCodes code : SearchErrorCodes.values()) {
      assertEquals(AuditModule.SYS, code.module());
      assertTrue(code.numericCode() >= 16001, code.name());
      assertTrue(seen.add(code.numericCode()), "duplicate numeric: " + code.numericCode());
      assertNotNull(code.userMessageTemplate());
      assertNotNull(code.logMessageTemplate());
      assertTrue(code.qualifiedCode().startsWith("SYS-"));
    }
    assertEquals(16, SearchErrorCodes.values().length);
  }

  @Test
  void onlyAuthenticationFailedIsAuditable() {
    for (SearchErrorCodes code : SearchErrorCodes.values()) {
      if (code == SearchErrorCodes.SEARCH_ENGINE_AUTHENTICATION_FAILED) {
        assertTrue(code.isAuditable(), code.name());
        assertEquals(AuditEventType.AUTH_FAILURE, code.eventType());
        assertEquals(AuditOutcome.FAILURE, code.defaultOutcome());
      } else {
        assertFalse(code.isAuditable(), code.name());
        assertNull(code.eventType(), code.name());
      }
    }
  }

  @Test
  void preservesLegacyIpsSearchErrorsNumericValues() {
    assertEquals(16001, SearchErrorCodes.SEARCH_ENGINE_UNIMPLEMENTED_OPERATION.numericCode());
    assertEquals(16012, SearchErrorCodes.INVALID_INDEX_CONTENTTYPE.numericCode());
    assertEquals(16051, SearchErrorCodes.SEARCH_ENGINE_FAILED_INIT.numericCode());
    assertEquals(16052, SearchErrorCodes.SEARCH_ENGINE_AUTHENTICATION_FAILED.numericCode());
    assertEquals(16054, SearchErrorCodes.USE_GET_INSTANCE.numericCode());
  }
}
