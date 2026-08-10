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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class AuditLogResourceTest {

  private IAuditLogAdaptor adaptor;
  private AuditLogResource resource;

  @BeforeEach
  public void setUp() {
    adaptor = mock(IAuditLogAdaptor.class);
    resource = new AuditLogResource(adaptor);
  }

  @Test
  public void queryDelegates() {
    SystemAuditLogPage page = new SystemAuditLogPage(List.of(), 0, 0, 50);
    when(adaptor.query(
            isNull(), isNull(), eq("AUTH"), isNull(), isNull(), isNull(), eq(0), eq(50)))
        .thenReturn(page);

    SystemAuditLogPage out =
        resource.queryEntries(null, null, "AUTH", null, null, null, 0, 50);
    assertSame(page, out);
    verify(adaptor)
        .query(isNull(), isNull(), eq("AUTH"), isNull(), isNull(), isNull(), eq(0), eq(50));
  }

  @Test
  public void queryMapsSecurityExceptionTo403() {
    when(adaptor.query(
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(0), eq(50)))
        .thenThrow(new SecurityException("not allowed"));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.queryEntries(null, null, null, null, null, null, 0, 50));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void queryMapsIllegalArgumentTo400() {
    when(adaptor.query(
            eq("bad"), isNull(), isNull(), isNull(), isNull(), isNull(), eq(0), eq(50)))
        .thenThrow(new IllegalArgumentException("from is not a valid ISO-8601 instant"));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.queryEntries("bad", null, null, null, null, null, 0, 50));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void getEntryDelegates() {
    SystemAuditLogEntry entry = new SystemAuditLogEntry();
    entry.setAuditId("id-1");
    when(adaptor.findById(eq("id-1"))).thenReturn(entry);
    assertEquals("id-1", resource.getEntry("id-1").getAuditId());
  }

  @Test
  public void getEntryMissingIs404() {
    when(adaptor.findById(eq("missing"))).thenReturn(null);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getEntry("missing"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void getEntryForbiddenIs403() {
    when(adaptor.findById(eq("id-1"))).thenThrow(new SecurityException("denied"));
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getEntry("id-1"));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void missingAdaptorIs503() {
    AuditLogResource bare = new AuditLogResource();
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> bare.queryEntries(null, null, null, null, null, null, 0, 50));
    assertEquals(503, ex.getResponse().getStatus());
  }

  @Test
  public void exportJsonAuthorizedShape() {
    SystemAuditLogEntry entry = new SystemAuditLogEntry();
    entry.setAuditId("id-export-1");
    entry.setEventTime(Instant.parse("2026-08-09T12:00:00Z"));
    entry.setModuleCode("AUTH");
    entry.setOutcome("SUCCESS");
    when(adaptor.export(
            isNull(), isNull(), eq("AUTH"), isNull(), isNull(), isNull(), eq(100)))
        .thenReturn(List.of(entry));

    Response response =
        resource.exportEntries("json", null, null, "AUTH", null, null, null, 100);
    assertEquals(200, response.getStatus());
    assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString());
    String body = String.valueOf(response.getEntity());
    assertTrue(body.contains("id-export-1"));
    assertTrue(body.contains("AUTH"));
    assertTrue(String.valueOf(response.getHeaderString("Content-Disposition")).contains(".json"));
    verify(adaptor)
        .export(isNull(), isNull(), eq("AUTH"), isNull(), isNull(), isNull(), eq(100));
  }

  @Test
  public void exportCsvAuthorizedShape() {
    SystemAuditLogEntry entry = new SystemAuditLogEntry();
    entry.setAuditId("id-csv-1");
    entry.setModuleCode("AUTH");
    entry.setUserMessage("ok");
    when(adaptor.export(
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(0)))
        .thenReturn(List.of(entry));

    Response response =
        resource.exportEntries("csv", null, null, null, null, null, null, 0);
    assertEquals(200, response.getStatus());
    assertEquals("text/csv", response.getMediaType().toString());
    String body = String.valueOf(response.getEntity());
    assertTrue(body.startsWith("auditId,") || body.replace("\r\n", "\n").startsWith("auditId,"));
    assertTrue(body.contains("id-csv-1"));
    assertTrue(String.valueOf(response.getHeaderString("Content-Disposition")).contains(".csv"));
  }

  @Test
  public void exportMapsSecurityExceptionTo403() {
    when(adaptor.export(
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(0)))
        .thenThrow(new SecurityException("not allowed"));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.exportEntries("json", null, null, null, null, null, null, 0));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void exportInvalidFormatIs400() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.exportEntries("xml", null, null, null, null, null, null, 0));
    assertEquals(400, ex.getResponse().getStatus());
  }
}
