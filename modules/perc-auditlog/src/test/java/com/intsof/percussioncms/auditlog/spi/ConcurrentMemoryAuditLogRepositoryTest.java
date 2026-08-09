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
package com.intsof.percussioncms.auditlog.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intsof.percussioncms.auditlog.AuditLogId;
import com.intsof.percussioncms.auditlog.AuditOutcome;
import com.intsof.percussioncms.auditlog.AuditRecord;
import com.intsof.percussioncms.auditlog.codes.AuthenticationErrorCodes;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ConcurrentMemoryAuditLogRepositoryTest {

  @Test
  void saveAndFind() {
    ConcurrentMemoryAuditLogRepository repo = new ConcurrentMemoryAuditLogRepository();
    AuditLogId id = AuditLogId.of("11111111-1111-1111-1111-111111111111");
    AuditRecord rec =
        AuditRecord.builder()
            .logId(id)
            .eventTime(Instant.parse("2026-08-09T00:00:00Z"))
            .code(AuthenticationErrorCodes.LOGIN_SUCCESS)
            .outcome(AuditOutcome.SUCCESS)
            .userMessage("u")
            .logMessage("l")
            .formattedLine("[AUTH-1001]-[" + id.value() + "] l")
            .build();
    repo.save(rec);
    assertTrue(repo.find(id).isPresent());
    assertEquals(1, repo.size());
  }
}
