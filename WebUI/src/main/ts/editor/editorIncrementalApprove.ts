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
 * Approve the open editor page or asset onto the incremental queue (#5124).
 *
 * <p>Reuses the Explorer client
 * {@code POST …/incremental/explorer/{contentId}/approve}. There is no second
 * queue API. HTTP 400/403/409 throw and are not success. A template or other
 * non-page/non-asset returns false and does not call the server.</p>
 */

import {
  formatApiError,
  isApiError,
  isSessionRedirectError,
} from "../api/client";
import { approveSelectedItemToIncrementalQueue } from "../api/contentExplorer/incrementalApproveApi";
import { message } from "../i18n/message";
import type { EditorPublishKind } from "./editorPublish";
import { EDITOR_MSG } from "./messages";

/**
 * True when the open item is a page or asset that can be queued.
 * Templates, folders, and a missing id are not queueable.
 */
export function editorItemCanJoinIncrementalQueue(
  itemId: string,
  kind: EditorPublishKind,
): boolean {
  return itemId.trim().length > 0 && (kind === "page" || kind === "asset");
}

/**
 * Approve one open item. Returns false when it cannot be queued (no HTTP).
 * Resolves true only after the Explorer approve call succeeds.
 */
export async function approveEditorItemToIncrementalQueue(
  itemId: string,
  kind: EditorPublishKind,
  approve: (contentId: string) => Promise<void> = approveSelectedItemToIncrementalQueue,
): Promise<boolean> {
  const id = itemId.trim();
  if (!editorItemCanJoinIncrementalQueue(id, kind)) {
    return false;
  }
  await approve(id);
  return true;
}

/**
 * User-visible failure for incremental approve. Empty when the session is
 * already redirecting. HTTP 400/403/409 stay failures.
 */
export function editorIncrementalApproveFailureMessage(err: unknown): string {
  if (isSessionRedirectError(err)) {
    return "";
  }
  if (isApiError(err)) {
    if (err.status === 403) {
      return formatApiError(err, message(EDITOR_MSG.APPROVE_INCREMENTAL_FORBIDDEN));
    }
    if (err.status === 400) {
      return formatApiError(err, message(EDITOR_MSG.APPROVE_INCREMENTAL_REJECTED));
    }
    if (err.status === 409) {
      return formatApiError(err, message(EDITOR_MSG.APPROVE_INCREMENTAL_CONFLICT));
    }
  }
  const text = formatApiError(err, message(EDITOR_MSG.APPROVE_INCREMENTAL_FAILED));
  if (/\bFORBIDDEN\b/i.test(text)) {
    return message(EDITOR_MSG.APPROVE_INCREMENTAL_FORBIDDEN);
  }
  if (/\bCONFLICT\b/i.test(text)) {
    return message(EDITOR_MSG.APPROVE_INCREMENTAL_CONFLICT);
  }
  return text;
}
