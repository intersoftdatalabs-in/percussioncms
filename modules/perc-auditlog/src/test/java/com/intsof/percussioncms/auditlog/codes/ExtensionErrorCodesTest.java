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

class ExtensionErrorCodesTest {

  @Test
  void everyConstantHasExplicitModuleSysAndUniqueNumericCode() {
    Set<Integer> seen = new HashSet<>();
    for (ExtensionErrorCodes code : ExtensionErrorCodes.values()) {
      assertEquals(AuditModule.SYS, code.module());
      assertTrue(code.numericCode() > 0, code.name());
      assertTrue(seen.add(code.numericCode()), "duplicate numeric: " + code.numericCode());
      assertNotNull(code.userMessageTemplate());
      assertNotNull(code.logMessageTemplate());
      assertTrue(code.qualifiedCode().startsWith("SYS-"));
    }
    assertEquals(129, ExtensionErrorCodes.values().length);
  }

  @Test
  void auditableCodesRequireEventType() {
    for (ExtensionErrorCodes code : ExtensionErrorCodes.values()) {
      if (code.isAuditable()) {
        assertNotNull(code.eventType(), code.name() + " auditable without eventType");
      } else {
        assertNull(code.eventType(), code.name() + " non-auditable should not force eventType");
      }
    }
  }

  @Test
  void authAndAccessControlFailuresAreAuditable() {
    assertTrue(ExtensionErrorCodes.AUTHENTICATION_FAILED1.isAuditable());
    assertEquals(AuditEventType.AUTH_FAILURE, ExtensionErrorCodes.AUTHENTICATION_FAILED1.eventType());
    assertTrue(ExtensionErrorCodes.AUTHENTICATION_FAILED2.isAuditable());
    assertTrue(ExtensionErrorCodes.CHECKOUT_NOT_ALLOWED.isAuditable());
    assertEquals(AuditEventType.ACCESS_DENIED, ExtensionErrorCodes.CHECKOUT_NOT_ALLOWED.eventType());
    assertTrue(ExtensionErrorCodes.CHECKIN_NOT_ALLOWED.isAuditable());
    assertTrue(
        ExtensionErrorCodes.COMMUNITIES_AUTHENTICATION_FAILED_NOCOMMUNITY.isAuditable());
    assertTrue(
        ExtensionErrorCodes.AUTHENTICATION_FAILED_DIFFERENT_ITEM_USER_COMMUNITIES.isAuditable());
  }

  @Test
  void operationalExtensionNoiseIsNotAuditable() {
    assertFalse(ExtensionErrorCodes.EXT_PARAM_VALUE_INVALID.isAuditable());
    assertFalse(ExtensionErrorCodes.JS_COMPILE_FAILED.isAuditable());
    assertFalse(ExtensionErrorCodes.EXT_NOT_FOUND.isAuditable());
    assertFalse(ExtensionErrorCodes.JEXL_EVALUATION_FAILED.isAuditable());
  }

  @Test
  void preservesLegacyIpsExtensionErrorsNumericValues() {
    assertEquals(7001, ExtensionErrorCodes.BACKEND_COLUMN_ERROR.numericCode());
    assertEquals(7007, ExtensionErrorCodes.EXT_PARAM_VALUE_INVALID.numericCode());
    assertEquals(7442, ExtensionErrorCodes.CHECKOUT_NOT_ALLOWED.numericCode());
    assertEquals(7480, ExtensionErrorCodes.MANDATORY_TRANSITION_VALIDATION_FAILURE.numericCode());
    assertEquals(7636, ExtensionErrorCodes.SCHEME_CANT_BE_FOUND.numericCode());
  }
}
