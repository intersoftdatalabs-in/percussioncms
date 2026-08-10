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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.server.PSServer;
import com.percussion.services.audit.impl.PSSystemAuditLogRepository;
import com.percussion.util.PSProperties;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for {@link PSSystemAuditLogRetentionJob}. Uses exact production types ({@link
 * PSSystemAuditLogRepository}, {@link PSProperties}) for stubs.
 */
class PSSystemAuditLogRetentionJobTest {

  private static final Instant FIXED_NOW = Instant.parse("2026-08-10T12:00:00Z");

  private Field propsField;
  private PSProperties previousProps;

  @BeforeEach
  void captureServerProps() throws Exception {
    propsField = PSServer.class.getDeclaredField("ms_serverProps");
    propsField.setAccessible(true);
    previousProps = (PSProperties) propsField.get(null);
    // Field type is PSProperties (extends java.util.Properties); plain Properties cannot be set.
    propsField.set(null, new PSProperties());
  }

  @AfterEach
  void restoreServerProps() throws Exception {
    propsField.set(null, previousProps);
  }

  private void setServerProp(String key, String value) throws Exception {
    PSProperties p = (PSProperties) propsField.get(null);
    if (value == null) {
      p.remove(key);
    } else {
      p.setProperty(key, value);
    }
  }

