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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intsof.percussioncms.auditlog.AuditEventType;
import com.intsof.percussioncms.auditlog.AuditModule;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WorkflowErrorCodesTest {

  @Test
  void everyConstantHasExplicitModuleWfAndUniqueNumericCode() {
    Set<Integer> seen = new HashSet<>();
    for (WorkflowErrorCodes code : WorkflowErrorCodes.values()) {
      assertEquals(AuditModule.WF, code.module());
      assertTrue(code.numericCode() > 0, code.name());
      assertTrue(seen.add(code.numericCode()), "duplicate numeric: " + code.numericCode());
      assertNotNull(code.userMessageTemplate());
      assertNotNull(code.logMessageTemplate());
      assertTrue(code.qualifiedCode().startsWith("WF-"));
    }
  }

  @Test
  void auditableCodesRequireEventType() {
    for (WorkflowErrorCodes code : WorkflowErrorCodes.values()) {
      if (code.isAuditable()) {
        assertNotNull(code.eventType(), code.name() + " auditable without eventType");
      } else {
        assertNull(code.eventType(), code.name() + " non-auditable should not force eventType");
      }
    }
  }

  @Test
  void securityRelevantWorkflowFailuresAreAuditable() {
    assertTrue(WorkflowErrorCodes.ACCESS_DENIED.isAuditable());
    assertEquals(AuditEventType.ACCESS_DENIED, WorkflowErrorCodes.ACCESS_DENIED.eventType());
    assertTrue(WorkflowErrorCodes.INVALID_TRANSITION.isAuditable());
    assertEquals(
        AuditEventType.WORKFLOW_TRANSITION, WorkflowErrorCodes.INVALID_TRANSITION.eventType());
    assertTrue(WorkflowErrorCodes.ASSIGNMENT_ERROR.isAuditable());
    assertTrue(WorkflowErrorCodes.TRANSITION.isAuditable());
  }

  @Test
  void loadAndConfigOperationalNoiseIsNotAuditable() {
    assertFalse(WorkflowErrorCodes.WORKFLOW_NOT_FOUND.isAuditable());
    assertFalse(WorkflowErrorCodes.STATE_NOT_FOUND.isAuditable());
    assertFalse(WorkflowErrorCodes.OPERATION_FAILED.isAuditable());
    assertFalse(WorkflowErrorCodes.CONFIGURATION_ERROR.isAuditable());
    assertFalse(WorkflowErrorCodes.TRANSITION_NOT_FOUND.isAuditable());
  }

  @Test
  void preservesPhase2aAndLegacyIpsWorkflowNumericValues() {
    assertEquals(4001, WorkflowErrorCodes.TRANSITION.numericCode());
    assertEquals(1, WorkflowErrorCodes.WORKFLOW_NOT_FOUND.numericCode());
    assertEquals(2, WorkflowErrorCodes.STATE_NOT_FOUND.numericCode());
    assertEquals(6, WorkflowErrorCodes.ACCESS_DENIED.numericCode());
    assertEquals(8, WorkflowErrorCodes.INVALID_TRANSITION.numericCode());
    assertEquals(10, WorkflowErrorCodes.ASSIGNMENT_ERROR.numericCode());
  }
}
