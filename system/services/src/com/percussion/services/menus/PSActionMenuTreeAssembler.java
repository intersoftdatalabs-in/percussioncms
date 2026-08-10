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

package com.percussion.services.menus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure assembly of cascading action-menu trees from a flat menu list +
 * parent/child relation pairs ({@code RXMENUACTIONRELATION}).
 *
 * <p>Used by {@code IPSCmsObjectMgr#findActionMenusTree()} so Explorer REST
 * consumers receive nested MENU parents instead of a dumped flat button list
 * (#2730). Extracted for unit testing without a Hibernate session.
 */
public final class PSActionMenuTreeAssembler {

  private PSActionMenuTreeAssembler() {}

  /**
   * @param all flat menus (may include children that also appear at top level)
   * @param parentChildPairs each element is {@code [parentActionId, childActionId]}
   * @return root menus only, children attached and sorted by sort order
   */
  public static List<PSActionMenu> assemble(
      List<PSActionMenu> all, List<int[]> parentChildPairs) {
    if (all == null || all.isEmpty()) {
      return new ArrayList<>();
    }

    Map<Integer, PSActionMenu> byId = new HashMap<>(all.size() * 2);
    for (PSActionMenu menu : all) {
      if (menu == null) {
        continue;
      }
      byId.put(menu.getActionId(), menu);
      menu.setChildren(new ArrayList<>());
    }

    Set<Integer> childIds = new HashSet<>();
    if (parentChildPairs != null) {
      for (int[] pair : parentChildPairs) {
        if (pair == null || pair.length < 2) {
          continue;
        }
        int parentId = pair[0];
        int childId = pair[1];
        PSActionMenu parent = byId.get(parentId);
        PSActionMenu child = byId.get(childId);
        if (parent == null || child == null) {
          continue;
        }
        List<PSActionMenu> children = parent.getChildren();
        if (children == null) {
          children = new ArrayList<>();
          parent.setChildren(children);
        }
        children.add(child);
        childIds.add(childId);
      }
    }

    List<PSActionMenu> roots = new ArrayList<>();
    for (PSActionMenu menu : all) {
      if (menu == null) {
        continue;
      }
      List<PSActionMenu> children = menu.getChildren();
      if (children != null) {
        if (children.isEmpty()) {
          menu.setChildren(null);
        } else {
          children.sort(Comparator.comparingInt(PSActionMenu::getSortOrder));
        }
      }
      if (!childIds.contains(menu.getActionId())) {
        roots.add(menu);
      }
    }
    roots.sort(Comparator.comparingInt(PSActionMenu::getSortOrder));
    return roots;
  }
}
