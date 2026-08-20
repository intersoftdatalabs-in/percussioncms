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

import { get, post, type ApiError } from "../client";
import { PATHS } from "../paths";
import { bindExplorerPathItemId } from "./pathItemId";
import type {
  PSPathItem,
  PSPagedResult,
  PSFolderPermission,
  PSFolderProperties,
  PSPrincipal,
  PSRenameFolderItem,
  PSMoveFolderItem,
  PSCopyRequest,
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
 * Pathmanagement {@code displayFormatId} is JAX-RS {@code Integer}.
 * Names (e.g. {@code FolderList}) and {@code 0} are not valid ids.
 */
export function isNumericDisplayFormatId(
  id: string | null | undefined,
): boolean {
  if (id == null) {
    return false;
  }
  return /^[1-9]\d*$/.test(String(id).trim());
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
function isPathItemErrorEnvelope(item: unknown): boolean {
  if (item == null || typeof item !== "object") {
    return false;
  }
  const rec = item as Record<string, unknown>;
  const errors = rec.Errors ?? rec.errors;
  const hasPath = typeof rec.path === "string" && rec.path.length > 0;
  const hasName = typeof rec.name === "string" && rec.name.length > 0;
  return !!errors && !hasPath && !hasName;
}

/**
 * Treat a {@code PathItem} payload that is actually a PSErrors envelope as a
 * failure so the Explorer tree shows an error instead of an empty panel (#3196).
 */
export function unwrapPathItemList(res: PSPathItemListResponse | null | undefined): PSPathItem[] {
  const items = res?.PathItem;
  if (!items) {
    return [];
  }
  const list = Array.isArray(items) ? items : [items];
  if (list.length > 0 && list.every(isPathItemErrorEnvelope)) {
    const err: ApiError = {
      status: 500,
      statusText: "Internal Server Error",
      body: res,
    };
    throw err;
  }
  return list.map(bindExplorerPathItemId);
}

export async function findChildren(path: string): Promise<PSPathItem[]> {
  const res = await get<PSPathItemListResponse>(
    joinPathUrl(PATHS.PATH_FOLDER, path),
  );
  return unwrapPathItemList(res);
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
  if (isNumericDisplayFormatId(params.displayFormatId)) {
    q.set("displayFormatId", String(params.displayFormatId).trim());
  }
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
    children: (paged.childrenInPage ?? []).map(bindExplorerPathItemId),
    totalCount: paged.childrenCount ?? undefined,
    startIndex: paged.startIndex ?? params.startIndex,
  };
}

/**
 * Whether {@code path} has a non-empty pathmanagement {@code /path/item/{path}}
 * suffix. CMS root {@code /} (and blank) encode to {@code …/path/item/} which
 * the server rejects as HTTP 400 "Invalid path" (#3468 / #3458).
 */
export function isPathItemLookupPath(
  path: string | null | undefined,
): boolean {
  if (path == null) {
    return false;
  }
  return encodePath(String(path)) !== "";
}

export async function findItemByPath(path: string): Promise<PSPathItem> {
  // Every caller (security folder-id, editor host, Refresh) must skip root —
  // defaultResolveFolderId alone still left live H2 probing path/item/ (#3468).
  if (!isPathItemLookupPath(path)) {
    return {} as PSPathItem;
  }
  const res = await get<PSPathItemResponse>(joinPathUrl(PATHS.PATH_ITEM, path));
  const item = res?.PathItem ?? ({} as PSPathItem);
  return bindExplorerPathItemId(item);
}

export async function findItemById(id: string): Promise<PSPathItem> {
  const res = await get<PSPathItemResponse>(
    `${PATHS.PATH_ITEM_ID}/${encodeURIComponent(id)}`,
  );
  const item = res?.PathItem ?? ({} as PSPathItem);
  return bindExplorerPathItemId(item);
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

/**
 * Jackson / JAXB root for sitemanage {@code PSRenameFolderItem}
 * ({@code @XmlRootElement(name = "RenameFolderItem")}). The Java field is
 * {@code name}, not the SPA alias {@code newName}.
 */
export const RENAME_FOLDER_ITEM_ROOT = "RenameFolderItem";

function withFolderTrailingSlash(path: string): string {
  const p = String(path || "").trim();
  if (!p || p === "/") {
    return p;
  }
  return p.endsWith("/") ? p : `${p}/`;
}

export async function renameFolder(
  body: PSRenameFolderItem,
): Promise<PSPathItem> {
  const path = withFolderTrailingSlash(String(body.path ?? "").trim());
  const name = String(body.newName ?? "").trim();
  const res = await post<PSPathItemResponse>(PATHS.PATH_RENAME_FOLDER, {
    [RENAME_FOLDER_ITEM_ROOT]: { path, name },
  });
  return res?.PathItem ?? ({} as PSPathItem);
}

/**
 * Jackson / JAXB root for sitemanage {@code PSMoveFolderItem}
 * ({@code @XmlRootElement(name = "MoveFolderItem")}). CXF JAXB rejects a
 * bare {@code sourcePath} field (#3362).
 */
export const MOVE_FOLDER_ITEM_ROOT = "MoveFolderItem";

/** Wire envelope required by WRAP_ROOT_VALUE / JAXB on moveItem. */
export type MoveFolderItemEnvelope = {
  MoveFolderItem: { itemPath: string; targetFolderPath: string };
};

/**
 * Jackson / JAXB root for rest {@code CopyFolderItemRequest}. Public copy
 * lives on {@code POST /rest/folders/copy/folder} — not moveItem.
 */
export const COPY_FOLDER_ITEM_REQUEST_ROOT = "CopyFolderItemRequest";

export type CopyFolderItemRequestEnvelope = {
  CopyFolderItemRequest: { itemPath: string; targetFolderPath: string };
};

function resolveFolderItemPaths(
  body: PSMoveFolderItem | PSCopyRequest,
  op: string,
): { itemPath: string; targetFolderPath: string } {
  const itemPath = String(body.itemPath ?? body.sourcePath ?? "").trim();
  const targetFolderPath = String(
    body.targetFolderPath ?? body.targetPath ?? "",
  ).trim();
  if (!itemPath) {
    throw new Error(`${op} requires itemPath (or sourcePath)`);
  }
  if (!targetFolderPath) {
    throw new Error(`${op} requires targetFolderPath (or targetPath)`);
  }
  return { itemPath, targetFolderPath };
}

/**
 * Wrap move fields under {@link MOVE_FOLDER_ITEM_ROOT}. Maps SPA
 * {@code sourcePath}/{@code targetPath} to server {@code itemPath}/
 * {@code targetFolderPath}. Never includes {@code copy} or a bare
 * {@code sourcePath} root.
 */
export function wrapMoveFolderItem(
  request: PSMoveFolderItem | MoveFolderItemEnvelope,
): MoveFolderItemEnvelope {
  const rec = asRecord(request);
  if (rec != null) {
    const nested = rec[MOVE_FOLDER_ITEM_ROOT];
    if (asRecord(nested) != null) {
      return {
        MoveFolderItem: resolveFolderItemPaths(
          nested as PSMoveFolderItem,
          "moveItem",
        ),
      };
    }
  }
  return {
    MoveFolderItem: resolveFolderItemPaths(
      request as PSMoveFolderItem,
      "moveItem",
    ),
  };
}

/**
 * Wrap copy fields under {@link COPY_FOLDER_ITEM_REQUEST_ROOT} for
 * {@code FoldersResource} copy endpoints.
 */
export function wrapCopyFolderItemRequest(
  request: PSCopyRequest | CopyFolderItemRequestEnvelope,
): CopyFolderItemRequestEnvelope {
  const rec = asRecord(request);
  if (rec != null) {
    const nested = rec[COPY_FOLDER_ITEM_REQUEST_ROOT];
    if (asRecord(nested) != null) {
      return {
        CopyFolderItemRequest: resolveFolderItemPaths(
          nested as PSCopyRequest,
          "copyFolder",
        ),
      };
    }
  }
  return {
    CopyFolderItemRequest: resolveFolderItemPaths(
      request as PSCopyRequest,
      "copyFolder",
    ),
  };
}

export async function moveItem(body: PSMoveFolderItem): Promise<void> {
  await post<PSPathItemResponse>(PATHS.PATH_MOVE_ITEM, wrapMoveFolderItem(body));
}

/**
 * Copy a folder via public REST {@code POST /folders/copy/folder}.
 * {@code PSMoveFolderItem} is move-only — do not invent {@code copy} on it.
 */
export async function copyFolder(body: PSCopyRequest): Promise<void> {
  await post<void>(PATHS.FOLDERS_COPY_FOLDER, wrapCopyFolderItemRequest(body));
}

/**
 * Copy a non-folder item via {@code POST /folders/copy/item}.
 */
export async function copyFolderItem(body: PSCopyRequest): Promise<void> {
  await post<void>(PATHS.FOLDERS_COPY_ITEM, wrapCopyFolderItemRequest(body));
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
    return normalizeFolderProperties(nested);
  }
  // Flat body (unit tests, already-unwrapped). Require an id so a bare
  // envelope mis-shape is not treated as success.
  if (typeof root.id === "string" || typeof root.name === "string") {
    return normalizeFolderProperties(root);
  }
  return null;
}

/**
 * JAXB+Jackson may wrap principal lists as {@code { Principal: [...] }} or a
 * single object. Folder Security maps these lists — a non-array crashes the
 * panel (#3206).
 */
export function unwrapPrincipalList(value: unknown): PSPrincipal[] {
  if (value == null) {
    return [];
  }
  if (Array.isArray(value)) {
    return value.filter(isPrincipalLike);
  }
  const rec = asRecord(value);
  if (!rec) {
    return [];
  }
  const wrapped = rec.Principal ?? rec.principal;
  if (wrapped != null) {
    return unwrapPrincipalList(wrapped);
  }
  if (isPrincipalLike(rec)) {
    return [rec as PSPrincipal];
  }
  return [];
}

function isPrincipalLike(value: unknown): value is PSPrincipal {
  if (value == null || typeof value !== "object") {
    return false;
  }
  const name = (value as PSPrincipal).name;
  return typeof name === "string" && name.length > 0;
}

function normalizeFolderProperties(
  raw: Record<string, unknown>,
): PSFolderProperties {
  const props = { ...raw } as unknown as PSFolderProperties;
  const permRaw = asRecord(raw.permission);
  if (permRaw) {
    props.permission = {
      ...(permRaw as unknown as PSFolderPermission),
      adminPrincipals: unwrapPrincipalList(permRaw.adminPrincipals),
      writePrincipals: unwrapPrincipalList(permRaw.writePrincipals),
      readPrincipals: unwrapPrincipalList(permRaw.readPrincipals),
      viewPrincipals: unwrapPrincipalList(permRaw.viewPrincipals),
    };
  }
  return props;
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
