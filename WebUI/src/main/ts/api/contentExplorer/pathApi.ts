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
 * Join a pathmanagement base URL with an {@link encodePath} suffix.
 *
 * <p>{@code base} must already be the resource root <em>without</em> a
 * trailing slash (e.g. {@code …/path/folder}); this helper appends exactly
 * one {@code /} then the encoded suffix. An empty suffix (CMS root {@code /})
 * yields {@code base/} so the server sees {@code folder/}, never
 * {@code folder//} or {@code folder//Sites}.</p>
 *
 * <p>Exported for unit tests that lock the double-slash regression.</p>
 */
export function joinPathUrl(base: string, path: string): string {
  const safe = encodePath(path);
  return safe ? `${base}/${safe}` : `${base}/`;
}

/**
 * List folder children (small folders). For large folders use
 * {@link paginatedFolder}.
 *
 * <p>Server response shape: {@code {"PathItem": [...]}} (the
 * {@code PSPathItemList} DTO is an {@code ArrayList<PSPathItem>} subclass;
 * JAX-RS serializes ArrayList subclasses as a JSON object with a key
 * matching the DTO's local element name — Evidence Over Invention: do not
 * invent alternate bare-array wire shapes).</p>
 */
export async function findChildren(path: string): Promise<PSPathItem[]> {
  const res = await get<PSPathItemListResponse>(
    joinPathUrl(PATHS.PATH_FOLDER, path),
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
    `${joinPathUrl(PATHS.PATH_PAGINATED_FOLDER, path)}?${q.toString()}`,
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
  const res = await get<PSPathItemResponse>(joinPathUrl(PATHS.PATH_ITEM, path));
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
  const res = await get<PSPathItemResponse>(
    `${joinPathUrl(PATHS.PATH_ADD_NEW_FOLDER, path)}?name=${encodeURIComponent(name)}`,
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
  await post<PSPathItemResponse>(joinPathUrl(PATHS.PATH_DELETE_ITEM, path));
}

/**
 * Jackson {@code @JsonRootName("FolderProperties")} for sitemanage
 * {@code PSFolderProperties}. {@code JacksonContextResolver} enables
 * WRAP_ROOT_VALUE / UNWRAP_ROOT_VALUE, so GET responses and POST bodies use
 * {@code { "FolderProperties": { ... } }} (same class of wire contract as
 * {@code UserPreference} / #2708 / #2749).
 */
export const FOLDER_PROPERTIES_ROOT = "FolderProperties";

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value != null && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

/**
 * Normalize a folderProperties GET response (or already-flat object) to a
 * client {@link PSFolderProperties}. Prefers the Jackson root wrap; accepts a
 * flat body for tests / legacy callers.
 *
 * @returns unwrapped props, or {@code null} when the payload has neither a
 *   FolderProperties root nor a flat {@code id} field.
 */
export function unwrapFolderProperties(
  data: unknown,
): PSFolderProperties | null {
  const root = asRecord(data);
  if (!root) {
    return null;
  }
  const nested = asRecord(root[FOLDER_PROPERTIES_ROOT]);
  if (nested && (typeof nested.id === "string" || typeof nested.name === "string")) {
    return nested as unknown as PSFolderProperties;
  }
  // Flat body (unit tests, already-unwrapped). Require an id so a bare
  // envelope mis-shape is not treated as success.
  if (typeof root.id === "string" || typeof root.name === "string") {
    return root as unknown as PSFolderProperties;
  }
  return null;
}

/**
 * Wrap flat folder properties for POST {@code saveFolderProperties}.
 * Server UNWRAP_ROOT_VALUE rejects a bare object without the
 * {@code FolderProperties} root (#2749 — null id / Validate.notNull).
 */
export function wrapFolderProperties(
  props: PSFolderProperties,
): Record<string, PSFolderProperties> {
  return { [FOLDER_PROPERTIES_ROOT]: props };
}

export async function folderProperties(
  id: string,
): Promise<PSFolderProperties> {
  if (id == null || String(id).trim().length === 0) {
    throw new Error("folderProperties requires a non-empty folder id");
  }
  const raw = await get<unknown>(
    `${PATHS.PATH_FOLDER_PROPERTIES}/${encodeURIComponent(id)}`,
  );
  const props = unwrapFolderProperties(raw);
  if (!props) {
    throw new Error(
      `folderProperties: response missing ${FOLDER_PROPERTIES_ROOT} (or flat id) for id=${id}`,
    );
  }
  return props;
}

export async function saveFolderProperties(
  props: PSFolderProperties,
): Promise<void> {
  if (props == null || props.id == null || String(props.id).trim().length === 0) {
    throw new Error("saveFolderProperties requires props.id");
  }
  // Ensure permission is present so server setFolderPermission does not
  // Validate.notNull on a null PSFolderPermission (#2749).
  const body: PSFolderProperties = {
    ...props,
    permission: props.permission ?? { accessLevel: "ADMIN" },
  };
  await post<void>(PATHS.PATH_SAVE_FOLDER_PROPERTIES, wrapFolderProperties(body));
}

export async function validatePath(path: string): Promise<string> {
  return get<string>(joinPathUrl(PATHS.PATH_VALIDATE, path));
}

export async function lastExisting(path: string): Promise<string> {
  return get<string>(joinPathUrl(PATHS.PATH_LAST_EXISTING, path));
}

/**
 * Encode each `/`-separated path segment with {@link encodeURIComponent} for
 * use as the JAX-RS `{path:.*}` suffix under `/pathmanagement/path/folder/…`.
 *
 * <p><strong>Leading/trailing slashes are stripped.</strong> Callers pass CMS
 * paths like {@code /Sites/} or {@code /} (from {@code PSPathItem.path}); the
 * server endpoint is mounted as {@code /folder/{path:.*}} and rejects a path
 * that starts with {@code /} (HTTP 400 "Invalid path") — so the wire form must
 * be {@code folder/Sites} or {@code folder/} (empty), never {@code folder//Sites}.
 */
export function encodePath(path: string): string {
  if (path === "") {
    return "";
  }
  return path
    .split("/")
    .filter((seg) => seg.length > 0)
    .map((seg) => encodeURIComponent(seg))
    .join("/");
}
