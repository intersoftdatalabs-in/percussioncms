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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.workflows.WorkflowAgingIntervalWrite;
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
  void changesAbsoluteIntervalAndListsTheNewMinutes() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    PSWorkflow wf = workflow("Nightly QA", draft, review);
    stub(wf, false);
    adaptor.createAbsoluteAgingTransition(null, "Nightly QA", body("Draft", "Review", 15));

    WorkflowGraph graph =
        adaptor.changeAbsoluteAgingInterval(null, "Nightly QA", intervalBody("Draft", "Review", 15, 30));

    WorkflowGraph.Edge edge = graph.getEdges().get(0);
    assertTrue(edge.isAging());
    assertEquals(30L, edge.getIntervalMinutes());
    assertEquals("Aging 30", edge.getLabel());
    assertEquals("Review", edge.getTo());
    assertEquals(1, draft.getAgingTransitions().size());
    assertEquals(30L, draft.getAgingTransitions().get(0).getInterval());
    assertEquals(PSAgingTransition.PSAgingTypeEnum.ABSOLUTE, draft.getAgingTransitions().get(0).getAgingTypeEnum());
    assertEquals(2L, draft.getAgingTransitions().get(0).getToState());
    assertEquals(0, draft.getTransitions().size());
    assertEquals(2, wf.getStates().size());
    verify(workflowService, times(2)).saveWorkflow(wf);
    verify(workflowService, times(1)).createTransition(any(), any());
  }

  @Test
  void unchangedOrNonPositiveIntervalDoesNotSave() {
    PSState draft = state(1, "Draft");
    PSWorkflow wf = workflow("Nightly QA", draft, state(2, "Review"));
    stub(wf, false);
    adaptor.createAbsoluteAgingTransition(null, "Nightly QA", body("Draft", "Review", 15));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            adaptor.changeAbsoluteAgingInterval(
                null, "Nightly QA", intervalBody("Draft", "Review", 15, 0)));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            adaptor.changeAbsoluteAgingInterval(
                null, "Nightly QA", intervalBody("Draft", "Review", 15, 15)));
    assertEquals(15L, draft.getAgingTransitions().get(0).getInterval());
    verify(workflowService, times(1)).saveWorkflow(wf);
  }

  @Test
  void duplicateNewIntervalDoesNotSaveAgain() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    PSWorkflow wf = workflow("Nightly QA", draft, review);
    stub(wf, false);
    adaptor.createAbsoluteAgingTransition(null, "Nightly QA", body("Draft", "Review", 15));
    adaptor.createAbsoluteAgingTransition(null, "Nightly QA", body("Draft", "Review", 45));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                adaptor.changeAbsoluteAgingInterval(
                    null, "Nightly QA", intervalBody("Draft", "Review", 15, 45)));
    assertEquals(409, ex.getResponse().getStatus());
    assertEquals(15L, draft.getAgingTransitions().get(0).getInterval());
    assertEquals(45L, draft.getAgingTransitions().get(1).getInterval());
    verify(workflowService, times(2)).saveWorkflow(wf);
  }

  @Test
  void packagedIntervalChangeIs403() {
    PSWorkflow wf = workflow("Simple Workflow", state(1, "Draft"), state(2, "Review"));
    stub(wf, false);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                adaptor.changeAbsoluteAgingInterval(
                    null, "Simple Workflow", intervalBody("Draft", "Review", 15, 30)));
    assertEquals(403, ex.getResponse().getStatus());
    verify(workflowService, never()).saveWorkflow(wf);
  }

  @Test
  void missingAgingIntervalIs404() {
    PSWorkflow wf = workflow("Nightly QA", state(1, "Draft"), state(2, "Review"));
    stub(wf, false);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                adaptor.changeAbsoluteAgingInterval(
                    null, "Nightly QA", intervalBody("Draft", "Review", 15, 30)));
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
            () -> locked.createAbsoluteAgingTransition(null, "Nightly QA", body("Draft", "Review", 15)));
    assertEquals(403, ex.getResponse().getStatus());
    verify(workflowService, never()).saveWorkflow(any());
  }

  @Test
  void deletesAbsoluteAgingAndLeavesTheRegularTransition() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    draft.addTransition(regular("Submit", 2));
    PSWorkflow wf = workflow("Nightly QA", draft, review);
    stub(wf, false);
    adaptor.createAbsoluteAgingTransition(null, "Nightly QA", body("Draft", "Review", 15));

    WorkflowGraph graph =
        adaptor.deleteAbsoluteAgingTransition(null, "Nightly QA", "Draft", "Review", 15);

    assertEquals(2, graph.getNodes().size());
    assertEquals(1, graph.getEdges().size());
    assertFalse(graph.getEdges().get(0).isAging());
    assertEquals("Submit", graph.getEdges().get(0).getLabel());
    assertTrue(draft.getAgingTransitions().isEmpty());
    assertEquals(1, draft.getTransitions().size());
    assertEquals("Submit", draft.getTransitions().get(0).getLabel());
    assertEquals(2, wf.getStates().size());
    verify(workflowService, times(2)).saveWorkflow(wf);
  }

  @Test
  void deletesOnlyTheNamedAbsoluteInterval() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    draft.addTransition(regular("Submit", 2));
    PSWorkflow wf = workflow("Nightly QA", draft, review);
    stub(wf, false);
    adaptor.createAbsoluteAgingTransition(null, "Nightly QA", body("Draft", "Review", 15));
    adaptor.createAbsoluteAgingTransition(null, "Nightly QA", body("Draft", "Review", 45));

    WorkflowGraph graph =
        adaptor.deleteAbsoluteAgingTransition(null, "Nightly QA", "Draft", "Review", 15);

    assertEquals(1, draft.getAgingTransitions().size());
    assertEquals(45L, draft.getAgingTransitions().get(0).getInterval());
    assertEquals(1, draft.getTransitions().size());
    assertEquals(2, graph.getEdges().size());
    verify(workflowService, times(3)).saveWorkflow(wf);
  }

  @Test
  void repeatedAgingIs409AndIsNotDeleted() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    PSAgingTransition repeated = new PSAgingTransition();
    repeated.setType(PSAgingTransition.PSAgingTypeEnum.REPEATED);
    repeated.setInterval(15);
    repeated.setToState(2);
    repeated.setLabel("Repeat");
    draft.addAgingTransition(repeated);
    draft.addTransition(regular("Submit", 2));
    PSWorkflow wf = workflow("Nightly QA", draft, review);
    stub(wf, false);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.deleteAbsoluteAgingTransition(null, "Nightly QA", "Draft", "Review", 15));

    assertEquals(409, ex.getResponse().getStatus());
    assertEquals(1, draft.getAgingTransitions().size());
    assertEquals(PSAgingTransition.PSAgingTypeEnum.REPEATED, draft.getAgingTransitions().get(0).getAgingTypeEnum());
    assertEquals(1, draft.getTransitions().size());
    verify(workflowService, never()).saveWorkflow(wf);
  }

  @Test
  void missingAgingDeleteIs404() {
    PSWorkflow wf = workflow("Nightly QA", state(1, "Draft"), state(2, "Review"));
    stub(wf, false);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.deleteAbsoluteAgingTransition(null, "Nightly QA", "Draft", "Review", 15));
    assertEquals(404, ex.getResponse().getStatus());
    verify(workflowService, never()).saveWorkflow(wf);
  }

  @Test
  void nonPositiveAgingDeleteDoesNotSave() {
    PSState draft = state(1, "Draft");
    PSWorkflow wf = workflow("Nightly QA", draft, state(2, "Review"));
    stub(wf, false);
    adaptor.createAbsoluteAgingTransition(null, "Nightly QA", body("Draft", "Review", 15));
    assertThrows(
        IllegalArgumentException.class,
        () -> adaptor.deleteAbsoluteAgingTransition(null, "Nightly QA", "Draft", "Review", 0));
    assertEquals(1, draft.getAgingTransitions().size());
    verify(workflowService, times(1)).saveWorkflow(wf);
  }

  @Test
  void packagedAgingDeleteIs403() {
    PSWorkflow wf = workflow("Simple Workflow", state(1, "Draft"), state(2, "Review"));
    stub(wf, false);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                adaptor.deleteAbsoluteAgingTransition(null, "Simple Workflow", "Draft", "Review", 15));
    assertEquals(403, ex.getResponse().getStatus());
    verify(workflowService, never()).saveWorkflow(wf);
  }

  private static PSTransition regular(String label, long toState) {
    PSTransition transition = new PSTransition();
    transition.setLabel(label);
    transition.setTrigger(label);
    transition.setToState(toState);
    return transition;
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

  private static WorkflowAgingIntervalWrite intervalBody(
      String from, String to, long currentMinutes, long newMinutes) {
    WorkflowAgingIntervalWrite body = new WorkflowAgingIntervalWrite();
    body.setFrom(from);
    body.setTo(to);
    body.setIntervalMinutes(currentMinutes);
    body.setNewIntervalMinutes(newMinutes);
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
