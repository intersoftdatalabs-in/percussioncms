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
import { PATHS } from "../paths";
import type { ItemFilter, ItemFilterRule, ItemFilterRuleParam } from "./types";

/**
 * Coerce wire {@code rules} to an array. Jackson/JAXB often emits a single
 * rule as an object (not a one-element array) for Set-backed fields.
 */
export function coerceRules(rules: unknown): ItemFilterRule[] {
  if (rules == null) {
    return [];
  }
  if (Array.isArray(rules)) {
    return rules as ItemFilterRule[];
  }
  if (typeof rules === "object") {
    return [rules as ItemFilterRule];
  }
  return [];
}

function coerceParams(params: unknown): ItemFilterRuleParam[] {
  if (params == null) {
    return [];
  }
  if (Array.isArray(params)) {
    return params as ItemFilterRuleParam[];
  }
  if (typeof params === "object") {
    return [params as ItemFilterRuleParam];
  }
  return [];
}

/**
 * Writable fields for POST/PUT /services/itemfilters. Name is the catalog key
 * (not renamed on PUT). {@code rules[]} may be sent on create/update; omit to
 * leave stored rules unchanged; send {@code []} to clear.
 */
export type ItemFilterWriteBody = Pick<
  ItemFilter,
  "name" | "description" | "legacyAuthtype" | "rules" | "clearRules" | "parentFilter"
>;

/** Jackson / JAXB root for ItemFilter (UNWRAP_ROOT_VALUE on POST/PUT). */
export const ITEM_FILTER_ROOT = "ItemFilter";

function asArray<T>(payload: unknown): T[] {
  if (payload == null) return [];
  if (Array.isArray(payload)) return payload as T[];
  if (typeof payload === "object") {
    const obj = payload as Record<string, unknown>;
    const raw = obj.ItemFilter ?? obj.itemFilter;
    if (raw == null) return [];
    return Array.isArray(raw) ? (raw as T[]) : [raw as T];
  }
  return [];
}

function containsWhitespace(value: string): boolean {
  return /\s/.test(value);
}

/** True when the name is a safe REST item-filter key (no spaces or path chars). */
export function isSafeFilterName(name: string): boolean {
  if (!name) return false;
  return !name.includes("..") && !name.includes("/") && !name.includes("\\") && !name.includes("\0");
}

/** Trim a filter name for write. Empty / null becomes "". */
export function normalizeFilterName(name: string | undefined | null): string {
  return name == null ? "" : name.trim();
}

/**
 * True when the (trimmed) name is accepted by REST create
 * ({@code ItemFilterAdaptor.requireValidName}).
 */
export function isValidFilterName(name: string | undefined | null): boolean {
  const key = normalizeFilterName(name);
  if (!key) return false;
  if (containsWhitespace(key)) return false;
  if (key.includes("*") || key.includes("%")) return false;
  return isSafeFilterName(key);
}

/** Save is enabled when the filter name is valid (create) or already loaded (edit). */
export function isItemFilterWriteReady(opts: { isNew: boolean; name: string }): boolean {
  if (opts.isNew) return isValidFilterName(opts.name);
  return Boolean(normalizeFilterName(opts.name));
}

/** Wire JSON for POST/PUT — a flat body fails JAXB root unwrap. */
export function wrapItemFilterForWire(
  body: ItemFilterWriteBody,
): Record<string, ItemFilterWriteBody> {
  return { [ITEM_FILTER_ROOT]: body };
}

/** Normalize ItemFilter so {@code rules} / nested params are always arrays. */
export function normalizeItemFilter(filter: ItemFilter): ItemFilter {
  const rules = coerceRules(filter.rules).map((r) => ({
    ...r,
    params: coerceParams(r.params),
  }));
  let parentFilter = filter.parentFilter;
  if (parentFilter != null && typeof parentFilter === "object") {
    parentFilter = normalizeItemFilter(parentFilter);
  }
  return { ...filter, rules, parentFilter };
}

/** Unwrap GET/POST/PUT payload that may be wrapped as { ItemFilter: {...} }. */
export function unwrapItemFilter(payload: unknown): ItemFilter {
  if (payload == null || typeof payload !== "object" || Array.isArray(payload)) {
    return {};
  }
  const obj = payload as Record<string, unknown>;
  const raw = obj.ItemFilter ?? obj.itemFilter;
  if (raw != null && typeof raw === "object" && !Array.isArray(raw)) {
    return normalizeItemFilter(raw as ItemFilter);
  }
  return normalizeItemFilter(obj as ItemFilter);
}

/** GET /services/itemfilters */
export async function listItemFilters(): Promise<ItemFilter[]> {
  const payload = await get<unknown>(PATHS.ITEM_FILTERS);
  return asArray<ItemFilter>(payload).map(normalizeItemFilter);
}

/** GET /services/itemfilters/{idOrName} */
export async function getItemFilterDetail(idOrName: string): Promise<ItemFilter> {
  const key = encodeURIComponent(idOrName);
  const payload = await get<unknown>(`${PATHS.ITEM_FILTERS}/${key}`);
  return unwrapItemFilter(payload);
}

/** POST /services/itemfilters — Admin. Name required. Duplicate is 409. Invalid is 400. */
export async function createItemFilter(body: ItemFilterWriteBody): Promise<ItemFilter> {
  const payload = await post<unknown>(PATHS.ITEM_FILTERS, wrapItemFilterForWire(body));
  return unwrapItemFilter(payload);
}

/** PUT /services/itemfilters/{idOrName} — Admin. Name is not renamed. Missing is 404. */
export async function updateItemFilter(
  idOrName: string,
  body: ItemFilterWriteBody,
): Promise<ItemFilter> {
  const payload = await put<unknown>(
    `${PATHS.ITEM_FILTERS}/${encodeURIComponent(idOrName)}`,
    wrapItemFilterForWire(body),
  );
  return unwrapItemFilter(payload);
}

/** DELETE /services/itemfilters/{idOrName} — Admin. 204 on success; missing is 404. */
export async function deleteItemFilter(idOrName: string): Promise<void> {
  await del(`${PATHS.ITEM_FILTERS}/${encodeURIComponent(idOrName)}`);
}
