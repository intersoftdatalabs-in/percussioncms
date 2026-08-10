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
 *
 * <p>Does <strong>not</strong> mutate input {@link PSActionMenu} instances —
 * tree nodes are shallow shells so Hibernate session entities and caller caches
 * keep their original {@code children} state.
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

    // Shell copies only — never call setChildren on caller/Hibernate instances.
    Map<Integer, PSActionMenu> byId = new HashMap<>(all.size() * 2);
    List<PSActionMenu> shellsInOrder = new ArrayList<>(all.size());
    for (PSActionMenu menu : all) {
      if (menu == null) {
        continue;
      }
      int id = menu.getActionId();
      // First occurrence wins for duplicate action ids in the flat list.
      if (byId.containsKey(id)) {
        continue;
      }
      PSActionMenu shell = shellCopy(menu);
      shell.setChildren(new ArrayList<>());
      byId.put(id, shell);
      shellsInOrder.add(shell);
    }

    // childId -> first parentId (for multi-parent dedupe + cycle walk)
    Map<Integer, Integer> parentOf = new HashMap<>();
    Set<Integer> childIds = new HashSet<>();
    if (parentChildPairs != null) {
      for (int[] pair : parentChildPairs) {
        if (pair == null || pair.length < 2) {
          continue;
        }
        int parentId = pair[0];
        int childId = pair[1];
        if (parentId == childId) {
          continue; // self-loop
        }
        PSActionMenu parent = byId.get(parentId);
        PSActionMenu child = byId.get(childId);
        if (parent == null || child == null) {
          continue;
        }
        // Multi-parent: RXMENUACTIONRELATION allows (parent, child) many-to-many;
        // first parent wins so the same MENUITEM is not shared under multiple
        // MENU roots (which would duplicate toolbar entries if both roots show).
        if (parentOf.containsKey(childId)) {
          continue;
        }
        // Cycle guard: if parent is already a descendant of child, skip.
        if (isAncestor(parentOf, parentId, childId)) {
          continue;
        }
        List<PSActionMenu> children = parent.getChildren();
        if (children == null) {
          children = new ArrayList<>();
          parent.setChildren(children);
        }
        children.add(child);
        parentOf.put(childId, parentId);
        childIds.add(childId);
      }
    }

    List<PSActionMenu> roots = new ArrayList<>();
    for (PSActionMenu menu : shellsInOrder) {
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

  /**
   * True if {@code possibleAncestorId} is on the parent-of chain starting at
   * {@code nodeId} (walk toward roots). Used to reject relations that would
   * close a cycle.
   */
  static boolean isAncestor(
      Map<Integer, Integer> parentOf, int nodeId, int possibleAncestorId) {
    int cur = nodeId;
    Set<Integer> seen = new HashSet<>();
    while (parentOf.containsKey(cur)) {
      if (!seen.add(cur)) {
        break; // defensive against corrupt parentOf
      }
      cur = parentOf.get(cur);
      if (cur == possibleAncestorId) {
        return true;
      }
    }
    return false;
  }

  /**
   * Shallow structural copy: identity + display fields only. Children are always
   * rebuilt by {@link #assemble}; parameters/properties/visibility are shared
   * read-only references from the source (not mutated here).
   */
  static PSActionMenu shellCopy(PSActionMenu src) {
    PSActionMenu m =
        new PSActionMenu(
            src.getName(),
            src.getDisplayName(),
            src.getType(),
            src.getUrl(),
            src.getHandler(),
            src.getSortOrder());
    m.setActionId(src.getActionId());
    m.setDescription(src.getDescription());
    m.setVersion(src.getVersion());
    if (src.getParameters() != null) {
      m.setParameters(src.getParameters());
    }
    if (src.getProperties() != null) {
      m.setProperties(src.getProperties());
    }
    if (src.getVisibility() != null) {
      m.setVisibility(src.getVisibility());
    }
    // Leave children null — assemble assigns its own lists on shells only.
    return m;
  }
}
