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

import { get } from "../client";
import { asJsonRecord, asObjectArray } from "../jsonList";
import { PATHS } from "../paths";

/**
 * Allow-listed File Explorer root (GET /services/fileexplorer).
 * Catalog {@code id} is the API key — never a filesystem path.
 */
export type FileExplorerRoot = {
  id: string;
  displayName: string;
  exists?: boolean;
};

/**
 * Child under an allow-listed root (GET …/{rootId}/children).
 * {@code relativePath} uses {@code /} (REST contract, not OS separators).
 */
export type FileExplorerEntry = {
  name: string;
  relativePath: string;
  directory: boolean;
  size?: number;
};

/** Matches REST FileExplorerAdaptor SAFE_ROOT_ID. */
export const FILE_EXPLORER_ROOT_ID_RE = /^[A-Za-z][A-Za-z0-9_-]{0,63}$/;

export function isSafeFileExplorerRootId(rootId: string): boolean {
  return FILE_EXPLORER_ROOT_ID_RE.test(rootId);
}

/**
 * Validate a REST relative path (slash-separated under a catalog root).
 * Rejects parent traversal, absolute/drive/UNC forms. Empty is the root.
 */
export function isSafeFileExplorerRelativePath(path: string): boolean {
  const trimmed = path.trim();
  if (!trimmed) return true;
  if (trimmed.includes("\\")) return false;
  if (trimmed.startsWith("/") || trimmed.startsWith("//")) return false;
  if (/^[A-Za-z]:/.test(trimmed)) return false;
  const parts = trimmed.split("/");
  for (const part of parts) {
    if (!part || part === "." || part === "..") {
      return false;
    }
  }
  return true;
}

/** Parent of a REST relative path; empty string is the allow-listed root. */
export function parentFileExplorerPath(relativePath: string): string {
  const parts = relativePath.split("/").filter((p) => p.length > 0);
  parts.pop();
  return parts.join("/");
}

function parseNamedList(
  payload: unknown,
  names: readonly string[],
): unknown[] {
  if (payload == null) return [];
  if (Array.isArray(payload)) return payload;
  const obj = asJsonRecord(payload);
  if (!obj) {
    return [];
  }
  for (const key of names) {
    const raw = obj[key];
    if (raw == null) continue;
    if (Array.isArray(raw)) return raw;
    if (typeof raw === "object") return [raw];
  }
  if (typeof obj.id === "string" || typeof obj.name === "string") {
    return [obj];
  }
  return asObjectArray(payload);
}

function asOptionalBoolean(value: unknown): boolean | undefined {
  if (typeof value === "boolean") return value;
  return undefined;
}

function asOptionalSize(value: unknown): number | undefined {
  if (typeof value === "number" && Number.isFinite(value) && value >= 0) {
    return value;
  }
  return undefined;
}

export function unwrapFileExplorerRoots(payload: unknown): FileExplorerRoot[] {
  const raw = parseNamedList(payload, [
    "FileExplorerRoot",
    "fileExplorerRoot",
    "roots",
  ]);
  const out: FileExplorerRoot[] = [];
  for (const item of raw) {
    const rec = asJsonRecord(item);
    if (!rec) continue;
    const id = typeof rec.id === "string" ? rec.id.trim() : "";
    if (!id || !isSafeFileExplorerRootId(id)) continue;
    const display =
      typeof rec.displayName === "string" && rec.displayName.trim()
        ? rec.displayName.trim()
        : id;
    out.push({
      id,
      displayName: display,
      exists: asOptionalBoolean(rec.exists),
    });
  }
  return out;
}

export function unwrapFileExplorerEntries(payload: unknown): FileExplorerEntry[] {
  const raw = parseNamedList(payload, [
    "FileExplorerEntry",
    "fileExplorerEntry",
    "entries",
    "children",
  ]);
  const out: FileExplorerEntry[] = [];
  for (const item of raw) {
    const rec = asJsonRecord(item);
    if (!rec) continue;
    const name = typeof rec.name === "string" ? rec.name.trim() : "";
    const relativePath =
      typeof rec.relativePath === "string" ? rec.relativePath.trim() : "";
    if (!name || !relativePath || !isSafeFileExplorerRelativePath(relativePath)) {
      continue;
    }
    out.push({
      name,
      relativePath,
      directory: rec.directory === true,
      size: asOptionalSize(rec.size),
    });
  }
  return out;
}

export function fileExplorerChildrenUrl(
  rootId: string,
  relativePath?: string | null,
): string {
  const base = `${PATHS.FILE_EXPLORER}/${encodeURIComponent(rootId)}/children`;
  const path = (relativePath ?? "").trim();
  if (!path) return base;
  return `${base}?path=${encodeURIComponent(path)}`;
}

/** GET /services/fileexplorer — Admin allow-listed roots (ids only). */
export async function listFileExplorerRoots(): Promise<FileExplorerRoot[]> {
  return unwrapFileExplorerRoots(await get<unknown>(PATHS.FILE_EXPLORER));
}

/** GET /services/fileexplorer/{rootId}/children?path= — Admin; omit path for root. */
export async function listFileExplorerChildren(
  rootId: string,
  relativePath?: string | null,
): Promise<FileExplorerEntry[]> {
  if (!isSafeFileExplorerRootId(rootId)) {
    throw new Error("Invalid File Explorer root");
  }
  const path = (relativePath ?? "").trim();
  if (!isSafeFileExplorerRelativePath(path)) {
    throw new Error("Invalid File Explorer path");
  }
  return unwrapFileExplorerEntries(
    await get<unknown>(fileExplorerChildrenUrl(rootId, path)),
  );
}
