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

/**
 * Server-action enablement / visibility helpers for product Explorer
 * (#2849 / parent #2407 / grandparent #2400).
 *
 * <p>The REST {@code /actions/*} catalog includes Desktop Content Explorer
 * (DCE) menu entries that are not web-executable (custom app protocols,
 * {@code file:}, {@code javascript:}, empty client markers that only the
 * desktop CX understands). The product SPA must not surface those as
 * toolbar / context-menu affordances. These pure helpers filter
 * {@link MenuAction} trees after mapping from wire {@code ActionMenu}
 * DTOs — they do not invent new action types and do not execute actions.</p>
 *
 * <p>Rules (FR-011: hide unauthorized / non-applicable):</p>
 * <ul>
 *   <li>Client-handled leaves (no URL, or URL sentinel {@code CLIENT}) stay —
 *       the shell routes them through {@code onInvoke}.</li>
 *   <li>Leaves with a URL must pass {@link classifyUrl} (same-origin /
 *       relative / http(s) whitelist) or they are treated as desktop-only
 *       and dropped for the SPA surface.</li>
 *   <li>{@code CONTEXTMENU} roots are context-menu only (not toolbar
 *       chrome), matching DCE menu-type semantics.</li>
 *   <li>Empty cascading {@code MENU} parents after filtering are dropped.</li>
 * </ul>
 */

import { collapseFlattenedMenuActionRoots } from "../api/contentExplorer/actionMenuApi";
import type { MenuAction, PSPathItem } from "../api/contentExplorer/types";
import { classifyUrl } from "../util/safeNavigate";
import { resolvePublishKind } from "./itemPublish";
import { isFolder } from "./selection";

/** Where the filtered menu will be rendered. */
export type ActionSurface = "toolbar" | "contextmenu";

/**
 * Optional selection context for enablement. Multi-select / clipboard is
 * intentionally out of scope for #2849 (see #2408).
 */
export interface ActionEnablementContext {
  surface: ActionSurface;
  /**
   * Currently selected detail-list item, or {@code null} when only a folder
   * is active. Toolbar and context-menu Publish Now are hidden until a
   * page/asset is selected so a Sites-folder click cannot claim published
   * (#3467).
   */
  selectionItem?: PSPathItem | null;
  /**
   * Synthetic base URL for {@link classifyUrl} (tests pass an absolute
   * origin; production callers may omit and use {@code window.location}).
   */
  baseHref?: string;
}

/**
 * DCE / legacy CX marks pure client actions with the literal URL token
 * {@code CLIENT} (see ContentExplorerMenu.xml). That is not a navigable
 * href — the SPA treats it as "client-handled" like a missing URL.
 */
const CLIENT_URL_SENTINELS: ReadonlySet<string> = new Set([
  "client",
  "clientaction",
  "client-action",
]);

/**
 * True when the action has no navigable URL and should be delegated to
 * the shell {@code onInvoke} path (or is a pure cascade parent).
 */
export function isClientHandledAction(action: MenuAction): boolean {
  const raw = action.url;
  if (raw == null) return true;
  const trimmed = String(raw).trim();
  if (trimmed.length === 0) return true;
  return CLIENT_URL_SENTINELS.has(trimmed.toLowerCase());
}

/**
 * True when the action URL cannot run in the product SPA (desktop-only
 * protocol, different origin, or known-dangerous scheme). Client-handled
 * actions are never desktop-only.
 */
export function isDesktopOnlyActionUrl(
  url: string | undefined | null,
  baseHref?: string,
): boolean {
  if (url == null) return false;
  const trimmed = String(url).trim();
  if (trimmed.length === 0) return false;
  if (CLIENT_URL_SENTINELS.has(trimmed.toLowerCase())) return false;
  const result = classifyUrl(trimmed, baseHref);
  return !result.ok;
}

