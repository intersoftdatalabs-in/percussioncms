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

class MailErrorCodesTest {

  @Test
  void everyConstantHasExplicitModuleSysAndUniqueNumericCode() {
    Set<Integer> seen = new HashSet<>();
    for (MailErrorCodes code : MailErrorCodes.values()) {
      assertEquals(AuditModule.SYS, code.module());
      assertTrue(code.numericCode() >= 3501, code.name());
      assertTrue(seen.add(code.numericCode()), "duplicate numeric: " + code.numericCode());
      assertNotNull(code.userMessageTemplate());
      assertNotNull(code.logMessageTemplate());
      assertTrue(code.qualifiedCode().startsWith("SYS-"));
    }
    assertEquals(8, MailErrorCodes.values().length);
  }

  @Test
  void allMailCodesAreNonAuditable() {
    for (MailErrorCodes code : MailErrorCodes.values()) {
      assertFalse(code.isAuditable(), code.name());
      assertNull(code.eventType(), code.name());
    }
  }

  @Test
  void preservesLegacyIpsMailErrorsNumericValues() {
    assertEquals(3501, MailErrorCodes.MAIL_ADDRESS_EMPTY.numericCode());
    assertEquals(3502, MailErrorCodes.MAIL_ADDRESS_INVALID.numericCode());
    assertEquals(3505, MailErrorCodes.MAIL_SEND_UNEXPECTED_EXCEPTION.numericCode());
    assertEquals(3507, MailErrorCodes.MAIL_SERVER_CONNECTION_ERROR.numericCode());
    assertEquals(3508, MailErrorCodes.HOST_NOT_VALID.numericCode());
  }
}
