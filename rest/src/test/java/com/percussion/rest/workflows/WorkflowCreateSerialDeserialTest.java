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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rest.JacksonContextResolver;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/** Slice 21 workflow create wire DTOs: Jackson root wrap round-trips. */
@Tag("UnitTest")
public class WorkflowCreateSerialDeserialTest {

  @Test
  public void createBodyRoundTripWithRootWrap() {
    WorkflowCreate body = new WorkflowCreate();
    body.setName("Nightly QA");
    body.setDescription("Created by night-issue-prs");

    ObjectMapper mapper = new JacksonContextResolver().getContext(WorkflowCreate.class);
    String json = mapper.writeValueAsString(body);
    assertTrue(json.contains("Nightly QA"), json);

    WorkflowCreate roundTrip = mapper.readValue(json, WorkflowCreate.class);
    assertEquals("Nightly QA", roundTrip.getName());
    assertEquals("Created by night-issue-prs", roundTrip.getDescription());
  }

  @Test
  public void summaryRoundTripWithRootWrap() {
    WorkflowSummary summary = new WorkflowSummary();
    summary.setWorkflowName("Nightly QA");
    summary.setWorkflowDescription("Created by night-issue-prs");
    summary.setDefaultWorkflow(false);

    ObjectMapper mapper = new JacksonContextResolver().getContext(WorkflowSummary.class);
    String json = mapper.writeValueAsString(summary);
    assertTrue(json.contains("Nightly QA"), json);

    WorkflowSummary roundTrip = mapper.readValue(json, WorkflowSummary.class);
    assertNotNull(roundTrip.getWorkflowName());
    assertEquals("Nightly QA", roundTrip.getWorkflowName());
    assertEquals("Created by night-issue-prs", roundTrip.getWorkflowDescription());
    assertFalse(roundTrip.isDefaultWorkflow());
  }
}
