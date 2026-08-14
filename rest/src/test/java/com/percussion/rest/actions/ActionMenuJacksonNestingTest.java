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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Wire shape for nested {@link ActionMenu} children under the production
 * WRAP_ROOT_VALUE mapper (#3379). ActionMenuList must serialize as a JSON
 * array, not an ArrayList bean ({@code {"empty":false}}).
 */
@Tag("UnitTest")
public class ActionMenuJacksonNestingTest {

  private static JsonMapper wrapRootMapper() {
    return JsonMapper.builder()
        .enable(SerializationFeature.WRAP_ROOT_VALUE)
        .enable(DeserializationFeature.UNWRAP_ROOT_VALUE)
        .build();
  }

  private static ActionMenu sampleTree() {
    ActionMenu child = new ActionMenu();
    child.setId(2);
    child.setName("open");
    child.setLabel("Open");
    child.setMenuType("MENUITEM");
    child.setSortRank(1);
    child.setParentId(8);

    ActionMenu parent = new ActionMenu();
    parent.setId(8);
    parent.setName("file");
    parent.setLabel("File");
    parent.setMenuType("MENU");
    parent.setSortRank(0);
    parent.setChildren(new ActionMenuList(List.of(child)));
    return parent;
  }

  @Test
  public void childrenSerializeAsJsonArrayNotEmptyBean() throws JacksonException {
    String json = wrapRootMapper().writeValueAsString(sampleTree());
    assertTrue(json.contains("\"ActionMenu\""), "expected WRAP_ROOT_VALUE: " + json);
    assertTrue(json.contains("\"open\""), json);
    assertTrue(json.contains("\"file\""), json);
    assertFalse(
        json.contains("\"empty\""),
        "ActionMenuList must not serialize as an ArrayList bean: " + json);

    JsonNode root = JsonMapper.builder().build().readTree(json);
    JsonNode menu = root.has("ActionMenu") ? root.get("ActionMenu") : root;
    JsonNode children = menu.get("children");
    assertTrue(children != null && children.isArray(), "children should be a JSON array: " + json);
    assertEquals(1, children.size());
    assertEquals("open", children.get(0).get("name").asString());
    assertEquals(8, children.get(0).get("parentId").asInt());
  }
}
