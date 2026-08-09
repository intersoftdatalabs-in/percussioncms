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
package com.percussion.services.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.intsof.percussioncms.auditlog.AuditContext;
import com.intsof.percussioncms.auditlog.DefaultAuditLogService;
import com.intsof.percussioncms.auditlog.LegacyErrorCodeRegistry;
import com.intsof.percussioncms.auditlog.codes.SecurityErrorCodes;
import com.intsof.percussioncms.auditlog.spi.ConcurrentMemoryAuditLogRepository;
import com.percussion.security.IPSSecurityErrors;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PSSystemAuditLoggerTest {

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
  void loginSuccessWritesAuditableRecord() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRemoteAddr()).thenReturn("203.0.113.10");
    when(request.getRemoteHost()).thenReturn("client.example");

    PSSystemAuditLogger.loginSuccess(request, "jdoe");

    assertEquals(1, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
    var rec = ConcurrentMemoryAuditLogRepository.INSTANCE.findAll().get(0);
    assertEquals("AUTH", rec.code().module().code());
    assertEquals(1001, rec.code().numericCode());
    assertTrue(rec.formattedLine().contains("[AUTH-1001]-"));
    assertEquals("jdoe", rec.actor().orElse(""));
  }

  @Test
  void loginFailureIsAuditable() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRemoteAddr()).thenReturn("203.0.113.11");
    when(request.getRemoteHost()).thenReturn("client.example");

    PSSystemAuditLogger.loginFailure(request, "baduser", "LoginException");

    assertEquals(1, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
    var rec = ConcurrentMemoryAuditLogRepository.INSTANCE.findAll().get(0);
    assertEquals(1002, rec.code().numericCode());
    assertEquals(com.intsof.percussioncms.auditlog.AuditOutcome.FAILURE, rec.outcome());
  }

  @Test
  void logoutIsAuditable() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRemoteAddr()).thenReturn("203.0.113.12");
    when(request.getRemoteHost()).thenReturn("client.example");

    PSSystemAuditLogger.logout(request, "jdoe");

    assertEquals(1, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
    var rec = ConcurrentMemoryAuditLogRepository.INSTANCE.findAll().get(0);
    assertEquals(1003, rec.code().numericCode());
    assertEquals(com.intsof.percussioncms.auditlog.AuditOutcome.SUCCESS, rec.outcome());
  }

  @Test
  void legacyAuthFailureDualWritesViaBridge() {
    var id =
        PSSystemAuditLogger.logLegacyIfAuditable(
            IPSSecurityErrors.AUTHENTICATION_FAILED,
            AuditContext.builder().actor("jdoe").sourceIp("10.0.0.1").build(),
            "Directory",
            "ldap1",
            "jdoe");

    assertTrue(!id.value().equals(LegacyErrorCodeRegistry.SKIPPED.value()));
    assertEquals(1, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
    var rec = ConcurrentMemoryAuditLogRepository.INSTANCE.findAll().get(0);
    assertEquals(SecurityErrorCodes.AUTHENTICATION_FAILED, rec.code());
    assertTrue(rec.formattedLine().startsWith("[SEC-9002]-"));
  }

  @Test
  void legacyNonAuditableProviderNoiseSkipsDualWrite() {
    var id =
        PSSystemAuditLogger.logLegacyIfAuditable(
            IPSSecurityErrors.PROVIDER_UNKNOWN,
            AuditContext.builder().actor("system").build(),
            "Directory",
            "ldap1");

    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    assertEquals(0, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
  }

  @Test
  void ipsSecurityErrorsIntsMatchSecurityErrorCodes() {
    assertEquals(
        SecurityErrorCodes.GENERIC_AUTHENTICATION_FAILED.numericCode(),
        IPSSecurityErrors.GENERIC_AUTHENTICATION_FAILED);
    assertEquals(
        SecurityErrorCodes.SESS_NOT_AUTHORIZED.numericCode(),
        IPSSecurityErrors.SESS_NOT_AUTHORIZED);
  }
}
