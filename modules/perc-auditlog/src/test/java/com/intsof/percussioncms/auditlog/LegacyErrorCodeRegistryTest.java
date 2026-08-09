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
import com.intsof.percussioncms.auditlog.codes.SecurityErrorCodes;
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
    assertSame(
        ContentErrorCodes.CREATE, LegacyErrorCodeRegistry.find(2001).orElseThrow());
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
    assertSame(
        WorkflowErrorCodes.ACCESS_DENIED, LegacyErrorCodeRegistry.find(6).orElseThrow());
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
    assertSame(
        WorkflowErrorCodes.TRANSITION, LegacyErrorCodeRegistry.find(4001).orElseThrow());
  }

  @Test
  void registryContainsContentAndWorkflowSlice() {
    // SEC (~30) + CONT (16) + WF (11) — allow headroom for residual growth
    assertTrue(LegacyErrorCodeRegistry.size() >= 50);
  }
}
