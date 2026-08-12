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
 * Dual-run folder mutation router for Content Explorer (#3074).
 *
 * <p>When the dual-run flag is <strong>off</strong>, delegates entirely to
 * pathmanagement {@link pathApi}. When <strong>on</strong> and the path is
 * under an RX-capable root ({@code /Folders}, {@code /Sites}), uses
 * {@link rxFolderApi} (content-explorer folders REST). Browse/list stay on
 * pathmanagement.</p>
 *
 * <p>ACL save ({@code saveFolderProperties}) remains on pathmanagement — the
 * REST façade {@code RxFolder} property model is not a drop-in for CM1
 * {@code PSFolderProperties} security UI.</p>
 *
 * <p>Copy ({@code moveItem} with {@code copy:true}) always stays on
 * pathmanagement (no RX folder copy on the façade v1 surface).</p>
 */

import {
  addNewFolder as pathAddNewFolder,
  deleteItem as pathDeleteItem,
  moveItem as pathMoveItem,
  renameFolder as pathRenameFolder,
} from "./pathApi";
import {
  addRxFolder,
  deleteRxFolder,
  loadFolderByPath,
  moveRxFolderChildren,
  parentFolderPath,
  rxFolderToPathItem,
  saveRxFolder,
} from "./rxFolderApi";
import { shouldUseRxFolderMutations } from "./rxFolderMutationsFlag";
import type { PSMoveFolderItem, PSPathItem, PSRenameFolderItem } from "./types";

/**
 * Create a folder. Dual-run: RX REST under Folders/Sites when flag on.
 */
export async function addNewFolder(
  path: string,
  name: string,
): Promise<PSPathItem> {
  if (shouldUseRxFolderMutations(path)) {
    const created = await addRxFolder(path, name);
    return rxFolderToPathItem(created);
  }
  return pathAddNewFolder(path, name);
}

/**
 * Rename a folder by path. Dual-run: load-by-path + save name via REST.
 */
export async function renameFolder(
  body: PSRenameFolderItem,
): Promise<PSPathItem> {
  if (shouldUseRxFolderMutations(body.path)) {
    const existing = await loadFolderByPath(body.path);
    if (!existing?.id) {
      throw new Error(
        `renameFolder (RX): could not resolve folder id for path=${body.path}`,
      );
    }
    const saved = await saveRxFolder(existing.id, { name: body.newName });
    return rxFolderToPathItem(saved);
  }
  return pathRenameFolder(body);
}

/**
 * Move (or copy) an item. Dual-run for non-copy moves under RX roots.
 * Copy always uses pathmanagement.
 */
export async function moveItem(body: PSMoveFolderItem): Promise<void> {
  if (body.copy) {
    await pathMoveItem(body);
    return;
  }
  // Prefer source path for RX gate; target must also be RX-capable when using REST.
  if (
    shouldUseRxFolderMutations(body.sourcePath) &&
    shouldUseRxFolderMutations(body.targetPath)
  ) {
    const folder = await loadFolderByPath(body.sourcePath);
    if (!folder?.id) {
      throw new Error(
        `moveItem (RX): could not resolve folder id for path=${body.sourcePath}`,
      );
    }
    const sourceParent = parentFolderPath(body.sourcePath);
    if (!sourceParent) {
      throw new Error(
        `moveItem (RX): cannot derive parent for path=${body.sourcePath}`,
      );
    }
    await moveRxFolderChildren({
      sourcePath: sourceParent,
      targetPath: body.targetPath,
      childIds: [folder.id],
    });
    return;
  }
  await pathMoveItem(body);
}

/**
 * Delete a folder (or fall back to pathmanagement for non-folder items).
 *
 * <p>When dual-run is on and the path is RX-capable, attempts REST folder
 * delete by id. On failure to resolve a folder (e.g. content item path),
 * falls back to pathmanagement delete so non-folder deletes still work.</p>
 */
export async function deleteItem(path: string): Promise<void> {
  if (shouldUseRxFolderMutations(path)) {
    try {
      const folder = await loadFolderByPath(path);
      if (folder?.id) {
        await deleteRxFolder(folder.id, false);
        return;
      }
    } catch {
      // Non-folder or not found on RX surface — pathmanagement handles items.
    }
  }
  await pathDeleteItem(path);
}

/** Re-export flag helpers for call sites / tests. */
export {
  isRxCapableFolderPath,
  isRxFolderMutationsEnabled,
  shouldUseRxFolderMutations,
} from "./rxFolderMutationsFlag";
