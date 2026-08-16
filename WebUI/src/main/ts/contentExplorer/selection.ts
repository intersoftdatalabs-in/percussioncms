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
 * Server {@link IPSItemSummary.Category} values that are folders / nav.
 * Stable across customer content-type names (#3456).
 */
const FOLDER_CATEGORY_KEYS = new Set([
  "folder",
  "site",
  "section_folder",
  "external_section_folder",
  "system",
]);

/**
 * Server categories that are workflowed items. {@code PSItemSummaryService}
 * sets {@code PAGE} only for {@code percPage}, {@code LANDING_PAGE} for
 * landing pages, and defaults every other non-folder / non-nav type
 * (FastForward, customer types, assets) to {@code ASSET}.
 */
const ITEM_CATEGORY_KEYS = new Set([
  "page",
  "asset",
  "landing_page",
  "resource",
]);

/**
 * Stock / FastForward {@code type} names used when {@code category} is
 * omitted on a paginated row. Customer types are <em>not</em> listed —
 * those names are not stable after upgrade (#3456).
 */
const PAGE_OR_ASSET_TYPE_KEYS = new Set([
  "page",
  "percpage",
  "percasset",
  "asset",
  "rffhome",
  "rffevent",
  "rffimage",
  "rfffile",
  "rffgeneric",
  "rffgenericword",
  "rffbrief",
  "rffcalendar",
  "rffcontacts",
  "rffpressrelease",
  "rffexternallink",
  "rffautoindex",
  "rffnavimage",
]);

/** FastForward nav types are folder-like, not previewable items. */
const RFF_NAV_TYPE_KEYS = new Set(["rffnavon", "rffnavtree"]);

function folderTypeOrCategory(type: string, category: string): boolean {
  return (
    FOLDER_TYPE_KEYS.has(type) ||
    FOLDER_CATEGORY_KEYS.has(category) ||
    RFF_NAV_TYPE_KEYS.has(type) ||
    RFF_NAV_TYPE_KEYS.has(category)
  );
}

/**
 * Content types that are previewable items, not folders.
 *
 * <p>Prefer {@code category} (object class) over {@code type} (content-type
 * name). Customers define their own types and templates; those names are
 * not consistent after upgrade outside FastForward (#3456). Pathmanagement
 * lists {@code percPage} / {@code Page} / customer items even when
 * {@code leaf} is omitted or {@code hasFolderChildren} is set (#2745).</p>
 */
export function isPageOrAssetContentType(item: PSPathItem | null): boolean {
  if (!item) return false;
  const type = (item.type ?? "").trim().toLowerCase();
  const category = (item.category ?? "").trim().toLowerCase();
  if (folderTypeOrCategory(type, category)) {
    return false;
  }
  if (ITEM_CATEGORY_KEYS.has(category)) {
    return true;
  }
  if (PAGE_OR_ASSET_TYPE_KEYS.has(type) || PAGE_OR_ASSET_TYPE_KEYS.has(category)) {
    return true;
  }
  // Customer / legacy type name: any non-folder type is an item.
  return type.length > 0;
}

export function isFolder(item: PSPathItem | null): boolean {
  if (!item) return false;
  const type = (item.type ?? "").trim().toLowerCase();
  const category = (item.category ?? "").trim().toLowerCase();
  if (folderTypeOrCategory(type, category)) return true;
  // Listed percPage / Page / rffHome / customer items are never folders (#3456).
  if (isPageOrAssetContentType(item)) return false;
  // Server PSPathItem.setPath appends '/' only for folders. $System$ and
  // similar under /Folders/ may omit type but still use a folder path (#3330).
  const path = (item.path ?? "").trim();
  if (path.endsWith("/")) return true;
  // Untyped rows: leaf / children. Customer types with a name already
  // returned above via {@link isPageOrAssetContentType}.
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