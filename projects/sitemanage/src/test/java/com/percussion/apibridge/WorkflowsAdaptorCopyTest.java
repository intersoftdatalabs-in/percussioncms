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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.workflows.WorkflowCreate;
import com.percussion.rest.workflows.WorkflowSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.services.workflow.data.PSState;
import com.percussion.services.workflow.data.PSTransition;
import com.percussion.services.workflow.data.PSWorkflow;
import com.percussion.share.data.PSEnumVals;
import com.percussion.utils.request.PSRequestInfoBase;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.workflow.service.IPSSteppedWorkflowService;
import jakarta.ws.rs.WebApplicationException;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Slice 36: copy keeps source steps and transitions under a new name and refuses a name collision
 * without saving.
 */
@Tag("UnitTest")
class WorkflowsAdaptorCopyTest {

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
    PSEnumVals catalog = new PSEnumVals();
    catalog.addEntry("Simple Workflow", "7");
    when(stepped.getWorkflowList()).thenReturn(catalog);
    adaptor =
        new WorkflowsAdaptor(
            mock(IPSContentDesignWs.class),
            workflowService,
            mock(IPSContentMgr.class),
            () -> true,
            stepped,
            () -> new PSGuid(PSTypeEnum.WORKFLOW, 99));
  }

  @AfterEach
  void tearDown() {
    PSRequestInfoBase.resetRequestInfo();
  }

  @Test
  void copy_persistsDistinctWorkflowWithSourceStepsAndTransitions() {
    PSWorkflow source = sourceWorkflow();
    when(workflowService.findWorkflowsByName("Simple Workflow")).thenReturn(List.of(source));

    WorkflowCreate body = new WorkflowCreate();
    body.setName("Nightly Copy");
    body.setDescription("copied desc");

    WorkflowSummary out = adaptor.copyWorkflow(null, "Simple Workflow", body);

    assertEquals("Nightly Copy", out.getWorkflowName());
    assertEquals("copied desc", out.getWorkflowDescription());
    ArgumentCaptor<PSWorkflow> saved = ArgumentCaptor.forClass(PSWorkflow.class);
    verify(workflowService).saveWorkflow(saved.capture());
    PSWorkflow copy = saved.getValue();
    assertEquals("Nightly Copy", copy.getName());
    assertEquals(99L, copy.getGUID().longValue());
    assertNotEquals(source.getGUID().longValue(), copy.getGUID().longValue());
    assertEquals("Simple Workflow", source.getName());
    assertEquals(7L, source.getGUID().longValue());
    assertEquals(1, copy.getStates().size());
    PSState copiedState = copy.getStates().get(0);
    assertEquals("Draft", copiedState.getName());
    assertEquals(99L, copiedState.getWorkflowId());
    assertEquals(1, copiedState.getTransitions().size());
    PSTransition copiedTransition = copiedState.getTransitions().get(0);
    assertEquals("Submit", copiedTransition.getLabel());
    assertEquals(99L, copiedTransition.getWorkflowId());
    assertEquals(2L, copiedTransition.getToState());
    assertEquals(7L, source.getStates().get(0).getWorkflowId());
    assertEquals(7L, source.getStates().get(0).getTransitions().get(0).getWorkflowId());
  }

  @Test
  void copy_duplicateNameDoesNotSave() {
    WorkflowCreate body = new WorkflowCreate();
    body.setName("simple workflow");

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.copyWorkflow(null, "Other", body));
    assertEquals(409, ex.getResponse().getStatus());
    verify(workflowService, never()).saveWorkflow(any());
    verify(workflowService, never()).findWorkflowsByName(any());
  }

  @Test
  void copy_missingSourceIs404() {
    when(workflowService.findWorkflowsByName("Missing")).thenReturn(List.of());
    WorkflowCreate body = new WorkflowCreate();
    body.setName("Nightly Copy");

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> adaptor.copyWorkflow(null, "Missing", body));
    assertEquals(404, ex.getResponse().getStatus());
    verify(workflowService, never()).saveWorkflow(any());
  }

  @Test
  void copy_invalidNameIs400() {
    WorkflowCreate body = new WorkflowCreate();
    body.setName("bad/name");
    assertThrows(IllegalArgumentException.class, () -> adaptor.copyWorkflow(null, "Simple Workflow", body));
    verify(workflowService, never()).saveWorkflow(any());
  }

  private static PSWorkflow sourceWorkflow() {
    PSWorkflow source = new PSWorkflow();
    source.setGUID(new PSGuid(PSTypeEnum.WORKFLOW, 7));
    source.setName("Simple Workflow");
    source.setDescription("source desc");
    PSState state = new PSState();
    state.setStateId(1);
    state.setWorkflowId(7);
    state.setName("Draft");
    PSTransition transition = new PSTransition();
    transition.setGUID(new PSGuid(PSTypeEnum.WORKFLOW_TRANSITION, 11));
    transition.setWorkflowId(7);
    transition.setStateId(1);
    transition.setToState(2);
    transition.setLabel("Submit");
    transition.setTrigger("Submit");
    state.setTransitions(List.of(transition));
    source.setStates(List.of(state));
    return source;
  }
}
