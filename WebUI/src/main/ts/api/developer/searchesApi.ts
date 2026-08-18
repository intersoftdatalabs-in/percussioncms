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
import { toRepositorySearchFolderPath } from "../../contentExplorer/folderPath";
import type {
  SearchDef,
  SearchExecuteRequest,
  SearchExecuteResult,
  SearchResultItem,
} from "./types";

/**
 * Jackson / JAXB root for {@link SearchExecuteRequest}
 * ({@code @XmlRootElement(name = "SearchExecuteRequest")}). CXF
 * {@code UNWRAP_ROOT_VALUE} rejects a bare {@code folderPath} field
 * (QA #2799 / #3438) — same envelope as {@code ViewExecuteRequest}.
 */
export const SEARCH_EXECUTE_REQUEST_ROOT = "SearchExecuteRequest";

/** Wire envelope required by WRAP_ROOT_VALUE / JAXB on search execute. */
export type SearchExecuteRequestEnvelope = {
  SearchExecuteRequest: SearchExecuteRequest;
};

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value != null && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

/**
 * Wrap execute overrides under {@link SEARCH_EXECUTE_REQUEST_ROOT}.
 * Does not double-wrap an already-enveloped payload. Folder paths are
 * normalized to repository form ({@code //Sites}) so a host that still
 * holds {@code /Sites} does not 400 on {@code getIdByPath}.
 */
export function wrapSearchExecuteRequest(
  request?: SearchExecuteRequest | SearchExecuteRequestEnvelope | null,
): SearchExecuteRequestEnvelope {
  const rec = asRecord(request);
  let inner: SearchExecuteRequest = {};
  if (rec != null) {
    const nested = rec[SEARCH_EXECUTE_REQUEST_ROOT];
    if (asRecord(nested) != null) {
      inner = nested as SearchExecuteRequest;
    } else {
      inner = request as SearchExecuteRequest;
    }
  }
  const folderPath = toRepositorySearchFolderPath(inner.folderPath);
  const rest: SearchExecuteRequest = { ...inner };
  delete rest.folderPath;
  return {
    SearchExecuteRequest: {
      ...rest,
      ...(folderPath !== undefined ? { folderPath } : {}),
    },
  };
}

/**
 * Catalog-level design gaps (REST-GAPS-02). Server omits these on list rows;
 * detail re-attaches or SPA falls back via this constant.
 */
export const SEARCH_DESIGN_GAPS: string[] = [
  "Search create / update / delete not supported via this API",
  "Search field criterion editing not supported via this API",
  "Views are a separate catalog (Developer Views / UI-07)",
];

const SEARCH_DEF_WRAP_KEYS = [
  "SearchDef",
  "searchDef",
  "SearchDefList",
  "searchDefList",
  "ArrayList",
  "arrayList",
  "items",
] as const;

const MAX_UNWRAP_DEPTH = 6;

function hasSearchDefIdentity(obj: Record<string, unknown>): boolean {
  const name = obj.name != null ? String(obj.name).trim() : "";
  if (name) {
    return true;
  }
  const id = obj.id != null ? String(obj.id).trim() : "";
  if (id && id !== "0") {
    return true;
  }
  const label = obj.label != null ? String(obj.label).trim() : "";
  return Boolean(label);
}

/**
 * Unwrap Jackson / JAXB / ArrayList wrappers for {@link SearchDef} lists.
 * Nested {@code SearchDefList.SearchDef} must not collapse to one empty row
 * (#3576).
 */
export function unwrapSearchDefList(payload: unknown, depth = 0): SearchDef[] {
  if (payload == null || depth > MAX_UNWRAP_DEPTH) {
    return [];
  }
  if (Array.isArray(payload)) {
    const out: SearchDef[] = [];
    for (const item of payload) {
      if (item == null || typeof item !== "object") {
        continue;
      }
      const rec = item as Record<string, unknown>;
      if (
        !hasSearchDefIdentity(rec) &&
        SEARCH_DEF_WRAP_KEYS.some((k) => rec[k] != null)
      ) {
        out.push(...unwrapSearchDefList(rec, depth + 1));
      } else {
        out.push(item as SearchDef);
      }
    }
    return out;
  }
  const obj = asRecord(payload);
  if (obj == null) {
    return [];
  }
  for (const key of SEARCH_DEF_WRAP_KEYS) {
    if (obj[key] != null) {
      return unwrapSearchDefList(obj[key], depth + 1);
    }
  }
  if (hasSearchDefIdentity(obj)) {
    return [obj as SearchDef];
  }
  return [];
}

function asArray<T>(payload: unknown): T[] {
  return unwrapSearchDefList(payload) as T[];
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

/**
 * GET /services/searches
 *
 * <p>Explorer saved-search picker must pass {@code includeViews: true} so the
 * default All view ({@code View_All}) is in the catalog. Developer Searches
 * stays searches-only (default).</p>
 */
export async function listSearches(options?: {
  includeViews?: boolean;
}): Promise<SearchDef[]> {
  const url =
    options?.includeViews === true
      ? `${PATHS.SEARCHES}?includeViews=true`
      : PATHS.SEARCHES;
  const payload = await get<unknown>(url);
  return asArray<SearchDef>(payload);
}

/** Explorer catalog: searches plus CX views (View_All / All). */
export function listExplorerSavedSearches(): Promise<SearchDef[]> {
  return listSearches({ includeViews: true });
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
  const payload = await post<unknown>(
    `${PATHS.SEARCHES}/${pathKey}/execute`,
    wrapSearchExecuteRequest(request),
  );
  return unwrapSearchExecuteResult(payload);
}
