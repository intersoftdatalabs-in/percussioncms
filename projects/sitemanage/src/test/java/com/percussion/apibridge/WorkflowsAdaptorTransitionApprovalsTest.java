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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.workflows.WorkflowGraph;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.services.workflow.data.PSAgingTransition;
import com.percussion.services.workflow.data.PSState;
import com.percussion.services.workflow.data.PSTransition;
import com.percussion.services.workflow.data.PSTransition.PSWorkflowCommentEnum;
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
class WorkflowsAdaptorTransitionApprovalsTest {

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
  void setsApprovalsAndLeavesOtherFields() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    PSTransition submit = transition("Submit", 2);
    submit.setRequiresComment(PSWorkflowCommentEnum.REQUIRED);
    submit.setDefaultTransition(true);
    submit.setApprovals(1);
    PSTransition reject = transition("Reject", 2);
    reject.setApprovals(4);
    reject.setRequiresComment(PSWorkflowCommentEnum.OPTIONAL);
    draft.addTransition(submit);
    draft.addTransition(reject);
    PSWorkflow wf = workflow("Nightly QA", draft, review);
    stub(wf);

    WorkflowGraph graph =
        adaptor.updateTransitionApprovalsRequired(null, "Nightly QA", "Draft", "Submit", "Review", 2);

    assertEquals(2, draft.getTransitions().get(0).getApprovals());
    assertEquals(PSWorkflowCommentEnum.REQUIRED, draft.getTransitions().get(0).getRequiresComment());
    assertTrue(draft.getTransitions().get(0).isDefaultTransition());
    assertEquals("Submit", draft.getTransitions().get(0).getLabel());
    assertEquals(2L, draft.getTransitions().get(0).getToState());
    assertEquals(4, draft.getTransitions().get(1).getApprovals());
    assertEquals("Reject", draft.getTransitions().get(1).getLabel());
    assertEquals(2, graph.getEdges().size());
    assertEquals(2, graph.getEdges().get(0).getApprovalsRequired());
    assertTrue(graph.getEdges().get(0).isCommentRequired());
    assertEquals(4, graph.getEdges().get(1).getApprovalsRequired());
    assertFalse(graph.getEdges().get(1).isCommentRequired());
    assertEquals(2, wf.getStates().size());
    verify(workflowService).saveWorkflow(wf);
  }

  @Test
  void zeroIsStored() {
    PSState draft = state(1, "Draft");
    PSTransition submit = transition("Submit", 2);
    submit.setApprovals(1);
    draft.addTransition(submit);
    PSWorkflow wf = workflow("Nightly QA", draft, state(2, "Review"));
    stub(wf);

    WorkflowGraph graph =
        adaptor.updateTransitionApprovalsRequired(null, "Nightly QA", "Draft", "Submit", null, 0);

    assertEquals(0, draft.getTransitions().get(0).getApprovals());
    assertEquals(0, graph.getEdges().get(0).getApprovalsRequired());
    verify(workflowService).saveWorkflow(wf);
  }

  @Test
  void negativeDoesNotSave() {
    PSState draft = state(1, "Draft");
    PSTransition submit = transition("Submit", 2);
    submit.setApprovals(1);
    draft.addTransition(submit);
    PSWorkflow wf = workflow("Nightly QA", draft, state(2, "Review"));
    stub(wf);

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                adaptor.updateTransitionApprovalsRequired(
                    null, "Nightly QA", "Draft", "Submit", "Review", -1));
    assertTrue(ex.getMessage().toLowerCase().contains("non-negative"));
    assertEquals(1, draft.getTransitions().get(0).getApprovals());
    verify(workflowService, never()).saveWorkflow(wf);
  }

  @Test
  void unchangedDoesNotSave() {
    PSState draft = state(1, "Draft");
    PSTransition submit = transition("Submit", 2);
    submit.setApprovals(3);
    draft.addTransition(submit);
    PSWorkflow wf = workflow("Nightly QA", draft, state(2, "Review"));
    stub(wf);

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                adaptor.updateTransitionApprovalsRequired(
                    null, "Nightly QA", "Draft", "Submit", null, 3));
    assertTrue(ex.getMessage().toLowerCase().contains("unchanged"));
    assertEquals(3, draft.getTransitions().get(0).getApprovals());
    verify(workflowService, never()).saveWorkflow(wf);
  }

  @Test
  void eachRoleSentinelIs409AndDoesNotSave() {
    PSState draft = state(1, "Draft");
    PSTransition submit = transition("Submit", 2);
    submit.setApprovals(-1);
    draft.addTransition(submit);
    PSWorkflow wf = workflow("Nightly QA", draft, state(2, "Review"));
    stub(wf);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                adaptor.updateTransitionApprovalsRequired(
                    null, "Nightly QA", "Draft", "Submit", "Review", 2));
    assertEquals(409, ex.getResponse().getStatus());
    assertEquals(-1, draft.getTransitions().get(0).getApprovals());
    verify(workflowService, never()).saveWorkflow(wf);
  }

  @Test
  void agingTransitionIs400AndDoesNotSave() {
    PSState live = state(4, "Live");
    PSState archive = state(5, "Archive");
    PSAgingTransition expire = new PSAgingTransition();
    expire.setLabel("Expire");
    expire.setTrigger("Expire");
    expire.setToState(5L);
    live.addAgingTransition(expire);
    PSWorkflow wf = workflow("Nightly QA", live, archive);
    stub(wf);

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                adaptor.updateTransitionApprovalsRequired(
                    null, "Nightly QA", "Live", "Expire", "Archive", 2));
    assertTrue(ex.getMessage().toLowerCase().contains("aging"));
    verify(workflowService, never()).saveWorkflow(wf);
  }

  @Test
  void packagedWorkflowIs403() {
    PSState draft = state(1, "Draft");
    PSTransition submit = transition("Submit", 2);
    submit.setApprovals(1);
    draft.addTransition(submit);
    PSWorkflow wf = workflow("Simple Workflow", draft, state(2, "Review"));
    stub(wf);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                adaptor.updateTransitionApprovalsRequired(
                    null, "Simple Workflow", "Draft", "Submit", "Review", 2));
    assertEquals(403, ex.getResponse().getStatus());
    assertEquals(1, draft.getTransitions().get(0).getApprovals());
    verify(workflowService, never()).saveWorkflow(wf);
  }

  @Test
  void missingTransitionIs404() {
    PSState draft = state(1, "Draft");
    draft.addTransition(transition("Submit", 2));
    PSWorkflow wf = workflow("Nightly QA", draft, state(2, "Review"));
    stub(wf);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                adaptor.updateTransitionApprovalsRequired(
                    null, "Nightly QA", "Draft", "Publish", null, 2));
    assertEquals(404, ex.getResponse().getStatus());
    verify(workflowService, never()).saveWorkflow(wf);
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
