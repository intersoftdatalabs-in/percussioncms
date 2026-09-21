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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.workflows.WorkflowSummary;
import com.percussion.rest.workflows.WorkflowUpdate;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Slice 21 Developer workflow update / delete: Admin PUT description, Admin DELETE; 403/404/409
 * mapping; stepped-service delegation.
 */
@Tag("UnitTest")
class WorkflowsAdaptorUpdateDeleteTest {

  private static final String NAME = "Nightly QA";

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
    adaptor = new WorkflowsAdaptor(designWs, workflowService, contentMgr, () -> true, stepped);
  }

  @AfterEach
  void tearDown() {
    PSRequestInfoBase.resetRequestInfo();
  }

  private static WorkflowUpdate body(String name, String description) {
    WorkflowUpdate update = new WorkflowUpdate();
    update.setName(name);
    update.setDescription(description);
    return update;
  }

  private PSWorkflow mockStoredWorkflow(String storedName) {
    PSWorkflow wf = mock(PSWorkflow.class);
    when(wf.getName()).thenReturn(storedName);
    return wf;
  }

  // --- update ---

  @Test
  void update_storesDescriptionOnWorkflow() {
    PSWorkflow stored = mockStoredWorkflow(NAME);
    when(workflowService.findWorkflowsByName(NAME)).thenReturn(List.of(stored));
    PSUiWorkflow refreshed = new PSUiWorkflow();
    refreshed.setWorkflowName(NAME);
    refreshed.setWorkflowDescription("Edited");
    refreshed.setDefaultWorkflow(false);
    when(stepped.getWorkflow(NAME)).thenReturn(refreshed);

    WorkflowSummary out = adaptor.updateWorkflow(null, NAME, body(NAME, "Edited"));
    assertEquals(NAME, out.getWorkflowName());
    assertEquals("Edited", out.getWorkflowDescription());
    verify(workflowService).saveWorkflow(stored);
    verify(stored).setDescription("Edited");
  }

  @Test
  void update_emptyStringClearsStoredDescription() {
    PSWorkflow stored = mockStoredWorkflow(NAME);
    when(workflowService.findWorkflowsByName(NAME)).thenReturn(List.of(stored));
    PSUiWorkflow prior = new PSUiWorkflow();
    prior.setWorkflowName(NAME);
    prior.setWorkflowDescription("Prior stale text");
    prior.setDefaultWorkflow(false);
    when(stepped.getWorkflow(NAME)).thenReturn(prior);

    WorkflowSummary out = adaptor.updateWorkflow(null, NAME, body(NAME, ""));
    assertEquals("", out.getWorkflowDescription());
    verify(workflowService).saveWorkflow(stored);
    verify(stored).setDescription("");
  }

  @Test
  void update_nullDescriptionIsNoOp() {
    PSWorkflow stored = mockStoredWorkflow(NAME);
    when(workflowService.findWorkflowsByName(NAME)).thenReturn(List.of(stored));
    when(stepped.getWorkflow(NAME)).thenReturn(new PSUiWorkflow());

    adaptor.updateWorkflow(null, NAME, body(NAME, null));
    verify(workflowService, never()).saveWorkflow(any());
  }

  @Test
  void update_nameMismatchIs400() {
    PSWorkflow stored = mockStoredWorkflow(NAME);
    when(workflowService.findWorkflowsByName(NAME)).thenReturn(List.of(stored));

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> adaptor.updateWorkflow(null, NAME, body("Other", "Edited")));
    assertEquals(
        "Workflow update body name does not match path idOrName: Other != Nightly QA",
        ex.getMessage());
    verify(workflowService, never()).saveWorkflow(any());
  }

  @Test
  void update_blankNameIs400() {
    assertThrows(
        IllegalArgumentException.class,
        () -> adaptor.updateWorkflow(null, NAME, body("  ", "Edited")));
    verify(workflowService, never()).saveWorkflow(any());
  }

  @Test
  void update_nullBodyIs400() {
    assertThrows(
        IllegalArgumentException.class, () -> adaptor.updateWorkflow(null, NAME, null));
    verify(workflowService, never()).saveWorkflow(any());
  }

  @Test
  void update_missingWorkflowIs404() {
    when(workflowService.findWorkflowsByName(eq("missing"))).thenReturn(List.of());

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.updateWorkflow(null, "missing", body("missing", "Edited")));
    assertEquals(404, ex.getResponse().getStatus());
    verify(workflowService, never()).saveWorkflow(any());
  }

  @Test
  void update_nonAdminIs403() {
    WorkflowsAdaptor locked =
        new WorkflowsAdaptor(designWs, workflowService, contentMgr, () -> false, stepped);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> locked.updateWorkflow(null, NAME, body(NAME, "Edited")));
    assertEquals(403, ex.getResponse().getStatus());
    verify(workflowService, never()).saveWorkflow(any());
  }

  @Test
  void update_missingSessionIs403() {
    PSRequestInfoBase.resetRequestInfo();
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.updateWorkflow(null, NAME, body(NAME, "Edited")));
    assertEquals(403, ex.getResponse().getStatus());
    verify(workflowService, never()).saveWorkflow(any());
  }

  // --- delete ---

  @Test
  void delete_delegatesToSteppedService() {
    adaptor.deleteWorkflow(null, NAME);
    verify(stepped).deleteWorkflow(NAME);
  }

  @Test
  void delete_trimsLeadingAndTrailingWhitespace() {
    adaptor.deleteWorkflow(null, "  Nightly QA  ");
    verify(stepped).deleteWorkflow("Nightly QA");
  }

  @Test
  void delete_systemWorkflowIs409() {
    doThrow(new PSWorkflowEditorServiceException("is a system workflow"))
        .when(stepped)
        .deleteWorkflow(NAME);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteWorkflow(null, NAME));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  void delete_itemsStillAssignedIs409() {
    doThrow(
            new PSWorkflowEditorServiceException(
                "Workflow " + NAME + " still have items assigned."))
        .when(stepped)
        .deleteWorkflow(NAME);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteWorkflow(null, NAME));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  void delete_missingWorkflowIs404() {
    doThrow(new PSWorkflowEditorServiceException("Can't find the workflow by given name 'Nightly QA'."))
        .when(stepped)
        .deleteWorkflow(NAME);

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteWorkflow(null, NAME));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  void delete_invalidIdOrNameIs400() {
    assertThrows(
        IllegalArgumentException.class, () -> adaptor.deleteWorkflow(null, "  "));
    assertThrows(IllegalArgumentException.class, () -> adaptor.deleteWorkflow(null, null));
    verify(stepped, never()).deleteWorkflow(any());
  }

  @Test
  void delete_nonAdminIs403() {
    WorkflowsAdaptor locked =
        new WorkflowsAdaptor(designWs, workflowService, contentMgr, () -> false, stepped);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> locked.deleteWorkflow(null, NAME));
    assertEquals(403, ex.getResponse().getStatus());
    verify(stepped, never()).deleteWorkflow(any());
  }

  @Test
  void delete_missingSessionIs403() {
    PSRequestInfoBase.resetRequestInfo();
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteWorkflow(null, NAME));
    assertEquals(403, ex.getResponse().getStatus());
    verify(stepped, never()).deleteWorkflow(any());
  }

  @Test
  void delete_unexpectedErrorIs500() {
    doThrow(new PSWorkflowEditorServiceException("Other failure"))
        .when(stepped)
        .deleteWorkflow(NAME);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.deleteWorkflow(null, NAME));
    assertEquals(500, ex.getResponse().getStatus());
  }

  @SuppressWarnings("unused")
  private static void assertNullDescription(WorkflowSummary summary) {
    assertNull(summary.getWorkflowDescription());
  }
}
