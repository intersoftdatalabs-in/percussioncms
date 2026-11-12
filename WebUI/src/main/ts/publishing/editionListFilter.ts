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
 * Schedule facet for the Design editions list (operator filter).
 * {@code scheduled} keeps editions with an embargo (start) or expiry (end)
 * date; {@code unscheduled} keeps editions with neither side set.
 */
export type EditionScheduleFilter = "all" | "scheduled" | "unscheduled";

/** True when either schedule side carries a stored (wire-format) date. */
export function editionIsScheduled(edition: EditionSummary): boolean {
  return (
    (edition.startDate ?? "").trim() !== "" ||
    (edition.endDate ?? "").trim() !== ""
  );
}

function editionSearchHaystack(edition: EditionSummary): string {
  return [edition.name, edition.comment]
    .filter((v) => v != null && String(v).trim() !== "")
    .join(" ")
    .toLowerCase();
}

/**
 * Narrow already-loaded editions by name/comment substring
 * (case-insensitive). Blank query returns the original list.
 */
export function filterEditionsBySearch(
  editions: EditionSummary[],
  query: string,
): EditionSummary[] {
  const q = query.trim().toLowerCase();
  if (!q) {
    return editions;
  }
  return editions.filter((e) => editionSearchHaystack(e).includes(q));
}

/**
 * Narrow already-loaded editions by schedule facet. {@code all} returns
 * the original list.
 */
export function filterEditionsBySchedule(
  editions: EditionSummary[],
  mode: EditionScheduleFilter,
): EditionSummary[] {
  if (mode === "scheduled") {
    return editions.filter((e) => editionIsScheduled(e));
  }
  if (mode === "unscheduled") {
    return editions.filter((e) => !editionIsScheduled(e));
  }
  return editions;
}

/**
 * Combined operator filter for the Design editions list: schedule facet
 * first, then the search query. Returns the original list when both
 * controls are at rest.
 */
export function filterDesignEditions(
  editions: EditionSummary[],
  query: string,
  mode: EditionScheduleFilter,
): EditionSummary[] {
  return filterEditionsBySearch(filterEditionsBySchedule(editions, mode), query);
}
