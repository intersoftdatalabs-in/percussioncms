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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
public class WorkflowsResourceRoleAddTest {

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
  public void postDelegatesReaderOrAssignee() {
    WorkflowStepRoleAssignmentList list = row("Draft", "System", "READER");
    when(adaptor.addStepRole(any(), eq("Nightly QA"), eq("Draft"), any())).thenReturn(list);

    WorkflowStepRoleAdd body = body("System", "reader");
    WorkflowStepRoleAssignmentList out = resource.addStepRole("Nightly QA", "Draft", body);
    assertEquals("READER", out.getAssignments().get(0).getAssignmentType());
    verify(adaptor).addStepRole(any(), eq("Nightly QA"), eq("Draft"), eq(body));
  }

  @Test
  public void blankBodyAndInvalidTypeDoNotCallAdaptor() {
    assertEquals(
        400,
        assertThrows(
                WebApplicationException.class,
                () -> resource.addStepRole("Nightly QA", "Draft", null))
            .getResponse()
            .getStatus());
    assertEquals(
        400,
        assertThrows(
                WebApplicationException.class,
                () -> resource.addStepRole("Nightly QA", "Draft", body(" ", "READER")))
            .getResponse()
            .getStatus());
    assertEquals(
        400,
        assertThrows(
                WebApplicationException.class,
                () -> resource.addStepRole("Nightly QA", "Draft", body("System", "ADMIN")))
            .getResponse()
            .getStatus());
    assertEquals(
        400,
        assertThrows(
                WebApplicationException.class,
                () -> resource.addStepRole("Nightly QA", " ", body("System", "READER")))
            .getResponse()
            .getStatus());
    verify(adaptor, never()).addStepRole(any(), any(), any(), any());
  }

  @Test
  public void adaptor409Is409() {
    when(adaptor.addStepRole(any(), any(), any(), any()))
        .thenThrow(new WebApplicationException("Role is already assigned to this step", 409));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.addStepRole("Nightly QA", "Draft", body("Author", "READER")));
    assertEquals(409, ex.getResponse().getStatus());
  }

  private static WorkflowStepRoleAdd body(String role, String type) {
    WorkflowStepRoleAdd body = new WorkflowStepRoleAdd();
    body.setRoleName(role);
    body.setAssignmentType(type);
    return body;
  }

  private static WorkflowStepRoleAssignmentList row(String step, String role, String type) {
    WorkflowStepRoleAssignment item = new WorkflowStepRoleAssignment();
    item.setStepName(step);
    item.setRoleName(role);
    item.setAssignmentType(type);
    WorkflowStepRoleAssignmentList list = new WorkflowStepRoleAssignmentList();
    list.setAssignments(java.util.List.of(item));
    return list;
  }
}
