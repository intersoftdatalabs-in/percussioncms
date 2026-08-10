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

class SecurityErrorCodesTest {

  @Test
  void everyConstantHasExplicitModuleSecAndUniqueNumericCode() {
    Set<Integer> seen = new HashSet<>();
    for (SecurityErrorCodes code : SecurityErrorCodes.values()) {
      assertEquals(AuditModule.SEC, code.module());
      assertTrue(code.numericCode() > 0, code.name());
      assertTrue(seen.add(code.numericCode()), "duplicate numeric: " + code.numericCode());
      assertNotNull(code.userMessageTemplate());
      assertNotNull(code.logMessageTemplate());
      assertNotNull(code.qualifiedCode());
      assertTrue(code.qualifiedCode().startsWith("SEC-"));
    }
  }

  @Test
  void auditableCodesRequireEventType() {
    for (SecurityErrorCodes code : SecurityErrorCodes.values()) {
      if (code.isAuditable()) {
        assertNotNull(code.eventType(), code.name() + " auditable without eventType");
      } else {
        assertNull(code.eventType(), code.name() + " non-auditable should not force eventType");
      }
    }
  }

  @Test
  void loginPathAuthFailuresAreAuditable() {
    assertTrue(SecurityErrorCodes.AUTHENTICATION_FAILED.isAuditable());
    assertEquals(AuditEventType.AUTH_FAILURE, SecurityErrorCodes.AUTHENTICATION_FAILED.eventType());
    assertTrue(SecurityErrorCodes.AUTHENTICATION_FAILED_WITH_MSG.isAuditable());
    assertTrue(SecurityErrorCodes.MULTI_AUTHENTICATION_FAILED.isAuditable());
    assertTrue(SecurityErrorCodes.GENERIC_AUTHENTICATION_FAILED.isAuditable());
    assertTrue(SecurityErrorCodes.DIR_AUTHENTICATION_FAILED.isAuditable());
    assertTrue(SecurityErrorCodes.USER_NOT_AUTHORIZED.isAuditable());
    assertTrue(SecurityErrorCodes.SESS_NOT_AUTHORIZED.isAuditable());
  }

  @Test
  void operationalProviderNoiseIsNotAuditable() {
    assertFalse(SecurityErrorCodes.AUTHENTICATION_NOT_SUPPORTED.isAuditable());
    assertFalse(SecurityErrorCodes.PROVIDER_NOT_SUPPORTED_BY_CLASS.isAuditable());
    assertFalse(SecurityErrorCodes.PROVIDER_INIT_EXCEPTION.isAuditable());
    assertFalse(SecurityErrorCodes.PROVIDER_UNKNOWN.isAuditable());
    assertFalse(SecurityErrorCodes.SECURITY_NOT_INITIALIZED.isAuditable());
    assertFalse(SecurityErrorCodes.GET_GROUPS_FAILURE.isAuditable());
  }

  @Test
  void preservesLegacyIpsSecurityErrorsNumericValues() {
    assertEquals(9002, SecurityErrorCodes.AUTHENTICATION_FAILED.numericCode());
    assertEquals(9008, SecurityErrorCodes.SESS_NOT_AUTHORIZED.numericCode());
    assertEquals(9009, SecurityErrorCodes.USER_NOT_AUTHORIZED.numericCode());
    assertEquals(9021, SecurityErrorCodes.GENERIC_AUTHENTICATION_FAILED.numericCode());
    assertEquals(9801, SecurityErrorCodes.DIR_AUTHENTICATION_FAILED.numericCode());
    assertEquals(9501, SecurityErrorCodes.HOST_ADDR_FILTER_INVALID.numericCode());
    assertEquals(9601, SecurityErrorCodes.OS_IMPERSONATE_FAILURE.numericCode());
    assertEquals(9701, SecurityErrorCodes.LOCAL_ROLE_NOT_DEFINED.numericCode());
    assertEquals(9852, SecurityErrorCodes.BETABLE_ERROR_UID_NOT_UNIQUE.numericCode());
    assertEquals(9903, SecurityErrorCodes.PARSE_JNDI_PROVIDER_URL_ERROR.numericCode());
  }

  @Test
  void residualSecRangesAuditableFlags() {
    assertTrue(SecurityErrorCodes.OS_IMPERSONATE_FAILURE.isAuditable());
    assertTrue(SecurityErrorCodes.BETABLE_ERROR_UID_NOT_UNIQUE.isAuditable());
    assertTrue(SecurityErrorCodes.LOCAL_ROLE_ALREADY_DEFINED.isAuditable());
    assertFalse(SecurityErrorCodes.HOST_ADDR_FILTER_INVALID.isAuditable());
    assertFalse(SecurityErrorCodes.OSMETA_GET_OBJECTS_FAILURE.isAuditable());
    assertFalse(SecurityErrorCodes.DIR_PASSWORD_FILTER_INIT_ERROR.isAuditable());
    assertFalse(SecurityErrorCodes.MISSING_REQUIRED_ATTRIBUTE.isAuditable());
  }
}
