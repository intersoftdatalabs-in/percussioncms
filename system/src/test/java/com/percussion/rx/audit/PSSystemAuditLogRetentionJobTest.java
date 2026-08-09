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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.services.audit.impl.PSSystemAuditLogRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PSSystemAuditLogRetentionJobTest {

  @Test
  void disabledWhenRetentionDaysNonPositive() {
    PSSystemAuditLogRepository repo = mock(PSSystemAuditLogRepository.class);
    PSSystemAuditLogRetentionJob job = new PSSystemAuditLogRetentionJob();
    job.setRepository(repo);
    job.setRetentionDays(0);
    assertEquals(0, job.runOnce());
    verify(repo, never()).deleteOlderThan(any());
  }

  @Test
  void skipsWhenRepositoryMissing() {
    PSSystemAuditLogRetentionJob job = new PSSystemAuditLogRetentionJob();
    job.setRetentionDays(30);
    assertEquals(0, job.runOnce());
  }

  @Test
  void happyPathDelegatesDelete() {
    PSSystemAuditLogRepository repo = mock(PSSystemAuditLogRepository.class);
    when(repo.deleteOlderThan(any(Instant.class))).thenReturn(3);
    PSSystemAuditLogRetentionJob job = new PSSystemAuditLogRetentionJob();
    job.setRepository(repo);
    job.setRetentionDays(30);
    assertEquals(3, job.runOnce());
    verify(repo).deleteOlderThan(any(Instant.class));
  }
}
