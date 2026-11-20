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

/**
 * Covers the SPA shell UI-mode claim (#6619, parent #4530): entry matching,
 * menu-level visibility, and the cascade filter applied by
 * {@code GET /actions/find?uiMode=}.
 */
@Tag("UnitTest")
public class ActionMenuUiModeFilterTest {

  private static ActionMenuModeUIContext entry(String modeId, String modeName) {
    ActionMenuModeUIContext ctx = new ActionMenuModeUIContext();
    ctx.setModeId(modeId);
    ctx.setModeName(modeName);
    ctx.setContextId("1");
    ctx.setContextName("ctx");
    return ctx;
  }

  private static ActionMenu menu(String name, ActionMenuModeUIContext... ui) {
    ActionMenu m = new ActionMenu();
    m.setName(name);
    m.setUiContexts(ui);
    return m;
  }

  @Test
  public void shellClaimIsPublished() {
    assertEquals("4", ActionMenuModeUIContext.SPA_SHELL_MODE_ID);
    assertEquals("CXMAIN", ActionMenuModeUIContext.SPA_SHELL_MODE_NAME);
  }

  @Test
  public void matchesUiModeByNameCaseInsensitive() {
    assertTrue(
        ActionMenuModeUIContext.matchesUiMode(
            entry("4", "CXMAIN"), "cxmain"));
    assertTrue(
        ActionMenuModeUIContext.matchesUiMode(
            entry("4", "CXMAIN"), "CXMAIN"));
  }

  @Test
  public void matchesUiModeById() {
    assertTrue(ActionMenuModeUIContext.matchesUiMode(entry(" 7 ", "Other"), "7"));
  }

  @Test
  public void matchesUiModeRejectsNullsAndBlanks() {
    assertFalse(ActionMenuModeUIContext.matchesUiMode(null, "CXMAIN"));
    assertFalse(ActionMenuModeUIContext.matchesUiMode(entry("4", "CXMAIN"), null));
    assertFalse(ActionMenuModeUIContext.matchesUiMode(entry("4", "CXMAIN"), "  "));
    assertFalse(
        ActionMenuModeUIContext.matchesUiMode(entry("7", "Publishing"), "CXMAIN"));
  }

  @Test
  public void unscopedMenuIsVisibleInEveryMode() {
    assertTrue(ActionMenuModeUIContext.isVisibleInUiMode(null, "CXMAIN"));
    assertTrue(
        ActionMenuModeUIContext.isVisibleInUiMode(new ActionMenuModeUIContext[0], "CXMAIN"));
  }

  @Test
  public void blankClaimIsNoConstraint() {
    assertTrue(
        ActionMenuModeUIContext.isVisibleInUiMode(
            new ActionMenuModeUIContext[] {entry("7", "Publishing")}, null));
    assertTrue(
        ActionMenuModeUIContext.isVisibleInUiMode(
            new ActionMenuModeUIContext[] {entry("7", "Publishing")}, "  "));
  }

  @Test
  public void scopedToClaimedModeStaysVisible() {
    ActionMenuModeUIContext[] ui = {
      entry("7", "Publishing"), entry("4", "CXMAIN")
    };
    assertTrue(ActionMenuModeUIContext.isVisibleInUiMode(ui, "CXMAIN"));
  }

  @Test
  public void scopedExclusivelyElsewhereHides() {
    ActionMenuModeUIContext[] ui = {entry("7", "Publishing"), entry("9", "Design")};
    assertFalse(ActionMenuModeUIContext.isVisibleInUiMode(ui, "CXMAIN"));
  }

  @Test
  public void unresolvableEntryStaysPermissive() {
    ActionMenuModeUIContext[] ui = {entry("7", "Publishing"), entry(" ", " ")};
    assertTrue(ActionMenuModeUIContext.isVisibleInUiMode(ui, "CXMAIN"));
  }

  @Test
  public void filterBlankClaimKeepsTree() {
    ActionMenu m = menu("Edit", entry("7", "Publishing"));
    List<ActionMenu> out = ActionMenuUiModeFilter.filterMenusByUiMode(List.of(m), "  ");
    assertEquals(1, out.size());
  }

  @Test
  public void filterHidesForeignRootButKeepsClaimedSibling() {
    ActionMenu foreign = menu("Publish", entry("7", "Publishing"));
    ActionMenu claimed = menu("Edit", entry("4", "CXMAIN"));
    ActionMenu unscoped = menu("Copy");
    List<ActionMenu> out =
        ActionMenuUiModeFilter.filterMenusByUiMode(
            List.of(foreign, claimed, unscoped), "CXMAIN");
    assertEquals(2, out.size());
    assertEquals("Edit", out.get(0).getName());
    assertEquals("Copy", out.get(1).getName());
  }

  @Test
  public void filterHiddenParentTakesSubtree() {
    ActionMenu parent = menu("Parent", entry("7", "Publishing"));
    ActionMenuList children = new ActionMenuList();
    children.add(menu("Child", entry("4", "CXMAIN")));
    parent.setChildren(children);
    List<ActionMenu> out =
        ActionMenuUiModeFilter.filterMenusByUiMode(List.of(parent), "CXMAIN");
    assertTrue(out.isEmpty());
  }

  @Test
  public void filterParentWithAllChildrenHiddenDrops() {
    ActionMenu parent = menu("Parent");
    ActionMenuList children = new ActionMenuList();
    children.add(menu("Child", entry("7", "Publishing")));
    parent.setChildren(children);
    List<ActionMenu> out =
        ActionMenuUiModeFilter.filterMenusByUiMode(List.of(parent), "CXMAIN");
    assertTrue(out.isEmpty());
  }

  @Test
  public void filterKeepsVisibleChildrenUnderVisibleParent() {
    ActionMenu parent = menu("Parent");
    ActionMenuList children = new ActionMenuList();
    children.add(menu("Gone", entry("7", "Publishing")));
    children.add(menu("Kept", entry("4", "CXMAIN")));
    parent.setChildren(children);
    List<ActionMenu> out =
        ActionMenuUiModeFilter.filterMenusByUiMode(List.of(parent), "CXMAIN");
    assertEquals(1, out.size());
    assertEquals(1, out.get(0).getChildren().size());
    assertEquals("Kept", out.get(0).getChildren().get(0).getName());
  }

  @Test
  public void filterNeverMutatesTheInputTree() {
    ActionMenu parent = menu("Parent");
    parent.setLabel("Parent label");
    ActionMenuList children = new ActionMenuList();
    children.add(menu("Gone", entry("7", "Publishing")));
    children.add(menu("Kept", entry("4", "CXMAIN")));
    parent.setChildren(children);
    List<ActionMenu> out =
        ActionMenuUiModeFilter.filterMenusByUiMode(List.of(parent), "CXMAIN");
    assertEquals(1, out.size());
    // Input still carries both children; the pruned copy carries the label.
    assertEquals(2, parent.getChildren().size());
    assertEquals("Parent label", out.get(0).getLabel());
    assertEquals("Parent", out.get(0).getName());
  }

  @Test
  public void filterNullSafe() {
    assertTrue(ActionMenuUiModeFilter.filterMenusByUiMode(null, "CXMAIN").isEmpty());
  }
}
