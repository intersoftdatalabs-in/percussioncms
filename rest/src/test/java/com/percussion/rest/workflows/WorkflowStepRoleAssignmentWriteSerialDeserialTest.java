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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rest.JacksonContextResolver;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/** Slice 61 assignment-type body and list: Jackson root wrap round-trip. */
@Tag("UnitTest")
public class WorkflowStepRoleAssignmentWriteSerialDeserialTest {

  @Test
  public void writeBodyRoundTrip() {
    WorkflowStepRoleAssignmentWrite body = new WorkflowStepRoleAssignmentWrite();
    body.setRoleName("Author");
    body.setAssignmentType("READER");

    ObjectMapper mapper =
        new JacksonContextResolver().getContext(WorkflowStepRoleAssignmentWrite.class);
    String json = mapper.writeValueAsString(body);
    assertTrue(json.contains("WorkflowStepRoleAssignmentWrite"), json);
    assertTrue(json.contains("Author"), json);
    assertTrue(json.contains("READER"), json);

    WorkflowStepRoleAssignmentWrite roundTrip =
        mapper.readValue(json, WorkflowStepRoleAssignmentWrite.class);
    assertEquals("Author", roundTrip.getRoleName());
    assertEquals("READER", roundTrip.getAssignmentType());
  }

  @Test
  public void listRoundTripKeepsReader() {
    WorkflowStepRoleAssignment row = new WorkflowStepRoleAssignment();
    row.setStepName("Draft");
    row.setRoleName("Author");
    row.setAssignmentType("READER");
    WorkflowStepRoleAssignmentList list = new WorkflowStepRoleAssignmentList();
    list.setAssignments(List.of(row));

    ObjectMapper mapper =
        new JacksonContextResolver().getContext(WorkflowStepRoleAssignmentList.class);
    String json = mapper.writeValueAsString(list);
    assertTrue(json.contains("WorkflowStepRoleAssignmentList"), json);
    assertTrue(json.contains("READER"), json);

    WorkflowStepRoleAssignmentList roundTrip =
        mapper.readValue(json, WorkflowStepRoleAssignmentList.class);
    assertEquals(1, roundTrip.getAssignments().size());
    assertEquals("Draft", roundTrip.getAssignments().get(0).getStepName());
    assertEquals("Author", roundTrip.getAssignments().get(0).getRoleName());
    assertEquals("READER", roundTrip.getAssignments().get(0).getAssignmentType());
  }
}
