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
package com.percussion.itemmanagement.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ItemWorkflowAssignmentRulesTest {

  @Test
  void acceptsADifferentAssociatedWorkflow() {
    assertEquals(
        ItemWorkflowAssignmentRules.Reason.OK,
        ItemWorkflowAssignmentRules.decide("7", 4, Set.of(4, 7)));
  }

  @Test
  void rejectsBlankMalformedUnchangedAndForbidden() {
    assertEquals(
        ItemWorkflowAssignmentRules.Reason.BLANK,
        ItemWorkflowAssignmentRules.decide("  ", 4, Set.of(7)));
    assertEquals(
        ItemWorkflowAssignmentRules.Reason.MALFORMED,
        ItemWorkflowAssignmentRules.decide("abc", 4, Set.of(7)));
    assertEquals(
        ItemWorkflowAssignmentRules.Reason.MALFORMED,
        ItemWorkflowAssignmentRules.decide("0", 4, Set.of(7)));
    assertEquals(
        ItemWorkflowAssignmentRules.Reason.UNCHANGED,
        ItemWorkflowAssignmentRules.decide("4", 4, Set.of(4, 7)));
    assertEquals(
        ItemWorkflowAssignmentRules.Reason.FORBIDDEN,
        ItemWorkflowAssignmentRules.decide("9", 4, List.of(7)));
    assertEquals(
        ItemWorkflowAssignmentRules.Reason.FORBIDDEN,
        ItemWorkflowAssignmentRules.decide("7", 4, null));
  }
}
