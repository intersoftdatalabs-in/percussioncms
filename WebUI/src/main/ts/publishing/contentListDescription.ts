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
 * RXCONTENTLIST.DESCRIPTION is VARCHAR(255). Keep in step with
 * {@code PSPublishingDesignRestService.MAX_CONTENT_LIST_DESCRIPTION_LENGTH}.
 */
export const CONTENT_LIST_DESCRIPTION_MAX_LENGTH = 255;

export const CONTENT_LIST_DESCRIPTION_TOO_LONG =
  "Content list description must be 255 characters or fewer";

export type ContentListDescriptionResult =
  | { ok: true; description: string }
  | { ok: false; error: string };

/**
 * Blank (after trim) is allowed and clears the stored description.
 * Overlong text is rejected before any update request.
 */
export function validateContentListDescription(
  raw: string,
): ContentListDescriptionResult {
  const description = raw.trim();
  if (description.length > CONTENT_LIST_DESCRIPTION_MAX_LENGTH) {
    return { ok: false, error: CONTENT_LIST_DESCRIPTION_TOO_LONG };
  }
  return { ok: true, description };
}

/**
 * Description-only {@code updateContentList} body. Name, type, generator,
 * legacy URL, and item filter are omitted so the server leaves them stored.
 * An empty string clears the description. The id is the path parameter.
 */
export function buildContentListDescriptionBody(
  description: string,
): ContentListSummary {
  return { description };
}

/**
 * List to show after a description save succeeds. Prefer the refreshed rows.
 * If the refresh failed, keep every previous row and replace only the
 * description so the name, type, generator or URL, and item filter stay
 * visible. Do not call this when the update failed. An empty description
 * clears the stored text.
 */
export function contentListsAfterSuccessfulDescription(
  refreshed: ContentListSummary[] | null,
  contentListId: string,
  description: string,
  previous: ContentListSummary[],
): ContentListSummary[] {
  if (refreshed) {
    return refreshed;
  }
  return previous.map((row) =>
    row.contentListId === contentListId ? { ...row, description } : row,
  );
}
