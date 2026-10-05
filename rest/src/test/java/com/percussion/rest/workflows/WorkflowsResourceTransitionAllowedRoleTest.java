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
public class WorkflowsResourceTransitionAllowedRoleTest {

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
  public void putDelegatesOneRole() {
    WorkflowGraph graph = graph();
    when(adaptor.restrictTransitionToOneRole(
            any(), eq("Nightly QA"), eq("Draft"), eq("Send"), eq("Live"), eq("Editor")))
        .thenReturn(graph);

    WorkflowGraph out =
        resource.restrictTransitionToOneRole(
            "Nightly QA", "Draft", "Send", "Live", body("Editor"));
    assertEquals(Boolean.FALSE, out.getEdges().get(0).getAllowAllRoles());
    assertEquals(List.of("Editor"), out.getEdges().get(0).getAllowedRoles());
    verify(adaptor)
        .restrictTransitionToOneRole(
            any(), eq("Nightly QA"), eq("Draft"), eq("Send"), eq("Live"), eq("Editor"));
    verify(adaptor, never())
        .updateTransitionDefault(any(), any(), any(), any(), any(), eq(true));
  }

  @Test
  public void blankBodyAndBlankFromDoNotCallAdaptor() {
    assertEquals(
        400,
        assertThrows(
                WebApplicationException.class,
                () ->
                    resource.restrictTransitionToOneRole(
                        "Nightly QA", "Draft", "Send", "Live", null))
            .getResponse()
            .getStatus());
    assertEquals(
        400,
        assertThrows(
                WebApplicationException.class,
                () ->
                    resource.restrictTransitionToOneRole(
                        "Nightly QA", "Draft", "Send", "Live", new WorkflowTransitionAllowedRole()))
            .getResponse()
            .getStatus());
    assertEquals(
        400,
        assertThrows(
                WebApplicationException.class,
                () ->
                    resource.restrictTransitionToOneRole(
                        "Nightly QA", "Draft", "Send", "Live", body(" ")))
            .getResponse()
            .getStatus());
    assertEquals(
        400,
        assertThrows(
                WebApplicationException.class,
                () ->
                    resource.restrictTransitionToOneRole(
                        "Nightly QA", " ", "Send", "Live", body("Editor")))
            .getResponse()
            .getStatus());
    verify(adaptor, never())
        .restrictTransitionToOneRole(any(), any(), any(), any(), any(), any());
  }

