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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.workflows.WorkflowStepRoleAssignment;
import com.percussion.rest.workflows.WorkflowStepRoleAssignmentList;
import com.percussion.rest.workflows.WorkflowStepRoleAssignmentWrite;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.workflow.IPSWorkflowService;
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

/** Slice 61: one role's assignment type, without renaming the step or replacing the role list. */
@Tag("UnitTest")
class WorkflowsAdaptorStepRoleAssignmentTest {

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
  void setTypeSavesOnlyThatRole() {
    PSWorkflow workflow = workflow();
    when(workflowService.findWorkflowsByName(NAME)).thenReturn(List.of(workflow));

    WorkflowStepRoleAssignmentList out =
        adaptor.setStepRoleAssignment(null, NAME, "Draft", body("Author", "READER"));

    assertEquals(PSAssignmentTypeEnum.READER, assigned(workflow, "Draft", 11).getAssignmentType());
    assertEquals(PSAssignmentTypeEnum.READER, assigned(workflow, "Draft", 12).getAssignmentType());
    assertEquals("Draft", workflow.getStates().get(0).getName());
    assertEquals(2, workflow.getStates().get(0).getAssignedRoles().size());
    assertTrue(assigned(workflow, "Draft", 11).isDoNotify());
    verify(workflowService).saveWorkflow(workflow);
    assertEquals("READER", typeOf(out, "Draft", "Author"));
    assertEquals("READER", typeOf(out, "Draft", "Editor"));
  }

  @Test
  void listIncludesReader() {
    PSWorkflow workflow = workflow();
    when(workflowService.findWorkflowsByName(NAME)).thenReturn(List.of(workflow));

    WorkflowStepRoleAssignmentList out = adaptor.listStepRoleAssignments(null, NAME);
    assertEquals("ASSIGNEE", typeOf(out, "Draft", "Author"));
    assertEquals("READER", typeOf(out, "Draft", "Editor"));
    verify(workflowService, never()).saveWorkflow(any());
  }

  @Test
  void packagedDoesNotSave() {
    PSWorkflow workflow = workflow();
    workflow.setName("Default Workflow");
    when(workflowService.findWorkflowsByName("Default Workflow")).thenReturn(List.of(workflow));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                adaptor.setStepRoleAssignment(
                    null, "Default Workflow", "Draft", body("Author", "READER")));
    assertEquals(403, ex.getResponse().getStatus());
    assertEquals(PSAssignmentTypeEnum.ASSIGNEE, assigned(workflow, "Draft", 11).getAssignmentType());
    verify(workflowService, never()).saveWorkflow(any());
  }

  @Test
  void unchangedAndAdminDoNotSave() {
    PSWorkflow workflow = workflow();
    when(workflowService.findWorkflowsByName(NAME)).thenReturn(List.of(workflow));
    assertThrows(
        IllegalArgumentException.class,
        () -> adaptor.setStepRoleAssignment(null, NAME, "Draft", body("Author", "ASSIGNEE")));
    assigned(workflow, "Draft", 11).setAssignmentType(PSAssignmentTypeEnum.ADMIN);
    WebApplicationException conflict =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.setStepRoleAssignment(null, NAME, "Draft", body("Author", "READER")));
    assertEquals(409, conflict.getResponse().getStatus());
    assertEquals(PSAssignmentTypeEnum.ADMIN, assigned(workflow, "Draft", 11).getAssignmentType());
    verify(workflowService, never()).saveWorkflow(any());
  }

  @Test
  void missingWorkflowIs404() {
    when(workflowService.findWorkflowsByName("missing")).thenReturn(List.of());
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.setStepRoleAssignment(null, "missing", "Draft", body("Author", "READER")));
    assertEquals(404, ex.getResponse().getStatus());
    verify(workflowService, never()).saveWorkflow(any());
  }

  private static String typeOf(WorkflowStepRoleAssignmentList list, String step, String role) {
    for (WorkflowStepRoleAssignment row : list.getAssignments()) {
      if (step.equals(row.getStepName()) && role.equals(row.getRoleName())) {
        return row.getAssignmentType();
      }
    }
    return "";
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

  private static WorkflowStepRoleAssignmentWrite body(String role, String type) {
    WorkflowStepRoleAssignmentWrite body = new WorkflowStepRoleAssignmentWrite();
    body.setRoleName(role);
    body.setAssignmentType(type);
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
    draft.addAssignedRole(assignedRole(11, PSAssignmentTypeEnum.ASSIGNEE, true));
    draft.addAssignedRole(assignedRole(12, PSAssignmentTypeEnum.READER, false));
    List<PSState> states = new ArrayList<>();
    states.add(draft);
    workflow.setStates(states);
    return workflow;
  }

  private static PSAssignedRole assignedRole(long roleId, PSAssignmentTypeEnum type, boolean notify) {
    PSAssignedRole role = new PSAssignedRole();
    role.setGUID(new PSGuid(PSTypeEnum.WORKFLOW_ROLE, roleId));
    role.setStateId(1);
    role.setWorkflowId(7);
    role.setAssignmentType(type);
    role.setDoNotify(notify);
    return role;
  }
}
