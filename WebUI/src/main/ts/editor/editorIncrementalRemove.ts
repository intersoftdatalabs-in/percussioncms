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
 * Remove the open editor page or asset from the incremental queue (#5162).
 *
 * <p>Reuses the Explorer client
 * {@code POST …/incremental/explorer/{contentId}/remove}. There is no second
 * queue API. HTTP 400/403/409 throw and are not success. A template, folder,
 * or other non-page/non-asset returns false and does not call the server.
 * This is not unapprove.</p>
 */

import {
  formatApiError,
  isApiError,
  isSessionRedirectError,
} from "../api/client";
import { removeSelectedItemFromIncrementalQueue } from "../api/contentExplorer/incrementalApproveApi";
import { message } from "../i18n/message";
import type { EditorPublishKind } from "./editorPublish";
import { editorItemCanJoinIncrementalQueue } from "./editorIncrementalApprove";
import { EDITOR_MSG } from "./messages";

/**
 * Remove one open item from the incremental queue. Returns false when it
 * cannot be removed (no HTTP). Resolves true only after the Explorer remove
 * call succeeds.
 */
export async function removeEditorItemFromIncrementalQueue(
  itemId: string,
  kind: EditorPublishKind,
  remove: (
    contentId: string,
  ) => Promise<void> = removeSelectedItemFromIncrementalQueue,
): Promise<boolean> {
  const id = itemId.trim();
  if (!editorItemCanJoinIncrementalQueue(id, kind)) {
    return false;
  }
  await remove(id);
  return true;
}

/**
 * User-visible failure for incremental remove. Empty when the session is
 * already redirecting. HTTP 400/403/409 stay failures.
 */
export function editorIncrementalRemoveFailureMessage(err: unknown): string {
  if (isSessionRedirectError(err)) {
    return "";
  }
  if (isApiError(err)) {
    if (err.status === 403) {
      return formatApiError(err, message(EDITOR_MSG.REMOVE_INCREMENTAL_FORBIDDEN));
    }
    if (err.status === 400) {
      return formatApiError(err, message(EDITOR_MSG.REMOVE_INCREMENTAL_REJECTED));
    }
    if (err.status === 409) {
      return formatApiError(err, message(EDITOR_MSG.REMOVE_INCREMENTAL_CONFLICT));
    }
  }
  const text = formatApiError(err, message(EDITOR_MSG.REMOVE_INCREMENTAL_FAILED));
  if (/\bFORBIDDEN\b/i.test(text)) {
    return message(EDITOR_MSG.REMOVE_INCREMENTAL_FORBIDDEN);
  }
  if (/\bCONFLICT\b/i.test(text)) {
    return message(EDITOR_MSG.REMOVE_INCREMENTAL_CONFLICT);
  }
  return text;
}
