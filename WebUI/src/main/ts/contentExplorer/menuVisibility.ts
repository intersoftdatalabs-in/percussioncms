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
 * Client-side evaluation of Workbench action-menu visibility (#6517 /
 * parent #4530).
 *
 * <p>Desktop Content Explorer drove its menus from the visibility
 * configuration stored on each server action ({@code PSActionVisibilityContext}
 * entries named {@code 1}–{@code 11}, plus per-mode UI contexts). The
 * modern shell previously typed those DTOs
 * ({@code ActionMenuVisibilityContext} / {@code ActionMenuModeUIContext})
 * but never evaluated them, so Workbench visibility rules silently did
 * not shape the React menus the way they shaped DCE.</p>
 *
 * <p>Semantics mirror {@code PSActionVisibilityChecker} (system module):
 * every visibility-context entry lists values that <em>hide</em> the action
 * when they match the current environment. Entries the client cannot
 * resolve (no selection data, unknown context names) are permissive —
 * the action stays visible, matching the pre-#6517 behavior. Anything the
 * client cannot evaluate is a server gap, not a silent rule; see the
 * operator docs section "DCE chrome model parity". Pure module: no DOM,
 * no fetch, no React — Vitest locks the mapping.</p>
 */

import type {
  ActionMenuModeUIContext,
  ActionMenuVisibilityContext,
  MenuAction,
} from "../api/contentExplorer/types";

/** Server numeric visibility-context ids ({@code PSActionVisibilityContext}). */
export const VISIBILITY_CONTEXT_ASSIGNMENT_TYPE = "1";
export const VISIBILITY_CONTEXT_COMMUNITY = "2";
export const VISIBILITY_CONTEXT_CONTENT_TYPE = "3";
export const VISIBILITY_CONTEXT_OBJECT_TYPE = "4";
export const VISIBILITY_CONTEXT_CLIENT_CONTEXT = "5";
export const VISIBILITY_CONTEXT_CHECKOUT_STATUS = "6";
export const VISIBILITY_CONTEXT_ROLES = "7";
export const VISIBILITY_CONTEXT_LOCALES = "8";
export const VISIBILITY_CONTEXT_WORKFLOWS = "9";
export const VISIBILITY_CONTEXT_PUBLISHABLE_TYPE = "10";
export const VISIBILITY_CONTEXT_FOLDER_SECURITY = "11";

/**
 * Workbench alias spellings accepted by the REST facade
 * ({@code ActionMenuAdaptor} alias table). Case-insensitive; surrounding
 * whitespace is ignored. Unknown names return {@code null} (permissive).
 */
const VISIBILITY_CONTEXT_ALIASES: Readonly<Record<string, string>> = {
  assignmenttype: VISIBILITY_CONTEXT_ASSIGNMENT_TYPE,
  community: VISIBILITY_CONTEXT_COMMUNITY,
  contenttype: VISIBILITY_CONTEXT_CONTENT_TYPE,
  objecttype: VISIBILITY_CONTEXT_OBJECT_TYPE,
  clientcontext: VISIBILITY_CONTEXT_CLIENT_CONTEXT,
  checkoutstatus: VISIBILITY_CONTEXT_CHECKOUT_STATUS,
  roles: VISIBILITY_CONTEXT_ROLES,
  role: VISIBILITY_CONTEXT_ROLES,
  locales: VISIBILITY_CONTEXT_LOCALES,
  locale: VISIBILITY_CONTEXT_LOCALES,
  workflows: VISIBILITY_CONTEXT_WORKFLOWS,
  workflow: VISIBILITY_CONTEXT_WORKFLOWS,
  publishable: VISIBILITY_CONTEXT_PUBLISHABLE_TYPE,
  publishabletype: VISIBILITY_CONTEXT_PUBLISHABLE_TYPE,
  foldersecurity: VISIBILITY_CONTEXT_FOLDER_SECURITY,
};

/**
 * Normalize a wire visibility-context name to its numeric id. Accepts the
 * numeric ids ({@code "1"}–{@code "11"}) and the Workbench alias
 * spellings. Returns {@code null} for blank or unknown names.
 */
export function normalizeVisibilityContextName(
  raw: unknown,
): string | null {
  if (typeof raw !== "string") {
    return null;
  }
  const trimmed = raw.trim();
  if (!trimmed) {
    return null;
  }
  if (/^(1[0-1]|[1-9])$/.test(trimmed)) {
    return trimmed;
  }
  return VISIBILITY_CONTEXT_ALIASES[trimmed.toLowerCase()] ?? null;
}

/**
 * Read the listed hide-values off one wire visibility-context entry.
 * The REST DTO serializes one value per entry ({@code value}); the
 * client also accepts a {@code values} array for forward compatibility.
 * Blanks are dropped; everything is stringified for comparison.
 */
export function visibilityContextValues(
  entry: ActionMenuVisibilityContext | null | undefined,
): string[] {
  if (entry == null || typeof entry !== "object") {
    return [];
  }
  const record = entry as unknown as Record<string, unknown>;
  const raw = record.value ?? record.values;
  const collect = (candidate: unknown): string[] => {
    if (candidate == null) {
      return [];
    }
    if (Array.isArray(candidate)) {
      return candidate.flatMap(collect);
    }
    const text = String(candidate).trim();
    return text ? [text] : [];
  };
  return collect(raw);
}

/**
 * Server multi-select property ({@code PSAction.PROP_MUTLI_SELECT}).
 * An action may back a multi-object request only when this property is
 * {@code "yes"} (case-insensitive). Pure helper so the shell and tests
 * share one spelling.
 */
export const MULTI_SELECT_PROPERTY_NAME = "SupportsMultiSelect";

function menuSupportsMultiSelect(menu: {
  properties?: ReadonlyArray<{ name?: string; value?: string }> | null;
}): boolean {
  const props = menu.properties;
  if (!props) {
    return false;
  }
  for (const prop of props) {
    if (
      typeof prop?.name === "string" &&
      prop.name.trim().toLowerCase() ===
        MULTI_SELECT_PROPERTY_NAME.toLowerCase() &&
      typeof prop.value === "string" &&
      prop.value.trim().toLowerCase() === "yes"
    ) {
      return true;
    }
  }
  return false;
}

/**
 * What the shell knows about the current environment when it filters
 * menus. Every field is optional: {@code undefined} means "not known to
 * the client", and unknown state never hides an action (permissive).
 */
export interface ExplorerMenuVisibilityState {
  /** True while more than one row is checked in the detail list. */
  multiSelected?: boolean;
  /** Signed-in user name (bootstrap {@code userName}). */
  userName?: string | null;
  /** Role names: flag-derived (Admin/Designer) plus explicit roles. */
  userRoles?: ReadonlyArray<string>;
  /** Session locale (bootstrap {@code locale}), e.g. {@code "en-us"}. */
  locale?: string | null;
  /** Community id of the current session, when known. */
  communityId?: string | number | null;
  /** Checkout status of the selected item, when known. */
  checkoutStatus?: string | null;
  /** Content-type id of the selected item, when known. */
  contentTypeId?: string | number | null;
  /** Object type of the selected item, when known. */
  objectType?: string | number | null;
  /** Assignment type of the selected item, when known. */
  assignmentType?: string | number | null;
  /** Workflow id of the selected item, when known. */
  workflowId?: string | number | null;
  /** Publishable type of the selected item, when known. */
  publishableType?: string | null;
  /** Client context token (e.g. drag-drop surfaces), when known. */
  clientContext?: string | null;
  /**
   * Coarse access band of the selected row ({@code PSPathItem.accessLevel}).
   * Compared by rank {@code ADMIN > WRITE > READ > VIEW}; hiding only
   * fires when the band is strictly below the listed value's band.
   */
  accessLevel?: "ADMIN" | "WRITE" | "READ" | "VIEW" | null;
}

const ACCESS_RANK: Readonly<Record<string, number>> = {
  view: 1,
  read: 2,
  write: 3,
  admin: 4,
};

function equalsFold(left: string, right: string): boolean {
  return left.toLowerCase() === right.toLowerCase();
}

function numericEquals(raw: string, candidate: string | number | null | undefined): boolean {
  if (candidate == null || candidate === "") {
    return false;
  }
  const left = Number(raw);
  const right = Number(candidate);
  if (!Number.isFinite(left) || !Number.isFinite(right)) {
    return false;
  }
  return left === right;
}

function hidesOnCheckoutStatus(
  values: string[],
  status: string | null | undefined,
): boolean {
  const current = (status ?? "").trim();
  if (!current) {
    return false;
  }
  return values.some(
    (value) =>
      equalsFold(value, current) ||
      (equalsFold(value, "myself") &&
        equalsFold(current, "Checked Out By Me")) ||
      (equalsFold(value, "someoneelse") &&
        equalsFold(current, "Checked Out")),
  );
}

function hidesOnRoles(
  values: string[],
  userRoles: ReadonlyArray<string> | undefined,
): boolean {
  // Server hides when the listed values contain ALL of the user's roles.
  // Without a known role list the client cannot evaluate.
  const roles = (userRoles ?? [])
    .map((role) => (typeof role === "string" ? role.trim() : ""))
    .filter((role) => role.length > 0);
  if (roles.length === 0) {
    return false;
  }
  const listed = new Set(values.map((value) => value.toLowerCase()));
  return roles.every((role) => listed.has(role.toLowerCase()));
}

function hidesOnAccessBand(
  values: string[],
  accessLevel: ExplorerMenuVisibilityState["accessLevel"],
): boolean {
  // Coarse client approximation of the server bitmask check: hide when
  // the row's access band ranks below the listed value's band.
  const current = accessLevel
    ? (ACCESS_RANK[accessLevel.toLowerCase()] ?? 0)
    : 0;
  if (current === 0) {
    return false;
  }
  return values.some((value) => {
    const required = ACCESS_RANK[value.trim().toLowerCase()] ?? 0;
    return required > 0 && current < required;
  });
}

function stateText(value: string | null | undefined): string {
  return (value ?? "").trim();
}

/**
 * Hide-on-match for one normalized context id, mirroring
 * {@code PSActionVisibilityChecker#checkObjectState} /
 * {@code #checkGlobalState}. Returns {@code true} when the listed
 * values match the current state — i.e. the action must be hidden.
 */
function contextHidesAction(
  contextId: string,
  values: string[],
  state: ExplorerMenuVisibilityState,
): boolean {
  if (values.length === 0) {
    return false;
  }
  switch (contextId) {
    case VISIBILITY_CONTEXT_CHECKOUT_STATUS:
      return hidesOnCheckoutStatus(values, state.checkoutStatus);
    case VISIBILITY_CONTEXT_PUBLISHABLE_TYPE:
      return (
        stateText(state.publishableType) !== "" &&
        values.some((value) =>
          equalsFold(value, stateText(state.publishableType)),
        )
      );
    case VISIBILITY_CONTEXT_COMMUNITY:
      return (
        state.communityId != null &&
        state.communityId !== "" &&
        values.some((value) => value === String(state.communityId))
      );
    case VISIBILITY_CONTEXT_ROLES:
      return hidesOnRoles(values, state.userRoles);
    case VISIBILITY_CONTEXT_LOCALES:
      return (
        stateText(state.locale) !== "" &&
        values.some((value) => equalsFold(value, stateText(state.locale)))
      );
    case VISIBILITY_CONTEXT_CLIENT_CONTEXT:
      return (
        stateText(state.clientContext) !== "" &&
        values.some((value) =>
          equalsFold(value, stateText(state.clientContext)),
        )
      );
    case VISIBILITY_CONTEXT_CONTENT_TYPE:
      return values.some((value) =>
        numericEquals(value, state.contentTypeId),
      );
    case VISIBILITY_CONTEXT_OBJECT_TYPE:
      return values.some((value) => numericEquals(value, state.objectType));
    case VISIBILITY_CONTEXT_ASSIGNMENT_TYPE:
      return values.some((value) =>
        numericEquals(value, state.assignmentType),
      );
    case VISIBILITY_CONTEXT_WORKFLOWS:
      return values.some((value) => numericEquals(value, state.workflowId));
    case VISIBILITY_CONTEXT_FOLDER_SECURITY:
      return hidesOnAccessBand(values, state.accessLevel);
    default:
      return false;
  }
}

export interface VisibilityMenuShape {
  visibilityContexts?: ReadonlyArray<ActionMenuVisibilityContext> | null;
  uiContexts?: ReadonlyArray<ActionMenuModeUIContext> | null;
  properties?: ReadonlyArray<{ name?: string; value?: string }> | null;
}

/**
 * True when the menu survives Workbench visibility evaluation for the
 * given state. Unknown context names, unresolvable state, and uiContexts
 * entries are permissive (visible) — the client cannot resolve Workbench
 * mode ids, so uiContexts never hide a menu here; that gap is documented
 * in the operator docs and tracked as a server residual.
 */
export function isMenuVisibleByVisibilityContexts(
  menu: VisibilityMenuShape | null | undefined,
  state: ExplorerMenuVisibilityState,
): boolean {
  if (menu == null) {
    return false;
  }
  if (state.multiSelected && !menuSupportsMultiSelect(menu)) {
    // Mirrors PSActionVisibilityChecker#isVisible: multi-object requests
    // hide actions that do not declare multi support.
    return false;
  }
  const contexts = menu.visibilityContexts ?? [];
  for (const entry of contexts) {
    const contextId = normalizeVisibilityContextName(
      (entry as unknown as Record<string, unknown>)?.name,
    );
    if (contextId == null) {
      continue;
    }
    if (
      contextHidesAction(contextId, visibilityContextValues(entry), state)
    ) {
      return false;
    }
  }
  return true;
}

/**
 * Filter a mapped menu tree by Workbench visibility. Hidden parents take
 * their whole subtree (DCE cascade behavior); visible parents keep only
 * their visible children. Pure: does not mutate the input.
 */
export function filterMenuActionsByVisibility(
  menus: MenuAction[] | null | undefined,
  state: ExplorerMenuVisibilityState,
): MenuAction[] {
  if (menus == null || menus.length === 0) {
    return [];
  }
  const out: MenuAction[] = [];
  for (const menu of menus) {
    if (menu == null || !menu.name) {
      continue;
    }
    if (!isMenuVisibleByVisibilityContexts(menu, state)) {
      continue;
    }
    // Recurse only over array children. Legacy envelope shapes
    // ({@code ActionMenuList}) pass through untouched — downstream
    // {@code normalizeMenuActionTree} unwraps them as before.
    if (!Array.isArray(menu.children)) {
      out.push({ ...menu });
      continue;
    }
    const children = filterMenuActionsByVisibility(menu.children, state);
    if (children.length === 0 && menu.children.length > 0) {
      // A cascade parent whose every child is hidden keeps no button.
      continue;
    }
    out.push({ ...menu, children });
  }
  return out;
}
