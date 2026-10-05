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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.workflows.WorkflowGraph;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.services.workflow.data.PSAgingTransition;
import com.percussion.services.workflow.data.PSState;
import com.percussion.services.workflow.data.PSTransition;
import com.percussion.services.workflow.data.PSTransition.PSWorkflowCommentEnum;
import com.percussion.services.workflow.data.PSTransitionRole;
import com.percussion.services.workflow.data.PSWorkflow;
import com.percussion.services.workflow.data.PSWorkflowRole;
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
class WorkflowsAdaptorTransitionAllowedRoleTest {

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
  void restrictsOneAllowAllTransitionToOneRoleAndLeavesTheSiblingAllowAll() {
    PSWorkflowRole editor = role(12, "Editor");
    PSWorkflowRole author = role(11, "Author");
    PSState draft = state(1, "Draft");
    PSTransition submit = transition(21, "Submit", 2);
    submit.setDefaultTransition(true);
    submit.setApprovals(1);
    submit.setRequiresComment(PSWorkflowCommentEnum.REQUIRED);
    PSTransition send = transition(22, "Send", 3);
    send.setDefaultTransition(false);
    send.setApprovals(4);
    send.setRequiresComment(PSWorkflowCommentEnum.OPTIONAL);
    draft.addTransition(submit);
    draft.addTransition(send);
    PSState review = state(2, "Review");
    PSState live = state(3, "Live");
    PSAgingTransition expire = aging("Expire", 3);
    draft.addAgingTransition(expire);
    PSWorkflow wf = workflow("Nightly QA", List.of(editor, author), draft, review, live);
    stub(wf);

    WorkflowGraph graph =
        adaptor.restrictTransitionToOneRole(null, "Nightly QA", "Draft", "Send", "Live", "editor");

    PSTransition storedSend = draft.getTransitions().get(1);
    PSTransition storedSubmit = draft.getTransitions().get(0);
    assertFalse(storedSend.isAllowAllRoles());
    assertEquals(1, storedSend.getTransitionRoles().size());
    assertEquals(12L, storedSend.getTransitionRoles().get(0).getRoleId());
    assertEquals(22L, storedSend.getTransitionRoles().get(0).getTransitionId());
    assertEquals(7L, storedSend.getTransitionRoles().get(0).getWorkflowId());
    assertTrue(storedSubmit.isAllowAllRoles());
    assertTrue(storedSubmit.getTransitionRoles().isEmpty());
    assertEquals(1, storedSubmit.getApprovals());
    assertEquals(PSWorkflowCommentEnum.REQUIRED, storedSubmit.getRequiresComment());
    assertTrue(storedSubmit.isDefaultTransition());
    assertEquals("Submit", storedSubmit.getLabel());
    assertEquals(4, storedSend.getApprovals());
    assertEquals(PSWorkflowCommentEnum.OPTIONAL, storedSend.getRequiresComment());
    assertFalse(storedSend.isDefaultTransition());
    assertEquals("Send", storedSend.getLabel());
    assertEquals(3L, storedSend.getToState());
    assertEquals("Expire", draft.getAgingTransitions().get(0).getLabel());
    assertEquals(3, wf.getStates().size());
    assertEquals(2, draft.getTransitions().size());

    WorkflowGraph.Edge restricted = edge(graph, "Send");
    WorkflowGraph.Edge untouched = edge(graph, "Submit");
    assertEquals(Boolean.FALSE, restricted.getAllowAllRoles());
    assertEquals(List.of("Editor"), restricted.getAllowedRoles());
    assertEquals(Boolean.TRUE, untouched.getAllowAllRoles());
    assertNull(untouched.getAllowedRoles());
    assertEquals(Boolean.FALSE, restricted.getDefaultTransition());
    assertEquals(4, restricted.getApprovalsRequired());
    assertFalse(restricted.isCommentRequired());
    assertTrue(untouched.isCommentRequired());
    assertTrue(graph.getRoles().contains("Editor"));
    assertTrue(graph.getRoles().contains("Author"));
    WorkflowGraph.Edge agingEdge = edge(graph, "Expire");
    assertTrue(agingEdge.isAging());
    assertNull(agingEdge.getAllowAllRoles());
    assertNull(agingEdge.getAllowedRoles());
    verify(workflowService).saveWorkflow(wf);
  }

