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
package com.percussion.rest.auditlog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rest.JacksonContextResolver;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Wire contract for {@link SystemAuditLogPage} / {@link SystemAuditLogEntry} under WRAP_ROOT_VALUE
 * / UNWRAP_ROOT_VALUE (#3089).
 *
 * <p>SPA clients must unwrap {@code {"SystemAuditLogPage":{…}}} or the Security Audit Log viewer
 * always shows 0 rows even when PSX_SYSTEM_AUDIT_LOG and Log4j dual-write succeeded.
 */
@Tag("UnitTest")
public class SystemAuditLogPageSerialDeserialTest {

  private final ObjectMapper pageMapper =
      new JacksonContextResolver().getContext(SystemAuditLogPage.class);

  private final ObjectMapper entryMapper =
      new JacksonContextResolver().getContext(SystemAuditLogEntry.class);

  @Test
  public void serializesPageUnderSystemAuditLogPageRoot() {
    SystemAuditLogEntry e = sampleEntry();
    SystemAuditLogPage page = new SystemAuditLogPage(List.of(e), 1L, 0, 50);

    String json = pageMapper.writeValueAsString(page);

    assertTrue(json.contains("\"SystemAuditLogPage\""), "expected WRAP_ROOT_VALUE: " + json);
    assertTrue(json.contains("\"entries\""), json);
    assertTrue(json.contains("\"total\""), json);
    assertTrue(json.contains("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"), json);
    assertTrue(json.contains("AUTH"), json);
  }

  @Test
  public void deserializesWrappedPageWithEntries() {
    String json =
        "{\"SystemAuditLogPage\":{"
            + "\"entries\":[{"
            + "\"auditId\":\"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee\","
            + "\"moduleCode\":\"AUTH\","
            + "\"eventType\":\"AUTH_LOGIN\","
            + "\"outcome\":\"SUCCESS\","
            + "\"actor\":\"Admin\""
            + "}],"
            + "\"total\":1,"
            + "\"offset\":0,"
            + "\"limit\":50"
            + "}}";

    SystemAuditLogPage page = pageMapper.readValue(json, SystemAuditLogPage.class);
    assertEquals(1L, page.getTotal());
    assertEquals(0, page.getOffset());
    assertEquals(50, page.getLimit());
    assertNotNull(page.getEntries());
    assertEquals(1, page.getEntries().size());
    assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", page.getEntries().get(0).getAuditId());
    assertEquals("AUTH", page.getEntries().get(0).getModuleCode());
  }

  @Test
  public void serializesEntryUnderSystemAuditLogEntryRoot() {
    SystemAuditLogEntry e = sampleEntry();
    String json = entryMapper.writeValueAsString(e);

    assertTrue(json.contains("\"SystemAuditLogEntry\""), "expected WRAP_ROOT_VALUE: " + json);
    assertTrue(json.contains("\"auditId\""), json);
    assertTrue(json.contains("User Admin logged in"), json);
  }

  @Test
  public void deserializesWrappedEntry() {
    String json =
        "{\"SystemAuditLogEntry\":{"
            + "\"auditId\":\"bbbbbbbb-bbbb-cccc-dddd-eeeeeeeeeeee\","
            + "\"moduleCode\":\"AUTH\","
            + "\"userMessage\":\"User Admin logged out\""
            + "}}";

    SystemAuditLogEntry e = entryMapper.readValue(json, SystemAuditLogEntry.class);
    assertEquals("bbbbbbbb-bbbb-cccc-dddd-eeeeeeeeeeee", e.getAuditId());
    assertEquals("AUTH", e.getModuleCode());
    assertEquals("User Admin logged out", e.getUserMessage());
  }

  @Test
  public void roundTripPagePreservesTotalAndActor() {
    SystemAuditLogPage page = new SystemAuditLogPage(List.of(sampleEntry()), 7L, 5, 25);
    String json = pageMapper.writeValueAsString(page);
    SystemAuditLogPage back = pageMapper.readValue(json, SystemAuditLogPage.class);
    assertEquals(7L, back.getTotal());
    assertEquals(5, back.getOffset());
    assertEquals(25, back.getLimit());
    assertEquals(1, back.getEntries().size());
    assertEquals("Admin", back.getEntries().get(0).getActor());
  }

  private static SystemAuditLogEntry sampleEntry() {
    SystemAuditLogEntry e = new SystemAuditLogEntry();
    e.setAuditId("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    e.setEventTime(Instant.parse("2026-08-09T15:00:00Z"));
    e.setModuleCode("AUTH");
    e.setMessageCode(1001);
    e.setEventType("AUTH_LOGIN");
    e.setOutcome("SUCCESS");
    e.setActor("Admin");
    e.setUserMessage("User Admin logged in");
    e.setLogMessage("[AUTH-1001]-[aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee] login ok");
    return e;
  }
}
