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

class TableFactoryErrorCodesTest {

  @Test
  void everyConstantHasExplicitModuleCfgAndUniqueNumericCode() {
    Set<Integer> seen = new HashSet<>();
    for (TableFactoryErrorCodes code : TableFactoryErrorCodes.values()) {
      assertEquals(AuditModule.CFG, code.module());
      assertTrue(code.numericCode() >= 1001, code.name());
      assertTrue(seen.add(code.numericCode()), "duplicate numeric: " + code.numericCode());
      assertNotNull(code.userMessageTemplate());
      assertNotNull(code.logMessageTemplate());
      assertTrue(code.qualifiedCode().startsWith("CFG-"));
    }
    assertEquals(37, TableFactoryErrorCodes.values().length);
  }

  @Test
  void allTableFactoryCodesAreNonAuditable() {
    for (TableFactoryErrorCodes code : TableFactoryErrorCodes.values()) {
      assertFalse(code.isAuditable(), code.name());
      assertNull(code.eventType(), code.name());
    }
  }

  @Test
  void preservesLegacyIpsTableFactoryErrorsNumericValues() {
    assertEquals(1001, TableFactoryErrorCodes.XML_ELEMENT_NULL.numericCode());
    assertEquals(1008, TableFactoryErrorCodes.LOG_FILE_WRITE_ERROR.numericCode());
    assertEquals(1101, TableFactoryErrorCodes.DATA_TYPE_MAP_NOT_FOUND.numericCode());
    assertEquals(1114, TableFactoryErrorCodes.INVALID_ENCODING.numericCode());
    assertEquals(1201, TableFactoryErrorCodes.SQL_TABLE_META_DATA.numericCode());
    assertEquals(1205, TableFactoryErrorCodes.SQL_CATALOG_DATA.numericCode());
    assertEquals(1301, TableFactoryErrorCodes.SCHEMA_PROCESS_ERROR.numericCode());
    assertEquals(1310, TableFactoryErrorCodes.DATA_HANDLER_CLASS_NOT_FOUND.numericCode());
  }
}
