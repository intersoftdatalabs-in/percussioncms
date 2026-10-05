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
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/** Slice 69 adhoc body and list type: Jackson root wrap round-trip. */
@Tag("UnitTest")
public class WorkflowStepRoleAdhocWriteSerialDeserialTest {

  @Test
  public void writeBodyRoundTrip() {
    WorkflowStepRoleAdhocWrite body = new WorkflowStepRoleAdhocWrite();
    body.setRoleName("Author");
    body.setAdhocType("anonymous");

    ObjectMapper mapper =
        new JacksonContextResolver().getContext(WorkflowStepRoleAdhocWrite.class);
    String json = mapper.writeValueAsString(body);
    assertTrue(json.contains("WorkflowStepRoleAdhocWrite"), json);
    assertTrue(json.contains("Author"), json);
    assertTrue(json.contains("anonymous"), json);
    assertTrue(json.contains("adhocType"), json);
    assertFalse(json.toLowerCase().contains("notify"), json);
    assertFalse(json.contains("inbox"), json);
    assertFalse(json.contains("assignmentType"), json);

    WorkflowStepRoleAdhocWrite roundTrip =
        mapper.readValue(json, WorkflowStepRoleAdhocWrite.class);
    assertEquals("Author", roundTrip.getRoleName());
    assertEquals("anonymous", roundTrip.getAdhocType());
  }

  @Test
  public void listRoundTripKeepsAdhoc() {
    WorkflowStepRoleAssignment row = new WorkflowStepRoleAssignment();
    row.setStepName("Draft");
    row.setRoleName("Author");
    row.setAssignmentType("ASSIGNEE");
    row.setNotify(true);
    row.setInbox(true);
    row.setAdhocType("enabled");
    WorkflowStepRoleAssignmentList list = new WorkflowStepRoleAssignmentList();
    list.setAssignments(List.of(row));

    ObjectMapper mapper =
        new JacksonContextResolver().getContext(WorkflowStepRoleAssignmentList.class);
    String json = mapper.writeValueAsString(list);
    assertTrue(json.contains("adhocType"), json);
    assertTrue(json.contains("enabled"), json);
    assertTrue(json.toLowerCase().contains("inbox"), json);
    assertTrue(json.toLowerCase().contains("notify"), json);

    WorkflowStepRoleAssignmentList roundTrip =
        mapper.readValue(json, WorkflowStepRoleAssignmentList.class);
    assertEquals("enabled", roundTrip.getAssignments().get(0).getAdhocType());
    assertTrue(roundTrip.getAssignments().get(0).isInbox());
    assertTrue(roundTrip.getAssignments().get(0).isNotify());
    assertEquals("ASSIGNEE", roundTrip.getAssignments().get(0).getAssignmentType());
  }
}
