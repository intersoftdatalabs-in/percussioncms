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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.workflows.WorkflowStepWrite;
import com.percussion.rest.workflows.WorkflowSummary;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.services.workflow.data.PSWorkflow;
import com.percussion.utils.request.PSRequestInfoBase;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.workflow.data.PSUiWorkflow;
import com.percussion.workflow.data.PSUiWorkflowStep;
import com.percussion.workflow.service.IPSSteppedWorkflowService;
import com.percussion.workflow.service.IPSSteppedWorkflowService.PSWorkflowEditorServiceException;
import jakarta.ws.rs.WebApplicationException;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@Tag("UnitTest")
class WorkflowsAdaptorStepWriteTest {

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

  private static WorkflowStepWrite body(String name, String after) {
    WorkflowStepWrite w = new WorkflowStepWrite();
    w.setName(name);
    w.setAfterStep(after);
    return w;
  }

  private void stubCustomWorkflow(String name) {
    PSWorkflow wf = mock(PSWorkflow.class);
    when(wf.getName()).thenReturn(name);
    when(workflowService.findWorkflowsByName(name)).thenReturn(List.of(wf));
    PSUiWorkflow ui = new PSUiWorkflow();
    ui.setWorkflowName(name);
    ui.setDefaultWorkflow(false);
    PSUiWorkflowStep draft = new PSUiWorkflowStep();
    draft.setStepName("Draft");
    ui.getWorkflowSteps().add(draft);
    try {
      when(stepped.getWorkflow(name)).thenReturn(ui);
    } catch (PSWorkflowEditorServiceException e) {
      throw new IllegalStateException(e);
    }
  }

  @Test
  void createStep_delegatesToSteppedService() throws Exception {
    stubCustomWorkflow("Nightly QA");
    PSUiWorkflow saved = new PSUiWorkflow();
    saved.setWorkflowName("Nightly QA");
    when(stepped.createStep(eq("Nightly QA"), eq("Review"), any())).thenReturn(saved);

    WorkflowSummary out = adaptor.createWorkflowStep(null, "Nightly QA", body("Review", "Draft"));
    assertEquals("Nightly QA", out.getWorkflowName());

    ArgumentCaptor<PSUiWorkflow> captor = ArgumentCaptor.forClass(PSUiWorkflow.class);
    verify(stepped).createStep(eq("Nightly QA"), eq("Review"), captor.capture());
    assertEquals("Draft", captor.getValue().getPreviousStepName());
    assertEquals("Review", captor.getValue().getWorkflowSteps().get(0).getStepName());
  }

  @Test
  void createStep_packagedDefaultIs403() {
    PSWorkflow wf = mock(PSWorkflow.class);
    when(wf.getName()).thenReturn("Default Workflow");
    when(workflowService.findWorkflowsByName("Default Workflow")).thenReturn(List.of(wf));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.createWorkflowStep(null, "Default Workflow", body("Review", "Draft")));
    assertEquals(403, ex.getResponse().getStatus());
    try {
      verify(stepped, never()).createStep(any(), any(), any());
    } catch (PSWorkflowEditorServiceException e) {
      throw new IllegalStateException(e);
    }
  }

  @Test
  void createStep_blankNameIs400() {
    stubCustomWorkflow("Nightly QA");
    assertThrows(
        IllegalArgumentException.class,
        () -> adaptor.createWorkflowStep(null, "Nightly QA", body("  ", "Draft")));
  }

  @Test
  void createStep_duplicateIs409() throws Exception {
    stubCustomWorkflow("Nightly QA");
    when(stepped.createStep(any(), any(), any()))
        .thenThrow(new PSWorkflowEditorServiceException("Step already exists"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.createWorkflowStep(null, "Nightly QA", body("Draft", "Draft")));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  void createStep_nonAdminIs403() {
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
            () -> locked.createWorkflowStep(null, "Nightly QA", body("Review", "Draft")));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void updateStep_delegatesToSteppedService() throws Exception {
    stubCustomWorkflow("Nightly QA");
    PSUiWorkflow saved = new PSUiWorkflow();
    saved.setWorkflowName("Nightly QA");
    when(stepped.updateStep(eq("Nightly QA"), eq("Review"), any())).thenReturn(saved);

    WorkflowSummary out =
        adaptor.updateWorkflowStep(null, "Nightly QA", "Review", body("Review 2", null));
    assertEquals("Nightly QA", out.getWorkflowName());
    ArgumentCaptor<PSUiWorkflow> captor = ArgumentCaptor.forClass(PSUiWorkflow.class);
    verify(stepped).updateStep(eq("Nightly QA"), eq("Review"), captor.capture());
    assertEquals("Review", captor.getValue().getPreviousStepName());
    assertEquals("Review 2", captor.getValue().getWorkflowSteps().get(0).getStepName());
  }

  @Test
  void updateStep_missingWorkflowIs404() {
    when(workflowService.findWorkflowsByName("missing")).thenReturn(List.of());
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.updateWorkflowStep(null, "missing", "Draft", body("Draft 2", null)));
    assertEquals(404, ex.getResponse().getStatus());
  }
}
