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

import com.intsof.percussioncms.auditlog.AuditRecord;
import com.intsof.percussioncms.auditlog.DefaultAuditLogService;
import com.intsof.percussioncms.auditlog.spi.AuditLogRepository;
import com.percussion.services.audit.data.PSSystemAuditLogEntry;
import com.percussion.system.utils.PSBaseBean;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * JPA durable sink for system-wide audit records ({@code PSX_SYSTEM_AUDIT_LOG}).
 *
 * <p>Uses {@link TransactionTemplate} for writes so dual-write from {@link
 * DefaultAuditLogService.Holder} works without relying on a Spring AOP proxy (the Holder cannot
 * hold {@code this} from {@link #afterPropertiesSet()} and expect {@code @Transactional} to apply).
 */
@PSBaseBean("sys_systemAuditLogRepository")
public class PSSystemAuditLogRepository implements AuditLogRepository, InitializingBean {

  @PersistenceContext private EntityManager entityManager;

  private TransactionTemplate transactionTemplate;

  @Autowired
  public void setTransactionManager(PlatformTransactionManager transactionManager) {
    Objects.requireNonNull(transactionManager, "transactionManager");
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  /** Package-visible for unit tests. */
  void setTransactionTemplate(TransactionTemplate transactionTemplate) {
    this.transactionTemplate = transactionTemplate;
  }

  /** Package-visible for unit tests. */
  void setEntityManager(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  public void afterPropertiesSet() {
    Objects.requireNonNull(transactionTemplate, "transactionTemplate");
    Objects.requireNonNull(entityManager, "entityManager");
    DefaultAuditLogService.Holder.set(DefaultAuditLogService.create(this));
  }

  @Override
  public void save(AuditRecord record) {
    Objects.requireNonNull(record, "record");
    Objects.requireNonNull(transactionTemplate, "transactionTemplate");
    transactionTemplate.executeWithoutResult(
        status -> entityManager.persist(PSSystemAuditLogEntry.from(record)));
  }

  /**
   * Retention helper (AU-11 skeleton): delete rows older than {@code before}.
   *
   * @param before exclusive upper bound (UTC instant)
   * @return number of deleted rows
   */
  public int deleteOlderThan(Instant before) {
    Objects.requireNonNull(before, "before");
    Objects.requireNonNull(transactionTemplate, "transactionTemplate");
    Integer deleted =
        transactionTemplate.execute(
            status ->
                entityManager
                    .createQuery("DELETE FROM PSSystemAuditLogEntry e WHERE e.eventTime < :before")
                    .setParameter("before", Date.from(before))
                    .executeUpdate());
    return deleted == null ? 0 : deleted;
  }

  /** Test / ops helper: count all rows. */
  public long countAll() {
    Objects.requireNonNull(transactionTemplate, "transactionTemplate");
    Long count =
        transactionTemplate.execute(
            status -> {
              TypedQuery<Long> q =
                  entityManager.createQuery(
                      "SELECT COUNT(e) FROM PSSystemAuditLogEntry e", Long.class);
              return q.getSingleResult();
            });
    return count == null ? 0L : count;
  }

  /**
   * Load one durable audit row by primary key.
   *
   * @param auditId UUID string (AUDIT_ID); may not be blank
   * @return entry if present
   */
  public Optional<PSSystemAuditLogEntry> findById(String auditId) {
    Objects.requireNonNull(auditId, "auditId");
    if (auditId.isBlank()) {
      throw new IllegalArgumentException("auditId may not be blank");
    }
    Objects.requireNonNull(transactionTemplate, "transactionTemplate");
    return Optional.ofNullable(
        transactionTemplate.execute(status -> entityManager.find(PSSystemAuditLogEntry.class, auditId)));
  }

  /**
   * Query durable audit rows with optional filters, newest first.
   *
   * <p>Null/blank filter arguments are ignored. {@code offset} defaults to 0 when negative; {@code
   * limit} is clamped to {@code 1..MAX_PAGE_SIZE}.
   */
  public List<PSSystemAuditLogEntry> findEntries(
      Instant fromInclusive,
      Instant toExclusive,
      String moduleCode,
      String eventType,
      String outcome,
      String actor,
      int offset,
      int limit) {
    Objects.requireNonNull(transactionTemplate, "transactionTemplate");
    int safeOffset = Math.max(0, offset);
    int safeLimit = clampLimit(limit);
    List<PSSystemAuditLogEntry> rows =
        transactionTemplate.execute(
            status -> {
              QueryParts qp =
                  buildWhere(
                      fromInclusive, toExclusive, moduleCode, eventType, outcome, actor);
              String jpql =
                  "SELECT e FROM PSSystemAuditLogEntry e"
                      + qp.where
                      + " ORDER BY e.eventTime DESC, e.auditId DESC";
              TypedQuery<PSSystemAuditLogEntry> q =
                  entityManager.createQuery(jpql, PSSystemAuditLogEntry.class);
              qp.apply(q);
              q.setFirstResult(safeOffset);
              q.setMaxResults(safeLimit);
              return q.getResultList();
            });
    return rows == null ? List.of() : rows;
  }

  /**
   * Count rows matching the same filters as {@link #findEntries(Instant, Instant, String, String,
   * String, String, int, int)} (offset/limit ignored).
   */
  public long countEntries(
      Instant fromInclusive,
      Instant toExclusive,
      String moduleCode,
      String eventType,
      String outcome,
      String actor) {
    Objects.requireNonNull(transactionTemplate, "transactionTemplate");
    Long count =
        transactionTemplate.execute(
            status -> {
              QueryParts qp =
                  buildWhere(
                      fromInclusive, toExclusive, moduleCode, eventType, outcome, actor);
              String jpql = "SELECT COUNT(e) FROM PSSystemAuditLogEntry e" + qp.where;
              TypedQuery<Long> q = entityManager.createQuery(jpql, Long.class);
              qp.apply(q);
              return q.getSingleResult();
            });
    return count == null ? 0L : count;
  }

  /** Maximum page size for REST / ops queries (hard cap). */
  public static final int MAX_PAGE_SIZE = 200;

  /** Default page size when caller omits or passes non-positive limit. */
  public static final int DEFAULT_PAGE_SIZE = 50;

  public static int clampLimit(int limit) {
    if (limit <= 0) {
      return DEFAULT_PAGE_SIZE;
    }
    return Math.min(limit, MAX_PAGE_SIZE);
  }

  private static QueryParts buildWhere(
      Instant fromInclusive,
      Instant toExclusive,
      String moduleCode,
      String eventType,
      String outcome,
      String actor) {
    StringBuilder where = new StringBuilder();
    Map<String, Object> params = new LinkedHashMap<>();
    if (fromInclusive != null) {
      where.append(where.isEmpty() ? " WHERE " : " AND ");
      where.append("e.eventTime >= :fromTime");
      params.put("fromTime", Date.from(fromInclusive));
    }
    if (toExclusive != null) {
      where.append(where.isEmpty() ? " WHERE " : " AND ");
      where.append("e.eventTime < :toTime");
      params.put("toTime", Date.from(toExclusive));
    }
    if (moduleCode != null && !moduleCode.isBlank()) {
      where.append(where.isEmpty() ? " WHERE " : " AND ");
      where.append("e.moduleCode = :moduleCode");
      params.put("moduleCode", moduleCode.trim());
    }
    if (eventType != null && !eventType.isBlank()) {
      where.append(where.isEmpty() ? " WHERE " : " AND ");
      where.append("e.eventType = :eventType");
      params.put("eventType", eventType.trim());
    }
    if (outcome != null && !outcome.isBlank()) {
      where.append(where.isEmpty() ? " WHERE " : " AND ");
      where.append("e.outcome = :outcome");
      params.put("outcome", outcome.trim());
    }
    if (actor != null && !actor.isBlank()) {
      where.append(where.isEmpty() ? " WHERE " : " AND ");
      where.append("LOWER(e.actor) = :actor");
      params.put("actor", actor.trim().toLowerCase(Locale.ROOT));
    }
    return new QueryParts(where.toString(), params);
  }

  private static final class QueryParts {
    final String where;
    final Map<String, Object> params;

    QueryParts(String where, Map<String, Object> params) {
      this.where = where;
      this.params = params;
    }

    void apply(TypedQuery<?> q) {
      for (Map.Entry<String, Object> e : params.entrySet()) {
        q.setParameter(e.getKey(), e.getValue());
      }
    }
  }
}
