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
class WorkflowStepRoleAssignmentWriterTest {

  @Test
  void listsReaderAndAssigneeWithoutDroppingEither() {
    Fixture fixture = fixture();
    List<WorkflowStepRoleAssignmentWriter.StepRoleAssignment> rows =
        WorkflowStepRoleAssignmentWriter.list(fixture.states, fixture.roles);

    assertEquals(3, rows.size());
    assertEquals("Draft", rows.get(0).stepName());
    assertEquals("Author", rows.get(0).roleName());
    assertEquals("ASSIGNEE", rows.get(0).assignmentType());
    assertTrue(rows.get(0).notifyOn());
    assertEquals("Editor", rows.get(1).roleName());
    assertEquals("READER", rows.get(1).assignmentType());
    assertFalse(rows.get(1).notifyOn());
    assertEquals("Review", rows.get(2).stepName());
    assertEquals("Reviewer", rows.get(2).roleName());
    assertEquals("ASSIGNEE", rows.get(2).assignmentType());
    assertTrue(rows.get(2).notifyOn());
  }

  @Test
  void setNotifyChangesOnlyThatFlag() {
    Fixture fixture = fixture();
    WorkflowStepRoleAssignmentWriter.setNotify(
        fixture.states, fixture.roles, "draft", "author", Boolean.FALSE);

    assertEquals("Draft", fixture.draft.getName());
    assertEquals(2, fixture.draft.getAssignedRoles().size());
    assertEquals(1, fixture.draft.getTransitions().size());
    assertEquals(PSAssignmentTypeEnum.ASSIGNEE, fixture.author.getAssignmentType());
    assertFalse(fixture.author.isDoNotify());
    assertTrue(fixture.author.isShowInInbox());
    assertEquals(PSAdhocTypeEnum.DISABLED, fixture.author.getAdhocType());
    assertFalse(fixture.editor.isDoNotify());
    assertFalse(fixture.editor.isShowInInbox());
    assertEquals(PSAssignmentTypeEnum.ASSIGNEE, fixture.reviewer.getAssignmentType());
    assertTrue(fixture.reviewer.isDoNotify());
  }

