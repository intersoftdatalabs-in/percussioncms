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

class XmlErrorCodesTest {

  @Test
  void everyConstantHasExplicitModuleSysAndUniqueNumericCode() {
    Set<Integer> seen = new HashSet<>();
    for (XmlErrorCodes code : XmlErrorCodes.values()) {
      assertEquals(AuditModule.SYS, code.module());
      assertTrue(code.numericCode() >= 1, code.name());
      assertTrue(seen.add(code.numericCode()), "duplicate numeric: " + code.numericCode());
      assertNotNull(code.userMessageTemplate());
      assertNotNull(code.logMessageTemplate());
      assertTrue(code.qualifiedCode().startsWith("SYS-"));
    }
    assertEquals(12, XmlErrorCodes.values().length);
  }

  @Test
  void allXmlCodesAreNonAuditable() {
    for (XmlErrorCodes code : XmlErrorCodes.values()) {
      assertFalse(code.isAuditable(), code.name());
      assertNull(code.eventType(), code.name());
    }
  }

  @Test
  void preservesLegacyIpsXmlErrorsNumericValues() {
    // utils package-local
    assertEquals(1, XmlErrorCodes.XML_ELEMENT_MISSING.numericCode());
    assertEquals(6, XmlErrorCodes.XML_RESTORE_ERROR.numericCode());
    // system
    assertEquals(6001, XmlErrorCodes.RAW_XML_DUMP.numericCode());
    assertEquals(6002, XmlErrorCodes.XML_PROCESSING_ERROR.numericCode());
    assertEquals(6025, XmlErrorCodes.DTD_IO_ERROR.numericCode());
    assertEquals(6028, XmlErrorCodes.DTD_ELEMENT_NOTFOUND_ERROR.numericCode());
  }
}