  private static PSSystemAuditLogRetentionJob newJob(PSSystemAuditLogRepository repo, int days) {
    PSSystemAuditLogRetentionJob job = new PSSystemAuditLogRetentionJob();
    job.setRepository(repo);
    job.setRetentionDays(days);
    job.setClock(Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    return job;
  }

  @Test
  void disabledWhenRetentionDaysZero() {
    PSSystemAuditLogRepository repo = mock(PSSystemAuditLogRepository.class);
    PSSystemAuditLogRetentionJob job = newJob(repo, 0);
    assertEquals(0, job.runOnce());
    verify(repo, never()).deleteOlderThan(any());
  }

  @Test
  void disabledWhenRetentionDaysNegative() {
    PSSystemAuditLogRepository repo = mock(PSSystemAuditLogRepository.class);
    PSSystemAuditLogRetentionJob job = newJob(repo, -1);
    assertEquals(0, job.runOnce());
    verify(repo, never()).deleteOlderThan(any());
  }

  @Test
  void skipsWhenRepositoryMissing() {
    PSSystemAuditLogRetentionJob job = newJob(null, 30);
    assertEquals(0, job.runOnce());
  }

  @Test
  void happyPathDelegatesDeleteWithOlderThanCutoff() {
    PSSystemAuditLogRepository repo = mock(PSSystemAuditLogRepository.class);
    when(repo.deleteOlderThan(any(Instant.class))).thenReturn(3);
    PSSystemAuditLogRetentionJob job = newJob(repo, 30);

    assertEquals(3, job.runOnce());

    ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
    verify(repo).deleteOlderThan(cutoffCaptor.capture());
    Instant expectedCutoff = FIXED_NOW.minus(30, ChronoUnit.DAYS);
    assertEquals(expectedCutoff, cutoffCaptor.getValue());
  }

  @Test
  void cutoffDoesNotDeleteRowsNewerThanWindow() {
    // Retention is exclusive upper bound: eventTime < cutoff. A row at FIXED_NOW - 29 days
    // is newer than the 30-day window and must not be in the delete range (cutoff is day-30).
    PSSystemAuditLogRepository repo = mock(PSSystemAuditLogRepository.class);
    when(repo.deleteOlderThan(any(Instant.class))).thenReturn(1);
    PSSystemAuditLogRetentionJob job = newJob(repo, 30);

    job.runOnce();

    ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
    verify(repo).deleteOlderThan(cutoffCaptor.capture());
    Instant cutoff = cutoffCaptor.getValue();
    Instant olderRow = FIXED_NOW.minus(31, ChronoUnit.DAYS);
    Instant newerRow = FIXED_NOW.minus(29, ChronoUnit.DAYS);
    assertTrue(olderRow.isBefore(cutoff), "older row must be before exclusive cutoff");
    assertFalse(newerRow.isBefore(cutoff), "newer row must not be before exclusive cutoff");
    assertTrue(
        newerRow.compareTo(cutoff) >= 0, "rows at/after cutoff are retained by deleteOlderThan");
  }

  @Test
  void computeCutoffThrowsWhenDisabled() {
    PSSystemAuditLogRetentionJob job = newJob(null, 0);
    assertThrows(IllegalStateException.class, job::computeCutoff);
  }

  @Test
  void resolveRetentionDaysUsesDefaultWhenAbsent() {
    assertEquals(
        PSSystemAuditLogRetentionJob.DEFAULT_RETENTION_DAYS,
        PSSystemAuditLogRetentionJob.resolveRetentionDays(new PSProperties()));
  }

  @Test
  void resolveRetentionDaysReadsServerProperties() throws Exception {
    setServerProp(PSSystemAuditLogRetentionJob.PROP_RETENTION_DAYS, "90");
    assertEquals(90, PSSystemAuditLogRetentionJob.resolveRetentionDays(null));
  }

  @Test
  void resolveRetentionDaysAllowsZeroToDisable() throws Exception {
    setServerProp(PSSystemAuditLogRetentionJob.PROP_RETENTION_DAYS, "0");
    assertEquals(0, PSSystemAuditLogRetentionJob.resolveRetentionDays(null));
  }

  @Test
  void resolveRetentionDaysFallsBackOnInvalid() throws Exception {
    setServerProp(PSSystemAuditLogRetentionJob.PROP_RETENTION_DAYS, "not-a-number");
    assertEquals(
        PSSystemAuditLogRetentionJob.DEFAULT_RETENTION_DAYS,
        PSSystemAuditLogRetentionJob.resolveRetentionDays(null));
  }

  @Test
  void applyRetentionFromServerPropertiesSetsField() throws Exception {
    setServerProp(PSSystemAuditLogRetentionJob.PROP_RETENTION_DAYS, "14");
    PSSystemAuditLogRetentionJob job = new PSSystemAuditLogRetentionJob();
    assertEquals(14, job.applyRetentionFromServerProperties());
    assertEquals(14, job.getRetentionDays());
  }

  @Test
  void afterPropertiesSetDoesNotStartWorkerWhenDisabled() throws Exception {
    setServerProp(PSSystemAuditLogRetentionJob.PROP_RETENTION_DAYS, "0");
    PSSystemAuditLogRepository repo = mock(PSSystemAuditLogRepository.class);
    PSSystemAuditLogRetentionJob job = new PSSystemAuditLogRetentionJob();
    job.setRepository(repo);
    job.afterPropertiesSet();
    try {
      assertFalse(job.isWorkerStarted());
      verify(repo, never()).deleteOlderThan(any());
    } finally {
      job.shutdown();
    }
  }

  @Test
  void afterPropertiesSetDoesNotStartWorkerWhenRepositoryUnset() throws Exception {
    setServerProp(PSSystemAuditLogRetentionJob.PROP_RETENTION_DAYS, "30");
    PSSystemAuditLogRetentionJob job = new PSSystemAuditLogRetentionJob();
    job.afterPropertiesSet();
    try {
      assertFalse(job.isWorkerStarted());
    } finally {
      job.shutdown();
    }
  }

  @Test
  void afterPropertiesSetStartsWorkerWhenEnabled() throws Exception {
    setServerProp(PSSystemAuditLogRetentionJob.PROP_RETENTION_DAYS, "30");
    PSSystemAuditLogRepository repo = mock(PSSystemAuditLogRepository.class);
    when(repo.deleteOlderThan(any(Instant.class))).thenReturn(0);
    PSSystemAuditLogRetentionJob job = new PSSystemAuditLogRetentionJob();
    job.setRepository(repo);
    // long sleep so we only observe first runOnce in the loop
    job.setSleepIntervalMins(60);
    job.setClock(Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    job.afterPropertiesSet();
    try {
      assertTrue(job.isWorkerStarted());
      Instant expected = FIXED_NOW.minus(30, ChronoUnit.DAYS);
      long deadline = System.currentTimeMillis() + 5_000L;
      boolean seen = false;
      while (System.currentTimeMillis() < deadline) {
        try {
          verify(repo).deleteOlderThan(expected);
          seen = true;
          break;
        } catch (AssertionError e) {
          Thread.sleep(50);
        }
      }
      assertTrue(seen, "worker should call deleteOlderThan within 5s");
    } finally {
      job.shutdown();
    }
  }

  @Test
  void deleteOlderThanForwardsExplicitCutoff() {
    PSSystemAuditLogRepository repo = mock(PSSystemAuditLogRepository.class);
    when(repo.deleteOlderThan(any(Instant.class))).thenReturn(2);
    PSSystemAuditLogRetentionJob job = newJob(repo, 365);
    Instant before = FIXED_NOW.minus(10, ChronoUnit.DAYS);
    assertEquals(2, job.deleteOlderThan(before));
    verify(repo).deleteOlderThan(before);
  }
}
