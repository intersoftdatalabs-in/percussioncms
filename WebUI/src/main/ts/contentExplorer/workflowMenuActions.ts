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
 * Pure mapping helpers that project itemmanagement workflow transitions into
 * Explorer {@link MenuAction} trees (#2732 / parent #2400).
 *
 * <p>DCE exposes workflow transitions as menu entries under the Workflow
 * action group. The modern Explorer uses the same trigger names from
 * {@code GET .../itemmanagement/workflow/getTransitions/{id}} and tags each
 * action name with a stable client prefix so invoke handlers can route to
 * {@code transitionWithComments} without inventing server menu fields.</p>
 */

import type { MenuAction } from "../api/contentExplorer/types";

/** Stable prefix for client-routed workflow transition menu actions. */
export const WORKFLOW_TRANSITION_PREFIX = "workflow-transition:";

/** Canonical parent menu name for the Workflow group (DCE-aligned). */
export const WORKFLOW_MENU_NAME = "workflow";

/** Default sort rank so the Workflow group trails ordinary server menus. */
export const WORKFLOW_MENU_SORT_RANK = 9000;

export interface BuildWorkflowMenuOptions {
  /** Display label for the parent group (i18n-resolved by caller). */
  groupLabel?: string;
  /** Optional state name for description / accessibility. */
  stateName?: string;
  /** Parent sort rank; defaults to {@link WORKFLOW_MENU_SORT_RANK}. */
  sortRank?: number;
  /** Trigger names that must collect a comment before invoke (#4723). */
  commentRequiredTriggers?: readonly string[] | null;
}

/**
 * True when {@code name} is a client-tagged workflow transition action.
 */
export function isWorkflowTransitionActionName(name: string | undefined): boolean {
  return typeof name === "string" && name.startsWith(WORKFLOW_TRANSITION_PREFIX);
}

/**
 * Extract the server trigger from a tagged action name, or {@code null}.
 */
export function parseWorkflowTransitionTrigger(
  actionName: string | undefined,
): string | null {
  if (!isWorkflowTransitionActionName(actionName)) {
    return null;
  }
  const trigger = actionName!.slice(WORKFLOW_TRANSITION_PREFIX.length).trim();
  return trigger.length > 0 ? trigger : null;
}

/**
 * Build a single MenuAction for one transition trigger.
 */
function foldTrigger(value: string): string {
  return String(value ?? "")
    .trim()
    .toLowerCase()
    .replace(/\s+/g, "");
}

/** True when {@code trigger} is in the server comment-required list (case/space folded). */
export function triggerCommentRequired(
  trigger: string,
  required: readonly string[] | null | undefined,
): boolean {
  const folded = foldTrigger(trigger);
  if (!folded) {
    return false;
  }
  return (required ?? []).some((name) => foldTrigger(name) === folded);
}

export function buildWorkflowTransitionChild(
  trigger: string,
  sortRank: number,
  commentRequired = false,
): MenuAction {
  const label = String(trigger ?? "").trim() || "transition";
  return {
    name: `${WORKFLOW_TRANSITION_PREFIX}${label}`,
    label,
    description: label,
    sortRank,
    menuType: "MENUITEM",
    handler: "client",
    commentRequired,
  };
}

/**
 * Map allowed transition trigger names into a cascading Workflow menu group.
 *
 * <p>Returns {@code null} when there are no usable triggers so the host can
 * omit the group entirely (server is authoritative — FR-011).</p>
 */
export function buildWorkflowTransitionMenu(
  triggers: readonly string[] | null | undefined,
  options: BuildWorkflowMenuOptions = {},
): MenuAction | null {
  const cleaned = (triggers ?? [])
    .map((t) => String(t ?? "").trim())
    .filter((t) => t.length > 0);
  // De-dupe while preserving first-seen order (default trigger is first).
  const seen = new Set<string>();
  const unique: string[] = [];
  for (const t of cleaned) {
    if (seen.has(t)) continue;
    seen.add(t);
    unique.push(t);
  }
  if (unique.length === 0) {
    return null;
  }
  const children = unique.map((t, i) =>
    buildWorkflowTransitionChild(
      t,
      i + 1,
      triggerCommentRequired(t, options.commentRequiredTriggers),
    ),
  );
  const groupLabel = options.groupLabel?.trim() || "Workflow";
  const stateName = options.stateName?.trim();
  return {
    name: WORKFLOW_MENU_NAME,
    label: groupLabel,
    description: stateName
      ? `${groupLabel} (${stateName})`
      : groupLabel,
    sortRank: options.sortRank ?? WORKFLOW_MENU_SORT_RANK,
    menuType: "MENU",
    handler: "client",
    children,
  };
}

/**
 * Merge a Workflow group into an existing action list.
 *
 * <p>Replaces any prior client Workflow group (same {@link WORKFLOW_MENU_NAME})
 * then sorts by {@code sortRank}. Pure — does not mutate inputs.</p>
 */
/**
 * Triggers shared by every supplied Workflow menu (intersection).
 *
 * <p>A {@code null} menu means that item has no allowed transitions (or the
 * load failed). Shared triggers are then empty so Explorer does not offer a
 * transition some selected items cannot take. Comment-required is true when
 * any item marks the shared trigger. Order follows the first menu.</p>
 */
export function intersectWorkflowMenus(
  menus: readonly (MenuAction | null | undefined)[],
  options: BuildWorkflowMenuOptions = {},
): MenuAction | null {
  if (menus.length === 0) {
    return null;
  }
  const perItem: { trigger: string; commentRequired: boolean }[][] = [];
  for (const menu of menus) {
    const children = menu?.children ?? [];
    const rows: { trigger: string; commentRequired: boolean }[] = [];
    for (const child of children) {
      const trigger = parseWorkflowTransitionTrigger(child.name);
      if (!trigger) {
        continue;
      }
      rows.push({
        trigger,
        commentRequired: child.commentRequired === true,
      });
    }
    if (rows.length === 0) {
      return null;
    }
    perItem.push(rows);
  }
  const [first, ...rest] = perItem;
  const shared = (first ?? []).filter((row) =>
    rest.every((list) =>
      list.some((other) => foldTrigger(other.trigger) === foldTrigger(row.trigger)),
    ),
  );
  if (shared.length === 0) {
    return null;
  }
  const commentRequired = shared
    .filter((row) =>
      perItem.some((list) =>
        list.some(
          (other) =>
            foldTrigger(other.trigger) === foldTrigger(row.trigger) &&
            other.commentRequired,
        ),
      ),
    )
    .map((row) => row.trigger);
  return buildWorkflowTransitionMenu(
    shared.map((row) => row.trigger),
    {
      ...options,
      commentRequiredTriggers: [
        ...(options.commentRequiredTriggers ?? []),
        ...commentRequired,
      ],
    },
  );
}

export function mergeWorkflowMenuActions(
  baseActions: readonly MenuAction[] | null | undefined,
  workflowMenu: MenuAction | null | undefined,
): MenuAction[] {
  const base = (baseActions ?? []).filter(
    (a) => a != null && a.name !== WORKFLOW_MENU_NAME,
  );
  if (!workflowMenu || !(workflowMenu.children?.length)) {
    return base.slice().sort((a, b) => a.sortRank - b.sortRank);
  }
  return [...base, workflowMenu].sort((a, b) => a.sortRank - b.sortRank);
}
