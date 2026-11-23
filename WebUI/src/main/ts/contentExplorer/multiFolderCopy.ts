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

import type { PSPathItem } from "../api/contentExplorer/types";
import { message } from "../i18n/message";
import { formatCopyFolderError } from "./copyFolderErrors";
import { formatCopyItemError } from "./copyItemErrors";
import { EXPLORER_MSG } from "./messages";
import { isFolder } from "./selection";

export interface MultiFolderCopyFailure {
  name: string;
  message: string;
}

export interface MultiFolderCopyResult {
  copiedNames: string[];
  failures: MultiFolderCopyFailure[];
  /**
   * True only when at least one page, asset, or folder copied and none of
   * those copies failed. A partial HTTP failure is not full success (#6255).
   */
  fullSuccess: boolean;
}

export type MultiFolderCopyOutcome = "success" | "partial" | "none";

function displayName(item: PSPathItem): string {
  const name = (item.name || "").trim();
  if (name) return name;
  const path = (item.path || "").trim();
  return path || message(EXPLORER_MSG.MULTI_UNNAMED_ITEM);
}

/**
 * Folders are deep-copied with the folder copy call; pages, assets, and
 * other non-folder rows with a path use the item copy call. Any row with no
 * path is a failure, not a skipped folder.
 */
export function partitionMultiFolderCopy(items: readonly PSPathItem[]): {
  copyable: PSPathItem[];
  folders: PSPathItem[];
  missingPath: PSPathItem[];
} {
  const copyable: PSPathItem[] = [];
  const folders: PSPathItem[] = [];
  const missingPath: PSPathItem[] = [];
  for (const item of items) {
    if (!(item.path || "").trim()) {
      missingPath.push(item);
      continue;
    }
    if (isFolder(item)) {
      folders.push(item);
      continue;
    }
    copyable.push(item);
  }
  return { copyable, folders, missingPath };
}

/**
 * Copies each checked row into {@code targetPath}: non-folders with the
 * single-item copy call, folders with the folder deep-copy call
 * ({@code POST /folders/copy/folder}, server uniquifies conflicts with
 * {@code getUniqueFolderName}). Continues after an HTTP failure so earlier
 * copies stay in the destination. Per-row 400/403/404/409 failures are named
 * with the folder-aware message for folders, so a partial failure is never
 * full success. Each copied folder is named with the created (possibly
 * uniquified) name reported by the folder call; when the response carries no
 * name the source display name is used.
 */
export async function copyCheckedItemsToFolder(
  items: readonly PSPathItem[],
  targetPath: string,
  copyOne: (sourcePath: string, targetFolderPath: string) => Promise<void>,
  copyOneFolder: (
    sourcePath: string,
    targetFolderPath: string,
  ) => Promise<string | null>,
): Promise<MultiFolderCopyResult> {
  const { copyable, folders, missingPath } =
    partitionMultiFolderCopy(items);
  const copiedNames: string[] = [];
  const failures: MultiFolderCopyFailure[] = missingPath.map((item) => ({
    name: displayName(item),
    message: message(EXPLORER_MSG.ACTION_COPY_NOT_FOUND),
  }));
  const target = targetPath.trim();
  for (const item of copyable) {
    try {
      await copyOne(item.path, target);
      copiedNames.push(displayName(item));
    } catch (err) {
      failures.push({
        name: displayName(item),
        message: formatCopyItemError(err),
      });
    }
  }
  for (const item of folders) {
    try {
      const created = await copyOneFolder(item.path, target);
      const createdName = (created ?? "").trim();
      copiedNames.push(createdName ? createdName : displayName(item));
    } catch (err) {
      failures.push({
        name: displayName(item),
        message: formatCopyFolderError(err),
      });
    }
  }
  return {
    copiedNames,
    failures,
    fullSuccess: failures.length === 0 && copiedNames.length > 0,
  };
}

export function multiCopyOutcome(
  result: MultiFolderCopyResult,
): MultiFolderCopyOutcome {
  if (result.fullSuccess) return "success";
  if (result.copiedNames.length > 0) return "partial";
  return "none";
}

/** Status text. Partial and none outcomes must not use the success sentence. */
export function formatMultiFolderCopyStatus(
  result: MultiFolderCopyResult,
  targetPath: string,
): string {
  const parts: string[] = [];
  if (result.fullSuccess) {
    parts.push(
      message(EXPLORER_MSG.MULTI_COPY_SUCCESS)
        .replace("{count}", String(result.copiedNames.length))
        .replace("{path}", targetPath),
    );
  } else if (result.copiedNames.length > 0) {
    parts.push(
      message(EXPLORER_MSG.MULTI_COPY_PARTIAL)
        .replace("{count}", String(result.copiedNames.length))
        .replace("{path}", targetPath),
    );
  } else {
    parts.push(message(EXPLORER_MSG.MULTI_COPY_NONE));
  }
  if (result.copiedNames.length > 0) {
    parts.push(
      message(EXPLORER_MSG.MULTI_COPY_COPIED).replace(
        "{names}",
        result.copiedNames.join(", "),
      ),
    );
  }
  if (result.failures.length > 0) {
    const details = result.failures
      .map((failure) => `${failure.name}: ${failure.message}`)
      .join("; ");
    parts.push(
      message(EXPLORER_MSG.MULTI_COPY_FAILURES).replace("{details}", details),
    );
  }
  return parts.join(" ");
}
