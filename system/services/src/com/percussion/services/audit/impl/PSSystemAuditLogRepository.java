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
import java.util.Objects;
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
}
