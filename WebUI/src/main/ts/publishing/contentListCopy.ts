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

import type { ContentListSummary } from "../api/publishing/designApi";

/**
 * RXCONTENTLIST.NAME is VARCHAR(100). Keep in step with
 * {@code PSPublishingDesignRestService.MAX_CONTENT_LIST_NAME_LENGTH}.
 */
export const CONTENT_LIST_NAME_MAX_LENGTH = 100;

export const CONTENT_LIST_NAME_REQUIRED = "Name is required";

export const CONTENT_LIST_NAME_TOO_LONG =
  "Content list name must be 100 characters or fewer";

export type ContentListCopyNameResult =
  | { ok: true; name: string }
  | { ok: false; error: string };

/** Reject a blank or overlong copy name before any create request. */
export function validateContentListCopyName(
  raw: string,
): ContentListCopyNameResult {
  const name = raw.trim();
  if (!name) {
    return { ok: false, error: CONTENT_LIST_NAME_REQUIRED };
  }
  if (name.length > CONTENT_LIST_NAME_MAX_LENGTH) {
    return { ok: false, error: CONTENT_LIST_NAME_TOO_LONG };
  }
  return { ok: true, name };
}

/**
 * Suggested name for the copy form. Empty when the suggestion would exceed
 * {@link CONTENT_LIST_NAME_MAX_LENGTH}.
 */
export function suggestedContentListCopyName(
  sourceName: string | undefined,
): string {
  const base = (sourceName ?? "").trim();
  if (!base) {
    return "";
  }
  const suggestion = `${base} copy`;
  if (suggestion.length > CONTENT_LIST_NAME_MAX_LENGTH) {
    return "";
  }
  return suggestion;
}

/**
 * List to show after copy succeeds. Prefer the refreshed rows. If the refresh
 * failed or omitted the new row, append the created list so a successful copy
 * is visible. Do not call this when create failed. The source row already in
 * {@code previous} is kept when a failed refresh would otherwise drop it.
 */
export function contentListsAfterSuccessfulCopy(
  refreshed: ContentListSummary[] | null,
  created: ContentListSummary,
  previous: ContentListSummary[],
): ContentListSummary[] {
  const rows = [...(refreshed ?? previous)];
  const sourceIds = new Set(
    previous
      .map((row) => row.contentListId)
      .filter((id): id is string => !!id && id !== created.contentListId),
  );
  for (const id of sourceIds) {
    if (!rows.some((row) => row.contentListId === id)) {
      const source = previous.find((row) => row.contentListId === id);
      if (source) {
        rows.push(source);
      }
    }
  }
  const createdId = created.contentListId;
  const createdName = created.name?.trim();
  const present = rows.some((row) => {
    if (createdId && row.contentListId === createdId) {
      return true;
    }
    return !!createdName && row.name === createdName;
  });
  if (present) {
    return rows;
  }
  return [...rows, created];
}
