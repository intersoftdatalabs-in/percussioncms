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

import com.intsof.percussioncms.auditlog.codes.SecurityErrorCodes;
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
}
