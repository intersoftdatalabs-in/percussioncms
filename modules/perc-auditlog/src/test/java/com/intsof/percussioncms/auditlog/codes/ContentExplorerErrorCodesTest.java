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
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ContentExplorerErrorCodesTest {

  @Test
  void everyConstantHasExplicitModuleSysAndUniqueNumericCode() {
    Set<Integer> seen = new HashSet<>();
    for (ContentExplorerErrorCodes code : ContentExplorerErrorCodes.values()) {
      assertEquals(AuditModule.SYS, code.module());
      assertTrue(code.numericCode() >= 20001, code.name());
      assertTrue(code.numericCode() <= 20011, code.name());
      assertTrue(seen.add(code.numericCode()), "duplicate numeric: " + code.numericCode());
      assertNotNull(code.userMessageTemplate());
      assertNotNull(code.logMessageTemplate());
      assertTrue(code.qualifiedCode().startsWith("SYS-"));
    }
    assertEquals(11, ContentExplorerErrorCodes.values().length);
  }

  @Test
  void allContentExplorerCodesAreNonAuditable() {
    for (ContentExplorerErrorCodes code : ContentExplorerErrorCodes.values()) {
      assertFalse(code.isAuditable(), code.name());
      assertNull(code.eventType(), code.name());
    }
  }

  @Test
  void preservesLegacyIpsContentExplorerErrorsNumericValues() {
    assertEquals(20001, ContentExplorerErrorCodes.GENERAL_ERROR.numericCode());
    assertEquals(20004, ContentExplorerErrorCodes.OPTIONS_LOAD_ERROR.numericCode());
    assertEquals(20007, ContentExplorerErrorCodes.SEARCH_ERROR.numericCode());
    assertEquals(20010, ContentExplorerErrorCodes.INCOMPATIBLE_PATHS.numericCode());
    assertEquals(20011, ContentExplorerErrorCodes.SITEDEF_UPDATE_FAILURES.numericCode());
  }
}
