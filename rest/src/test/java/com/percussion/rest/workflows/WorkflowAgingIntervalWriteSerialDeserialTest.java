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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/** Slice 58 aging-interval change body: Jackson root wrap round-trip. */
@Tag("UnitTest")
public class WorkflowAgingIntervalWriteSerialDeserialTest {

  @Test
  public void bodyRoundTripKeepsStepsAndBothIntervals() {
    WorkflowAgingIntervalWrite body = new WorkflowAgingIntervalWrite();
    body.setFrom("Draft");
    body.setTo("Review");
    body.setIntervalMinutes(15);
    body.setNewIntervalMinutes(30);

    ObjectMapper mapper = new JacksonContextResolver().getContext(WorkflowAgingIntervalWrite.class);
    String json = mapper.writeValueAsString(body);
    assertTrue(json.contains("WorkflowAgingIntervalWrite"), json);
    assertTrue(json.contains("Draft"), json);
    assertTrue(json.contains("Review"), json);
    assertTrue(json.contains("15"), json);
    assertTrue(json.contains("30"), json);

    WorkflowAgingIntervalWrite roundTrip = mapper.readValue(json, WorkflowAgingIntervalWrite.class);
    assertEquals("Draft", roundTrip.getFrom());
    assertEquals("Review", roundTrip.getTo());
    assertEquals(15L, roundTrip.getIntervalMinutes());
    assertEquals(30L, roundTrip.getNewIntervalMinutes());
  }
}
