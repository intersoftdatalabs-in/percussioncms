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

class ServerWebServicesErrorCodesTest {

  @Test
  void everyConstantHasExplicitModuleSysAndUniqueNumericCode() {
    Set<Integer> seen = new HashSet<>();
    for (ServerWebServicesErrorCodes code : ServerWebServicesErrorCodes.values()) {
      assertEquals(AuditModule.SYS, code.module());
      assertTrue(code.numericCode() >= 14001, code.name());
      assertTrue(seen.add(code.numericCode()), "duplicate numeric: " + code.numericCode());
      assertNotNull(code.userMessageTemplate());
      assertNotNull(code.logMessageTemplate());
      assertTrue(code.qualifiedCode().startsWith("SYS-"));
    }
    assertEquals(26, ServerWebServicesErrorCodes.values().length);
  }

  @Test
  void auditableCodesRequireEventType() {
    for (ServerWebServicesErrorCodes code : ServerWebServicesErrorCodes.values()) {
      if (code.isAuditable()) {
        assertNotNull(code.eventType(), code.name() + " auditable without eventType");
      } else {
        assertNull(code.eventType(), code.name() + " non-auditable should not force eventType");
      }
    }
  }

  @Test
  void loginAndAccessFailuresAreAuditable() {
    assertTrue(ServerWebServicesErrorCodes.WEB_SERVICE_LOGIN_FAILURE.isAuditable());
    assertEquals(
        AuditEventType.AUTH_FAILURE,
        ServerWebServicesErrorCodes.WEB_SERVICE_LOGIN_FAILURE.eventType());
    assertTrue(ServerWebServicesErrorCodes.WEB_SERVICE_CHECKOUT_USER_FAILURE.isAuditable());
    assertTrue(ServerWebServicesErrorCodes.WEB_SERVICE_INVALID_CLIENT_ACESS.isAuditable());
  }

  @Test
  void operationalContentSearchIsNotAuditable() {
    assertFalse(ServerWebServicesErrorCodes.WEB_SERVICE_CONTENT_ITEM_NOT_FOUND.isAuditable());
    assertFalse(ServerWebServicesErrorCodes.WEB_SERVICE_INSERT_FAILURE.isAuditable());
    assertFalse(ServerWebServicesErrorCodes.WEB_SERVICE_INTERNAL_SEARCH_NOT_FOUND.isAuditable());
  }

  @Test
  void preservesLegacyIpsWebServicesErrorsNumericValues() {
    assertEquals(14001, ServerWebServicesErrorCodes.WEB_SERVICE_CONTENT_ITEM_NOT_FOUND.numericCode());
    assertEquals(14010, ServerWebServicesErrorCodes.WEB_SERVICE_LOGIN_FAILURE.numericCode());
    assertEquals(14026, ServerWebServicesErrorCodes.WEB_SERVICE_SEARCH_RESOURCE_NOT_FOUND.numericCode());
  }
}
