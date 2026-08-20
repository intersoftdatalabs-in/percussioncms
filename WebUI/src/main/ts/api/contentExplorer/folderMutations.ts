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
 * <p>Copy uses public REST {@code POST /folders/copy/folder}
 * ({@code CopyFolderItemRequest}) — {@code PSMoveFolderItem} is move-only
 * and has no {@code copy} field (#3362). RX façade v1 has no copy.</p>
 */

import {
  addNewFolder as pathAddNewFolder,
  copyFolder as pathCopyFolder,
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
import type {
  PSCopyRequest,
  PSMoveFolderItem,
  PSPathItem,
  PSRenameFolderItem,
} from "./types";

function folderDisplayNamesEqual(a: string, b: string): boolean {
  return a.trim().toLowerCase() === b.trim().toLowerCase();
}

/**
 * Pathmanagement {@code addNewFolder} ignores {@code ?name=} and always
 * creates {@code New-Folder} (unique suffix). Apply the operator-prompted
 * name via rename so product Create Folder matches the dialog (#3640).
 */
async function applyRequestedFolderName(
  created: PSPathItem,
  requested: string,
): Promise<PSPathItem> {
  const wanted = String(requested ?? "").trim();
  if (!wanted) {
    return created;
  }
  const got = String(created?.name ?? "").trim();
  if (got && folderDisplayNamesEqual(got, wanted)) {
    return created;
  }
  const createdPath = String(created?.path ?? "").trim();
  if (!createdPath) {
    return created;
  }
  return pathRenameFolder({ path: createdPath, newName: wanted });
}

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
  const created = await pathAddNewFolder(path, name);
  return applyRequestedFolderName(created, name);
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
 * Copy a folder via {@code FoldersResource#copyFolder}. Not pathmanagement
 * {@code moveItem} (that DTO cannot carry {@code copy}).
 */
export async function copyFolder(body: PSCopyRequest): Promise<void> {
  await pathCopyFolder(body);
}

/**
 * Move an item. Dual-run for moves under RX roots. A client-only
 * {@code copy:true} flag (never posted) still routes to {@link copyFolder}
 * so older call sites do not silently move.
 */
export async function moveItem(body: PSMoveFolderItem): Promise<void> {
  if (body.copy) {
    await pathCopyFolder(body);
    return;
  }
  const sourcePath = String(body.sourcePath ?? body.itemPath ?? "").trim();
  const targetPath = String(body.targetPath ?? body.targetFolderPath ?? "").trim();
  // Prefer source path for RX gate; target must also be RX-capable when using REST.
  if (
    shouldUseRxFolderMutations(sourcePath) &&
    shouldUseRxFolderMutations(targetPath)
  ) {
    const folder = await loadFolderByPath(sourcePath);
    if (!folder?.id) {
      throw new Error(
        `moveItem (RX): could not resolve folder id for path=${sourcePath}`,
      );
    }
    const sourceParent = parentFolderPath(sourcePath);
    if (!sourceParent) {
      throw new Error(
        `moveItem (RX): cannot derive parent for path=${sourcePath}`,
      );
    }
    await moveRxFolderChildren({
      sourcePath: sourceParent,
      targetPath,
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
