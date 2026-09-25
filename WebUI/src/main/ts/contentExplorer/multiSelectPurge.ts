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
import { EXPLORER_MSG } from "./messages";
import { formatPurgeItemError } from "./purgeItemErrors";
import { isFolder } from "./selection";

export interface MultiSelectPurgeFailure {
  name: string;
  message: string;
}

export interface MultiSelectPurgeResult {
  purgedNames: string[];
  skippedFolderNames: string[];
  failures: MultiSelectPurgeFailure[];
  /**
   * True only when every checked page or asset was purged and no folder was
   * in the selection. A skipped folder or any HTTP failure is not full success.
   */
  fullSuccess: boolean;
}

export type MultiSelectPurgeOutcome = "success" | "partial" | "none";

function displayName(item: PSPathItem): string {
  const name = (item.name || "").trim();
  if (name) return name;
  const path = (item.path || "").trim();
  return path || "(unnamed)";
}

/**
 * Folders are never purged by multi-select. Pages and assets need a GUID
 * (the same id single-item purge sends to {@code DELETE /rest/folders/recycle/{guid}}).
 */
export function partitionMultiSelectPurge(items: readonly PSPathItem[]): {
  purgable: PSPathItem[];
  skippedFolderNames: string[];
  missingId: PSPathItem[];
} {
  const purgable: PSPathItem[] = [];
  const skippedFolderNames: string[] = [];
  const missingId: PSPathItem[] = [];
  for (const item of items) {
    if (isFolder(item)) {
      skippedFolderNames.push(displayName(item));
      continue;
    }
    if (!(item.id || "").trim()) {
      missingId.push(item);
      continue;
    }
    purgable.push(item);
  }
  return { purgable, skippedFolderNames, missingId };
}

/**
 * Purges each non-folder selection with the existing single-item
 * {@code DELETE /rest/folders/recycle/{guid}} call. Continues after an HTTP
 * failure. Does not purge folders.
 */
export async function purgeCheckedItems(
  items: readonly PSPathItem[],
  purgeOne: (guid: string) => Promise<void>,
): Promise<MultiSelectPurgeResult> {
  const { purgable, skippedFolderNames, missingId } =
    partitionMultiSelectPurge(items);
  const purgedNames: string[] = [];
  const failures: MultiSelectPurgeFailure[] = missingId.map((item) => ({
    name: displayName(item),
    message: message(EXPLORER_MSG.ACTION_PURGE_NOT_FOUND),
  }));
  for (const item of purgable) {
    try {
      await purgeOne(String(item.id).trim());
      purgedNames.push(displayName(item));
    } catch (err) {
      failures.push({
        name: displayName(item),
        message: formatPurgeItemError(err),
      });
    }
  }
  return {
    purgedNames,
    skippedFolderNames,
    failures,
    fullSuccess:
      failures.length === 0 &&
      skippedFolderNames.length === 0 &&
      purgedNames.length > 0 &&
      purgedNames.length === items.length,
  };
}

export function multiPurgeOutcome(
  result: MultiSelectPurgeResult,
): MultiSelectPurgeOutcome {
  if (result.fullSuccess) return "success";
  if (result.purgedNames.length > 0) return "partial";
  return "none";
}

/** Status text. Partial and none outcomes must not use the success sentence. */
export function formatMultiSelectPurgeStatus(
  result: MultiSelectPurgeResult,
): string {
  const parts: string[] = [];
  if (result.fullSuccess) {
    parts.push(
      message(EXPLORER_MSG.MULTI_PURGE_SUCCESS).replace(
        "{count}",
        String(result.purgedNames.length),
      ),
    );
  } else if (result.purgedNames.length > 0) {
    parts.push(
      message(EXPLORER_MSG.MULTI_PURGE_PARTIAL).replace(
        "{count}",
        String(result.purgedNames.length),
      ),
    );
  } else {
    parts.push(message(EXPLORER_MSG.MULTI_PURGE_NONE));
  }
  if (result.skippedFolderNames.length > 0) {
    parts.push(
      message(EXPLORER_MSG.MULTI_PURGE_SKIPPED).replace(
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
      message(EXPLORER_MSG.MULTI_PURGE_FAILURES).replace("{details}", details),
    );
  }
  return parts.join(" ");
}
