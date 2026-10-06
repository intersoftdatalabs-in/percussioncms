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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rest.JacksonContextResolver;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/** Slice 57 aging-transition create body: Jackson root wrap round-trip. */
@Tag("UnitTest")
public class WorkflowAgingTransitionWriteSerialDeserialTest {

  @Test
  public void bodyRoundTripKeepsStepsAndMinutes() {
    WorkflowAgingTransitionWrite body = new WorkflowAgingTransitionWrite();
    body.setFrom("Draft");
    body.setTo("Review");
    body.setIntervalMinutes(15);

    ObjectMapper mapper = new JacksonContextResolver().getContext(WorkflowAgingTransitionWrite.class);
    String json = mapper.writeValueAsString(body);
    assertTrue(json.contains("Draft"), json);
    assertTrue(json.contains("Review"), json);
    assertTrue(json.contains("15"), json);

    WorkflowAgingTransitionWrite roundTrip = mapper.readValue(json, WorkflowAgingTransitionWrite.class);
    assertEquals("Draft", roundTrip.getFrom());
    assertEquals("Review", roundTrip.getTo());
    assertEquals(15L, roundTrip.getIntervalMinutes());
    assertNull(roundTrip.getType());
    assertEquals(WorkflowAgingTransitionWrite.Kind.ABSOLUTE, roundTrip.kind());
  }

  @Test
  public void repeatedTypeRoundTripsAndUnknownTypeIsRejected() {
    WorkflowAgingTransitionWrite body = new WorkflowAgingTransitionWrite();
    body.setFrom("Draft");
    body.setTo("Review");
    body.setIntervalMinutes(15);
    body.setType("REPEATED");

    ObjectMapper mapper = new JacksonContextResolver().getContext(WorkflowAgingTransitionWrite.class);
    String json = mapper.writeValueAsString(body);
    assertTrue(json.contains("REPEATED"), json);
    WorkflowAgingTransitionWrite roundTrip = mapper.readValue(json, WorkflowAgingTransitionWrite.class);
    assertEquals("REPEATED", roundTrip.getType());
    assertEquals(WorkflowAgingTransitionWrite.Kind.REPEATED, roundTrip.kind());

    roundTrip.setType("nope");
    assertThrows(IllegalArgumentException.class, roundTrip::kind);
  }

  @Test
  public void systemFieldTypeRoundTripsAndBlankFieldIsRejected() {
    WorkflowAgingTransitionWrite body = new WorkflowAgingTransitionWrite();
    body.setFrom("Draft");
    body.setTo("Review");
    body.setType("SYSTEM_FIELD");
    body.setSystemField("contentstartdate");

    ObjectMapper mapper = new JacksonContextResolver().getContext(WorkflowAgingTransitionWrite.class);
    String json = mapper.writeValueAsString(body);
    assertTrue(json.contains("SYSTEM_FIELD"), json);
    assertTrue(json.contains("contentstartdate"), json);
    WorkflowAgingTransitionWrite roundTrip = mapper.readValue(json, WorkflowAgingTransitionWrite.class);
    assertEquals(WorkflowAgingTransitionWrite.Kind.SYSTEM_FIELD, roundTrip.kind());
    assertEquals("CONTENTSTARTDATE", roundTrip.canonicalSystemField());

    roundTrip.setSystemField(" ");
    assertThrows(IllegalArgumentException.class, roundTrip::canonicalSystemField);
    roundTrip.setSystemField("sys_title");
    assertThrows(IllegalArgumentException.class, roundTrip::canonicalSystemField);
  }
}
