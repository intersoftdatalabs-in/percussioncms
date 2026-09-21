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
 * DTOs. Toolbar / context-menu filtering also injects Explorer
 * <strong>Take Down</strong>, <strong>Stage</strong>,
 * <strong>Remove from Staging</strong>, <strong>Schedule</strong>, and
 * <strong>Publishing History</strong> when a page or asset is selected
 * (CX catalogs expose Publish Now; Finder publishing dropdown is not a
 * CX action).</p>
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
import { parseExplorerContentId } from "../api/contentExplorer/pathItemId";
import type { MenuAction, PSPathItem } from "../api/contentExplorer/types";
import { classifyUrl } from "../util/safeNavigate";
import {
  isCheckinActionName,
  isCheckoutActionName,
  isForceCheckinActionName,
  isPublishingHistoryActionName,
  isRemoveFromStagingActionName,
  isStageActionName,
  isTakedownActionName,
  resolvePublishKind,
} from "./itemPublish";
import { isScheduleActionName } from "./itemScheduleDates";
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
   * (#3467). Take Down, Stage, Remove from Staging, Schedule, and
   * Publishing History follow the same kind rules (#4533 / #4546 /
   * #4547 / #4559).
   */
  selectionItem?: PSPathItem | null;
  /** Admin sessions may inject force check-in (#4561). */
  isAdmin?: boolean;
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
 * Take Down is item-scoped like Publish Now. Folder catalogs must not
 * look unpublishable-from-Sites (#4533).
 */
export function isToolbarTakedownHidden(
  action: MenuAction,
  selectionItem: PSPathItem | null | undefined,
): boolean {
  if (!isTakedownActionName(action.name)) {
    return false;
  }
  return resolvePublishKind(selectionItem ?? null) === "none";
}

/**
 * Stage / Remove from Staging are item-scoped like Publish Now.
 */
export function isToolbarStageHidden(
  action: MenuAction,
  selectionItem: PSPathItem | null | undefined,
): boolean {
  if (
    !isStageActionName(action.name) &&
    !isRemoveFromStagingActionName(action.name)
  ) {
    return false;
  }
  return resolvePublishKind(selectionItem ?? null) === "none";
}

/**
 * Schedule is item-scoped like Publish Now.
 */
export function isToolbarScheduleHidden(
  action: MenuAction,
  selectionItem: PSPathItem | null | undefined,
): boolean {
  if (!isScheduleActionName(action.name)) {
    return false;
  }
  return resolvePublishKind(selectionItem ?? null) === "none";
}

/**
 * Publishing History is item-scoped like Publish Now.
 */
export function isToolbarPublishingHistoryHidden(
  action: MenuAction,
  selectionItem: PSPathItem | null | undefined,
): boolean {
  if (!isPublishingHistoryActionName(action.name)) {
    return false;
  }
  return resolvePublishKind(selectionItem ?? null) === "none";
}

export function isToolbarForceCheckinHidden(
  action: MenuAction,
  selectionItem: PSPathItem | null | undefined,
): boolean {
  if (!isForceCheckinActionName(action.name)) {
    return false;
  }
  return resolvePublishKind(selectionItem ?? null) === "none";
}

export function isToolbarCheckoutHidden(
  action: MenuAction,
  selectionItem: PSPathItem | null | undefined,
): boolean {
  if (!isCheckoutActionName(action.name) && !isCheckinActionName(action.name)) {
    return false;
  }
  return resolvePublishKind(selectionItem ?? null) === "none";
}

/** Injected when CX catalog has no Take Down leaf for a page/asset. */
export const EXPLORER_TAKEDOWN_ACTION: MenuAction = {
  name: "Take_Down",
  label: "Take Down",
  sortRank: 10_000,
  menuType: "MENUITEM",
};

/** Injected when CX catalog has no Stage leaf for a page/asset. */
export const EXPLORER_STAGE_ACTION: MenuAction = {
  name: "Stage",
  label: "Stage",
  sortRank: 10_010,
  menuType: "MENUITEM",
};

/** Injected when CX catalog has no Remove from Staging leaf. */
export const EXPLORER_REMOVE_FROM_STAGING_ACTION: MenuAction = {
  name: "Remove_from_Staging",
  label: "Remove from Staging",
  sortRank: 10_020,
  menuType: "MENUITEM",
};

/** Injected when CX catalog has no Schedule leaf for a page/asset. */
export const EXPLORER_SCHEDULE_ACTION: MenuAction = {
  name: "Schedule",
  label: "Schedule",
  sortRank: 10_030,
  menuType: "MENUITEM",
};

/** Injected when CX catalog has no Publishing History leaf. */
export const EXPLORER_PUBLISHING_HISTORY_ACTION: MenuAction = {
  name: "Publishing_History",
  label: "Publishing History",
  sortRank: 10_040,
  menuType: "MENUITEM",
};

/** Injected for Admin sessions when a page/asset is selected (#4561). */
export const EXPLORER_FORCE_CHECKIN_ACTION: MenuAction = {
  name: "Force_Checkin",
  label: "Force Check-in",
  sortRank: 10_050,
  menuType: "MENUITEM",
};

/** Injected when a page/asset is selected (#4699). */
export const EXPLORER_CHECKOUT_ACTION: MenuAction = {
  name: "Check_Out",
  label: "Check Out",
  sortRank: 10_045,
  menuType: "MENUITEM",
};

/** Injected when a page/asset is selected (#4699). */
export const EXPLORER_CHECKIN_ACTION: MenuAction = {
  name: "Check_In",
  label: "Check In",
  sortRank: 10_046,
  menuType: "MENUITEM",
};

function menuHasMatchingAction(
  actions: MenuAction[],
  match: (name: string | undefined) => boolean,
): boolean {
  for (const action of actions) {
    if (match(action.name)) {
      return true;
    }
    if (action.children && menuHasMatchingAction(action.children, match)) {
      return true;
    }
  }
  return false;
}

function injectPublishItemAction(
  actions: MenuAction[],
  selectionItem: PSPathItem | null | undefined,
  match: (name: string | undefined) => boolean,
  injected: MenuAction,
): MenuAction[] {
  if (resolvePublishKind(selectionItem ?? null) === "none") {
    return actions;
  }
  if (menuHasMatchingAction(actions, match)) {
    return actions;
  }
  return [...actions, { ...injected }];
}

/**
 * CX {@code /actions/find} lists Publish Now, not Finder Take Down.
 * Inject a client-handled leaf when the selection is a page or asset.
 */
export function withExplorerTakedownAction(
  actions: MenuAction[],
  selectionItem: PSPathItem | null | undefined,
): MenuAction[] {
  return injectPublishItemAction(
    actions,
    selectionItem,
    isTakedownActionName,
    EXPLORER_TAKEDOWN_ACTION,
  );
}

/**
 * CX catalogs omit Finder Stage / Remove from Staging. Inject both when
 * the selection is a page or asset.
 */
export function withExplorerStagingActions(
  actions: MenuAction[],
  selectionItem: PSPathItem | null | undefined,
): MenuAction[] {
  return injectPublishItemAction(
    injectPublishItemAction(
      actions,
      selectionItem,
      isStageActionName,
      EXPLORER_STAGE_ACTION,
    ),
    selectionItem,
    isRemoveFromStagingActionName,
    EXPLORER_REMOVE_FROM_STAGING_ACTION,
  );
}

/**
 * CX catalogs omit Finder Schedule. Inject when the selection is a page
 * or asset.
 */
export function withExplorerScheduleAction(
  actions: MenuAction[],
  selectionItem: PSPathItem | null | undefined,
): MenuAction[] {
  return injectPublishItemAction(
    actions,
    selectionItem,
    isScheduleActionName,
    EXPLORER_SCHEDULE_ACTION,
  );
}

/**
 * CX catalogs omit Finder Publishing History. Inject when the selection
 * is a page or asset.
 */
export function withExplorerPublishingHistoryAction(
  actions: MenuAction[],
  selectionItem: PSPathItem | null | undefined,
): MenuAction[] {
  return injectPublishItemAction(
    actions,
    selectionItem,
    isPublishingHistoryActionName,
    EXPLORER_PUBLISHING_HISTORY_ACTION,
  );
}

/**
 * Admin force check-in — only when the session is Admin and a page/asset
 * is selected. Non-Admin catalogs stay unchanged.
 */
export function withExplorerForceCheckinAction(
  actions: MenuAction[],
  selectionItem: PSPathItem | null | undefined,
  isAdmin?: boolean,
): MenuAction[] {
  if (!isAdmin) {
    return actions;
  }
  return injectPublishItemAction(
    actions,
    selectionItem,
    isForceCheckinActionName,
    EXPLORER_FORCE_CHECKIN_ACTION,
  );
}

/**
 * Check-out / check-in of the selected page or asset (#4699). Hidden when
 * the selection is not a page or asset (not applicable).
 */
export function withExplorerCheckoutActions(
  actions: MenuAction[],
  selectionItem: PSPathItem | null | undefined,
): MenuAction[] {
  return injectPublishItemAction(
    injectPublishItemAction(
      actions,
      selectionItem,
      isCheckoutActionName,
      EXPLORER_CHECKOUT_ACTION,
    ),
    selectionItem,
    isCheckinActionName,
    EXPLORER_CHECKIN_ACTION,
  );
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
 * View → IA Relationships (and the matching panel) for a selected page or
 * asset. Requires a parseable CMS content id / GUID — not an asset title
 * whose last hyphenated token happens to be digits (#3811 / #2778).
 *
 * <p>{@code item} is the production {@link PSPathItem} from pathmanagement
 * (same type the list and action catalog use). Folders and unparseable
 * names stay disabled so the shell shows the select-item hint instead of
 * a permission error.</p>
 */
export function canOpenIaRelationships(
  item: PSPathItem | null | undefined,
): boolean {
  if (item == null || isFolder(item)) {
    return false;
  }
  return parseExplorerContentId(item.id) != null;
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
    if (isToolbarTakedownHidden(action, ctx.selectionItem)) {
      continue;
    }
    if (isToolbarStageHidden(action, ctx.selectionItem)) {
      continue;
    }
    if (isToolbarScheduleHidden(action, ctx.selectionItem)) {
      continue;
    }
    if (isToolbarPublishingHistoryHidden(action, ctx.selectionItem)) {
      continue;
    }
    if (isToolbarForceCheckinHidden(action, ctx.selectionItem)) {
      continue;
    }
    if (isToolbarCheckoutHidden(action, ctx.selectionItem)) {
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
  isAdmin?: boolean,
): MenuAction[] {
  return prepareToolbarActions(
    withExplorerForceCheckinAction(
      withExplorerCheckoutActions(
        withExplorerPublishingHistoryAction(
          withExplorerScheduleAction(
            withExplorerStagingActions(
              withExplorerTakedownAction(
                filterEnabledMenuActions(actions, {
                  surface: "toolbar",
                  baseHref,
                  selectionItem,
                  isAdmin,
                }),
                selectionItem,
              ),
              selectionItem,
            ),
            selectionItem,
          ),
          selectionItem,
        ),
        selectionItem,
      ),
      selectionItem,
      isAdmin,
    ),
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
  isAdmin?: boolean,
): MenuAction[] {
  return prepareMenuActionTree(
    withExplorerForceCheckinAction(
      withExplorerCheckoutActions(
        withExplorerPublishingHistoryAction(
          withExplorerScheduleAction(
            withExplorerStagingActions(
              withExplorerTakedownAction(
                filterEnabledMenuActions(actions, {
                  surface: "contextmenu",
                  baseHref,
                  selectionItem,
                  isAdmin,
                }),
                selectionItem,
              ),
              selectionItem,
            ),
            selectionItem,
          ),
          selectionItem,
        ),
        selectionItem,
      ),
      selectionItem,
      isAdmin,
    ),
  );
}
