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

package com.percussion.rest.workflows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class WorkflowsResourceRoleInboxTest {

  private IWorkflowsAdaptor adaptor;
  private WorkflowsResource resource;

  @BeforeEach
  public void setUp() {
    adaptor = mock(IWorkflowsAdaptor.class);
    resource = new WorkflowsResource(adaptor);
    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost/services/"));
    resource.setUriInfo(uriInfo);
  }

  @Test
  public void putDelegatesInbox() {
    WorkflowStepRoleAssignmentList list = row("Draft", "Author", false);
    when(adaptor.setStepRoleInbox(any(), eq("Nightly QA"), eq("Draft"), any())).thenReturn(list);

    WorkflowStepRoleInboxWrite body = body("Author", false);
    WorkflowStepRoleAssignmentList out = resource.setStepRoleInbox("Nightly QA", "Draft", body);
    assertFalse(out.getAssignments().get(0).isInbox());
    assertTrue(out.getAssignments().get(0).isNotify());
    verify(adaptor).setStepRoleInbox(any(), eq("Nightly QA"), eq("Draft"), eq(body));
    verify(adaptor, never()).setStepRoleNotify(any(), any(), any(), any());
    verify(adaptor, never()).setStepRoleAssignment(any(), any(), any(), any());
  }

  @Test
  public void blankBodyAndMissingInboxDoNotCallAdaptor() {
    assertEquals(
        400,
        assertThrows(
                WebApplicationException.class,
                () -> resource.setStepRoleInbox("Nightly QA", "Draft", null))
            .getResponse()
            .getStatus());
    assertEquals(
        400,
        assertThrows(
                WebApplicationException.class,
                () -> resource.setStepRoleInbox("Nightly QA", "Draft", body(" ", false)))
            .getResponse()
            .getStatus());
    WorkflowStepRoleInboxWrite missing = new WorkflowStepRoleInboxWrite();
    missing.setRoleName("Author");
    assertEquals(
        400,
        assertThrows(
                WebApplicationException.class,
                () -> resource.setStepRoleInbox("Nightly QA", "Draft", missing))
            .getResponse()
            .getStatus());
    assertEquals(
        400,
        assertThrows(
                WebApplicationException.class,
                () -> resource.setStepRoleInbox("Nightly QA", " ", body("Author", false)))
            .getResponse()
            .getStatus());
    verify(adaptor, never()).setStepRoleInbox(any(), any(), any(), any());
  }

  @Test
  public void unchangedFromAdaptorIs400() {
    when(adaptor.setStepRoleInbox(any(), any(), any(), any()))
        .thenThrow(new IllegalArgumentException("inbox is unchanged"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.setStepRoleInbox("Nightly QA", "Draft", body("Author", true)));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void adaptor404Is404() {
    when(adaptor.setStepRoleInbox(any(), any(), any(), any()))
        .thenThrow(new WebApplicationException("Role is not assigned to this step", 404));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.setStepRoleInbox("Nightly QA", "Draft", body("Missing", false)));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void adaptor403Is403() {
    when(adaptor.setStepRoleInbox(any(), any(), any(), any()))
        .thenThrow(new WebApplicationException("protected", 403));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.setStepRoleInbox("Default Workflow", "Draft", body("Author", false)));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void adaptor409Is409() {
    when(adaptor.setStepRoleInbox(any(), any(), any(), any()))
        .thenThrow(new WebApplicationException("not Reader or Assignee", 409));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.setStepRoleInbox("Nightly QA", "Draft", body("Admin", false)));
    assertEquals(409, ex.getResponse().getStatus());
  }

  private static WorkflowStepRoleInboxWrite body(String role, boolean inbox) {
    WorkflowStepRoleInboxWrite body = new WorkflowStepRoleInboxWrite();
    body.setRoleName(role);
    body.setInbox(inbox);
    return body;
  }

  private static WorkflowStepRoleAssignmentList row(String step, String role, boolean inbox) {
    WorkflowStepRoleAssignment item = new WorkflowStepRoleAssignment();
    item.setStepName(step);
    item.setRoleName(role);
    item.setAssignmentType("ASSIGNEE");
    item.setNotify(true);
    item.setInbox(inbox);
    WorkflowStepRoleAssignmentList list = new WorkflowStepRoleAssignmentList();
    list.setAssignments(java.util.List.of(item));
    return list;
  }
}
