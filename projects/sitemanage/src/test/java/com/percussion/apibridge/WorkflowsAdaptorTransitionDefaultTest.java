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
class WorkflowsAdaptorTransitionDefaultTest {

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
  void marksChosenTransitionAndClearsThePreviousDefaultOnThatStep() {
    PSState draft = state(1, "Draft");
    PSState review = state(2, "Review");
    PSTransition submit = transition("Submit", 2);
    submit.setDefaultTransition(true);
    submit.setApprovals(1);
    submit.setRequiresComment(PSWorkflowCommentEnum.REQUIRED);
    PSTransition send = transition("Send", 2);
    send.setDefaultTransition(false);
    send.setApprovals(4);
    send.setRequiresComment(PSWorkflowCommentEnum.OPTIONAL);
    draft.addTransition(submit);
    draft.addTransition(send);
    PSTransition approve = transition("Approve", 1);
    approve.setDefaultTransition(true);
    approve.setApprovals(2);
    review.addTransition(approve);
    PSWorkflow wf = workflow("Nightly QA", draft, review);
    stub(wf);

    WorkflowGraph graph =
        adaptor.updateTransitionDefault(null, "Nightly QA", "Draft", "Send", "Review", true);

    assertFalse(draft.getTransitions().get(0).isDefaultTransition());
    assertTrue(draft.getTransitions().get(1).isDefaultTransition());
    assertEquals(1, draft.getTransitions().get(0).getApprovals());
    assertEquals(4, draft.getTransitions().get(1).getApprovals());
    assertEquals(PSWorkflowCommentEnum.REQUIRED, draft.getTransitions().get(0).getRequiresComment());
    assertEquals(PSWorkflowCommentEnum.OPTIONAL, draft.getTransitions().get(1).getRequiresComment());
    assertEquals("Submit", draft.getTransitions().get(0).getLabel());
    assertEquals("Send", draft.getTransitions().get(1).getLabel());
    assertEquals(2L, draft.getTransitions().get(1).getToState());
    assertTrue(review.getTransitions().get(0).isDefaultTransition());
    assertEquals(2, review.getTransitions().get(0).getApprovals());
    assertEquals(Boolean.FALSE, graph.getEdges().get(0).getDefaultTransition());
    assertEquals(Boolean.TRUE, graph.getEdges().get(1).getDefaultTransition());
    assertEquals(1, graph.getEdges().get(0).getApprovalsRequired());
    assertTrue(graph.getEdges().get(0).isCommentRequired());
    assertEquals(2, wf.getStates().size());
    long draftDefaults =
        draft.getTransitions().stream().filter(PSTransition::isDefaultTransition).count();
    assertEquals(1, draftDefaults);
    verify(workflowService).saveWorkflow(wf);
  }

  @Test
  void alreadyTheOnlyDefaultDoesNotSave() {
    PSState draft = state(1, "Draft");
    PSTransition submit = transition("Submit", 2);
    submit.setDefaultTransition(true);
    PSTransition send = transition("Send", 2);
    send.setDefaultTransition(false);
    draft.addTransition(submit);
    draft.addTransition(send);
    PSWorkflow wf = workflow("Nightly QA", draft, state(2, "Review"));
    stub(wf);

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                adaptor.updateTransitionDefault(
                    null, "Nightly QA", "Draft", "Submit", "Review", true));
    assertTrue(ex.getMessage().toLowerCase().contains("already"));
    assertTrue(draft.getTransitions().get(0).isDefaultTransition());
    assertFalse(draft.getTransitions().get(1).isDefaultTransition());
    verify(workflowService, never()).saveWorkflow(wf);
  }

  @Test
  void clearingTheCurrentDefaultIs409AndDoesNotSave() {
    PSState draft = state(1, "Draft");
    PSTransition submit = transition("Submit", 2);
    submit.setDefaultTransition(true);
    PSTransition send = transition("Send", 2);
    send.setDefaultTransition(false);
    draft.addTransition(submit);
    draft.addTransition(send);
    PSWorkflow wf = workflow("Nightly QA", draft, state(2, "Review"));
    stub(wf);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                adaptor.updateTransitionDefault(
                    null, "Nightly QA", "Draft", "Submit", "Review", false));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(draft.getTransitions().get(0).isDefaultTransition());
    assertFalse(draft.getTransitions().get(1).isDefaultTransition());
    verify(workflowService, never()).saveWorkflow(wf);
  }

  @Test
  void falseOnANonDefaultIs400AndDoesNotSave() {
    PSState draft = state(1, "Draft");
    PSTransition submit = transition("Submit", 2);
    submit.setDefaultTransition(true);
    PSTransition send = transition("Send", 2);
    draft.addTransition(submit);
    draft.addTransition(send);
    PSWorkflow wf = workflow("Nightly QA", draft, state(2, "Review"));
    stub(wf);

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> adaptor.updateTransitionDefault(null, "Nightly QA", "Draft", "Send", null, false));
    assertTrue(ex.getMessage().toLowerCase().contains("true"));
    assertTrue(draft.getTransitions().get(0).isDefaultTransition());
    assertFalse(draft.getTransitions().get(1).isDefaultTransition());
    verify(workflowService, never()).saveWorkflow(wf);
  }

  @Test
  void agingTransitionIs400AndDoesNotSave() {
    PSState live = state(4, "Live");
    PSTransition publish = transition("Publish", 5);
    publish.setDefaultTransition(true);
    live.addTransition(publish);
    PSAgingTransition expire = new PSAgingTransition();
    expire.setLabel("Expire");
    expire.setTrigger("Expire");
    expire.setToState(5L);
    live.addAgingTransition(expire);
    PSWorkflow wf = workflow("Nightly QA", live, state(5, "Archive"));
    stub(wf);

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                adaptor.updateTransitionDefault(
                    null, "Nightly QA", "Live", "Expire", "Archive", true));
    assertTrue(ex.getMessage().toLowerCase().contains("aging"));
    assertTrue(live.getTransitions().get(0).isDefaultTransition());
    verify(workflowService, never()).saveWorkflow(wf);
  }

  @Test
  void packagedWorkflowIs403() {
    PSState draft = state(1, "Draft");
    PSTransition submit = transition("Submit", 2);
    submit.setDefaultTransition(true);
    PSTransition send = transition("Send", 2);
    draft.addTransition(submit);
    draft.addTransition(send);
    PSWorkflow wf = workflow("Simple Workflow", draft, state(2, "Review"));
    stub(wf);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                adaptor.updateTransitionDefault(
                    null, "Simple Workflow", "Draft", "Send", "Review", true));
    assertEquals(403, ex.getResponse().getStatus());
    assertTrue(draft.getTransitions().get(0).isDefaultTransition());
    assertFalse(draft.getTransitions().get(1).isDefaultTransition());
    verify(workflowService, never()).saveWorkflow(wf);
  }

  @Test
  void missingTransitionIs404() {
    PSState draft = state(1, "Draft");
    PSTransition submit = transition("Submit", 2);
    submit.setDefaultTransition(true);
    draft.addTransition(submit);
    PSWorkflow wf = workflow("Nightly QA", draft, state(2, "Review"));
    stub(wf);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                adaptor.updateTransitionDefault(
                    null, "Nightly QA", "Draft", "Publish", null, true));
    assertEquals(404, ex.getResponse().getStatus());
    assertTrue(draft.getTransitions().get(0).isDefaultTransition());
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
