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
import com.intsof.percussioncms.auditlog.codes.PathItemErrorCodes;
import com.intsof.percussioncms.auditlog.codes.SecurityErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ServerWebServicesErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ServletErrorCodes;
import com.intsof.percussioncms.auditlog.codes.TransformationErrorCodes;
import com.intsof.percussioncms.auditlog.codes.WebdavErrorCodes;
import com.intsof.percussioncms.auditlog.codes.WebserviceErrorCodes;
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
    // 99999 is outside every cataloged IPS*Errors range (WS 1–73 / 14001–14026, WebDAV 70001+, …)
    assertFalse(LegacyErrorCodeRegistry.isAuditable(99999));
    assertTrue(LegacyErrorCodeRegistry.find(99999).isEmpty());
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

  @Test
  void webserviceAccessControlIsAuditableAndResolves() {
    assertTrue(LegacyErrorCodeRegistry.isAuditable(32));
    assertSame(
        WebserviceErrorCodes.ACCESS_CONTROL_ERROR,
        LegacyErrorCodeRegistry.find(32).orElseThrow());
    assertTrue(WebserviceErrorCodes.ACCESS_CONTROL_ERROR.isAuditable());
  }

  @Test
  void webserviceNotAuthorizedDualWrites() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            WebserviceErrorCodes.NOT_AUTHORIZED.numericCode(),
            AuditContext.builder().actor("jdoe").build(),
            "jdoe",
            "save",
            "denied");

    assertFalse(id.value().equals(LegacyErrorCodeRegistry.SKIPPED.value()));
    assertEquals(1, sink.records().size());
    assertEquals(WebserviceErrorCodes.NOT_AUTHORIZED, sink.records().get(0).code());
    assertTrue(sink.records().get(0).formattedLine().startsWith("[SYS-72]-"));
  }

  @Test
  void webserviceSaveFailedIsNotAuditableButRegistered() {
    assertFalse(LegacyErrorCodeRegistry.isAuditable(28));
    assertSame(
        WebserviceErrorCodes.FAILED_SAVE_RELATIONSHIPS,
        LegacyErrorCodeRegistry.find(28).orElseThrow());
    assertFalse(WebserviceErrorCodes.FAILED_SAVE_RELATIONSHIPS.isAuditable());
  }

  @Test
  void webserviceSaveFailedNonAuditableSkipsDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            WebserviceErrorCodes.FAILED_SAVE_RELATIONSHIPS.numericCode(),
            AuditContext.builder().actor("jdoe").build(),
            "detail");

    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void webservicePackageLocalOneRemainsWorkflowInFlatRegistry() {
    // Bare int 1 is WorkflowErrorCodes.WORKFLOW_NOT_FOUND, not Webservice INVALID_CONTRACT
    assertSame(WorkflowErrorCodes.WORKFLOW_NOT_FOUND, LegacyErrorCodeRegistry.find(1).orElseThrow());
    assertEquals(1, WebserviceErrorCodes.INVALID_CONTRACT.numericCode());
    assertTrue(WebserviceErrorCodes.INVALID_SESSION.isAuditable());
    assertEquals(3, WebserviceErrorCodes.INVALID_SESSION.numericCode());
  }

  @Test
  void serverWebServicesLoginFailureIsAuditableAndResolves() {
    assertTrue(LegacyErrorCodeRegistry.isAuditable(14010));
    assertSame(
        ServerWebServicesErrorCodes.WEB_SERVICE_LOGIN_FAILURE,
        LegacyErrorCodeRegistry.find(14010).orElseThrow());
  }

  @Test
  void serverWebServicesLoginFailureDualWrites() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            ServerWebServicesErrorCodes.WEB_SERVICE_LOGIN_FAILURE.numericCode(),
            AuditContext.builder().actor("jdoe").build(),
            "jdoe",
            "****");

    assertFalse(id.value().equals(LegacyErrorCodeRegistry.SKIPPED.value()));
    assertEquals(
        ServerWebServicesErrorCodes.WEB_SERVICE_LOGIN_FAILURE, sink.records().get(0).code());
    assertTrue(sink.records().get(0).formattedLine().startsWith("[SYS-14010]-"));
  }

  @Test
  void serverWebServicesContentNotFoundSkipsDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            ServerWebServicesErrorCodes.WEB_SERVICE_CONTENT_ITEM_NOT_FOUND.numericCode(),
            AuditContext.builder().actor("jdoe").build(),
            "42",
            "1");

    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void webdavUnsupportedMethodIsRegisteredButNotAuditable() {
    assertFalse(LegacyErrorCodeRegistry.isAuditable(70101));
    assertSame(WebdavErrorCodes.UNSUPPORTED_METHOD, LegacyErrorCodeRegistry.find(70101).orElseThrow());
    assertFalse(WebdavErrorCodes.UNSUPPORTED_METHOD.isAuditable());
  }

  @Test
  void webdavNonAuditableSkipsDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            WebdavErrorCodes.UNSUPPORTED_METHOD.numericCode(),
            AuditContext.builder().actor("jdoe").build(),
            "PROPFIND");

    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void servletConnectionErrorIsRegisteredButNotAuditable() {
    assertFalse(LegacyErrorCodeRegistry.isAuditable(10151));
    assertSame(ServletErrorCodes.CONNECTION_ERROR, LegacyErrorCodeRegistry.find(10151).orElseThrow());
  }

  @Test
  void transformationNotFlatRegisteredButEnumIsNonAuditable() {
    assertSame(WorkflowErrorCodes.WORKFLOW_NOT_FOUND, LegacyErrorCodeRegistry.find(1).orElseThrow());
    assertFalse(TransformationErrorCodes.NO_CONVERTER_FOUND.isAuditable());
    assertEquals(1, TransformationErrorCodes.NO_CONVERTER_FOUND.numericCode());
  }

  @Test
  void registryCoversWebservicesWebdavAndServlet() {
    assertTrue(LegacyErrorCodeRegistry.size() >= 150);
    assertTrue(LegacyErrorCodeRegistry.find(32).isPresent()); // webservice ACCESS_CONTROL
    assertTrue(LegacyErrorCodeRegistry.find(72).isPresent()); // webservice NOT_AUTHORIZED
    assertTrue(LegacyErrorCodeRegistry.find(14010).isPresent()); // server WS login
    assertTrue(LegacyErrorCodeRegistry.find(70101).isPresent()); // webdav
    assertTrue(LegacyErrorCodeRegistry.find(10151).isPresent()); // servlet
  }
}
