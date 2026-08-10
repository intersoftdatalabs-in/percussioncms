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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.intsof.percussioncms.auditlog.AuditLogId;
import com.intsof.percussioncms.auditlog.AuditOutcome;
import com.intsof.percussioncms.auditlog.AuditRecord;
import com.intsof.percussioncms.auditlog.DefaultAuditLogService;
import com.intsof.percussioncms.auditlog.codes.AuthenticationErrorCodes;
import com.percussion.services.audit.data.PSSystemAuditLogEntry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Proves durable save runs inside a TransactionTemplate with REQUIRES_NEW (Holder dual-write safe
 * without AOP proxy and independent of ambient request TX), flush, seed-if-empty, and Phase 3 query
 * helpers.
 */
class PSSystemAuditLogRepositoryTest {

  @BeforeEach
  void resetHolder() {
    DefaultAuditLogService.Holder.resetToDefault();
  }

  @AfterEach
  void tearDownHolder() {
    DefaultAuditLogService.Holder.resetToDefault();
  }

  @Test
  void saveUsesRequiresNewTransactionAndFlushes() {
    EntityManager em = mock(EntityManager.class);
    PlatformTransactionManager tm = mock(PlatformTransactionManager.class);
    TransactionStatus status = new SimpleTransactionStatus();
    when(tm.getTransaction(any(TransactionDefinition.class))).thenReturn(status);

    PSSystemAuditLogRepository repo = new PSSystemAuditLogRepository();
    repo.setEntityManager(em);
    repo.setTransactionManager(tm);

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
    verify(em).flush();
    ArgumentCaptor<TransactionDefinition> defCap =
        ArgumentCaptor.forClass(TransactionDefinition.class);
    verify(tm).getTransaction(defCap.capture());
    assertEquals(
        TransactionDefinition.PROPAGATION_REQUIRES_NEW, defCap.getValue().getPropagationBehavior());
    verify(tm).commit(status);
  }

  @Test
  void holderDualWritePathInvokesRepositorySaveUnderRequiresNew() {
    EntityManager em = mock(EntityManager.class);
    PlatformTransactionManager tm = mock(PlatformTransactionManager.class);
    TransactionStatus status = new SimpleTransactionStatus();
    when(tm.getTransaction(any(TransactionDefinition.class))).thenReturn(status);

    PSSystemAuditLogRepository repo = new PSSystemAuditLogRepository();
    repo.setEntityManager(em);
    repo.setTransactionManager(tm);
    repo.setSeedIfEmpty(false);
    repo.afterPropertiesSet();

    DefaultAuditLogService.Holder.get()
        .log(
            AuthenticationErrorCodes.LOGIN_SUCCESS,
            com.intsof.percussioncms.auditlog.AuditContext.builder().actor("admin").build(),
            AuditOutcome.SUCCESS,
            "admin",
            "127.0.0.1");

    verify(em).persist(any(PSSystemAuditLogEntry.class));
    verify(em).flush();
    ArgumentCaptor<TransactionDefinition> defCap =
        ArgumentCaptor.forClass(TransactionDefinition.class);
    verify(tm).getTransaction(defCap.capture());
    assertEquals(
        TransactionDefinition.PROPAGATION_REQUIRES_NEW, defCap.getValue().getPropagationBehavior());
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
    repo.setTransactionManager(tm);

    Instant from = Instant.parse("2026-08-01T00:00:00Z");
    Instant to = Instant.parse("2026-08-10T00:00:00Z");
    repo.findEntries(from, to, "AUTH", "AUTH_LOGIN", "SUCCESS", "Admin", 5, 10);

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
    verify(q).setParameter(eq("eventType"), eq("AUTH_LOGIN"));
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
    repo.setTransactionManager(tm);

    Optional<PSSystemAuditLogEntry> found = repo.findById("id-1");
    assertTrue(found.isPresent());
    assertEquals("id-1", found.get().getAuditId());
  }

