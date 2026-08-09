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
package com.percussion.services.audit.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.intsof.percussioncms.auditlog.AuditLogId;
import com.intsof.percussioncms.auditlog.AuditOutcome;
import com.intsof.percussioncms.auditlog.AuditRecord;
import com.intsof.percussioncms.auditlog.codes.AuthenticationErrorCodes;
import com.percussion.services.audit.data.PSSystemAuditLogEntry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Proves durable save runs inside a TransactionTemplate (Holder dual-write safe without AOP
 * proxy) and that Phase 3 query helpers clamp paging and bind filters.
 */
class PSSystemAuditLogRepositoryTest {

  @Test
  void saveUsesTransactionTemplateAndPersistsEntity() {
    EntityManager em = mock(EntityManager.class);
    PlatformTransactionManager tm = mock(PlatformTransactionManager.class);
    TransactionStatus status = new SimpleTransactionStatus();
    when(tm.getTransaction(any(TransactionDefinition.class))).thenReturn(status);

    TransactionTemplate tt = new TransactionTemplate(tm);

    PSSystemAuditLogRepository repo = new PSSystemAuditLogRepository();
    repo.setEntityManager(em);
    repo.setTransactionTemplate(tt);

    AuditRecord record =
        AuditRecord.builder()
            .logId(AuditLogId.of("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"))
            .eventTime(Instant.parse("2026-08-09T12:00:00Z"))
            .code(AuthenticationErrorCodes.LOGIN_SUCCESS)
            .outcome(AuditOutcome.SUCCESS)
            .userMessage("u")
            .logMessage("l")
            .formattedLine("[AUTH-1001]-[aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee] l")
            .build();

    repo.save(record);

    verify(em).persist(any(PSSystemAuditLogEntry.class));
    verify(tm).getTransaction(any(TransactionDefinition.class));
    verify(tm).commit(status);
  }

  @Test
  void clampLimitDefaultsAndCaps() {
    assertEquals(
        PSSystemAuditLogRepository.DEFAULT_PAGE_SIZE, PSSystemAuditLogRepository.clampLimit(0));
    assertEquals(
        PSSystemAuditLogRepository.DEFAULT_PAGE_SIZE, PSSystemAuditLogRepository.clampLimit(-5));
    assertEquals(10, PSSystemAuditLogRepository.clampLimit(10));
    assertEquals(
        PSSystemAuditLogRepository.MAX_PAGE_SIZE,
        PSSystemAuditLogRepository.clampLimit(PSSystemAuditLogRepository.MAX_PAGE_SIZE + 50));
  }

  @Test
  @SuppressWarnings("unchecked")
  void findEntriesAppliesFiltersOffsetAndLimit() {
    EntityManager em = mock(EntityManager.class);
    PlatformTransactionManager tm = mock(PlatformTransactionManager.class);
    TransactionStatus status = new SimpleTransactionStatus();
    when(tm.getTransaction(any(TransactionDefinition.class))).thenReturn(status);
    TypedQuery<PSSystemAuditLogEntry> q = mock(TypedQuery.class);
    when(em.createQuery(anyString(), eq(PSSystemAuditLogEntry.class))).thenReturn(q);
    when(q.setParameter(anyString(), any())).thenReturn(q);
    when(q.setFirstResult(any(Integer.class))).thenReturn(q);
    when(q.setMaxResults(any(Integer.class))).thenReturn(q);
    when(q.getResultList()).thenReturn(List.of());

    PSSystemAuditLogRepository repo = new PSSystemAuditLogRepository();
    repo.setEntityManager(em);
    repo.setTransactionTemplate(new TransactionTemplate(tm));

    Instant from = Instant.parse("2026-08-01T00:00:00Z");
    Instant to = Instant.parse("2026-08-10T00:00:00Z");
    repo.findEntries(from, to, "AUTH", "LOGIN", "SUCCESS", "Admin", 5, 10);

    verify(em)
        .createQuery(
            eq(
                "SELECT e FROM PSSystemAuditLogEntry e WHERE e.eventTime >= :fromTime AND"
                    + " e.eventTime < :toTime AND e.moduleCode = :moduleCode AND e.eventType ="
                    + " :eventType AND e.outcome = :outcome AND LOWER(e.actor) = :actor ORDER BY"
                    + " e.eventTime DESC, e.auditId DESC"),
            eq(PSSystemAuditLogEntry.class));
    verify(q).setParameter(eq("fromTime"), eq(Date.from(from)));
    verify(q).setParameter(eq("toTime"), eq(Date.from(to)));
    verify(q).setParameter(eq("moduleCode"), eq("AUTH"));
    verify(q).setParameter(eq("eventType"), eq("LOGIN"));
    verify(q).setParameter(eq("outcome"), eq("SUCCESS"));
    verify(q).setParameter(eq("actor"), eq("admin"));
    verify(q).setFirstResult(5);
    verify(q).setMaxResults(10);
  }

  @Test
  void findByIdDelegatesToEntityManager() {
    EntityManager em = mock(EntityManager.class);
    PlatformTransactionManager tm = mock(PlatformTransactionManager.class);
    TransactionStatus status = new SimpleTransactionStatus();
    when(tm.getTransaction(any(TransactionDefinition.class))).thenReturn(status);
    PSSystemAuditLogEntry entry = new PSSystemAuditLogEntry();
    entry.setAuditId("id-1");
    when(em.find(PSSystemAuditLogEntry.class, "id-1")).thenReturn(entry);

    PSSystemAuditLogRepository repo = new PSSystemAuditLogRepository();
    repo.setEntityManager(em);
    repo.setTransactionTemplate(new TransactionTemplate(tm));

    Optional<PSSystemAuditLogEntry> found = repo.findById("id-1");
    assertTrue(found.isPresent());
    assertEquals("id-1", found.get().getAuditId());
  }
}
