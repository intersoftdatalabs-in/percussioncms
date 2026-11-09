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
  datetimeLocalToServerDate,
  serverDateToDatetimeLocal,
} from "../contentExplorer/itemScheduleDates";

/** Stored schedule dates in server wire format ({@code ""} clears). */
export interface EditionScheduleDates {
  startDate: string;
  endDate: string;
}

/**
 * {@code datetime-local} inputs prefilled from the stored schedule.
 * Unscheduled sides stay blank; a blank field never writes by itself.
 */
export function editionScheduleInputDefaults(edition: EditionSummary): {
  startLocal: string;
  endLocal: string;
} {
  return {
    startLocal: serverDateToDatetimeLocal(edition.startDate ?? ""),
    endLocal: serverDateToDatetimeLocal(edition.endDate ?? ""),
  };
}

/**
 * Schedule-only {@code updateEdition} body. Name, priority, comment, and
 * site id are omitted so the server leaves them stored. An empty string
 * clears that side; content-list order is a different resource and is not
 * included. Both inputs must already be server-format dates (or blank).
 */
export function buildEditionScheduleBody(
  dates: EditionScheduleDates,
): EditionSummary {
  return { startDate: dates.startDate, endDate: dates.endDate };
}

/** Server-format dates from {@code datetime-local} inputs (blank stays blank). */
export function editionScheduleServerDates(
  startLocal: string,
  endLocal: string,
): EditionScheduleDates {
  return {
    startDate: datetimeLocalToServerDate(startLocal),
    endDate: datetimeLocalToServerDate(endLocal),
  };
}

/**
 * Row label for the stored schedule. Both dates, one side, or nothing
 * (unscheduled). Stored values are already server-format display strings.
 */
export function formatEditionScheduleLabel(
  startDate?: string | null,
  endDate?: string | null,
): string {
  const start = (startDate ?? "").trim();
  const end = (endDate ?? "").trim();
  if (start && end) {
    return `${start} → ${end}`;
  }
  if (start) {
    return `from ${start}`;
  }
  if (end) {
    return `until ${end}`;
  }
  return "";
}

/**
 * List to show after a schedule save succeeds. Prefer the refreshed rows.
 * If the refresh failed, keep every previous row and replace only the
 * schedule so the name, priority, and comment stay visible. Do not call
 * this when the update failed.
 */
export function editionsAfterSuccessfulSchedule(
  refreshed: EditionSummary[] | null,
  editionId: string,
  dates: EditionScheduleDates,
  previous: EditionSummary[],
): EditionSummary[] {
  if (refreshed) {
    return refreshed;
  }
  return previous.map((row) =>
    row.editionId === editionId
      ? { ...row, startDate: dates.startDate, endDate: dates.endDate }
      : row,
  );
}
