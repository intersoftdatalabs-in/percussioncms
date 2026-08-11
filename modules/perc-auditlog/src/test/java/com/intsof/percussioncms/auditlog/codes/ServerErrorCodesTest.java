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

class ServerErrorCodesTest {

  @Test
  void everyConstantHasExplicitModuleSysAndUniqueNumericCode() {
    Set<Integer> seen = new HashSet<>();
    for (ServerErrorCodes code : ServerErrorCodes.values()) {
      assertEquals(AuditModule.SYS, code.module());
      assertTrue(code.numericCode() > 0, code.name());
      assertTrue(seen.add(code.numericCode()), "duplicate numeric: " + code.numericCode());
      assertNotNull(code.userMessageTemplate());
      assertNotNull(code.logMessageTemplate());
      assertTrue(code.qualifiedCode().startsWith("SYS-"));
    }
    assertEquals(295, ServerErrorCodes.values().length);
  }

  @Test
  void auditableCodesRequireEventType() {
    for (ServerErrorCodes code : ServerErrorCodes.values()) {
      if (code.isAuditable()) {
        assertNotNull(code.eventType(), code.name() + " auditable without eventType");
      } else {
        assertNull(code.eventType(), code.name() + " non-auditable should not force eventType");
      }
    }
  }

  @Test
  void securityRelevantServerFailuresAreAuditable() {
    assertTrue(ServerErrorCodes.AUTHORIZATION_ERROR.isAuditable());
    assertEquals(
        AuditEventType.ACCESS_DENIED, ServerErrorCodes.AUTHORIZATION_ERROR.eventType());
    assertTrue(ServerErrorCodes.TOO_MANY_LOGIN_ATTEMPTS.isAuditable());
    assertEquals(
        AuditEventType.AUTH_FAILURE, ServerErrorCodes.TOO_MANY_LOGIN_ATTEMPTS.eventType());
    assertTrue(ServerErrorCodes.NO_AUTHORIZATION.isAuditable());
    assertTrue(ServerErrorCodes.COMMUNITIES_AUTHENTICATION_FAILED_INVALID_COMMUNITY.isAuditable());
    assertTrue(ServerErrorCodes.COMMUNITIES_AUTHENTICATION_FAILED_ERROR.isAuditable());
    assertTrue(ServerErrorCodes.PRODUCT_EXPIRED.isAuditable());
    assertTrue(ServerErrorCodes.CE_MISSING_CREDENTIALS.isAuditable());
  }

  @Test
  void operationalServerNoiseIsNotAuditable() {
    assertFalse(ServerErrorCodes.NATIVE_ERROR.isAuditable());
    assertFalse(ServerErrorCodes.REQ_DOC_MISSING.isAuditable());
    assertFalse(ServerErrorCodes.REQUEST_HANDLER_NOT_FOUND.isAuditable());
    assertFalse(ServerErrorCodes.SERVER_INIT_START.isAuditable());
    assertFalse(ServerErrorCodes.RCONSOLE_CMD_EMPTY.isAuditable());
    assertFalse(ServerErrorCodes.CE_MISSING_FIELD.isAuditable());
    assertFalse(ServerErrorCodes.APP_LOGIN_PAGE_EXCEPTION.isAuditable());
  }

  @Test
  void preservesLegacyIpsServerErrorsNumericValues() {
    assertEquals(1001, ServerErrorCodes.NATIVE_ERROR.numericCode());
    assertEquals(1101, ServerErrorCodes.AUTHORIZATION_ERROR.numericCode());
    assertEquals(1105, ServerErrorCodes.TOO_MANY_LOGIN_ATTEMPTS.numericCode());
    assertEquals(1120, ServerErrorCodes.NO_AUTHORIZATION.numericCode());
    assertEquals(
        1247, ServerErrorCodes.COMMUNITIES_AUTHENTICATION_FAILED_INVALID_COMMUNITY.numericCode());
    assertEquals(1248, ServerErrorCodes.COMMUNITIES_AUTHENTICATION_FAILED_ERROR.numericCode());
    assertEquals(1527, ServerErrorCodes.PRODUCT_EXPIRED.numericCode());
    assertEquals(1685, ServerErrorCodes.CE_MISSING_CREDENTIALS.numericCode());
    assertEquals(1709, ServerErrorCodes.NORUN_NAMESPACE_CLEANUP_WARNING.numericCode());
  }
}
