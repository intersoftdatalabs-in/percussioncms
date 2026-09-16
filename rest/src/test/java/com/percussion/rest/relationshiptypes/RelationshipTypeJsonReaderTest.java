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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** #4470: empty cloneOverrides array is clear, not omit. */
class RelationshipTypeJsonReaderTest {

  @Test
  void parse_emptyCloneOverridesArrayIsEmptyListNotNull() {
    RelationshipType t =
        RelationshipTypeJsonReader.parse(
            "{\"RelationshipType\":{\"label\":\"With overrides\",\"cloneOverrides\":[]}}");
    assertEquals("With overrides", t.getLabel());
    assertNotNull(t.getCloneOverrides());
    assertTrue(t.getCloneOverrides().isEmpty());
  }

  @Test
  void parse_omittedCloneOverridesStaysNull() {
    RelationshipType t =
        RelationshipTypeJsonReader.parse("{\"RelationshipType\":{\"label\":\"No list\"}}");
    assertEquals("No list", t.getLabel());
    assertNull(t.getCloneOverrides());
  }

  @Test
  void parse_singleObjectNotArray() {
    RelationshipType t =
        RelationshipTypeJsonReader.parse(
            "{\"RelationshipType\":{\"cloneOverrides\":{\"fieldName\":\"sys_title\","
                + "\"extensionRef\":\"Java/global/percussion/generic/sys_Literal\","
                + "\"extensionParams\":\"cloned\"}}}");
    assertEquals(1, t.getCloneOverrides().size());
    assertEquals("sys_title", t.getCloneOverrides().get(0).getFieldName());
    assertEquals(java.util.List.of("cloned"), t.getCloneOverrides().get(0).getExtensionParams());
  }

  @Test
  void parse_arrayRoundTrip() {
    RelationshipType t =
        RelationshipTypeJsonReader.parse(
            "{\"RelationshipType\":{\"cloneOverrides\":[{\"fieldName\":\"sys_title\","
                + "\"extensionRef\":\"Java/x\",\"extensionParams\":[\"a\",\"b\"]}]}}");
    assertEquals(1, t.getCloneOverrides().size());
    assertEquals(java.util.List.of("a", "b"), t.getCloneOverrides().get(0).getExtensionParams());
  }

  @Test
  void parse_emptyObjectCloneOverridesIsEmptyListNotPhantom() {
    RelationshipType t =
        RelationshipTypeJsonReader.parse(
            "{\"RelationshipType\":{\"label\":\"Cleared\",\"cloneOverrides\":{}}}");
    assertNotNull(t.getCloneOverrides());
    assertTrue(t.getCloneOverrides().isEmpty());
  }

  @Test
  void parse_jaxbWrapperEmptyArrayIsEmptyList() {
    RelationshipType t =
        RelationshipTypeJsonReader.parse(
            "{\"RelationshipType\":{\"cloneOverrides\":{\"RelationshipTypeCloneOverride\":[]}}}");
    assertNotNull(t.getCloneOverrides());
    assertTrue(t.getCloneOverrides().isEmpty());
  }

  @Test
  void parse_clearCloneOverridesFlagBindsTrue() {
    RelationshipType t =
        RelationshipTypeJsonReader.parse(
            "{\"RelationshipType\":{\"label\":\"Cleared\",\"clearCloneOverrides\":true}}");
    assertEquals(Boolean.TRUE, t.getClearCloneOverrides());
    assertNull(t.getCloneOverrides());
  }

  @Test
  void parse_jaxbWrapperEmptyObjectIsEmptyList() {
    RelationshipType t =
        RelationshipTypeJsonReader.parse(
            "{\"RelationshipType\":{\"cloneOverrides\":{\"RelationshipTypeCloneOverride\":{}}}}");
    assertNotNull(t.getCloneOverrides());
    assertTrue(t.getCloneOverrides().isEmpty());
  }
}
