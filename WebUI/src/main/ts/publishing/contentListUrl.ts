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
 * RXCONTENTLIST.URL is VARCHAR(2100). Keep in step with
 * {@code PSPublishingDesignRestService.MAX_CONTENT_LIST_URL_LENGTH}.
 */
export const CONTENT_LIST_URL_MAX_LENGTH = 2100;

export const CONTENT_LIST_URL_REQUIRED = "Content list URL is required";

export const CONTENT_LIST_URL_TOO_LONG =
  "Content list URL must be 2100 characters or fewer";

export const CONTENT_LIST_URL_MODERN =
  "A modern content list does not use a legacy URL";

export type ContentListUrlResult =
  | { ok: true; url: string }
  | { ok: false; error: string };

/**
 * Blank (after trim) is rejected and must not be written. Overlong text is
 * rejected before any update request. A blank URL does not clear the stored
 * URL.
 */
export function validateContentListUrl(raw: string): ContentListUrlResult {
  const url = raw.trim();
  if (url.length === 0) {
    return { ok: false, error: CONTENT_LIST_URL_REQUIRED };
  }
  if (url.length > CONTENT_LIST_URL_MAX_LENGTH) {
    return { ok: false, error: CONTENT_LIST_URL_TOO_LONG };
  }
  return { ok: true, url };
}

/**
 * URL-only {@code updateContentList} body. Name, description, type, generator,
 * and item filter are omitted. The server leaves a null name and description
 * stored, so this does not resend them. The id is the path parameter.
 */
export function buildContentListUrlBody(url: string): ContentListSummary {
  return { url };
}

/**
 * List to show after a legacy URL save succeeds. Prefer the refreshed rows.
 * If the refresh failed, keep every previous row and replace only the URL so
 * the name, description, type, and item filter stay visible. Do not call this
 * when the update failed. The new URL is not applied until save succeeds.
 */
export function contentListsAfterSuccessfulUrl(
  refreshed: ContentListSummary[] | null,
  contentListId: string,
  url: string,
  previous: ContentListSummary[],
): ContentListSummary[] {
  if (refreshed) {
    return refreshed;
  }
  return previous.map((row) =>
    row.contentListId === contentListId ? { ...row, url } : row,
  );
}
