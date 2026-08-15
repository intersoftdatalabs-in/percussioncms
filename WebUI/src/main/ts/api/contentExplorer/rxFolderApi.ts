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
 * Typed client for the content-explorer folders REST façade (#3073 / #3074).
 *
 * <p>Peer of {@link pathApi} for <strong>mutations</strong> only. Browse /
 * pagination remain on pathmanagement. Wire contract:
 * {@code rest/.../contentexplorer/folders/ContentExplorerFoldersResource}
 * and product-docs {@code developer/rest.md}.</p>
 *
 * <pre>
 *   POST   /Rhythmyx/rest/content-explorer/folders
 *   PUT    /Rhythmyx/rest/content-explorer/folders/by-id/{id}
 *   POST   /Rhythmyx/rest/content-explorer/folders/move-children
 *   DELETE /Rhythmyx/rest/content-explorer/folders/by-id/{id}?purge=
 *   GET    /Rhythmyx/rest/content-explorer/folders/by-path/{path}
 * </pre>
 *
 * <p>Base path follows content-explorer peers ({@code relationshipsApi},
 * {@code translationsApi}): {@code /Rhythmyx/rest/content-explorer/folders}.</p>
 */

import { del, get, post, put } from "../client";
import type { PSPathItem } from "./types";

/** Wire DTO (subset) for {@code RxFolder}. */
export interface RxFolder {
  id?: string;
  contentId?: number;
  name?: string;
  path?: string;
  description?: string;
  communityId?: number;
  communityName?: string;
  locale?: string;
  displayFormatName?: string;
  permissions?: number;
  properties?: Array<{ name?: string; value?: string }>;
}

export interface AddFolderRequest {
  name: string;
  parentPath: string;
  sourcePath?: string;
}

/**
 * Jackson / JAXB root for {@link AddFolderRequest}
 * ({@code @XmlRootElement(name = "AddFolderRequest")}). CXF JAXB rejects a
 * bare {@code name} field (QA #3360 / #3361).
 */
export const ADD_FOLDER_REQUEST_ROOT = "AddFolderRequest";

/** Wire envelope required by WRAP_ROOT_VALUE / JAXB on folder create. */
export type AddFolderRequestEnvelope = {
  AddFolderRequest: AddFolderRequest;
};

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value != null && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

/**
 * Wrap create fields under {@link ADD_FOLDER_REQUEST_ROOT}.
 * Does not double-wrap an already-enveloped payload.
 */
export function wrapAddFolderRequest(
  request: AddFolderRequest | AddFolderRequestEnvelope,
): AddFolderRequestEnvelope {
  const rec = asRecord(request);
  if (rec != null) {
    const nested = rec[ADD_FOLDER_REQUEST_ROOT];
    if (asRecord(nested) != null) {
      return { AddFolderRequest: nested as AddFolderRequest };
    }
  }
  return { AddFolderRequest: request as AddFolderRequest };
}

export interface FolderChildrenRequest {
  sourcePath?: string;
  sourceId?: string;
  targetPath?: string;
  targetId?: string;
  parentPath?: string;
  parentId?: string;
  childIds?: string[];
  purgeItems?: boolean;
  checkFolderPermission?: boolean;
}

/**
 * REST base for content-explorer folders. Hardcoded {@code /Rhythmyx/rest/…}
 * to match relationships/translations peers; Jetty product context may still
 * serve this path via reverse-proxy conventions used by those clients.
 */
export const RX_FOLDER_REST_BASE = "/Rhythmyx/rest/content-explorer/folders";

/**
 * Encode an RX / finder path for the JAX-RS {@code {path:.+}} segment.
 *
 * <p>Preserves a leading {@code //} repository prefix as two encoded empty
 * segments is wrong — instead encode each non-empty segment and re-join with
 * {@code /}. Leading double-slash is represented by prefixing the first
 * segment path after encoding individual segments of the stripped form, then
 * re-applying {@code //} when the original had repository form.</p>
 *
 * <p>Server {@code decodePath} + {@code normalizeRxPath} accept both
 * {@code /Folders/...} and {@code //Folders/...}; we send a slash-joined
 * encoded segment list without inventing a third dialect.</p>
 */
export function encodeRxFolderPath(path: string): string {
  if (path == null || String(path).trim().length === 0) {
    return "";
  }
  let p = String(path).trim().replace(/\\/g, "/");
  const repo = p.startsWith("//");
  // Strip leading slashes for segment split; re-apply single leading slash form
  // (server promotes /Folders → //Folders).
  while (p.startsWith("/")) {
    p = p.slice(1);
  }
  const encoded = p
    .split("/")
    .filter((seg) => seg.length > 0)
    .map((seg) => encodeURIComponent(seg))
    .join("/");
  // Prefer single-slash form on the wire (Folders/…); server normalizes.
  // Keep a leading slash only for pure root.
  if (!encoded) {
    return encodeURIComponent("/");
  }
  // For repository form we can still send Folders/... — adaptor promotes.
  void repo;
  return encoded;
}

function byPathUrl(path: string): string {
  const suffix = encodeRxFolderPath(path);
  return `${RX_FOLDER_REST_BASE}/by-path/${suffix}`;
}

function byIdUrl(id: string): string {
  return `${RX_FOLDER_REST_BASE}/by-id/${encodeURIComponent(id)}`;
}

/** Map REST {@link RxFolder} to Explorer {@link PSPathItem} for callers that expect pathmanagement shape. */
export function rxFolderToPathItem(folder: RxFolder | null | undefined): PSPathItem {
  if (!folder) {
    return { name: "", path: "" };
  }
  // Prefer finder-style single-slash path when REST returns // form so tree keys match.
  let path = folder.path ?? "";
  if (path.startsWith("//")) {
    path = path.slice(1);
  }
  return {
    id: folder.id,
    name: folder.name ?? "",
    path,
    type: "folder",
    category: "folder",
    leaf: false,
  };
}

/** Load folder by RX/finder path. */
export async function loadFolderByPath(path: string): Promise<RxFolder> {
  return get<RxFolder>(byPathUrl(path));
}

/** Load folder by guid / content id. */
export async function loadFolderById(id: string): Promise<RxFolder> {
  return get<RxFolder>(byIdUrl(id));
}

/** Create a single folder under {@code parentPath}. */
export async function addRxFolder(
  parentPath: string,
  name: string,
  sourcePath?: string,
): Promise<RxFolder> {
  const fields: AddFolderRequest = { name, parentPath };
  if (sourcePath) {
    fields.sourcePath = sourcePath;
  }
  return post<RxFolder>(RX_FOLDER_REST_BASE, wrapAddFolderRequest(fields));
}

/** Save folder fields (rename via {@code name}). */
export async function saveRxFolder(
  id: string,
  body: Partial<RxFolder>,
): Promise<RxFolder> {
  return put<RxFolder>(byIdUrl(id), body);
}

/** Move children from source parent to target parent. */
export async function moveRxFolderChildren(
  request: FolderChildrenRequest,
): Promise<void> {
  await post<void>(`${RX_FOLDER_REST_BASE}/move-children`, request);
}

/** Recursive delete folder by id. */
export async function deleteRxFolder(
  id: string,
  purge = false,
): Promise<void> {
  const q = purge ? "?purge=true" : "?purge=false";
  await del<void>(`${byIdUrl(id)}${q}`);
}

/**
 * Parent path of a CMS folder path (finder or repository form).
 *
 * @returns parent path starting with {@code /} (or {@code //} when input was repo form),
 *   or {@code null} when path has no parent segment.
 */
export function parentFolderPath(path: string | null | undefined): string | null {
  if (path == null) {
    return null;
  }
  let p = String(path).trim().replace(/\\/g, "/");
  if (p.length === 0) {
    return null;
  }
  const repo = p.startsWith("//");
  while (p.endsWith("/") && p.length > 1) {
    p = p.slice(0, -1);
  }
  const parts = p.split("/").filter((s) => s.length > 0);
  if (parts.length <= 1) {
    // Under a root (e.g. /Folders or //Sites) — parent is the root itself for
    // multi-child ops when moving the only child… actually parent of /Folders/X
    // is /Folders. Parent of /Folders is null (cannot move root).
    return null;
  }
  parts.pop();
  const joined = parts.join("/");
  return repo ? `//${joined}` : `/${joined}`;
}
