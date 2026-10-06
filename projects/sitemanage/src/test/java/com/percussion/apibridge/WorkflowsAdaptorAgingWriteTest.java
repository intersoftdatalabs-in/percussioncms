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
import com.percussion.rest.workflows.WorkflowAgingSystemFieldWrite;
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
  void createsRepeatedAgingBesideAnAbsoluteRow() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    PSWorkflow wf = workflow("Nightly QA", draft, review);
    stub(wf, false);
    adaptor.createAbsoluteAgingTransition(null, "Nightly QA", body("Draft", "Review", 15));

    WorkflowAgingTransitionWrite repeated = body("Draft", "Review", 15);
    repeated.setType("REPEATED");
    WorkflowGraph graph = adaptor.createAbsoluteAgingTransition(null, "Nightly QA", repeated);

    assertEquals(2, graph.getEdges().size());
    WorkflowGraph.Edge absolute =
        graph.getEdges().stream().filter(edge -> "ABSOLUTE".equals(edge.getAgingType())).findFirst().orElseThrow();
    WorkflowGraph.Edge repeatedEdge =
        graph.getEdges().stream().filter(edge -> "REPEATED".equals(edge.getAgingType())).findFirst().orElseThrow();
    assertTrue(absolute.isAging());
    assertEquals(15L, absolute.getIntervalMinutes());
    assertEquals("Aging 15", absolute.getLabel());
    assertTrue(repeatedEdge.isAging());
    assertEquals(15L, repeatedEdge.getIntervalMinutes());
    assertEquals("Repeated aging 15", repeatedEdge.getLabel());
    assertEquals("Draft", repeatedEdge.getFrom());
    assertEquals("Review", repeatedEdge.getTo());
    assertEquals(2, draft.getAgingTransitions().size());
    assertEquals(0, draft.getTransitions().size());
    assertEquals(PSAgingTransition.PSAgingTypeEnum.ABSOLUTE, draft.getAgingTransitions().get(0).getAgingTypeEnum());
    assertEquals(PSAgingTransition.PSAgingTypeEnum.REPEATED, draft.getAgingTransitions().get(1).getAgingTypeEnum());
    assertEquals(2, wf.getStates().size());
    verify(workflowService, times(2)).saveWorkflow(wf);
  }

  @Test
  void omittedTypeStaysAbsolute() {
    PSState draft = state(1, "Draft");
    PSWorkflow wf = workflow("Nightly QA", draft, state(2, "Review"));
    stub(wf, false);
    WorkflowAgingTransitionWrite explicit = body("Draft", "Review", 20);
    explicit.setType("ABSOLUTE");
    WorkflowGraph omitted = adaptor.createAbsoluteAgingTransition(null, "Nightly QA", body("Draft", "Review", 15));
    WorkflowGraph named = adaptor.createAbsoluteAgingTransition(null, "Nightly QA", explicit);
    assertEquals("ABSOLUTE", omitted.getEdges().get(0).getAgingType());
    assertEquals("Aging 20", named.getEdges().get(1).getLabel());
    assertEquals(PSAgingTransition.PSAgingTypeEnum.ABSOLUTE, draft.getAgingTransitions().get(0).getAgingTypeEnum());
    assertEquals(PSAgingTransition.PSAgingTypeEnum.ABSOLUTE, draft.getAgingTransitions().get(1).getAgingTypeEnum());
  }

  @Test
  void duplicateRepeatedDoesNotSaveAgain() {
    PSState draft = state(1, "Draft");
    PSWorkflow wf = workflow("Nightly QA", draft, state(2, "Review"));
    stub(wf, false);
    WorkflowAgingTransitionWrite repeated = body("Draft", "Review", 15);
    repeated.setType("repeated");
    adaptor.createAbsoluteAgingTransition(null, "Nightly QA", repeated);
    WorkflowAgingTransitionWrite again = body("Draft", "Review", 15);
    again.setType("REPEATED");
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.createAbsoluteAgingTransition(null, "Nightly QA", again));
    assertEquals(409, ex.getResponse().getStatus());
    assertEquals(1, draft.getAgingTransitions().size());
    verify(workflowService, times(1)).saveWorkflow(wf);
  }

  @Test
  void blankOrUnknownSystemFieldDoesNotSave() {
    PSState draft = state(1, "Draft");
    PSWorkflow wf = workflow("Nightly QA", draft, state(2, "Review"));
    stub(wf, false);
    WorkflowAgingTransitionWrite blank = body("Draft", "Review", 0);
    blank.setType("SYSTEM_FIELD");
    assertThrows(
        IllegalArgumentException.class,
        () -> adaptor.createAbsoluteAgingTransition(null, "Nightly QA", blank));
    WorkflowAgingTransitionWrite unknown = body("Draft", "Review", 15);
    unknown.setType("SYSTEM_FIELD");
    unknown.setSystemField("sys_title");
    assertThrows(
        IllegalArgumentException.class,
        () -> adaptor.createAbsoluteAgingTransition(null, "Nightly QA", unknown));
    assertEquals(0, draft.getAgingTransitions().size());
    verify(workflowService, never()).saveWorkflow(wf);
    verify(workflowService, never()).createTransition(any(), any());
  }

  @Test
  void createsSystemFieldAgingBesideAbsoluteAndRepeatedRows() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    PSWorkflow wf = workflow("Nightly QA", draft, review);
    stub(wf, false);
    adaptor.createAbsoluteAgingTransition(null, "Nightly QA", body("Draft", "Review", 15));
    WorkflowAgingTransitionWrite repeated = body("Draft", "Review", 15);
    repeated.setType("REPEATED");
    adaptor.createAbsoluteAgingTransition(null, "Nightly QA", repeated);

    WorkflowAgingTransitionWrite system = new WorkflowAgingTransitionWrite();
    system.setFrom("Draft");
    system.setTo("Review");
    system.setType("system_field");
    system.setSystemField(" contentstartdate ");
    WorkflowGraph graph = adaptor.createAbsoluteAgingTransition(null, "Nightly QA", system);

    assertEquals(3, graph.getEdges().size());
    WorkflowGraph.Edge systemEdge =
        graph.getEdges().stream()
            .filter(edge -> "SYSTEM_FIELD".equals(edge.getAgingType()))
            .findFirst()
            .orElseThrow();
    assertTrue(systemEdge.isAging());
    assertEquals("CONTENTSTARTDATE", systemEdge.getSystemField());
    assertEquals(null, systemEdge.getIntervalMinutes());
    assertEquals("System field aging CONTENTSTARTDATE", systemEdge.getLabel());
    assertEquals("Draft", systemEdge.getFrom());
    assertEquals("Review", systemEdge.getTo());
    assertEquals(1, graph.getEdges().stream().filter(edge -> "ABSOLUTE".equals(edge.getAgingType())).count());
    assertEquals(1, graph.getEdges().stream().filter(edge -> "REPEATED".equals(edge.getAgingType())).count());
    assertEquals(3, draft.getAgingTransitions().size());
    assertEquals(0, draft.getTransitions().size());
    PSAgingTransition saved = draft.getAgingTransitions().get(2);
    assertEquals(PSAgingTransition.PSAgingTypeEnum.SYSTEM_FIELD, saved.getAgingTypeEnum());
    assertEquals("CONTENTSTARTDATE", saved.getSystemField());
    assertEquals(2, wf.getStates().size());
    verify(workflowService, times(3)).saveWorkflow(wf);
  }

  @Test
  void duplicateSystemFieldDoesNotSaveAgain() {
    PSState draft = state(1, "Draft");
    PSWorkflow wf = workflow("Nightly QA", draft, state(2, "Review"));
    stub(wf, false);
    WorkflowAgingTransitionWrite system = systemBody("Draft", "Review", "REMINDERDATE");
    adaptor.createAbsoluteAgingTransition(null, "Nightly QA", system);
    WorkflowAgingTransitionWrite again = systemBody("Draft", "Review", "reminderdate");
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.createAbsoluteAgingTransition(null, "Nightly QA", again));
    assertEquals(409, ex.getResponse().getStatus());
    assertEquals(1, draft.getAgingTransitions().size());
    verify(workflowService, times(1)).saveWorkflow(wf);
  }

  @Test
  void packagedSystemFieldCreateIs403() {
    PSWorkflow wf = workflow("Simple Workflow", state(1, "Draft"), state(2, "Review"));
    stub(wf, false);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                adaptor.createAbsoluteAgingTransition(
                    null, "Simple Workflow", systemBody("Draft", "Review", "CONTENTEXPIRYDATE")));
    assertEquals(403, ex.getResponse().getStatus());
    verify(workflowService, never()).saveWorkflow(wf);
    verify(workflowService, never()).createTransition(any(), any());
  }

  @Test
  void packagedRepeatedCreateIs403() {
    PSWorkflow wf = workflow("Simple Workflow", state(1, "Draft"), state(2, "Review"));
    stub(wf, false);
    WorkflowAgingTransitionWrite repeated = body("Draft", "Review", 15);
    repeated.setType("REPEATED");
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.createAbsoluteAgingTransition(null, "Simple Workflow", repeated));
    assertEquals(403, ex.getResponse().getStatus());
    verify(workflowService, never()).saveWorkflow(wf);
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
  void changesRepeatedIntervalAndLeavesTheAbsoluteEdge() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    PSWorkflow wf = workflow("Nightly QA", draft, review);
    stub(wf, false);
    adaptor.createAbsoluteAgingTransition(null, "Nightly QA", body("Draft", "Review", 15));
    WorkflowAgingTransitionWrite repeated = body("Draft", "Review", 15);
    repeated.setType("REPEATED");
    adaptor.createAbsoluteAgingTransition(null, "Nightly QA", repeated);

    WorkflowAgingIntervalWrite change = intervalBody("Draft", "Review", 15, 30);
    change.setType("REPEATED");
    WorkflowGraph graph = adaptor.changeAbsoluteAgingInterval(null, "Nightly QA", change);

    PSAgingTransition absolute =
        draft.getAgingTransitions().stream()
            .filter(edge -> edge.getAgingTypeEnum() == PSAgingTransition.PSAgingTypeEnum.ABSOLUTE)
            .findFirst()
            .orElseThrow();
    PSAgingTransition updated =
        draft.getAgingTransitions().stream()
            .filter(edge -> edge.getAgingTypeEnum() == PSAgingTransition.PSAgingTypeEnum.REPEATED)
            .findFirst()
            .orElseThrow();
    assertEquals(15L, absolute.getInterval());
    assertEquals("Aging 15", absolute.getLabel());
    assertEquals(30L, updated.getInterval());
    assertEquals("Repeated aging 30", updated.getLabel());
    assertEquals(PSAgingTransition.PSAgingTypeEnum.REPEATED, updated.getAgingTypeEnum());
    assertEquals(2L, updated.getToState());
    assertEquals(0, draft.getTransitions().size());
    assertEquals(2, wf.getStates().size());
    WorkflowGraph.Edge repeatedEdge =
        graph.getEdges().stream()
            .filter(edge -> "REPEATED".equals(edge.getAgingType()))
            .findFirst()
            .orElseThrow();
    WorkflowGraph.Edge absoluteEdge =
        graph.getEdges().stream()
            .filter(edge -> "ABSOLUTE".equals(edge.getAgingType()))
            .findFirst()
            .orElseThrow();
    assertEquals(30L, repeatedEdge.getIntervalMinutes());
    assertEquals("Repeated aging 30", repeatedEdge.getLabel());
    assertEquals(15L, absoluteEdge.getIntervalMinutes());
    verify(workflowService, times(3)).saveWorkflow(wf);
  }

  @Test
  void repeatedIntervalChangeDoesNotTouchAnAbsoluteOnlyEdge() {
    PSState draft = state(1, "Draft");
    PSWorkflow wf = workflow("Nightly QA", draft, state(2, "Review"));
    stub(wf, false);
    adaptor.createAbsoluteAgingTransition(null, "Nightly QA", body("Draft", "Review", 15));
    WorkflowAgingIntervalWrite change = intervalBody("Draft", "Review", 15, 30);
    change.setType("REPEATED");
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.changeAbsoluteAgingInterval(null, "Nightly QA", change));
    assertEquals(404, ex.getResponse().getStatus());
    assertEquals(15L, draft.getAgingTransitions().get(0).getInterval());
    assertEquals(
        PSAgingTransition.PSAgingTypeEnum.ABSOLUTE,
        draft.getAgingTransitions().get(0).getAgingTypeEnum());
    verify(workflowService, times(1)).saveWorkflow(wf);
  }

  @Test
  void duplicateRepeatedIntervalDoesNotSaveAndLeavesTheAbsoluteEdge() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    PSWorkflow wf = workflow("Nightly QA", draft, review);
    stub(wf, false);
    adaptor.createAbsoluteAgingTransition(null, "Nightly QA", body("Draft", "Review", 15));
    WorkflowAgingTransitionWrite first = body("Draft", "Review", 15);
    first.setType("REPEATED");
    WorkflowAgingTransitionWrite second = body("Draft", "Review", 45);
    second.setType("REPEATED");
    adaptor.createAbsoluteAgingTransition(null, "Nightly QA", first);
    adaptor.createAbsoluteAgingTransition(null, "Nightly QA", second);
    WorkflowAgingIntervalWrite change = intervalBody("Draft", "Review", 15, 45);
    change.setType("repeated");
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.changeAbsoluteAgingInterval(null, "Nightly QA", change));
    assertEquals(409, ex.getResponse().getStatus());
    assertEquals(15L, draft.getAgingTransitions().get(0).getInterval());
    assertEquals(15L, draft.getAgingTransitions().get(1).getInterval());
    assertEquals(45L, draft.getAgingTransitions().get(2).getInterval());
    assertEquals(
        PSAgingTransition.PSAgingTypeEnum.ABSOLUTE,
        draft.getAgingTransitions().get(0).getAgingTypeEnum());
    verify(workflowService, times(3)).saveWorkflow(wf);
  }

  @Test
  void systemFieldOrUnknownIntervalTypeDoesNotSave() {
    PSState draft = state(1, "Draft");
    PSWorkflow wf = workflow("Nightly QA", draft, state(2, "Review"));
    stub(wf, false);
    WorkflowAgingTransitionWrite repeated = body("Draft", "Review", 15);
    repeated.setType("REPEATED");
    adaptor.createAbsoluteAgingTransition(null, "Nightly QA", repeated);
    WorkflowAgingIntervalWrite system = intervalBody("Draft", "Review", 15, 30);
    system.setType("SYSTEM_FIELD");
    WorkflowAgingIntervalWrite unknown = intervalBody("Draft", "Review", 15, 30);
    unknown.setType("NOPE");
    assertThrows(
        IllegalArgumentException.class,
        () -> adaptor.changeAbsoluteAgingInterval(null, "Nightly QA", system));
    assertThrows(
        IllegalArgumentException.class,
        () -> adaptor.changeAbsoluteAgingInterval(null, "Nightly QA", unknown));
    assertEquals(15L, draft.getAgingTransitions().get(0).getInterval());
    assertEquals(
        PSAgingTransition.PSAgingTypeEnum.REPEATED,
        draft.getAgingTransitions().get(0).getAgingTypeEnum());
    verify(workflowService, times(1)).saveWorkflow(wf);
  }

  @Test
  void packagedRepeatedIntervalChangeIs403() {
    PSWorkflow wf = workflow("Simple Workflow", state(1, "Draft"), state(2, "Review"));
    stub(wf, false);
    WorkflowAgingIntervalWrite change = intervalBody("Draft", "Review", 15, 30);
    change.setType("REPEATED");
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.changeAbsoluteAgingInterval(null, "Simple Workflow", change));
    assertEquals(403, ex.getResponse().getStatus());
    verify(workflowService, never()).saveWorkflow(wf);
  }

  @Test
  void changesSystemFieldAndLeavesAbsoluteAndRepeatedRows() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    PSWorkflow wf = workflow("Nightly QA", draft, review);
    stub(wf, false);
    adaptor.createAbsoluteAgingTransition(null, "Nightly QA", body("Draft", "Review", 15));
    WorkflowAgingTransitionWrite repeated = body("Draft", "Review", 15);
    repeated.setType("REPEATED");
    adaptor.createAbsoluteAgingTransition(null, "Nightly QA", repeated);
    adaptor.createAbsoluteAgingTransition(
        null, "Nightly QA", systemBody("Draft", "Review", "CONTENTSTARTDATE"));
    adaptor.createAbsoluteAgingTransition(
        null, "Nightly QA", systemBody("Draft", "Review", "REMINDERDATE"));

    WorkflowGraph graph =
        adaptor.changeSystemFieldAging(
            null,
            "Nightly QA",
            fieldBody("Draft", "Review", " contentstartdate ", "CONTENTEXPIRYDATE"));

    PSAgingTransition absolute =
        draft.getAgingTransitions().stream()
            .filter(edge -> edge.getAgingTypeEnum() == PSAgingTransition.PSAgingTypeEnum.ABSOLUTE)
            .findFirst()
            .orElseThrow();
    PSAgingTransition repeatedEdge =
        draft.getAgingTransitions().stream()
            .filter(edge -> edge.getAgingTypeEnum() == PSAgingTransition.PSAgingTypeEnum.REPEATED)
            .findFirst()
            .orElseThrow();
    PSAgingTransition changed =
        draft.getAgingTransitions().stream()
            .filter(edge -> "CONTENTEXPIRYDATE".equals(edge.getSystemField()))
            .findFirst()
            .orElseThrow();
    PSAgingTransition untouched =
        draft.getAgingTransitions().stream()
            .filter(edge -> "REMINDERDATE".equals(edge.getSystemField()))
            .findFirst()
            .orElseThrow();
    assertEquals(15L, absolute.getInterval());
    assertEquals("Aging 15", absolute.getLabel());
    assertEquals(PSAgingTransition.PSAgingTypeEnum.ABSOLUTE, absolute.getAgingTypeEnum());
    assertEquals(15L, repeatedEdge.getInterval());
    assertEquals("Repeated aging 15", repeatedEdge.getLabel());
    assertEquals(PSAgingTransition.PSAgingTypeEnum.REPEATED, repeatedEdge.getAgingTypeEnum());
    assertEquals(PSAgingTransition.PSAgingTypeEnum.SYSTEM_FIELD, changed.getAgingTypeEnum());
    assertEquals("CONTENTEXPIRYDATE", changed.getSystemField());
    assertEquals("System field aging CONTENTEXPIRYDATE", changed.getLabel());
    assertEquals("System field aging CONTENTEXPIRYDATE", changed.getTrigger());
    assertEquals(1L, changed.getInterval());
    assertEquals(2L, changed.getToState());
    assertEquals(PSAgingTransition.PSAgingTypeEnum.SYSTEM_FIELD, untouched.getAgingTypeEnum());
    assertEquals("REMINDERDATE", untouched.getSystemField());
    assertEquals(4, draft.getAgingTransitions().size());
    assertEquals(0, draft.getTransitions().size());
    assertEquals(2, wf.getStates().size());
    WorkflowGraph.Edge projected =
        graph.getEdges().stream()
            .filter(edge -> "CONTENTEXPIRYDATE".equals(edge.getSystemField()))
            .findFirst()
            .orElseThrow();
    assertEquals("SYSTEM_FIELD", projected.getAgingType());
    assertEquals(null, projected.getIntervalMinutes());
    assertEquals("System field aging CONTENTEXPIRYDATE", projected.getLabel());
    assertEquals(1, graph.getEdges().stream().filter(edge -> "ABSOLUTE".equals(edge.getAgingType())).count());
    assertEquals(1, graph.getEdges().stream().filter(edge -> "REPEATED".equals(edge.getAgingType())).count());
    assertEquals(
        15L,
        graph.getEdges().stream()
            .filter(edge -> "ABSOLUTE".equals(edge.getAgingType()))
            .findFirst()
            .orElseThrow()
            .getIntervalMinutes());
    verify(workflowService, times(5)).saveWorkflow(wf);
  }

  @Test
  void systemFieldChangeDoesNotTouchAnAbsoluteIntervalOfOneMinute() {
    PSState draft = state(1, "Draft");
    PSWorkflow wf = workflow("Nightly QA", draft, state(2, "Review"));
    stub(wf, false);
    adaptor.createAbsoluteAgingTransition(null, "Nightly QA", body("Draft", "Review", 1));
    adaptor.createAbsoluteAgingTransition(
        null, "Nightly QA", systemBody("Draft", "Review", "CONTENTSTARTDATE"));

    adaptor.changeSystemFieldAging(
        null, "Nightly QA", fieldBody("Draft", "Review", "CONTENTSTARTDATE", "REMINDERDATE"));

    PSAgingTransition absolute = draft.getAgingTransitions().get(0);
    PSAgingTransition system = draft.getAgingTransitions().get(1);
    assertEquals(PSAgingTransition.PSAgingTypeEnum.ABSOLUTE, absolute.getAgingTypeEnum());
    assertEquals(1L, absolute.getInterval());
    assertEquals("Aging 1", absolute.getLabel());
    assertEquals(PSAgingTransition.PSAgingTypeEnum.SYSTEM_FIELD, system.getAgingTypeEnum());
    assertEquals("REMINDERDATE", system.getSystemField());
    assertEquals(1L, system.getInterval());
    verify(workflowService, times(3)).saveWorkflow(wf);
  }

  @Test
  void customSystemFieldLabelStaysWhileTheColumnChanges() {
    PSState draft = state(1, "Draft");
    PSWorkflow wf = workflow("Nightly QA", draft, state(2, "Review"));
    stub(wf, false);
    adaptor.createAbsoluteAgingTransition(
        null, "Nightly QA", systemBody("Draft", "Review", "CONTENTSTARTDATE"));
    draft.getAgingTransitions().get(0).setLabel("Keep me");

    adaptor.changeSystemFieldAging(
        null, "Nightly QA", fieldBody("Draft", "Review", "CONTENTSTARTDATE", "CONTENTEXPIRYDATE"));

    PSAgingTransition system = draft.getAgingTransitions().get(0);
    assertEquals("Keep me", system.getLabel());
    assertEquals("CONTENTEXPIRYDATE", system.getSystemField());
    assertEquals(PSAgingTransition.PSAgingTypeEnum.SYSTEM_FIELD, system.getAgingTypeEnum());
    assertEquals("System field aging CONTENTEXPIRYDATE", system.getTrigger());
    verify(workflowService, times(2)).saveWorkflow(wf);
  }

  @Test
  void duplicateSystemFieldChangeDoesNotSave() {
    PSState draft = state(1, "Draft");
    PSWorkflow wf = workflow("Nightly QA", draft, state(2, "Review"));
    stub(wf, false);
    adaptor.createAbsoluteAgingTransition(
        null, "Nightly QA", systemBody("Draft", "Review", "CONTENTSTARTDATE"));
    adaptor.createAbsoluteAgingTransition(
        null, "Nightly QA", systemBody("Draft", "Review", "REMINDERDATE"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                adaptor.changeSystemFieldAging(
                    null,
                    "Nightly QA",
                    fieldBody("Draft", "Review", "CONTENTSTARTDATE", "REMINDERDATE")));
    assertEquals(409, ex.getResponse().getStatus());
    assertEquals("CONTENTSTARTDATE", draft.getAgingTransitions().get(0).getSystemField());
    assertEquals("REMINDERDATE", draft.getAgingTransitions().get(1).getSystemField());
    verify(workflowService, times(2)).saveWorkflow(wf);
  }

  @Test
  void missingSystemFieldChangeIs404AndDoesNotSave() {
    PSState draft = state(1, "Draft");
    PSWorkflow wf = workflow("Nightly QA", draft, state(2, "Review"));
    stub(wf, false);
    adaptor.createAbsoluteAgingTransition(null, "Nightly QA", body("Draft", "Review", 15));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                adaptor.changeSystemFieldAging(
                    null,
                    "Nightly QA",
                    fieldBody("Draft", "Review", "CONTENTSTARTDATE", "REMINDERDATE")));
    assertEquals(404, ex.getResponse().getStatus());
    assertEquals(15L, draft.getAgingTransitions().get(0).getInterval());
    assertEquals(
        PSAgingTransition.PSAgingTypeEnum.ABSOLUTE,
        draft.getAgingTransitions().get(0).getAgingTypeEnum());
    verify(workflowService, times(1)).saveWorkflow(wf);
  }

  @Test
  void blankSameOrUnknownSystemFieldChangeDoesNotSave() {
    PSState draft = state(1, "Draft");
    PSWorkflow wf = workflow("Nightly QA", draft, state(2, "Review"));
    stub(wf, false);
    adaptor.createAbsoluteAgingTransition(
        null, "Nightly QA", systemBody("Draft", "Review", "CONTENTSTARTDATE"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            adaptor.changeSystemFieldAging(
                null, "Nightly QA", fieldBody("Draft", "Review", " ", "REMINDERDATE")));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            adaptor.changeSystemFieldAging(
                null, "Nightly QA", fieldBody("Draft", "Review", "CONTENTSTARTDATE", "sys_title")));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            adaptor.changeSystemFieldAging(
                null,
                "Nightly QA",
                fieldBody("Draft", "Review", "contentstartdate", "CONTENTSTARTDATE")));
    assertEquals("CONTENTSTARTDATE", draft.getAgingTransitions().get(0).getSystemField());
    assertEquals(
        PSAgingTransition.PSAgingTypeEnum.SYSTEM_FIELD,
        draft.getAgingTransitions().get(0).getAgingTypeEnum());
    verify(workflowService, times(1)).saveWorkflow(wf);
  }

  @Test
  void packagedSystemFieldChangeIs403() {
    PSWorkflow wf = workflow("Simple Workflow", state(1, "Draft"), state(2, "Review"));
    stub(wf, false);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                adaptor.changeSystemFieldAging(
                    null,
                    "Simple Workflow",
                    fieldBody("Draft", "Review", "CONTENTSTARTDATE", "REMINDERDATE")));
    assertEquals(403, ex.getResponse().getStatus());
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
  void deletesRepeatedAgingAndLeavesTheAbsoluteEdgeWithTheSameInterval() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    draft.addTransition(regular("Submit", 2));
    PSWorkflow wf = workflow("Nightly QA", draft, review);
    stub(wf, false);
    adaptor.createAbsoluteAgingTransition(null, "Nightly QA", body("Draft", "Review", 15));
    WorkflowAgingTransitionWrite repeated = body("Draft", "Review", 15);
    repeated.setType("REPEATED");
    adaptor.createAbsoluteAgingTransition(null, "Nightly QA", repeated);
    adaptor.createAbsoluteAgingTransition(
        null, "Nightly QA", systemBody("Draft", "Review", "CONTENTSTARTDATE"));

    WorkflowGraph graph =
        adaptor.deleteTypedAgingTransition(null, "Nightly QA", "Draft", "Review", 15L, "REPEATED", null);

    assertEquals(PSAgingTransition.PSAgingTypeEnum.ABSOLUTE, draft.getAgingTransitions().get(0).getAgingTypeEnum());
    assertEquals(15L, draft.getAgingTransitions().get(0).getInterval());
    assertEquals(
        PSAgingTransition.PSAgingTypeEnum.SYSTEM_FIELD, draft.getAgingTransitions().get(1).getAgingTypeEnum());
    assertEquals(2, draft.getAgingTransitions().size());
    assertEquals(1, draft.getTransitions().size());
    assertEquals("Submit", draft.getTransitions().get(0).getLabel());
    assertTrue(graph.getEdges().stream().anyMatch(edge -> "ABSOLUTE".equals(edge.getAgingType())));
    assertTrue(graph.getEdges().stream().noneMatch(edge -> "REPEATED".equals(edge.getAgingType())));
    assertEquals(2, wf.getStates().size());
    verify(workflowService, times(4)).saveWorkflow(wf);
  }

  @Test
  void repeatedDeleteWhenOnlyAbsoluteExistsIs409() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    draft.addTransition(regular("Submit", 2));
    PSWorkflow wf = workflow("Nightly QA", draft, review);
    stub(wf, false);
    adaptor.createAbsoluteAgingTransition(null, "Nightly QA", body("Draft", "Review", 15));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                adaptor.deleteTypedAgingTransition(
                    null, "Nightly QA", "Draft", "Review", 15L, "REPEATED", null));

    assertEquals(409, ex.getResponse().getStatus());
    assertEquals(1, draft.getAgingTransitions().size());
    assertEquals(PSAgingTransition.PSAgingTypeEnum.ABSOLUTE, draft.getAgingTransitions().get(0).getAgingTypeEnum());
    verify(workflowService, times(1)).saveWorkflow(wf);
  }

  @Test
  void deletesOneSystemFieldAndLeavesAbsoluteAndRepeatedEdges() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    draft.addTransition(regular("Submit", 2));
    PSWorkflow wf = workflow("Nightly QA", draft, review);
    stub(wf, false);
    adaptor.createAbsoluteAgingTransition(null, "Nightly QA", body("Draft", "Review", 1));
    WorkflowAgingTransitionWrite repeated = body("Draft", "Review", 1);
    repeated.setType("REPEATED");
    adaptor.createAbsoluteAgingTransition(null, "Nightly QA", repeated);
    adaptor.createAbsoluteAgingTransition(
        null, "Nightly QA", systemBody("Draft", "Review", "CONTENTSTARTDATE"));
    adaptor.createAbsoluteAgingTransition(
        null, "Nightly QA", systemBody("Draft", "Review", "REMINDERDATE"));

    WorkflowGraph graph =
        adaptor.deleteTypedAgingTransition(
            null, "Nightly QA", "Draft", "Review", null, "SYSTEM_FIELD", "contentstartdate");

    assertEquals(3, draft.getAgingTransitions().size());
    assertEquals(PSAgingTransition.PSAgingTypeEnum.ABSOLUTE, draft.getAgingTransitions().get(0).getAgingTypeEnum());
    assertEquals(1L, draft.getAgingTransitions().get(0).getInterval());
    assertEquals(PSAgingTransition.PSAgingTypeEnum.REPEATED, draft.getAgingTransitions().get(1).getAgingTypeEnum());
    assertEquals("REMINDERDATE", draft.getAgingTransitions().get(2).getSystemField());
    assertEquals(1, draft.getTransitions().size());
    assertTrue(graph.getEdges().stream().anyMatch(edge -> "ABSOLUTE".equals(edge.getAgingType())));
    assertTrue(graph.getEdges().stream().noneMatch(edge -> "CONTENTSTARTDATE".equals(edge.getSystemField())));
    assertTrue(graph.getEdges().stream().anyMatch(edge -> "REMINDERDATE".equals(edge.getSystemField())));
    verify(workflowService, times(5)).saveWorkflow(wf);
  }

  @Test
  void unknownSystemFieldDeleteDoesNotSave() {
    PSState draft = state(1, "Draft");
    PSWorkflow wf = workflow("Nightly QA", draft, state(2, "Review"));
    stub(wf, false);
    adaptor.createAbsoluteAgingTransition(
        null, "Nightly QA", systemBody("Draft", "Review", "CONTENTSTARTDATE"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            adaptor.deleteTypedAgingTransition(
                null, "Nightly QA", "Draft", "Review", null, "SYSTEM_FIELD", "NOT_A_FIELD"));
    assertEquals(1, draft.getAgingTransitions().size());
    verify(workflowService, times(1)).saveWorkflow(wf);
  }

  @Test
  void unknownAgingDeleteTypeDoesNotSave() {
    PSWorkflow wf = workflow("Nightly QA", state(1, "Draft"), state(2, "Review"));
    stub(wf, false);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            adaptor.deleteTypedAgingTransition(
                null, "Nightly QA", "Draft", "Review", 15L, "NOPE", null));
    verify(workflowService, never()).saveWorkflow(wf);
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

  private static WorkflowAgingTransitionWrite systemBody(String from, String to, String systemField) {
    WorkflowAgingTransitionWrite body = new WorkflowAgingTransitionWrite();
    body.setFrom(from);
    body.setTo(to);
    body.setType("SYSTEM_FIELD");
    body.setSystemField(systemField);
    return body;
  }

  private static WorkflowAgingSystemFieldWrite fieldBody(
      String from, String to, String current, String next) {
    WorkflowAgingSystemFieldWrite body = new WorkflowAgingSystemFieldWrite();
    body.setFrom(from);
    body.setTo(to);
    body.setSystemField(current);
    body.setNewSystemField(next);
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
