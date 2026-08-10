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

package com.percussion.services.menus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral unit tests for cascading action-menu tree assembly (#2730).
 *
 * <p>Ensures parent/child RXMENUACTIONRELATION pairs produce nested MENU roots
 * rather than a flat list of every MENUITEM.
 */
@Tag("UnitTest")
public class PSActionMenuTreeAssemblerTest {

  private static final String TYPE_MENU = "MENU";
  private static final String TYPE_MENUITEM = "MENUITEM";

  @Test
  public void assembleEmptyInput() {
    assertTrue(PSActionMenuTreeAssembler.assemble(null, null).isEmpty());
    assertTrue(PSActionMenuTreeAssembler.assemble(Collections.emptyList(), null).isEmpty());
  }

  @Test
  public void assembleNestsChildrenAndOmitsThemFromRoots() {
    PSActionMenu file = menu(1, "file", "File", TYPE_MENU, 10);
    PSActionMenu open = menu(2, "open", "Open", TYPE_MENUITEM, 1);
    PSActionMenu save = menu(3, "save", "Save", TYPE_MENUITEM, 2);
    PSActionMenu help = menu(4, "help", "Help", TYPE_MENUITEM, 20);

    List<PSActionMenu> roots =
        PSActionMenuTreeAssembler.assemble(
            Arrays.asList(file, open, save, help),
            Arrays.asList(new int[] {1, 3}, new int[] {1, 2}));

    assertEquals(2, roots.size());
    assertEquals("file", roots.get(0).getName());
    assertEquals("help", roots.get(1).getName());

    List<PSActionMenu> children = roots.get(0).getChildren();
    assertNotNull(children);
    assertEquals(2, children.size());
    // Sorted by sortOrder: open(1), save(2)
    assertEquals("open", children.get(0).getName());
    assertEquals("save", children.get(1).getName());
    assertNull(roots.get(1).getChildren());
  }

  @Test
  public void assembleWithoutPairsReturnsAllAsRoots() {
    PSActionMenu a = menu(1, "a", "A", TYPE_MENUITEM, 2);
    PSActionMenu b = menu(2, "b", "B", TYPE_MENUITEM, 1);
    List<PSActionMenu> roots =
        PSActionMenuTreeAssembler.assemble(Arrays.asList(a, b), Collections.emptyList());
    assertEquals(2, roots.size());
    assertEquals("b", roots.get(0).getName());
    assertEquals("a", roots.get(1).getName());
  }

  private static PSActionMenu menu(
      int id, String name, String label, String type, int sort) {
    PSActionMenu m = new PSActionMenu(name, label, type, "", "server", sort);
    m.setActionId(id);
    return m;
  }
}
