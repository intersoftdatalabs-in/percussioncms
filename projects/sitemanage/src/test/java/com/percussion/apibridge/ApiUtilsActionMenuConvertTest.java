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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rest.actions.ActionMenu;
import com.percussion.services.menus.PSActionMenu;
import com.percussion.services.menus.PSActionMenuProperty;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral unit tests for {@link ApiUtils#convertPSActionMenu} cascading children
 * preservation (#2730 residual).
 *
 * <p>Without child conversion, REST {@code /actions/find} trees lose nesting and the
 * modern Explorer {@code ActionToolbar} dumps MENUITEMs as flat buttons.
 */
@Tag("UnitTest")
public class ApiUtilsActionMenuConvertTest {

  private static final String TYPE_MENU = "MENU";
  private static final String TYPE_MENUITEM = "MENUITEM";

  @Test
  public void convertPreservesNestedChildren() {
    PSActionMenu child1 =
        new PSActionMenu("open", "Open", TYPE_MENUITEM, "/open", "client", 1);
    child1.setActionId(2);
    PSActionMenu child2 =
        new PSActionMenu("edit", "Edit", TYPE_MENUITEM, "/edit", "client", 2);
    child2.setActionId(3);
    PSActionMenu parent =
        new PSActionMenu("content", "Content", TYPE_MENU, "", "client", 0);
    parent.setActionId(1);
    parent.setChildren(Arrays.asList(child1, child2));

    ActionMenu converted = ApiUtils.convertPSActionMenu(parent);
    assertEquals("content", converted.getName());
    assertEquals(TYPE_MENU, converted.getMenuType());
    assertNotNull(converted.getChildren());
    assertEquals(2, converted.getChildren().size());
    assertEquals("open", converted.getChildren().get(0).getName());
    assertEquals("edit", converted.getChildren().get(1).getName());
    assertEquals("Open", converted.getChildren().get(0).getLabel());
    assertEquals(1, converted.getChildren().get(0).getParentId());
    assertEquals(1, converted.getChildren().get(1).getParentId());
  }

  @Test
  public void convertSetsActionGuidFromActionId() {
    PSActionMenu leaf =
        new PSActionMenu("rename", "Rename", TYPE_MENUITEM, "", "client", 0);
    leaf.setActionId(9);
    ActionMenu converted = ApiUtils.convertPSActionMenu(leaf);
    assertNotNull(converted.getGuid());
    assertEquals(
        "0-107-9",
        converted.getGuid().getStringValue(),
        "ACTION type 107 + actionId for Object ACL (#3380)");
    assertEquals(107, converted.getGuid().getType());
    assertEquals(9, converted.getGuid().getUuid());
  }

  @Test
  public void convertOmitsGuidWhenActionIdUnset() {
    PSActionMenu leaf =
        new PSActionMenu("draft", "Draft", TYPE_MENUITEM, "", "client", 0);
    leaf.setActionId(0);
    ActionMenu converted = ApiUtils.convertPSActionMenu(leaf);
    assertNull(converted.getGuid());
  }

  @Test
  public void convertOmitsChildrenWhenEmpty() {
    PSActionMenu leaf =
        new PSActionMenu("rename", "Rename", TYPE_MENUITEM, "", "client", 0);
    leaf.setActionId(9);
    ActionMenu converted = ApiUtils.convertPSActionMenu(leaf);
    assertNull(converted.getChildren());
  }

  @Test
  public void convertListSkipsNullEntries() {
    PSActionMenu one =
        new PSActionMenu("one", "One", TYPE_MENUITEM, "", "client", 0);
    one.setActionId(1);
    List<ActionMenu> list = ApiUtils.convertPSActionMenuList(Arrays.asList(null, one));
    assertEquals(1, list.size());
    assertEquals("one", list.get(0).getName());
  }

  @Test
  public void convertListNullIsEmpty() {
    assertTrue(ApiUtils.convertPSActionMenuList(null).isEmpty());
  }

  @Test
  public void convertSkipsPropertiesWithoutPrimaryKey() {
    PSActionMenu leaf =
        new PSActionMenu("preview", "Preview", TYPE_MENUITEM, "/p", "server", 0);
    leaf.setActionId(12);
    leaf.addProperty(new PSActionMenuProperty());
    leaf.addProperty(new PSActionMenuProperty(12, "launchesWindow", "yes"));
    ActionMenu converted = ApiUtils.convertPSActionMenu(leaf);
    assertEquals("preview", converted.getName());
    assertNotNull(converted.getProperties());
    assertEquals(1, converted.getProperties().length);
    assertEquals("launchesWindow", converted.getProperties()[0].getName());
  }
}