  @Test
  void unchangedNotifyIs400AndDoesNotMutate() {
    Fixture fixture = fixture();
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                WorkflowStepRoleAssignmentWriter.setNotify(
                    fixture.states, fixture.roles, "Draft", "Author", Boolean.TRUE));
    assertTrue(ex.getMessage().toLowerCase().contains("unchanged"));
    assertTrue(fixture.author.isDoNotify());
    assertEquals(PSAssignmentTypeEnum.ASSIGNEE, fixture.author.getAssignmentType());
  }

  @Test
  void adminRoleNotifyCanChange() {
    Fixture fixture = fixture();
    fixture.author.setAssignmentType(PSAssignmentTypeEnum.ADMIN);
    WorkflowStepRoleAssignmentWriter.setNotify(
        fixture.states, fixture.roles, "Draft", "Author", Boolean.FALSE);
    assertEquals(PSAssignmentTypeEnum.ADMIN, fixture.author.getAssignmentType());
    assertFalse(fixture.author.isDoNotify());
    assertTrue(fixture.author.isShowInInbox());
  }

  @Test
  void missingNotifyStepAndRoleAre404() {
    Fixture fixture = fixture();
    WebApplicationException missingStep =
        assertThrows(
            WebApplicationException.class,
            () ->
                WorkflowStepRoleAssignmentWriter.setNotify(
                    fixture.states, fixture.roles, "Archive", "Author", Boolean.FALSE));
    assertEquals(404, missingStep.getResponse().getStatus());
    WebApplicationException missingRole =
        assertThrows(
            WebApplicationException.class,
            () ->
                WorkflowStepRoleAssignmentWriter.setNotify(
                    fixture.states, fixture.roles, "Draft", "Designer", Boolean.FALSE));
    assertEquals(404, missingRole.getResponse().getStatus());
    assertTrue(fixture.author.isDoNotify());
  }

  @Test
  void missingNotifyValueIs400() {
    Fixture fixture = fixture();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            WorkflowStepRoleAssignmentWriter.setNotify(
                fixture.states, fixture.roles, "Draft", "Author", null));
    assertTrue(fixture.author.isDoNotify());
  }

  @Test
  void setTypeChangesOnlyThatRole() {
    Fixture fixture = fixture();
    WorkflowStepRoleAssignmentWriter.setType(
        fixture.states, fixture.roles, "draft", "author", "Reader");

    assertEquals("Draft", fixture.draft.getName());
    assertEquals(2, fixture.draft.getAssignedRoles().size());
    assertEquals(1, fixture.draft.getTransitions().size());
    assertEquals(PSAssignmentTypeEnum.READER, fixture.author.getAssignmentType());
    assertEquals(PSAssignmentTypeEnum.READER, fixture.editor.getAssignmentType());
    assertTrue(fixture.author.isDoNotify());
    assertTrue(fixture.author.isShowInInbox());
    assertEquals(PSAdhocTypeEnum.DISABLED, fixture.author.getAdhocType());
    assertFalse(fixture.editor.isDoNotify());
    assertEquals("Review", fixture.review.getName());
    assertEquals(PSAssignmentTypeEnum.ASSIGNEE, fixture.reviewer.getAssignmentType());
  }

  @Test
  void unchangedTypeIs400AndDoesNotMutate() {
    Fixture fixture = fixture();
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                WorkflowStepRoleAssignmentWriter.setType(
                    fixture.states, fixture.roles, "Draft", "Author", "ASSIGNEE"));
    assertTrue(ex.getMessage().toLowerCase().contains("unchanged"));
    assertEquals(PSAssignmentTypeEnum.ASSIGNEE, fixture.author.getAssignmentType());
  }

  @Test
  void adminRoleIs409AndStaysAdmin() {
    Fixture fixture = fixture();
    fixture.author.setAssignmentType(PSAssignmentTypeEnum.ADMIN);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                WorkflowStepRoleAssignmentWriter.setType(
                    fixture.states, fixture.roles, "Draft", "Author", "READER"));
    assertEquals(409, ex.getResponse().getStatus());
    assertEquals(PSAssignmentTypeEnum.ADMIN, fixture.author.getAssignmentType());
  }

  @Test
  void missingStepAndRoleAre404() {
    Fixture fixture = fixture();
    WebApplicationException missingStep =
        assertThrows(
            WebApplicationException.class,
            () ->
                WorkflowStepRoleAssignmentWriter.setType(
                    fixture.states, fixture.roles, "Archive", "Author", "READER"));
    assertEquals(404, missingStep.getResponse().getStatus());
    WebApplicationException missingRole =
        assertThrows(
            WebApplicationException.class,
            () ->
                WorkflowStepRoleAssignmentWriter.setType(
                    fixture.states, fixture.roles, "Draft", "Designer", "READER"));
    assertEquals(404, missingRole.getResponse().getStatus());
    assertEquals(PSAssignmentTypeEnum.ASSIGNEE, fixture.author.getAssignmentType());
  }

  @Test
  void invalidTypeIs400() {
    Fixture fixture = fixture();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            WorkflowStepRoleAssignmentWriter.setType(
                fixture.states, fixture.roles, "Draft", "Author", "ADMIN"));
    assertEquals(PSAssignmentTypeEnum.ASSIGNEE, fixture.author.getAssignmentType());
  }

  private static Fixture fixture() {
    PSWorkflowRole authorRole = role(11, "Author");
    PSWorkflowRole editorRole = role(12, "Editor");
    PSWorkflowRole reviewerRole = role(13, "Reviewer");
    PSState draft = state(1, "Draft");
    PSAssignedRole author = assigned(11, 1, PSAssignmentTypeEnum.ASSIGNEE, true, true);
    PSAssignedRole editor = assigned(12, 1, PSAssignmentTypeEnum.READER, false, false);
    draft.addAssignedRole(author);
    draft.addAssignedRole(editor);
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
    roles.add(editorRole);
    roles.add(reviewerRole);
    return new Fixture(states, roles, draft, review, author, editor, reviewer);
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
      PSAssignedRole editor,
      PSAssignedRole reviewer) {}
}
