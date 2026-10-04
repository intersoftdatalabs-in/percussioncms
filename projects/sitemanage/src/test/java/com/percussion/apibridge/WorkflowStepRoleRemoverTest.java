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
class WorkflowStepRoleRemoverTest {

  @Test
  void removeDropsOneRoleAndLeavesTheOtherStepAndFlags() {
    Fixture fixture = fixture();
    WorkflowStepRoleRemover.remove(fixture.states, fixture.roles, "draft", "author");

    assertEquals(1, fixture.draft.getAssignedRoles().size());
    assertEquals("Draft", fixture.draft.getName());
    assertEquals(1, fixture.draft.getTransitions().size());
    assertFalse(assignedIds(fixture.draft).contains(11));
    PSAssignedRole editor = assigned(fixture.draft, 12);
    assertEquals(PSAssignmentTypeEnum.READER, editor.getAssignmentType());
    assertTrue(editor.isDoNotify());
    assertTrue(editor.isShowInInbox());
    assertEquals(PSAdhocTypeEnum.DISABLED, editor.getAdhocType());
    assertEquals(2, fixture.review.getAssignedRoles().size());
    assertEquals(PSAssignmentTypeEnum.ASSIGNEE, assigned(fixture.review, 11).getAssignmentType());
    assertFalse(assigned(fixture.review, 11).isDoNotify());
    assertEquals(PSAssignmentTypeEnum.ADMIN, assigned(fixture.review, 15).getAssignmentType());
  }

  @Test
  void adminAssignmentIs409AndDoesNotMutate() {
    Fixture fixture = fixture();
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> WorkflowStepRoleRemover.remove(fixture.states, fixture.roles, "Review", "Admin"));
    assertEquals(409, ex.getResponse().getStatus());
    assertEquals(2, fixture.review.getAssignedRoles().size());
    assertEquals(PSAssignmentTypeEnum.ADMIN, assigned(fixture.review, 15).getAssignmentType());
    assertEquals(2, fixture.draft.getAssignedRoles().size());
  }

  @Test
  void missingStepAndRoleAre404() {
    Fixture fixture = fixture();
    WebApplicationException missingStep =
        assertThrows(
            WebApplicationException.class,
            () ->
                WorkflowStepRoleRemover.remove(
                    fixture.states, fixture.roles, "Archive", "Author"));
    assertEquals(404, missingStep.getResponse().getStatus());
    WebApplicationException missingRole =
        assertThrows(
            WebApplicationException.class,
            () ->
                WorkflowStepRoleRemover.remove(
                    fixture.states, fixture.roles, "Draft", "Designer"));
    assertEquals(404, missingRole.getResponse().getStatus());
    WebApplicationException notAssigned =
        assertThrows(
            WebApplicationException.class,
            () ->
                WorkflowStepRoleRemover.remove(fixture.states, fixture.roles, "Draft", "System"));
    assertEquals(404, notAssigned.getResponse().getStatus());
    assertEquals(2, fixture.draft.getAssignedRoles().size());
  }

  @Test
  void blankNamesAre400() {
    Fixture fixture = fixture();
    assertThrows(
        IllegalArgumentException.class,
        () -> WorkflowStepRoleRemover.remove(fixture.states, fixture.roles, "Draft", " "));
    assertThrows(
        IllegalArgumentException.class,
        () -> WorkflowStepRoleRemover.remove(fixture.states, fixture.roles, " ", "Author"));
    assertEquals(2, fixture.draft.getAssignedRoles().size());
  }

  private static List<Integer> assignedIds(PSState state) {
    List<Integer> ids = new ArrayList<>();
    for (PSAssignedRole role : state.getAssignedRoles()) {
      ids.add(role.getGUID().getUUID());
    }
    return ids;
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
    PSState draft = state(1, "Draft");
    PSAssignedRole author = assigned(11, 1, PSAssignmentTypeEnum.ASSIGNEE, false, false);
    PSAssignedRole editor = assigned(12, 1, PSAssignmentTypeEnum.READER, true, true);
    draft.addAssignedRole(author);
    draft.addAssignedRole(editor);
    PSTransition submit = new PSTransition();
    submit.setLabel("Submit");
    draft.addTransition(submit);
    PSState review = state(2, "Review");
    review.addAssignedRole(assigned(11, 2, PSAssignmentTypeEnum.ASSIGNEE, false, false));
    review.addAssignedRole(assigned(15, 2, PSAssignmentTypeEnum.ADMIN, true, true));
    List<PSState> states = new ArrayList<>();
    states.add(draft);
    states.add(review);
    List<PSWorkflowRole> roles = new ArrayList<>();
    roles.add(role(11, "Author"));
    roles.add(role(12, "Editor"));
    roles.add(role(14, "System"));
    roles.add(role(15, "Admin"));
    return new Fixture(states, roles, draft, review);
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
      long roleId, long stateId, PSAssignmentTypeEnum type, boolean notify, boolean inbox) {
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
      List<PSState> states, List<PSWorkflowRole> roles, PSState draft, PSState review) {}
}
