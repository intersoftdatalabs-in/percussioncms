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

class ServiceSecurityErrorCodesTest {

  @Test
  void everyConstantHasExplicitModuleSecAndUniqueNumericCode() {
    Set<Integer> seen = new HashSet<>();
    for (ServiceSecurityErrorCodes code : ServiceSecurityErrorCodes.values()) {
      assertEquals(AuditModule.SEC, code.module());
      assertTrue(code.numericCode() > 0, code.name());
      assertTrue(seen.add(code.numericCode()), "duplicate numeric: " + code.numericCode());
      assertNotNull(code.userMessageTemplate());
      assertNotNull(code.logMessageTemplate());
      assertTrue(code.qualifiedCode().startsWith("SEC-"));
    }
    assertEquals(14, ServiceSecurityErrorCodes.values().length);
  }

  @Test
  void auditableCodesRequireEventType() {
    for (ServiceSecurityErrorCodes code : ServiceSecurityErrorCodes.values()) {
      if (code.isAuditable()) {
        assertNotNull(code.eventType(), code.name() + " auditable without eventType");
      } else {
        assertNull(code.eventType(), code.name() + " non-auditable should not force eventType");
      }
    }
  }

  @Test
  void accessAuthzAndPolicyAreAuditable() {
    assertTrue(ServiceSecurityErrorCodes.ACCESS_DENIED.isAuditable());
    assertEquals(AuditEventType.ACCESS_DENIED, ServiceSecurityErrorCodes.ACCESS_DENIED.eventType());
    assertTrue(ServiceSecurityErrorCodes.AUTHENTICATION_FAILED.isAuditable());
    assertEquals(
        AuditEventType.AUTH_FAILURE, ServiceSecurityErrorCodes.AUTHENTICATION_FAILED.eventType());
    assertTrue(ServiceSecurityErrorCodes.AUTHORIZATION_FAILED.isAuditable());
    assertTrue(ServiceSecurityErrorCodes.SESSION_SECURITY_ERROR.isAuditable());
    assertTrue(ServiceSecurityErrorCodes.SECURITY_POLICY_VIOLATION.isAuditable());
  }

  @Test
  void aclLookupAndConfigAreNonAuditable() {
    assertFalse(ServiceSecurityErrorCodes.MISSING_COMMUNITY.isAuditable());
    assertFalse(ServiceSecurityErrorCodes.ACL_NOT_FOUND.isAuditable());
    assertFalse(ServiceSecurityErrorCodes.OBJECT_ACL_NOT_FOUND.isAuditable());
    assertFalse(ServiceSecurityErrorCodes.ACL_SAVE_ERROR.isAuditable());
    assertFalse(ServiceSecurityErrorCodes.ACL_DELETE_ERROR.isAuditable());
    assertFalse(ServiceSecurityErrorCodes.ACL_OPERATION_FAILED.isAuditable());
    assertFalse(ServiceSecurityErrorCodes.CONFIGURATION_ERROR.isAuditable());
    assertFalse(ServiceSecurityErrorCodes.ROLE_MANAGEMENT_ERROR.isAuditable());
    assertFalse(ServiceSecurityErrorCodes.PRINCIPAL_VALIDATION_ERROR.isAuditable());
  }

  @Test
  void preservesLegacyIpsSecurityErrorsNumericValues() {
    assertEquals(1, ServiceSecurityErrorCodes.MISSING_COMMUNITY.numericCode());
    assertEquals(2, ServiceSecurityErrorCodes.ACL_NOT_FOUND.numericCode());
    assertEquals(6, ServiceSecurityErrorCodes.ACCESS_DENIED.numericCode());
    assertEquals(14, ServiceSecurityErrorCodes.SECURITY_POLICY_VIOLATION.numericCode());
  }
}
