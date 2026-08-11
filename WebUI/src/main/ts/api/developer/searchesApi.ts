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

import { get, post } from "../client";
import { PATHS } from "../paths";
import type {
  SearchDef,
  SearchExecuteRequest,
  SearchExecuteResult,
  SearchResultItem,
} from "./types";

/**
 * Catalog-level design gaps (REST-GAPS-02). Server omits these on list rows;
 * detail re-attaches or SPA falls back via this constant.
 */
export const SEARCH_DESIGN_GAPS: string[] = [
  "Search create / update / delete not supported via this API",
  "Search field criterion editing not supported via this API",
  "Views are a separate catalog (Developer Views / UI-07)",
];

function asArray<T>(payload: unknown): T[] {
  if (payload == null) return [];
  if (Array.isArray(payload)) return payload as T[];
  if (typeof payload === "object") {
    const obj = payload as Record<string, unknown>;
    const raw = obj.SearchDef ?? obj.searchDef ?? obj.SearchDefList;
    if (raw == null) return [];
    return Array.isArray(raw) ? (raw as T[]) : [raw as T];
  }
  return [];
}

function withGaps(s: SearchDef): SearchDef {
  return {
    ...s,
    designGaps:
      s.designGaps && s.designGaps.length > 0 ? s.designGaps : [...SEARCH_DESIGN_GAPS],
  };
}

/**
 * Unwrap Jackson root-name wrapping for {@link SearchExecuteResult}
 * ({@code SearchExecuteResult} / camelCase aliases) while accepting a flat
 * payload when root wrapping is off.
 */
export function unwrapSearchExecuteResult(payload: unknown): SearchExecuteResult {
  if (payload == null || typeof payload !== "object") {
    return { children: [], totalCount: 0, startIndex: 1 };
  }
  const root = payload as Record<string, unknown>;
  const nested =
    root.SearchExecuteResult ??
    root.searchExecuteResult ??
    (Array.isArray(root.children) ||
    typeof root.totalCount === "number" ||
    typeof root.startIndex === "number"
      ? root
      : null);
  if (nested == null || typeof nested !== "object") {
    return { children: [], totalCount: 0, startIndex: 1 };
  }
  const body = nested as SearchExecuteResult;
  const children = Array.isArray(body.children)
    ? (body.children as SearchResultItem[])
    : [];
  return {
    children,
    totalCount: typeof body.totalCount === "number" ? body.totalCount : children.length,
    startIndex: typeof body.startIndex === "number" ? body.startIndex : 1,
    searchName: body.searchName,
    displayFormatId: body.displayFormatId,
  };
}

/** GET /services/searches */
export async function listSearches(): Promise<SearchDef[]> {
  const payload = await get<unknown>(PATHS.SEARCHES);
  return asArray<SearchDef>(payload);
}

/** GET /services/searches/{idOrName} */
export async function getSearchDetail(idOrName: string): Promise<SearchDef> {
  const key = encodeURIComponent(idOrName);
  const detail = await get<SearchDef>(`${PATHS.SEARCHES}/${key}`);
  return withGaps(detail);
}

/**
 * POST /services/searches/{idOrName}/execute — run a CX design search with
 * optional folder / paging / sort overrides (slice B façade).
 *
 * <p>Empty or blank {@code idOrName} rejects client-side before the network
 * call so the picker cannot fire a meaningless path segment.</p>
 */
export async function executeSearch(
  idOrName: string,
  request?: SearchExecuteRequest | null,
): Promise<SearchExecuteResult> {
  const key = (idOrName ?? "").trim();
  if (!key) {
    throw new Error("Search id or name is required");
  }
  const pathKey = encodeURIComponent(key);
  const body = request ?? {};
  const payload = await post<unknown>(
    `${PATHS.SEARCHES}/${pathKey}/execute`,
    body,
  );
  return unwrapSearchExecuteResult(payload);
}