/**
 * True when a leaf action is usable in the SPA: client-handled or a
 * web-safe URL. Cascade parents with children are evaluated separately.
 */
export function isWebExecutableLeaf(
  action: MenuAction,
  baseHref?: string,
): boolean {
  if (isClientHandledAction(action)) {
    return true;
  }
  return !isDesktopOnlyActionUrl(action.url, baseHref);
}

/**
 * Whether this action (as a root or nested entry) may appear on the
 * given surface before child filtering.
 *
 * <p>{@code CONTEXTMENU} menu types are DCE context-popup roots; they are
 * kept for the context-menu surface and hidden from the horizontal
 * toolbar so product chrome does not dump the entire popup tree as
 * buttons.</p>
 */
export function isActionAllowedOnSurface(
  action: MenuAction,
  surface: ActionSurface,
): boolean {
  const type = (action.menuType ?? "MENUITEM").toUpperCase();
  if (surface === "toolbar" && type === "CONTEXTMENU") {
    return false;
  }
  return true;
}

/**
 * Unwrap {@link MenuAction.children} whether the tree is already mapped
 * (array) or still carries a Jackson {@code ActionMenu}/{@code ActionMenuList}
 * envelope. Toolbar chrome must see an array or MENU parents render as
 * ordinary buttons (#3560).
 */
export function unwrapMenuActionChildren(
  children: MenuAction[] | unknown,
): MenuAction[] {
  if (children == null) {
    return [];
  }
  if (Array.isArray(children)) {
    return children.filter(
      (child): child is MenuAction =>
        child != null && typeof child.name === "string" && child.name.length > 0,
    );
  }
  if (typeof children === "object") {
    const env = children as Record<string, unknown>;
    const listed =
      env.ActionMenuList ?? env.ActionMenu ?? env.actionMenuList ?? env.actionMenu;
    if (Array.isArray(listed)) {
      return unwrapMenuActionChildren(listed);
    }
    if (
      listed &&
      typeof listed === "object" &&
      typeof (listed as MenuAction).name === "string"
    ) {
      return [listed as MenuAction];
    }
  }
  return [];
}

function hasChildren(action: MenuAction): boolean {
  return unwrapMenuActionChildren(action.children).length > 0;
}

/**
 * Recursively copy a tree so {@code children} is always an array (or omitted).
 * Pure: does not mutate the input.
 */
export function normalizeMenuActionTree(
  actions: MenuAction[] | null | undefined,
): MenuAction[] {
  if (actions == null || actions.length === 0) {
    return [];
  }
  const out: MenuAction[] = [];
  for (const action of actions) {
    if (!action || !action.name) {
      continue;
    }
    const kids = normalizeMenuActionTree(
      unwrapMenuActionChildren(action.children),
    );
    if (kids.length === 0) {
      const rest: MenuAction = { ...action };
      delete rest.children;
      out.push(rest);
      continue;
    }
    out.push({ ...action, children: kids });
  }
  return out;
}

/**
 * Shared unwrap + collapse for Explorer surfaces: Jackson
 * {@code ActionMenu}/{@code ActionMenuList} envelopes become arrays,
 * then descendant names that were also dumped as roots are dropped so
 * neither the toolbar nor the item context menu can flatten MENU
 * children (#3560 toolbar / #3629 context menu).
 */
export function prepareMenuActionTree(
  actions: MenuAction[] | null | undefined,
): MenuAction[] {
  return collapseFlattenedMenuActionRoots(normalizeMenuActionTree(actions));
}

/**
 * Toolbar-ready tree: unwrap envelopes, then drop roots that already
 * appear as descendants so ActionToolbar cannot dump MENU children as
 * extra top-level buttons (#3560 / #3379).
 */
export function prepareToolbarActions(
  actions: MenuAction[] | null | undefined,
): MenuAction[] {
  return prepareMenuActionTree(actions);
}

function actionNameKey(name: string | undefined | null): string {
  return (name ?? "").replace(/[\s-]/g, "_").toLowerCase();
}

