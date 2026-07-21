/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

import type { PublishingJob } from "./types";

export type StatusSortKey =
  | "siteName"
  | "status"
  | "serverName"
  | "completedItems"
  | "startDate"
  | "elapsedTime";

export type SortDirection = "asc" | "desc";

export interface StatusSortState {
  key: StatusSortKey;
  direction: SortDirection;
}

function cellValue(job: PublishingJob, key: StatusSortKey): string | number {
  switch (key) {
    case "siteName":
      return String(job.siteName ?? "");
    case "status":
      return String(job.status ?? "");
    case "serverName":
      return String(job.serverName ?? job.pubServerName ?? "");
    case "completedItems": {
      const n = job.completedItems;
      return typeof n === "number" ? n : Number(n) || 0;
    }
    case "startDate":
      return String(job.startDate ?? job.startTime ?? "");
    case "elapsedTime": {
      const n = job.elapsedTime;
      return typeof n === "number" ? n : Number(n) || 0;
    }
    default:
      return "";
  }
}

function compareValues(a: string | number, b: string | number): number {
  if (typeof a === "number" && typeof b === "number") {
    return a - b;
  }
  return String(a).localeCompare(String(b), undefined, {
    numeric: true,
    sensitivity: "base",
  });
}

/** Immutable sort of job rows by column + direction. */
export function sortJobs(
  jobs: PublishingJob[],
  state: StatusSortState,
): PublishingJob[] {
  const dir = state.direction === "asc" ? 1 : -1;
  return [...jobs].sort((left, right) => {
    const cmp = compareValues(
      cellValue(left, state.key),
      cellValue(right, state.key),
    );
    return cmp * dir;
  });
}

/** Toggle sort: same key flips direction; new key starts ascending. */
export function nextSortState(
  current: StatusSortState,
  key: StatusSortKey,
): StatusSortState {
  if (current.key === key) {
    return {
      key,
      direction: current.direction === "asc" ? "desc" : "asc",
    };
  }
  return { key, direction: "asc" };
}

export function sortIndicator(
  current: StatusSortState,
  key: StatusSortKey,
): string {
  if (current.key !== key) {
    return "";
  }
  return current.direction === "asc" ? " ▲" : " ▼";
}
