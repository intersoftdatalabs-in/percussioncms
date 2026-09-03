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

import { del, get, post, put } from "../client";
import {
  normalizeDesignObjectGuid,
  resolveActionMenuObjectGuid,
} from "../displayFormatGuid";
import { PATHS } from "../paths";
import type {
  ActionMenu,
  ActionMenuModeUIContext,
  ActionMenuParameter,
  ActionMenuProperty,
  ActionMenuVisibilityContext,
} from "./types";

export { resolveActionMenuObjectGuid };

/**
 * Writable fields for POST/PUT /services/actions. Name is the catalog key
 * (not renamed on PUT). Create POST sends identity fields only (JAXB #4171).
 * PUT also sends usage/command/visibility (UI-03). Nested {@code children} on
 * this body are ignored; ordered child associations use {@link saveActionMenuChildren}.
 */
export type ActionMenuWriteBody = Pick<
  ActionMenu,
  | "name"
  | "label"
  | "description"
  | "menuType"
  | "url"
  | "handler"
  | "parameters"
  | "properties"
  | "visibilityContexts"
  | "uiContexts"
>;

/** Child identity for PUT /services/actions/{idOrName}/children. */
export type ActionMenuChildrenWriteBody = Pick<ActionMenu, "name"> & {
  id?: number;
};

/** Jackson / JAXB root for ActionMenu (UNWRAP_ROOT_VALUE on POST/PUT). */
export const ACTION_MENU_ROOT = "ActionMenu";

/** REST default type on create ({@code PSAction.TYPE_MENUITEM}). */
export const ACTION_MENU_TYPE_ITEM = "MENUITEM";

/** Cascading parent type for UI-04 children PUT ({@code PSAction.TYPE_MENU}). */
export const ACTION_MENU_TYPE_MENU = "MENU";

/** Marker property on REST-created user menus ({@code RxmActionMenuConstants}). */
export const REST_USER_MENU_PROP = "sys_restUserMenu";

export const ACTION_MENU_TYPES = [
  "MENUITEM",
  "MENU",
  "CONTEXTMENU",
  "DYNAMICMENU",
] as const;

export type ActionMenuType = (typeof ACTION_MENU_TYPES)[number];

/** Workbench Usage handler ({@code PSAction.HANDLER_*}). */
export const ACTION_MENU_HANDLERS = ["CLIENT", "SERVER"] as const;

export type ActionMenuHandler = (typeof ACTION_MENU_HANDLERS)[number];

/** Workbench {@code PSAction.RefreshHint} values. */
export const ACTION_MENU_REFRESH_HINTS = ["none", "parent", "root", "selected"] as const;

/** Named command/usage properties written on PUT. */
export const ACTION_MENU_PROP = {
  ACCEL: "AcceleratorKey",
  MNEM: "MnemonicKey",
  SHORT_DESC: "ShortDescription",
  ICON: "SmallIcon",
  LAUNCH: "launchesWindow",
  MULTI: "SupportsMultiSelect",
  REFRESH: "refreshHint",
  TARGET: "target",
  TARGET_STYLE: "targetStyle",
} as const;

const KNOWN_PROP_NAMES = new Set<string>(Object.values(ACTION_MENU_PROP));

/**
 * Workbench Visibility context aliases accepted by REST
 * ({@code PSActionVisibilityContext.VIS_CONTEXT_*}).
 */
export const ACTION_MENU_VISIBILITY_ALIASES = [
  "assignmentType",
  "community",
  "contentType",
  "objectType",
  "clientContext",
  "checkoutStatus",
  "roles",
  "role",
  "locales",
  "locale",
  "workflows",
  "workflow",
  "publishable",
  "publishableType",
  "folderSecurity",
] as const;

/**
 * GET catalog/detail {@code visibilityContexts[].name} is the Workbench numeric
 * id ({@code 1}–{@code 11}). Map it to the alias the Visibility picker uses.
 */
export const ACTION_MENU_VISIBILITY_ID_ALIASES: Record<string, string> = {
  "1": "assignmentType",
  "2": "community",
  "3": "contentType",
  "4": "objectType",
  "5": "clientContext",
  "6": "checkoutStatus",
  "7": "roles",
  "8": "locales",
  "9": "workflows",
  "10": "publishable",
  "11": "folderSecurity",
};

