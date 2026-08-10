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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
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
    when(adaptor.query(isNull(), isNull(), eq("AUTH"), isNull(), isNull(), isNull(), eq(0), eq(50)))
        .thenReturn(page);

    SystemAuditLogPage out = resource.queryEntries(null, null, "AUTH", null, null, null, 0, 50);
    assertSame(page, out);
    verify(adaptor)
        .query(isNull(), isNull(), eq("AUTH"), isNull(), isNull(), isNull(), eq(0), eq(50));
  }

  @Test
  public void queryMapsSecurityExceptionTo403() {
    when(adaptor.query(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(0), eq(50)))
        .thenThrow(new SecurityException("not allowed"));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.queryEntries(null, null, null, null, null, null, 0, 50));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void queryMapsIllegalArgumentTo400() {
    when(adaptor.query(eq("bad"), isNull(), isNull(), isNull(), isNull(), isNull(), eq(0), eq(50)))
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
}
