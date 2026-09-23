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
 * Recycle the open item from the React Content Editor host (#4773).
 *
 * <p>Reuses public REST {@code DELETE /rest/folders/item/{path}} (same as
 * Explorer delete of a non-folder). Folders are refused. HTTP 403, 404, and
 * 409 are failures, not success.</p>
 */

import { isApiError } from "../api/client";
import type { EditorHostMode } from "./editorHostUrl";

export type EditorRecycleErrorReason =
  | "forbidden"
  | "not_found"
  | "conflict"
  | "folder"
  | "failed";

/** Minimal pathmanagement row needed to decide whether recycle is allowed. */
export interface EditorRecycleTarget {
  path?: string | null;
  type?: string | null;
  category?: string | null;
}

/** Recycle is available only while editing an open item. */
export function canRecycleFromEditor(mode: EditorHostMode): boolean {
  return mode === "edit";
}

function looksLikeFolder(item: EditorRecycleTarget): boolean {
  const type = String(item.type ?? "").trim().toLowerCase();
  const category = String(item.category ?? "").trim().toLowerCase();
  if (type === "folder" || category === "folder") {
    return true;
  }
  const path = String(item.path ?? "").trim();
  return path.endsWith("/");
}

/**
 * Folder path of a non-folder item. Folders and missing paths are not recycled.
 */
export function editorRecycleItemPath(
  item: EditorRecycleTarget | null | undefined,
): { ok: true; path: string } | { ok: false; reason: "folder" | "not_found" } {
  if (item == null) {
    return { ok: false, reason: "not_found" };
  }
  if (looksLikeFolder(item)) {
    return { ok: false, reason: "folder" };
  }
  const path = String(item.path ?? "").trim();
  if (!path) {
    return { ok: false, reason: "not_found" };
  }
  return { ok: true, path };
}

/** Map REST failures so 403/404/409 are not treated as a successful recycle. */
export function editorRecycleErrorReason(err: unknown): EditorRecycleErrorReason {
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
