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

import com.percussion.rest.workflows.WorkflowGraph;
import com.percussion.rest.workflows.WorkflowTransitionWrite;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.services.workflow.data.PSState;
import com.percussion.services.workflow.data.PSTransition;
import com.percussion.services.workflow.data.PSWorkflow;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfoBase;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.workflow.data.PSUiWorkflow;
import com.percussion.workflow.service.IPSSteppedWorkflowService;
import jakarta.ws.rs.WebApplicationException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class WorkflowsAdaptorTransitionWriteTest {

  private IPSWorkflowService workflowService;
  private IPSSteppedWorkflowService stepped;
  private WorkflowsAdaptor adaptor;
  private final AtomicLong ids = new AtomicLong(900);

  @BeforeEach
  void setUp() {
    PSRequestInfoBase.initRequestInfo(new HashMap<>());
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_JSESSIONID, "test-session");
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_USER, "Admin");
    workflowService = mock(IPSWorkflowService.class);
    stepped = mock(IPSSteppedWorkflowService.class);
    when(workflowService.createTransition(any(), any()))
        .thenAnswer(
            inv -> {
              PSTransition created = new PSTransition();
              created.setGUID(new PSGuid(PSTypeEnum.WORKFLOW_TRANSITION, ids.incrementAndGet()));
              IPSGuid stateId = inv.getArgument(1);
              created.setStateId(stateId.longValue());
              created.setWorkflowId(7);
              return created;
            });
    adaptor =
        new WorkflowsAdaptor(
            mock(IPSContentDesignWs.class),
            workflowService,
            mock(IPSContentMgr.class),
            () -> true,
            stepped);
  }

  @AfterEach
  void tearDown() {
    PSRequestInfoBase.resetRequestInfo();
  }

  @Test
  void createsTransitionAndSavesWithoutAddingSteps() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    PSWorkflow wf = workflow("Nightly QA", draft, review);
    stub(wf);

    WorkflowGraph graph =
        adaptor.createWorkflowTransition(null, "Nightly QA", body("Draft", "Review", "Send"));

    assertEquals(2, graph.getNodes().size());
    assertEquals(1, graph.getEdges().size());
    assertEquals("Send", graph.getEdges().get(0).getLabel());
    assertEquals("Draft", graph.getEdges().get(0).getFrom());
    assertEquals("Review", graph.getEdges().get(0).getTo());
    assertEquals(2, wf.getStates().size());
    verify(workflowService).saveWorkflow(wf);
    verify(workflowService).createTransition(any(), any());
  }

  @Test
  void duplicateIs409AndDoesNotSave() {
    PSState draft = state(1, "Draft");
    draft.addTransition(transition("Submit", 2));
    PSState review = state(2, "Review");
    PSWorkflow wf = workflow("Nightly QA", draft, review);
    stub(wf);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                adaptor.createWorkflowTransition(
                    null, "Nightly QA", body("Draft", "Review", "Submit")));
    assertEquals(409, ex.getResponse().getStatus());
    assertEquals(1, draft.getTransitions().size());
    verify(workflowService, never()).saveWorkflow(wf);
  }

  @Test
  void packagedCreateIs403() {
    PSWorkflow wf = workflow("Simple Workflow", state(1, "Draft"), state(2, "Review"));
    stub(wf);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                adaptor.createWorkflowTransition(
                    null, "Simple Workflow", body("Draft", "Review", "Send")));
    assertEquals(403, ex.getResponse().getStatus());
    verify(workflowService, never()).saveWorkflow(wf);
    verify(workflowService, never()).createTransition(any(), any());
  }

  @Test
  void missingWorkflowIs404() {
    when(workflowService.findWorkflowsByName("Missing")).thenReturn(List.of());
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.createWorkflowTransition(null, "Missing", body("Draft", "Review", "Send")));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  void updatesLabelAndDestination() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    PSState pending = state(3, "Pending");
    draft.addTransition(transition("Submit", 2));
    PSWorkflow wf = workflow("Nightly QA", draft, review, pending);
    stub(wf);

    WorkflowGraph graph =
        adaptor.updateWorkflowTransition(
            null, "Nightly QA", "Draft", "Submit", "Review", body(null, "Pending", "Send"));

    assertEquals(3, graph.getNodes().size());
    assertEquals(1, graph.getEdges().size());
    assertEquals("Send", graph.getEdges().get(0).getLabel());
    assertEquals("Pending", graph.getEdges().get(0).getTo());
    assertEquals("Send", draft.getTransitions().get(0).getLabel());
    verify(workflowService).saveWorkflow(wf);
    verify(workflowService, never()).createTransition(any(), any());
  }

  @Test
  void updateMissingTransitionDoesNotSave() {
    PSState draft = state(1, "Draft");
    draft.addTransition(transition("Submit", 2));
    PSWorkflow wf = workflow("Nightly QA", draft, state(2, "Review"));
    stub(wf);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                adaptor.updateWorkflowTransition(
                    null, "Nightly QA", "Draft", "Publish", null, body(null, "Review", "Send")));
    assertEquals(404, ex.getResponse().getStatus());
    verify(workflowService, never()).saveWorkflow(wf);
  }

  @Test
  void nonAdminIs403() {
    WorkflowsAdaptor locked =
        new WorkflowsAdaptor(
            mock(IPSContentDesignWs.class),
            workflowService,
            mock(IPSContentMgr.class),
            () -> false,
            stepped);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> locked.createWorkflowTransition(null, "Nightly QA", body("Draft", "Review", "Send")));
    assertEquals(403, ex.getResponse().getStatus());
    verify(workflowService, never()).saveWorkflow(any());
  }

  private void stub(PSWorkflow wf) {
    when(workflowService.findWorkflowsByName(wf.getName())).thenReturn(List.of(wf));
    PSUiWorkflow ui = new PSUiWorkflow();
    ui.setDefaultWorkflow(false);
    when(stepped.getWorkflow(wf.getName())).thenReturn(ui);
  }

  private static WorkflowTransitionWrite body(String from, String to, String label) {
    WorkflowTransitionWrite body = new WorkflowTransitionWrite();
    body.setFrom(from);
    body.setTo(to);
    body.setLabel(label);
    return body;
  }

  private static PSWorkflow workflow(String name, PSState... states) {
    PSWorkflow wf = mock(PSWorkflow.class);
    when(wf.getName()).thenReturn(name);
    when(wf.getStates()).thenReturn(List.of(states));
    when(wf.getGUID()).thenReturn(new PSGuid(PSTypeEnum.WORKFLOW, 7));
    return wf;
  }

  private static PSState state(long id, String name) {
    PSState state = new PSState();
    state.setStateId(id);
    state.setName(name);
    state.setWorkflowId(7);
    return state;
  }

  private static PSTransition transition(String label, long toState) {
    PSTransition transition = new PSTransition();
    transition.setLabel(label);
    transition.setTrigger(label);
    transition.setToState(toState);
    return transition;
  }
}
