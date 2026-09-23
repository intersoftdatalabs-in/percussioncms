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

/**
 * Move the open editor item into another folder (#4774 / parent #4532).
 *
 * <p>Reuses public REST {@code POST /rest/folders/move/item}
 * ({@code FoldersResource#moveFolderItem}). HTTP 403, 404, and 409 are
 * failures. CMS paths use {@code /}, not OS separators. View mode has no
 * move action.</p>
 */

import { isApiError } from "../api/client";
import { findItemById, moveFolderItem } from "../api/contentExplorer/pathApi";
import type { EditorHostMode } from "./editorHostUrl";

export type EditorMoveErrorReason =
  | "forbidden"
  | "not_found"
  | "conflict"
  | "failed";

/** Move is available only while editing an open item. */
export function canMoveFromEditor(mode: EditorHostMode): boolean {
  return mode === "edit";
}

/**
 * Folder that contains {@code itemPath} (last segment stripped).
 * Repository {@code //} and Finder {@code /} forms both work.
 */
export function parentFolderOfItemPath(
  itemPath: string | null | undefined,
): string | null {
  if (itemPath == null) {
    return null;
  }
  let path = String(itemPath).trim().replace(/\\/g, "/");
  if (!path || path === "/" || path === "//") {
    return null;
  }
  const repo = path.startsWith("//");
  path = path.replace(/\/+$/, "");
  const slash = path.lastIndexOf("/");
  if (slash < 0) {
    return null;
  }
  if (repo && slash <= 1) {
    return null;
  }
  if (!repo && slash === 0) {
    return "/";
  }
  return path.slice(0, slash);
}

/** Compare folder paths ignoring separator style and a leading {@code //}. */
export function cmsFoldersEqual(
  left: string | null | undefined,
  right: string | null | undefined,
): boolean {
  const norm = (raw: string | null | undefined): string => {
    if (raw == null) {
      return "";
    }
    let path = String(raw).trim().replace(/\\/g, "/").replace(/^[A-Za-z]:/, "");
    while (path.startsWith("//")) {
      path = path.slice(1);
    }
    path = path.replace(/\/{2,}/g, "/").replace(/\/+$/, "");
    if (!path) {
      return "";
    }
    if (!path.startsWith("/")) {
      path = `/${path}`;
    }
    return path.toLowerCase();
  };
  const a = norm(left);
  const b = norm(right);
  return a.length > 0 && a === b;
}

/** Map REST failures so 403/404/409 are not treated as a successful move. */
export function editorMoveErrorReason(err: unknown): EditorMoveErrorReason {
  if (isApiError(err)) {
    if (err.status === 403) {
      return "forbidden";
    }
    if (err.status === 404) {
      return "not_found";
    }
    if (err.status === 409) {
      return "conflict";
    }
  }
  return "failed";
}

/** Full item path for the open content id (pathmanagement by id). */
export async function loadEditorItemPath(itemId: string): Promise<string> {
  const item = await findItemById(itemId);
  const path = String(item?.path ?? "").trim();
  if (!path) {
    const missing = { status: 404, statusText: "Not Found", body: {} };
    throw missing;
  }
  return path;
}

/** Move a non-folder item. Does not change the open content id. */
export async function moveEditorItemToFolder(
  itemPath: string,
  targetFolderPath: string,
): Promise<void> {
  await moveFolderItem({
    sourcePath: itemPath,
    targetPath: targetFolderPath,
  });
}
