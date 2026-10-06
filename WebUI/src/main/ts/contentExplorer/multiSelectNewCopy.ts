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

/**
 * Create → New Copy for every checked page or asset (#5247 / parent #4530).
 *
 * <p>One confirm posts the existing same-folder {@code newCopy} API once per
 * page or asset. Folders are named and not copied. The folder list is told
 * about a copy only after that item's POST succeeds. HTTP 400, 403, or 409
 * on one item does not claim the whole selection was copied.</p>
 */

import { isApiError } from "../api/client";
import type { PSPathItem } from "../api/contentExplorer/types";
import { message } from "../i18n/message";
import { partitionStageSelection } from "./itemPublish";
import { EXPLORER_MSG } from "./messages";

export interface MultiNewCopyShown {
  sourceId: string;
  name: string;
  copyId?: string;
}

export interface MultiNewCopyFailure {
  name: string;
  status?: number;
  message: string;
}

export interface MultiNewCopyResult {
  copied: MultiNewCopyShown[];
  skippedFolderNames: string[];
  skippedOtherNames: string[];
  failures: MultiNewCopyFailure[];
}

export interface MultiNewCopyPlan {
  eligible: PSPathItem[];
  skippedFolderNames: string[];
  skippedOtherNames: string[];
}

export interface MultiNewCopyDispatch {
  refresh?: boolean;
  messageKey?: string;
  messageText?: string;
}

function itemLabel(item: PSPathItem): string {
  const name = (item.name ?? "").trim();
  if (name) {
    return name;
  }
  const path = (item.path ?? "").trim();
  if (path) {
    return path;
  }
  return (item.id ?? "").trim() || "item";
}

/** Pages and assets are copied. Folders and other rows are named and skipped. */
export function planMultiNewCopy(items: readonly PSPathItem[]): MultiNewCopyPlan {
  const plan = partitionStageSelection(items);
  return {
    eligible: plan.eligible,
    skippedFolderNames: plan.skippedFolders,
    skippedOtherNames: plan.skippedOther,
  };
}

function failureFrom(item: PSPathItem, err: unknown): MultiNewCopyFailure {
  const name = itemLabel(item);
  if (isApiError(err)) {
    return {
      name,
      status: err.status,
      message: `HTTP ${err.status}`,
    };
  }
  const text = err instanceof Error ? err.message : "new copy failed";
  return { name, message: text || "new copy failed" };
}

/**
 * Copy each eligible page or asset. {@code onCopied} runs only after that
 * item's copy returns, and never for a folder or a failed POST.
 */
export async function copyCheckedItems(
  items: readonly PSPathItem[],
  copyOne: (itemId: string) => Promise<{ itemId?: string } | void>,
  onCopied?: (copied: MultiNewCopyShown) => void | Promise<void>,
): Promise<MultiNewCopyResult> {
  const plan = planMultiNewCopy(items);
  const copied: MultiNewCopyShown[] = [];
  const failures: MultiNewCopyFailure[] = [];
  for (const item of plan.eligible) {
    const sourceId = (item.id ?? "").trim();
    try {
      const raw = await copyOne(sourceId);
      const copyId =
        raw && typeof raw.itemId === "string" && raw.itemId.trim()
          ? raw.itemId.trim()
          : undefined;
      const shown: MultiNewCopyShown = {
        sourceId,
        name: itemLabel(item),
        copyId,
      };
      copied.push(shown);
      if (onCopied) {
        await onCopied(shown);
      }
    } catch (err: unknown) {
      failures.push(failureFrom(item, err));
    }
  }
  return {
    copied,
    skippedFolderNames: plan.skippedFolderNames,
    skippedOtherNames: plan.skippedOtherNames,
    failures,
  };
}

/** Status text. A failure or a skipped folder is not a full-selection success. */
export function describeMultiNewCopy(
  result: MultiNewCopyResult,
): string | undefined {
  const parts: string[] = [];
  if (result.skippedFolderNames.length > 0) {
    parts.push(
      message(EXPLORER_MSG.NEW_COPY_SKIPPED_FOLDERS)
        .split("{names}")
        .join(result.skippedFolderNames.join(", ")),
    );
  }
  if (result.skippedOtherNames.length > 0) {
    parts.push(
      message(EXPLORER_MSG.NEW_COPY_SKIPPED_OTHER)
        .split("{names}")
        .join(result.skippedOtherNames.join(", ")),
    );
  }
  if (result.failures.length > 0) {
    const detail = result.failures
      .map((failure) =>
        failure.status != null
          ? `${failure.name} (HTTP ${failure.status})`
          : `${failure.name} (${failure.message})`,
      )
      .join("; ");
    parts.push(
      message(EXPLORER_MSG.NEW_COPY_BATCH_INCOMPLETE)
        .split("{detail}")
        .join(detail),
    );
  }
  if (parts.length === 0) {
    return undefined;
  }
  return parts.join(" ");
}

function dispatchFromResult(result: MultiNewCopyResult): MultiNewCopyDispatch {
  const messageText = describeMultiNewCopy(result);
  let messageKey: string | undefined;
  if (result.failures.length > 0) {
    messageKey = EXPLORER_MSG.NEW_COPY_BATCH_INCOMPLETE;
  } else if (result.skippedFolderNames.length > 0) {
    messageKey = EXPLORER_MSG.NEW_COPY_SKIPPED_FOLDERS;
  } else if (result.skippedOtherNames.length > 0) {
    messageKey = EXPLORER_MSG.NEW_COPY_SKIPPED_OTHER;
  }
  return {
    refresh: result.copied.length > 0,
    messageText,
    messageKey,
  };
}

/**
 * Confirm once, then copy. Cancel and a selection with no page or asset
 * do not call {@code copyOne}.
 */
export async function runMultiNewCopy(input: {
  items: readonly PSPathItem[];
  confirm: (body: string) => boolean;
  copyOne: (itemId: string) => Promise<{ itemId?: string } | void>;
  onItemCopied?: (copied: MultiNewCopyShown) => void | Promise<void>;
}): Promise<MultiNewCopyDispatch> {
  const plan = planMultiNewCopy(input.items);
  if (plan.eligible.length === 0) {
    const noted = describeMultiNewCopy({
      copied: [],
      skippedFolderNames: plan.skippedFolderNames,
      skippedOtherNames: plan.skippedOtherNames,
      failures: [],
    });
    return {
      messageKey: EXPLORER_MSG.NEW_COPY_NOTHING_ELIGIBLE,
      messageText: noted ?? message(EXPLORER_MSG.NEW_COPY_NOTHING_ELIGIBLE),
    };
  }
  const confirmBody = message(EXPLORER_MSG.CONFIRM_NEW_COPY_MULTI)
    .split("{count}")
    .join(String(plan.eligible.length));
  if (!input.confirm(confirmBody)) {
    return {};
  }
  const result = await copyCheckedItems(
    input.items,
    input.copyOne,
    input.onItemCopied,
  );
  return dispatchFromResult(result);
}
