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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.workflows.WorkflowGraph;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.services.workflow.data.PSAgingTransition;
import com.percussion.services.workflow.data.PSState;
import com.percussion.services.workflow.data.PSTransition;
import com.percussion.services.workflow.data.PSWorkflow;
import com.percussion.utils.request.PSRequestInfoBase;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.workflow.data.PSUiWorkflow;
import com.percussion.workflow.service.IPSSteppedWorkflowService;
import jakarta.ws.rs.WebApplicationException;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class WorkflowsAdaptorTransitionDeleteTest {

  private IPSWorkflowService workflowService;
  private IPSSteppedWorkflowService stepped;
  private WorkflowsAdaptor adaptor;

  @BeforeEach
  void setUp() {
    PSRequestInfoBase.initRequestInfo(new HashMap<>());
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_JSESSIONID, "test-session");
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_USER, "Admin");
    workflowService = mock(IPSWorkflowService.class);
    stepped = mock(IPSSteppedWorkflowService.class);
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
  void deletesOneTransitionAndKeepsSteps() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    PSTransition submit = transition("Submit", 2);
    PSTransition reject = transition("Reject", 1);
    draft.addTransition(submit);
    draft.addTransition(reject);
    PSWorkflow wf = workflow("Nightly QA", draft, review);
    stub(wf);

    WorkflowGraph graph =
        adaptor.deleteWorkflowTransition(null, "Nightly QA", "Draft", "Submit", "Review");

    assertEquals(2, graph.getNodes().size());
    assertEquals(1, graph.getEdges().size());
    assertEquals("Reject", graph.getEdges().get(0).getLabel());
    assertEquals(1, draft.getTransitions().size());
    assertEquals(2, wf.getStates().size());
    verify(workflowService).saveWorkflow(wf);
  }

  @Test
  void deletesAgingTransitionWithoutRemovingStates() {
    PSState live = state(4, "Live");
    PSState archive = state(5, "Archive");
    PSAgingTransition expire = new PSAgingTransition();
    expire.setLabel("Expire");
    expire.setTrigger("Expire");
    expire.setToState(5L);
    live.addAgingTransition(expire);
    PSWorkflow wf = workflow("Nightly QA", live, archive);
    stub(wf);

    WorkflowGraph graph =
        adaptor.deleteWorkflowTransition(null, "Nightly QA", "Live", "Expire", "Archive");

    assertTrue(graph.getEdges().isEmpty());
    assertEquals(2, graph.getNodes().size());
    assertTrue(live.getAgingTransitions().isEmpty());
    verify(workflowService).saveWorkflow(wf);
  }

  @Test
  void missingTransitionIs404AndDoesNotSave() {
    PSState draft = state(1, "Draft");
    draft.addTransition(transition("Submit", 2));
    PSState review = state(2, "Review");
    PSWorkflow wf = workflow("Nightly QA", draft, review);
    stub(wf);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.deleteWorkflowTransition(null, "Nightly QA", "Draft", "Publish", null));
    assertEquals(404, ex.getResponse().getStatus());
    assertEquals(1, draft.getTransitions().size());
    verify(workflowService, never()).saveWorkflow(wf);
  }

  @Test
  void ambiguousLabelWithoutDestinationIs400() {
    PSState draft = state(1, "Draft");
    draft.addTransition(transition("Submit", 2));
    draft.addTransition(transition("Submit", 3));
    PSWorkflow wf = workflow("Nightly QA", draft, state(2, "Review"), state(3, "Pending"));
    stub(wf);

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> adaptor.deleteWorkflowTransition(null, "Nightly QA", "Draft", "Submit", null));
    assertTrue(ex.getMessage().toLowerCase().contains("specify to"));
    verify(workflowService, never()).saveWorkflow(wf);
  }

  @Test
  void packagedWorkflowIs403() {
    PSWorkflow wf = workflow("Simple Workflow", state(1, "Draft"));
    stub(wf);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                adaptor.deleteWorkflowTransition(
                    null, "Simple Workflow", "Draft", "Submit", "Review"));
    assertEquals(403, ex.getResponse().getStatus());
    verify(workflowService, never()).saveWorkflow(wf);
  }

  @Test
  void missingWorkflowIs404() {
    when(workflowService.findWorkflowsByName("Missing")).thenReturn(List.of());
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.deleteWorkflowTransition(null, "Missing", "Draft", "Submit", null));
    assertEquals(404, ex.getResponse().getStatus());
  }

  private void stub(PSWorkflow wf) {
    when(workflowService.findWorkflowsByName(wf.getName())).thenReturn(List.of(wf));
    PSUiWorkflow ui = new PSUiWorkflow();
    ui.setDefaultWorkflow(false);
    when(stepped.getWorkflow(wf.getName())).thenReturn(ui);
  }

  private static PSWorkflow workflow(String name, PSState... states) {
    PSWorkflow wf = mock(PSWorkflow.class);
    when(wf.getName()).thenReturn(name);
    when(wf.getStates()).thenReturn(List.of(states));
    return wf;
  }

  private static PSState state(long id, String name) {
    PSState state = new PSState();
    state.setStateId(id);
    state.setName(name);
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
