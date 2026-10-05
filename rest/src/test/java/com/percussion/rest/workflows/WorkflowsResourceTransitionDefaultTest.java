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
import static org.mockito.ArgumentMatchers.anyBoolean;
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
public class WorkflowsResourceTransitionDefaultTest {

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
  public void putDelegatesDefaultMark() {
    WorkflowGraph graph = graph(true);
    when(adaptor.updateTransitionDefault(
            any(), eq("Nightly QA"), eq("Draft"), eq("Send"), eq("Review"), eq(true)))
        .thenReturn(graph);

    WorkflowGraph out =
        resource.updateTransitionDefault("Nightly QA", "Draft", "Send", "Review", body(true));
    assertEquals(Boolean.TRUE, out.getEdges().get(0).getDefaultTransition());
    verify(adaptor)
        .updateTransitionDefault(
            any(), eq("Nightly QA"), eq("Draft"), eq("Send"), eq("Review"), eq(true));
    verify(adaptor, never())
        .updateTransitionApprovalsRequired(any(), any(), any(), any(), any(), eq(1));
  }

  @Test
  public void blankBodyAndBlankFromDoNotCallAdaptor() {
    assertEquals(
        400,
        assertThrows(
                WebApplicationException.class,
                () ->
                    resource.updateTransitionDefault(
                        "Nightly QA", "Draft", "Send", "Review", null))
            .getResponse()
            .getStatus());
    assertEquals(
        400,
        assertThrows(
                WebApplicationException.class,
                () ->
                    resource.updateTransitionDefault(
                        "Nightly QA", "Draft", "Send", "Review", new WorkflowTransitionDefault()))
            .getResponse()
            .getStatus());
    assertEquals(
        400,
        assertThrows(
                WebApplicationException.class,
                () -> resource.updateTransitionDefault("Nightly QA", " ", "Send", "Review", body(true)))
            .getResponse()
            .getStatus());
    verify(adaptor, never()).updateTransitionDefault(any(), any(), any(), any(), any(), anyBoolean());
  }

  @Test
  public void alreadyDefaultFromAdaptorIs400() {
    when(adaptor.updateTransitionDefault(any(), any(), any(), any(), any(), eq(true)))
        .thenThrow(new IllegalArgumentException("transition is already the default"));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                resource.updateTransitionDefault(
                    "Nightly QA", "Draft", "Submit", "Review", body(true)));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void adaptor404Is404() {
    when(adaptor.updateTransitionDefault(any(), any(), any(), any(), any(), eq(true)))
        .thenThrow(new WebApplicationException("missing", 404));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                resource.updateTransitionDefault("Nightly QA", "Draft", "Missing", null, body(true)));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void adaptor403Is403() {
    when(adaptor.updateTransitionDefault(any(), any(), any(), any(), any(), eq(true)))
        .thenThrow(new WebApplicationException("protected", 403));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                resource.updateTransitionDefault(
                    "Simple Workflow", "Draft", "Send", "Review", body(true)));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  public void adaptor409Is409() {
    when(adaptor.updateTransitionDefault(any(), any(), any(), any(), any(), eq(false)))
        .thenThrow(new WebApplicationException("clear", 409));
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                resource.updateTransitionDefault(
                    "Nightly QA", "Draft", "Submit", "Review", body(false)));
    assertEquals(409, ex.getResponse().getStatus());
  }

  private static WorkflowTransitionDefault body(boolean mark) {
    WorkflowTransitionDefault body = new WorkflowTransitionDefault();
    body.setDefaultTransition(mark);
    return body;
  }

  private static WorkflowGraph graph(boolean mark) {
    WorkflowGraph graph = new WorkflowGraph();
    graph.setWorkflowName("Nightly QA");
    WorkflowGraph.Edge edge = new WorkflowGraph.Edge();
    edge.setFrom("Draft");
    edge.setTo("Review");
    edge.setLabel("Send");
    edge.setDefaultTransition(mark);
    edge.setApprovalsRequired(1);
    edge.setCommentRequired(false);
    graph.setEdges(java.util.List.of(edge));
    return graph;
  }
}
