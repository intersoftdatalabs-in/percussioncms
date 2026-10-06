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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rest.JacksonContextResolver;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/** Slice 79 system-field date-column change body: Jackson root wrap round-trip. */
@Tag("UnitTest")
public class WorkflowAgingSystemFieldWriteSerialDeserialTest {

  @Test
  public void bodyRoundTripKeepsStepsAndBothColumns() {
    WorkflowAgingSystemFieldWrite body = new WorkflowAgingSystemFieldWrite();
    body.setFrom("Draft");
    body.setTo("Review");
    body.setSystemField("CONTENTSTARTDATE");
    body.setNewSystemField("CONTENTEXPIRYDATE");

    ObjectMapper mapper =
        new JacksonContextResolver().getContext(WorkflowAgingSystemFieldWrite.class);
    String json = mapper.writeValueAsString(body);
    assertTrue(json.contains("WorkflowAgingSystemFieldWrite"), json);
    assertTrue(json.contains("Draft"), json);
    assertTrue(json.contains("Review"), json);
    assertTrue(json.contains("CONTENTSTARTDATE"), json);
    assertTrue(json.contains("CONTENTEXPIRYDATE"), json);
    assertTrue(!json.contains("intervalMinutes"), json);

    WorkflowAgingSystemFieldWrite roundTrip =
        mapper.readValue(json, WorkflowAgingSystemFieldWrite.class);
    assertEquals("Draft", roundTrip.getFrom());
    assertEquals("Review", roundTrip.getTo());
    assertEquals("CONTENTSTARTDATE", roundTrip.canonicalSystemField());
    assertEquals("CONTENTEXPIRYDATE", roundTrip.canonicalNewSystemField());
  }

  @Test
  public void blankSameAndUnknownColumnsAreRejected() {
    WorkflowAgingSystemFieldWrite blank = new WorkflowAgingSystemFieldWrite();
    blank.setSystemField("  ");
    blank.setNewSystemField("REMINDERDATE");
    assertThrows(IllegalArgumentException.class, blank::canonicalSystemField);

    WorkflowAgingSystemFieldWrite unknown = new WorkflowAgingSystemFieldWrite();
    unknown.setSystemField("CONTENTSTARTDATE");
    unknown.setNewSystemField("sys_title");
    assertThrows(IllegalArgumentException.class, unknown::canonicalNewSystemField);

    WorkflowAgingSystemFieldWrite same = new WorkflowAgingSystemFieldWrite();
    same.setSystemField(" contentstartdate ");
    same.setNewSystemField("CONTENTSTARTDATE");
    assertEquals(same.canonicalSystemField(), same.canonicalNewSystemField());
  }
}
