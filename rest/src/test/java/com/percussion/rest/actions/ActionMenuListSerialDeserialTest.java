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

import com.percussion.rest.JacksonContextResolver;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Top-level {@link ActionMenuList} wire shape under the production CXF mapper
 * (#3379 review). Mirrors {@code SiteListSerialDeserialTest}.
 */
@Tag("UnitTest")
public class ActionMenuListSerialDeserialTest {

  private static ActionMenu sampleMenu() {
    ActionMenu menu = new ActionMenu();
    menu.setId(2);
    menu.setName("open");
    menu.setLabel("Open");
    menu.setMenuType("MENUITEM");
    menu.setSortRank(1);
    menu.setParentId(8);
    return menu;
  }

  @Test
  public void productionMapperWrapsTopLevelActionMenuList() {
    ObjectMapper mapper = new JacksonContextResolver().getContext(ActionMenuList.class);
    ActionMenuList list = new ActionMenuList();
    list.add(sampleMenu());

    String json = mapper.writeValueAsString(list);
    assertTrue(json.contains("\"ActionMenuList\""), "expected WRAP_ROOT_VALUE ActionMenuList: " + json);
    assertTrue(json.contains("\"open\""), json);
    assertFalse(
        json.contains("\"empty\""),
        "ActionMenuList must not serialize as an ArrayList bean: " + json);

    JsonNode root = JsonMapper.builder().build().readTree(json);
    assertTrue(root.has("ActionMenuList"), "root wrapper missing: " + json);
    JsonNode items = root.get("ActionMenuList");
    assertTrue(items.isArray(), "ActionMenuList value must be a JSON array: " + json);
    assertEquals(1, items.size());
    assertEquals("open", items.get(0).get("name").asString());
    assertEquals(8, items.get(0).get("parentId").asInt());

    ActionMenuList roundTrip = mapper.readValue(json, ActionMenuList.class);
    assertEquals(1, roundTrip.size());
    assertEquals("open", roundTrip.get(0).getName());
    assertEquals(8, roundTrip.get(0).getParentId());
  }

  @Test
  public void productionMapperSerializesEmptyActionMenuListEnvelope() {
    ObjectMapper mapper = new JacksonContextResolver().getContext(ActionMenuList.class);
    String json = mapper.writeValueAsString(new ActionMenuList());
    assertTrue(json.contains("ActionMenuList") || json.contains("["), json);
    ActionMenuList roundTrip = mapper.readValue(json, ActionMenuList.class);
    assertTrue(roundTrip.isEmpty());
  }

  @Test
  public void productionMapperKeepsNestedChildrenAsArray() {
    ObjectMapper mapper = new JacksonContextResolver().getContext(ActionMenu.class);
    ActionMenu parent = new ActionMenu();
    parent.setId(8);
    parent.setName("file");
    parent.setLabel("File");
    parent.setMenuType("MENU");
    parent.setChildren(new ActionMenuList(java.util.List.of(sampleMenu())));

    String json = mapper.writeValueAsString(parent);
    JsonNode root = JsonMapper.builder().build().readTree(json);
    JsonNode menu = root.has("ActionMenu") ? root.get("ActionMenu") : root;
    JsonNode children = menu.get("children");
    assertTrue(children != null && children.isArray(), "nested children must stay a JSON array: " + json);
    assertEquals("open", children.get(0).get("name").asString());
  }
}
