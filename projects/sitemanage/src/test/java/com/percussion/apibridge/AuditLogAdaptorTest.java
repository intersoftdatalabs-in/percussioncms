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
package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.auditlog.SystemAuditLogEntry;
import com.percussion.rest.auditlog.SystemAuditLogPage;
import com.percussion.services.audit.data.PSSystemAuditLogEntry;
import com.percussion.services.audit.impl.PSSystemAuditLogRepository;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class AuditLogAdaptorTest {

  @Test
  void adminCanQuery() {
    PSSystemAuditLogRepository repo = mock(PSSystemAuditLogRepository.class);
    PSSystemAuditLogEntry row = sampleRow();
    when(repo.findEntries(
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(0), eq(50)))
        .thenReturn(List.of(row));
    when(repo.countEntries(isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
        .thenReturn(1L);

    AuditLogAdaptor adaptor =
        new AuditLogAdaptor(repo, () -> List.of("Admin"), role -> false);

    SystemAuditLogPage page =
        adaptor.query(null, null, null, null, null, null, 0, 50);
    assertEquals(1, page.getEntries().size());
    assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", page.getEntries().get(0).getAuditId());
    assertEquals(1L, page.getTotal());
    assertEquals(50, page.getLimit());
  }

  @Test
  void propertyHolderCanQuery() {
    PSSystemAuditLogRepository repo = mock(PSSystemAuditLogRepository.class);
    when(repo.findEntries(
            any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(List.of());
    when(repo.countEntries(any(), any(), any(), any(), any(), any())).thenReturn(0L);

    AuditLogAdaptor adaptor =
        new AuditLogAdaptor(repo, () -> List.of("Editor"), role -> "Editor".equals(role));

    SystemAuditLogPage page =
        adaptor.query(null, null, null, null, null, null, 0, 10);
    assertNotNull(page);
    assertEquals(0, page.getEntries().size());
  }

  @Test
  void unauthorizedThrowsSecurityExceptionWithoutTouchingRepo() {
    PSSystemAuditLogRepository repo = mock(PSSystemAuditLogRepository.class);
    AuditLogAdaptor adaptor =
        new AuditLogAdaptor(repo, () -> List.of("Author"), role -> false);

    assertThrows(
        SecurityException.class,
        () -> adaptor.query(null, null, null, null, null, null, 0, 50));
    verify(repo, never())
        .findEntries(any(), any(), any(), any(), any(), any(), anyInt(), anyInt());
  }

  @Test
  void invalidFromIsIllegalArgument() {
    PSSystemAuditLogRepository repo = mock(PSSystemAuditLogRepository.class);
    AuditLogAdaptor adaptor =
        new AuditLogAdaptor(repo, () -> List.of("Admin"), role -> false);
    assertThrows(
        IllegalArgumentException.class,
        () -> adaptor.query("not-an-instant", null, null, null, null, null, 0, 50));
  }

  @Test
  void findByIdMapsAndReturnsNullWhenMissing() {
    PSSystemAuditLogRepository repo = mock(PSSystemAuditLogRepository.class);
    when(repo.findById(eq("missing"))).thenReturn(Optional.empty());
    when(repo.findById(eq("id-1"))).thenReturn(Optional.of(sampleRow()));

    AuditLogAdaptor adaptor =
        new AuditLogAdaptor(repo, () -> List.of("Admin"), role -> false);

    assertNull(adaptor.findById("missing"));
    SystemAuditLogEntry found = adaptor.findById("id-1");
    assertNotNull(found);
    assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", found.getAuditId());
    assertEquals("AUTH", found.getModuleCode());
  }

  @Test
  void findByIdUnauthorized() {
    PSSystemAuditLogRepository repo = mock(PSSystemAuditLogRepository.class);
    AuditLogAdaptor adaptor =
        new AuditLogAdaptor(repo, () -> List.of("Author"), role -> false);
    assertThrows(SecurityException.class, () -> adaptor.findById("id-1"));
    verify(repo, never()).findById(anyString());
  }

  @Test
  void exportAdminPagesThroughRepositoryAndMaps() {
    PSSystemAuditLogRepository repo = mock(PSSystemAuditLogRepository.class);
    when(repo.findEntries(
            isNull(), isNull(), eq("AUTH"), isNull(), isNull(), isNull(), eq(0), eq(50)))
        .thenReturn(List.of(sampleRow()));

    AuditLogAdaptor adaptor =
        new AuditLogAdaptor(repo, () -> List.of("Admin"), role -> false);

    List<SystemAuditLogEntry> out =
        adaptor.export(null, null, "AUTH", null, null, null, 50);
    assertEquals(1, out.size());
    assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", out.get(0).getAuditId());
    assertEquals("AUTH", out.get(0).getModuleCode());
    verify(repo)
        .findEntries(
            isNull(), isNull(), eq("AUTH"), isNull(), isNull(), isNull(), eq(0), eq(50));
  }

  @Test
  void exportUnauthorizedDoesNotTouchRepo() {
    PSSystemAuditLogRepository repo = mock(PSSystemAuditLogRepository.class);
    AuditLogAdaptor adaptor =
        new AuditLogAdaptor(repo, () -> List.of("Author"), role -> false);

    assertThrows(
        SecurityException.class,
        () -> adaptor.export(null, null, null, null, null, null, 100));
    verify(repo, never())
        .findEntries(any(), any(), any(), any(), any(), any(), anyInt(), anyInt());
  }

  @Test
  void exportPropertyHolderAllowed() {
    PSSystemAuditLogRepository repo = mock(PSSystemAuditLogRepository.class);
    when(repo.findEntries(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(List.of());

    AuditLogAdaptor adaptor =
        new AuditLogAdaptor(repo, () -> List.of("Editor"), role -> "Editor".equals(role));

    List<SystemAuditLogEntry> out =
        adaptor.export(null, null, null, null, null, null, 10);
    assertNotNull(out);
    assertEquals(0, out.size());
  }

  @Test
  void exportInvalidFromIsIllegalArgument() {
    PSSystemAuditLogRepository repo = mock(PSSystemAuditLogRepository.class);
    AuditLogAdaptor adaptor =
        new AuditLogAdaptor(repo, () -> List.of("Admin"), role -> false);
    assertThrows(
        IllegalArgumentException.class,
        () -> adaptor.export("not-an-instant", null, null, null, null, null, 10));
  }

  private static PSSystemAuditLogEntry sampleRow() {
    PSSystemAuditLogEntry e = new PSSystemAuditLogEntry();
    e.setAuditId("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    e.setEventTime(Date.from(Instant.parse("2026-08-09T12:00:00Z")));
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
