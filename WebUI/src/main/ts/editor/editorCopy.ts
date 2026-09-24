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
 * New copy / promotable version from the React Content Editor host (#4570).
 *
 * <p>Reuses itemmanagement {@code createNewCopy} / {@code createPromotableVersion}.
 * HTTP 403 and 404 are failures, not success.</p>
 */

import { isApiError } from "../api/client";
import { copyFolderItem } from "../api/contentExplorer/pathApi";
import { parseExplorerContentId } from "../api/contentExplorer/pathItemId";
import type { EditorHostMode } from "./editorHostUrl";

export type EditorCopyKind = "copy" | "promotable";

export type EditorCopyErrorReason =
  | "forbidden"
  | "not_found"
  | "bad_request"
  | "failed";

/** Copy actions are available in edit mode when an item is open. */
export function canCopyFromEditor(mode: EditorHostMode): boolean {
  return mode === "edit";
}

/**
 * Numeric CMS content id from a copy result ({@code itemId} or GUID).
 */
export function parseCopyLandingContentId(
  itemId: string | number | null | undefined,
): number | null {
  return parseExplorerContentId(itemId);
}

/** Map REST failures so 400/403/404 are not treated as a successful copy. */
export function editorCopyErrorReason(err: unknown): EditorCopyErrorReason {
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

/**
 * Copy the open item into a chosen folder via
 * {@code POST /rest/folders/copy/item}. Blank paths are HTTP 400 and do not
 * call the server. The public contract returns a status, not a new item id.
 */
export async function copyEditorItemToFolder(
  itemPath: string,
  targetFolderPath: string,
): Promise<void> {
  const item = String(itemPath ?? "").trim();
  const target = String(targetFolderPath ?? "").trim();
  if (!item || !target) {
    const bad = { status: 400, statusText: "Bad Request", body: {} };
    throw bad;
  }
  await copyFolderItem({ itemPath: item, targetFolderPath: target });
}
