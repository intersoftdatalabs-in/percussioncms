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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.intsof.percussioncms.auditlog.AuditContext;
import com.intsof.percussioncms.auditlog.AuditLogId;
import com.intsof.percussioncms.auditlog.AuditLogService;
import com.intsof.percussioncms.auditlog.AuditOutcome;
import com.intsof.percussioncms.auditlog.DefaultAuditLogService;
import com.intsof.percussioncms.auditlog.SystemErrorCode;
import com.intsof.percussioncms.auditlog.codes.PathItemErrorCodes;
import com.intsof.percussioncms.auditlog.codes.SecurityErrorCodes;
import com.intsof.percussioncms.auditlog.spi.ConcurrentMemoryAuditLogRepository;
import com.percussion.security.IPSSecurityErrors;
import com.percussion.server.PSRequest;
import com.percussion.utils.request.PSRequestInfo;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
    if (PSRequestInfo.isInited()) {
      PSRequestInfo.resetRequestInfo();
    }
  }

  @AfterEach
  void tearDown() {
    ConcurrentMemoryAuditLogRepository.INSTANCE.clear();
    DefaultAuditLogService.Holder.resetToDefault();
    if (PSRequestInfo.isInited()) {
      PSRequestInfo.resetRequestInfo();
    }
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

  @Test
  void auditContextFromThreadLocalEnrichesActorIpAndSessionHash() {
    Map<String, Object> initial = new HashMap<>();
    initial.put(PSRequestInfo.KEY_USER, "jdoe");
    initial.put(PSRequestInfo.KEY_JSESSIONID, "sess-abc-123");
    PSRequestInfo.initRequestInfo(initial);

    HttpServletRequest http = mock(HttpServletRequest.class);
    when(http.getRemoteAddr()).thenReturn("10.0.0.9");
    when(http.getRemoteHost()).thenReturn("client.example");
    when(http.getRemoteUser()).thenReturn(null);
    PSRequest psRequest = mock(PSRequest.class);
    when(psRequest.getServletRequest()).thenReturn(http);
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_PSREQUEST, psRequest);

    AuditContext ctx = PSErrorHandler.auditContextFromThreadLocal();
    assertEquals(Optional.of("jdoe"), ctx.actor());
    assertEquals(Optional.of("10.0.0.9"), ctx.sourceIp());
    assertEquals(Optional.of("client.example"), ctx.sourceHost());
    assertTrue(ctx.sessionIdHash().isPresent());
    assertEquals(16, ctx.sessionIdHash().get().length());
  }

  @Test
  void runtimeExceptionFromAuditIsSwallowed() {
    DefaultAuditLogService.Holder.set(throwingService(new IllegalStateException("forced")));
    PSException ex =
        new PSException(PathItemErrorCodes.FOLDER_PERMISSION_DENIED.numericCode(), (Object[]) null);

    PSErrorHandler.logLegacyExceptionIfAuditable(ex); // must not throw

    assertEquals(0, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
  }

  @Test
  void errorFromAuditPropagates() {
    DefaultAuditLogService.Holder.set(throwingService(new LinkageError("forced-error")));
    PSException ex =
        new PSException(PathItemErrorCodes.FOLDER_PERMISSION_DENIED.numericCode(), (Object[]) null);

    assertThrows(LinkageError.class, () -> PSErrorHandler.logLegacyExceptionIfAuditable(ex));
  }

  private static AuditLogService throwingService(Throwable t) {
    return new AuditLogService() {
      private RuntimeException fail() {
        if (t instanceof RuntimeException re) {
          throw re;
        }
        if (t instanceof Error err) {
          throw err;
        }
        throw new IllegalStateException(t);
      }

      @Override
      public AuditLogId log(SystemErrorCode code, Object... params) {
        throw fail();
      }

      @Override
      public AuditLogId log(SystemErrorCode code, AuditContext context, Object... params) {
        throw fail();
      }

      @Override
      public AuditLogId log(
          SystemErrorCode code, AuditContext context, AuditOutcome outcome, Object... params) {
        throw fail();
      }
    };
  }
}
