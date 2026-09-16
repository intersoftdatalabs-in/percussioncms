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

package com.percussion.rest.relationshiptypes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rest.JacksonContextResolver;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Wire contract for {@link RelationshipType} under WRAP_ROOT_VALUE (#4527). Empty {@code
 * cloneOverrides} must serialize as an array so SPA clear-save does not rehydrate a phantom row.
 */
@Tag("UnitTest")
public class RelationshipTypeSerialDeserialTest {

  private final ObjectMapper mapper =
      new JacksonContextResolver().getContext(RelationshipType.class);

  @Test
  public void emptyCloneOverridesSerializesAsArrayNotObject() {
    RelationshipType t = new RelationshipType();
    t.setName("MyUserRel");
    t.setCloneOverrides(List.of());
    String json = mapper.writeValueAsString(t);
    assertTrue(json.contains("\"cloneOverrides\""), json);
    assertTrue(
        json.contains("\"cloneOverrides\":[]") || json.contains("\"cloneOverrides\": []"), json);
    RelationshipType back = mapper.readValue(json, RelationshipType.class);
    assertNotNull(back.getCloneOverrides());
    assertEquals(0, back.getCloneOverrides().size());
  }

  @Test
  public void oneCloneOverrideRoundTripsUnderRoot() {
    RelationshipTypeCloneOverride row = new RelationshipTypeCloneOverride();
    row.setFieldName("sys_title");
    row.setExtensionRef("Java/global/percussion/generic/sys_Literal");
    row.setExtensionParams(List.of("cloned"));
    RelationshipType t = new RelationshipType();
    t.setName("MyUserRel");
    t.setCloneOverrides(List.of(row));
    String json = mapper.writeValueAsString(t);
    assertTrue(json.contains("\"RelationshipType\""), json);
    RelationshipType back = mapper.readValue(json, RelationshipType.class);
    assertEquals(1, back.getCloneOverrides().size());
    assertEquals("sys_title", back.getCloneOverrides().get(0).getFieldName());
    assertEquals(List.of("cloned"), back.getCloneOverrides().get(0).getExtensionParams());
  }
}
