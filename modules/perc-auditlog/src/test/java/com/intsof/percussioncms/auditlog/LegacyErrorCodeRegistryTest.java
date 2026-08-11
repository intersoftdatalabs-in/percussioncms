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
package com.intsof.percussioncms.auditlog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intsof.percussioncms.auditlog.codes.ContentErrorCodes;
import com.intsof.percussioncms.auditlog.codes.DesignErrorCodes;
import com.intsof.percussioncms.auditlog.codes.HttpErrorCodes;
import com.intsof.percussioncms.auditlog.codes.JobErrorCodes;
import com.intsof.percussioncms.auditlog.codes.PathItemErrorCodes;
import com.intsof.percussioncms.auditlog.codes.SecurityErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ServerErrorCodes;
import com.intsof.percussioncms.auditlog.codes.WorkflowErrorCodes;
import com.intsof.percussioncms.auditlog.sink.CapturingAuditLogSink;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LegacyErrorCodeRegistryTest {

  @BeforeEach
  void rebootstrap() {
    LegacyErrorCodeRegistry.clearForTests();
    LegacyErrorCodeRegistry.bootstrap();
  }

  @Test
  void authFailureLegacyCodeIsAuditableAndResolves() {
    assertTrue(LegacyErrorCodeRegistry.isAuditable(9002));
    assertSame(
        SecurityErrorCodes.AUTHENTICATION_FAILED,
        LegacyErrorCodeRegistry.find(9002).orElseThrow());
    assertTrue(SecurityErrorCodes.AUTHENTICATION_FAILED.isAuditable());
  }

  @Test
  void providerConfigLegacyCodeIsNotAuditable() {
    assertFalse(LegacyErrorCodeRegistry.isAuditable(9011));
    assertSame(
        SecurityErrorCodes.PROVIDER_UNKNOWN, LegacyErrorCodeRegistry.find(9011).orElseThrow());
    assertFalse(SecurityErrorCodes.PROVIDER_UNKNOWN.isAuditable());
  }

  @Test
  void unregisteredLegacyCodeIsNotAuditable() {
    assertFalse(LegacyErrorCodeRegistry.isAuditable(42));
    assertTrue(LegacyErrorCodeRegistry.find(42).isEmpty());
  }

  @Test
  void nonAuditableLegacyCodeSkipsDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            SecurityErrorCodes.PROVIDER_UNKNOWN.numericCode(),
            AuditContext.builder().actor("jdoe").build(),
            "Directory",
            "ldap1");

    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void auditableLegacyCodeDualWrites() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            SecurityErrorCodes.AUTHENTICATION_FAILED.numericCode(),
            AuditContext.builder().actor("jdoe").sourceIp("10.0.0.1").build(),
            "Directory",
            "ldap1",
            "jdoe");

    assertFalse(id.value().equals(LegacyErrorCodeRegistry.SKIPPED.value()));
    assertEquals(1, sink.records().size());
    assertEquals(SecurityErrorCodes.AUTHENTICATION_FAILED, sink.records().get(0).code());
    assertTrue(sink.records().get(0).formattedLine().startsWith("[SEC-9002]-"));
  }

  @Test
  void genericAuthFailedIsAuditable() {
    assertTrue(SecurityErrorCodes.GENERIC_AUTHENTICATION_FAILED.isAuditable());
    assertTrue(
        LegacyErrorCodeRegistry.isAuditable(
            SecurityErrorCodes.GENERIC_AUTHENTICATION_FAILED.numericCode()));
  }

  @Test
  void securityNotInitializedIsNotAuditable() {
    assertFalse(SecurityErrorCodes.SECURITY_NOT_INITIALIZED.isAuditable());
    assertFalse(
        LegacyErrorCodeRegistry.isAuditable(
            SecurityErrorCodes.SECURITY_NOT_INITIALIZED.numericCode()));
  }

  @Test
  void registryContainsAuthSecuritySlice() {
    assertTrue(LegacyErrorCodeRegistry.size() >= 30);
  }

  @Test
  void contentLifecycleLegacyCodeIsAuditableAndResolves() {
    assertTrue(LegacyErrorCodeRegistry.isAuditable(2001));
    assertSame(ContentErrorCodes.CREATE, LegacyErrorCodeRegistry.find(2001).orElseThrow());
    assertTrue(ContentErrorCodes.CREATE.isAuditable());
  }

  @Test
  void contentConversionLegacyCodeIsNotAuditable() {
    assertFalse(LegacyErrorCodeRegistry.isAuditable(17001));
    assertSame(
        ContentErrorCodes.UNSUPPORTED_FILE_TYPE,
        LegacyErrorCodeRegistry.find(17001).orElseThrow());
    assertFalse(ContentErrorCodes.UNSUPPORTED_FILE_TYPE.isAuditable());
  }

  @Test
  void contentConversionNonAuditableSkipsDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            ContentErrorCodes.UNSUPPORTED_MIMETYPE.numericCode(),
            AuditContext.builder().actor("jdoe").build());

    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void contentCreateAuditableDualWrites() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            ContentErrorCodes.CREATE.numericCode(),
            AuditContext.builder().actor("jdoe").build(),
            "guid-1",
            "42",
            "/Sites/demo");

    assertFalse(id.value().equals(LegacyErrorCodeRegistry.SKIPPED.value()));
    assertEquals(1, sink.records().size());
    assertEquals(ContentErrorCodes.CREATE, sink.records().get(0).code());
    assertTrue(sink.records().get(0).formattedLine().startsWith("[CONT-2001]-"));
  }

  @Test
  void workflowAccessDeniedIsAuditableAndResolves() {
    assertTrue(LegacyErrorCodeRegistry.isAuditable(6));
    assertSame(WorkflowErrorCodes.ACCESS_DENIED, LegacyErrorCodeRegistry.find(6).orElseThrow());
    assertTrue(WorkflowErrorCodes.ACCESS_DENIED.isAuditable());
  }

  @Test
  void workflowNotFoundIsNotAuditable() {
    assertFalse(LegacyErrorCodeRegistry.isAuditable(1));
    assertSame(
        WorkflowErrorCodes.WORKFLOW_NOT_FOUND, LegacyErrorCodeRegistry.find(1).orElseThrow());
    assertFalse(WorkflowErrorCodes.WORKFLOW_NOT_FOUND.isAuditable());
  }

  @Test
  void workflowNotFoundNonAuditableSkipsDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            WorkflowErrorCodes.WORKFLOW_NOT_FOUND.numericCode(),
            AuditContext.builder().actor("jdoe").build(),
            "99");

    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void workflowAccessDeniedDualWrites() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            WorkflowErrorCodes.ACCESS_DENIED.numericCode(),
            AuditContext.builder().actor("jdoe").build(),
            "5",
            "jdoe");

    assertFalse(id.value().equals(LegacyErrorCodeRegistry.SKIPPED.value()));
    assertEquals(1, sink.records().size());
    assertEquals(WorkflowErrorCodes.ACCESS_DENIED, sink.records().get(0).code());
    assertTrue(sink.records().get(0).formattedLine().startsWith("[WF-6]-"));
  }

  @Test
  void workflowTransitionHighLevelIsAuditable() {
    assertTrue(LegacyErrorCodeRegistry.isAuditable(4001));
    assertSame(WorkflowErrorCodes.TRANSITION, LegacyErrorCodeRegistry.find(4001).orElseThrow());
  }

  @Test
  void residualOsImpersonateIsAuditableAndResolves() {
    assertTrue(LegacyErrorCodeRegistry.isAuditable(9601));
    assertSame(
        SecurityErrorCodes.OS_IMPERSONATE_FAILURE,
        LegacyErrorCodeRegistry.find(9601).orElseThrow());
  }

  @Test
  void residualHostFilterIsNotAuditable() {
    assertFalse(LegacyErrorCodeRegistry.isAuditable(9501));
    assertSame(
        SecurityErrorCodes.HOST_ADDR_FILTER_INVALID,
        LegacyErrorCodeRegistry.find(9501).orElseThrow());
  }

  @Test
  void folderPermissionDeniedDualWrites() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            PathItemErrorCodes.FOLDER_PERMISSION_DENIED.numericCode(),
            AuditContext.builder().actor("jdoe").build());

    assertFalse(id.value().equals(LegacyErrorCodeRegistry.SKIPPED.value()));
    assertEquals(1, sink.records().size());
    assertEquals(PathItemErrorCodes.FOLDER_PERMISSION_DENIED, sink.records().get(0).code());
    assertTrue(sink.records().get(0).formattedLine().startsWith("[CONT-13007]-"));
  }

  @Test
  void invalidFolderIdSkipsDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            PathItemErrorCodes.INVALID_FOLDER_ID.numericCode(),
            AuditContext.builder().actor("jdoe").build(),
            "42");

    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void designAclNoAdminDualWrites() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            DesignErrorCodes.SRV_ACL_NO_ADMIN.numericCode(),
            AuditContext.builder().actor("admin").build());

    assertFalse(id.value().equals(LegacyErrorCodeRegistry.SKIPPED.value()));
    assertEquals(DesignErrorCodes.SRV_ACL_NO_ADMIN, sink.records().get(0).code());
    assertTrue(sink.records().get(0).formattedLine().startsWith("[DESN-2353]-"));
  }

  @Test
  void designAclEntryNullSkipsDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc, DesignErrorCodes.ACL_ENTRYLIST_NULL.numericCode(), AuditContext.empty());

    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void registryCoversContentWorkflowSecPathAndDesign() {
    // SEC residual + CONT + WF + path/item + design ACL (well beyond first-slice 30)
    assertTrue(LegacyErrorCodeRegistry.size() >= 70);
    assertTrue(LegacyErrorCodeRegistry.find(9903).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(13007).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(2353).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(2001).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(6).isPresent());
  }

  // --- Server / HTTP / Job residual (#2863) ---

  @Test
  void serverAuthFailureLegacyCodeIsAuditableAndResolves() {
    assertTrue(LegacyErrorCodeRegistry.isAuditable(1101));
    assertSame(
        ServerErrorCodes.AUTHORIZATION_ERROR, LegacyErrorCodeRegistry.find(1101).orElseThrow());
    assertTrue(ServerErrorCodes.AUTHORIZATION_ERROR.isAuditable());

    assertTrue(LegacyErrorCodeRegistry.isAuditable(1105));
    assertSame(
        ServerErrorCodes.TOO_MANY_LOGIN_ATTEMPTS, LegacyErrorCodeRegistry.find(1105).orElseThrow());
    assertTrue(LegacyErrorCodeRegistry.isAuditable(1247));
    assertSame(
        ServerErrorCodes.COMMUNITIES_AUTHENTICATION_FAILED_INVALID_COMMUNITY,
        LegacyErrorCodeRegistry.find(1247).orElseThrow());
  }

  @Test
  void serverOperationalNoiseIsRegisteredButNotAuditable() {
    assertFalse(LegacyErrorCodeRegistry.isAuditable(1001));
    assertSame(ServerErrorCodes.NATIVE_ERROR, LegacyErrorCodeRegistry.find(1001).orElseThrow());
    assertFalse(ServerErrorCodes.NATIVE_ERROR.isAuditable());
    assertFalse(LegacyErrorCodeRegistry.isAuditable(1308));
    assertSame(
        ServerErrorCodes.REQUEST_HANDLER_NOT_FOUND,
        LegacyErrorCodeRegistry.find(1308).orElseThrow());
  }

  @Test
  void nonAuditableServerCodeSkipsDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc, ServerErrorCodes.NATIVE_ERROR.numericCode(), AuditContext.empty(), "db", "err");

    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void auditableServerCodeDualWrites() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            ServerErrorCodes.AUTHORIZATION_ERROR.numericCode(),
            AuditContext.builder().actor("jdoe").build(),
            "sess1",
            "/Rhythmyx/app");

    assertFalse(id.value().equals(LegacyErrorCodeRegistry.SKIPPED.value()));
    assertEquals(1, sink.records().size());
    assertEquals(ServerErrorCodes.AUTHORIZATION_ERROR, sink.records().get(0).code());
  }

  @Test
  void httpStatusCodesAreRegisteredButNeverAuditable() {
    assertFalse(LegacyErrorCodeRegistry.isAuditable(401));
    assertSame(HttpErrorCodes.HTTP_UNAUTHORIZED, LegacyErrorCodeRegistry.find(401).orElseThrow());
    assertFalse(HttpErrorCodes.HTTP_UNAUTHORIZED.isAuditable());
    assertFalse(LegacyErrorCodeRegistry.isAuditable(403));
    assertSame(HttpErrorCodes.HTTP_FORBIDDEN, LegacyErrorCodeRegistry.find(403).orElseThrow());
    assertFalse(LegacyErrorCodeRegistry.isAuditable(500));
    assertSame(
        HttpErrorCodes.HTTP_INTERNAL_SERVER_ERROR, LegacyErrorCodeRegistry.find(500).orElseThrow());
  }

  @Test
  void nonAuditableHttpCodeSkipsDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc, HttpErrorCodes.HTTP_UNAUTHORIZED.numericCode(), AuditContext.empty());

    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void jobConfigFileNotFoundIsRegisteredNonAuditable() {
    assertFalse(LegacyErrorCodeRegistry.isAuditable(11));
    assertSame(
        JobErrorCodes.CONFIG_FILE_NOT_FOUND, LegacyErrorCodeRegistry.find(11).orElseThrow());
    assertFalse(JobErrorCodes.CONFIG_FILE_NOT_FOUND.isAuditable());
  }

  @Test
  void jobPackageLocalIntsDoNotClobberWorkflowInFlatRegistry() {
    // Job ints 1–10 intentionally not registered; Workflow owns bare 1–10.
    assertSame(WorkflowErrorCodes.ACCESS_DENIED, LegacyErrorCodeRegistry.find(6).orElseThrow());
    assertTrue(LegacyErrorCodeRegistry.isAuditable(6));
    assertSame(
        WorkflowErrorCodes.WORKFLOW_NOT_FOUND, LegacyErrorCodeRegistry.find(1).orElseThrow());
    assertEquals(1, JobErrorCodes.JOB_DEFINITION_NOT_FOUND.numericCode());
    assertFalse(JobErrorCodes.JOB_DEFINITION_NOT_FOUND.isAuditable());
  }

  @Test
  void nonAuditableJobCodeSkipsDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc, JobErrorCodes.CONFIG_FILE_NOT_FOUND.numericCode(), AuditContext.empty(), "cfg.xml");

    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void registryCoversServerHttpAndJob() {
    // prior catalogs + full IPSServerErrors (295) + IPSHttpErrors (37) + job int 11
    assertTrue(LegacyErrorCodeRegistry.size() >= 350);
    assertTrue(LegacyErrorCodeRegistry.find(1101).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(401).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(11).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(1001).isPresent());
  }
}

