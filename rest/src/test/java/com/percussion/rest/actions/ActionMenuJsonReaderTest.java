/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * you may obtain a copy of the License at
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;

/** #4123: wrap and flat ActionMenu JSON for Admin create POST. */
class ActionMenuJsonReaderTest {

  @Test
  void parse_wrappedSpaEnvelope() {
    ActionMenu menu =
        ActionMenuJsonReader.parse(
            "{\"ActionMenu\":{\"name\":\"n1\",\"label\":\"L\",\"description\":\"d\","
                + "\"menuType\":\"MENUITEM\",\"url\":\"/app\"}}");
    assertEquals("n1", menu.getName());
    assertEquals("L", menu.getLabel());
    assertEquals("d", menu.getDescription());
    assertEquals("MENUITEM", menu.getMenuType());
    assertEquals("/app", menu.getUrl());
  }

  @Test
  void parse_flatBody() {
    ActionMenu menu = ActionMenuJsonReader.parse("{\"name\":\"flatName\",\"label\":\"x\"}");
    assertEquals("flatName", menu.getName());
    assertEquals("x", menu.getLabel());
  }

  @Test
  void parse_emptyIsEmptyDto() {
    ActionMenu menu = ActionMenuJsonReader.parse("  ");
    assertNull(menu.getName());
  }

  @Test
  void parse_invalidJsonIs400() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> ActionMenuJsonReader.parse("{"));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(ActionMenuJsonReader.INVALID_JSON, ex.getMessage());
    String jacksonHint = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
    assertFalse(jacksonHint.contains("unexpected"));
    assertFalse(jacksonHint.contains("offset"));
    assertFalse(jacksonHint.contains("token"));
  }

  @Test
  void parse_nonObjectIs400() {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> ActionMenuJsonReader.parse("[1]"));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  void parse_childrenArrayHonorsNameAndId() {
    ActionMenu menu =
        ActionMenuJsonReader.parse(
            "{\"name\":\"Parent\",\"children\":[{\"name\":\"ChildA\"},{\"id\":12}]}");
    assertEquals("Parent", menu.getName());
    assertEquals(2, menu.getChildren().size());
    assertEquals("ChildA", menu.getChildren().get(0).getName());
    assertEquals(12, menu.getChildren().get(1).getId());
  }

  @Test
  void isReadable_actionMenuJsonOnly() {
    ActionMenuJsonReader reader = new ActionMenuJsonReader();
    assertTrue(
        reader.isReadable(
            ActionMenu.class, ActionMenu.class, null, MediaType.APPLICATION_JSON_TYPE));
    assertFalse(
        reader.isReadable(
            ActionMenuList.class, ActionMenuList.class, null, MediaType.APPLICATION_JSON_TYPE));
    assertFalse(
        reader.isReadable(
            AllowedWorkflowTransitionsRequest.class,
            AllowedWorkflowTransitionsRequest.class,
            null,
            MediaType.APPLICATION_JSON_TYPE));
  }
}