/** REST GET numeric Workbench id or alias → picker alias. */
export function visibilityContextName(
  row: ActionMenuVisibilityContext | string | undefined | null,
): string {
  const raw = typeof row === "string" ? row : text(row?.name);
  return ACTION_MENU_VISIBILITY_ID_ALIASES[raw] ?? raw;
}

/**
 * Catalog-level design gaps. Create/save/delete, UI-03 usage/command/visibility,
 * and UI-04 cascading children write from this chrome.
 */
export const ACTION_MENU_DESIGN_GAPS: string[] = [];

const STALE_WRITE_GAP = /create\s*\/\s*update\s*\/\s*delete/i;
const STALE_UI03_GAP = /usage\s*\/\s*command\s*\/\s*visibility/i;
const STALE_VIS_GAP = /visibility context editing/i;
const STALE_CHILDREN_GAP = /cascading child/i;

function asArray<T>(payload: unknown): T[] {
  if (payload == null) return [];
  if (Array.isArray(payload)) return payload as T[];
  if (typeof payload === "object") {
    const obj = payload as Record<string, unknown>;
    const raw = obj.ActionMenu ?? obj.actionMenu ?? obj.ActionMenuList;
    if (raw == null) return [];
    return Array.isArray(raw) ? (raw as T[]) : [raw as T];
  }
  return [];
}

/** Unwrap a Jackson array or {@code {TypeName: …}} envelope. */
export function asNamedArray<T>(payload: unknown, typeName: string): T[] {
  if (payload == null) return [];
  if (Array.isArray(payload)) return payload as T[];
  if (typeof payload === "object") {
    const obj = payload as Record<string, unknown>;
    const camel = typeName.charAt(0).toLowerCase() + typeName.slice(1);
    const raw = obj[typeName] ?? obj[camel];
    if (raw == null) return [];
    return Array.isArray(raw) ? (raw as T[]) : [raw as T];
  }
  return [];
}

function text(value: string | undefined | null): string {
  return value == null ? "" : String(value);
}

/** Visibility context value from {@code value} or {@code values}. */
export function visibilityContextValue(row: ActionMenuVisibilityContext | undefined | null): string {
  if (row == null) return "";
  if (row.value != null && String(row.value).length > 0) return String(row.value);
  if (row.values != null) return String(row.values);
  return "";
}

export function propertyValue(
  properties: ActionMenuProperty[] | undefined | null,
  name: string,
): string {
  if (properties == null) return "";
  const hit = properties.find((p) => p.name === name);
  return hit?.value == null ? "" : String(hit.value);
}

export function extraActionMenuProperties(
  properties: ActionMenuProperty[] | undefined | null,
): ActionMenuProperty[] {
  if (properties == null) return [];
  return properties.filter((p) => p.name && !KNOWN_PROP_NAMES.has(p.name));
}

/** Merge dedicated usage/command fields onto leftover named properties. */
export function mergeActionMenuProperties(
  extra: ActionMenuProperty[],
  known: Record<string, string>,
): ActionMenuProperty[] {
  const out: ActionMenuProperty[] = [];
  for (const name of Object.values(ACTION_MENU_PROP)) {
    out.push({ name, value: text(known[name]) });
  }
  for (const p of extra) {
    if (!p.name) continue;
    out.push({ name: p.name, value: text(p.value), description: p.description });
  }
  return out;
}

export function normalizeActionMenuParameters(
  rows: ActionMenuParameter[] | undefined | null,
): ActionMenuParameter[] {
  return asNamedArray<ActionMenuParameter>(rows, "ActionMenuParameter").map((p) => ({
    name: text(p.name),
    value: text(p.value),
    description: text(p.description),
  }));
}

export function normalizeActionMenuProperties(
  rows: ActionMenuProperty[] | undefined | null,
): ActionMenuProperty[] {
  return asNamedArray<ActionMenuProperty>(rows, "ActionMenuProperty").map((p) => ({
    name: text(p.name),
    value: text(p.value),
    description: text(p.description),
    actionId: p.actionId,
  }));
}

