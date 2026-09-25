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

export interface MultiFolderPurgeFailure {
  name: string;
  message: string;
}

export interface MultiFolderPurgeResult {
  purgedNames: string[];
  skippedFolderNames: string[];
  failures: MultiFolderPurgeFailure[];
  /**
   * True only when at least one page or asset purged and none of those
   * purges failed. Skipped folders do not make a partial HTTP failure look
   * successful, and a selection of only folders is not success (#4883).
   */
  fullSuccess: boolean;
}

export type MultiFolderPurgeOutcome = "success" | "partial" | "none";

function displayName(item: PSPathItem): string {
  const name = (item.name || "").trim();
  if (name) return name;
  const path = (item.path || "").trim();
  return path || "(unnamed)";
}

/**
 * Folders are not purged by this command. Pages and assets with an id are.
 * A non-folder row with no id cannot call the single-item purge REST.
 */
export function partitionMultiFolderPurge(items: readonly PSPathItem[]): {
  purgeable: PSPathItem[];
  skippedFolderNames: string[];
  missingId: PSPathItem[];
} {
  const purgeable: PSPathItem[] = [];
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
    purgeable.push(item);
  }
  return { purgeable, skippedFolderNames, missingId };
}

/**
 * Purges each non-folder selection with the existing single-item page or
 * asset purge (`DELETE` pagemanagement / assetmanagement). Continues after an
 * HTTP failure so earlier purges stay gone. Does not purge folders.
 *
 * {@code purgeOne} is {@code purgeSelectedItem}: true when the DELETE
 * succeeded, false when the row is not a page or asset.
 */
export async function purgeCheckedItems(
  items: readonly PSPathItem[],
  purgeOne: (item: PSPathItem) => Promise<boolean>,
): Promise<MultiFolderPurgeResult> {
  const { purgeable, skippedFolderNames, missingId } =
    partitionMultiFolderPurge(items);
  const purgedNames: string[] = [];
  const failures: MultiFolderPurgeFailure[] = missingId.map((item) => ({
    name: displayName(item),
    message: message(EXPLORER_MSG.ACTION_PURGE_NOT_FOUND),
  }));
  for (const item of purgeable) {
    try {
      const ok = await purgeOne(item);
      if (!ok) {
        failures.push({
          name: displayName(item),
          message: message(EXPLORER_MSG.ACTION_UNAVAILABLE),
        });
        continue;
      }
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
    fullSuccess: failures.length === 0 && purgedNames.length > 0,
  };
}

export function multiPurgeOutcome(
  result: MultiFolderPurgeResult,
): MultiFolderPurgeOutcome {
  if (result.fullSuccess) return "success";
  if (result.purgedNames.length > 0) return "partial";
  return "none";
}

/** Status text. Partial and none outcomes must not use the success sentence. */
export function formatMultiFolderPurgeStatus(
  result: MultiFolderPurgeResult,
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
