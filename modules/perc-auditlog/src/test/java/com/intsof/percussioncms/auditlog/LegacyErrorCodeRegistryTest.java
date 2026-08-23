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

import com.intsof.percussioncms.auditlog.codes.AssemblyErrorCodes;
import com.intsof.percussioncms.auditlog.codes.BackEndErrorCodes;
import com.intsof.percussioncms.auditlog.codes.BeansErrorCodes;
import com.intsof.percussioncms.auditlog.codes.CatalogErrorCodes;
import com.intsof.percussioncms.auditlog.codes.CloneErrorCodes;
import com.intsof.percussioncms.auditlog.codes.CmsErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ConnectionErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ContentErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ContentExplorerErrorCodes;
import com.intsof.percussioncms.auditlog.codes.DataErrorCodes;
import com.intsof.percussioncms.auditlog.codes.DeliveryErrorCodes;
import com.intsof.percussioncms.auditlog.codes.DeploymentErrorCodes;
import com.intsof.percussioncms.auditlog.codes.DesignErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ExtensionErrorCodes;
import com.intsof.percussioncms.auditlog.codes.FilterServiceErrorCodes;
import com.intsof.percussioncms.auditlog.codes.HttpErrorCodes;
import com.intsof.percussioncms.auditlog.codes.JBossErrorCodes;
import com.intsof.percussioncms.auditlog.codes.JobErrorCodes;
import com.intsof.percussioncms.auditlog.codes.LocaleErrorCodes;
import com.intsof.percussioncms.auditlog.codes.LockErrorCodes;
import com.intsof.percussioncms.auditlog.codes.LuceneErrorCodes;
import com.intsof.percussioncms.auditlog.codes.MailErrorCodes;
import com.intsof.percussioncms.auditlog.codes.NavigationErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ObjectStoreErrorCodes;
import com.intsof.percussioncms.auditlog.codes.PathItemErrorCodes;
import com.intsof.percussioncms.auditlog.codes.PublisherErrorCodes;
import com.intsof.percussioncms.auditlog.codes.RemoteErrorCodes;
import com.intsof.percussioncms.auditlog.codes.SearchErrorCodes;
import com.intsof.percussioncms.auditlog.codes.SecurityErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ServerErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ServerWebServicesErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ServletErrorCodes;
import com.intsof.percussioncms.auditlog.codes.SiteManageErrorCodes;
import com.intsof.percussioncms.auditlog.codes.SiteManagerErrorCodes;
import com.intsof.percussioncms.auditlog.codes.SystemServiceErrorCodes;
import com.intsof.percussioncms.auditlog.codes.TableFactoryErrorCodes;
import com.intsof.percussioncms.auditlog.codes.TransformationErrorCodes;
import com.intsof.percussioncms.auditlog.codes.UiErrorCodes;
import com.intsof.percussioncms.auditlog.codes.UtilErrorCodes;
import com.intsof.percussioncms.auditlog.codes.WebdavErrorCodes;
import com.intsof.percussioncms.auditlog.codes.WebserviceErrorCodes;
import com.intsof.percussioncms.auditlog.codes.WorkflowErrorCodes;
import com.intsof.percussioncms.auditlog.codes.XmlErrorCodes;
import com.intsof.percussioncms.auditlog.sink.CapturingAuditLogSink;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

  @Test
  void dataQueryProcessingIsRegisteredButNotAuditable() {
    assertFalse(LegacyErrorCodeRegistry.isAuditable(5201));
    assertSame(
        DataErrorCodes.QUERY_PROCESSING_ERROR, LegacyErrorCodeRegistry.find(5201).orElseThrow());
    assertFalse(DataErrorCodes.QUERY_PROCESSING_ERROR.isAuditable());
  }

  @Test
  void dataNonAuditableSkipsDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            DataErrorCodes.QUERY_PROCESSING_ERROR.numericCode(),
            AuditContext.builder().actor("jdoe").build(),
            "sess1");

    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void backEndAuthorizationIsAuditableAndResolves() {
    assertTrue(LegacyErrorCodeRegistry.isAuditable(5001));
    assertSame(
        BackEndErrorCodes.AUTHORIZATION_ERROR, LegacyErrorCodeRegistry.find(5001).orElseThrow());
    assertTrue(BackEndErrorCodes.AUTHORIZATION_ERROR.isAuditable());
  }

  @Test
  void backEndAuthorizationDualWrites() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            BackEndErrorCodes.AUTHORIZATION_ERROR.numericCode(),
            AuditContext.builder().actor("jdoe").sourceIp("10.0.0.1").build(),
            "10.0.0.1",
            "jdoe",
            "jtds",
            "dbserver");

    assertFalse(id.value().equals(LegacyErrorCodeRegistry.SKIPPED.value()));
    assertEquals(1, sink.records().size());
    assertEquals(BackEndErrorCodes.AUTHORIZATION_ERROR, sink.records().get(0).code());
    assertTrue(sink.records().get(0).formattedLine().startsWith("[SYS-5001]-"));
  }

  @Test
  void backEndJdbcNoiseIsRegisteredButNotAuditable() {
    assertFalse(LegacyErrorCodeRegistry.isAuditable(5008));
    assertSame(
        BackEndErrorCodes.JDBC_DRIVER_LOAD_FAILED, LegacyErrorCodeRegistry.find(5008).orElseThrow());
    assertFalse(BackEndErrorCodes.JDBC_DRIVER_LOAD_FAILED.isAuditable());
  }

  @Test
  void backEndJdbcNonAuditableSkipsDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            BackEndErrorCodes.JDBC_DRIVER_LOAD_FAILED.numericCode(),
            AuditContext.builder().actor("jdoe").build(),
            "driver",
            "detail");

    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void connectionUnauthorizedIsAuditableAndResolves() {
    assertTrue(LegacyErrorCodeRegistry.isAuditable(3107));
    assertSame(ConnectionErrorCodes.UNAUTHORIZED, LegacyErrorCodeRegistry.find(3107).orElseThrow());
    assertTrue(ConnectionErrorCodes.UNAUTHORIZED.isAuditable());
  }

  @Test
  void connectionUnauthorizedDualWrites() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            ConnectionErrorCodes.UNAUTHORIZED.numericCode(),
            AuditContext.builder().actor("jdoe").build());

    assertFalse(id.value().equals(LegacyErrorCodeRegistry.SKIPPED.value()));
    assertEquals(1, sink.records().size());
    assertEquals(ConnectionErrorCodes.UNAUTHORIZED, sink.records().get(0).code());
    assertTrue(sink.records().get(0).formattedLine().startsWith("[SYS-3107]-"));
  }

  @Test
  void connectionPortInvalidIsRegisteredButNotAuditable() {
    // 3001 overlaps Phase-2a UserManagementErrorCodes.CREATE package-local int; USER codes are
    // not flat-registered, so connection owns the flat registry key.
    assertFalse(LegacyErrorCodeRegistry.isAuditable(3001));
    assertSame(
        ConnectionErrorCodes.PORT_NUMBER_INVALID, LegacyErrorCodeRegistry.find(3001).orElseThrow());
    assertFalse(ConnectionErrorCodes.PORT_NUMBER_INVALID.isAuditable());
  }

  @Test
  void connectionPortInvalidNonAuditableSkipsDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc, ConnectionErrorCodes.PORT_NUMBER_INVALID.numericCode(), AuditContext.empty(), "abc");

    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void cloneNotAuthenticatedIsAuditableAndResolves() {
    assertTrue(LegacyErrorCodeRegistry.isAuditable(17502));
    assertSame(
        CloneErrorCodes.NOT_AUTHENTICACATED, LegacyErrorCodeRegistry.find(17502).orElseThrow());
    assertTrue(CloneErrorCodes.NOT_AUTHENTICACATED.isAuditable());
  }

  @Test
  void cloneNotAuthorizedDualWrites() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            CloneErrorCodes.NOT_AUTHORIZED.numericCode(),
            AuditContext.builder().actor("jdoe").build(),
            "denied");

    assertFalse(id.value().equals(LegacyErrorCodeRegistry.SKIPPED.value()));
    assertEquals(1, sink.records().size());
    assertEquals(CloneErrorCodes.NOT_AUTHORIZED, sink.records().get(0).code());
    assertTrue(sink.records().get(0).formattedLine().startsWith("[SYS-17503]-"));
  }

  @Test
  void cloneInvalidSourceIdIsRegisteredButNotAuditable() {
    assertFalse(LegacyErrorCodeRegistry.isAuditable(17501));
    assertSame(
        CloneErrorCodes.INVALID_CLONESOURCEID, LegacyErrorCodeRegistry.find(17501).orElseThrow());
    assertFalse(CloneErrorCodes.INVALID_CLONESOURCEID.isAuditable());
  }

  @Test
  void cloneInvalidSourceIdNonAuditableSkipsDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            CloneErrorCodes.INVALID_CLONESOURCEID.numericCode(),
            AuditContext.builder().actor("jdoe").build(),
            "x",
            "parse");

    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void registryCoversDataBackEndConnectionAndClone() {
    // prior catalogs + data (108) + backend (60) + connection (19) + clone (6)
    assertTrue(LegacyErrorCodeRegistry.size() >= 540);
    assertTrue(LegacyErrorCodeRegistry.find(5201).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(5001).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(3001).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(3107).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(17502).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(17503).isPresent());
  }

  @Test
  void publisherPackageLocalNotFlatRegisteredButJobFailedIsAuditableViaEnum() {
    // 19 is assembly FINDER_ERROR; flat registry does not claim publisher JOB_FAILED
    assertSame(AssemblyErrorCodes.FINDER_ERROR, LegacyErrorCodeRegistry.find(19).orElseThrow());
    assertTrue(PublisherErrorCodes.JOB_FAILED.isAuditable());
    assertEquals(19, PublisherErrorCodes.JOB_FAILED.numericCode());
    assertTrue(PublisherErrorCodes.ITEM_PUBLISH_FAILED.isAuditable());
  }

  @Test
  void publisherAuditableViaServiceLogDualWrites() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        svc.log(
            PublisherErrorCodes.JOB_FAILED,
            AuditContext.builder().actor("jdoe").build(),
            "job-9",
            "boom");

    assertFalse(id.value().equals(LegacyErrorCodeRegistry.SKIPPED.value()));
    assertEquals(1, sink.records().size());
    assertEquals(PublisherErrorCodes.JOB_FAILED, sink.records().get(0).code());
    assertTrue(sink.records().get(0).formattedLine().startsWith("[PUB-19]-"));
  }

  @Test
  void siteManagerAndFilterPackageLocalRemainWorkflowInFlatRegistry() {
    assertSame(WorkflowErrorCodes.WORKFLOW_NOT_FOUND, LegacyErrorCodeRegistry.find(1).orElseThrow());
    assertEquals(1, SiteManagerErrorCodes.SITE_ID_NOT_EXIST.numericCode());
    assertFalse(SiteManagerErrorCodes.SITE_ID_NOT_EXIST.isAuditable());
    assertEquals(1, FilterServiceErrorCodes.FILTER_MISSING.numericCode());
    assertFalse(FilterServiceErrorCodes.FILTER_MISSING.isAuditable());
  }

  @Test
  void lockPermissionDeniedAuditableViaEnumNotFlatRegistered() {
    // bare 9 remains WorkflowErrorCodes.CONFIGURATION_ERROR in the flat map
    assertSame(
        WorkflowErrorCodes.CONFIGURATION_ERROR, LegacyErrorCodeRegistry.find(9).orElseThrow());
    assertTrue(LockErrorCodes.PERMISSION_DENIED.isAuditable());
    assertEquals(9, LockErrorCodes.PERMISSION_DENIED.numericCode());
  }

  @Test
  void lockPermissionDeniedDualWritesViaEnum() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        svc.log(
            LockErrorCodes.PERMISSION_DENIED, AuditContext.builder().actor("jdoe").build());

    assertFalse(id.value().equals(LegacyErrorCodeRegistry.SKIPPED.value()));
    assertEquals(LockErrorCodes.PERMISSION_DENIED, sink.records().get(0).code());
    assertTrue(sink.records().get(0).formattedLine().startsWith("[SYS-9]-"));
  }

  @Test
  void uiAccessDeniedAuditableViaEnumNotFlatRegistered() {
    assertSame(WorkflowErrorCodes.INVALID_TRANSITION, LegacyErrorCodeRegistry.find(8).orElseThrow());
    assertTrue(UiErrorCodes.ACCESS_DENIED.isAuditable());
    assertEquals(8, UiErrorCodes.ACCESS_DENIED.numericCode());
  }

  @Test
  void uiAccessDeniedDualWritesViaEnum() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        svc.log(
            UiErrorCodes.ACCESS_DENIED,
            AuditContext.builder().actor("jdoe").build(),
            "delete",
            "node-1");

    assertFalse(id.value().equals(LegacyErrorCodeRegistry.SKIPPED.value()));
    assertEquals(UiErrorCodes.ACCESS_DENIED, sink.records().get(0).code());
    assertTrue(sink.records().get(0).formattedLine().startsWith("[SYS-8]-"));
  }

  @Test
  void catalogDesignRangeIsRegisteredButNotAuditable() {
    assertFalse(LegacyErrorCodeRegistry.isAuditable(4101));
    assertSame(
        CatalogErrorCodes.REQD_PROP_NOT_SPECIFIED,
        LegacyErrorCodeRegistry.find(4101).orElseThrow());
    assertFalse(CatalogErrorCodes.REQD_PROP_NOT_SPECIFIED.isAuditable());
    assertFalse(LegacyErrorCodeRegistry.isAuditable(4310));
    assertSame(CatalogErrorCodes.CATALOG_ERROR, LegacyErrorCodeRegistry.find(4310).orElseThrow());
  }

  @Test
  void catalogServicePackageLocalRemainsWorkflowInFlatRegistry() {
    assertSame(WorkflowErrorCodes.WORKFLOW_NOT_FOUND, LegacyErrorCodeRegistry.find(1).orElseThrow());
    assertEquals(1, CatalogErrorCodes.SUMMARY_ERROR.numericCode());
  }

  @Test
  void catalogNonAuditableSkipsDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc, CatalogErrorCodes.REQD_PROP_NOT_SPECIFIED.numericCode(), AuditContext.empty(), "p");

    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void deploymentNonCollidingVersionCodeIsRegisteredButNotAuditable() {
    assertFalse(LegacyErrorCodeRegistry.isAuditable(74));
    assertSame(
        DeploymentErrorCodes.VERSION_LOWER_THAN_INSTALLED,
        LegacyErrorCodeRegistry.find(74).orElseThrow());
    assertFalse(DeploymentErrorCodes.VERSION_LOWER_THAN_INSTALLED.isAuditable());
    assertSame(
        DeploymentErrorCodes.WRONG_FORMAT_FOR_PAIRID_DEP_ID,
        LegacyErrorCodeRegistry.find(85).orElseThrow());
  }

  @Test
  void deploymentLockCodesNotFlatRegisteredButAuditableViaEnum() {
    // 46 is webservice FAILED_FIND_FOLDER_CHILDREN in the flat map
    assertSame(
        WebserviceErrorCodes.FAILED_FIND_FOLDER_CHILDREN,
        LegacyErrorCodeRegistry.find(46).orElseThrow());
    assertTrue(DeploymentErrorCodes.LOCK_ALREADY_HELD.isAuditable());
    assertEquals(46, DeploymentErrorCodes.LOCK_ALREADY_HELD.numericCode());
  }

  @Test
  void deploymentLockAlreadyHeldDualWritesViaEnum() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        svc.log(
            DeploymentErrorCodes.LOCK_ALREADY_HELD,
            AuditContext.builder().actor("jdoe").build());

    assertFalse(id.value().equals(LegacyErrorCodeRegistry.SKIPPED.value()));
    assertEquals(DeploymentErrorCodes.LOCK_ALREADY_HELD, sink.records().get(0).code());
    assertTrue(sink.records().get(0).formattedLine().startsWith("[SYS-46]-"));
  }

  @Test
  void deploymentCatalogClientLeftoverCodesSkipDualWriteViaEnum() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    DeploymentErrorCodes[] leftovers = {
      DeploymentErrorCodes.NULL_INPUT_DOC,
      DeploymentErrorCodes.UNEXPECTED_ERROR,
      DeploymentErrorCodes.INVALID_REQUEST_TYPE,
      DeploymentErrorCodes.CATALOG_REQD_PROP_NOT_SPECIFIED,
      DeploymentErrorCodes.CATALOG_INVALID_DIRECTORY_SPECIFIED,
      DeploymentErrorCodes.ARCHIVE_READ_ERROR,
      DeploymentErrorCodes.ARCHIVE_WRITE_ERROR,
      DeploymentErrorCodes.MISSING_ID_MAPPING,
      DeploymentErrorCodes.INCOMPLETE_ID_MAPPING,
      DeploymentErrorCodes.INVALID_ID_MAPPING_TARGET,
      DeploymentErrorCodes.INCOMPLETE_ID_TYPE_MAPPING,
      DeploymentErrorCodes.SERVER_RESPONSE_ELEMENT_MISSING,
      DeploymentErrorCodes.SERVER_RESPONSE_ELEMENT_INVALID,
      DeploymentErrorCodes.NOT_CONNECTED_ERROR,
      DeploymentErrorCodes.LOCK_NOT_RELEASED
    };
    for (DeploymentErrorCodes code : leftovers) {
      assertFalse(code.isAuditable(), code.name());
      AuditLogId id = svc.log(code, AuditContext.empty());
      assertEquals(LegacyErrorCodeRegistry.SKIPPED.value(), id.value(), code.name());
    }
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void deploymentServerHandlerLeftoverCodesSkipDualWriteViaEnum() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    DeploymentErrorCodes[] leftovers = {
      DeploymentErrorCodes.SERVER_REQUEST_MALFORMED,
      DeploymentErrorCodes.SERVER_OBJECT_NOT_FOUND,
      DeploymentErrorCodes.DEPENDENCY_HANDLER_INIT,
      DeploymentErrorCodes.DEPENDENCY_MGR_INIT,
      DeploymentErrorCodes.MISSING_DEPENDENCY_FILE,
      DeploymentErrorCodes.INVALID_DEPENDENCY_FILE,
      DeploymentErrorCodes.DEP_OBJECT_NOT_FOUND,
      DeploymentErrorCodes.NO_ROWS_TO_PROCESS,
      DeploymentErrorCodes.INCOMPLATE_ORDER_DEF,
      DeploymentErrorCodes.INVALID_NUM_CHILD_DEFS,
      DeploymentErrorCodes.CANNOT_FIND_PARENT_DEP_DEF,
      DeploymentErrorCodes.ARCHIVE_REF_FOUND,
      DeploymentErrorCodes.MAX_DEP_COUNT_EXCEEDED,
      DeploymentErrorCodes.MULTISERVER_MANAGER_DISABLED,
      DeploymentErrorCodes.PACKAGE_CREATED_ON_SYSTEM
    };
    for (DeploymentErrorCodes code : leftovers) {
      assertFalse(code.isAuditable(), code.name());
      AuditLogId id = svc.log(code, AuditContext.empty());
      assertEquals(LegacyErrorCodeRegistry.SKIPPED.value(), id.value(), code.name());
    }
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void jobLeftoverCodesSkipDualWriteViaEnum() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    JobErrorCodes[] leftovers = {
      JobErrorCodes.INVALID_JOB_DESCRIPTOR,
      JobErrorCodes.UNEXPECTED_ERROR,
      JobErrorCodes.CONFIG_FILE_NOT_FOUND
    };
    for (JobErrorCodes code : leftovers) {
      assertFalse(code.isAuditable(), code.name());
      AuditLogId id = svc.log(code, AuditContext.empty());
      assertEquals(LegacyErrorCodeRegistry.SKIPPED.value(), id.value(), code.name());
    }
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void navigationCodesAreRegisteredButNotAuditable() {
    assertFalse(LegacyErrorCodeRegistry.isAuditable(18001));
    assertSame(
        NavigationErrorCodes.NAVIGATION_SERVICE_FOLDER_ID_NOT_FOUND_FOR_PATH,
        LegacyErrorCodeRegistry.find(18001).orElseThrow());
    assertFalse(
        NavigationErrorCodes.NAVIGATION_SERVICE_FOLDER_ID_NOT_FOUND_FOR_PATH.isAuditable());
    assertSame(
        NavigationErrorCodes.NAVIGATION_SERVICE_CANNOT_FIND_NAVTREE_FOR_SITE,
        LegacyErrorCodeRegistry.find(18009).orElseThrow());
  }

  @Test
  void navigationNonAuditableSkipsDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            NavigationErrorCodes.NAVIGATION_SERVICE_FOLDER_ID_NOT_FOUND_FOR_PATH.numericCode(),
            AuditContext.builder().actor("jdoe").build(),
            "/Sites/x");

    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void registryCoversCatalogDeploymentAndNavigationResidual() {
    assertTrue(LegacyErrorCodeRegistry.size() >= 370);
    assertTrue(LegacyErrorCodeRegistry.find(4101).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(4311).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(74).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(85).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(18001).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(18009).isPresent());
    // WF ownership of package-local low ints preserved
    assertSame(WorkflowErrorCodes.ACCESS_DENIED, LegacyErrorCodeRegistry.find(6).orElseThrow());
  }

  @Test
  void utilCodesAreRegisteredButNotAuditable() {
    assertFalse(LegacyErrorCodeRegistry.isAuditable(10001));
    assertSame(
        UtilErrorCodes.BASE64_ENCODING_EXCEPTION,
        LegacyErrorCodeRegistry.find(10001).orElseThrow());
    assertFalse(LegacyErrorCodeRegistry.isAuditable(10203));
    assertSame(UtilErrorCodes.POST_DATA_ERROR, LegacyErrorCodeRegistry.find(10203).orElseThrow());
  }

  @Test
  void utilNonAuditableSkipsDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            UtilErrorCodes.BASE64_DECODING_EXCEPTION.numericCode(),
            AuditContext.empty(),
            "input",
            "detail");

    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void systemXmlCodesAreRegisteredButNotAuditable() {
    assertFalse(LegacyErrorCodeRegistry.isAuditable(6001));
    assertSame(XmlErrorCodes.RAW_XML_DUMP, LegacyErrorCodeRegistry.find(6001).orElseThrow());
    assertFalse(LegacyErrorCodeRegistry.isAuditable(6028));
    assertSame(
        XmlErrorCodes.DTD_ELEMENT_NOTFOUND_ERROR,
        LegacyErrorCodeRegistry.find(6028).orElseThrow());
  }

  @Test
  void systemXmlNonAuditableSkipsDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc, XmlErrorCodes.XML_PROCESSING_ERROR.numericCode(), AuditContext.empty(), "sess");

    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void utilsXmlPackageLocalOneRemainsWorkflowInFlatRegistry() {
    assertSame(WorkflowErrorCodes.WORKFLOW_NOT_FOUND, LegacyErrorCodeRegistry.find(1).orElseThrow());
    assertEquals(1, XmlErrorCodes.XML_ELEMENT_MISSING.numericCode());
    assertFalse(XmlErrorCodes.XML_ELEMENT_MISSING.isAuditable());
    assertEquals(6, XmlErrorCodes.XML_RESTORE_ERROR.numericCode());
    assertSame(WorkflowErrorCodes.ACCESS_DENIED, LegacyErrorCodeRegistry.find(6).orElseThrow());
  }

  @Test
  void beansNotFlatRegisteredButEnumIsNonAuditable() {
    assertSame(ServerErrorCodes.NATIVE_ERROR, LegacyErrorCodeRegistry.find(1001).orElseThrow());
    assertFalse(BeansErrorCodes.XML_PROCESSING_ERROR.isAuditable());
    assertEquals(1001, BeansErrorCodes.XML_PROCESSING_ERROR.numericCode());
  }

  @Test
  void tableFactoryNotFlatRegisteredButEnumIsNonAuditable() {
    // Server owns flat 1001/1101/1201/1301 ranges
    assertSame(ServerErrorCodes.NATIVE_ERROR, LegacyErrorCodeRegistry.find(1001).orElseThrow());
    assertSame(
        ServerErrorCodes.AUTHORIZATION_ERROR, LegacyErrorCodeRegistry.find(1101).orElseThrow());
    assertFalse(TableFactoryErrorCodes.XML_ELEMENT_NULL.isAuditable());
    assertEquals(1001, TableFactoryErrorCodes.XML_ELEMENT_NULL.numericCode());
    assertEquals(1310, TableFactoryErrorCodes.DATA_HANDLER_CLASS_NOT_FOUND.numericCode());
  }

  @Test
  void jbossNotFlatRegisteredButEnumIsNonAuditable() {
    assertSame(WorkflowErrorCodes.WORKFLOW_NOT_FOUND, LegacyErrorCodeRegistry.find(1).orElseThrow());
    assertFalse(JBossErrorCodes.APP_POLICY_ELEMENT_MISSING.isAuditable());
    assertEquals(1, JBossErrorCodes.APP_POLICY_ELEMENT_MISSING.numericCode());
  }

  @Test
  void siteManageCodesAreRegisteredButNotAuditable() {
    assertFalse(LegacyErrorCodeRegistry.isAuditable(18252));
    assertSame(
        SiteManageErrorCodes.SITE_MANAGE_SERVICE_DELETING_BAD_SITE_RECORD,
        LegacyErrorCodeRegistry.find(18252).orElseThrow());
    assertFalse(SiteManageErrorCodes.SITE_MANAGE_SERVICE_DELETING_BAD_SITE_RECORD.isAuditable());
  }

  @Test
  void siteManageNonAuditableSkipsDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            SiteManageErrorCodes.SITE_MANAGE_SERVICE_DELETING_BAD_SITE_RECORD.numericCode(),
            AuditContext.empty());

    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void registryCoversUtilXmlAndSiteManage() {
    assertTrue(LegacyErrorCodeRegistry.size() >= 390);
    assertTrue(LegacyErrorCodeRegistry.find(10001).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(6001).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(18252).isPresent());
  }

  @Test
  void objectStoreBatchACodesAreRegisteredButNotAuditable() {
    assertFalse(LegacyErrorCodeRegistry.isAuditable(2011));
    assertSame(
        ObjectStoreErrorCodes.XML_ELEMENT_NULL, LegacyErrorCodeRegistry.find(2011).orElseThrow());
    assertFalse(LegacyErrorCodeRegistry.isAuditable(2102));
    assertSame(ObjectStoreErrorCodes.CONN_OBJ_NULL, LegacyErrorCodeRegistry.find(2102).orElseThrow());
    assertFalse(LegacyErrorCodeRegistry.isAuditable(2260));
    assertSame(
        ObjectStoreErrorCodes.NOTIFIER_FROM_TOO_BIG,
        LegacyErrorCodeRegistry.find(2260).orElseThrow());
  }

  @Test
  void objectStoreNonAuditableSkipsDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            ObjectStoreErrorCodes.XML_ELEMENT_WRONG_TYPE.numericCode(),
            AuditContext.empty(),
            "expected",
            "actual");

    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void designOwnedObjectStoreAclIntsRemainDesignErrorCodes() {
    // ObjectStore batch A must not steal Design ACL flat ownership (auditable dual-write path).
    assertSame(
        DesignErrorCodes.APP_ACL_NO_MANAGER, LegacyErrorCodeRegistry.find(2203).orElseThrow());
    assertSame(DesignErrorCodes.SRV_ACL_NO_ADMIN, LegacyErrorCodeRegistry.find(2353).orElseThrow());
    assertTrue(LegacyErrorCodeRegistry.isAuditable(2353));
    assertSame(
        DesignErrorCodes.ACL_ENTRYLIST_NULL, LegacyErrorCodeRegistry.find(2201).orElseThrow());
  }

  @Test
  void registryCoversObjectStoreBatchA() {
    // Prior catalogs (~380+) plus ObjectStore batch A (63 non-colliding ints).
    assertTrue(LegacyErrorCodeRegistry.size() >= 440);
    assertTrue(LegacyErrorCodeRegistry.find(2011).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(2209).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(2260).isPresent());
  }

  @Test
  void objectStoreBatchBCodesAreRegisteredButNotAuditable() {
    assertFalse(LegacyErrorCodeRegistry.isAuditable(2261));
    assertSame(
        ObjectStoreErrorCodes.ROLE_NAME_EMPTY, LegacyErrorCodeRegistry.find(2261).orElseThrow());
    assertFalse(LegacyErrorCodeRegistry.isAuditable(2309));
    assertSame(
        ObjectStoreErrorCodes.JDBC_DRIVER_CLASS_LOAD_ERROR,
        LegacyErrorCodeRegistry.find(2309).orElseThrow());
    assertFalse(LegacyErrorCodeRegistry.isAuditable(2320));
    assertSame(
        ObjectStoreErrorCodes.UPDATEPIPE_NO_SYNC_TYPES,
        LegacyErrorCodeRegistry.find(2320).orElseThrow());
  }

  @Test
  void objectStoreBatchBNonAuditableSkipsDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            ObjectStoreErrorCodes.ROLESET_PROVIDER_TYPE_INVALID.numericCode(),
            AuditContext.empty(),
            "badProvider");

    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void registryCoversObjectStoreBatchAAndB() {
    // Prior catalogs (~380+) plus ObjectStore A+B (123 non-colliding ints).
    assertTrue(LegacyErrorCodeRegistry.size() >= 500);
    assertTrue(LegacyErrorCodeRegistry.find(2011).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(2209).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(2260).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(2261).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(2320).isPresent());
  }

  @Test
  void residualCmsCodeIsRegisteredButNotAuditable() {
    assertFalse(LegacyErrorCodeRegistry.isAuditable(13001));
    assertSame(
        CmsErrorCodes.CORRUPT_DATABASE_ENTRY, LegacyErrorCodeRegistry.find(13001).orElseThrow());
    assertFalse(CmsErrorCodes.CORRUPT_DATABASE_ENTRY.isAuditable());
    assertFalse(LegacyErrorCodeRegistry.isAuditable(13250));
    assertSame(
        CmsErrorCodes.FAILED_GET_NAVON_CIRCULAR_AA_RELATIONSHIP,
        LegacyErrorCodeRegistry.find(13250).orElseThrow());
  }

  @Test
  void residualCmsNonAuditableSkipsDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            CmsErrorCodes.CMS_INTERNAL_REQUEST_ERROR.numericCode(),
            AuditContext.builder().actor("jdoe").build(),
            "app/resource",
            "boom");

    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void pathItemStillOwnsFolderPermissionDeniedAfterCmsResidual() {
    // PathItem registered before CmsErrorCodes; residual CMS skips 13007.
    assertSame(
        PathItemErrorCodes.FOLDER_PERMISSION_DENIED,
        LegacyErrorCodeRegistry.find(13007).orElseThrow());
    assertTrue(LegacyErrorCodeRegistry.isAuditable(13007));
  }

  @Test
  void contentExplorerCodesAreRegisteredButNotAuditable() {
    assertFalse(LegacyErrorCodeRegistry.isAuditable(20001));
    assertSame(
        ContentExplorerErrorCodes.GENERAL_ERROR, LegacyErrorCodeRegistry.find(20001).orElseThrow());
    assertFalse(LegacyErrorCodeRegistry.isAuditable(20011));
    assertSame(
        ContentExplorerErrorCodes.SITEDEF_UPDATE_FAILURES,
        LegacyErrorCodeRegistry.find(20011).orElseThrow());
  }

  @Test
  void contentExplorerNonAuditableSkipsDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            ContentExplorerErrorCodes.SEARCH_ERROR.numericCode(),
            AuditContext.builder().actor("jdoe").build(),
            "search failed");

    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void remoteCodesAreRegisteredButNotAuditable() {
    assertFalse(LegacyErrorCodeRegistry.isAuditable(15001));
    assertSame(
        RemoteErrorCodes.REMOTE_WRONG_SOAP_RESP, LegacyErrorCodeRegistry.find(15001).orElseThrow());
    assertFalse(LegacyErrorCodeRegistry.isAuditable(15002));
    assertSame(
        RemoteErrorCodes.REMOTE_UNEXPECTED_ERROR,
        LegacyErrorCodeRegistry.find(15002).orElseThrow());
  }

  @Test
  void remoteNonAuditableSkipsDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            RemoteErrorCodes.REMOTE_UNEXPECTED_ERROR.numericCode(),
            AuditContext.empty(),
            "timeout");

    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void systemServicePackageLocalDoesNotClobberWorkflowInFlatRegistry() {
    // SystemService ints 1 and 4 intentionally not registered; Workflow owns bare 1–10.
    assertSame(
        WorkflowErrorCodes.WORKFLOW_NOT_FOUND, LegacyErrorCodeRegistry.find(1).orElseThrow());
    assertEquals(1, SystemServiceErrorCodes.MISSING_SHARED_PROPERTY.numericCode());
    assertFalse(SystemServiceErrorCodes.MISSING_SHARED_PROPERTY.isAuditable());
    assertEquals(4, SystemServiceErrorCodes.ERROR_DETERMINING_FOLDER_READ.numericCode());
    assertFalse(SystemServiceErrorCodes.ERROR_DETERMINING_FOLDER_READ.isAuditable());
  }

  @Test
  void registryCoversCmsContentExplorerRemoteAndSystemService() {
    assertTrue(LegacyErrorCodeRegistry.size() >= 470);
    assertTrue(LegacyErrorCodeRegistry.find(13001).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(13007).isPresent()); // PathItem
    assertTrue(LegacyErrorCodeRegistry.find(15001).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(20001).isPresent());
    // SystemService not flat-registered:
    assertSame(
        WorkflowErrorCodes.WORKFLOW_NOT_FOUND, LegacyErrorCodeRegistry.find(1).orElseThrow());
  }

  @Test
  void objectStoreBatchCCodesAreRegisteredButNotAuditable() {
    assertFalse(LegacyErrorCodeRegistry.isAuditable(2321));
    assertSame(
        ObjectStoreErrorCodes.COND_VALUE_NULL, LegacyErrorCodeRegistry.find(2321).orElseThrow());
    assertFalse(LegacyErrorCodeRegistry.isAuditable(2350));
    assertSame(
        ObjectStoreErrorCodes.SRV_ROOT_TOO_BIG, LegacyErrorCodeRegistry.find(2350).orElseThrow());
    assertFalse(LegacyErrorCodeRegistry.isAuditable(2377));
    assertSame(
        ObjectStoreErrorCodes.NO_JNDI_DATASOURCE, LegacyErrorCodeRegistry.find(2377).orElseThrow());
    assertFalse(LegacyErrorCodeRegistry.isAuditable(2380));
    assertSame(
        ObjectStoreErrorCodes.APP_LOGIN_PAGE_NOT_SUPPORTED,
        LegacyErrorCodeRegistry.find(2380).orElseThrow());
  }

  @Test
  void objectStoreBatchCNonAuditableSkipsDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            ObjectStoreErrorCodes.NO_DATASOURCE_CONNECTION.numericCode(),
            AuditContext.empty(),
            "jndi",
            "db",
            "origin");

    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void objectStoreBatchDCodesAreRegisteredButNotAuditable() {
    assertFalse(LegacyErrorCodeRegistry.isAuditable(2401));
    assertSame(
        ObjectStoreErrorCodes.FIELD_NAME_NOT_UNIQUE,
        LegacyErrorCodeRegistry.find(2401).orElseThrow());
    assertFalse(LegacyErrorCodeRegistry.isAuditable(2450));
    assertSame(
        ObjectStoreErrorCodes.SYSTEM_TABLE_NOT_FOUND,
        LegacyErrorCodeRegistry.find(2450).orElseThrow());
    assertFalse(LegacyErrorCodeRegistry.isAuditable(2466));
    assertSame(
        ObjectStoreErrorCodes.CE_DUPLICATE_MERGED_FIELD_NAME,
        LegacyErrorCodeRegistry.find(2466).orElseThrow());
    assertFalse(LegacyErrorCodeRegistry.isAuditable(2475));
    assertSame(
        ObjectStoreErrorCodes.CHOICE_FILTER_DEPENDENT_FIELD_MISSING_ATTR,
        LegacyErrorCodeRegistry.find(2475).orElseThrow());
  }

  @Test
  void objectStoreBatchDNonAuditableSkipsDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            ObjectStoreErrorCodes.CE_DUPLICATE_MERGED_FIELD_NAME.numericCode(),
            AuditContext.empty(),
            "fieldName");

    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void registryCoversObjectStoreBatchAThroughC() {
    // Prior catalogs (~380+) plus ObjectStore A+B+C (157 non-colliding ints).
    assertTrue(LegacyErrorCodeRegistry.size() >= 530);
    assertTrue(LegacyErrorCodeRegistry.find(2011).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(2261).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(2320).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(2321).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(2350).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(2357).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(2380).isPresent());
    // Design still owns 2327 / 2351-2356
    assertSame(DesignErrorCodes.ACL_TYPE_INVALID, LegacyErrorCodeRegistry.find(2327).orElseThrow());
    assertSame(DesignErrorCodes.SRV_ACL_NO_ADMIN, LegacyErrorCodeRegistry.find(2353).orElseThrow());
  }

  @Test
  void registryCoversObjectStoreBatchAThroughD() {
    // Prior catalogs (~380+) plus ObjectStore A+B+C+D (232 non-colliding ints).
    assertTrue(LegacyErrorCodeRegistry.size() >= 600);
    assertTrue(LegacyErrorCodeRegistry.find(2011).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(2380).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(2401).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(2450).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(2475).isPresent());
    // Design still owns ACL ints
    assertSame(DesignErrorCodes.ACL_TYPE_INVALID, LegacyErrorCodeRegistry.find(2327).orElseThrow());
    assertSame(DesignErrorCodes.SRV_ACL_NO_ADMIN, LegacyErrorCodeRegistry.find(2353).orElseThrow());
  }

  @Test
  void objectStoreBatchECodesAreRegisteredButNotAuditable() {
    assertFalse(LegacyErrorCodeRegistry.isAuditable(2801));
    assertSame(
        ObjectStoreErrorCodes.METHOD_NOT_SUPPORTED,
        LegacyErrorCodeRegistry.find(2801).orElseThrow());
    assertFalse(LegacyErrorCodeRegistry.isAuditable(2812));
    assertSame(
        ObjectStoreErrorCodes.LOCK_ALREADY_HELD, LegacyErrorCodeRegistry.find(2812).orElseThrow());
    assertFalse(LegacyErrorCodeRegistry.isAuditable(2826));
    assertSame(
        ObjectStoreErrorCodes.HANDLER_IO_ERROR, LegacyErrorCodeRegistry.find(2826).orElseThrow());
    assertFalse(LegacyErrorCodeRegistry.isAuditable(2848));
    assertSame(
        ObjectStoreErrorCodes.LOOKUP_TABLE_INFO_NULL,
        LegacyErrorCodeRegistry.find(2848).orElseThrow());
  }

  @Test
  void objectStoreBatchENonAuditableSkipsDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            ObjectStoreErrorCodes.HANDLER_UNEXPECTED_EXCEPTION.numericCode(),
            AuditContext.empty(),
            "detail");

    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void registryCoversObjectStoreBatchAThroughE() {
    // Prior catalogs (~380+) plus ObjectStore A+B+C+D+E (280 non-colliding ints).
    assertTrue(LegacyErrorCodeRegistry.size() >= 650);
    assertTrue(LegacyErrorCodeRegistry.find(2011).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(2475).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(2801).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(2812).isPresent());
    assertTrue(LegacyErrorCodeRegistry.find(2848).isPresent());
    // Design still owns ACL ints
    assertSame(DesignErrorCodes.ACL_TYPE_INVALID, LegacyErrorCodeRegistry.find(2327).orElseThrow());
    assertSame(DesignErrorCodes.SRV_ACL_NO_ADMIN, LegacyErrorCodeRegistry.find(2353).orElseThrow());
  }
}