export function normalizeVisibilityContexts(
  rows: ActionMenuVisibilityContext[] | undefined | null,
): ActionMenuVisibilityContext[] {
  return asNamedArray<ActionMenuVisibilityContext>(rows, "ActionMenuVisibilityContext").map(
    (row) => ({
      name: visibilityContextName(row),
      description: text(row.description),
      value: visibilityContextValue(row),
    }),
  );
}

export function normalizeUiContexts(
  rows: ActionMenuModeUIContext[] | undefined | null,
): ActionMenuModeUIContext[] {
  return asNamedArray<ActionMenuModeUIContext>(rows, "ActionMenuModeUIContext").map((row) => ({
    modeId: text(row.modeId),
    modeName: text(row.modeName),
    contextId: text(row.contextId),
    contextName: text(row.contextName),
    description: text(row.description),
  }));
}

/** JSON-stable compare for editor dirty detection. */
export function actionMenuRowsEqual(a: unknown, b: unknown): boolean {
  return JSON.stringify(a) === JSON.stringify(b);
}

/**
 * Unwrap Jackson WRAP_ROOT_VALUE {@code {"ActionMenu":{…}}} and fill
 * {@code guid.stringValue} / {@code guidString} (nested Guid, catalog, or
 * {@code 0-107-{id}}) so Object ACL can bind (#3380).
 */
export function unwrapActionMenu(payload: unknown): ActionMenu {
  if (payload == null || typeof payload !== "object" || Array.isArray(payload)) {
    return {};
  }
  const root = payload as Record<string, unknown>;
  const nested = root.ActionMenu ?? root.actionMenu;
  let body: ActionMenu;
  if (nested != null && typeof nested === "object" && !Array.isArray(nested)) {
    body = nested as ActionMenu;
  } else {
    body = root as ActionMenu;
  }
  const gs = resolveActionMenuObjectGuid(body);
  const normalized = normalizeDesignObjectGuid(body, gs);
  return {
    ...normalized,
    parameters: normalizeActionMenuParameters(normalized.parameters),
    properties: normalizeActionMenuProperties(normalized.properties),
    visibilityContexts: normalizeVisibilityContexts(normalized.visibilityContexts),
    uiContexts: normalizeUiContexts(normalized.uiContexts),
    children: unwrapActionMenuChildren(body.children),
  };
}

export function unwrapActionMenuChildren(children: unknown): ActionMenu[] {
  if (children == null) {
    return [];
  }
  if (Array.isArray(children)) {
    return children.map((item) => unwrapActionMenu(item));
  }
  if (typeof children === "object") {
    const env = children as Record<string, unknown>;
    const listed =
      env.ActionMenuList ?? env.actionMenuList ?? env.ActionMenu ?? env.actionMenu ?? env.children;
    if (listed != null && listed !== children) {
      return unwrapActionMenuChildren(listed);
    }
    if ("name" in env || "id" in env) {
      return [unwrapActionMenu(env)];
    }
  }
  return [];
}

/** Unwrap list envelopes and normalize each row GUID (#3380). */
export function unwrapActionMenuList(payload: unknown): ActionMenu[] {
  return asArray<ActionMenu>(payload).map((item) => unwrapActionMenu(item));
}

/** ASCII \\s plus Unicode separators (NBSP, ideographic space) and ZWSP. */
function containsWhitespace(value: string): boolean {
  return /[\s\p{Z}\u200B]/u.test(value);
}

/** True when the name is a safe REST action-menu key (no path chars). */
export function isSafeActionMenuName(name: string): boolean {
  if (!name) return false;
  return !name.includes("..") && !name.includes("/") && !name.includes("\\") && !name.includes("\0");
}

/** Trim an action-menu name for write. Empty / null becomes "". */
export function normalizeActionMenuName(name: string | undefined | null): string {
  return name == null ? "" : name.trim();
}

/**
 * True when the (trimmed) name is accepted by REST create
 * ({@code ActionMenuAdaptor.requireValidName}): no whitespace, wildcards, or
 * path characters.
 */
export function isValidActionMenuName(name: string | undefined | null): boolean {
  const key = normalizeActionMenuName(name);
  if (!key) return false;
  if (containsWhitespace(key)) return false;
  if (key.includes("*") || key.includes("%")) return false;
  return isSafeActionMenuName(key);
}

