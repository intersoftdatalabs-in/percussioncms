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
import { formatMoveItemError } from "./moveItemErrors";
import { isFolder } from "./selection";

export interface MultiFolderMoveFailure {
  name: string;
  message: string;
}

export interface MultiFolderMoveResult {
  movedNames: string[];
  skippedFolderNames: string[];
  failures: MultiFolderMoveFailure[];
  /**
   * True only when at least one page or asset moved and none of those
   * moves failed. Skipped folders do not make a partial HTTP failure look
   * successful, and a selection of only folders is not success (#4856).
   */
  fullSuccess: boolean;
}

export type MultiFolderMoveOutcome = "success" | "partial" | "none";

function displayName(item: PSPathItem): string {
  const name = (item.name || "").trim();
  if (name) return name;
  const path = (item.path || "").trim();
  return path || "(unnamed)";
}

/**
 * Folders are not moved by this command. Pages, assets, and other non-folder
 * rows with a path are. A non-folder row with no path is a failure.
 */
export function partitionMultiFolderMove(items: readonly PSPathItem[]): {
  movable: PSPathItem[];
  skippedFolderNames: string[];
  missingPath: PSPathItem[];
} {
  const movable: PSPathItem[] = [];
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
    movable.push(item);
  }
  return { movable, skippedFolderNames, missingPath };
}

/**
 * Moves each non-folder selection into {@code targetPath} with the existing
 * single-item move call. Continues after an HTTP failure so earlier moves
 * stay in the destination. Does not move folders.
 */
export async function moveCheckedItemsToFolder(
  items: readonly PSPathItem[],
  targetPath: string,
  moveOne: (sourcePath: string, targetFolderPath: string) => Promise<void>,
): Promise<MultiFolderMoveResult> {
  const { movable, skippedFolderNames, missingPath } =
    partitionMultiFolderMove(items);
  const movedNames: string[] = [];
  const failures: MultiFolderMoveFailure[] = missingPath.map((item) => ({
    name: displayName(item),
    message: message(EXPLORER_MSG.ACTION_MOVE_NOT_FOUND),
  }));
  const target = targetPath.trim();
  for (const item of movable) {
    try {
      await moveOne(item.path, target);
      movedNames.push(displayName(item));
    } catch (err) {
      failures.push({
        name: displayName(item),
        message: formatMoveItemError(err),
      });
    }
  }
  return {
    movedNames,
    skippedFolderNames,
    failures,
    fullSuccess: failures.length === 0 && movedNames.length > 0,
  };
}

export function multiMoveOutcome(
  result: MultiFolderMoveResult,
): MultiFolderMoveOutcome {
  if (result.fullSuccess) return "success";
  if (result.movedNames.length > 0) return "partial";
  return "none";
}

/** Status text. Partial and none outcomes must not use the success sentence. */
export function formatMultiFolderMoveStatus(
  result: MultiFolderMoveResult,
  targetPath: string,
): string {
  const parts: string[] = [];
  if (result.fullSuccess) {
    parts.push(
      message(EXPLORER_MSG.MULTI_MOVE_SUCCESS)
        .replace("{count}", String(result.movedNames.length))
        .replace("{path}", targetPath),
    );
  } else if (result.movedNames.length > 0) {
    parts.push(
      message(EXPLORER_MSG.MULTI_MOVE_PARTIAL)
        .replace("{count}", String(result.movedNames.length))
        .replace("{path}", targetPath),
    );
  } else {
    parts.push(message(EXPLORER_MSG.MULTI_MOVE_NONE));
  }
  if (result.skippedFolderNames.length > 0) {
    parts.push(
      message(EXPLORER_MSG.MULTI_MOVE_SKIPPED).replace(
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
      message(EXPLORER_MSG.MULTI_MOVE_FAILURES).replace("{details}", details),
    );
  }
  return parts.join(" ");
}
