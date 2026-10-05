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

/** Slice 70 approvals body: Jackson root wrap round-trip. */
@Tag("UnitTest")
public class WorkflowTransitionApprovalsSerialDeserialTest {

  @Test
  public void writeBodyRoundTrip() {
    WorkflowTransitionApprovals body = new WorkflowTransitionApprovals();
    body.setApprovalsRequired(0);

    ObjectMapper mapper =
        new JacksonContextResolver().getContext(WorkflowTransitionApprovals.class);
    String json = mapper.writeValueAsString(body);
    assertTrue(json.contains("WorkflowTransitionApprovals"), json);
    assertTrue(json.contains("approvalsRequired"), json);
    assertTrue(json.contains("0"), json);
    assertFalse(json.contains("commentRequired"), json);
    assertFalse(json.contains("label"), json);

    WorkflowTransitionApprovals roundTrip =
        mapper.readValue(json, WorkflowTransitionApprovals.class);
    assertEquals(0, roundTrip.getApprovalsRequired());
  }

  @Test
  public void graphEdgeKeepsApprovalsAndComment() {
    WorkflowGraph graph = new WorkflowGraph();
    graph.setWorkflowName("Nightly QA");
    WorkflowGraph.Edge edge = new WorkflowGraph.Edge();
    edge.setFrom("Draft");
    edge.setTo("Review");
    edge.setLabel("Submit");
    edge.setApprovalsRequired(2);
    edge.setCommentRequired(true);
    graph.setEdges(java.util.List.of(edge));

    ObjectMapper mapper = new JacksonContextResolver().getContext(WorkflowGraph.class);
    String json = mapper.writeValueAsString(graph);
    assertTrue(json.contains("approvalsRequired"), json);
    assertTrue(json.contains("commentRequired"), json);

    WorkflowGraph roundTrip = mapper.readValue(json, WorkflowGraph.class);
    assertEquals(2, roundTrip.getEdges().get(0).getApprovalsRequired());
    assertTrue(roundTrip.getEdges().get(0).isCommentRequired());
    assertEquals("Submit", roundTrip.getEdges().get(0).getLabel());
  }
}
