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
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WebserviceErrorCodesTest {

  @Test
  void everyConstantHasExplicitModuleSysAndUniqueNumericCode() {
    Set<Integer> seen = new HashSet<>();
    for (WebserviceErrorCodes code : WebserviceErrorCodes.values()) {
      assertEquals(AuditModule.SYS, code.module());
      assertTrue(code.numericCode() > 0, code.name());
      assertTrue(seen.add(code.numericCode()), "duplicate numeric: " + code.numericCode());
      assertNotNull(code.userMessageTemplate());
      assertNotNull(code.logMessageTemplate());
      assertTrue(code.qualifiedCode().startsWith("SYS-"));
    }
    assertEquals(73, WebserviceErrorCodes.values().length);
  }

  @Test
  void auditableCodesRequireEventType() {
    for (WebserviceErrorCodes code : WebserviceErrorCodes.values()) {
      if (code.isAuditable()) {
        assertNotNull(code.eventType(), code.name() + " auditable without eventType");
      } else {
        assertNull(code.eventType(), code.name() + " non-auditable should not force eventType");
      }
    }
  }

  @Test
  void sessionAndAccessFailuresAreAuditable() {
    assertTrue(WebserviceErrorCodes.INVALID_SESSION.isAuditable());
    assertEquals(AuditEventType.AUTH_FAILURE, WebserviceErrorCodes.INVALID_SESSION.eventType());
    assertTrue(WebserviceErrorCodes.MISSING_SESSION.isAuditable());
    assertTrue(WebserviceErrorCodes.ACCESS_CONTROL_ERROR.isAuditable());
    assertEquals(
        AuditEventType.ACCESS_DENIED, WebserviceErrorCodes.ACCESS_CONTROL_ERROR.eventType());
    assertTrue(WebserviceErrorCodes.NOT_AUTHORIZED.isAuditable());
    assertTrue(WebserviceErrorCodes.ITEM_NOT_CHECKED_OUT.isAuditable());
    assertTrue(WebserviceErrorCodes.USER_NOT_MEMBER_COMMUNITY.isAuditable());
  }

  @Test
  void operationalDesignCrudIsNotAuditable() {
    assertFalse(WebserviceErrorCodes.INVALID_CONTRACT.isAuditable());
    assertFalse(WebserviceErrorCodes.OBJECT_NOT_FOUND.isAuditable());
    assertFalse(WebserviceErrorCodes.SAVE_FAILED.isAuditable());
    assertFalse(WebserviceErrorCodes.DELETE_FAILED.isAuditable());
    assertFalse(WebserviceErrorCodes.UNEXPECTED_ERROR.isAuditable());
  }

  @Test
  void preservesLegacyIpsWebserviceErrorsNumericValues() {
    assertEquals(1, WebserviceErrorCodes.INVALID_CONTRACT.numericCode());
    assertEquals(3, WebserviceErrorCodes.INVALID_SESSION.numericCode());
    assertEquals(32, WebserviceErrorCodes.ACCESS_CONTROL_ERROR.numericCode());
    assertEquals(72, WebserviceErrorCodes.NOT_AUTHORIZED.numericCode());
    assertEquals(73, WebserviceErrorCodes.FAILED_TO_OBTAIN_PATH_FROM_OBJECT_ID.numericCode());
  }
}
