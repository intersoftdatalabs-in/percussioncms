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
 * Pure multi-select helpers for ContentBrowser (992 US2 + #2793 search host).
 * Extracted so activation-path dedupe and search-result selection mapping are
 * unit-testable without mounting the full ExplorerTree/DetailList shell.
 */

import type {
  PSItemProperties,
  SelectionItem,
} from "../api/contentExplorer/types";

export type SelectableId = { id: string };

/**
 * Append {@code item} to a multi-select list if its id is not already present.
 * Repeated activate (double-click / Enter) on the same row is a no-op.
 */
export function appendUniqueById<T extends SelectableId>(
  prev: ReadonlyArray<T>,
  item: T,
): T[] {
  if (prev.some((s) => s.id === item.id)) {
    return prev.slice();
  }
  return [...prev, item];
}

/**
 * Map a SearchPanel result row into a ContentBrowser {@link SelectionItem}.
 *
 * <p>Search rows expose {@code folderPath} (parent) rather than the item's own
 * path; when a name is present we append it so confirm payloads stay path-shaped
 * for hosts that key off {@code path}. Id falls back to folderPath/name when
 * the search hit has no id (folder-only / partial rows).</p>
 */
export function selectionItemFromSearchResult(
  result: PSItemProperties,
): SelectionItem {
  const name = (result.name ?? result.title ?? "").trim();
  const folder = (result.folderPath ?? "").trim();
  let path = folder;
  if (name && folder) {
    const base = folder.replace(/\/+$/, "");
    path = `${base}/${name}`;
  } else if (name && !folder) {
    path = name;
  }
  const id =
    result.id != null && String(result.id).trim().length > 0
      ? String(result.id)
      : path || name || "unknown";
  return {
    id,
    path: path || id,
    name: name || path || id,
    type: result.type,
    category: result.type,
  };
}