  @Test
  void allowAllMarkerIs409AndDoesNotSave() {
    PSTransition send = allowAllSend();
    PSWorkflow wf = workflowWith(send);
    stub(wf);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                adaptor.restrictTransitionToOneRole(
                    null, "Nightly QA", "Draft", "Send", "Live", "*ALL*"));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(send.isAllowAllRoles());
    assertTrue(send.getTransitionRoles().isEmpty());
    verify(workflowService, never()).saveWorkflow(wf);
  }

  @Test
  void alreadyRestrictedIs409AndKeepsTheExistingRole() {
    PSWorkflowRole editor = role(12, "Editor");
    PSWorkflowRole author = role(11, "Author");
    PSTransition send = transition(22, "Send", 3);
    send.setAllowAllRoles(false);
    PSTransitionRole existing = new PSTransitionRole();
    existing.setRoleId(12L);
    existing.setTransitionId(22L);
    existing.setWorkflowId(7L);
    send.setTransitionRoles(new ArrayList<>(List.of(existing)));
    PSState draft = state(1, "Draft");
    draft.addTransition(send);
    PSWorkflow wf =
        workflow("Nightly QA", List.of(editor, author), draft, state(3, "Live"));
    stub(wf);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                adaptor.restrictTransitionToOneRole(
                    null, "Nightly QA", "Draft", "Send", "Live", "Author"));
    assertEquals(409, ex.getResponse().getStatus());
    assertFalse(send.isAllowAllRoles());
    assertEquals(1, send.getTransitionRoles().size());
    assertEquals(12L, send.getTransitionRoles().get(0).getRoleId());
    verify(workflowService, never()).saveWorkflow(wf);
  }

  @Test
  void unknownRoleIs404AndLeavesAllowAll() {
    PSTransition send = allowAllSend();
    PSWorkflow wf = workflowWith(send);
    stub(wf);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                adaptor.restrictTransitionToOneRole(
                    null, "Nightly QA", "Draft", "Send", "Live", "Missing"));
    assertEquals(404, ex.getResponse().getStatus());
    assertTrue(send.isAllowAllRoles());
    verify(workflowService, never()).saveWorkflow(wf);
  }

  @Test
  void agingTransitionIs400AndLeavesAllowAll() {
    PSTransition publish = transition(30, "Publish", 5);
    PSState live = state(4, "Live");
    live.addTransition(publish);
    PSAgingTransition expire = aging("Expire", 5);
    live.addAgingTransition(expire);
    PSWorkflow wf = workflow("Nightly QA", List.of(role(12, "Editor")), live, state(5, "Archive"));
    stub(wf);

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                adaptor.restrictTransitionToOneRole(
                    null, "Nightly QA", "Live", "Expire", "Archive", "Editor"));
    assertTrue(ex.getMessage().toLowerCase().contains("aging"));
    assertTrue(publish.isAllowAllRoles());
    assertEquals("Expire", live.getAgingTransitions().get(0).getLabel());
    verify(workflowService, never()).saveWorkflow(wf);
  }

  @Test
  void ambiguousLabelIs400AndLeavesAllowAll() {
    PSTransition sendReview = transition(22, "Send", 2);
    PSTransition sendLive = transition(23, "Send", 3);
    PSState draft = state(1, "Draft");
    draft.addTransition(sendReview);
    draft.addTransition(sendLive);
    PSWorkflow wf =
        workflow(
            "Nightly QA",
            List.of(role(12, "Editor")),
            draft,
            state(2, "Review"),
            state(3, "Live"));
    stub(wf);

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                adaptor.restrictTransitionToOneRole(
                    null, "Nightly QA", "Draft", "Send", null, "Editor"));
    assertTrue(ex.getMessage().toLowerCase().contains("specify to"));
    assertTrue(sendReview.isAllowAllRoles());
    assertTrue(sendLive.isAllowAllRoles());
    verify(workflowService, never()).saveWorkflow(wf);
  }

  @Test
  void packagedWorkflowIs403AndLeavesAllowAll() {
    PSTransition send = transition(22, "Send", 3);
    PSState draft = state(1, "Draft");
    draft.addTransition(send);
    PSWorkflow wf = workflow("Simple Workflow", List.of(role(12, "Editor")), draft, state(3, "Live"));
    stub(wf);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                adaptor.restrictTransitionToOneRole(
                    null, "Simple Workflow", "Draft", "Send", "Live", "Editor"));
    assertEquals(403, ex.getResponse().getStatus());
    assertTrue(send.isAllowAllRoles());
    verify(workflowService, never()).saveWorkflow(wf);
  }

  @Test
  void addsOneRoleToAnAlreadyRestrictedTransitionAndKeepsTheFirstRole() {
    PSWorkflowRole editor = role(12, "Editor");
    PSWorkflowRole author = role(11, "Author");
    PSState draft = state(1, "Draft");
    PSTransition submit = transition(21, "Submit", 2);
    submit.setDefaultTransition(true);
    submit.setApprovals(1);
    submit.setRequiresComment(PSWorkflowCommentEnum.REQUIRED);
    PSTransition send = restricted(transition(22, "Send", 3), 12L);
    send.setDefaultTransition(false);
    send.setApprovals(4);
    send.setRequiresComment(PSWorkflowCommentEnum.OPTIONAL);
    draft.addTransition(submit);
    draft.addTransition(send);
    PSAgingTransition expire = aging("Expire", 3);
    draft.addAgingTransition(expire);
    PSWorkflow wf =
        workflow("Nightly QA", List.of(editor, author), draft, state(2, "Review"), state(3, "Live"));
    stub(wf);

    WorkflowGraph graph =
        adaptor.addTransitionAllowedRole(null, "Nightly QA", "Draft", "Send", "Live", "author");

    PSTransition storedSend = draft.getTransitions().get(1);
    PSTransition storedSubmit = draft.getTransitions().get(0);
    assertFalse(storedSend.isAllowAllRoles());
    assertEquals(2, storedSend.getTransitionRoles().size());
    assertEquals(12L, storedSend.getTransitionRoles().get(0).getRoleId());
    assertEquals(11L, storedSend.getTransitionRoles().get(1).getRoleId());
    assertEquals(22L, storedSend.getTransitionRoles().get(1).getTransitionId());
    assertEquals(7L, storedSend.getTransitionRoles().get(1).getWorkflowId());
    assertTrue(storedSubmit.isAllowAllRoles());
    assertTrue(storedSubmit.getTransitionRoles().isEmpty());
    assertEquals(4, storedSend.getApprovals());
    assertEquals(PSWorkflowCommentEnum.OPTIONAL, storedSend.getRequiresComment());
    assertFalse(storedSend.isDefaultTransition());
    assertEquals("Send", storedSend.getLabel());
    assertEquals(3L, storedSend.getToState());
    assertEquals(1, storedSubmit.getApprovals());
    assertEquals("Expire", draft.getAgingTransitions().get(0).getLabel());
    assertEquals(3, wf.getStates().size());
    assertEquals(2, draft.getTransitions().size());

    WorkflowGraph.Edge added = edge(graph, "Send");
    WorkflowGraph.Edge untouched = edge(graph, "Submit");
    assertEquals(Boolean.FALSE, added.getAllowAllRoles());
    assertEquals(List.of("Editor", "Author"), added.getAllowedRoles());
    assertEquals(Boolean.TRUE, untouched.getAllowAllRoles());
    assertNull(untouched.getAllowedRoles());
    assertEquals(4, added.getApprovalsRequired());
    assertFalse(added.isCommentRequired());
    assertEquals(Boolean.FALSE, added.getDefaultTransition());
    WorkflowGraph.Edge agingEdge = edge(graph, "Expire");
    assertTrue(agingEdge.isAging());
    assertNull(agingEdge.getAllowAllRoles());
    assertNull(agingEdge.getAllowedRoles());
    verify(workflowService).saveWorkflow(wf);
  }

  @Test
  void addRoleOnAllowAllIs409AndDoesNotSave() {
    PSTransition send = allowAllSend();
    PSWorkflow wf = workflowWith(send);
    stub(wf);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                adaptor.addTransitionAllowedRole(
                    null, "Nightly QA", "Draft", "Send", "Live", "Editor"));
    assertEquals(409, ex.getResponse().getStatus());
    assertTrue(send.isAllowAllRoles());
    assertTrue(send.getTransitionRoles().isEmpty());
    verify(workflowService, never()).saveWorkflow(wf);
  }

  @Test
  void addRoleAlreadyOnTheListIs409AndKeepsThePreviousRole() {
    PSWorkflowRole editor = role(12, "Editor");
    PSWorkflowRole author = role(11, "Author");
    PSTransition send = restricted(transition(22, "Send", 3), 12L);
    PSState draft = state(1, "Draft");
    draft.addTransition(send);
    PSWorkflow wf = workflow("Nightly QA", List.of(editor, author), draft, state(3, "Live"));
    stub(wf);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                adaptor.addTransitionAllowedRole(
                    null, "Nightly QA", "Draft", "Send", "Live", "Editor"));
    assertEquals(409, ex.getResponse().getStatus());
    assertFalse(send.isAllowAllRoles());
    assertEquals(1, send.getTransitionRoles().size());
    assertEquals(12L, send.getTransitionRoles().get(0).getRoleId());
    verify(workflowService, never()).saveWorkflow(wf);
  }

  @Test
  void addAllowAllMarkerIs409AndKeepsThePreviousRole() {
    PSTransition send = restricted(transition(22, "Send", 3), 12L);
    PSState draft = state(1, "Draft");
    draft.addTransition(send);
    PSWorkflow wf = workflow("Nightly QA", List.of(role(12, "Editor")), draft, state(3, "Live"));
    stub(wf);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                adaptor.addTransitionAllowedRole(
                    null, "Nightly QA", "Draft", "Send", "Live", "*ALL*"));
    assertEquals(409, ex.getResponse().getStatus());
    assertFalse(send.isAllowAllRoles());
    assertEquals(List.of(12L), roleIds(send));
    verify(workflowService, never()).saveWorkflow(wf);
  }

  @Test
  void addUnknownRoleIs404AndKeepsThePreviousRole() {
    PSTransition send = restricted(transition(22, "Send", 3), 12L);
    PSState draft = state(1, "Draft");
    draft.addTransition(send);
    PSWorkflow wf =
        workflow("Nightly QA", List.of(role(12, "Editor"), role(11, "Author")), draft, state(3, "Live"));
    stub(wf);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                adaptor.addTransitionAllowedRole(
                    null, "Nightly QA", "Draft", "Send", "Live", "Missing"));
    assertEquals(404, ex.getResponse().getStatus());
    assertFalse(send.isAllowAllRoles());
    assertEquals(List.of(12L), roleIds(send));
    verify(workflowService, never()).saveWorkflow(wf);
  }

  @Test
  void addRoleOnAgingIs400AndKeepsThePreviousRole() {
    PSTransition publish = restricted(transition(30, "Publish", 5), 12L);
    PSState live = state(4, "Live");
    live.addTransition(publish);
    PSAgingTransition expire = aging("Expire", 5);
    live.addAgingTransition(expire);
    PSWorkflow wf =
        workflow(
            "Nightly QA",
            List.of(role(12, "Editor"), role(11, "Author")),
            live,
            state(5, "Archive"));
    stub(wf);

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                adaptor.addTransitionAllowedRole(
                    null, "Nightly QA", "Live", "Expire", "Archive", "Author"));
    assertTrue(ex.getMessage().toLowerCase().contains("aging"));
    assertFalse(publish.isAllowAllRoles());
    assertEquals(List.of(12L), roleIds(publish));
    assertEquals("Expire", live.getAgingTransitions().get(0).getLabel());
    verify(workflowService, never()).saveWorkflow(wf);
  }

  @Test
  void addRoleAmbiguousLabelIs400AndKeepsThePreviousRoles() {
    PSTransition sendReview = restricted(transition(22, "Send", 2), 12L);
    PSTransition sendLive = restricted(transition(23, "Send", 3), 12L);
    PSState draft = state(1, "Draft");
    draft.addTransition(sendReview);
    draft.addTransition(sendLive);
    PSWorkflow wf =
        workflow(
            "Nightly QA",
            List.of(role(12, "Editor"), role(11, "Author")),
            draft,
            state(2, "Review"),
            state(3, "Live"));
    stub(wf);

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                adaptor.addTransitionAllowedRole(
                    null, "Nightly QA", "Draft", "Send", null, "Author"));
    assertTrue(ex.getMessage().toLowerCase().contains("specify to"));
    assertEquals(List.of(12L), roleIds(sendReview));
    assertEquals(List.of(12L), roleIds(sendLive));
    assertFalse(sendReview.isAllowAllRoles());
    assertFalse(sendLive.isAllowAllRoles());
    verify(workflowService, never()).saveWorkflow(wf);
  }

  @Test
  void addRoleOnPackagedWorkflowIs403AndKeepsThePreviousRole() {
    PSTransition send = restricted(transition(22, "Send", 3), 12L);
    PSState draft = state(1, "Draft");
    draft.addTransition(send);
    PSWorkflow wf = workflow("Simple Workflow", List.of(role(12, "Editor"), role(11, "Author")), draft, state(3, "Live"));
    stub(wf);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                adaptor.addTransitionAllowedRole(
                    null, "Simple Workflow", "Draft", "Send", "Live", "Author"));
    assertEquals(403, ex.getResponse().getStatus());
    assertFalse(send.isAllowAllRoles());
    assertEquals(List.of(12L), roleIds(send));
    verify(workflowService, never()).saveWorkflow(wf);
  }

  private static PSTransition restricted(PSTransition transition, long roleId) {
    transition.setAllowAllRoles(false);
    PSTransitionRole existing = new PSTransitionRole();
    existing.setRoleId(roleId);
    existing.setTransitionId(transition.getGUID().longValue());
    existing.setWorkflowId(7L);
    transition.setTransitionRoles(new ArrayList<>(List.of(existing)));
    return transition;
  }

  private static List<Long> roleIds(PSTransition transition) {
    List<Long> ids = new ArrayList<>();
    for (PSTransitionRole role : transition.getTransitionRoles()) {
      ids.add(role.getRoleId());
    }
    return ids;
  }

  private PSTransition allowAllSend() {
    return transition(22, "Send", 3);
  }

  private PSWorkflow workflowWith(PSTransition send) {
    PSState draft = state(1, "Draft");
    draft.addTransition(send);
    return workflow("Nightly QA", List.of(role(12, "Editor")), draft, state(3, "Live"));
  }

  private void stub(PSWorkflow wf) {
    when(workflowService.findWorkflowsByName(wf.getName())).thenReturn(List.of(wf));
    PSUiWorkflow ui = new PSUiWorkflow();
    ui.setDefaultWorkflow(false);
    when(stepped.getWorkflow(wf.getName())).thenReturn(ui);
  }

  private static WorkflowGraph.Edge edge(WorkflowGraph graph, String label) {
    for (WorkflowGraph.Edge edge : graph.getEdges()) {
      if (label.equals(edge.getLabel())) {
        return edge;
      }
    }
    throw new AssertionError("missing edge " + label);
  }

  private static PSWorkflow workflow(String name, List<PSWorkflowRole> roles, PSState... states) {
    PSWorkflow wf = new PSWorkflow();
    wf.setName(name);
    wf.setGUID(new PSGuid(PSTypeEnum.WORKFLOW, 7));
    wf.setRoles(new ArrayList<>(roles));
    List<PSState> stateList = new ArrayList<>();
    for (PSState state : states) {
      stateList.add(state);
    }
    wf.setStates(stateList);
    return wf;
  }

  private static PSWorkflowRole role(long id, String name) {
    PSWorkflowRole role = new PSWorkflowRole();
    role.setGUID(new PSGuid(PSTypeEnum.WORKFLOW_ROLE, id));
    role.setName(name);
    role.setWorkflowId(7L);
    return role;
  }

  private static PSState state(long id, String name) {
    PSState state = new PSState();
    state.setStateId(id);
    state.setWorkflowId(7L);
    state.setName(name);
    return state;
  }

  private static PSTransition transition(long id, String label, long toState) {
    PSTransition transition = new PSTransition();
    transition.setGUID(new PSGuid(PSTypeEnum.WORKFLOW_TRANSITION, id));
    transition.setLabel(label);
    transition.setTrigger(label);
    transition.setToState(toState);
    transition.setAllowAllRoles(true);
    return transition;
  }

  private static PSAgingTransition aging(String label, long toState) {
    PSAgingTransition aging = new PSAgingTransition();
    aging.setLabel(label);
    aging.setTrigger(label);
    aging.setToState(toState);
    return aging;
  }
}
