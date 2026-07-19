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
 */

import { get, post } from "../client";
import { PATHS } from "../paths";
import type {
  PSPathItem,
  PSPagedItemList,
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

/**
 * List folder children (small folders). For large folders use
 * {@link paginatedFolder}.
 */
export function findChildren(path: string): Promise<PSPathItem[]> {
  const safe = encodePath(path);
  return get<PSPathItem[]>(`${PATHS.PATH_FOLDER}/${safe}`);
}

/**
 * Paged children loader — required for SC-005 large-folder performance gate.
 * Server-side pagination avoids loading the full child set; the client
 * applies virtualization on top (see plan.md Performance Goals).
 */
export function paginatedFolder(
  path: string,
  params: PaginatedFolderParams,
): Promise<PSPagedItemList> {
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
  return get<PSPagedItemList>(
    `${PATHS.PATH_PAGINATED_FOLDER}/${safe}?${q.toString()}`,
  );
}

export function findItemByPath(path: string): Promise<PSPathItem> {
  const safe = encodePath(path);
  return get<PSPathItem>(`${PATHS.PATH_ITEM}/${safe}`);
}

export function findItemById(id: string): Promise<PSPathItem> {
  return get<PSPathItem>(`${PATHS.PATH_ITEM_ID}/${encodeURIComponent(id)}`);
}

export function addNewFolder(path: string, name: string): Promise<PSPathItem> {
  const safe = encodePath(path);
  return get<PSPathItem>(
    `${PATHS.PATH_ADD_NEW_FOLDER}/${safe}?name=${encodeURIComponent(name)}`,
  );
}

export function renameFolder(body: PSRenameFolderItem): Promise<PSPathItem> {
  return post<PSPathItem>(PATHS.PATH_RENAME_FOLDER, body);
}

export function moveItem(body: PSMoveFolderItem): Promise<void> {
  return post<void>(PATHS.PATH_MOVE_ITEM, body);
}

export function deleteItem(path: string): Promise<void> {
  const safe = encodePath(path);
  return post<void>(`${PATHS.PATH_DELETE_ITEM}/${safe}`);
}

export function folderProperties(id: string): Promise<PSFolderProperties> {
  return get<PSFolderProperties>(
    `${PATHS.PATH_FOLDER_PROPERTIES}/${encodeURIComponent(id)}`,
  );
}

export function saveFolderProperties(
  props: PSFolderProperties,
): Promise<void> {
  return post<void>(PATHS.PATH_SAVE_FOLDER_PROPERTIES, props);
}

export function validatePath(path: string): Promise<string> {
  const safe = encodePath(path);
  return get<string>(`${PATHS.PATH_VALIDATE}/${safe}`);
}

export function lastExisting(path: string): Promise<string> {
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