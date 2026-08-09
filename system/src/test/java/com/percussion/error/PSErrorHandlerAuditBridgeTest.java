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
package com.percussion.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intsof.percussioncms.auditlog.DefaultAuditLogService;
import com.intsof.percussioncms.auditlog.codes.PathItemErrorCodes;
import com.intsof.percussioncms.auditlog.codes.SecurityErrorCodes;
import com.intsof.percussioncms.auditlog.spi.ConcurrentMemoryAuditLogRepository;
import com.percussion.security.IPSSecurityErrors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Phase 2b: {@link PSErrorHandler} dual-writes only when the legacy error int is registered and
 * auditable.
 */
class PSErrorHandlerAuditBridgeTest {

  @BeforeEach
  void setUp() {
    ConcurrentMemoryAuditLogRepository.INSTANCE.clear();
    DefaultAuditLogService.Holder.resetToDefault();
  }

  @AfterEach
  void tearDown() {
    ConcurrentMemoryAuditLogRepository.INSTANCE.clear();
    DefaultAuditLogService.Holder.resetToDefault();
  }

  @Test
  void auditablePsExceptionDualWritesViaAppendError() {
    PSException ex =
        new PSException(
            IPSSecurityErrors.AUTHENTICATION_FAILED,
            new Object[] {"Directory", "ldap1", "jdoe"});

    Document doc = PSErrorHandler.fillErrorResponse(ex);

    assertEquals(1, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
    var rec = ConcurrentMemoryAuditLogRepository.INSTANCE.findAll().get(0);
    assertEquals(SecurityErrorCodes.AUTHENTICATION_FAILED, rec.code());
    assertTrue(rec.formattedLine().startsWith("[SEC-9002]-"));

    Element root = doc.getDocumentElement();
    assertEquals("PSXError", root.getNodeName());
  }

  @Test
  void nonAuditablePsExceptionSkipsDualWrite() {
    PSException ex =
        new PSException(IPSSecurityErrors.PROVIDER_UNKNOWN, new Object[] {"Directory", "ldap1"});

    PSErrorHandler.fillErrorResponse(ex);

    assertEquals(0, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
  }

  @Test
  void folderPermissionDeniedDualWritesContCode() {
    PSException ex =
        new PSException(PathItemErrorCodes.FOLDER_PERMISSION_DENIED.numericCode(), (Object[]) null);

    PSErrorHandler.logLegacyExceptionIfAuditable(ex);

    assertEquals(1, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
    var rec = ConcurrentMemoryAuditLogRepository.INSTANCE.findAll().get(0);
    assertEquals(PathItemErrorCodes.FOLDER_PERMISSION_DENIED, rec.code());
  }

  @Test
  void nullExceptionIsNoOp() {
    PSErrorHandler.logLegacyExceptionIfAuditable(null);
    assertEquals(0, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
  }
}
