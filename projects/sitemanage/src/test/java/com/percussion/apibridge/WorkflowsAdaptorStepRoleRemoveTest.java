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

import com.percussion.rest.workflows.WorkflowStepRoleAssignment;
import com.percussion.rest.workflows.WorkflowStepRoleAssignmentList;
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

/** Slice 64: remove one Reader or Assignee role from one step. */
@Tag("UnitTest")
class WorkflowsAdaptorStepRoleRemoveTest {

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
  void removeSavesOneRoleAndLeavesTheOtherAssignment() {
    PSWorkflow workflow = workflow();
    when(workflowService.findWorkflowsByName(NAME)).thenReturn(List.of(workflow));

    WorkflowStepRoleAssignmentList out = adaptor.removeStepRole(null, NAME, "Draft", "Author");

    assertEquals(1, workflow.getStates().get(0).getAssignedRoles().size());
    assertEquals("Draft", workflow.getStates().get(0).getName());
    assertFalse(hasRole(workflow, "Draft", 11));
    assertEquals(PSAssignmentTypeEnum.READER, assigned(workflow, "Draft", 12).getAssignmentType());
    assertTrue(assigned(workflow, "Draft", 12).isDoNotify());
    assertTrue(assigned(workflow, "Draft", 12).isShowInInbox());
    assertEquals(PSAssignmentTypeEnum.ASSIGNEE, assigned(workflow, "Review", 11).getAssignmentType());
    verify(workflowService).saveWorkflow(workflow);
    assertEquals("", typeOf(out, "Draft", "Author"));
    assertEquals("READER", typeOf(out, "Draft", "Editor"));
    assertEquals("ASSIGNEE", typeOf(out, "Review", "Author"));
  }

  @Test
  void adminAndPackagedDoNotSave() {
    PSWorkflow workflow = workflow();
    when(workflowService.findWorkflowsByName(NAME)).thenReturn(List.of(workflow));
    WebApplicationException conflict =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.removeStepRole(null, NAME, "Review", "Admin"));
    assertEquals(409, conflict.getResponse().getStatus());
    assertTrue(hasRole(workflow, "Review", 15));

    workflow.setName("Default Workflow");
    when(workflowService.findWorkflowsByName("Default Workflow")).thenReturn(List.of(workflow));
    WebApplicationException packaged =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.removeStepRole(null, "Default Workflow", "Draft", "Author"));
    assertEquals(403, packaged.getResponse().getStatus());
    assertTrue(hasRole(workflow, "Draft", 11));
    verify(workflowService, never()).saveWorkflow(any());
  }

  @Test
  void missingWorkflowStepAndRoleAre404() {
    when(workflowService.findWorkflowsByName("missing")).thenReturn(List.of());
    WebApplicationException missingWorkflow =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.removeStepRole(null, "missing", "Draft", "Author"));
    assertEquals(404, missingWorkflow.getResponse().getStatus());

    PSWorkflow workflow = workflow();
    when(workflowService.findWorkflowsByName(NAME)).thenReturn(List.of(workflow));
    WebApplicationException missingStep =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.removeStepRole(null, NAME, "Archive", "Author"));
    assertEquals(404, missingStep.getResponse().getStatus());
    WebApplicationException missingRole =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.removeStepRole(null, NAME, "Draft", "Designer"));
    assertEquals(404, missingRole.getResponse().getStatus());
    WebApplicationException notAssigned =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.removeStepRole(null, NAME, "Draft", "System"));
    assertEquals(404, notAssigned.getResponse().getStatus());
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

  private static boolean hasRole(PSWorkflow workflow, String step, int roleId) {
    for (PSState state : workflow.getStates()) {
      if (!step.equals(state.getName()) || state.getAssignedRoles() == null) {
        continue;
      }
      for (PSAssignedRole role : state.getAssignedRoles()) {
        if (role.getGUID().getUUID() == roleId) {
          return true;
        }
      }
    }
    return false;
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

  private static PSWorkflow workflow() {
    PSWorkflow workflow = new PSWorkflow();
    workflow.setName(NAME);
    List<PSWorkflowRole> roles = new ArrayList<>();
    roles.add(workflowRole(11, "Author"));
    roles.add(workflowRole(12, "Editor"));
    roles.add(workflowRole(14, "System"));
    roles.add(workflowRole(15, "Admin"));
    workflow.setRoles(roles);
    PSState draft = state(1, "Draft");
    draft.addAssignedRole(assignedRole(11, 1, PSAssignmentTypeEnum.ASSIGNEE, false));
    draft.addAssignedRole(assignedRole(12, 1, PSAssignmentTypeEnum.READER, true));
    PSState review = state(2, "Review");
    review.addAssignedRole(assignedRole(11, 2, PSAssignmentTypeEnum.ASSIGNEE, false));
    review.addAssignedRole(assignedRole(15, 2, PSAssignmentTypeEnum.ADMIN, true));
    List<PSState> states = new ArrayList<>();
    states.add(draft);
    states.add(review);
    workflow.setStates(states);
    return workflow;
  }

  private static PSState state(long id, String name) {
    PSState state = new PSState();
    state.setStateId(id);
    state.setWorkflowId(7);
    state.setName(name);
    return state;
  }

  private static PSWorkflowRole workflowRole(long id, String name) {
    PSWorkflowRole role = new PSWorkflowRole();
    role.setGUID(new PSGuid(PSTypeEnum.WORKFLOW_ROLE, id));
    role.setName(name);
    role.setWorkflowId(7);
    return role;
  }

  private static PSAssignedRole assignedRole(
      long roleId, long stateId, PSAssignmentTypeEnum type, boolean notify) {
    PSAssignedRole role = new PSAssignedRole();
    role.setGUID(new PSGuid(PSTypeEnum.WORKFLOW_ROLE, roleId));
    role.setStateId(stateId);
    role.setWorkflowId(7);
    role.setAssignmentType(type);
    role.setDoNotify(notify);
    role.setShowInInbox(notify);
    return role;
  }
}
