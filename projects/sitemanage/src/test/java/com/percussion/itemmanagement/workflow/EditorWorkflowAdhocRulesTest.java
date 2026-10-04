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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.workflow.data.PSAdhocTypeEnum;
import com.percussion.services.workflow.data.PSAssignedRole;
import com.percussion.services.workflow.data.PSAssignmentTypeEnum;
import com.percussion.services.workflow.data.PSState;
import com.percussion.services.workflow.data.PSTransition;
import com.percussion.services.workflow.data.PSWorkflow;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Ad-hoc assignee classification for editor transitions (#5163). */
class EditorWorkflowAdhocRulesTest {

  @Test
  void parseDropsBlanksAndKeepsFirstSeenOrder() {
    assertEquals(List.of(), EditorWorkflowAdhocRules.parseAdhocAssignees(null));
    assertEquals(List.of(), EditorWorkflowAdhocRules.parseAdhocAssignees("  "));
    assertEquals(List.of(), EditorWorkflowAdhocRules.parseAdhocAssignees(", ,"));
    assertEquals(
        List.of("alice", "bob"),
        EditorWorkflowAdhocRules.parseAdhocAssignees(" alice, bob,alice , ,bob "));
  }

  @Test
  void assigneeOrAdminWithAdhocEnabledRequiresAssignees() {
    PSWorkflow workflow = workflow(adhocState(2, PSAssignmentTypeEnum.ASSIGNEE, PSAdhocTypeEnum.ENABLED));
    assertTrue(EditorWorkflowAdhocRules.triggerRequiresAssignees(workflow, transition("Submit", 2)));
    assertTrue(
        EditorWorkflowAdhocRules.triggerRequiresAssignees(
            workflow(adhocState(3, PSAssignmentTypeEnum.ADMIN, PSAdhocTypeEnum.ANONYMOUS)),
            transition("Route", 3)));
  }

  @Test
  void disabledAdhocReaderOrMissingStateDoesNotRequireAssignees() {
    assertFalse(
        EditorWorkflowAdhocRules.triggerRequiresAssignees(
            workflow(adhocState(2, PSAssignmentTypeEnum.ASSIGNEE, PSAdhocTypeEnum.DISABLED)),
            transition("Submit", 2)));
    assertFalse(
        EditorWorkflowAdhocRules.triggerRequiresAssignees(
            workflow(adhocState(2, PSAssignmentTypeEnum.READER, PSAdhocTypeEnum.ENABLED)),
            transition("Submit", 2)));
    assertFalse(
        EditorWorkflowAdhocRules.triggerRequiresAssignees(
            workflow(adhocState(2, PSAssignmentTypeEnum.ASSIGNEE, PSAdhocTypeEnum.ENABLED)),
            transition("Reject", 9)));
    assertFalse(
        EditorWorkflowAdhocRules.triggerRequiresAssignees(
            workflow(adhocState(2, PSAssignmentTypeEnum.ASSIGNEE, PSAdhocTypeEnum.ENABLED)),
            transition("  ", 2)));
    assertFalse(EditorWorkflowAdhocRules.triggerRequiresAssignees(null, transition("Submit", 2)));
    assertFalse(
        EditorWorkflowAdhocRules.triggerRequiresAssignees(
            workflow(adhocState(2, PSAssignmentTypeEnum.ASSIGNEE, PSAdhocTypeEnum.ENABLED)),
            null));
  }

  private static PSWorkflow workflow(PSState to) {
    PSState from = new PSState();
    from.setStateId(1);
    PSWorkflow workflow = new PSWorkflow();
    workflow.setStates(List.of(from, to));
    return workflow;
  }

  private static PSState adhocState(long id, PSAssignmentTypeEnum assignment, PSAdhocTypeEnum adhoc) {
    PSAssignedRole role = new PSAssignedRole();
    role.setAssignmentType(assignment);
    role.setAdhocType(adhoc);
    PSState state = new PSState();
    state.setStateId(id);
    state.setAssignedRoles(List.of(role));
    return state;
  }

  private static PSTransition transition(String trigger, long toState) {
    PSTransition transition = new PSTransition();
    transition.setTrigger(trigger);
    transition.setToState(toState);
    return transition;
  }
}
