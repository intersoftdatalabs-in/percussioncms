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

import com.intsof.percussioncms.auditlog.codes.AssemblyErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ContentErrorCodes;
import com.intsof.percussioncms.auditlog.codes.DeliveryErrorCodes;
import com.intsof.percussioncms.auditlog.codes.DesignErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ExtensionErrorCodes;
import com.intsof.percussioncms.auditlog.codes.HttpErrorCodes;
import com.intsof.percussioncms.auditlog.codes.JobErrorCodes;
import com.intsof.percussioncms.auditlog.codes.LocaleErrorCodes;
import com.intsof.percussioncms.auditlog.codes.LuceneErrorCodes;
import com.intsof.percussioncms.auditlog.codes.MailErrorCodes;
import com.intsof.percussioncms.auditlog.codes.PathItemErrorCodes;
import com.intsof.percussioncms.auditlog.codes.SearchErrorCodes;
import com.intsof.percussioncms.auditlog.codes.SecurityErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ServerErrorCodes;
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
    // Sentinel outside all cataloged ranges (webservice package-local now claims 42).
    assertFalse(LegacyErrorCodeRegistry.isAuditable(999_999));
    assertTrue(LegacyErrorCodeRegistry.find(999_999).isEmpty());
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
  void registryCoversServerHttpJobExtensionAssemblyAndWebservices() {
    // prior catalogs + server/http/job + extension + assembly non-colliding ints
    assertTrue(LegacyErrorCodeRegistry.size() >= 350);
    assertTrue(LegacyErrorCodeRegistry.find(1101).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(401).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(11).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(1001).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(7442).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(7007).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(12).isPresent()); // assembly MISSING_FINDER
    assertTrue(LegacyErrorCodeRegistry.find(27).isPresent()); // assembly INLINE_LINK_ERROR
    // webservice/webdav/servlet residual (#2865)
    assertTrue(LegacyErrorCodeRegistry.find(32).isPresent()); // webservice ACCESS_CONTROL
    assertTrue(LegacyErrorCodeRegistry.find(72).isPresent()); // webservice NOT_AUTHORIZED
    assertTrue(LegacyErrorCodeRegistry.find(14010).isPresent()); // server WS login
    assertTrue(LegacyErrorCodeRegistry.find(70101).isPresent()); // webdav
    assertTrue(LegacyErrorCodeRegistry.find(10151).isPresent()); // servlet
  }

  // --- Assembly / Extension / Delivery residual (#2864) ---

  @Test
  void extensionCheckoutNotAllowedIsAuditableAndResolves() {
    assertTrue(LegacyErrorCodeRegistry.isAuditable(7442));
    assertSame(
        ExtensionErrorCodes.CHECKOUT_NOT_ALLOWED,
        LegacyErrorCodeRegistry.find(7442).orElseThrow());
    assertTrue(ExtensionErrorCodes.CHECKOUT_NOT_ALLOWED.isAuditable());
  }

  @Test
  void extensionParamNoiseIsNotAuditable() {
    assertFalse(LegacyErrorCodeRegistry.isAuditable(7007));
    assertSame(
        ExtensionErrorCodes.EXT_PARAM_VALUE_INVALID,
        LegacyErrorCodeRegistry.find(7007).orElseThrow());
    assertFalse(ExtensionErrorCodes.EXT_PARAM_VALUE_INVALID.isAuditable());
  }

  @Test
  void extensionNonAuditableSkipsDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            ExtensionErrorCodes.EXT_PARAM_VALUE_INVALID.numericCode(),
            AuditContext.builder().actor("jdoe").build());

    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void extensionAuditableDualWrites() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            ExtensionErrorCodes.CHECKOUT_NOT_ALLOWED.numericCode(),
            AuditContext.builder().actor("jdoe").build(),
            "detail");

    assertFalse(id.value().equals(LegacyErrorCodeRegistry.SKIPPED.value()));
    assertEquals(1, sink.records().size());
    assertEquals(ExtensionErrorCodes.CHECKOUT_NOT_ALLOWED, sink.records().get(0).code());
    assertTrue(sink.records().get(0).formattedLine().startsWith("[SYS-7442]-"));
  }

  @Test
  void assemblyNonCollidingLegacyCodeIsRegisteredButNotAuditable() {
    // 12 is package-local MISSING_FINDER; non-colliding with WF 1–10 so flat-registered
    assertFalse(LegacyErrorCodeRegistry.isAuditable(12));
    assertSame(AssemblyErrorCodes.MISSING_FINDER, LegacyErrorCodeRegistry.find(12).orElseThrow());
    assertFalse(AssemblyErrorCodes.MISSING_FINDER.isAuditable());
  }

  @Test
  void assemblyNonAuditableSkipsDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            AssemblyErrorCodes.MISSING_FINDER.numericCode(),
            AuditContext.builder().actor("jdoe").build());

    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void assemblyPackageLocalOneRemainsWorkflowInFlatRegistry() {
    // Bare int 1 is WorkflowErrorCodes.WORKFLOW_NOT_FOUND, not Assembly TEMPLATE_MISSING
    assertSame(WorkflowErrorCodes.WORKFLOW_NOT_FOUND, LegacyErrorCodeRegistry.find(1).orElseThrow());
    assertEquals(1, AssemblyErrorCodes.TEMPLATE_MISSING.numericCode());
  }

  @Test
  void deliveryDecryptCredentialsNotFlatRegisteredButEnumIsAuditable() {
    // package-local 7 collides with WF; flat registry keeps WorkflowErrorCodes
    assertSame(WorkflowErrorCodes.TRANSITION_NOT_FOUND, LegacyErrorCodeRegistry.find(7).orElseThrow());
    assertTrue(DeliveryErrorCodes.COULD_NOT_DECRYPT_CREDENTIALS.isAuditable());
    assertEquals(7, DeliveryErrorCodes.COULD_NOT_DECRYPT_CREDENTIALS.numericCode());
  }


  // --- Webservices / WebDAV / Servlet residual (#2865) ---

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

  // --- Search / Lucene / Locale / Mail residual (#2880) ---

  @Test
  void searchAuthenticationFailedIsAuditableAndResolves() {
    assertTrue(LegacyErrorCodeRegistry.isAuditable(16052));
    assertSame(
        SearchErrorCodes.SEARCH_ENGINE_AUTHENTICATION_FAILED,
        LegacyErrorCodeRegistry.find(16052).orElseThrow());
    assertTrue(SearchErrorCodes.SEARCH_ENGINE_AUTHENTICATION_FAILED.isAuditable());
  }

  @Test
  void searchAuthenticationFailedDualWrites() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            SearchErrorCodes.SEARCH_ENGINE_AUTHENTICATION_FAILED.numericCode(),
            AuditContext.builder().actor("jdoe").build());

    assertFalse(id.value().equals(LegacyErrorCodeRegistry.SKIPPED.value()));
    assertEquals(1, sink.records().size());
    assertEquals(
        SearchErrorCodes.SEARCH_ENGINE_AUTHENTICATION_FAILED, sink.records().get(0).code());
    assertTrue(sink.records().get(0).formattedLine().startsWith("[SYS-16052]-"));
  }

  @Test
  void searchOperationalCodeIsRegisteredButNotAuditable() {
    assertFalse(LegacyErrorCodeRegistry.isAuditable(16001));
    assertSame(
        SearchErrorCodes.SEARCH_ENGINE_UNIMPLEMENTED_OPERATION,
        LegacyErrorCodeRegistry.find(16001).orElseThrow());
    assertFalse(SearchErrorCodes.SEARCH_ENGINE_UNIMPLEMENTED_OPERATION.isAuditable());
  }

  @Test
  void searchNonAuditableSkipsDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            SearchErrorCodes.SEARCH_ENGINE_FATAL_ERROR.numericCode(),
            AuditContext.builder().actor("jdoe").build());

    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void luceneIndexCodesAreRegisteredButNotAuditable() {
    assertFalse(LegacyErrorCodeRegistry.isAuditable(16311));
    assertSame(
        LuceneErrorCodes.INDEX_DIR_PARAM_INVALID_MISSING,
        LegacyErrorCodeRegistry.find(16311).orElseThrow());
    assertFalse(LegacyErrorCodeRegistry.isAuditable(16451));
    assertSame(
        LuceneErrorCodes.INDEX_CURRUPTED_EXCEPTION_SEARCHING,
        LegacyErrorCodeRegistry.find(16451).orElseThrow());
  }

  @Test
  void luceneNonAuditableSkipsDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            LuceneErrorCodes.SEARCH_QUERY_PARSEEXCEPTION.numericCode(),
            AuditContext.builder().actor("jdoe").build());

    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void localeCodesAreRegisteredButNotAuditable() {
    assertFalse(LegacyErrorCodeRegistry.isAuditable(1801));
    assertSame(
        LocaleErrorCodes.INVALID_COLUMN_VALUE, LegacyErrorCodeRegistry.find(1801).orElseThrow());
    assertFalse(LegacyErrorCodeRegistry.isAuditable(1804));
    assertSame(
        LocaleErrorCodes.LOCALE_MGR_UNEXPECTED_ERROR,
        LegacyErrorCodeRegistry.find(1804).orElseThrow());
  }

  @Test
  void localeNonAuditableSkipsDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            LocaleErrorCodes.LOCALE_MGR_INIT.numericCode(),
            AuditContext.empty(),
            "init failed");

    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void mailCodesAreRegisteredButNotAuditable() {
    assertFalse(LegacyErrorCodeRegistry.isAuditable(3501));
    assertSame(MailErrorCodes.MAIL_ADDRESS_EMPTY, LegacyErrorCodeRegistry.find(3501).orElseThrow());
    assertFalse(LegacyErrorCodeRegistry.isAuditable(3507));
    assertSame(
        MailErrorCodes.MAIL_SERVER_CONNECTION_ERROR,
        LegacyErrorCodeRegistry.find(3507).orElseThrow());
  }

  @Test
  void mailNonAuditableSkipsDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            MailErrorCodes.MAIL_ADDRESS_INVALID.numericCode(),
            AuditContext.empty(),
            "bad@");

    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void registryCoversSearchLuceneLocaleAndMail() {
    assertTrue(LegacyErrorCodeRegistry.size() >= 380);
    assertTrue(LegacyErrorCodeRegistry.find(16052).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(16311).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(1801).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(3501).isPresent());
  }

}
