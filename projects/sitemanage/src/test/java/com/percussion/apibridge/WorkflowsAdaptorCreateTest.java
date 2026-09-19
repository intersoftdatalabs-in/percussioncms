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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.workflows.WorkflowCreate;
import com.percussion.rest.workflows.WorkflowSummary;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.services.workflow.data.PSWorkflow;
import com.percussion.share.data.PSEnumVals;
import com.percussion.utils.request.PSRequestInfoBase;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.workflow.data.PSUiWorkflow;
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

/**
 * Slice 21 Developer workflow create: Admin POST, name validation (400), duplicate name (409),
 * non-Admin (403), and stepped-service delegation with base-template defaults.
 */
@Tag("UnitTest")
class WorkflowsAdaptorCreateTest {

  private IPSContentDesignWs designWs;
  private IPSWorkflowService workflowService;
  private IPSContentMgr contentMgr;
  private IPSSteppedWorkflowService stepped;
  private WorkflowsAdaptor adaptor;

  @BeforeEach
  void setUp() {
    PSRequestInfoBase.initRequestInfo(new HashMap<>());
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_JSESSIONID, "test-session");
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_USER, "Admin");
    designWs = mock(IPSContentDesignWs.class);
    workflowService = mock(IPSWorkflowService.class);
    contentMgr = mock(IPSContentMgr.class);
    stepped = mock(IPSSteppedWorkflowService.class);
    when(stepped.getWorkflowList()).thenReturn(new PSEnumVals());
    adaptor = new WorkflowsAdaptor(designWs, workflowService, contentMgr, () -> true, stepped);
  }

  @AfterEach
  void tearDown() {
    PSRequestInfoBase.resetRequestInfo();
  }

  private static WorkflowCreate body(String name, String description) {
    WorkflowCreate create = new WorkflowCreate();
    create.setName(name);
    create.setDescription(description);
    return create;
  }

  private static PSUiWorkflow createdUi(String name) {
    PSUiWorkflow ui = new PSUiWorkflow();
    ui.setWorkflowName(name);
    ui.setWorkflowDescription("Created by night-issue-prs");
    ui.setDefaultWorkflow(false);
    return ui;
  }

  @Test
  void create_delegatesToSteppedServiceWithDefaults() {
    when(stepped.createWorkflow(eq("Nightly QA"), any())).thenReturn(createdUi("Nightly QA"));
    PSWorkflow persisted = mock(PSWorkflow.class);
    when(persisted.getName()).thenReturn("Nightly QA");
    when(workflowService.findWorkflowsByName("Nightly QA")).thenReturn(List.of(persisted));

    WorkflowSummary out = adaptor.createWorkflow(null, body("Nightly QA", "  Created  "));

    assertEquals("Nightly QA", out.getWorkflowName());
    assertEquals("Created", out.getWorkflowDescription());
    assertFalse(out.isDefaultWorkflow());

    ArgumentCaptor<PSUiWorkflow> captor = ArgumentCaptor.forClass(PSUiWorkflow.class);
    verify(stepped).createWorkflow(eq("Nightly QA"), captor.capture());
    PSUiWorkflow sent = captor.getValue();
    assertEquals("Nightly QA", sent.getWorkflowName());
    assertEquals("Created", sent.getWorkflowDescription());
    assertEquals("", sent.getStagingRoleNames());
    assertFalse(sent.isDefaultWorkflow());

    // The stepped editor drops descriptions; the adaptor stores it with a follow-up save.
    verify(persisted).setDescription("Created");
    verify(workflowService).saveWorkflow(persisted);
  }

  @Test
  void create_blankDescriptionSkipsFollowUpSave() {
    when(stepped.createWorkflow(eq("Nightly QA"), any())).thenReturn(createdUi("Nightly QA"));

    WorkflowSummary out = adaptor.createWorkflow(null, body("Nightly QA", "  "));

    assertEquals("Nightly QA", out.getWorkflowName());
    verify(workflowService, never()).saveWorkflow(any());
  }

  @Test
  void create_blankNameIs400() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> adaptor.createWorkflow(null, body("  ", null)));
    assertEquals("Workflow name is required", ex.getMessage());
    verify(stepped, never()).createWorkflow(any(), any());
  }

  @Test
  void create_nullBodyIs400() {
    assertThrows(IllegalArgumentException.class, () -> adaptor.createWorkflow(null, null));
    verify(stepped, never()).createWorkflow(any(), any());
  }

  @Test
  void create_invalidCharactersAre400() {
    assertThrows(
        IllegalArgumentException.class,
        () -> adaptor.createWorkflow(null, body("Bad/Name!", null)));
    verify(stepped, never()).createWorkflow(any(), any());
  }

  @Test
  void create_tooLongNameIs400() {
    assertThrows(
        IllegalArgumentException.class,
        () -> adaptor.createWorkflow(null, body("N".repeat(51), null)));
    verify(stepped, never()).createWorkflow(any(), any());
  }

  @Test
  void create_duplicateNameIs409() {
    PSEnumVals list = new PSEnumVals();
    list.addEntry("Simple Workflow", "4");
    when(stepped.getWorkflowList()).thenReturn(list);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.createWorkflow(null, body("simple workflow", null)));
    assertEquals(409, ex.getResponse().getStatus());
    verify(stepped, never()).createWorkflow(any(), any());
  }

  @Test
  void create_serviceRaceOnDuplicateIs409() {
    when(stepped.createWorkflow(any(), any()))
        .thenThrow(
            new PSWorkflowEditorServiceException(
                "Cannot create workflow 'Nightly QA' because a workflow named 'Nightly QA' already"
                    + " exists."));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.createWorkflow(null, body("Nightly QA", null)));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  void create_serviceValidationFailureIs400() {
    when(stepped.createWorkflow(any(), any()))
        .thenThrow(new PSWorkflowEditorServiceException("Workflow name cannot be blank."));

    assertThrows(
        IllegalArgumentException.class,
        () -> adaptor.createWorkflow(null, body("Nightly QA", null)));
  }

  @Test
  void create_nonAdminIs403() {
    WorkflowsAdaptor locked =
        new WorkflowsAdaptor(designWs, workflowService, contentMgr, () -> false, stepped);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> locked.createWorkflow(null, body("Nightly QA", null)));
    assertEquals(403, ex.getResponse().getStatus());
    verify(stepped, never()).createWorkflow(any(), any());
  }

  @Test
  void create_missingSessionIs403() {
    PSRequestInfoBase.resetRequestInfo();
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.createWorkflow(null, body("Nightly QA", null)));
    assertEquals(403, ex.getResponse().getStatus());
    verify(stepped, never()).createWorkflow(any(), any());
  }

  @Test
  void create_withoutSteppedServiceFails() {
    WorkflowsAdaptor bare = new WorkflowsAdaptor(designWs, workflowService, contentMgr, () -> true);
    assertThrows(
        IllegalStateException.class,
        () -> bare.createWorkflow(null, body("Nightly QA", null)));
  }
}
