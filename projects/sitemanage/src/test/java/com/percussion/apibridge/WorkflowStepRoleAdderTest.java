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

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.workflow.data.PSAdhocTypeEnum;
import com.percussion.services.workflow.data.PSAssignedRole;
import com.percussion.services.workflow.data.PSAssignmentTypeEnum;
import com.percussion.services.workflow.data.PSState;
import com.percussion.services.workflow.data.PSTransition;
import com.percussion.services.workflow.data.PSWorkflowRole;
import jakarta.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class WorkflowStepRoleAdderTest {

  @Test
  void addPutsReaderOnTheStepAndLeavesNotifyAndInboxAtDefaults() {
    Fixture fixture = fixture();
    WorkflowStepRoleAdder.add(fixture.states, fixture.roles, "draft", "system", "Reader");

    assertEquals(2, fixture.draft.getAssignedRoles().size());
    assertEquals(1, fixture.review.getAssignedRoles().size());
    assertEquals("Draft", fixture.draft.getName());
    assertEquals(1, fixture.draft.getTransitions().size());
    PSAssignedRole added = assigned(fixture.draft, 14);
    assertEquals(PSAssignmentTypeEnum.READER, added.getAssignmentType());
    assertTrue(added.isDoNotify());
    assertTrue(added.isShowInInbox());
    assertEquals(PSAdhocTypeEnum.DISABLED, added.getAdhocType());
    assertEquals(fixture.draft.getStateId(), added.getStateId());
    assertEquals(fixture.draft.getWorkflowId(), added.getWorkflowId());
    assertEquals(PSAssignmentTypeEnum.ASSIGNEE, fixture.author.getAssignmentType());
    assertFalse(fixture.author.isDoNotify());
    assertFalse(fixture.author.isShowInInbox());
    assertEquals(PSAssignmentTypeEnum.ASSIGNEE, fixture.reviewer.getAssignmentType());
  }

  @Test
  void addAssigneeDoesNotTouchTheOtherStep() {
    Fixture fixture = fixture();
    WorkflowStepRoleAdder.add(fixture.states, fixture.roles, "Draft", "System", "ASSIGNEE");

    assertEquals(PSAssignmentTypeEnum.ASSIGNEE, assigned(fixture.draft, 14).getAssignmentType());
    assertTrue(assigned(fixture.draft, 14).isDoNotify());
    assertTrue(assigned(fixture.draft, 14).isShowInInbox());
    assertEquals(1, fixture.review.getAssignedRoles().size());
    assertEquals(PSAssignmentTypeEnum.ASSIGNEE, fixture.reviewer.getAssignmentType());
  }

  @Test
  void roleAlreadyOnTheStepIs409AndDoesNotMutate() {
    Fixture fixture = fixture();
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                WorkflowStepRoleAdder.add(
                    fixture.states, fixture.roles, "Draft", "Author", "READER"));
    assertEquals(409, ex.getResponse().getStatus());
    assertEquals(1, fixture.draft.getAssignedRoles().size());
    assertEquals(PSAssignmentTypeEnum.ASSIGNEE, fixture.author.getAssignmentType());
    assertFalse(fixture.author.isDoNotify());
  }

  @Test
  void missingStepAndRoleAre404() {
    Fixture fixture = fixture();
    WebApplicationException missingStep =
        assertThrows(
            WebApplicationException.class,
            () ->
                WorkflowStepRoleAdder.add(
                    fixture.states, fixture.roles, "Archive", "System", "READER"));
    assertEquals(404, missingStep.getResponse().getStatus());
    WebApplicationException missingRole =
        assertThrows(
            WebApplicationException.class,
            () ->
                WorkflowStepRoleAdder.add(
                    fixture.states, fixture.roles, "Draft", "Designer", "READER"));
    assertEquals(404, missingRole.getResponse().getStatus());
    assertEquals(1, fixture.draft.getAssignedRoles().size());
  }

  @Test
  void blankRoleAndInvalidTypeAre400() {
    Fixture fixture = fixture();
    assertThrows(
        IllegalArgumentException.class,
        () -> WorkflowStepRoleAdder.add(fixture.states, fixture.roles, "Draft", " ", "READER"));
    assertThrows(
        IllegalArgumentException.class,
        () -> WorkflowStepRoleAdder.add(fixture.states, fixture.roles, "Draft", "System", "ADMIN"));
    assertEquals(1, fixture.draft.getAssignedRoles().size());
  }

  private static PSAssignedRole assigned(PSState state, int roleId) {
    for (PSAssignedRole role : state.getAssignedRoles()) {
      if (role.getGUID().getUUID() == roleId) {
        return role;
      }
    }
    throw new AssertionError("missing role " + roleId);
  }

  private static Fixture fixture() {
    PSWorkflowRole authorRole = role(11, "Author");
    PSWorkflowRole systemRole = role(14, "System");
    PSWorkflowRole reviewerRole = role(13, "Reviewer");
    PSState draft = state(1, "Draft");
    PSAssignedRole author = assigned(11, 1, PSAssignmentTypeEnum.ASSIGNEE, false, false);
    draft.addAssignedRole(author);
    PSTransition submit = new PSTransition();
    submit.setLabel("Submit");
    draft.addTransition(submit);
    PSState review = state(2, "Review");
    PSAssignedRole reviewer = assigned(13, 2, PSAssignmentTypeEnum.ASSIGNEE, true, true);
    review.addAssignedRole(reviewer);
    List<PSState> states = new ArrayList<>();
    states.add(draft);
    states.add(review);
    List<PSWorkflowRole> roles = new ArrayList<>();
    roles.add(authorRole);
    roles.add(systemRole);
    roles.add(reviewerRole);
    return new Fixture(states, roles, draft, review, author, reviewer);
  }

  private static PSState state(long id, String name) {
    PSState state = new PSState();
    state.setStateId(id);
    state.setName(name);
    state.setWorkflowId(42);
    return state;
  }

  private static PSWorkflowRole role(long id, String name) {
    PSWorkflowRole role = new PSWorkflowRole();
    role.setGUID(new PSGuid(PSTypeEnum.WORKFLOW_ROLE, id));
    role.setName(name);
    role.setWorkflowId(42);
    return role;
  }

  private static PSAssignedRole assigned(
      long roleId,
      long stateId,
      PSAssignmentTypeEnum type,
      boolean notify,
      boolean inbox) {
    PSAssignedRole role = new PSAssignedRole();
    role.setGUID(new PSGuid(PSTypeEnum.WORKFLOW_ROLE, roleId));
    role.setStateId(stateId);
    role.setWorkflowId(42);
    role.setAssignmentType(type);
    role.setDoNotify(notify);
    role.setShowInInbox(inbox);
    role.setAdhocType(PSAdhocTypeEnum.DISABLED);
    return role;
  }

  private record Fixture(
      List<PSState> states,
      List<PSWorkflowRole> roles,
      PSState draft,
      PSState review,
      PSAssignedRole author,
      PSAssignedRole reviewer) {}
}