/**
 * Publish Now is item-scoped on toolbar and context menu. Folder-only
 * catalogs still include it; hiding it here keeps Sites from looking
 * publishable (#3467).
 */
export function isToolbarPublishNowHidden(
  action: MenuAction,
  selectionItem: PSPathItem | null | undefined,
): boolean {
  if (actionNameKey(action.name) !== "publish_now") {
    return false;
  }
  return resolvePublishKind(selectionItem ?? null) === "none";
}

/**
 * Edit / Quick Edit / View content need a selected page or asset.
 * Folder-only catalogs still include those leaves; hiding them keeps
 * Sites non-editable (#3638). Toolbar {@code Open} stays — folders
 * browse; items open the React editor.
 */
const EDITOR_ACTION_KEYS: ReadonlySet<string> = new Set([
  "edit",
  "edit_content",
  "edit_properties",
  "quick_edit",
  "view_content",
  "view_properties",
  "revision_viewcontent",
  "revision_viewproperties",
  "revision_promote",
]);

export function isToolbarEditorActionHidden(
  action: MenuAction,
  selectionItem: PSPathItem | null | undefined,
): boolean {
  if (!EDITOR_ACTION_KEYS.has(actionNameKey(action.name))) {
    return false;
  }
  return !selectionItem || isFolder(selectionItem);
}

/**
 * Recursively filter a {@link MenuAction} tree for product Explorer
 * surfaces. Pure: does not mutate the input array or child arrays.
 *
 * @param actions Root actions from {@code mapActionMenusToMenuActions}
 * @param ctx Surface + optional base URL for URL classification
 * @returns New array of enabled actions (may be empty)
 */
export function filterEnabledMenuActions(
  actions: MenuAction[] | null | undefined,
  ctx: ActionEnablementContext,
): MenuAction[] {
  if (actions == null || actions.length === 0) {
    return [];
  }
  const baseHref = ctx.baseHref;
  const out: MenuAction[] = [];
  for (const action of actions) {
    if (!action || !action.name) {
      continue;
    }
    if (!isActionAllowedOnSurface(action, ctx.surface)) {
      continue;
    }

    if (hasChildren(action)) {
      const filteredChildren = filterEnabledMenuActions(
        unwrapMenuActionChildren(action.children),
        ctx,
      );
      if (filteredChildren.length === 0) {
        // Cascade parent with no web-usable children: drop entirely.
        continue;
      }
      out.push({
        ...action,
        children: filteredChildren,
      });
      continue;
    }

    // Leaf
    if (!isWebExecutableLeaf(action, baseHref)) {
      continue;
    }
    if (isToolbarPublishNowHidden(action, ctx.selectionItem)) {
      continue;
    }
    if (isToolbarEditorActionHidden(action, ctx.selectionItem)) {
      continue;
    }
    out.push(action);
  }
  return out;
}

/**
 * Convenience: filter for the horizontal server action toolbar.
 */
export function filterToolbarActions(
  actions: MenuAction[] | null | undefined,
  baseHref?: string,
  selectionItem?: PSPathItem | null,
): MenuAction[] {
  return prepareToolbarActions(
    filterEnabledMenuActions(actions, {
      surface: "toolbar",
      baseHref,
      selectionItem,
    }),
  );
}

/**
 * Convenience: filter for the item/folder context menu popup.
 * Unwraps envelopes and collapses flattened MENU children so right-click
 * chrome matches the toolbar catalog (#3629) instead of a label dump.
 */
export function filterContextMenuActions(
  actions: MenuAction[] | null | undefined,
  baseHref?: string,
  selectionItem?: PSPathItem | null,
): MenuAction[] {
  return prepareMenuActionTree(
    filterEnabledMenuActions(actions, {
      surface: "contextmenu",
      baseHref,
      selectionItem,
    }),
  );
}
