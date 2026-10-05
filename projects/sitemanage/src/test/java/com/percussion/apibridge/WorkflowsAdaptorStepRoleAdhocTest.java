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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.workflows.WorkflowStepRoleAdhocWrite;
import com.percussion.rest.workflows.WorkflowStepRoleAssignment;
import com.percussion.rest.workflows.WorkflowStepRoleAssignmentList;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.services.workflow.data.PSAdhocTypeEnum;
import com.percussion.services.workflow.data.PSAssignedRole;
import com.percussion.services.workflow.data.PSAssignmentTypeEnum;
import com.percussion.services.workflow.data.PSState;
import com.percussion.services.workflow.data.PSWorkflow;
import com.percussion.services.workflow.data.PSWorkflowRole;
import com.percussion.utils.request.PSRequestInfoBase;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.workflow.service.IPSSteppedWorkflowService;
import jakarta.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Slice 69: one Reader or Assignee adhoc type, without changing type, notify, or inbox. */
@Tag("UnitTest")
class WorkflowsAdaptorStepRoleAdhocTest {

  private static final String NAME = "Nightly QA";

  private IPSWorkflowService workflowService;
  private WorkflowsAdaptor adaptor;

  @BeforeEach
  void setUp() {
    PSRequestInfoBase.initRequestInfo(new HashMap<>());
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_JSESSIONID, "test-session");
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_USER, "Admin");
    workflowService = mock(IPSWorkflowService.class);
    adaptor =
        new WorkflowsAdaptor(
            mock(IPSContentDesignWs.class),
            workflowService,
            mock(IPSContentMgr.class),
            () -> true,
            mock(IPSSteppedWorkflowService.class));
  }

  @AfterEach
  void tearDown() {
    PSRequestInfoBase.resetRequestInfo();
  }

  @Test
  void setAdhocSavesOnlyThatType() {
    PSWorkflow workflow = workflow();
    when(workflowService.findWorkflowsByName(NAME)).thenReturn(List.of(workflow));

    WorkflowStepRoleAssignmentList out =
        adaptor.setStepRoleAdhoc(null, NAME, "Draft", body("Author", "enabled"));

    PSAssignedRole author = assigned(workflow, "Draft", 11);
    PSAssignedRole editor = assigned(workflow, "Draft", 12);
    assertEquals(PSAdhocTypeEnum.ENABLED, author.getAdhocType());
    assertEquals(PSAssignmentTypeEnum.ASSIGNEE, author.getAssignmentType());
    assertTrue(author.isDoNotify());
    assertTrue(author.isShowInInbox());
    assertEquals(PSAdhocTypeEnum.DISABLED, editor.getAdhocType());
    assertEquals(PSAssignmentTypeEnum.READER, editor.getAssignmentType());
    assertFalse(editor.isDoNotify());
    assertFalse(editor.isShowInInbox());
    assertEquals("Draft", workflow.getStates().get(0).getName());
    assertEquals(2, workflow.getStates().get(0).getAssignedRoles().size());
    verify(workflowService).saveWorkflow(workflow);
    assertEquals("enabled", adhocOf(out, "Draft", "Author"));
    assertEquals("disabled", adhocOf(out, "Draft", "Editor"));
    assertTrue(notifyOf(out, "Draft", "Author"));
    assertTrue(inboxOf(out, "Draft", "Author"));
    assertEquals("ASSIGNEE", typeOf(out, "Draft", "Author"));
  }

  @Test
  void listIncludesStoredAdhoc() {
    PSWorkflow workflow = workflow();
    assigned(workflow, "Draft", 12).setAdhocType(PSAdhocTypeEnum.ANONYMOUS);
    when(workflowService.findWorkflowsByName(NAME)).thenReturn(List.of(workflow));

    WorkflowStepRoleAssignmentList out = adaptor.listStepRoleAssignments(null, NAME);
    assertEquals("disabled", adhocOf(out, "Draft", "Author"));
    assertEquals("anonymous", adhocOf(out, "Draft", "Editor"));
    verify(workflowService, never()).saveWorkflow(any());
  }

  @Test
  void packagedNamesDoNotSave() {
    for (String packagedName : List.of("Default Workflow", "Simple Workflow", "Local Content")) {
      PSWorkflow workflow = workflow();
      workflow.setName(packagedName);
      when(workflowService.findWorkflowsByName(packagedName)).thenReturn(List.of(workflow));
      WebApplicationException ex =
          assertThrows(
              WebApplicationException.class,
              () ->
                  adaptor.setStepRoleAdhoc(
                      null, packagedName, "Draft", body("Author", "enabled")));
      assertEquals(403, ex.getResponse().getStatus(), packagedName);
      assertEquals(PSAdhocTypeEnum.DISABLED, assigned(workflow, "Draft", 11).getAdhocType());
    }
    verify(workflowService, never()).saveWorkflow(any());
  }

  @Test
  void unchangedDoesNotSave() {
    PSWorkflow workflow = workflow();
    when(workflowService.findWorkflowsByName(NAME)).thenReturn(List.of(workflow));
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> adaptor.setStepRoleAdhoc(null, NAME, "Draft", body("Author", "disabled")));
    assertTrue(ex.getMessage().toLowerCase().contains("unchanged"));
    assertEquals(PSAdhocTypeEnum.DISABLED, assigned(workflow, "Draft", 11).getAdhocType());
    verify(workflowService, never()).saveWorkflow(any());
  }

  @Test
  void invalidValueDoesNotSave() {
    PSWorkflow workflow = workflow();
    when(workflowService.findWorkflowsByName(NAME)).thenReturn(List.of(workflow));
    assertThrows(
        IllegalArgumentException.class,
        () -> adaptor.setStepRoleAdhoc(null, NAME, "Draft", body("Author", "maybe")));
    assertEquals(PSAdhocTypeEnum.DISABLED, assigned(workflow, "Draft", 11).getAdhocType());
    verify(workflowService, never()).saveWorkflow(any());
  }

  @Test
  void adminRoleIs409AndDoesNotSave() {
    PSWorkflow workflow = workflow();
    assigned(workflow, "Draft", 11).setAssignmentType(PSAssignmentTypeEnum.ADMIN);
    when(workflowService.findWorkflowsByName(NAME)).thenReturn(List.of(workflow));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.setStepRoleAdhoc(null, NAME, "Draft", body("Author", "enabled")));
    assertEquals(409, ex.getResponse().getStatus());
    assertEquals(PSAdhocTypeEnum.DISABLED, assigned(workflow, "Draft", 11).getAdhocType());
    assertEquals(PSAssignmentTypeEnum.ADMIN, assigned(workflow, "Draft", 11).getAssignmentType());
    assertTrue(assigned(workflow, "Draft", 11).isShowInInbox());
    verify(workflowService, never()).saveWorkflow(any());
  }

  @Test
  void missingWorkflowIs404() {
    when(workflowService.findWorkflowsByName("missing")).thenReturn(List.of());
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.setStepRoleAdhoc(null, "missing", "Draft", body("Author", "enabled")));
    assertEquals(404, ex.getResponse().getStatus());
    verify(workflowService, never()).saveWorkflow(any());
  }

  @Test
  void roleNotOnStepIs404AndDoesNotCreateAnAssignment() {
    PSWorkflow workflow = workflow();
    PSWorkflowRole designer = new PSWorkflowRole();
    designer.setGUID(new PSGuid(PSTypeEnum.WORKFLOW_ROLE, 14));
    designer.setName("Designer");
    workflow.getRoles().add(designer);
    when(workflowService.findWorkflowsByName(NAME)).thenReturn(List.of(workflow));
    int before = workflow.getStates().get(0).getAssignedRoles().size();
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.setStepRoleAdhoc(null, NAME, "Draft", body("Designer", "enabled")));
    assertEquals(404, ex.getResponse().getStatus());
    assertEquals(before, workflow.getStates().get(0).getAssignedRoles().size());
    assertEquals(PSAdhocTypeEnum.DISABLED, assigned(workflow, "Draft", 11).getAdhocType());
    verify(workflowService, never()).saveWorkflow(any());
  }

  private static String adhocOf(WorkflowStepRoleAssignmentList list, String step, String role) {
    return row(list, step, role).getAdhocType();
  }

  private static boolean inboxOf(WorkflowStepRoleAssignmentList list, String step, String role) {
    return row(list, step, role).isInbox();
  }

  private static boolean notifyOf(WorkflowStepRoleAssignmentList list, String step, String role) {
    return row(list, step, role).isNotify();
  }

  private static String typeOf(WorkflowStepRoleAssignmentList list, String step, String role) {
    return row(list, step, role).getAssignmentType();
  }

  private static WorkflowStepRoleAssignment row(
      WorkflowStepRoleAssignmentList list, String step, String role) {
    for (WorkflowStepRoleAssignment item : list.getAssignments()) {
      if (step.equals(item.getStepName()) && role.equals(item.getRoleName())) {
        return item;
      }
    }
    throw new AssertionError("missing row " + step + " " + role);
  }

  private static PSAssignedRole assigned(PSWorkflow workflow, String step, int roleId) {
    for (PSState state : workflow.getStates()) {
      if (!step.equals(state.getName())) {
        continue;
      }
      for (PSAssignedRole role : state.getAssignedRoles()) {
        if (role.getGUID().getUUID() == roleId) {
          return role;
        }
      }
    }
    throw new AssertionError("missing role " + roleId);
  }

  private static WorkflowStepRoleAdhocWrite body(String role, String adhocType) {
    WorkflowStepRoleAdhocWrite body = new WorkflowStepRoleAdhocWrite();
    body.setRoleName(role);
    body.setAdhocType(adhocType);
    return body;
  }

  private static PSWorkflow workflow() {
    PSWorkflow workflow = new PSWorkflow();
    workflow.setName(NAME);
    PSWorkflowRole author = new PSWorkflowRole();
    author.setGUID(new PSGuid(PSTypeEnum.WORKFLOW_ROLE, 11));
    author.setName("Author");
    PSWorkflowRole editor = new PSWorkflowRole();
    editor.setGUID(new PSGuid(PSTypeEnum.WORKFLOW_ROLE, 12));
    editor.setName("Editor");
    List<PSWorkflowRole> roles = new ArrayList<>();
    roles.add(author);
    roles.add(editor);
    workflow.setRoles(roles);
    PSState draft = new PSState();
    draft.setStateId(1);
    draft.setWorkflowId(7);
    draft.setName("Draft");
    draft.addAssignedRole(assignedRole(11, PSAssignmentTypeEnum.ASSIGNEE, true, true));
    draft.addAssignedRole(assignedRole(12, PSAssignmentTypeEnum.READER, false, false));
    List<PSState> states = new ArrayList<>();
    states.add(draft);
    workflow.setStates(states);
    return workflow;
  }

  private static PSAssignedRole assignedRole(
      long roleId, PSAssignmentTypeEnum type, boolean notify, boolean inbox) {
    PSAssignedRole role = new PSAssignedRole();
    role.setGUID(new PSGuid(PSTypeEnum.WORKFLOW_ROLE, roleId));
    role.setStateId(1);
    role.setWorkflowId(7);
    role.setAssignmentType(type);
    role.setDoNotify(notify);
    role.setShowInInbox(inbox);
    role.setAdhocType(PSAdhocTypeEnum.DISABLED);
    return role;
  }
}
