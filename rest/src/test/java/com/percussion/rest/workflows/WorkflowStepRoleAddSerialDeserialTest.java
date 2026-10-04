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

/** Slice 63 add-role body: Jackson root wrap round-trip. Notify and inbox are not on the wire. */
@Tag("UnitTest")
public class WorkflowStepRoleAddSerialDeserialTest {

  @Test
  public void writeBodyRoundTrip() {
    WorkflowStepRoleAdd body = new WorkflowStepRoleAdd();
    body.setRoleName("System");
    body.setAssignmentType("ASSIGNEE");

    ObjectMapper mapper = new JacksonContextResolver().getContext(WorkflowStepRoleAdd.class);
    String json = mapper.writeValueAsString(body);
    assertTrue(json.contains("WorkflowStepRoleAdd"), json);
    assertTrue(json.contains("System"), json);
    assertTrue(json.contains("ASSIGNEE"), json);
    assertFalse(json.toLowerCase().contains("notify"), json);
    assertFalse(json.toLowerCase().contains("inbox"), json);

    WorkflowStepRoleAdd roundTrip = mapper.readValue(json, WorkflowStepRoleAdd.class);
    assertEquals("System", roundTrip.getRoleName());
    assertEquals("ASSIGNEE", roundTrip.getAssignmentType());
  }
}
