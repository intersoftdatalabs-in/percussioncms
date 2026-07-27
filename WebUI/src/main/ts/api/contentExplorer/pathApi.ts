/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
 * Typed client for the sitemanage path management REST surface used by the
 * modern React Content Explorer (feature 992-react-content-explorer) and the
 * reusable Content Browser (US2).
 *
 * <p>Contracts: see {@code specs/992-react-content-explorer/contracts/path-api.md}.
 * Server provider: {@code projects/sitemanage} {@code PSPathService}.</p>
 *
 * <p>This module is intentionally thin: it maps the documented contract to a
 * typed TS surface and delegates transport to {@link get} / {@link post}
 * (CSRF + JSON + error normalization). It does <em>not</em> invent fields —
 * when a new server field is required, align types to the live
 * {@code PSPathItem} / {@code PSPagedItemList} / {@code PSFolderProperties}
 * DTOs per constitution II (Evidence Over Invention).</p>
 *
 * <p><strong>DTO wrapper handling (CRITICAL):</strong> the server's
 * {@code PSPathItemList} extends {@code ArrayList<PSPathItem>} and
 * {@code PSPagedItemList} has {@code @JsonRootName("PagedItemList")}, so
 * their wire format is:</p>
 * <ul>
 *   <li>{@code {"PathItem": [...]}} for list endpoints ({@code findChildren})</li>
 *   <li>{@code {"PagedItemList": {"childrenInPage": [...], "childrenCount", "startIndex", ...}}} for paginated endpoints ({@code paginatedFolder})</li>
 *   <li>{@code {"PathItem": {...}}} for single-item endpoints ({@code findItemByPath}, {@code findItemById}, {@code addNewFolder}, {@code renameFolder})</li>
 * </ul>
 * <p>The functions below unwrap these JSON envelopes so callers receive the
 * typed TS surface. The wrapper keys are derived from the server DTOs
 * ({@code @JsonRootName} / {@code @XmlRootElement}); do not invent field
 * names. Per constitution II (Evidence Over Invention), align with the live
 * DTOs in {@code projects/sitemanage/src/main/java/}.</p>
 */

import { get, post } from "../client";
import { PATHS } from "../paths";
import type {
  PSPathItem,
  PSPagedResult,
  PSFolderProperties,
  PSRenameFolderItem,
  PSMoveFolderItem,
} from "./types";

export interface PaginatedFolderParams {
  startIndex: number;
  maxResults: number;
  sortColumn?: string;
  sortOrder?: "asc" | "desc";
  child?: boolean;
  displayFormatId?: string;
  category?: string;
  type?: string;
}

/** Wrapper shapes for endpoints that return DTOs. These are internal —
 * the typed API functions below unwrap and return the inner type. */
interface PSPathItemListResponse {
  PathItem: PSPathItem[];
}
interface PSPagedItemListResponse {
  PagedItemList: import("./types").PSPagedItemList;
}
interface PSPathItemResponse {
  PathItem: PSPathItem;
}

/**
 * List folder children (small folders). For large folders use
 * {@link paginatedFolder}.
 *
 * <p>Server response shape: {@code {"PathItem": [...]}} (the
 * {@code PSPathItemList} DTO is an {@code ArrayList<PSPathItem>} subclass;
 * JAX-RS serializes ArrayList subclasses as a JSON object with a key
 * matching the DTO's local element name).</p>
 */
export async function findChildren(path: string): Promise<PSPathItem[]> {
  const safe = encodePath(path);
  const res = await get<PSPathItemListResponse>(
    `${PATHS.PATH_FOLDER}/${safe}`,
  );
  return res?.PathItem ?? [];
}

/**
 * Paged children loader — required for SC-005 large-folder performance gate.
 * Server-side pagination avoids loading the full child set; the client
 * applies virtualization on top (see plan.md Performance Goals).
 *
 * <p>Server response shape: {@code {"PagedItemList": {childrenInPage: [...],
 * childrenCount, startIndex, firstItemId, ...}}} — {@code PSPagedItemList}
 * has {@code @JsonRootName("PagedItemList")} and exposes the children
 * array as {@code childrenInPage}.</p>
 */
