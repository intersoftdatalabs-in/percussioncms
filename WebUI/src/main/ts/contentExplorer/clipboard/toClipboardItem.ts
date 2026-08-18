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
 * Map a detail-list / tree {@link PSPathItem} into a {@link ClipboardItem}.
 *
 * <p>Sites rows from pathmanagement use {@code type}/{@code category}
 * {@code site} / {@code FSFolder} (not lowercase {@code folder}). The
 * previous mapper only treated {@code type === "folder"} as a folder and
 * required {@code item.id ?? item.path}; missing path on a named Sites
 * row dropped the selection so Add to clipboard staged nothing.</p>
 */

import type { ClipboardItem, PSPathItem } from "../../api/contentExplorer/types";
import { firstNonBlank } from "../firstNonBlank";
import { isAssetContentType, isFolder } from "../selection";

/**
 * Convert a selected Explorer row into clipboard state.
 *
 * @return {@code null} only when the row has no stable id, path, or name
 */
export function toClipboardItem(item: PSPathItem): ClipboardItem | null {
  const id = firstNonBlank(item.id, item.path, item.name, item.title);
  if (id == null) return null;
  const path = firstNonBlank(item.path, item.folderPath, item.name, id) ?? id;
  const kind: ClipboardItem["kind"] = isFolder(item)
    ? "folder"
    : isAssetContentType(item)
      ? "asset"
      : "page";
  return {
    id,
    path,
    kind,
    name: firstNonBlank(item.name, item.title, path, id) ?? id,
    sourceAccessLevel: item.accessLevel,
  };
}
