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
import static org.mockito.ArgumentMatchers.anyInt;
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
public class WorkflowsResourceTransitionApprovalsTest {

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
  public void putDelegatesApprovals() {
    WorkflowGraph graph = graph(2);
    when(adaptor.updateTransitionApprovalsRequired(
            any(), eq("Nightly QA"), eq("Draft"), eq("Submit"), eq("Review"), eq(2)))
        .thenReturn(graph);

    WorkflowGraph out =
        resource.updateTransitionApprovalsRequired(
            "Nightly QA", "Draft", "Submit", "Review", body(2));
    assertEquals(2, out.getEdges().get(0).getApprovalsRequired());
    verify(adaptor)
        .updateTransitionApprovalsRequired(
            any(), eq("Nightly QA"), eq("Draft"), eq("Submit"), eq("Review"), eq(2));
    verify(adaptor, never())
        .updateTransitionCommentRequired(any(), any(), any(), any(), any(), eq(true));
  }

  @Test
  public void blankBodyAndNegativeDoNotCallAdaptor() {
    assertEquals(
        400,
        assertThrows(
                WebApplicationException.class,
                () ->
                    resource.updateTransitionApprovalsRequired(
                        "Nightly QA", "Draft", "Submit", "Review", null))
            .getResponse()
            .getStatus());
    assertEquals(
        400,
        assertThrows(
                WebApplicationException.class,
                () ->
                    resource.updateTransitionApprovalsRequired(
                        "Nightly QA", "Draft", "Submit", "Review", new WorkflowTransitionApprovals()))
            .getResponse()
            .getStatus());
    assertEquals(
        400,
        assertThrows(
                WebApplicationException.class,
                () ->
                    resource.updateTransitionApprovalsRequired(
                        "Nightly QA", "Draft", "Submit", "Review", body(-1)))
            .getResponse()
            .getStatus());
    assertEquals(
        400,
        assertThrows(
                WebApplicationException.class,
                () ->
                    resource.updateTransitionApprovalsRequired(
                        "Nightly QA", " ", "Submit", "Review", body(2)))
            .getResponse()
            .getStatus());
    verify(adaptor, never())
        .updateTransitionApprovalsRequired(any(), any(), any(), any(), any(), anyInt());
  }

  @Test
  public void unchangedFromAdaptorIs400() {
    when(adaptor.updateTransitionApprovalsRequired(any(), any(), any(), any(), any(), eq(1)))
        .thenThrow(new IllegalArgumentException("approvals required is unchanged"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                resource.updateTransitionApprovalsRequired(
                    "Nightly QA", "Draft", "Submit", "Review", body(1)));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void adaptor404Is404() {
    when(adaptor.updateTransitionApprovalsRequired(any(), any(), any(), any(), any(), eq(2)))
        .thenThrow(new WebApplicationException("missing", 404));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                resource.updateTransitionApprovalsRequired(
                    "Nightly QA", "Draft", "Missing", null, body(2)));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void adaptor403Is403() {
    when(adaptor.updateTransitionApprovalsRequired(any(), any(), any(), any(), any(), eq(2)))
        .thenThrow(new WebApplicationException("protected", 403));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                resource.updateTransitionApprovalsRequired(
                    "Simple Workflow", "Draft", "Submit", "Review", body(2)));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void adaptor409Is409() {
    when(adaptor.updateTransitionApprovalsRequired(any(), any(), any(), any(), any(), eq(2)))
        .thenThrow(new WebApplicationException("each-role", 409));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                resource.updateTransitionApprovalsRequired(
                    "Nightly QA", "Draft", "Submit", "Review", body(2)));
    assertEquals(409, ex.getResponse().getStatus());
  }

  private static WorkflowTransitionApprovals body(int count) {
    WorkflowTransitionApprovals body = new WorkflowTransitionApprovals();
    body.setApprovalsRequired(count);
    return body;
  }

  private static WorkflowGraph graph(int count) {
    WorkflowGraph graph = new WorkflowGraph();
    graph.setWorkflowName("Nightly QA");
    WorkflowGraph.Edge edge = new WorkflowGraph.Edge();
    edge.setFrom("Draft");
    edge.setTo("Review");
    edge.setLabel("Submit");
    edge.setApprovalsRequired(count);
    edge.setCommentRequired(false);
    graph.setEdges(java.util.List.of(edge));
    return graph;
  }
}
