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
import static org.mockito.Mockito.mock;
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
class WorkflowsAdaptorGraphTest {

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
  void graph_projectsStatesAndMarksPackaged() throws Exception {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    PSTransition submit = mock(PSTransition.class);
    when(submit.getLabel()).thenReturn("Submit");
    when(submit.getToState()).thenReturn(2L);
    when(draft.getTransitions()).thenReturn(List.of(submit));
    when(draft.getAgingTransitions()).thenReturn(List.of());
    PSAgingTransition age = mock(PSAgingTransition.class);
    when(age.getLabel()).thenReturn("Expire");
    when(age.getToState()).thenReturn(1L);
    when(review.getTransitions()).thenReturn(List.of());
    when(review.getAgingTransitions()).thenReturn(List.of(age));

    PSWorkflow wf = mock(PSWorkflow.class);
    when(wf.getName()).thenReturn("Simple Workflow");
    when(wf.getStates()).thenReturn(List.of(review, draft));
    when(workflowService.findWorkflowsByName("Simple Workflow")).thenReturn(List.of(wf));
    PSUiWorkflow ui = new PSUiWorkflow();
    ui.setDefaultWorkflow(false);
    when(stepped.getWorkflow("Simple Workflow")).thenReturn(ui);

    WorkflowGraph graph = adaptor.getWorkflowGraph(null, "Simple Workflow");
    assertTrue(graph.isPackaged());
    assertFalse(graph.isDefaultWorkflow());
    assertEquals(List.of("Draft", "Review"), graph.getNodes().stream().map(WorkflowGraph.Node::getName).toList());
    assertEquals(2, graph.getEdges().size());
    assertEquals("Draft", graph.getEdges().get(0).getFrom());
    assertEquals("Review", graph.getEdges().get(0).getTo());
    assertEquals("Submit", graph.getEdges().get(0).getLabel());
    assertEquals("Expire", graph.getEdges().get(1).getLabel());
  }

  @Test
  void graph_customDefaultFlagIsPackaged() throws Exception {
    PSWorkflow wf = mock(PSWorkflow.class);
    when(wf.getName()).thenReturn("Nightly QA");
    when(wf.getStates()).thenReturn(List.of());
    when(workflowService.findWorkflowsByName("Nightly QA")).thenReturn(List.of(wf));
    PSUiWorkflow ui = new PSUiWorkflow();
    ui.setDefaultWorkflow(true);
    when(stepped.getWorkflow("Nightly QA")).thenReturn(ui);

    WorkflowGraph graph = adaptor.getWorkflowGraph(null, "Nightly QA");
    assertTrue(graph.isPackaged());
    assertTrue(graph.isDefaultWorkflow());
    assertTrue(graph.getNodes().isEmpty());
  }

  @Test
  void graph_missingIs404() {
    when(workflowService.findWorkflowsByName("missing")).thenReturn(List.of());
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.getWorkflowGraph(null, "missing"));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  void graph_unknownToStateAddsSyntheticNode() {
    PSState draft = state(1, "Draft");
    PSTransition jump = mock(PSTransition.class);
    when(jump.getLabel()).thenReturn("Jump");
    when(jump.getToState()).thenReturn(99L);
    when(draft.getTransitions()).thenReturn(List.of(jump));
    when(draft.getAgingTransitions()).thenReturn(List.of());
    PSWorkflow wf = mock(PSWorkflow.class);
    when(wf.getName()).thenReturn("Nightly QA");
    when(wf.getStates()).thenReturn(List.of(draft));
    when(workflowService.findWorkflowsByName("Nightly QA")).thenReturn(List.of(wf));

    WorkflowGraph graph = adaptor.getWorkflowGraph(null, "Nightly QA");
    assertFalse(graph.isPackaged());
    assertEquals("state-99", graph.getEdges().get(0).getTo());
    assertEquals(2, graph.getNodes().size());
  }

  private static PSState state(long id, String name) {
    PSState state = mock(PSState.class);
    when(state.getStateId()).thenReturn(id);
    when(state.getName()).thenReturn(name);
    return state;
  }
}
