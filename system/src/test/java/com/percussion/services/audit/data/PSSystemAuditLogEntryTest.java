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
package com.percussion.services.audit.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intsof.percussioncms.auditlog.AuditLogId;
import com.intsof.percussioncms.auditlog.AuditOutcome;
import com.intsof.percussioncms.auditlog.AuditRecord;
import com.intsof.percussioncms.auditlog.codes.AuthenticationErrorCodes;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PSSystemAuditLogEntryTest {

  @Test
  void fromMapsAuditRecordFields() {
    AuditRecord record =
        AuditRecord.builder()
            .logId(AuditLogId.of("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"))
            .eventTime(Instant.parse("2026-08-09T12:00:00Z"))
            .code(AuthenticationErrorCodes.LOGIN_SUCCESS)
            .outcome(AuditOutcome.SUCCESS)
            .actor("jdoe")
            .sourceIp("10.0.0.1")
            .userMessage("User jdoe logged in successfully")
            .logMessage("Login success actor=jdoe sourceIp=10.0.0.1")
            .formattedLine(
                "[AUTH-1001]-[aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee] Login success actor=jdoe"
                    + " sourceIp=10.0.0.1")
            .attributes(Map.of("k", "v"))
            .build();

    PSSystemAuditLogEntry e = PSSystemAuditLogEntry.from(record);
    assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", e.getAuditId());
    assertEquals("AUTH", e.getModuleCode());
    assertEquals(1001, e.getMessageCode());
    assertEquals("AUTH_LOGIN", e.getEventType());
    assertEquals("SUCCESS", e.getOutcome());
    assertEquals("jdoe", e.getActor());
    assertEquals("10.0.0.1", e.getSourceIp());
    assertTrue(e.getAttributesJson().contains("\"k\":\"v\""));
  }

  @Test
  void truncateHonorsMax() {
    assertNull(PSSystemAuditLogEntry.truncate(null, 10));
    assertEquals("abc", PSSystemAuditLogEntry.truncate("abc", 10));
    assertEquals("0123456789", PSSystemAuditLogEntry.truncate("0123456789ABCDEF", 10));
  }

  @Test
  void attributesToJsonEscapesQuotesAndNewlines() {
    String json =
        PSSystemAuditLogEntry.attributesToJson(Map.of("note", "line1\nline2 \"quoted\""));
    assertTrue(json.contains("\\n"));
    assertTrue(json.contains("\\\""));
    assertTrue(json.startsWith("{"));
    assertTrue(json.endsWith("}"));
  }
}
