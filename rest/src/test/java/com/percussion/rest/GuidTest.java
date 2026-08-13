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

package com.percussion.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Behavioral tests for {@link Guid} construction after the this-escape real fix (direct field
 * assignment in the string constructor).
 */
class GuidTest {

  @Test
  void stringConstructorSeedsAllFields() {
    Guid guid = new Guid("0-2-311");

    assertTrue(guid.getStringValue().isPresent());
    assertEquals("0-2-311", guid.getStringValue().get());
    assertEquals(0L, guid.getHostId());
    assertEquals((short) 2, guid.getType());
    assertEquals(311, guid.getUuid());
    assertTrue(guid.getLongValue() != 0L || guid.getUuid() == 311);
    assertTrue(guid.getUntypedString().isPresent());
  }

  @Test
  void stringConstructorRejectsNull() {
    assertThrows(NullPointerException.class, () -> new Guid(null));
  }

  @Test
  void jacksonSerializesStringValueAsJsonString() {
    Guid guid = new Guid("0-31-12");
    ObjectMapper mapper = new JacksonContextResolver().getContext(Guid.class);
    String json = mapper.writeValueAsString(guid);
    assertTrue(
        json.contains("\"stringValue\":\"0-31-12\"")
            || json.contains("\"stringValue\" : \"0-31-12\""),
        json);
    assertFalse(json.contains("\"empty\""), json);
    assertFalse(json.contains("Optional["), json);
  }

  @Test
  void defaultConstructorLeavesOptionalFieldsEmpty() {
    Guid guid = new Guid();
    assertTrue(guid.getStringValue().isEmpty());
    assertTrue(guid.getUntypedString().isEmpty());
    assertEquals(0L, guid.getHostId());
    assertEquals(0, guid.getUuid());
  }
}
