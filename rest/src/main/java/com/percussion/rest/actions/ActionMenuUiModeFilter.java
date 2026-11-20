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

import java.util.ArrayList;
import java.util.List;

/**
 * Server-side enforcement of per-mode UI contexts for SPA entries (#6619,
 * parent #4530).
 *
 * <p>Workbench action menus may carry {@code uiContexts} entries scoping
 * them to (mode, context) combos. The modern Explorer shell claims one UI
 * mode ({@link ActionMenuModeUIContext#SPA_SHELL_MODE_ID} /
 * {@link ActionMenuModeUIContext#SPA_SHELL_MODE_NAME}); this filter hides
 * menus scoped exclusively to other modes. Matching delegates to
 * {@link ActionMenuModeUIContext#isVisibleInUiMode} so the server tier and
 * the WebUI evaluator apply the identical rule.</p>
 *
 * <p>Cascade semantics mirror the client evaluator: a hidden parent takes
 * its whole subtree, and a visible parent whose every child is hidden keeps
 * no button (the parent drops). Pure: never mutates the input.</p>
 */
public final class ActionMenuUiModeFilter {

  /** No instances. */
  private ActionMenuUiModeFilter() {}

  /**
   * Filters a menu tree by a UI-mode claim.
   *
   * @param menus the menu roots, may be {@code null}
   * @param uiMode the claimed mode token (id or name); blank means no constraint
   * @return the surviving roots, never {@code null}
   */
  public static List<ActionMenu> filterMenusByUiMode(List<ActionMenu> menus, String uiMode) {
    List<ActionMenu> out = new ArrayList<>();
    if (menus == null || menus.isEmpty()) {
      return out;
    }
    if (uiMode == null || uiMode.isBlank()) {
      return new ArrayList<>(menus);
    }
    for (ActionMenu menu : menus) {
      if (menu == null || menu.getName() == null) {
        continue;
      }
      if (!ActionMenuModeUIContext.isVisibleInUiMode(menu.getUiContexts(), uiMode)) {
        continue;
      }
      ActionMenuList children = menu.getChildren();
      if (children == null) {
        out.add(menu);
        continue;
      }
      List<ActionMenu> kept = filterMenusByUiMode(children, uiMode);
      if (kept.isEmpty() && !children.isEmpty()) {
        continue;
      }
      // Prune into a copy: the input tree (adaptor-owned) is never mutated,
      // so hidden children of a visible parent do not leak to the caller.
      ActionMenu copy = copyMenu(menu);
      ActionMenuList pruned = new ActionMenuList();
      pruned.addAll(kept);
      copy.setChildren(pruned);
      out.add(copy);
    }
    return out;
  }

  /**
   * Shallow-copies a menu DTO (child list replaced by the caller).
   *
   * <p>Array payloads are shared by reference — the filter never writes
   * into them — while the child list is always set explicitly via
   * {@link ActionMenu#setChildren}.</p>
   *
   * @param menu the source menu, never {@code null}
   * @return a copy carrying the same scalar and array fields
   */
  private static ActionMenu copyMenu(ActionMenu menu) {
    ActionMenu copy = new ActionMenu();
    copy.setId(menu.getId());
    copy.setGuid(menu.getGuid());
    copy.setName(menu.getName());
    copy.setLabel(menu.getLabel());
    copy.setDescription(menu.getDescription());
    copy.setUrl(menu.getUrl());
    copy.setSortRank(menu.getSortRank());
    copy.setMenuType(menu.getMenuType());
    copy.setHandler(menu.getHandler());
    copy.setParentId(menu.getParentId());
    copy.setParameters(menu.getParameters());
    copy.setVisibilityContexts(menu.getVisibilityContexts());
    copy.setUiContexts(menu.getUiContexts());
    copy.setProperties(menu.getProperties());
    copy.setPartialOverlay(menu.isPartialOverlay());
    return copy;
  }
}
