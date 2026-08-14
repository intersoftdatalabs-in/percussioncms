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

/**
 * Pure ARIA tree keyboard helpers for Navigation (#3354 / QA #3155).
 *
 * <p>Tab / Shift+Tab are intentionally not consumed so focus can enter and
 * leave the tree (no keyboard trap). Arrow / Home / End / Enter / Space
 * follow the APG tree pattern.</p>
 */

import type { NavTreeNode } from "../api/architecture/types";
import { isNavBranch } from "./treeModel";

/** Keys the tree owns (preventDefault). Tab is not in this set. */
export const NAV_TREE_ROVING_KEYS = [
  "ArrowUp",
  "ArrowDown",
  "ArrowLeft",
  "ArrowRight",
  "Home",
  "End",
  "Enter",
  " ",
] as const;

export type NavTreeRovingKey = (typeof NAV_TREE_ROVING_KEYS)[number];

export type NavTreeKeyResult =
  | { action: "none" }
  | { action: "prevent" }
  | { action: "select"; id: string; toggleExpand: boolean }
  | { action: "expand"; id: string }
  | { action: "collapse"; id: string }
  | { action: "focus"; id: string };

/** True for keys the Navigation tree must handle (not Tab). */
export function isNavTreeRovingKey(key: string): key is NavTreeRovingKey {
  return (NAV_TREE_ROVING_KEYS as readonly string[]).includes(key);
}

/** Flatten visible nodes in document order for Arrow Up/Down and Home/End. */
export function collectVisibleNavNodes(
  node: NavTreeNode,
  expanded: Record<string, boolean>,
  out: NavTreeNode[] = [],
): NavTreeNode[] {
  out.push(node);
  if (isNavBranch(node) && (expanded[node.id] ?? false)) {
    for (const child of node.children) {
      collectVisibleNavNodes(child, expanded, out);
    }
  }
  return out;
}

/** Parent id map for ArrowLeft focus-to-parent. */
export function buildNavParentMap(
  node: NavTreeNode,
  parentId: string | null = null,
  map: Map<string, string | null> = new Map(),
): Map<string, string | null> {
  map.set(node.id, parentId);
  if (node.children?.length) {
    for (const child of node.children) {
      buildNavParentMap(child, node.id, map);
    }
  }
  return map;
}

/**
 * Resolve an ARIA tree key to a command. {@code none} means the event must
 * not be prevented (Tab, typing, etc.). {@code prevent} means consume the
 * key but do not change selection or expansion (boundary / no-op).
 */
export function resolveNavTreeKey(
  key: string,
  node: NavTreeNode,
  root: NavTreeNode,
  expanded: Record<string, boolean>,
): NavTreeKeyResult {
  if (!isNavTreeRovingKey(key)) {
    return { action: "none" };
  }

  const branch = isNavBranch(node);
  const open = expanded[node.id] ?? false;
  const visible = collectVisibleNavNodes(root, expanded);
  const parentMap = buildNavParentMap(root);
  const idx = visible.findIndex((n) => n.id === node.id);

  if (key === "Enter" || key === " ") {
    return { action: "select", id: node.id, toggleExpand: branch };
  }

  if (key === "ArrowRight") {
    if (branch && !open) {
      return { action: "expand", id: node.id };
    }
    if (branch && open && node.children.length > 0) {
      return { action: "focus", id: node.children[0].id };
    }
    return { action: "prevent" };
  }

  if (key === "ArrowLeft") {
    if (branch && open) {
      return { action: "collapse", id: node.id };
    }
    const parentId = parentMap.get(node.id);
    if (parentId) {
      return { action: "focus", id: parentId };
    }
    return { action: "prevent" };
  }

  if (key === "ArrowDown") {
    if (idx >= 0 && idx < visible.length - 1) {
      return { action: "focus", id: visible[idx + 1].id };
    }
    return { action: "prevent" };
  }

  if (key === "ArrowUp") {
    if (idx > 0) {
      return { action: "focus", id: visible[idx - 1].id };
    }
    return { action: "prevent" };
  }

  if (key === "Home") {
    if (visible.length > 0) {
      return { action: "focus", id: visible[0].id };
    }
    return { action: "prevent" };
  }

  if (key === "End") {
    if (visible.length > 0) {
      return { action: "focus", id: visible[visible.length - 1].id };
    }
    return { action: "prevent" };
  }

  return { action: "none" };
}
