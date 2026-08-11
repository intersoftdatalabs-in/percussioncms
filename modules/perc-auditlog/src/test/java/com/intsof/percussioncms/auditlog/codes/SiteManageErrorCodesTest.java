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

import com.intsof.percussioncms.auditlog.AuditModule;
import org.junit.jupiter.api.Test;

class SiteManageErrorCodesTest {

  @Test
  void singleConstantIsNonAuditableCfgModule() {
    assertEquals(1, SiteManageErrorCodes.values().length);
    SiteManageErrorCodes code = SiteManageErrorCodes.SITE_MANAGE_SERVICE_DELETING_BAD_SITE_RECORD;
    assertEquals(AuditModule.CFG, code.module());
    assertEquals(18252, code.numericCode());
    assertFalse(code.isAuditable());
    assertNull(code.eventType());
    assertNotNull(code.userMessageTemplate());
    assertNotNull(code.logMessageTemplate());
    assertTrue(code.qualifiedCode().startsWith("CFG-"));
  }
}
