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
import type { ActionMenu } from "./types";

export { resolveActionMenuObjectGuid };

/**
 * Writable identity fields for POST/PUT /services/actions. Name is the catalog
 * key (not renamed on PUT). Cascading children and visibility are not written
 * from this chrome (UI-04 / UI-03).
 */
export type ActionMenuWriteBody = Pick<
  ActionMenu,
  "name" | "label" | "description" | "menuType" | "url"
>;

/** Jackson / JAXB root for ActionMenu (UNWRAP_ROOT_VALUE on POST/PUT). */
export const ACTION_MENU_ROOT = "ActionMenu";

/** REST default type on create ({@code PSAction.TYPE_MENUITEM}). */
export const ACTION_MENU_TYPE_ITEM = "MENUITEM";

export const ACTION_MENU_TYPES = [
  "MENUITEM",
  "MENU",
  "CONTEXTMENU",
  "DYNAMICMENU",
] as const;

export type ActionMenuType = (typeof ACTION_MENU_TYPES)[number];

/**
 * Catalog-level design gaps. Create / save / delete are supported (UI-02 SPA).
 * Cascading children (UI-04) and usage/command/visibility (UI-03) remain later.
 */
export const ACTION_MENU_DESIGN_GAPS: string[] = [
  "Cascading child menu composition not supported via this API",
  "Visibility context editing not supported via this API",
];

const STALE_WRITE_GAP = /create\s*\/\s*update\s*\/\s*delete/i;

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
  return normalizeDesignObjectGuid(body, gs);
}

/** Unwrap list envelopes and normalize each row GUID (#3380). */
export function unwrapActionMenuList(payload: unknown): ActionMenu[] {
  return asArray<ActionMenu>(payload).map((item) => unwrapActionMenu(item));
}

function containsWhitespace(value: string): boolean {
  return /\s/.test(value);
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
  return { [ACTION_MENU_ROOT]: body };
}

/** Drop stale REST write-gap strings now that UI-02 create/delete ships. */
export function withoutStaleActionMenuWriteGap(gaps: string[] | undefined | null): string[] {
  if (gaps == null || gaps.length === 0) return [];
  return gaps.filter((g) => !STALE_WRITE_GAP.test(g));
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
  return unwrapActionMenuList(payload);
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
