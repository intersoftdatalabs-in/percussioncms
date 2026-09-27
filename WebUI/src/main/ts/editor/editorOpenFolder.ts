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
 * Open the item's folder in the React Content Explorer from EditorHost (#4941).
 *
 * <p>CMS folder paths use {@code /} (not OS separators). A missing folder, or
 * HTTP 403/404 from the path lookup, must not navigate.</p>
 */

import { isApiError } from "../api/client";
import { normalizeExplorerPath } from "../app/deepLinks/allowlists";
import { normalizeExplorerFolderPath } from "../contentExplorer/folderPath";
import { parentFolderOfItemPath } from "./editorMove";

export type OpenFolderNotice = "missing" | "forbidden" | "not_found" | "failed";

/**
 * Client route for Explorer deep-link {@code /explorer?path=…}.
 * Returns null when the folder is blank, root-only, or rejected by the
 * explorer path allowlist.
 */
export function explorerRouteForFolder(
  folderPath: string | null | undefined,
): string | null {
  const normalized = normalizeExplorerFolderPath(folderPath);
  if (!normalized) {
    return null;
  }
  const allowed = normalizeExplorerPath(normalized);
  if (!allowed || allowed === "/") {
    return null;
  }
  return `/explorer?path=${encodeURIComponent(allowed)}`;
}

/**
 * Explorer route for the folder that contains {@code itemPath}.
 * Null when the item has no parent folder.
 */
export function explorerRouteForItemPath(
  itemPath: string | null | undefined,
): string | null {
  return explorerRouteForFolder(parentFolderOfItemPath(itemPath));
}

/** Map lookup failures so 403/404 stay on the editor. */
export function openFolderNotice(err: unknown): OpenFolderNotice {
  if (isApiError(err)) {
    if (err.status === 403) {
      return "forbidden";
    }
    if (err.status === 404) {
      return "not_found";
    }
  }
  return "failed";
}
