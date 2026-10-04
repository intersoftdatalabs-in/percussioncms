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

import com.percussion.rest.workflows.WorkflowRename;
import com.percussion.rest.workflows.WorkflowSummary;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.services.workflow.data.PSWorkflow;
import com.percussion.utils.request.PSRequestInfoBase;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.workflow.data.PSUiWorkflow;
import com.percussion.workflow.service.IPSSteppedWorkflowService;
import com.percussion.workflow.service.IPSSteppedWorkflowService.PSWorkflowEditorServiceException;
import jakarta.ws.rs.WebApplicationException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Slice 60: dedicated workflow rename delegates to stepped {@code updateWorkflow} and leaves
 * description-only PUT behavior alone.
 */
@Tag("UnitTest")
class WorkflowsAdaptorRenameTest {

  private static final String NAME = "Nightly QA";

  private IPSWorkflowService workflowService;
  private IPSSteppedWorkflowService stepped;
  private WorkflowsAdaptor adaptor;

  @BeforeEach
  void setUp() {
    PSRequestInfoBase.initRequestInfo(new HashMap<>());
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_JSESSIONID, "test-session");
    PSRequestInfoBase.setRequestInfo(PSRequestInfoBase.KEY_USER, "Admin");
    IPSContentDesignWs designWs = mock(IPSContentDesignWs.class);
    workflowService = mock(IPSWorkflowService.class);
    IPSContentMgr contentMgr = mock(IPSContentMgr.class);
    stepped = mock(IPSSteppedWorkflowService.class);
    adaptor = new WorkflowsAdaptor(designWs, workflowService, contentMgr, () -> true, stepped);
  }

  @AfterEach
  void tearDown() {
    PSRequestInfoBase.resetRequestInfo();
  }

  @Test
  void rename_delegatesNameAndKeepsStagingRoles() throws Exception {
    stubStored(NAME, false, "Editor;Admin", "Keep me");
    PSUiWorkflow saved = new PSUiWorkflow();
    saved.setWorkflowName("Nightly QA 2");
    saved.setWorkflowDescription("Keep me");
    saved.setStagingRoleNames("Editor;Admin");
    saved.setDefaultWorkflow(false);
    when(stepped.updateWorkflow(eq(NAME), any())).thenReturn(saved);

    WorkflowSummary out = adaptor.renameWorkflow(null, NAME, body("Nightly QA 2"));
    assertEquals("Nightly QA 2", out.getWorkflowName());
    assertEquals("Keep me", out.getWorkflowDescription());
    assertFalse(out.isDefaultWorkflow());

    ArgumentCaptor<PSUiWorkflow> captor = ArgumentCaptor.forClass(PSUiWorkflow.class);
    verify(stepped).updateWorkflow(eq(NAME), captor.capture());
    PSUiWorkflow sent = captor.getValue();
    assertEquals("Nightly QA 2", sent.getWorkflowName());
    assertEquals(NAME, sent.getPreviousWorkflowName());
    assertEquals("Editor;Admin", sent.getStagingRoleNames());
    assertFalse(sent.isDefaultWorkflow());
    assertEquals(0, sent.getWorkflowSteps().size());
    verify(workflowService, never()).saveWorkflow(any());
  }

  @Test
  void rename_sameNameDoesNotCallSteppedUpdate() throws Exception {
    stubStored(NAME, false, "Editor", "Keep me");

    WorkflowSummary out = adaptor.renameWorkflow(null, NAME, body(NAME));
    assertEquals(NAME, out.getWorkflowName());
    assertEquals("Keep me", out.getWorkflowDescription());
    verify(stepped, never()).updateWorkflow(any(), any());
    verify(workflowService, never()).saveWorkflow(any());
  }

  @Test
  void rename_resolvesNumericIdToStoredName() throws Exception {
    PSWorkflow stored = mock(PSWorkflow.class);
    when(stored.getName()).thenReturn(NAME);
    when(workflowService.findWorkflow(any())).thenReturn(Optional.of(stored));
    stubUi(NAME, false, "Editor", "Keep me");
    PSUiWorkflow saved = new PSUiWorkflow();
    saved.setWorkflowName("Nightly QA 2");
    saved.setWorkflowDescription("Keep me");
    when(stepped.updateWorkflow(eq(NAME), any())).thenReturn(saved);

    WorkflowSummary out = adaptor.renameWorkflow(null, "4", body("Nightly QA 2"));
    assertEquals("Nightly QA 2", out.getWorkflowName());
    verify(stepped).updateWorkflow(eq(NAME), any());
  }

  @Test
  void rename_packagedNamesAre403() throws Exception {
    for (String packaged : List.of("Default Workflow", "Simple Workflow", "Local Content", "LocalContent")) {
      PSWorkflow stored = mock(PSWorkflow.class);
      when(stored.getName()).thenReturn(packaged);
      when(workflowService.findWorkflowsByName(packaged)).thenReturn(List.of(stored));
      WebApplicationException ex =
          assertThrows(
              WebApplicationException.class,
              () -> adaptor.renameWorkflow(null, packaged, body("Custom Name")));
      assertEquals(403, ex.getResponse().getStatus(), packaged);
    }
    verify(stepped, never()).updateWorkflow(any(), any());
  }

  @Test
  void rename_systemDefaultIs403() throws Exception {
    stubStored(NAME, true, "Editor", "Keep me");
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.renameWorkflow(null, NAME, body("Nightly QA 2")));
    assertEquals(403, ex.getResponse().getStatus());
    verify(stepped, never()).updateWorkflow(any(), any());
  }

  @Test
  void rename_duplicateNameIs409() throws Exception {
    stubStored(NAME, false, "Editor", "Keep me");
    when(stepped.updateWorkflow(eq(NAME), any()))
        .thenThrow(
            new PSWorkflowEditorServiceException(
                "Cannot rename workflow 'Nightly QA' to 'Simple Workflow' because a workflow"
                    + " named 'Simple Workflow' already exists."));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.renameWorkflow(null, NAME, body("Simple Workflow")));
    assertEquals(409, ex.getResponse().getStatus());
    verify(workflowService, never()).saveWorkflow(any());
  }

  @Test
  void rename_invalidNameIs400() throws Exception {
    assertThrows(
        IllegalArgumentException.class, () -> adaptor.renameWorkflow(null, NAME, body("Bad!")));
    verify(stepped, never()).updateWorkflow(any(), any());
    verify(workflowService, never()).saveWorkflow(any());
  }

  @Test
  void rename_blankAndNullBodyAre400() throws Exception {
    assertThrows(
        IllegalArgumentException.class, () -> adaptor.renameWorkflow(null, NAME, body("  ")));
    assertThrows(
        IllegalArgumentException.class, () -> adaptor.renameWorkflow(null, NAME, null));
    verify(stepped, never()).updateWorkflow(any(), any());
  }

  @Test
  void rename_missingWorkflowIs404() throws Exception {
    when(workflowService.findWorkflowsByName("missing")).thenReturn(List.of());
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.renameWorkflow(null, "missing", body("Other")));
    assertEquals(404, ex.getResponse().getStatus());
    verify(stepped, never()).updateWorkflow(any(), any());
  }

  @Test
  void rename_nonAdminIs403() throws Exception {
    WorkflowsAdaptor locked =
        new WorkflowsAdaptor(
            mock(IPSContentDesignWs.class),
            workflowService,
            mock(IPSContentMgr.class),
            () -> false,
            stepped);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class, () -> locked.renameWorkflow(null, NAME, body("Other")));
    assertEquals(403, ex.getResponse().getStatus());
    verify(stepped, never()).updateWorkflow(any(), any());
  }

  @Test
  void descriptionPutStillRejectsMismatchedName() {
    PSWorkflow stored = mock(PSWorkflow.class);
    when(stored.getName()).thenReturn(NAME);
    when(workflowService.findWorkflowsByName(NAME)).thenReturn(List.of(stored));
    com.percussion.rest.workflows.WorkflowUpdate update =
        new com.percussion.rest.workflows.WorkflowUpdate();
    update.setName("Other");
    update.setDescription("Edited");
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> adaptor.updateWorkflow(null, NAME, update));
    assertEquals(
        "Workflow update body name does not match path idOrName: Other != Nightly QA",
        ex.getMessage());
    verify(workflowService, never()).saveWorkflow(any());
  }

  private void stubStored(String name, boolean isDefault, String staging, String description)
      throws Exception {
    PSWorkflow stored = mock(PSWorkflow.class);
    when(stored.getName()).thenReturn(name);
    when(workflowService.findWorkflowsByName(name)).thenReturn(List.of(stored));
    stubUi(name, isDefault, staging, description);
  }

  private void stubUi(String name, boolean isDefault, String staging, String description)
      throws Exception {
    PSUiWorkflow ui = new PSUiWorkflow();
    ui.setWorkflowName(name);
    ui.setDefaultWorkflow(isDefault);
    ui.setStagingRoleNames(staging);
    ui.setWorkflowDescription(description);
    when(stepped.getWorkflow(name)).thenReturn(ui);
  }

  private static WorkflowRename body(String name) {
    WorkflowRename rename = new WorkflowRename();
    rename.setName(name);
    return rename;
  }
}
