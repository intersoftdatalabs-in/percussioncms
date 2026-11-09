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
import {
  EDITION_PRIORITY_MAX,
  EDITION_PRIORITY_MIN,
  buildEditionPriorityBody,
} from "./editionPriority";

/**
 * Step direction for edition priority reorder. Up raises the stored priority
 * (toward {@link EDITION_PRIORITY_MAX}); down lowers it.
 */
export type EditionReorderDirection = -1 | 1;

/**
 * Priority-only {@code updateEdition} body that moves one edition up or down.
 * Returns {@code null} when the stored priority is missing or the step would
 * leave 1–5, so the caller disables the button instead of posting.
 * Name, comment, and site id are omitted so the server leaves them stored.
 * Content-list order is a different resource and is not included.
 */
export function buildEditionReorderBody(
  priority: number | null | undefined,
  direction: EditionReorderDirection,
): EditionSummary | null {
  if (priority == null || !Number.isInteger(priority)) {
    return null;
  }
  const next = priority + direction;
  if (next < EDITION_PRIORITY_MIN || next > EDITION_PRIORITY_MAX) {
    return null;
  }
  return buildEditionPriorityBody(next);
}

/**
 * Editions list order for the Design Editions tab: stored priority, highest
 * first, so Move up/down visibly reorders. Rows without a stored priority
 * sort last; name then id break ties so the order is stable across reloads.
 * Does not mutate the input.
 */
export function sortEditionsByPriority(
  editions: EditionSummary[],
): EditionSummary[] {
  return editions
    .slice()
    .sort((a, b) => {
      const pa = a.priority ?? Number.NEGATIVE_INFINITY;
      const pb = b.priority ?? Number.NEGATIVE_INFINITY;
      if (pa !== pb) {
        return pb - pa;
      }
      const na = (a.name ?? "").localeCompare(b.name ?? "");
      if (na !== 0) {
        return na;
      }
      return String(a.editionId ?? "").localeCompare(String(b.editionId ?? ""));
    });
}

/**
 * List to show after an edition reorder save succeeds. Prefer the refreshed
 * rows (re-sorted, so the moved edition lands in its stored position). If the
 * refresh failed, keep every previous row, replace only the moved priority,
 * and re-sort so the order still reflects the write. Do not call this when
 * the update failed.
 */
export function editionsAfterSuccessfulReorder(
  refreshed: EditionSummary[] | null,
  editionId: string,
  priority: number,
  previous: EditionSummary[],
): EditionSummary[] {
  if (refreshed) {
    return sortEditionsByPriority(refreshed);
  }
  return sortEditionsByPriority(
    previous.map((row) =>
      row.editionId === editionId ? { ...row, priority } : row,
    ),
  );
}
