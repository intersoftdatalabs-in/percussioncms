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

class UtilErrorCodesTest {

  @Test
  void everyConstantHasExplicitModuleSysAndUniqueNumericCode() {
    Set<Integer> seen = new HashSet<>();
    for (UtilErrorCodes code : UtilErrorCodes.values()) {
      assertEquals(AuditModule.SYS, code.module());
      assertTrue(code.numericCode() >= 10001, code.name());
      assertTrue(seen.add(code.numericCode()), "duplicate numeric: " + code.numericCode());
      assertNotNull(code.userMessageTemplate());
      assertNotNull(code.logMessageTemplate());
      assertTrue(code.qualifiedCode().startsWith("SYS-"));
    }
    assertEquals(6, UtilErrorCodes.values().length);
  }

  @Test
  void allUtilCodesAreNonAuditable() {
    for (UtilErrorCodes code : UtilErrorCodes.values()) {
      assertFalse(code.isAuditable(), code.name());
      assertNull(code.eventType(), code.name());
    }
  }

  @Test
  void preservesLegacyIpsUtilErrorsNumericValues() {
    assertEquals(10001, UtilErrorCodes.BASE64_ENCODING_EXCEPTION.numericCode());
    assertEquals(10002, UtilErrorCodes.BASE64_DECODING_EXCEPTION.numericCode());
    assertEquals(10051, UtilErrorCodes.COLLECTION_CLASS_NOT_FOUND.numericCode());
    assertEquals(10101, UtilErrorCodes.PURGABLE_TEMP_DIR_IS_FILE.numericCode());
    assertEquals(10202, UtilErrorCodes.RECEIVE_DATA_ERROR.numericCode());
    assertEquals(10203, UtilErrorCodes.POST_DATA_ERROR.numericCode());
  }
}
