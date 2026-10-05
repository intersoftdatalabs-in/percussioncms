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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rest.JacksonContextResolver;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/** Slice 71 default-transition body: Jackson root wrap round-trip. */
@Tag("UnitTest")
public class WorkflowTransitionDefaultSerialDeserialTest {

  @Test
  public void writeBodyRoundTrip() {
    WorkflowTransitionDefault body = new WorkflowTransitionDefault();
    body.setDefaultTransition(true);

    ObjectMapper mapper = new JacksonContextResolver().getContext(WorkflowTransitionDefault.class);
    String json = mapper.writeValueAsString(body);
    assertTrue(json.contains("WorkflowTransitionDefault"), json);
    assertTrue(json.contains("defaultTransition"), json);
    assertTrue(json.contains("true"), json);
    assertFalse(json.contains("approvalsRequired"), json);
    assertFalse(json.contains("commentRequired"), json);

    WorkflowTransitionDefault roundTrip = mapper.readValue(json, WorkflowTransitionDefault.class);
    assertEquals(Boolean.TRUE, roundTrip.getDefaultTransition());
  }

  @Test
  public void falseRoundTripsWithoutClearingOtherFields() {
    WorkflowTransitionDefault body = new WorkflowTransitionDefault();
    body.setDefaultTransition(false);

    ObjectMapper mapper = new JacksonContextResolver().getContext(WorkflowTransitionDefault.class);
    String json = mapper.writeValueAsString(body);
    WorkflowTransitionDefault roundTrip = mapper.readValue(json, WorkflowTransitionDefault.class);
    assertEquals(Boolean.FALSE, roundTrip.getDefaultTransition());
  }

  @Test
  public void graphEdgeKeepsDefaultApprovalsAndComment() {
    WorkflowGraph graph = new WorkflowGraph();
    graph.setWorkflowName("Nightly QA");
    WorkflowGraph.Edge edge = new WorkflowGraph.Edge();
    edge.setFrom("Draft");
    edge.setTo("Review");
    edge.setLabel("Send");
    edge.setDefaultTransition(false);
    edge.setApprovalsRequired(2);
    edge.setCommentRequired(true);
    graph.setEdges(java.util.List.of(edge));

    ObjectMapper mapper = new JacksonContextResolver().getContext(WorkflowGraph.class);
    String json = mapper.writeValueAsString(graph);
    assertTrue(json.contains("defaultTransition"), json);
    assertTrue(json.contains("approvalsRequired"), json);
    assertTrue(json.contains("commentRequired"), json);

    WorkflowGraph roundTrip = mapper.readValue(json, WorkflowGraph.class);
    assertEquals(Boolean.FALSE, roundTrip.getEdges().get(0).getDefaultTransition());
    assertEquals(2, roundTrip.getEdges().get(0).getApprovalsRequired());
    assertTrue(roundTrip.getEdges().get(0).isCommentRequired());
    assertEquals("Send", roundTrip.getEdges().get(0).getLabel());
  }
}
