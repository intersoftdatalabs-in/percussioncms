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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.intsof.percussioncms.auditlog.AuditLogId;
import com.intsof.percussioncms.auditlog.AuditOutcome;
import com.intsof.percussioncms.auditlog.AuditRecord;
import com.intsof.percussioncms.auditlog.codes.AuthenticationErrorCodes;
import com.percussion.services.audit.data.PSSystemAuditLogEntry;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Proves durable save runs inside a TransactionTemplate (Holder dual-write safe without AOP
 * proxy).
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
}
