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
public class WorkflowsResourceRoleAdhocTest {

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
  public void putDelegatesAdhoc() {
    WorkflowStepRoleAssignmentList list = row("Draft", "Author", "enabled");
    when(adaptor.setStepRoleAdhoc(any(), eq("Nightly QA"), eq("Draft"), any())).thenReturn(list);

    WorkflowStepRoleAdhocWrite body = body("Author", "enabled");
    WorkflowStepRoleAssignmentList out = resource.setStepRoleAdhoc("Nightly QA", "Draft", body);
    assertEquals("enabled", out.getAssignments().get(0).getAdhocType());
    assertTrue(out.getAssignments().get(0).isNotify());
    assertTrue(out.getAssignments().get(0).isInbox());
    verify(adaptor).setStepRoleAdhoc(any(), eq("Nightly QA"), eq("Draft"), eq(body));
    verify(adaptor, never()).setStepRoleInbox(any(), any(), any(), any());
    verify(adaptor, never()).setStepRoleNotify(any(), any(), any(), any());
    verify(adaptor, never()).setStepRoleAssignment(any(), any(), any(), any());
  }

  @Test
  public void blankBodyAndInvalidTypeDoNotCallAdaptor() {
    assertEquals(
        400,
        assertThrows(
                WebApplicationException.class,
                () -> resource.setStepRoleAdhoc("Nightly QA", "Draft", null))
            .getResponse()
            .getStatus());
    assertEquals(
        400,
        assertThrows(
                WebApplicationException.class,
                () -> resource.setStepRoleAdhoc("Nightly QA", "Draft", body(" ", "enabled")))
            .getResponse()
            .getStatus());
    WorkflowStepRoleAdhocWrite missing = new WorkflowStepRoleAdhocWrite();
    missing.setRoleName("Author");
    assertEquals(
        400,
        assertThrows(
                WebApplicationException.class,
                () -> resource.setStepRoleAdhoc("Nightly QA", "Draft", missing))
            .getResponse()
            .getStatus());
    assertEquals(
        400,
        assertThrows(
                WebApplicationException.class,
                () -> resource.setStepRoleAdhoc("Nightly QA", "Draft", body("Author", "maybe")))
            .getResponse()
            .getStatus());
    assertEquals(
        400,
        assertThrows(
                WebApplicationException.class,
                () -> resource.setStepRoleAdhoc("Nightly QA", " ", body("Author", "enabled")))
            .getResponse()
            .getStatus());
    verify(adaptor, never()).setStepRoleAdhoc(any(), any(), any(), any());
  }

  @Test
  public void unchangedFromAdaptorIs400() {
    when(adaptor.setStepRoleAdhoc(any(), any(), any(), any()))
        .thenThrow(new IllegalArgumentException("adhoc type is unchanged"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.setStepRoleAdhoc("Nightly QA", "Draft", body("Author", "disabled")));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void adaptor404Is404() {
    when(adaptor.setStepRoleAdhoc(any(), any(), any(), any()))
        .thenThrow(new WebApplicationException("Role is not assigned to this step", 404));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.setStepRoleAdhoc("Nightly QA", "Draft", body("Missing", "enabled")));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void adaptor403Is403() {
    when(adaptor.setStepRoleAdhoc(any(), any(), any(), any()))
        .thenThrow(new WebApplicationException("protected", 403));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                resource.setStepRoleAdhoc("Default Workflow", "Draft", body("Author", "enabled")));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void adaptor409Is409() {
    when(adaptor.setStepRoleAdhoc(any(), any(), any(), any()))
        .thenThrow(new WebApplicationException("not Reader or Assignee", 409));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.setStepRoleAdhoc("Nightly QA", "Draft", body("Admin", "enabled")));
    assertEquals(409, ex.getResponse().getStatus());
  }

  private static WorkflowStepRoleAdhocWrite body(String role, String adhocType) {
    WorkflowStepRoleAdhocWrite body = new WorkflowStepRoleAdhocWrite();
    body.setRoleName(role);
    body.setAdhocType(adhocType);
    return body;
  }

  private static WorkflowStepRoleAssignmentList row(String step, String role, String adhocType) {
    WorkflowStepRoleAssignment item = new WorkflowStepRoleAssignment();
    item.setStepName(step);
    item.setRoleName(role);
    item.setAssignmentType("ASSIGNEE");
    item.setNotify(true);
    item.setInbox(true);
    item.setAdhocType(adhocType);
    WorkflowStepRoleAssignmentList list = new WorkflowStepRoleAssignmentList();
    list.setAssignments(java.util.List.of(item));
    return list;
  }
}
