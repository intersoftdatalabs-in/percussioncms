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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class WorkflowsAdaptorStepDeleteTest {

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
  void deletesStepWithNoTransitions() {
    PSState draft = state(1, "Draft");
    draft.addTransition(transition("Submit", 1));
    PSState orphan = state(9, "Orphan");
    List<PSState> states = new ArrayList<>(List.of(draft, orphan));
    PSWorkflow wf = workflow("Nightly QA", states);
    stub(wf);

    WorkflowGraph graph = adaptor.deleteWorkflowStep(null, "Nightly QA", "Orphan");

    assertEquals(1, graph.getNodes().size());
    assertEquals("Draft", graph.getNodes().get(0).getName());
    assertEquals(1, states.size());
    assertEquals("Draft", states.get(0).getName());
    verify(workflowService).saveWorkflow(wf);
  }

  @Test
  void outgoingTransitionIs409() {
    PSState orphan = state(9, "Orphan");
    orphan.addTransition(transition("Submit", 1));
    List<PSState> states = new ArrayList<>(List.of(state(1, "Draft"), orphan));
    PSWorkflow wf = workflow("Nightly QA", states);
    stub(wf);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.deleteWorkflowStep(null, "Nightly QA", "Orphan"));
    assertEquals(409, ex.getResponse().getStatus());
    assertEquals(2, states.size());
    verify(workflowService, never()).saveWorkflow(wf);
  }

  @Test
  void incomingTransitionIs409() {
    PSState draft = state(1, "Draft");
    draft.addTransition(transition("Submit", 9));
    List<PSState> states = new ArrayList<>(List.of(draft, state(9, "Orphan")));
    PSWorkflow wf = workflow("Nightly QA", states);
    stub(wf);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.deleteWorkflowStep(null, "Nightly QA", "orphan"));
    assertEquals(409, ex.getResponse().getStatus());
    verify(workflowService, never()).saveWorkflow(wf);
  }

  @Test
  void incomingAgingTransitionIs409() {
    PSState live = state(4, "Live");
    PSAgingTransition expire = new PSAgingTransition();
    expire.setLabel("Expire");
    expire.setToState(5L);
    live.addAgingTransition(expire);
    List<PSState> states = new ArrayList<>(List.of(live, state(5, "Archive")));
    PSWorkflow wf = workflow("Nightly QA", states);
    stub(wf);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.deleteWorkflowStep(null, "Nightly QA", "Archive"));
    assertEquals(409, ex.getResponse().getStatus());
    verify(workflowService, never()).saveWorkflow(wf);
  }

  @Test
  void missingStepIs404() {
    List<PSState> states = new ArrayList<>(List.of(state(1, "Draft")));
    PSWorkflow wf = workflow("Nightly QA", states);
    stub(wf);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.deleteWorkflowStep(null, "Nightly QA", "Missing"));
    assertEquals(404, ex.getResponse().getStatus());
    verify(workflowService, never()).saveWorkflow(wf);
  }

  @Test
  void wildcardStepNameIs400() {
    PSWorkflow wf = workflow("Nightly QA", new ArrayList<>(List.of(state(1, "Draft"))));
    stub(wf);
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> adaptor.deleteWorkflowStep(null, "Nightly QA", "bad*name"));
    assertTrue(ex.getMessage().toLowerCase().contains("wildcard"));
    verify(workflowService, never()).saveWorkflow(wf);
  }

  @Test
  void packagedWorkflowIs403() {
    PSWorkflow wf = workflow("Simple Workflow", new ArrayList<>(List.of(state(1, "Draft"))));
    stub(wf);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.deleteWorkflowStep(null, "Simple Workflow", "Draft"));
    assertEquals(403, ex.getResponse().getStatus());
    verify(workflowService, never()).saveWorkflow(wf);
  }

  @Test
  void missingWorkflowIs404() {
    when(workflowService.findWorkflowsByName("Missing")).thenReturn(List.of());
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.deleteWorkflowStep(null, "Missing", "Draft"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  private void stub(PSWorkflow wf) {
    when(workflowService.findWorkflowsByName(wf.getName())).thenReturn(List.of(wf));
    PSUiWorkflow ui = new PSUiWorkflow();
    ui.setDefaultWorkflow(false);
    when(stepped.getWorkflow(wf.getName())).thenReturn(ui);
  }

  private static PSWorkflow workflow(String name, List<PSState> states) {
    PSWorkflow wf = mock(PSWorkflow.class);
    when(wf.getName()).thenReturn(name);
    when(wf.getStates()).thenReturn(states);
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
