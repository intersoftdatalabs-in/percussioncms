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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rest.JacksonContextResolver;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/** Slice 72 allowed-role body: Jackson root wrap round-trip. */
@Tag("UnitTest")
public class WorkflowTransitionAllowedRoleSerialDeserialTest {

  @Test
  public void writeBodyRoundTrip() {
    WorkflowTransitionAllowedRole body = new WorkflowTransitionAllowedRole();
    body.setRoleName("Editor");

    ObjectMapper mapper =
        new JacksonContextResolver().getContext(WorkflowTransitionAllowedRole.class);
    String json = mapper.writeValueAsString(body);
    assertTrue(json.contains("WorkflowTransitionAllowedRole"), json);
    assertTrue(json.contains("roleName"), json);
    assertTrue(json.contains("Editor"), json);
    assertFalse(json.contains("defaultTransition"), json);
    assertFalse(json.contains("approvalsRequired"), json);
    assertFalse(json.contains("commentRequired"), json);

    WorkflowTransitionAllowedRole roundTrip =
        mapper.readValue(json, WorkflowTransitionAllowedRole.class);
    assertEquals("Editor", roundTrip.getRoleName());
  }

  @Test
  public void graphEdgeKeepsOneRoleAndOmitsAllowAllList() {
    WorkflowGraph graph = new WorkflowGraph();
    graph.setWorkflowName("Nightly QA");
    graph.setRoles(List.of("Editor", "Author"));
    WorkflowGraph.Edge restricted = new WorkflowGraph.Edge();
    restricted.setFrom("Draft");
    restricted.setTo("Live");
    restricted.setLabel("Send");
    restricted.setAllowAllRoles(false);
    restricted.setAllowedRoles(List.of("Editor"));
    restricted.setDefaultTransition(false);
    restricted.setApprovalsRequired(4);
    restricted.setCommentRequired(false);
    WorkflowGraph.Edge open = new WorkflowGraph.Edge();
    open.setFrom("Draft");
    open.setTo("Review");
    open.setLabel("Submit");
    open.setAllowAllRoles(true);
    open.setDefaultTransition(true);
    open.setApprovalsRequired(1);
    open.setCommentRequired(true);
    graph.setEdges(List.of(restricted, open));

    ObjectMapper mapper = new JacksonContextResolver().getContext(WorkflowGraph.class);
    String json = mapper.writeValueAsString(graph);
    assertTrue(json.contains("allowAllRoles"), json);
    assertTrue(json.contains("Editor"), json);
    assertFalse(json.contains("\"allowedRoles\":null"), json);

    WorkflowGraph roundTrip = mapper.readValue(json, WorkflowGraph.class);
    assertEquals(List.of("Editor", "Author"), roundTrip.getRoles());
    assertEquals(Boolean.FALSE, roundTrip.getEdges().get(0).getAllowAllRoles());
    assertEquals(List.of("Editor"), roundTrip.getEdges().get(0).getAllowedRoles());
    assertEquals(Boolean.FALSE, roundTrip.getEdges().get(0).getDefaultTransition());
    assertEquals(4, roundTrip.getEdges().get(0).getApprovalsRequired());
    assertEquals(Boolean.TRUE, roundTrip.getEdges().get(1).getAllowAllRoles());
    assertNull(roundTrip.getEdges().get(1).getAllowedRoles());
    assertTrue(roundTrip.getEdges().get(1).isCommentRequired());
  }

  @Test
  public void graphEdgeKeepsBothAllowedRolesAndStaysRestricted() {
    WorkflowGraph graph = new WorkflowGraph();
    graph.setWorkflowName("Nightly QA");
    WorkflowGraph.Edge restricted = new WorkflowGraph.Edge();
    restricted.setFrom("Draft");
    restricted.setTo("Live");
    restricted.setLabel("Send");
    restricted.setAllowAllRoles(false);
    restricted.setAllowedRoles(List.of("Editor", "Author"));
    graph.setEdges(List.of(restricted));

    ObjectMapper mapper = new JacksonContextResolver().getContext(WorkflowGraph.class);
    String json = mapper.writeValueAsString(graph);
    assertTrue(json.contains("Editor"), json);
    assertTrue(json.contains("Author"), json);
    assertFalse(json.contains("\"allowAllRoles\":true"), json);

    WorkflowGraph roundTrip = mapper.readValue(json, WorkflowGraph.class);
    assertEquals(Boolean.FALSE, roundTrip.getEdges().get(0).getAllowAllRoles());
    assertEquals(List.of("Editor", "Author"), roundTrip.getEdges().get(0).getAllowedRoles());
  }
}