  @Test
  @SuppressWarnings("unchecked")
  void seedDemoEventsIfEmptyInsertsThreeRowsWhenEmpty() {
    EntityManager em = mock(EntityManager.class);
    PlatformTransactionManager tm = mock(PlatformTransactionManager.class);
    TransactionStatus status = new SimpleTransactionStatus();
    when(tm.getTransaction(any(TransactionDefinition.class))).thenReturn(status);

    TypedQuery<Long> countQuery = mock(TypedQuery.class);
    when(em.createQuery(anyString(), eq(Long.class))).thenReturn(countQuery);
    // First countAll() → 0 (empty); after seed, callers may count again.
    when(countQuery.getSingleResult()).thenReturn(0L);

    PSSystemAuditLogRepository repo = new PSSystemAuditLogRepository();
    repo.setEntityManager(em);
    repo.setTransactionManager(tm);

    int seeded = repo.seedDemoEventsIfEmpty();
    assertEquals(3, seeded);
    verify(em, times(3)).persist(any(PSSystemAuditLogEntry.class));
    verify(em, times(3)).flush();
  }

  @Test
  @SuppressWarnings("unchecked")
  void seedDemoEventsIfEmptySkipsWhenRowsExist() {
    EntityManager em = mock(EntityManager.class);
    PlatformTransactionManager tm = mock(PlatformTransactionManager.class);
    TransactionStatus status = new SimpleTransactionStatus();
    when(tm.getTransaction(any(TransactionDefinition.class))).thenReturn(status);

    TypedQuery<Long> countQuery = mock(TypedQuery.class);
    when(em.createQuery(anyString(), eq(Long.class))).thenReturn(countQuery);
    when(countQuery.getSingleResult()).thenReturn(7L);

    PSSystemAuditLogRepository repo = new PSSystemAuditLogRepository();
    repo.setEntityManager(em);
    repo.setTransactionManager(tm);

    assertEquals(0, repo.seedDemoEventsIfEmpty());
    verify(em, times(0)).persist(any(PSSystemAuditLogEntry.class));
  }

  @Test
  void resolveSeedIfEmptyDefaultsTrueAndParsesFalse() {
    assertTrue(PSSystemAuditLogRepository.resolveSeedIfEmpty(null));
    assertTrue(PSSystemAuditLogRepository.resolveSeedIfEmpty(new Properties()));
    Properties on = new Properties();
    on.setProperty(PSSystemAuditLogRepository.PROP_SEED_IF_EMPTY, "yes");
    assertTrue(PSSystemAuditLogRepository.resolveSeedIfEmpty(on));
    Properties off = new Properties();
    off.setProperty(PSSystemAuditLogRepository.PROP_SEED_IF_EMPTY, "false");
    assertEquals(false, PSSystemAuditLogRepository.resolveSeedIfEmpty(off));
  }

  @Test
  void newWriteTemplateUsesRequiresNew() {
    PlatformTransactionManager tm = mock(PlatformTransactionManager.class);
    TransactionTemplate tt = PSSystemAuditLogRepository.newWriteTemplate(tm);
    assertEquals(
        TransactionDefinition.PROPAGATION_REQUIRES_NEW, tt.getPropagationBehavior());
  }

  @Test
  void ambientRollbackDoesNotAffectRequiresNewWriteTemplateSemantics() {
    // Behavioral contract: write template asks TM for REQUIRES_NEW each save.
    AtomicInteger getTxCalls = new AtomicInteger();
    PlatformTransactionManager tm =
        new PlatformTransactionManager() {
          @Override
          public TransactionStatus getTransaction(TransactionDefinition definition) {
            getTxCalls.incrementAndGet();
            assertEquals(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW, definition.getPropagationBehavior());
            return new SimpleTransactionStatus();
          }

          @Override
          public void commit(TransactionStatus status) {}

          @Override
          public void rollback(TransactionStatus status) {}
        };
    EntityManager em = mock(EntityManager.class);
    PSSystemAuditLogRepository repo = new PSSystemAuditLogRepository();
    repo.setEntityManager(em);
    repo.setTransactionManager(tm);

    AuditRecord record =
        AuditRecord.builder()
            .logId(AuditLogId.of("bbbbbbbb-bbbb-cccc-dddd-eeeeeeeeeeee"))
            .eventTime(Instant.parse("2026-08-10T12:00:00Z"))
            .code(AuthenticationErrorCodes.LOGOUT)
            .outcome(AuditOutcome.SUCCESS)
            .userMessage("u")
            .logMessage("l")
            .formattedLine("[AUTH-1003]-[bbbbbbbb-bbbb-cccc-dddd-eeeeeeeeeeee] l")
            .build();
    repo.save(record);
    assertEquals(1, getTxCalls.get());
  }
}
