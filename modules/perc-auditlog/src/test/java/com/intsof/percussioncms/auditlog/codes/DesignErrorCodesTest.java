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

class DesignErrorCodesTest {

  @Test
  void everyConstantHasDesnModuleUniqueNumericAndTemplates() {
    Set<Integer> seen = new HashSet<>();
    for (DesignErrorCodes code : DesignErrorCodes.values()) {
      assertEquals(AuditModule.DESN, code.module());
      assertTrue(code.numericCode() > 0, code.name());
      assertTrue(seen.add(code.numericCode()), "duplicate numeric: " + code.numericCode());
      assertNotNull(code.userMessageTemplate());
      assertNotNull(code.logMessageTemplate());
      assertTrue(code.qualifiedCode().startsWith("DESN-"));
    }
  }

  @Test
  void auditableCodesRequireEventType() {
    for (DesignErrorCodes code : DesignErrorCodes.values()) {
      if (code.isAuditable()) {
        assertNotNull(code.eventType(), code.name());
      } else {
        assertNull(code.eventType(), code.name());
      }
    }
  }

  @Test
  void lifecycleAndAclFailuresAreAuditable() {
    assertTrue(DesignErrorCodes.CREATE.isAuditable());
    assertEquals(AuditEventType.DESIGN_CREATE, DesignErrorCodes.CREATE.eventType());
    assertTrue(DesignErrorCodes.UPDATE.isAuditable());
    assertTrue(DesignErrorCodes.DELETE.isAuditable());
    assertTrue(DesignErrorCodes.APP_ACL_NO_MANAGER.isAuditable());
    assertEquals(AuditEventType.ACL_CHANGE, DesignErrorCodes.APP_ACL_NO_MANAGER.eventType());
    assertTrue(DesignErrorCodes.SRV_ACL_NO_ADMIN.isAuditable());
  }

  @Test
  void structuralAclValidationNoiseIsNotAuditable() {
    assertFalse(DesignErrorCodes.ACL_ENTRYLIST_NULL.isAuditable());
    assertFalse(DesignErrorCodes.ACL_ENTRY_NAME_EMPTY.isAuditable());
    assertFalse(DesignErrorCodes.ACL_TYPE_INVALID.isAuditable());
    assertFalse(DesignErrorCodes.SPINST_TYPE_INVALID.isAuditable());
  }

  @Test
  void preservesLegacyObjectstoreAclNumericValues() {
    assertEquals(2203, DesignErrorCodes.APP_ACL_NO_MANAGER.numericCode());
    assertEquals(2353, DesignErrorCodes.SRV_ACL_NO_ADMIN.numericCode());
    assertEquals(2901, DesignErrorCodes.CREATE.numericCode());
  }
}
