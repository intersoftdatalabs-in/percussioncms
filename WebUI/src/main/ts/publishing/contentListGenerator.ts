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
 * RXCONTENTLIST.GENERATOR is VARCHAR(256). Keep in step with
 * {@code PSPublishingDesignRestService.MAX_CONTENT_LIST_GENERATOR_LENGTH}.
 */
export const CONTENT_LIST_GENERATOR_MAX_LENGTH = 256;

export const CONTENT_LIST_GENERATOR_REQUIRED = "Content list generator is required";

export const CONTENT_LIST_GENERATOR_TOO_LONG =
  "Content list generator must be 256 characters or fewer";

export type ContentListGeneratorResult =
  | { ok: true; generator: string }
  | { ok: false; error: string };

/**
 * Blank (after trim) is rejected and must not be written. A blank generator
 * would drop a modern list toward legacy. Overlong text is rejected before
 * any update request.
 */
export function validateContentListGenerator(
  raw: string,
): ContentListGeneratorResult {
  const generator = raw.trim();
  if (generator.length === 0) {
    return { ok: false, error: CONTENT_LIST_GENERATOR_REQUIRED };
  }
  if (generator.length > CONTENT_LIST_GENERATOR_MAX_LENGTH) {
    return { ok: false, error: CONTENT_LIST_GENERATOR_TOO_LONG };
  }
  return { ok: true, generator };
}

/**
 * Generator-only {@code updateContentList} body. Name, description, type,
 * legacy URL, and item filter are omitted so the server leaves them stored.
 * The id is the path parameter.
 */
export function buildContentListGeneratorBody(
  generator: string,
): ContentListSummary {
  return { generator };
}

/**
 * List to show after a generator save succeeds. Prefer the refreshed rows.
 * If the refresh failed, keep every previous row and replace only the
 * generator so the name, description, type, URL, and item filter stay
 * visible. Do not call this when the update failed.
 */
export function contentListsAfterSuccessfulGenerator(
  refreshed: ContentListSummary[] | null,
  contentListId: string,
  generator: string,
  previous: ContentListSummary[],
): ContentListSummary[] {
  if (refreshed) {
    return refreshed;
  }
  return previous.map((row) =>
    row.contentListId === contentListId ? { ...row, generator } : row,
  );
}
