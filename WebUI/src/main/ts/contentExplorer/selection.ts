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
 * ContentExplorer selection state shared between tree, list, and actions.
 *
 * <p>Single-select model in US1 (multi-select lives in US2 hosts).
 * Paths are the navigation key; ids are the stable identifier for refresh
 * after rename/move (paths change on rename).</p>
 */

import type { PSPathItem } from "../api/contentExplorer/types";

export interface Selection {
  /** Folder path that the list is currently showing children of. */
  folderPath: string | null;
  /** Selected item in the detail list (or null when only the folder is active). */
  item: PSPathItem | null;
}

export const EMPTY_SELECTION: Selection = {
  folderPath: null,
  item: null,
};

/**
 * Server {@code IPSItemSummary} types that are folders (not workflowed items).
 * Pathmanagement sends {@code Folder} / {@code FSFolder} (capital F), not
 * the lowercase {@code folder} the SPA historically used (#3330 / #3329).
 */
const FOLDER_TYPE_KEYS = new Set(["folder", "fsfolder", "site"]);

/**
 * Content types that are previewable items, not folders. Pathmanagement
 * lists FastForward pages as {@code percPage} / {@code Page}; those must
 * stay items even when {@code leaf} is omitted or {@code hasFolderChildren}
 * is set on a paginated row (#3456 / #2745).
 */
export function isPageOrAssetContentType(item: PSPathItem | null): boolean {
  if (!item) return false;
  const type = (item.type ?? "").trim().toLowerCase();
  const category = (item.category ?? "").trim().toLowerCase();
  const token = `${type} ${category}`;
  if (token.includes("page") || token.includes("asset")) return true;
  // FastForward landing pages (rffHome) are listed under /Pages (#3456).
  if (token.includes("rffhome")) return true;
  return false;
}

export function isFolder(item: PSPathItem | null): boolean {
  if (!item) return false;
  const type = (item.type ?? "").trim().toLowerCase();
  if (FOLDER_TYPE_KEYS.has(type)) return true;
  const category = (item.category ?? "").trim().toLowerCase();
  if (category === "folder") return true;
  // Listed percPage / Page / rffHome / asset rows are never folders (#3456).
  if (isPageOrAssetContentType(item)) return false;
  // Server PSPathItem.setPath appends '/' only for folders. $System$ and
  // similar under /Folders/ may omit type but still use a folder path (#3330).
  const path = (item.path ?? "").trim();
  if (path.endsWith("/")) return true;
  // Site-path content items with ids (listed pages) stay items even when
  // leaf is omitted/false on paginated rows.
  const hasId = Boolean((item.id ?? "").trim());
  const pathLower = path.replace(/\\/g, "/").toLowerCase();
  if (hasId && pathLower.includes("/sites/") && !pathLower.endsWith("/")) {
    return false;
  }
  if (item.leaf === true) return false;
  if (item.leaf === false) return true;
  return Boolean(item.hasFolderChildren);
}

export function canRead(item: PSPathItem | null): boolean {
  if (!item) return false;
  const level = item.accessLevel;
  return level === "ADMIN" || level === "WRITE" || level === "READ" || level === "VIEW";
}

export function canWrite(item: PSPathItem | null): boolean {
  if (!item) return false;
  const level = item.accessLevel;
  return level === "ADMIN" || level === "WRITE";
}

export function canAdmin(item: PSPathItem | null): boolean {
  if (!item) return false;
  return item.accessLevel === "ADMIN";
}