export async function paginatedFolder(
  path: string,
  params: PaginatedFolderParams,
): Promise<PSPagedResult> {
  const safe = encodePath(path);
  const q = new URLSearchParams();
  q.set("startIndex", String(params.startIndex));
  q.set("maxResults", String(params.maxResults));
  if (params.sortColumn) q.set("sortColumn", params.sortColumn);
  if (params.sortOrder) q.set("sortOrder", params.sortOrder);
  if (params.child !== undefined) q.set("child", String(params.child));
  if (params.displayFormatId) q.set("displayFormatId", params.displayFormatId);
  if (params.category) q.set("category", params.category);
  if (params.type) q.set("type", params.type);
  const res = await get<PSPagedItemListResponse>(
    `${PATHS.PATH_PAGINATED_FOLDER}/${safe}?${q.toString()}`,
  );
  // Normalize wire shape (childrenInPage + childrenCount + startIndex
  // under "PagedItemList") into the client-facing shape (children +
  // totalCount + startIndex). See PSPagedItemList for the server DTO.
  const paged = res?.PagedItemList;
  if (!paged) {
    return { children: [], totalCount: 0, startIndex: params.startIndex };
  }
  return {
    children: paged.childrenInPage ?? [],
    totalCount: paged.childrenCount ?? undefined,
    startIndex: paged.startIndex ?? params.startIndex,
  };
}

export async function findItemByPath(path: string): Promise<PSPathItem> {
  const safe = encodePath(path);
  const res = await get<PSPathItemResponse>(`${PATHS.PATH_ITEM}/${safe}`);
  return res?.PathItem ?? ({} as PSPathItem);
}

export async function findItemById(id: string): Promise<PSPathItem> {
  const res = await get<PSPathItemResponse>(
    `${PATHS.PATH_ITEM_ID}/${encodeURIComponent(id)}`,
  );
  return res?.PathItem ?? ({} as PSPathItem);
}

export async function addNewFolder(
  path: string,
  name: string,
): Promise<PSPathItem> {
  const safe = encodePath(path);
  const res = await get<PSPathItemResponse>(
    `${PATHS.PATH_ADD_NEW_FOLDER}/${safe}?name=${encodeURIComponent(name)}`,
  );
  return res?.PathItem ?? ({} as PSPathItem);
}

export async function renameFolder(
  body: PSRenameFolderItem,
): Promise<PSPathItem> {
  const res = await post<PSPathItemResponse>(PATHS.PATH_RENAME_FOLDER, body);
  return res?.PathItem ?? ({} as PSPathItem);
}

export async function moveItem(body: PSMoveFolderItem): Promise<void> {
  await post<PSPathItemResponse>(PATHS.PATH_MOVE_ITEM, body);
}

export async function deleteItem(path: string): Promise<void> {
  const safe = encodePath(path);
  await post<PSPathItemResponse>(`${PATHS.PATH_DELETE_ITEM}/${safe}`);
}

export async function folderProperties(
  id: string,
): Promise<PSFolderProperties> {
  return get<PSFolderProperties>(
    `${PATHS.PATH_FOLDER_PROPERTIES}/${encodeURIComponent(id)}`,
  );
}

export async function saveFolderProperties(
  props: PSFolderProperties,
): Promise<void> {
  await post<void>(PATHS.PATH_SAVE_FOLDER_PROPERTIES, props);
}

export async function validatePath(path: string): Promise<string> {
  const safe = encodePath(path);
  return get<string>(`${PATHS.PATH_VALIDATE}/${safe}`);
}

export async function lastExisting(path: string): Promise<string> {
  const safe = encodePath(path);
  return get<string>(`${PATHS.PATH_LAST_EXISTING}/${safe}`);
}

/**
 * Encode each `/`-separated path segment with {@link encodeURIComponent}.
 * The `/` separator is preserved by joining — server JAX-RS expects the
 * multi-segment `{path:.*}` pattern to remain readable.
 */
export function encodePath(path: string): string {
  return path
    .split("/")
    .map((seg) => encodeURIComponent(seg))
    .join("/");
}