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

import { del, get, post, put } from "../client";
import { normalizeDesignObjectGuid, resolveViewObjectGuid } from "../displayFormatGuid";
import { PATHS } from "../paths";
import type { ViewDef } from "./types";

export { resolveViewObjectGuid };

/**
 * Writable identity fields for POST/PUT /services/views. Name is the catalog
 * key (not renamed on PUT). Field criteria are not written from this chrome.
 */
export type ViewWriteBody = Pick<
  ViewDef,
  "name" | "label" | "description" | "type" | "displayFormatId"
>;

/** Jackson / JAXB root for ViewDef (UNWRAP_ROOT_VALUE on POST/PUT). */
export const VIEW_DEF_ROOT = "ViewDef";

/** REST default type on create ({@code PSSearch.TYPE_VIEW}). */
export const VIEW_TYPE_STANDARD = "View";

/** {@code PSSearch.INTERNALNAME_LENGTH} — create name max. */
export const VIEW_NAME_MAX = 128;

/** Seed / DCE internal name for the operator Inbox view. */
export const INBOX_VIEW_NAME = "Inbox";

/** DCE path form operators may use as a catalog key. */
export const INBOX_DCE_PATH = "//Views//MyContent/Inbox";

/**
 * Catalog-level design gaps (REST-GAPS-02). Server omits these on list rows;
 * detail re-attaches or SPA falls back via this constant.
 *
 * <p>Create / save / delete are supported (UI-07 SPA). Field-criterion write
 * and Inbox-family mutate remain later / REST-protected.</p>
 */
export const VIEW_DESIGN_GAPS: string[] = [
  "View field criterion editing not supported via this API",
  "Inbox-family and custom URL views cannot be updated or deleted via this API",
  "Searches are a separate catalog (Developer Searches / UI-06)",
];

function asArray<T>(payload: unknown): T[] {
  if (payload == null) return [];
  if (Array.isArray(payload)) return payload as T[];
  if (typeof payload === "object") {
    const obj = payload as Record<string, unknown>;
    const raw = obj.ViewDef ?? obj.viewDef ?? obj.ViewDefList;
    if (raw == null) return [];
    return Array.isArray(raw) ? (raw as T[]) : [raw as T];
  }
  return [];
}

function containsWhitespace(value: string): boolean {
  return /\s/.test(value);
}

/** True when the name is a safe REST view key (no path chars). */
export function isSafeViewName(name: string): boolean {
  if (!name) return false;
  return !name.includes("..") && !name.includes("/") && !name.includes("\\") && !name.includes("\0");
}

/** Trim a view name for write. Empty / null becomes "". */
export function normalizeViewName(name: string | undefined | null): string {
  return name == null ? "" : name.trim();
}

/**
 * True when the (trimmed) name is accepted by REST create
 * ({@code ViewAdaptor.requireValidName}): no whitespace, wildcards, or path
 * characters; max {@link VIEW_NAME_MAX}.
 */
export function isValidViewName(name: string | undefined | null): boolean {
  const key = normalizeViewName(name);
  if (!key) return false;
  if (containsWhitespace(key)) return false;
  if (key.includes("*") || key.includes("%")) return false;
  if (key.length > VIEW_NAME_MAX) return false;
  return isSafeViewName(key);
}

/** Save is enabled when the view name is valid (create) or already loaded (edit). */
export function isViewWriteReady(opts: { isNew: boolean; name: string }): boolean {
  if (opts.isNew) return isValidViewName(opts.name);
  return Boolean(normalizeViewName(opts.name));
}

/**
 * True when REST will 409 on PUT/DELETE (Inbox family or custom URL).
 * Catalog chrome must not delete these rows.
 */
export function isInboxViewName(name: string | undefined | null): boolean {
  const n = normalizeViewName(name).replace(/\\/g, "/");
  if (!n) return false;
  return n.toLowerCase() === INBOX_VIEW_NAME.toLowerCase() || n.toLowerCase() === INBOX_DCE_PATH.toLowerCase();
}