  @Test
  public void agingFromAdaptorIs400() {
    when(adaptor.restrictTransitionToOneRole(any(), any(), any(), any(), any(), eq("Editor")))
        .thenThrow(new IllegalArgumentException("not aging transitions"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                resource.restrictTransitionToOneRole(
                    "Nightly QA", "Live", "Expire", "Archive", body("Editor")));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void adaptor404Is404() {
    when(adaptor.restrictTransitionToOneRole(any(), any(), any(), any(), any(), eq("Missing")))
        .thenThrow(new WebApplicationException("missing", 404));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                resource.restrictTransitionToOneRole(
                    "Nightly QA", "Draft", "Send", "Live", body("Missing")));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void adaptor403Is403() {
    when(adaptor.restrictTransitionToOneRole(any(), any(), any(), any(), any(), eq("Editor")))
        .thenThrow(new WebApplicationException("protected", 403));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                resource.restrictTransitionToOneRole(
                    "Simple Workflow", "Draft", "Send", "Live", body("Editor")));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void adaptor409Is409() {
    when(adaptor.restrictTransitionToOneRole(any(), any(), any(), any(), any(), eq("*ALL*")))
        .thenThrow(new WebApplicationException("allow-all", 409));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                resource.restrictTransitionToOneRole(
                    "Nightly QA", "Draft", "Send", "Live", body("*ALL*")));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void postAddsOneRoleAndKeepsAllowAllFalse() {
    WorkflowGraph graph = graph();
    graph.getEdges().get(0).setAllowedRoles(List.of("Editor", "Author"));
    when(adaptor.addTransitionAllowedRole(
            any(), eq("Nightly QA"), eq("Draft"), eq("Send"), eq("Live"), eq("Author")))
        .thenReturn(graph);

    WorkflowGraph out =
        resource.addTransitionAllowedRole("Nightly QA", "Draft", "Send", "Live", body("Author"));
    assertEquals(Boolean.FALSE, out.getEdges().get(0).getAllowAllRoles());
    assertEquals(List.of("Editor", "Author"), out.getEdges().get(0).getAllowedRoles());
    verify(adaptor)
        .addTransitionAllowedRole(
            any(), eq("Nightly QA"), eq("Draft"), eq("Send"), eq("Live"), eq("Author"));
    verify(adaptor, never())
        .restrictTransitionToOneRole(any(), any(), any(), any(), any(), any());
  }

  @Test
  public void addBlankBodyAndBlankFromDoNotCallAdaptor() {
    assertEquals(
        400,
        assertThrows(
                WebApplicationException.class,
                () ->
                    resource.addTransitionAllowedRole(
                        "Nightly QA", "Draft", "Send", "Live", null))
            .getResponse()
            .getStatus());
    assertEquals(
        400,
        assertThrows(
                WebApplicationException.class,
                () ->
                    resource.addTransitionAllowedRole(
                        "Nightly QA", "Draft", "Send", "Live", body(" ")))
            .getResponse()
            .getStatus());
    assertEquals(
        400,
        assertThrows(
                WebApplicationException.class,
                () ->
                    resource.addTransitionAllowedRole(
                        "Nightly QA", " ", "Send", "Live", body("Author")))
            .getResponse()
            .getStatus());
    verify(adaptor, never()).addTransitionAllowedRole(any(), any(), any(), any(), any(), any());
  }

  @Test
  public void addAgingFromAdaptorIs400() {
    when(adaptor.addTransitionAllowedRole(any(), any(), any(), any(), any(), eq("Author")))
        .thenThrow(new IllegalArgumentException("not aging transitions"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                resource.addTransitionAllowedRole(
                    "Nightly QA", "Live", "Expire", "Archive", body("Author")));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void addAdaptor404Is404() {
    when(adaptor.addTransitionAllowedRole(any(), any(), any(), any(), any(), eq("Missing")))
        .thenThrow(new WebApplicationException("missing", 404));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                resource.addTransitionAllowedRole(
                    "Nightly QA", "Draft", "Send", "Live", body("Missing")));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void addAdaptor403Is403() {
    when(adaptor.addTransitionAllowedRole(any(), any(), any(), any(), any(), eq("Author")))
        .thenThrow(new WebApplicationException("protected", 403));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                resource.addTransitionAllowedRole(
                    "Simple Workflow", "Draft", "Send", "Live", body("Author")));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void addAdaptor409Is409() {
    when(adaptor.addTransitionAllowedRole(any(), any(), any(), any(), any(), eq("Editor")))
        .thenThrow(new WebApplicationException("already listed", 409));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                resource.addTransitionAllowedRole(
                    "Nightly QA", "Draft", "Send", "Live", body("Editor")));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void deleteClearsTheRoleListAndReturnsAllowAll() {
    WorkflowGraph graph = graph();
    graph.getEdges().get(0).setAllowAllRoles(true);
    graph.getEdges().get(0).setAllowedRoles(null);
    when(adaptor.clearTransitionAllowedRoles(
            any(), eq("Nightly QA"), eq("Draft"), eq("Send"), eq("Live")))
        .thenReturn(graph);

    WorkflowGraph out =
        resource.clearTransitionAllowedRoles("Nightly QA", "Draft", "Send", "Live");
    assertEquals(Boolean.TRUE, out.getEdges().get(0).getAllowAllRoles());
    assertEquals(null, out.getEdges().get(0).getAllowedRoles());
    verify(adaptor)
        .clearTransitionAllowedRoles(any(), eq("Nightly QA"), eq("Draft"), eq("Send"), eq("Live"));
    verify(adaptor, never())
        .addTransitionAllowedRole(any(), any(), any(), any(), any(), any());
    verify(adaptor, never())
        .restrictTransitionToOneRole(any(), any(), any(), any(), any(), any());
  }

  @Test
  public void clearBlankFromDoesNotCallAdaptor() {
    assertEquals(
        400,
        assertThrows(
                WebApplicationException.class,
                () -> resource.clearTransitionAllowedRoles("Nightly QA", " ", "Send", "Live"))
            .getResponse()
            .getStatus());
    assertEquals(
        400,
        assertThrows(
                WebApplicationException.class,
                () -> resource.clearTransitionAllowedRoles("Nightly QA", "Draft", " ", "Live"))
            .getResponse()
            .getStatus());
    verify(adaptor, never()).clearTransitionAllowedRoles(any(), any(), any(), any(), any());
  }

  @Test
  public void clearAgingFromAdaptorIs400() {
    when(adaptor.clearTransitionAllowedRoles(any(), any(), any(), any(), any()))
        .thenThrow(new IllegalArgumentException("not aging transitions"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.clearTransitionAllowedRoles("Nightly QA", "Live", "Expire", "Archive"));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void clearAdaptor403Is403() {
    when(adaptor.clearTransitionAllowedRoles(any(), any(), any(), any(), any()))
        .thenThrow(new WebApplicationException("protected", 403));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.clearTransitionAllowedRoles("Simple Workflow", "Draft", "Send", "Live"));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void clearAdaptor409Is409() {
    when(adaptor.clearTransitionAllowedRoles(any(), any(), any(), any(), any()))
        .thenThrow(new WebApplicationException("already allow-all", 409));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> resource.clearTransitionAllowedRoles("Nightly QA", "Draft", "Send", "Live"));
    assertEquals(409, ex.getResponse().getStatus());
  }

  private static WorkflowTransitionAllowedRole body(String roleName) {
    WorkflowTransitionAllowedRole body = new WorkflowTransitionAllowedRole();
    body.setRoleName(roleName);
    return body;
  }

  private static WorkflowGraph graph() {
    WorkflowGraph graph = new WorkflowGraph();
    graph.setWorkflowName("Nightly QA");
    graph.setRoles(List.of("Editor", "Author"));
    WorkflowGraph.Edge edge = new WorkflowGraph.Edge();
    edge.setFrom("Draft");
    edge.setTo("Live");
    edge.setLabel("Send");
    edge.setAllowAllRoles(false);
    edge.setAllowedRoles(List.of("Editor"));
    edge.setDefaultTransition(false);
    edge.setApprovalsRequired(4);
    graph.setEdges(List.of(edge));
    return graph;
  }
}
