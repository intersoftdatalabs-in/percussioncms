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
 * Rename the open item from the React Content Editor host (#4791).
 *
 * <p>Uses folders {@code POST /folders/rename/item}. Does not move the item.
 * HTTP 400, 403, and 404 are failures, not success.</p>
 */

import { isApiError } from "../api/client";
import type { EditorHostMode } from "./editorHostUrl";
import type { ItemEditorFields } from "./itemFieldsApi";

export type EditorRenameErrorReason =
  | "bad_request"
  | "forbidden"
  | "not_found"
  | "no_folder"
  | "failed";

/** Rename is available in edit mode only (hidden in view and promote). */
export function canRenameFromEditor(mode: EditorHostMode): boolean {
  return mode === "edit";
}

/** Listing name the host shows: {@code sys_title}, else the payload name. */
export function editorListingName(fields: ItemEditorFields | null | undefined): string {
  const rows = fields?.fields ?? [];
  const title = rows.find((row) => row.name === "sys_title");
  const fromField = title?.value != null ? String(title.value).trim() : "";
  if (fromField) {
    return fromField;
  }
  return String(fields?.name ?? "").trim();
}

/**
 * Item path for {@code rename/item}. By-id pathmanagement often returns
 * {@code folderPaths} plus {@code name} and leaves {@code path} blank.
 */
export function editorItemPathFromLookup(
  item:
    | {
        path?: string | null;
        name?: string | null;
        folderPath?: string | null;
        folderPaths?: string | readonly string[] | null;
      }
    | null
    | undefined,
): string {
  const direct = String(item?.path ?? "").trim();
  if (direct) {
    return direct;
  }
  let folder = "";
  const many = item?.folderPaths;
  if (Array.isArray(many)) {
    folder = String(many.find((entry) => String(entry ?? "").trim()) ?? "");
  } else if (typeof many === "string") {
    folder = many;
  }
  if (!folder.trim()) {
    folder = String(item?.folderPath ?? "");
  }
  return buildRenameItemPath(folder, item?.name);
}

/**
 * Full item path for {@code rename/item}. Folder paths use {@code /} (CMS URL form).
 * Returns empty when either side is blank so the caller does not POST.
 */
export function buildRenameItemPath(
  folderPath: string | null | undefined,
  listingName: string | null | undefined,
): string {
  const folder = String(folderPath ?? "")
    .trim()
    .replace(/\\/g, "/")
    .replace(/\/+$/, "");
  const name = String(listingName ?? "").trim();
  if (!folder || !name || name.includes("/") || name.includes("\\")) {
    return "";
  }
  return `${folder}/${name}`;
}

/** True when the reloaded listing name matches the name that was posted. */
export function renameLanded(
  requestedName: string | null | undefined,
  loadedName: string | null | undefined,
): boolean {
  const wanted = String(requestedName ?? "").trim();
  const loaded = String(loadedName ?? "").trim();
  return wanted.length > 0 && wanted === loaded;
}

/** Map REST failures so 400/403/404 are not treated as a successful rename. */
export function editorRenameErrorReason(err: unknown): EditorRenameErrorReason {
  if (isApiError(err)) {
    if (err.status === 400) {
      return "bad_request";
    }
    if (err.status === 403) {
      return "forbidden";
    }
    if (err.status === 404) {
      return "not_found";
    }
  }
  return "failed";
}
