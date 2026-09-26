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
 * Classify pathmanagement folder children for the related-item picker.
 * Folders are navigation only. Pages and assets supply the content id
 * that the existing slot-relationship insert posts.
 */

import { parseExplorerContentId } from "../api/contentExplorer/pathItemId";
import type { PSPathItem } from "../api/contentExplorer/types";

/** Explorer Sites root. Pathmanagement accepts a single leading slash. */
export const RELATED_FOLDER_PICK_ROOT = "/Sites";

export interface RelatedFolderRow {
  key: string;
  name: string;
  path: string;
  folder: boolean;
  /** Set only for a page or asset the insert POST can use. */
  contentId: number | null;
}

export function isRelatedFolderChild(
  item: Pick<PSPathItem, "type" | "category" | "path">,
): boolean {
  const type = (item.type ?? "").trim().toLowerCase();
  const category = (item.category ?? "").trim().toLowerCase();
  if (
    type === "folder" ||
    type === "fsfolder" ||
    type === "site" ||
    category === "folder" ||
    category === "fsfolder" ||
    category === "site"
  ) {
    return true;
  }
  return (item.path ?? "").trim().endsWith("/");
}

/**
 * Rows the picker can show. Folders stay even without a content id.
 * Non-folder rows without a parseable content id are dropped so a title
 * cannot be posted as an id.
 */
export function relatedFolderRows(
  items: readonly PSPathItem[] | null | undefined,
): RelatedFolderRow[] {
  if (items == null) {
    return [];
  }
  const rows: RelatedFolderRow[] = [];
  for (const item of items) {
    const path = (item.path ?? "").trim();
    const name = (item.name || item.title || path).trim();
    if (!path || !name) {
      continue;
    }
    const folder = isRelatedFolderChild(item);
    const contentId = folder ? null : parseExplorerContentId(item.id);
    if (!folder && contentId == null) {
      continue;
    }
    rows.push({
      key: `${folder ? "folder" : "item"}:${path}`,
      name,
      path,
      folder,
      contentId,
    });
  }
  return rows;
}