/** Inbox-family / custom-URL / system views are not deleted from this catalog. */
export function isProtectedViewWrite(
  view: Pick<ViewDef, "name" | "customView" | "url"> | null | undefined,
): boolean {
  if (view == null) return false;
  if (view.customView) return true;
  if (view.url != null && String(view.url).trim()) return true;
  return isInboxViewName(view.name);
}

/** Wire JSON for POST/PUT — a flat body fails JAXB root unwrap. */
export function wrapViewDefForWire(body: ViewWriteBody): Record<string, ViewWriteBody> {
  return { [VIEW_DEF_ROOT]: body };
}

/**
 * Unwrap Jackson WRAP_ROOT_VALUE {@code {"ViewDef":{…}}} and fill
 * {@code guid.stringValue} / {@code guidString} (nested Guid, catalog, or
 * {@code 0-18-{id}}) so Object ACL can bind (#3380).
 */
export function unwrapViewDef(payload: unknown): ViewDef {
  if (payload == null || typeof payload !== "object" || Array.isArray(payload)) {
    return {};
  }
  const root = payload as Record<string, unknown>;
  const nested = root.ViewDef ?? root.viewDef;
  let body: ViewDef;
  if (nested != null && typeof nested === "object" && !Array.isArray(nested)) {
    body = nested as ViewDef;
  } else {
    body = root as ViewDef;
  }
  const gs = resolveViewObjectGuid(body);
  return normalizeDesignObjectGuid(body, gs);
}

/** Unwrap list envelopes and normalize each row GUID (#3380). */
export function unwrapViewDefList(payload: unknown): ViewDef[] {
  return asArray<ViewDef>(payload).map((item) => unwrapViewDef(item));
}

const STALE_WRITE_GAP = /create\s*\/\s*update\s*\/\s*delete/i;

/** Drop the pre-UI-07 write gap when REST still attaches it on GET detail. */
export function withoutStaleViewWriteGap(gaps: string[] | undefined | null): string[] {
  const incoming = gaps && gaps.length > 0 ? gaps : [...VIEW_DESIGN_GAPS];
  const filtered = incoming.filter((g) => !STALE_WRITE_GAP.test(g));
  return filtered.length > 0 ? filtered : [...VIEW_DESIGN_GAPS];
}

function withGaps(v: ViewDef): ViewDef {
  return {
    ...v,
    designGaps: withoutStaleViewWriteGap(v.designGaps),
  };
}

/** GET /services/views — list omits designGaps on the wire (REST-GAPS-02). */
export async function listViews(): Promise<ViewDef[]> {
  const payload = await get<unknown>(PATHS.VIEWS);
  return unwrapViewDefList(payload);
}

/** GET /services/views/{idOrName} */
export async function getViewDetail(idOrName: string): Promise<ViewDef> {
  const key = encodeURIComponent(idOrName);
  const payload = await get<unknown>(`${PATHS.VIEWS}/${key}`);
  return withGaps(unwrapViewDef(payload));
}

/** POST /services/views — Admin. Name required. Duplicate is 409. Invalid is 400. */
export async function createView(body: ViewWriteBody): Promise<ViewDef> {
  const payload = await post<unknown>(PATHS.VIEWS, wrapViewDefForWire(body));
  return withGaps(unwrapViewDef(payload));
}

/** PUT /services/views/{idOrName} — Admin. Name is not renamed. Missing is 404. */
export async function saveView(idOrName: string, body: ViewWriteBody): Promise<ViewDef> {
  const payload = await put<unknown>(
    `${PATHS.VIEWS}/${encodeURIComponent(idOrName)}`,
    wrapViewDefForWire(body),
  );
  return withGaps(unwrapViewDef(payload));
}

/** DELETE /services/views/{idOrName} — Admin. 204 on success; missing is 404. */
export async function deleteView(idOrName: string): Promise<void> {
  await del(`${PATHS.VIEWS}/${encodeURIComponent(idOrName)}`);
}
