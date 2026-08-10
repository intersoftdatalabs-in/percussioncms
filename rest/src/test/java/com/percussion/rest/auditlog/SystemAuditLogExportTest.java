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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class SystemAuditLogExportTest {

  @Test
  public void clampMaxRowsDefaultsAndCaps() {
    assertEquals(SystemAuditLogExport.DEFAULT_MAX_ROWS, SystemAuditLogExport.clampMaxRows(0));
    assertEquals(SystemAuditLogExport.DEFAULT_MAX_ROWS, SystemAuditLogExport.clampMaxRows(-1));
    assertEquals(100, SystemAuditLogExport.clampMaxRows(100));
    assertEquals(
        SystemAuditLogExport.MAX_ROWS,
        SystemAuditLogExport.clampMaxRows(SystemAuditLogExport.MAX_ROWS + 500));
  }

  @Test
  public void normalizeFormatAcceptsCsvJsonDefault() {
    assertEquals("json", SystemAuditLogExport.normalizeFormat(null));
    assertEquals("json", SystemAuditLogExport.normalizeFormat(""));
    assertEquals("json", SystemAuditLogExport.normalizeFormat(" JSON "));
    assertEquals("csv", SystemAuditLogExport.normalizeFormat("csv"));
    assertThrows(IllegalArgumentException.class, () -> SystemAuditLogExport.normalizeFormat("xml"));
  }

  @Test
  public void toCsvIncludesHeaderAndEscapesCommasQuotes() {
    SystemAuditLogEntry e = sample();
    e.setUserMessage("hello, \"world\"");
    String csv = SystemAuditLogExport.toCsv(List.of(e));
    // Portable: normalize CRLF to LF for line assertions
    String[] lines = csv.replace("\r\n", "\n").split("\n", -1);
    assertTrue(lines[0].startsWith("auditId,eventTime,moduleCode"));
    assertTrue(lines[1].contains("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"));
    assertTrue(lines[1].contains("\"hello, \"\"world\"\"\""));
    assertTrue(lines[1].contains("AUTH"));
  }

  @Test
  public void toJsonIsArrayWithFields() {
    String json = SystemAuditLogExport.toJson(List.of(sample()));
    assertTrue(json.startsWith("["));
    assertTrue(json.contains("\"auditId\""));
    assertTrue(json.contains("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"));
    assertTrue(json.contains("AUTH"));
    assertFalse(json.contains("SystemAuditLogEntry"));
  }

  @Test
  public void emptyExportShapes() {
    assertEquals("[]", SystemAuditLogExport.toJson(List.of()));
    String csv = SystemAuditLogExport.toCsv(List.of());
    assertTrue(csv.replace("\r\n", "\n").startsWith("auditId,eventTime"));
  }

  private static SystemAuditLogEntry sample() {
    SystemAuditLogEntry e = new SystemAuditLogEntry();
    e.setAuditId("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    e.setEventTime(Instant.parse("2026-08-09T12:00:00Z"));
    e.setModuleCode("AUTH");
    e.setMessageCode(1001);
    e.setEventType("LOGIN");
    e.setOutcome("SUCCESS");
    e.setActor("admin");
    e.setUserMessage("login ok");
    e.setLogMessage("login ok");
    return e;
  }
}
