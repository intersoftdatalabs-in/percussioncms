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

package com.percussion.rest.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;

/** UI-04: wrap, children, and bare-array ActionMenuList JSON for children PUT. */
class ActionMenuListJsonReaderTest {

  @Test
  void parse_bareArrayHonorsOrder() {
    ActionMenuList list =
        ActionMenuListJsonReader.parse("[{\"name\":\"ChildA\"},{\"name\":\"ChildB\"}]");
    assertEquals(2, list.size());
    assertEquals("ChildA", list.get(0).getName());
    assertEquals("ChildB", list.get(1).getName());
  }

  @Test
  void parse_wrappedActionMenuList() {
    ActionMenuList list =
        ActionMenuListJsonReader.parse(
            "{\"ActionMenuList\":[{\"name\":\"A\"},{\"id\":9}]}");
    assertEquals(2, list.size());
    assertEquals("A", list.get(0).getName());
    assertEquals(9, list.get(1).getId());
  }

  @Test
  void parse_childrenEnvelope() {
    ActionMenuList list =
        ActionMenuListJsonReader.parse("{\"children\":[{\"name\":\"Nested\"}]}");
    assertEquals(1, list.size());
    assertEquals("Nested", list.get(0).getName());
  }

  @Test
  void parse_emptyIsEmptyList() {
    assertTrue(ActionMenuListJsonReader.parse("  ").isEmpty());
    assertTrue(ActionMenuListJsonReader.parse("[]").isEmpty());
  }

  @Test
  void parse_invalidJsonIs400() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> ActionMenuListJsonReader.parse("{"));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(ActionMenuListJsonReader.INVALID_JSON, ex.getMessage());
    String jacksonHint = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
    assertFalse(jacksonHint.contains("unexpected"));
  }

  @Test
  void isReadable_actionMenuListJsonOnly() {
    ActionMenuListJsonReader reader = new ActionMenuListJsonReader();
    assertTrue(
        reader.isReadable(
            ActionMenuList.class, ActionMenuList.class, null, MediaType.APPLICATION_JSON_TYPE));
    assertFalse(
        reader.isReadable(
            ActionMenu.class, ActionMenu.class, null, MediaType.APPLICATION_JSON_TYPE));
  }
}
