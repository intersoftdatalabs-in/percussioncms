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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.workflows.WorkflowAgingTransitionWrite;
import com.percussion.rest.workflows.WorkflowGraph;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.services.workflow.data.PSAgingTransition;
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
class WorkflowsAdaptorAgingWriteTest {

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
  void createsAbsoluteAgingAndListsItOnTheGraph() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    PSWorkflow wf = workflow("Nightly QA", draft, review);
    stub(wf, false);

    WorkflowGraph graph =
        adaptor.createAbsoluteAgingTransition(null, "Nightly QA", body("Draft", "Review", 15));

    assertEquals(2, graph.getNodes().size());
    assertEquals(1, graph.getEdges().size());
    WorkflowGraph.Edge edge = graph.getEdges().get(0);
    assertTrue(edge.isAging());
    assertEquals(15L, edge.getIntervalMinutes());
    assertEquals("Aging 15", edge.getLabel());
    assertEquals("Draft", edge.getFrom());
    assertEquals("Review", edge.getTo());
    assertEquals(2, wf.getStates().size());
    assertEquals(0, draft.getTransitions().size());
    PSAgingTransition saved = draft.getAgingTransitions().get(0);
    assertEquals(15L, saved.getInterval());
    assertEquals(PSAgingTransition.PSAgingTypeEnum.ABSOLUTE, saved.getAgingTypeEnum());
    verify(workflowService).saveWorkflow(wf);
    verify(workflowService).createTransition(any(), any());
  }

  @Test
  void duplicateDoesNotSaveAgain() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    PSWorkflow wf = workflow("Nightly QA", draft, review);
    stub(wf, false);
    adaptor.createAbsoluteAgingTransition(null, "Nightly QA", body("Draft", "Review", 15));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                adaptor.createAbsoluteAgingTransition(
                    null, "Nightly QA", body("Draft", "Review", 15)));
    assertEquals(409, ex.getResponse().getStatus());
    assertEquals(1, draft.getAgingTransitions().size());
    verify(workflowService, times(1)).saveWorkflow(wf);
  }

  @Test
  void nonPositiveDoesNotSave() {
    PSState draft = state(1, "Draft");
    PSWorkflow wf = workflow("Nightly QA", draft, state(2, "Review"));
    stub(wf, false);
    assertThrows(
        IllegalArgumentException.class,
        () -> adaptor.createAbsoluteAgingTransition(null, "Nightly QA", body("Draft", "Review", 0)));
    verify(workflowService, never()).saveWorkflow(wf);
    verify(workflowService, never()).createTransition(any(), any());
  }

  @Test
  void packagedCreateIs403() {
    PSWorkflow wf = workflow("Simple Workflow", state(1, "Draft"), state(2, "Review"));
    stub(wf, false);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                adaptor.createAbsoluteAgingTransition(
                    null, "Simple Workflow", body("Draft", "Review", 15)));
    assertEquals(403, ex.getResponse().getStatus());
    verify(workflowService, never()).saveWorkflow(wf);
    verify(workflowService, never()).createTransition(any(), any());
  }

  @Test
  void defaultWorkflowIs403() {
    PSWorkflow wf = workflow("Nightly QA", state(1, "Draft"), state(2, "Review"));
    stub(wf, true);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                adaptor.createAbsoluteAgingTransition(
                    null, "Nightly QA", body("Draft", "Review", 15)));
    assertEquals(403, ex.getResponse().getStatus());
    verify(workflowService, never()).saveWorkflow(any());
  }

  @Test
  void missingWorkflowIs404() {
    when(workflowService.findWorkflowsByName("Missing")).thenReturn(List.of());
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.createAbsoluteAgingTransition(null, "Missing", body("Draft", "Review", 15)));
    assertEquals(404, ex.getResponse().getStatus());
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
            () -> locked.createAbsoluteAgingTransition(null, "Nightly QA", body("Draft", "Review", 15)));
    assertEquals(403, ex.getResponse().getStatus());
    verify(workflowService, never()).saveWorkflow(any());
  }

  private void stub(PSWorkflow wf, boolean defaultWorkflow) {
    when(workflowService.findWorkflowsByName(wf.getName())).thenReturn(List.of(wf));
    PSUiWorkflow ui = new PSUiWorkflow();
    ui.setDefaultWorkflow(defaultWorkflow);
    when(stepped.getWorkflow(wf.getName())).thenReturn(ui);
  }

  private static WorkflowAgingTransitionWrite body(String from, String to, long minutes) {
    WorkflowAgingTransitionWrite body = new WorkflowAgingTransitionWrite();
    body.setFrom(from);
    body.setTo(to);
    body.setIntervalMinutes(minutes);
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
}
