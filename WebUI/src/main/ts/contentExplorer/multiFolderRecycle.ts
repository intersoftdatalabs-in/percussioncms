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
import { formatDeleteItemError } from "./deleteItemErrors";
import { EXPLORER_MSG } from "./messages";
import { isFolder } from "./selection";

export interface MultiFolderRecycleFailure {
  name: string;
  message: string;
}

export interface MultiFolderRecycleResult {
  recycledNames: string[];
  skippedFolderNames: string[];
  failures: MultiFolderRecycleFailure[];
  /**
   * True only when at least one page or asset recycled and none of those
   * recycles failed. Skipped folders do not make a partial HTTP failure look
   * successful, and a selection of only folders is not success (#4857).
   */
  fullSuccess: boolean;
}

export type MultiFolderRecycleOutcome = "success" | "partial" | "none";

function displayName(item: PSPathItem): string {
  const name = (item.name || "").trim();
  if (name) return name;
  const path = (item.path || "").trim();
  return path || "(unnamed)";
}

/**
 * Folders are not recycled by this command. Pages, assets, and other
 * non-folder rows with a path are. A non-folder row with no path is a failure.
 */
export function partitionMultiFolderRecycle(items: readonly PSPathItem[]): {
  recyclable: PSPathItem[];
  skippedFolderNames: string[];
  missingPath: PSPathItem[];
} {
  const recyclable: PSPathItem[] = [];
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
    recyclable.push(item);
  }
  return { recyclable, skippedFolderNames, missingPath };
}

/**
 * Recycles each non-folder selection with the existing single-item
 * {@code DELETE /rest/folders/item} call. Continues after an HTTP failure so
 * earlier recycles stay in the bin. Does not recycle folders.
 */
export async function recycleCheckedItems(
  items: readonly PSPathItem[],
  recycleOne: (itemPath: string) => Promise<void>,
): Promise<MultiFolderRecycleResult> {
  const { recyclable, skippedFolderNames, missingPath } =
    partitionMultiFolderRecycle(items);
  const recycledNames: string[] = [];
  const failures: MultiFolderRecycleFailure[] = missingPath.map((item) => ({
    name: displayName(item),
    message: message(EXPLORER_MSG.ACTION_DELETE_NOT_FOUND),
  }));
  for (const item of recyclable) {
    try {
      await recycleOne(item.path);
      recycledNames.push(displayName(item));
    } catch (err) {
      failures.push({
        name: displayName(item),
        message: formatDeleteItemError(err),
      });
    }
  }
  return {
    recycledNames,
    skippedFolderNames,
    failures,
    fullSuccess: failures.length === 0 && recycledNames.length > 0,
  };
}

export function multiRecycleOutcome(
  result: MultiFolderRecycleResult,
): MultiFolderRecycleOutcome {
  if (result.fullSuccess) return "success";
  if (result.recycledNames.length > 0) return "partial";
  return "none";
}

/** Status text. Partial and none outcomes must not use the success sentence. */
export function formatMultiFolderRecycleStatus(
  result: MultiFolderRecycleResult,
): string {
  const parts: string[] = [];
  if (result.fullSuccess) {
    parts.push(
      message(EXPLORER_MSG.MULTI_RECYCLE_SUCCESS).replace(
        "{count}",
        String(result.recycledNames.length),
      ),
    );
  } else if (result.recycledNames.length > 0) {
    parts.push(
      message(EXPLORER_MSG.MULTI_RECYCLE_PARTIAL).replace(
        "{count}",
        String(result.recycledNames.length),
      ),
    );
  } else {
    parts.push(message(EXPLORER_MSG.MULTI_RECYCLE_NONE));
  }
  if (result.skippedFolderNames.length > 0) {
    parts.push(
      message(EXPLORER_MSG.MULTI_RECYCLE_SKIPPED).replace(
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
      message(EXPLORER_MSG.MULTI_RECYCLE_FAILURES).replace("{details}", details),
    );
  }
  return parts.join(" ");
}
