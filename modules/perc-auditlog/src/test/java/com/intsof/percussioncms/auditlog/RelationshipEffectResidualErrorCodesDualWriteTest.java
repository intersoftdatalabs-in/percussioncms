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

import com.intsof.percussioncms.auditlog.codes.CmsErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ExtensionErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ServerErrorCodes;
import com.intsof.percussioncms.auditlog.sink.CapturingAuditLogSink;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Dual-write skip coverage for leftover relationship-effect catalog codes (#4156 / parent #2616).
 * Leftovers listed here are non-auditable operational / validation noise. Nearby auditable
 * extension authz codes keep dual-write.
 */
class RelationshipEffectResidualErrorCodesDualWriteTest {

  @BeforeEach
  void rebootstrap() {
    LegacyErrorCodeRegistry.clearForTests();
    LegacyErrorCodeRegistry.bootstrap();
  }

  @Test
  void leftoverRelationshipEffectCodesSkipDualWrite() {
    List<ExtensionErrorCodes> leftovers =
        List.of(
            ExtensionErrorCodes.EXT_MISSING_HTML_PARAMETER_ERROR,
            ExtensionErrorCodes.ILLEGAL_EXECUTION_CONTEXT,
            ExtensionErrorCodes.NONPROMOTABLE_RELATIONSHIP,
            ExtensionErrorCodes.WORKFLOWID_IN_REQUEST_ISNULL,
            ExtensionErrorCodes.INVALID_WORKFLOW_ACTION,
            ExtensionErrorCodes.ITEM_NOT_IN_PUBLIC_STATE,
            ExtensionErrorCodes.EFFECT_SELF_TRIGGERED,
            ExtensionErrorCodes.INVALID_OPTION_FOR_FORCETRANSITION,
            ExtensionErrorCodes.INVALID_TRANSITION_FOR_EFFECT,
            ExtensionErrorCodes.MISSING_INTERNAL_REQUEST_RESOURCE,
            ExtensionErrorCodes.DEPENDENT_ITEM_NOT_IN_DESIRED_STATE,
            ExtensionErrorCodes.DEPENDENT_ITEM_CANNOT_GOTO_DESIRED_STATE,
            ExtensionErrorCodes.EFFECT_VALIDATE_MESSAGE,
            ExtensionErrorCodes.PROMOTE_TRANSITION_FAILED,
            ExtensionErrorCodes.MANDATORY_TRANSITION_VALIDATION_FAILURE);

    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    for (ExtensionErrorCodes code : leftovers) {
      assertFalse(code.isAuditable(), code.name());
      assertSame(code, LegacyErrorCodeRegistry.find(code.numericCode()).orElseThrow());
      AuditLogId id =
          LegacyErrorCodeRegistry.logIfAuditable(
              svc, code.numericCode(), AuditContext.builder().actor("jdoe").build());
      assertEquals(LegacyErrorCodeRegistry.SKIPPED, id, code.name());
    }
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void leftoverCmsAndServerCodesOnSameFilesSkipDualWrite() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    assertFalse(CmsErrorCodes.UNEXPECTED_ERROR.isAuditable());
    assertFalse(CmsErrorCodes.UNDEFINED_DEFAULT_TRANSITION.isAuditable());
    assertFalse(ServerErrorCodes.MISSING_INTERNAL_REQUEST_RESOURCE.isAuditable());
    assertFalse(ServerErrorCodes.RAW_DUMP.isAuditable());

    for (int numeric :
        new int[] {
          CmsErrorCodes.UNEXPECTED_ERROR.numericCode(),
          CmsErrorCodes.UNDEFINED_DEFAULT_TRANSITION.numericCode(),
          ServerErrorCodes.MISSING_INTERNAL_REQUEST_RESOURCE.numericCode(),
          ServerErrorCodes.RAW_DUMP.numericCode()
        }) {
      AuditLogId id =
          LegacyErrorCodeRegistry.logIfAuditable(
              svc, numeric, AuditContext.builder().actor("jdoe").build());
      assertEquals(LegacyErrorCodeRegistry.SKIPPED, id);
    }
    assertTrue(sink.records().isEmpty());
  }

  @Test
  void leftoverAuditableExtensionAuthzStillDualWrites() {
    CapturingAuditLogSink sink = new CapturingAuditLogSink("cap");
    DefaultAuditLogService svc = DefaultAuditLogService.builder().addSink(sink).build();

    ExtensionErrorCodes auditable =
        ExtensionErrorCodes.AUTHENTICATION_FAILED_DIFFERENT_ITEM_USER_COMMUNITIES;
    assertTrue(auditable.isAuditable());
    AuditLogId id =
        LegacyErrorCodeRegistry.logIfAuditable(
            svc, auditable.numericCode(), AuditContext.builder().actor("jdoe").build());
    assertFalse(id.value().equals(LegacyErrorCodeRegistry.SKIPPED.value()));
    assertEquals(1, sink.records().size());
    assertEquals(auditable, sink.records().get(0).code());
  }
}
