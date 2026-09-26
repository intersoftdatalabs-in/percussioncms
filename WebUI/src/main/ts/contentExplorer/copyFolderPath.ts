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

import { resolveFolderPathFromSelection } from "./folderPath";

/**
 * Outcome of Content → Copy folder path (#4911 / parent #4530).
 * Cancel and clipboard failure do not imply a selection change — callers
 * must not update selection from this result.
 */
export type CopyFolderPathResult =
  | { status: "copied"; path: string }
  | { status: "empty" }
  | { status: "cancelled"; path: string }
  | { status: "failed"; path: string };

/**
 * Copy the folder path of the selected folder, or the folder that contains
 * the selected item. An empty path is an error and does not write the
 * clipboard. Cancel writes nothing.
 */
export async function copySelectedFolderPath(input: {
  folderPath: string | null | undefined;
  itemPath?: string | null;
  itemType?: string | null;
  confirm: (path: string) => boolean;
  writeClipboard: (text: string) => Promise<void>;
}): Promise<CopyFolderPathResult> {
  const path = resolveFolderPathFromSelection(
    input.folderPath,
    input.itemPath,
    input.itemType,
  );
  if (path == null || path.trim().length === 0) {
    return { status: "empty" };
  }
  if (!input.confirm(path)) {
    return { status: "cancelled", path };
  }
  try {
    await input.writeClipboard(path);
  } catch {
    return { status: "failed", path };
  }
  return { status: "copied", path };
}
