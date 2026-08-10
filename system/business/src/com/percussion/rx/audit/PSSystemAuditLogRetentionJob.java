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

import com.percussion.server.PSServer;
import com.percussion.services.audit.impl.PSSystemAuditLogRepository;
import com.percussion.system.utils.PSBaseBean;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Production retention job for {@code PSX_SYSTEM_AUDIT_LOG} (NIST AU-11).
 *
 * <p>Spring bean {@code sys_systemAuditLogRetentionJob} wires {@link
 * PSSystemAuditLogRepository} and starts a daemon worker (same lifecycle pattern as {@link
 * PSAuditLogReaper}) that periodically deletes rows older than the configured retention window.
 *
 * <h2>Configuration ({@code rxconfig/Server/server.properties})</h2>
 *
 * <table>
 *   <caption>Retention properties</caption>
 *   <tr>
 *     <th>Key</th>
 *     <th>Default</th>
 *     <th>Meaning</th>
 *   </tr>
 *   <tr>
 *     <td>{@value #PROP_RETENTION_DAYS}</td>
 *     <td>{@value #DEFAULT_RETENTION_DAYS}</td>
 *     <td>
 *       Days to keep system audit log rows. {@code <= 0} disables automatic deletion (rows kept
 *       indefinitely). Invalid / blank values fall back to the default.
 *     </td>
 *   </tr>
 * </table>
 *
 * <p>Sleep interval between runs defaults to 24 hours and is not a server.properties key (tests may
 * inject a shorter interval via {@link #setSleepIntervalMins(int)}).
 */
@PSBaseBean("sys_systemAuditLogRetentionJob")
public class PSSystemAuditLogRetentionJob implements InitializingBean, DisposableBean {

  /** {@code server.properties} key for retention window in days. */
  public static final String PROP_RETENTION_DAYS = "systemAuditLogRetentionDays";

  /** Default retention when the property is absent or unparsable. */
  public static final int DEFAULT_RETENTION_DAYS = 365;

  /** Default sleep between retention runs (24 hours). */
  public static final int DEFAULT_SLEEP_INTERVAL_MINS = 1440;

  private static final long MINS_TO_MILLIS = 60L * 1000L;

  private static final Logger log = LogManager.getLogger(PSSystemAuditLogRetentionJob.class);

  private PSSystemAuditLogRepository repository;
  private int retentionDays = DEFAULT_RETENTION_DAYS;
  private int sleepIntervalMins = DEFAULT_SLEEP_INTERVAL_MINS;
  private Clock clock = Clock.systemUTC();

  private final Object monitor = new Object();
  private volatile Thread worker;
  private volatile boolean started;

  /**
   * Spring injection of the durable audit repository. Starting the worker is deferred to {@link
   * #afterPropertiesSet()} so retention days can be resolved from server properties first.
   *
   * @param repository production repository bean; may be {@code null} only in unit tests
   */
  @Autowired(required = false)
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
   * Minutes to sleep between retention runs. Must be {@code > 0}.
   *
   * @param mins sleep interval in minutes
   */
  public void setSleepIntervalMins(int mins) {
    if (mins <= 0) {
      throw new IllegalArgumentException("sleepIntervalMins must be > 0");
    }
    this.sleepIntervalMins = mins;
  }

  public int getSleepIntervalMins() {
    return sleepIntervalMins;
  }

  /**
   * Package-visible for unit tests that need a fixed clock when asserting cutoffs.
   *
   * @param clock non-null clock
   */
  void setClock(Clock clock) {
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  /**
   * Resolves {@link #PROP_RETENTION_DAYS} from {@link PSServer#getServerProps()} (or the supplied
   * properties when non-null). Invalid or missing values yield {@link #DEFAULT_RETENTION_DAYS}.
   *
   * @param props properties to read; when {@code null}, uses {@link PSServer#getServerProps()}
   * @return parsed days (may be {@code <= 0} to disable)
   */
  public static int resolveRetentionDays(Properties props) {
    Properties source = props != null ? props : PSServer.getServerProps();
    if (source == null) {
      return DEFAULT_RETENTION_DAYS;
    }
    String raw = source.getProperty(PROP_RETENTION_DAYS);
    if (raw == null || raw.isBlank()) {
      return DEFAULT_RETENTION_DAYS;
    }
    try {
      return Integer.parseInt(raw.trim());
    } catch (NumberFormatException e) {
      log.warn(
          "Invalid {} value '{}'; using default {}",
          PROP_RETENTION_DAYS,
          raw,
          DEFAULT_RETENTION_DAYS);
      return DEFAULT_RETENTION_DAYS;
    }
  }

  /**
   * Applies retention days from server properties onto this instance (does not start the worker).
   *
   * @return the resolved retention days
   */
  public int applyRetentionFromServerProperties() {
    int days = resolveRetentionDays(null);
    this.retentionDays = days;
    return days;
  }

  /**
   * Computes the exclusive upper bound for deletion: now minus {@code retentionDays}.
   *
   * @return cutoff instant when retention is enabled; never {@code null}
   * @throws IllegalStateException if retention is disabled ({@code <= 0})
   */
  public Instant computeCutoff() {
    if (retentionDays <= 0) {
      throw new IllegalStateException("retention disabled (retentionDays=" + retentionDays + ")");
    }
    return clock.instant().minus(retentionDays, ChronoUnit.DAYS);
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
    Instant cutoff = computeCutoff();
    int deleted = repository.deleteOlderThan(cutoff);
    log.info(
        "System audit log retention deleted {} row(s) older than {} days (before {})",
        deleted,
        retentionDays,
        cutoff);
    return deleted;
  }

  /**
   * Test / ops helper: run with an explicit cutoff (does not consult retention days).
   *
   * @param before exclusive upper bound
   * @return deleted count
   */
  public int deleteOlderThan(Instant before) {
    Objects.requireNonNull(repository, "repository");
    Objects.requireNonNull(before, "before");
    return repository.deleteOlderThan(before);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Loads retention days from {@code server.properties} and starts the daemon worker when
   * retention is enabled and a repository is available.
   */
  @Override
  public void afterPropertiesSet() {
    applyRetentionFromServerProperties();
    startWorkerIfEnabled();
  }

  /**
   * Starts the background worker when retention is enabled. Idempotent.
   *
   * @return {@code true} if a worker is running (or was already running)
   */
  boolean startWorkerIfEnabled() {
    synchronized (monitor) {
      if (retentionDays <= 0) {
        log.info(
            "System audit log retention is disabled ({}={}). Rows will be kept indefinitely.",
            PROP_RETENTION_DAYS,
            retentionDays);
        return false;
      }
      if (repository == null) {
        log.warn(
            "System audit log retention enabled ({}={}) but repository is not set; worker not"
                + " started",
            PROP_RETENTION_DAYS,
            retentionDays);
        return false;
      }
      if (worker != null && worker.isAlive()) {
        return true;
      }
      Thread t = new Thread(this::runLoop, "SystemAuditLogRetention");
      t.setDaemon(true);
      worker = t;
      started = true;
      t.start();
      log.info(
          "System audit log retention worker started: keep {} day(s), interval {} minute(s)",
          retentionDays,
          sleepIntervalMins);
      return true;
    }
  }

  /** Whether a worker has been started (may still be alive after disable). */
  boolean isWorkerStarted() {
    return started && worker != null && worker.isAlive();
  }

  private void runLoop() {
    log.info("Starting system audit log retention loop");
    try {
      while (!Thread.currentThread().isInterrupted()) {
        synchronized (monitor) {
          try {
            runOnce();
          } catch (RuntimeException e) {
            log.error("Error during system audit log retention", e);
          }
        }
        long sleepTime = sleepIntervalMins * MINS_TO_MILLIS;
        log.debug("Next system audit log retention in {} minutes", sleepIntervalMins);
        Thread.sleep(sleepTime);
      }
    } catch (InterruptedException e) {
      log.info("System audit log retention worker interrupted, shutting down");
      Thread.currentThread().interrupt();
    } catch (Throwable t) {
      log.error("System audit log retention worker terminated by unexpected error", t);
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>Interrupts the retention worker without interrupting an in-flight delete.
   */
  @Override
  public void destroy() {
    shutdown();
  }

  /**
   * Stops the background worker if running. Safe to call when never started.
   */
  public void shutdown() {
    synchronized (monitor) {
      Thread t = worker;
      if (t != null) {
        t.interrupt();
        worker = null;
      }
      started = false;
    }
  }
}
