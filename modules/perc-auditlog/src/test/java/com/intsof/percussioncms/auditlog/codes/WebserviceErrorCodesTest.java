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
package com.intsof.percussioncms.auditlog.codes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intsof.percussioncms.auditlog.AuditContext;
import com.intsof.percussioncms.auditlog.AuditEventType;
import com.intsof.percussioncms.auditlog.AuditLogId;
import com.intsof.percussioncms.auditlog.AuditModule;
import com.intsof.percussioncms.auditlog.DefaultAuditLogService;
import com.intsof.percussioncms.auditlog.LegacyErrorCodeRegistry;
import com.intsof.percussioncms.auditlog.sink.CapturingAuditLogSink;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WebserviceErrorCodesTest {

  @Test
  void everyConstantHasExplicitModuleSysAndUniqueNumericCode() {
    Set<Integer> seen = new HashSet<>();
    for (WebserviceErrorCodes code : WebserviceErrorCodes.values()) {
      assertEquals(AuditModule.SYS, code.module());
      assertTrue(code.numericCode() > 0, code.name());
      assertTrue(seen.add(code.numericCode()), "duplicate numeric: " + code.numericCode());
      assertNotNull(code.userMessageTemplate());
      assertNotNull(code.logMessageTemplate());
      assertTrue(code.qualifiedCode().startsWith("SYS-"));
    }
    assertEquals(73, WebserviceErrorCodes.values().length);
  }

  @Test
  void auditableCodesRequireEventType() {
    for (WebserviceErrorCodes code : WebserviceErrorCodes.values()) {
      if (code.isAuditable()) {
        assertNotNull(code.eventType(), code.name() + " auditable without eventType");
      } else {
        assertNull(code.eventType(), code.name() + " non-auditable should not force eventType");
      }
    }
  }

  @Test
  void sessionAndAccessFailuresAreAuditable() {
    assertTrue(WebserviceErrorCodes.INVALID_SESSION.isAuditable());
    assertEquals(AuditEventType.AUTH_FAILURE, WebserviceErrorCodes.INVALID_SESSION.eventType());
    assertTrue(WebserviceErrorCodes.MISSING_SESSION.isAuditable());
    assertTrue(WebserviceErrorCodes.ACCESS_CONTROL_ERROR.isAuditable());
    assertEquals(
        AuditEventType.ACCESS_DENIED, WebserviceErrorCodes.ACCESS_CONTROL_ERROR.eventType());
    assertTrue(WebserviceErrorCodes.NOT_AUTHORIZED.isAuditable());
    assertTrue(WebserviceErrorCodes.ITEM_NOT_CHECKED_OUT.isAuditable());
    assertTrue(WebserviceErrorCodes.USER_NOT_MEMBER_COMMUNITY.isAuditable());
  }

  @Test
  void operationalDesignCrudIsNotAuditable() {
    assertFalse(WebserviceErrorCodes.INVALID_CONTRACT.isAuditable());
    assertFalse(WebserviceErrorCodes.OBJECT_NOT_FOUND.isAuditable());
    assertFalse(WebserviceErrorCodes.SAVE_FAILED.isAuditable());
    assertFalse(WebserviceErrorCodes.DELETE_FAILED.isAuditable());
    assertFalse(WebserviceErrorCodes.UNEXPECTED_ERROR.isAuditable());
  }

  @Test
  void preservesLegacyIpsWebserviceErrorsNumericValues() {
    assertEquals(1, WebserviceErrorCodes.INVALID_CONTRACT.numericCode());
    assertEquals(3, WebserviceErrorCodes.INVALID_SESSION.numericCode());
    assertEquals(32, WebserviceErrorCodes.ACCESS_CONTROL_ERROR.numericCode());
    assertEquals(72, WebserviceErrorCodes.NOT_AUTHORIZED.numericCode());
    assertEquals(73, WebserviceErrorCodes.FAILED_TO_OBTAIN_PATH_FROM_OBJECT_ID.numericCode());
  }

  @Test
  void collidingSessionCodesDualWriteViaEnumNotFlatInt() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId invalid =
        svc.log(
            WebserviceErrorCodes.INVALID_SESSION,
            AuditContext.builder().actor("jdoe").build(),
            "sid");
    assertFalse(invalid.value().equals(LegacyErrorCodeRegistry.SKIPPED.value()));
    assertEquals(WebserviceErrorCodes.INVALID_SESSION, sink.records().get(0).code());
    assertTrue(sink.records().get(0).formattedLine().startsWith("[SYS-3]-"));

    AuditLogId missing =
        svc.log(
            WebserviceErrorCodes.MISSING_SESSION,
            AuditContext.builder().actor("jdoe").build());
    assertFalse(missing.value().equals(LegacyErrorCodeRegistry.SKIPPED.value()));
    assertEquals(WebserviceErrorCodes.MISSING_SESSION, sink.records().get(1).code());
  }

  @Test
  void accessControlDualWritesViaRegisteredInt() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc,
            WebserviceErrorCodes.ACCESS_CONTROL_ERROR.numericCode(),
            AuditContext.builder().actor("jdoe").build(),
            "guid",
            "READ");

    assertFalse(id.value().equals(LegacyErrorCodeRegistry.SKIPPED.value()));
    assertSame(WebserviceErrorCodes.ACCESS_CONTROL_ERROR, sink.records().get(0).code());
  }

  @Test
  void operationalSaveFailedSkipsDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    AuditLogId id =
        svc.log(
            WebserviceErrorCodes.SAVE_FAILED,
            AuditContext.builder().actor("jdoe").build(),
            "obj");

    assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    assertTrue(sink.records().isEmpty());
  }
}
