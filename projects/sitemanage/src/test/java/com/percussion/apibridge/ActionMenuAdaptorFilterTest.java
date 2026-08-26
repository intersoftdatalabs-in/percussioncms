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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.percussion.rest.actions.ActionMenu;
import com.percussion.webservices.ui.IPSUiDesignWs;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for REST action-menu query filters on the nested tree path (#2730 review).
 */
@Tag("UnitTest")
public class ActionMenuAdaptorFilterTest {

  @Test
  public void nullFiltersReturnSameList() {
    ActionMenu a = menu("file", "File", "MENU", "");
    List<ActionMenu> in = Collections.singletonList(a);
    List<ActionMenu> out =
        ActionMenuAdaptor.filterMenus(in, null, null, null, null, null);
    assertEquals(1, out.size());
    assertEquals(a, out.get(0));
  }

  @Test
  public void nameFilterIsCaseInsensitiveSubstring() {
    List<ActionMenu> in =
        Arrays.asList(
            menu("FileMenu", "File", "MENU", ""),
            menu("edit", "Edit", "MENUITEM", "/e"));
    List<ActionMenu> out =
        ActionMenuAdaptor.filterMenus(in, "file", null, null, null, null);
    assertEquals(1, out.size());
    assertEquals("FileMenu", out.get(0).getName());
  }

  @Test
  public void itemTrueKeepsMenuItemsOnly() {
    List<ActionMenu> in =
        Arrays.asList(
            menu("file", "File", "MENU", ""),
            menu("open", "Open", "MENUITEM", "/o"));
    List<ActionMenu> out =
        ActionMenuAdaptor.filterMenus(in, null, null, true, null, null);
    assertEquals(1, out.size());
    assertEquals("open", out.get(0).getName());
  }

  @Test
  public void cascadingTrueKeepsCascadedMenusOnly() {
    List<ActionMenu> in =
        Arrays.asList(
            menu("file", "File", "MENU", ""),
            menu("dyn", "Dynamic", "MENU", "/load"),
            menu("open", "Open", "MENUITEM", "/o"));
    List<ActionMenu> out =
        ActionMenuAdaptor.filterMenus(in, null, null, null, null, true);
    assertEquals(1, out.size());
    assertEquals("file", out.get(0).getName());
  }

  @Test
  public void dynamicTrueKeepsDynamicMenusOnly() {
    List<ActionMenu> in =
        Arrays.asList(
            menu("file", "File", "MENU", ""),
            menu("dyn", "Dynamic", "MENU", "/load"));
    List<ActionMenu> out =
        ActionMenuAdaptor.filterMenus(in, null, null, null, true, null);
    assertEquals(1, out.size());
    assertEquals("dyn", out.get(0).getName());
  }

  @Test
  public void combinedFiltersAndTogether() {
    List<ActionMenu> in =
        Arrays.asList(
            menu("file_ops", "File", "MENU", ""),
            menu("file_dyn", "File Dyn", "MENU", "/x"),
            menu("edit", "Edit", "MENU", ""));
    List<ActionMenu> out =
        ActionMenuAdaptor.filterMenus(in, "file", null, null, null, true);
    assertEquals(1, out.size());
    assertEquals("file_ops", out.get(0).getName());
  }

  @Test
  public void emptyInputSafe() {
    assertTrue(
        ActionMenuAdaptor.filterMenus(Collections.emptyList(), "x", null, null, null, null)
            .isEmpty());
    assertTrue(ActionMenuAdaptor.filterMenus(null, "x", null, null, null, null).isEmpty());
  }

  @Test
  public void findAllowedTemplatesRejectsNonPositiveIds() {
    ActionMenuAdaptor adaptor = new ActionMenuAdaptor(mock(IPSUiDesignWs.class));
    assertTrue(adaptor.findAllowedTemplates(null, false).isEmpty());
    assertTrue(adaptor.findAllowedTemplates(0, true).isEmpty());
    assertTrue(adaptor.findAllowedTemplates(-1, false).isEmpty());
  }

  private static ActionMenu menu(String name, String label, String type, String url) {
    ActionMenu m = new ActionMenu();
    m.setName(name);
    m.setLabel(label);
    m.setMenuType(type);
    m.setUrl(url);
    return m;
  }
}
