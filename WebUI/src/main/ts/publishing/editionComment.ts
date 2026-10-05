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

import type { EditionSummary } from "../api/publishing/designApi";

/**
 * Blank and whitespace-only text clears the stored comment. Other text is
 * trimmed so a padded field does not keep accidental edge spaces.
 */
export function normalizeEditionComment(raw: string): string {
  return raw.trim();
}

/**
 * Comment-only {@code updateEdition} body. Name, priority, and site id are
 * omitted so the server leaves them stored. An empty string is included so a
 * blank field clears the stored comment (an omitted comment would leave it).
 * Content-list order is a different resource and is not included.
 */
export function buildEditionCommentBody(comment: string): EditionSummary {
  return { comment };
}

/**
 * List to show after a comment save succeeds. Prefer the refreshed rows.
 * If the refresh failed, keep every previous row and replace only the comment
 * so the name and priority stay visible. Do not call this when the update
 * failed.
 */
export function editionsAfterSuccessfulComment(
  refreshed: EditionSummary[] | null,
  editionId: string,
  comment: string,
  previous: EditionSummary[],
): EditionSummary[] {
  if (refreshed) {
    return refreshed;
  }
  return previous.map((row) =>
    row.editionId === editionId ? { ...row, comment } : row,
  );
}
