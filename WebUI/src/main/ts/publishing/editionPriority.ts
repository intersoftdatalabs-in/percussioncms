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

/** {@link IPSEdition.Priority} values. Lowest is 1, highest is 5. */
export const EDITION_PRIORITY_MIN = 1;
export const EDITION_PRIORITY_MAX = 5;

export const EDITION_PRIORITY_OUT_OF_RANGE =
  "Edition priority must be from 1 to 5";

export type EditionPriorityResult =
  | { ok: true; priority: number }
  | { ok: false; error: string };

/**
 * Accept a whole number from 1 to 5. Blank, zero, 6+, and non-integers are
 * rejected before any update request.
 */
export function validateEditionPriority(raw: string): EditionPriorityResult {
  const text = raw.trim();
  if (!/^[1-5]$/.test(text)) {
    return { ok: false, error: EDITION_PRIORITY_OUT_OF_RANGE };
  }
  return { ok: true, priority: Number(text) };
}

/**
 * Priority-only {@code updateEdition} body. Name, comment, and site id are
 * omitted so the server leaves them stored. Content-list order is a different
 * resource and is not included.
 */
export function buildEditionPriorityBody(priority: number): EditionSummary {
  return { priority };
}

/**
 * List to show after a priority save succeeds. Prefer the refreshed rows.
 * If the refresh failed, keep every previous row and replace only the
 * priority so the name and comment stay visible. Do not call this when the
 * update failed.
 */
export function editionsAfterSuccessfulPriority(
  refreshed: EditionSummary[] | null,
  editionId: string,
  priority: number,
  previous: EditionSummary[],
): EditionSummary[] {
  if (refreshed) {
    return refreshed;
  }
  return previous.map((row) =>
    row.editionId === editionId ? { ...row, priority } : row,
  );
}