/** Save is enabled when the menu name is valid (create) or already loaded (edit). */
export function isActionMenuWriteReady(opts: { isNew: boolean; name: string }): boolean {
  if (opts.isNew) return isValidActionMenuName(opts.name);
  return Boolean(normalizeActionMenuName(opts.name));
}

/** Wire JSON for POST/PUT — a flat body fails JAXB root unwrap. */
export function wrapActionMenuForWire(
  body: ActionMenuWriteBody,
): Record<string, ActionMenuWriteBody> {
  const visibility = body.visibilityContexts;
  if (visibility == null) {
    return { [ACTION_MENU_ROOT]: body };
  }
  const filtered = visibility.filter((row) => {
    const name = (row?.name || "").trim();
    const value = visibilityContextValue(row).trim();
    return Boolean(name) && Boolean(value);
  });
  return { [ACTION_MENU_ROOT]: { ...body, visibilityContexts: filtered } };
}

/** Drop stale REST write-gap strings now that UI-02/UI-03/UI-04 ship. */
export function withoutStaleActionMenuWriteGap(gaps: string[] | undefined | null): string[] {
  if (gaps == null || gaps.length === 0) return [];
  return gaps.filter(
    (g) =>
      !STALE_WRITE_GAP.test(g) &&
      !STALE_UI03_GAP.test(g) &&
      !STALE_VIS_GAP.test(g) &&
      !STALE_CHILDREN_GAP.test(g),
  );
}

/** Wire JSON for PUT children — ActionMenuList envelope (array also accepted). */
export function wrapActionMenuChildrenForWire(
  children: ActionMenuChildrenWriteBody[],
): Record<string, ActionMenuChildrenWriteBody[]> {
  return { ActionMenuList: children };
}

function withGaps(menu: ActionMenu): ActionMenu {
  const fromServer = withoutStaleActionMenuWriteGap(menu.designGaps);
  return {
    ...menu,
    designGaps: fromServer.length > 0 ? fromServer : ACTION_MENU_DESIGN_GAPS,
  };
}

/** GET /services/actions/catalog */
export async function listActionMenus(): Promise<ActionMenu[]> {
  const payload = await get<unknown>(PATHS.ACTION_MENUS);
  return unwrapActionMenuList(payload).map(withGaps);
}

/** GET /services/actions/catalog/{idOrName} */
export async function getActionMenuDetail(idOrName: string): Promise<ActionMenu> {
  const key = encodeURIComponent(idOrName);
  const payload = await get<unknown>(`${PATHS.ACTION_MENUS}/${key}`);
  return withGaps(unwrapActionMenu(payload));
}

/** POST /services/actions — Admin. Name required. Duplicate is 409. Invalid is 400. */
export async function createActionMenu(body: ActionMenuWriteBody): Promise<ActionMenu> {
  const payload = await post<unknown>(PATHS.ACTION_MENUS_ROOT, wrapActionMenuForWire(body));
  return withGaps(unwrapActionMenu(payload));
}

/** PUT /services/actions/{idOrName} — Admin. Name is not renamed. Missing is 404. */
export async function saveActionMenu(
  idOrName: string,
  body: ActionMenuWriteBody,
): Promise<ActionMenu> {
  const payload = await put<unknown>(
    `${PATHS.ACTION_MENUS_ROOT}/${encodeURIComponent(idOrName)}`,
    wrapActionMenuForWire(body),
  );
  return withGaps(unwrapActionMenu(payload));
}

/** DELETE /services/actions/{idOrName} — Admin. 204 on success; missing is 404. */
export async function deleteActionMenu(idOrName: string): Promise<void> {
  await del(`${PATHS.ACTION_MENUS_ROOT}/${encodeURIComponent(idOrName)}`);
}

/**
 * PUT /services/actions/{idOrName}/children — Admin. Replaces ordered child
 * associations on a user cascading MENU. Empty array clears children. System
 * parent is 409; non-cascading parent / illegal graph is 400; missing parent
 * is 404.
 */
export async function saveActionMenuChildren(
  idOrName: string,
  children: ActionMenuChildrenWriteBody[],
): Promise<ActionMenu> {
  const payload = await put<unknown>(
    `${PATHS.ACTION_MENUS_ROOT}/${encodeURIComponent(idOrName)}/children`,
    wrapActionMenuChildrenForWire(children),
  );
  return withGaps(unwrapActionMenu(payload));
}
