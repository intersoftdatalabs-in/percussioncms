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

export function isFolder(item: PSPathItem | null): boolean {
  if (!item) return false;
  if (item.type === "folder") return true;
  if (item.leaf === true) return false;
  if (item.leaf === false) return true;
  // Fallback heuristic: folders lack an "id" with content semantics; they
  // expose hasFolderChildren / hasItemChildren flags in PSPathItem.
  return Boolean(item.hasFolderChildren) || item.category === "folder";
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