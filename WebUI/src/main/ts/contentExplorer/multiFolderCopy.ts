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
import { formatCopyItemError } from "./copyItemErrors";
import { EXPLORER_MSG } from "./messages";
import { isFolder } from "./selection";

export interface MultiFolderCopyFailure {
  name: string;
  message: string;
}

export interface MultiFolderCopyResult {
  copiedNames: string[];
  skippedFolderNames: string[];
  failures: MultiFolderCopyFailure[];
  /**
   * True only when at least one page or asset copied and none of those
   * copies failed. Skipped folders do not make a partial HTTP failure look
   * successful, and a selection of only folders is not success (#4855).
   */
  fullSuccess: boolean;
}

export type MultiFolderCopyOutcome = "success" | "partial" | "none";

function displayName(item: PSPathItem): string {
  const name = (item.name || "").trim();
  if (name) return name;
  const path = (item.path || "").trim();
  return path || "(unnamed)";
}

/**
 * Folders are not copied. Pages, assets, and other non-folder rows with a
 * path are. A non-folder row with no path is a failure, not a skipped folder.
 */
export function partitionMultiFolderCopy(items: readonly PSPathItem[]): {
  copyable: PSPathItem[];
  skippedFolderNames: string[];
  missingPath: PSPathItem[];
} {
  const copyable: PSPathItem[] = [];
  const skippedFolderNames: string[] = [];
  const missingPath: PSPathItem[] = [];
  for (const item of items) {
    if (isFolder(item)) {
      skippedFolderNames.push(displayName(item));
      continue;
    }
    if (!(item.path || "").trim()) {
      missingPath.push(item);
      continue;
    }
    copyable.push(item);
  }
  return { copyable, skippedFolderNames, missingPath };
}

/**
 * Copies each non-folder selection into {@code targetPath} with the existing
 * single-item copy call. Continues after an HTTP failure so earlier copies
 * stay in the destination. Does not copy folders.
 */
export async function copyCheckedItemsToFolder(
  items: readonly PSPathItem[],
  targetPath: string,
  copyOne: (sourcePath: string, targetFolderPath: string) => Promise<void>,
): Promise<MultiFolderCopyResult> {
  const { copyable, skippedFolderNames, missingPath } =
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
  return {
    copiedNames,
    skippedFolderNames,
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
  if (result.skippedFolderNames.length > 0) {
    parts.push(
      message(EXPLORER_MSG.MULTI_COPY_SKIPPED).replace(
        "{names}",
        result.skippedFolderNames.join(", "),
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
