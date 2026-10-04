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

/** Slice 65 notify body and list flag: Jackson root wrap round-trip. Inbox is not on the wire. */
@Tag("UnitTest")
public class WorkflowStepRoleNotifyWriteSerialDeserialTest {

  @Test
  public void writeBodyRoundTrip() {
    WorkflowStepRoleNotifyWrite body = new WorkflowStepRoleNotifyWrite();
    body.setRoleName("Author");
    body.setNotify(Boolean.FALSE);

    ObjectMapper mapper =
        new JacksonContextResolver().getContext(WorkflowStepRoleNotifyWrite.class);
    String json = mapper.writeValueAsString(body);
    assertTrue(json.contains("WorkflowStepRoleNotifyWrite"), json);
    assertTrue(json.contains("Author"), json);
    assertTrue(json.contains("false") || json.contains("False"), json);
    assertFalse(json.toLowerCase().contains("inbox"), json);
    assertFalse(json.contains("assignmentType"), json);

    WorkflowStepRoleNotifyWrite roundTrip =
        mapper.readValue(json, WorkflowStepRoleNotifyWrite.class);
    assertEquals("Author", roundTrip.getRoleName());
    assertEquals(Boolean.FALSE, roundTrip.getNotify());
  }

  @Test
  public void listRoundTripKeepsNotify() {
    WorkflowStepRoleAssignment row = new WorkflowStepRoleAssignment();
    row.setStepName("Draft");
    row.setRoleName("Author");
    row.setAssignmentType("ASSIGNEE");
    row.setNotify(true);
    WorkflowStepRoleAssignmentList list = new WorkflowStepRoleAssignmentList();
    list.setAssignments(List.of(row));

    ObjectMapper mapper =
        new JacksonContextResolver().getContext(WorkflowStepRoleAssignmentList.class);
    String json = mapper.writeValueAsString(list);
    assertTrue(json.contains("notify"), json.toLowerCase());
    assertFalse(json.toLowerCase().contains("inbox"), json);

    WorkflowStepRoleAssignmentList roundTrip =
        mapper.readValue(json, WorkflowStepRoleAssignmentList.class);
    assertTrue(roundTrip.getAssignments().get(0).isNotify());
    assertEquals("ASSIGNEE", roundTrip.getAssignments().get(0).getAssignmentType());
  }
}
