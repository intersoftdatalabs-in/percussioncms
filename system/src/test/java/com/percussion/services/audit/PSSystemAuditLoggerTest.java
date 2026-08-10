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
import com.intsof.percussioncms.auditlog.AuditOutcome;
import com.intsof.percussioncms.auditlog.DefaultAuditLogService;
import com.intsof.percussioncms.auditlog.LegacyErrorCodeRegistry;
import com.intsof.percussioncms.auditlog.codes.ContentErrorCodes;
import com.intsof.percussioncms.auditlog.codes.DesignErrorCodes;
import com.intsof.percussioncms.auditlog.codes.PathItemErrorCodes;
import com.intsof.percussioncms.auditlog.codes.SecurityErrorCodes;
import com.intsof.percussioncms.auditlog.codes.WorkflowErrorCodes;
import com.intsof.percussioncms.auditlog.spi.ConcurrentMemoryAuditLogRepository;
import com.percussion.content.IPSContentErrors;
import com.percussion.security.IPSSecurityErrors;
import com.percussion.services.workflow.IPSWorkflowErrors;
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
    assertEquals(AuditOutcome.FAILURE, rec.outcome());
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
    assertEquals(AuditOutcome.SUCCESS, rec.outcome());
  }

  @Test
  void sessionRevokeIsAuditable() {
    HttpServletRequest request = mockRequest("admin", "10.0.0.1");
    PSSystemAuditLogger.sessionRevoke(request, "admin");

    assertEquals(1, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
    var rec = ConcurrentMemoryAuditLogRepository.INSTANCE.findAll().get(0);
    assertEquals(1004, rec.code().numericCode());
    assertEquals("AUTH", rec.code().module().code());
  }

  @Test
  void contentCreateWritesContCode() {
    HttpServletRequest request = mockRequest("editor", "10.0.0.2");
    PSSystemAuditLogger.contentCreate(
        request, AuditOutcome.SUCCESS, "guid-1", "42", "/Sites/demo");

    assertEquals(1, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
    var rec = ConcurrentMemoryAuditLogRepository.INSTANCE.findAll().get(0);
    assertEquals("CONT", rec.code().module().code());
    assertEquals(2001, rec.code().numericCode());
    assertTrue(rec.formattedLine().contains("guid-1"));
    assertEquals(AuditOutcome.SUCCESS, rec.outcome());
  }

  @Test
  void contentDeleteFailureOutcome() {
    HttpServletRequest request = mockRequest("editor", "10.0.0.3");
    PSSystemAuditLogger.contentDelete(
        request, AuditOutcome.FAILURE, "guid-2", "99", "/Sites/x");

    var rec = ConcurrentMemoryAuditLogRepository.INSTANCE.findAll().get(0);
    assertEquals(2003, rec.code().numericCode());
    assertEquals(AuditOutcome.FAILURE, rec.outcome());
  }

  @Test
  void contentRecycleAndSchedules() {
    HttpServletRequest request = mockRequest("editor", "10.0.0.4");
    PSSystemAuditLogger.contentRecycle(
        request, AuditOutcome.SUCCESS, "g-r", "1", "/path");
    PSSystemAuditLogger.pagePublishSchedule(
        request, AuditOutcome.SUCCESS, "g-p", "2", "/path");
    PSSystemAuditLogger.pageRemovalSchedule(
        request, AuditOutcome.SUCCESS, "g-m", "3", "/path");

    assertEquals(3, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
    var codes =
        ConcurrentMemoryAuditLogRepository.INSTANCE.findAll().stream()
            .map(r -> r.code().numericCode())
            .collect(java.util.stream.Collectors.toSet());
    assertTrue(codes.contains(2004));
    assertTrue(codes.contains(2005));
    assertTrue(codes.contains(2006));
    // Publish + removal schedules share the publishing lifecycle event type.
    assertTrue(
        ConcurrentMemoryAuditLogRepository.INSTANCE.findAll().stream()
            .filter(r -> r.code().numericCode() == 2005 || r.code().numericCode() == 2006)
            .allMatch(
                r ->
                    r.code().eventType()
                        == com.intsof.percussioncms.auditlog.AuditEventType.CONTENT_PUBLISH));
  }

  @Test
  void userManagementCreateUpdateDelete() {
    HttpServletRequest request = mockRequest("admin", "10.0.0.5");
    PSSystemAuditLogger.userCreate(request, AuditOutcome.SUCCESS, "newuser");
    PSSystemAuditLogger.userUpdate(request, AuditOutcome.SUCCESS, "newuser", "roles");
    PSSystemAuditLogger.userDelete(request, AuditOutcome.FAILURE, "newuser");

    assertEquals(3, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
    var codes =
        ConcurrentMemoryAuditLogRepository.INSTANCE.findAll().stream()
            .map(r -> r.code().numericCode())
            .collect(java.util.stream.Collectors.toSet());
    assertTrue(codes.contains(3001));
    assertTrue(codes.contains(3002));
    assertTrue(codes.contains(3003));
    assertTrue(
        ConcurrentMemoryAuditLogRepository.INSTANCE.findAll().stream()
            .anyMatch(
                r ->
                    r.code().numericCode() == 3003
                        && r.outcome() == AuditOutcome.FAILURE));
  }

  @Test
  void workflowTransitionWritesWfCode() {
    HttpServletRequest request = mockRequest("approver", "10.0.0.6");
    PSSystemAuditLogger.workflowTransition(
        request, AuditOutcome.SUCCESS, "55", "guid-wf", "Draft", "Pending");

    var rec = ConcurrentMemoryAuditLogRepository.INSTANCE.findAll().get(0);
    assertEquals("WF", rec.code().module().code());
    assertEquals(4001, rec.code().numericCode());
    assertTrue(rec.formattedLine().contains("Draft"));
    assertTrue(rec.formattedLine().contains("Pending"));
    // fromState then toState (not action labels) must appear in that semantic order.
    int fromIdx = rec.formattedLine().indexOf("Draft");
    int toIdx = rec.formattedLine().indexOf("Pending");
    assertTrue(fromIdx >= 0 && toIdx > fromIdx);
    assertEquals("Draft", rec.attributes().get("fromState"));
    assertEquals("Pending", rec.attributes().get("toState"));
  }

  @Test
  void userUpdateAcceptsExplicitSystemActor() {
    HttpServletRequest request = mockRequest("jdoe", "10.0.0.7");
    PSSystemAuditLogger.userUpdate(
        request, AuditOutcome.SUCCESS, "jdoe", "password re-encrypt", "system");

    var rec = ConcurrentMemoryAuditLogRepository.INSTANCE.findAll().get(0);
    assertEquals(3002, rec.code().numericCode());
    assertEquals("system", rec.actor().orElse(""));
    assertEquals("jdoe", rec.target().orElse(""));
  }

  @Test
  void nullRequestDoesNotThrow() {
    PSSystemAuditLogger.contentCreate(null, AuditOutcome.SUCCESS, "g", "1", "/p");
    PSSystemAuditLogger.userCreate(null, AuditOutcome.SUCCESS, "u");
    PSSystemAuditLogger.workflowTransition(null, AuditOutcome.SUCCESS, "1", "g", "a", "b");
    assertEquals(3, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
  }

  @Test
  void designUpdateWritesDesnUpdateCode() {
    PSSystemAuditLogger.designUpdate("designer", "CONTENT_LIST", "list-1", "guid-d1");

    assertEquals(1, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
    var rec = ConcurrentMemoryAuditLogRepository.INSTANCE.findAll().get(0);
    assertEquals(DesignErrorCodes.UPDATE, rec.code());
    assertEquals("DESN", rec.code().module().code());
    assertEquals(2902, rec.code().numericCode());
    assertEquals("designer", rec.actor().orElse(""));
    assertEquals("guid-d1", rec.target().orElse(""));
    assertTrue(rec.formattedLine().startsWith("[DESN-2902]-"));
  }

  @Test
  void designDeleteAndCreateWriteLifecycleCodes() {
    PSSystemAuditLogger.designCreate("u1", "TEMPLATE", "t1", "g-c");
    PSSystemAuditLogger.designDelete("u2", "TEMPLATE", "t1", "g-d");

    assertEquals(2, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
    var codes =
        ConcurrentMemoryAuditLogRepository.INSTANCE.findAll().stream()
            .map(r -> r.code().numericCode())
            .collect(java.util.stream.Collectors.toSet());
    assertTrue(codes.contains(2901));
    assertTrue(codes.contains(2903));
  }

  @Test
  void designBlankActorBecomesUnknown() {
    PSSystemAuditLogger.design(DesignErrorCodes.UPDATE, "  ", "SITE", "", "g-x");

    var rec = ConcurrentMemoryAuditLogRepository.INSTANCE.findAll().get(0);
    assertEquals("unknown", rec.actor().orElse(""));
  }

  @Test
  void designNullCodeIsNoOp() {
    PSSystemAuditLogger.design(null, "u", "t", "n", "g");
    assertEquals(0, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
  }

  private static HttpServletRequest mockRequest(String user, String ip) {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRemoteUser()).thenReturn(user);
    when(request.getRemoteAddr()).thenReturn(ip);
    when(request.getRemoteHost()).thenReturn("host.example");
    return request;
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
    assertEquals(
        SecurityErrorCodes.OS_IMPERSONATE_FAILURE.numericCode(),
        IPSSecurityErrors.OS_IMPERSONATE_FAILURE);
    assertEquals(
        SecurityErrorCodes.HOST_ADDR_FILTER_INVALID.numericCode(),
        IPSSecurityErrors.HOST_ADDR_FILTER_INVALID);
  }

  @Test
  void legacyFolderPermissionDeniedDualWrites() {
    var id =
        PSSystemAuditLogger.logLegacyIfAuditable(
            PathItemErrorCodes.FOLDER_PERMISSION_DENIED.numericCode(),
            AuditContext.builder().actor("editor").build());

    assertTrue(!id.value().equals(LegacyErrorCodeRegistry.SKIPPED.value()));
    assertEquals(1, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
    assertEquals(
        PathItemErrorCodes.FOLDER_PERMISSION_DENIED,
        ConcurrentMemoryAuditLogRepository.INSTANCE.findAll().get(0).code());
  }

  @Test
  void legacyDesignServerAclNoAdminDualWrites() {
    var id =
        PSSystemAuditLogger.logLegacyIfAuditable(
            DesignErrorCodes.SRV_ACL_NO_ADMIN.numericCode(),
            AuditContext.builder().actor("admin").build());

    assertTrue(!id.value().equals(LegacyErrorCodeRegistry.SKIPPED.value()));
    assertEquals(
        DesignErrorCodes.SRV_ACL_NO_ADMIN,
        ConcurrentMemoryAuditLogRepository.INSTANCE.findAll().get(0).code());
  }

  @Test
  void legacyOsMetaNoiseSkipsDualWrite() {
    var id =
        PSSystemAuditLogger.logLegacyIfAuditable(
            IPSSecurityErrors.OSMETA_GET_OBJECTS_FAILURE, AuditContext.empty());

    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    assertEquals(0, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
  }

  @Test
  void legacyContentConversionNoiseSkipsDualWrite() {
    var id =
        PSSystemAuditLogger.logLegacyIfAuditable(
            IPSContentErrors.UNSUPPORTED_FILE_TYPE,
            AuditContext.builder().actor("jdoe").build());

    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    assertEquals(0, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
  }

  @Test
  void legacyContentCreateDualWritesViaBridge() {
    var id =
        PSSystemAuditLogger.logLegacyIfAuditable(
            ContentErrorCodes.CREATE.numericCode(),
            AuditContext.builder().actor("jdoe").build(),
            "guid-1",
            "42",
            "/Sites/demo");

    assertTrue(!id.value().equals(LegacyErrorCodeRegistry.SKIPPED.value()));
    assertEquals(1, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
    var rec = ConcurrentMemoryAuditLogRepository.INSTANCE.findAll().get(0);
    assertEquals(ContentErrorCodes.CREATE, rec.code());
    assertTrue(rec.formattedLine().startsWith("[CONT-2001]-"));
  }

  @Test
  void legacyWorkflowAccessDeniedDualWritesViaBridge() {
    var id =
        PSSystemAuditLogger.logLegacyIfAuditable(
            IPSWorkflowErrors.ACCESS_DENIED,
            AuditContext.builder().actor("jdoe").build(),
            "5",
            "jdoe");

    assertTrue(!id.value().equals(LegacyErrorCodeRegistry.SKIPPED.value()));
    assertEquals(1, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
    var rec = ConcurrentMemoryAuditLogRepository.INSTANCE.findAll().get(0);
    assertEquals(WorkflowErrorCodes.ACCESS_DENIED, rec.code());
    assertTrue(rec.formattedLine().startsWith("[WF-6]-"));
  }

  @Test
  void legacyWorkflowNotFoundSkipsDualWrite() {
    var id =
        PSSystemAuditLogger.logLegacyIfAuditable(
            IPSWorkflowErrors.WORKFLOW_NOT_FOUND,
            AuditContext.builder().actor("jdoe").build(),
            "99");

    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    assertEquals(0, ConcurrentMemoryAuditLogRepository.INSTANCE.size());
  }

  @Test
  void ipsContentAndWorkflowErrorsIntsMatchCatalogs() {
    assertEquals(
        ContentErrorCodes.UNSUPPORTED_FILE_TYPE.numericCode(),
        IPSContentErrors.UNSUPPORTED_FILE_TYPE);
    assertEquals(
        ContentErrorCodes.UNSUPPORTED_CONVERT_CONSTRUCTOR.numericCode(),
        IPSContentErrors.UNSUPPORTED_CONVERT_CONSTRUCTOR);
    assertEquals(
        WorkflowErrorCodes.WORKFLOW_NOT_FOUND.numericCode(), IPSWorkflowErrors.WORKFLOW_NOT_FOUND);
    assertEquals(
        WorkflowErrorCodes.ACCESS_DENIED.numericCode(), IPSWorkflowErrors.ACCESS_DENIED);
    assertEquals(
        WorkflowErrorCodes.INVALID_TRANSITION.numericCode(), IPSWorkflowErrors.INVALID_TRANSITION);
  }
}
