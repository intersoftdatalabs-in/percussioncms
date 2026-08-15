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
 * Explorer toolbar catalog load (#3379 / parent #2730).
 *
 * <p>Always starts from {@code GET /actions/find} so MENU parents stay
 * nested. {@code POST /actions/find/types} is a flat content-type list
 * and must never replace the cascade (that dump is what human QA saw on
 * #2783). Extra type menus are merged under an existing New/Content MENU
 * or dropped — they are not appended as top-level toolbar buttons.</p>
 */

import {
  findActions,
  findAllowedContentTypeMenus,
  findAllowedTemplateMenus,
  mapActionMenusToMenuActions,
} from "../api/contentExplorer/actionMenuApi";
import type { MenuAction, PSPathItem } from "../api/contentExplorer/types";
import { isWorkflowEligibleItem } from "./workflowEligibility";

export function parseExplorerContentId(
  id: string | number | undefined,
): number | null {
  if (id == null || id === "") {
    return null;
  }
  if (typeof id === "number") {
    return Number.isFinite(id) && id > 0 ? Math.trunc(id) : null;
  }
  const s = String(id).trim();
  if (!s) {
    return null;
  }
  const whole = Number(s);
  if (Number.isFinite(whole) && whole > 0) {
    return Math.trunc(whole);
  }
  // Percussion GUID host-type-uuid (e.g. 1-101-708) — content id is last segment.
  const last = s.split("-").pop();
  if (!last) {
    return null;
  }
  const n = Number(last);
  return Number.isFinite(n) && n > 0 ? Math.trunc(n) : null;
}

function isCascadeParent(action: MenuAction): boolean {
  const type = (action.menuType ?? "").toUpperCase();
  return type === "MENU" || type === "CONTEXTMENU" || type === "DYNAMICMENU";
}

function collectActionNames(actions: MenuAction[], into: Set<string>): void {
  for (const action of actions) {
    if (action?.name) {
      into.add(action.name);
    }
    if (action.children?.length) {
      collectActionNames(action.children, into);
    }
  }
}

/**
 * Normalized {@code name} keys that host leftover content-type MENUITEMs.
 * Underscores/spaces/hyphens are stripped before compare, so {@code new_item}
 * matches {@code newitem}. Include common synonyms (create) for localized or
 * custom New/Create menus.
 */
export const NEW_ITEM_HOST_PREFERRED_KEYS: readonly string[] = [
  "new",
  "newitem",
  "contenttypes",
  "content",
  "create",
  "create_new_item",
  "createnewitem",
];

/** Normalized names of Preview MENU parents that host template children. */
export const PREVIEW_HOST_PREFERRED_KEYS: readonly string[] = [
  "itempreview",
  "enterprisepreview",
  "corporatepreview",
  "preview",
  "slotitempreview",
];

const NEW_ITEM_HOST_LABEL = /new|create/i;

function findNewItemHost(tree: MenuAction[]): MenuAction | null {
  const stack = [...tree];
  let fallback: MenuAction | null = null;
  while (stack.length > 0) {
    const node = stack.shift()!;
    if (isCascadeParent(node)) {
      const key = node.name.replace(/[\s_-]/g, "").toLowerCase();
      if (
        NEW_ITEM_HOST_PREFERRED_KEYS.includes(key) ||
        NEW_ITEM_HOST_LABEL.test(node.label ?? node.name)
      ) {
        return node;
      }
      if (fallback == null) {
        fallback = node;
      }
    }
    if (node.children?.length) {
      stack.push(...node.children);
    }
  }
  return fallback;
}

function cloneAction(action: MenuAction): MenuAction {
  return {
    ...action,
    children: action.children?.map(cloneAction),
  };
}

/**
 * Merge per-content-type menus into the cascading {@code find()} tree
 * without flattening. Pure: does not mutate inputs.
 */
export function mergeContentTypeMenusIntoCatalog(
  tree: MenuAction[],
  typeMenus: MenuAction[],
): MenuAction[] {
  if (typeMenus.length === 0) {
    return tree.slice();
  }
  const out = tree.map(cloneAction);
  const known = new Set<string>();
  collectActionNames(out, known);

  const extraParents: MenuAction[] = [];
  const extraLeaves: MenuAction[] = [];
  for (const menu of typeMenus) {
    if (!menu?.name || known.has(menu.name)) {
      continue;
    }
    const copy = cloneAction(menu);
    if ((copy.children?.length ?? 0) > 0 || isCascadeParent(copy)) {
      extraParents.push(copy);
    } else {
      extraLeaves.push(copy);
    }
  }

  if (extraLeaves.length > 0) {
    const host = findNewItemHost(out) ?? extraParents.find(isCascadeParent);
    if (host) {
      host.children = [...(host.children ?? []), ...extraLeaves];
    }
    // Else drop leftover leaves — do not dump them as top-level buttons.
  }

  return extraParents.length > 0 ? [...out, ...extraParents] : out;
}

function findPreviewHost(tree: MenuAction[]): MenuAction | null {
  const stack = [...tree];
  while (stack.length > 0) {
    const node = stack.shift()!;
    if (isCascadeParent(node)) {
      const key = node.name.replace(/[\s_-]/g, "").toLowerCase();
      if (PREVIEW_HOST_PREFERRED_KEYS.includes(key)) {
        return node;
      }
    }
    if (node.children?.length) {
      stack.push(...node.children);
    }
  }
  return null;
}

/**
 * Merge template preview menus under an existing Preview MENU parent.
 * Does not flatten leftover leaves onto the toolbar.
 */
export function mergeTemplateMenusIntoCatalog(
  tree: MenuAction[],
  templateMenus: MenuAction[],
): MenuAction[] {
  if (templateMenus.length === 0) {
    return tree.slice();
  }
  const out = tree.map(cloneAction);
  const known = new Set<string>();
  collectActionNames(out, known);
  const extra: MenuAction[] = [];
  for (const menu of templateMenus) {
    if (!menu?.name || known.has(menu.name)) {
      continue;
    }
    extra.push(cloneAction(menu));
  }
  if (extra.length === 0) {
    return out;
  }
  const host = findPreviewHost(out);
  if (host) {
    host.children = [...(host.children ?? []), ...extra];
    return out;
  }
  return out;
}

/**
 * Product default for Explorer toolbar / context-menu catalog load.
 *
 * <p>Always uses {@code GET /actions/find} for MENU cascade. When a
 * workflow-eligible item is selected, type menus are merged under existing
 * parents — they never replace the tree.</p>
 */
export async function loadExplorerMenuCatalog(
  item: PSPathItem | null,
): Promise<MenuAction[]> {
  const menus = await findActions({});
  const tree = mapActionMenusToMenuActions(menus);
  const contentId =
    item && isWorkflowEligibleItem(item)
      ? parseExplorerContentId(item.id)
      : null;
  if (contentId == null) {
    return tree;
  }
  let merged = tree;
  try {
    const typeMenus = mapActionMenusToMenuActions(
      await findAllowedContentTypeMenus([contentId]),
    );
    merged = mergeContentTypeMenusIntoCatalog(merged, typeMenus);
  } catch (err: unknown) {
    console.warn(
      "[ContentExplorerShell] content-type menus load failed; keeping find() cascade",
      err instanceof Error ? err.message : String(err),
    );
  }
  try {
    const templateMenus = mapActionMenusToMenuActions(
      await findAllowedTemplateMenus(contentId, false),
    );
    return mergeTemplateMenusIntoCatalog(merged, templateMenus);
  } catch (err: unknown) {
    console.warn(
      "[ContentExplorerShell] template menus load failed; keeping catalog",
      err instanceof Error ? err.message : String(err),
    );
    return merged;
  }
}
