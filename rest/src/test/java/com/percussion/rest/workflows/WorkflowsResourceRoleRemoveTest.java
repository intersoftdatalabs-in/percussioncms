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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class WorkflowsResourceRoleRemoveTest {

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
  public void deleteDelegatesOneRole() {
    WorkflowStepRoleAssignmentList list = row("Draft", "Editor", "READER");
    when(adaptor.removeStepRole(any(), eq("Nightly QA"), eq("Draft"), eq("Author")))
        .thenReturn(list);

    WorkflowStepRoleAssignmentList out = resource.removeStepRole("Nightly QA", "Draft", "Author");
    assertEquals("Editor", out.getAssignments().get(0).getRoleName());
    verify(adaptor).removeStepRole(any(), eq("Nightly QA"), eq("Draft"), eq("Author"));
  }

  @Test
  public void blankNamesDoNotCallAdaptor() {
    assertEquals(
        400,
        assertThrows(
                WebApplicationException.class,
                () -> resource.removeStepRole("Nightly QA", "Draft", " "))
            .getResponse()
            .getStatus());
    assertEquals(
        400,
        assertThrows(
                WebApplicationException.class,
                () -> resource.removeStepRole("Nightly QA", " ", "Author"))
            .getResponse()
            .getStatus());
    verify(adaptor, never()).removeStepRole(any(), any(), any(), any());
  }

  @Test
  public void adaptorStatusesPassThrough() {
    when(adaptor.removeStepRole(any(), any(), any(), eq("Admin")))
        .thenThrow(new WebApplicationException("protected assignment", 409));
    assertEquals(
        409,
        assertThrows(
                WebApplicationException.class,
                () -> resource.removeStepRole("Nightly QA", "Review", "Admin"))
            .getResponse()
            .getStatus());

    when(adaptor.removeStepRole(any(), any(), any(), eq("Missing")))
        .thenThrow(new WebApplicationException("missing", 404));
    assertEquals(
        404,
        assertThrows(
                WebApplicationException.class,
                () -> resource.removeStepRole("Nightly QA", "Draft", "Missing"))
            .getResponse()
            .getStatus());

    when(adaptor.removeStepRole(any(), eq("Default Workflow"), any(), any()))
        .thenThrow(new WebApplicationException("packaged", 403));
    assertEquals(
        403,
        assertThrows(
                WebApplicationException.class,
                () -> resource.removeStepRole("Default Workflow", "Draft", "Author"))
            .getResponse()
            .getStatus());
  }

  private static WorkflowStepRoleAssignmentList row(String step, String role, String type) {
    WorkflowStepRoleAssignment item = new WorkflowStepRoleAssignment();
    item.setStepName(step);
    item.setRoleName(role);
    item.setAssignmentType(type);
    WorkflowStepRoleAssignmentList list = new WorkflowStepRoleAssignmentList();
    list.setAssignments(List.of(item));
    return list;
  }
}
