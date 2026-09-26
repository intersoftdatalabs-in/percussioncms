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
import { formatRestoreItemError } from "./restoreItemErrors";

export interface MultiSelectRestoreFailure {
  name: string;
  message: string;
}

export interface MultiSelectRestoreResult {
  restoredNames: string[];
  failures: MultiSelectRestoreFailure[];
  /**
   * True only when every checked page, asset, or folder was restored.
   * Any HTTP failure is not full success.
   */
  fullSuccess: boolean;
}

export type MultiSelectRestoreOutcome = "success" | "partial" | "none";

function displayName(item: PSPathItem): string {
  const name = (item.name || "").trim();
  if (name) return name;
  const path = (item.path || "").trim();
  return path || "(unnamed)";
}

/**
 * Pages, assets, and folders all restore through
 * {@code PUT /rest/folders/recycle/restore/{guid}}. A row without a GUID
 * cannot be sent and is a named failure.
 */
export function partitionMultiSelectRestore(items: readonly PSPathItem[]): {
  restorable: PSPathItem[];
  missingId: PSPathItem[];
} {
  const restorable: PSPathItem[] = [];
  const missingId: PSPathItem[] = [];
  for (const item of items) {
    if (!(item.id || "").trim()) {
      missingId.push(item);
      continue;
    }
    restorable.push(item);
  }
  return { restorable, missingId };
}

/**
 * Restores each checked recycled item with the existing single-item
 * {@code PUT /rest/folders/recycle/restore/{guid}} call, including folders.
 * Continues after an HTTP failure.
 */
export async function restoreCheckedItems(
  items: readonly PSPathItem[],
  restoreOne: (guid: string) => Promise<void>,
): Promise<MultiSelectRestoreResult> {
  const { restorable, missingId } = partitionMultiSelectRestore(items);
  const restoredNames: string[] = [];
  const failures: MultiSelectRestoreFailure[] = missingId.map((item) => ({
    name: displayName(item),
    message: message(EXPLORER_MSG.ACTION_RESTORE_NOT_FOUND),
  }));
  for (const item of restorable) {
    try {
      await restoreOne(String(item.id).trim());
      restoredNames.push(displayName(item));
    } catch (err) {
      failures.push({
        name: displayName(item),
        message: formatRestoreItemError(err),
      });
    }
  }
  return {
    restoredNames,
    failures,
    fullSuccess:
      failures.length === 0 &&
      restoredNames.length > 0 &&
      restoredNames.length === items.length,
  };
}

export function multiRestoreOutcome(
  result: MultiSelectRestoreResult,
): MultiSelectRestoreOutcome {
  if (result.fullSuccess) return "success";
  if (result.restoredNames.length > 0) return "partial";
  return "none";
}

/** Status text. Partial and none outcomes must not use the success sentence. */
export function formatMultiSelectRestoreStatus(
  result: MultiSelectRestoreResult,
): string {
  const parts: string[] = [];
  if (result.fullSuccess) {
    parts.push(
      message(EXPLORER_MSG.MULTI_RESTORE_SUCCESS).replace(
        "{count}",
        String(result.restoredNames.length),
      ),
    );
  } else if (result.restoredNames.length > 0) {
    parts.push(
      message(EXPLORER_MSG.MULTI_RESTORE_PARTIAL).replace(
        "{count}",
        String(result.restoredNames.length),
      ),
    );
  } else {
    parts.push(message(EXPLORER_MSG.MULTI_RESTORE_NONE));
  }
  if (result.failures.length > 0) {
    const details = result.failures
      .map((failure) => `${failure.name}: ${failure.message}`)
      .join("; ");
    parts.push(
      message(EXPLORER_MSG.MULTI_RESTORE_FAILURES).replace("{details}", details),
    );
  }
  return parts.join(" ");
}
