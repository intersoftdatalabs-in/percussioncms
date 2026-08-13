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

import com.intsof.percussioncms.auditlog.AuditLogId;
import com.intsof.percussioncms.auditlog.AuditOutcome;
import com.intsof.percussioncms.auditlog.AuditRecord;
import com.intsof.percussioncms.auditlog.DefaultAuditLogService;
import com.intsof.percussioncms.auditlog.codes.AuthenticationErrorCodes;
import com.intsof.percussioncms.auditlog.spi.AuditLogRepository;
import com.percussion.server.PSServer;
import com.percussion.services.audit.data.PSSystemAuditLogEntry;
import com.percussion.system.utils.PSBaseBean;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.hibernate.Session;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * JPA durable sink for system-wide audit records ({@code PSX_SYSTEM_AUDIT_LOG}).
 *
 * <p>Uses {@link TransactionTemplate} with {@link TransactionDefinition#PROPAGATION_REQUIRES_NEW}
 * for writes so dual-write from {@link DefaultAuditLogService.Holder} commits independently of any
 * ambient request transaction (login/auth paths may join a TX that later rolls back or never
 * commits before redirect). The Holder cannot hold {@code this} from {@link #afterPropertiesSet()}
 * and expect {@code @Transactional} AOP to apply.
 *
 * <p>When the durable table is empty at Spring start and {@code systemAuditLogSeedIfEmpty=true}
 * (default), inserts a few clearly labeled {@code [SEED]} rows so Admin Security Audit Log QA can
 * exercise filters/detail without a customer environment. Real login dual-write still runs after
 * the seed.
 */
@PSBaseBean("sys_systemAuditLogRepository")
public class PSSystemAuditLogRepository implements AuditLogRepository, InitializingBean {

  private static final Logger log = LogManager.getLogger(PSSystemAuditLogRepository.class);

  /**
   * When {@code true} (default), seed a handful of demo audit rows if the durable table is empty
   * after Spring wires this repository. Set {@code false} in production if synthetic rows are not
   * desired before the first real login event.
   */
  public static final String PROP_SEED_IF_EMPTY = "systemAuditLogSeedIfEmpty";

  /** Actor used for synthetic seed rows (filterable in the Admin viewer). */
  public static final String SEED_ACTOR = "system";

  @PersistenceContext private EntityManager entityManager;

  private TransactionTemplate writeTransactionTemplate;
  private TransactionTemplate readTransactionTemplate;

  private boolean seedIfEmpty = true;
  /** When non-null, wins over {@link #resolveSeedIfEmpty(Properties)} (tests / explicit inject). */
  private Boolean seedIfEmptyOverride;

  @Autowired
  public void setTransactionManager(PlatformTransactionManager transactionManager) {
    Objects.requireNonNull(transactionManager, "transactionManager");
    this.writeTransactionTemplate = newWriteTemplate(transactionManager);
    this.readTransactionTemplate = newReadTemplate(transactionManager);
  }

  /** Package-visible for unit tests. */
  void setTransactionTemplate(TransactionTemplate transactionTemplate) {
    // Tests historically inject a single template; use it for both read and write paths.
    this.writeTransactionTemplate = transactionTemplate;
    this.readTransactionTemplate = transactionTemplate;
  }

  /** Package-visible for unit tests that assert REQUIRES_NEW on writes only. */
  void setWriteTransactionTemplate(TransactionTemplate writeTransactionTemplate) {
    this.writeTransactionTemplate = writeTransactionTemplate;
  }

  /** Package-visible for unit tests. */
  void setReadTransactionTemplate(TransactionTemplate readTransactionTemplate) {
    this.readTransactionTemplate = readTransactionTemplate;
  }

  /** Package-visible for unit tests. */
  void setEntityManager(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  /** Package-visible for unit tests / explicit wiring. */
  void setSeedIfEmpty(boolean seedIfEmpty) {
    this.seedIfEmpty = seedIfEmpty;
    this.seedIfEmptyOverride = seedIfEmpty;
  }

  static TransactionTemplate newWriteTemplate(PlatformTransactionManager transactionManager) {
    TransactionTemplate tt = new TransactionTemplate(transactionManager);
    // Audit rows must survive ambient request/login transaction rollback.
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    return tt;
  }

  static TransactionTemplate newReadTemplate(PlatformTransactionManager transactionManager) {
    TransactionTemplate tt = new TransactionTemplate(transactionManager);
    tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
    tt.setReadOnly(true);
    return tt;
  }

  /**
   * Resolves {@link #PROP_SEED_IF_EMPTY} from server properties (or supplied props). Missing or
   * unparsable values default to {@code true} so empty H2/QA installs have viewable rows.
   */
  public static boolean resolveSeedIfEmpty(Properties props) {
    Properties source = props != null ? props : PSServer.getServerProps();
    if (source == null) {
      return true;
    }
    String raw = source.getProperty(PROP_SEED_IF_EMPTY);
    if (raw == null || raw.isBlank()) {
      return true;
    }
    String v = raw.trim().toLowerCase(Locale.ROOT);
    if ("false".equals(v) || "no".equals(v) || "0".equals(v) || "n".equals(v) || "off".equals(v)) {
      return false;
    }
    if ("true".equals(v) || "yes".equals(v) || "1".equals(v) || "y".equals(v) || "on".equals(v)) {
      return true;
    }
    return true;
  }

  @Override
  public void afterPropertiesSet() {
    Objects.requireNonNull(writeTransactionTemplate, "writeTransactionTemplate");
    Objects.requireNonNull(readTransactionTemplate, "readTransactionTemplate");
    Objects.requireNonNull(entityManager, "entityManager");
    if (seedIfEmptyOverride == null) {
      this.seedIfEmpty = resolveSeedIfEmpty(null);
    } else {
      this.seedIfEmpty = seedIfEmptyOverride;
    }
    DefaultAuditLogService.Holder.set(DefaultAuditLogService.create(this));
    log.info(
        "Registered JPA dual-write sink for PSX_SYSTEM_AUDIT_LOG (write TX=REQUIRES_NEW,"
            + " seedIfEmpty={})",
        seedIfEmpty);
    if (seedIfEmpty) {
      try {
        int seeded = seedDemoEventsIfEmpty();
        if (seeded > 0) {
          log.info("Seeded {} demo system audit log row(s) (table was empty)", seeded);
        }
      } catch (RuntimeException ex) {
        // Never fail Spring context because seed failed (missing table on mid-upgrade, etc.).
        log.warn(
            "Could not seed PSX_SYSTEM_AUDIT_LOG demo rows (table may be missing until"
                + " tablefactory upgrade completes): {}",
            ex.toString());
        log.debug("System audit log seed failure details", ex);
      }
    }
  }

  @Override
  public void save(AuditRecord record) {
    Objects.requireNonNull(record, "record");
    Objects.requireNonNull(writeTransactionTemplate, "writeTransactionTemplate");
    writeTransactionTemplate.executeWithoutResult(
        status -> {
          entityManager.persist(PSSystemAuditLogEntry.from(record));
          // Force SQL before TX commit so sink failures surface as dual-write failures, not
          // deferred flush exceptions lost at request teardown.
          entityManager.flush();
        });
  }

  /**
   * Retention helper (AU-11 skeleton): delete rows older than {@code before}.
   *
   * @param before exclusive upper bound (UTC instant)
   * @return number of deleted rows
   */
  public int deleteOlderThan(Instant before) {
    Objects.requireNonNull(before, "before");
    Objects.requireNonNull(writeTransactionTemplate, "writeTransactionTemplate");
    Integer deleted =
        writeTransactionTemplate.execute(
            status ->
                entityManager
                    .unwrap(Session.class)
                    .createMutationQuery(DELETE_OLDER_THAN_HQL)
                    .setParameter("before", Date.from(before))
                    .executeUpdate());
    return deleted == null ? 0 : deleted;
  }

  /** Test / ops helper: count all rows. */
  public long countAll() {
    Objects.requireNonNull(readTransactionTemplate, "readTransactionTemplate");
    Long count =
        readTransactionTemplate.execute(
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
    Objects.requireNonNull(readTransactionTemplate, "readTransactionTemplate");
    return Optional.ofNullable(
        readTransactionTemplate.execute(
            status -> entityManager.find(PSSystemAuditLogEntry.class, auditId)));
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
    Objects.requireNonNull(readTransactionTemplate, "readTransactionTemplate");
    int safeOffset = Math.max(0, offset);
    int safeLimit = clampLimit(limit);
    List<PSSystemAuditLogEntry> rows =
        readTransactionTemplate.execute(
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
    Objects.requireNonNull(readTransactionTemplate, "readTransactionTemplate");
    Long count =
        readTransactionTemplate.execute(
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

  /**
   * When the durable store is empty, insert a few labeled demo events so Admin Security Audit Log
   * filters/detail can be exercised in H2 QA / fresh installs.
   *
   * @return number of rows inserted (0 when table already had data or seeding disabled)
   */
  public int seedDemoEventsIfEmpty() {
    if (countAll() > 0) {
      return 0;
    }
    Instant now = Instant.now();
    // Stagger times so newest-first sort and date filters are meaningful in QA.
    save(
        demoRecord(
            AuthenticationErrorCodes.LOGIN_SUCCESS,
            AuditOutcome.SUCCESS,
            now.minusSeconds(300),
            "Admin",
            "[SEED] User Admin logged in successfully",
            "[SEED] Login success actor=Admin sourceIp=127.0.0.1"));
    save(
        demoRecord(
            AuthenticationErrorCodes.LOGIN_FAILURE,
            AuditOutcome.FAILURE,
            now.minusSeconds(180),
            "baduser",
            "[SEED] Login failed for user baduser",
            "[SEED] Login failure actor=baduser reason=LoginException sourceIp=127.0.0.1"));
    save(
        demoRecord(
            AuthenticationErrorCodes.LOGOUT,
            AuditOutcome.SUCCESS,
            now.minusSeconds(60),
            "Admin",
            "[SEED] User Admin logged out",
            "[SEED] Logout actor=Admin sourceIp=127.0.0.1"));
    return 3;
  }

  private static AuditRecord demoRecord(
      AuthenticationErrorCodes code,
      AuditOutcome outcome,
      Instant when,
      String actor,
      String userMessage,
      String logMessage) {
    String id = UUID.randomUUID().toString();
    return AuditRecord.builder()
        .logId(AuditLogId.of(id))
        .eventTime(when)
        .code(code)
        .outcome(outcome)
        .actor(actor)
        .sourceIp("127.0.0.1")
        .sourceHost("localhost")
        .userMessage(userMessage)
        .logMessage(logMessage)
        .formattedLine("[" + code.qualifiedCode() + "]-[" + id + "] " + logMessage)
        .serverNode(SEED_ACTOR)
        .build();
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

  /** HQL for typed unit tests (issue #3265). */
  public static final String DELETE_OLDER_THAN_HQL =
      "DELETE FROM PSSystemAuditLogEntry e WHERE e.eventTime < :before";

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
