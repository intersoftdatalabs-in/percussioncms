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
package com.percussion.rx.audit;

import com.percussion.services.audit.impl.PSSystemAuditLogRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Retention skeleton for {@code PSX_SYSTEM_AUDIT_LOG} (NIST AU-11).
 *
 * <p>Phase 1 provides the delete-by-age API; scheduling / config wiring (days from
 * server.properties) is a follow-on to the design-object reaper generalization.
 */
public class PSSystemAuditLogRetentionJob {

  private static final Logger log = LogManager.getLogger(PSSystemAuditLogRetentionJob.class);

  private PSSystemAuditLogRepository repository;
  private int retentionDays = 365;

  public void setRepository(PSSystemAuditLogRepository repository) {
    this.repository = repository;
  }

  /**
   * @param retentionDays days to keep; {@code <= 0} disables deletion
   */
  public void setRetentionDays(int retentionDays) {
    this.retentionDays = retentionDays;
  }

  public int getRetentionDays() {
    return retentionDays;
  }

  /**
   * Deletes entries older than the configured retention window.
   *
   * @return deleted row count, or {@code 0} if disabled / not wired
   */
  public int runOnce() {
    if (retentionDays <= 0) {
      log.debug("System audit log retention disabled (retentionDays={})", retentionDays);
      return 0;
    }
    if (repository == null) {
      log.warn("System audit log retention skipped: repository not set");
      return 0;
    }
    Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
    int deleted = repository.deleteOlderThan(cutoff);
    log.info(
        "System audit log retention deleted {} row(s) older than {} days (before {})",
        deleted,
        retentionDays,
        cutoff);
    return deleted;
  }

  /** Test helper: run with explicit cutoff. */
  public int deleteOlderThan(Instant before) {
    Objects.requireNonNull(repository, "repository");
    Objects.requireNonNull(before, "before");
    return repository.deleteOlderThan(before);
  }
}
