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

import {
  fetchItemScheduleDates,
  saveItemScheduleDates,
} from "../api/publishing/itemScheduleDatesApi";
import {
  formatApiError,
  isApiError,
} from "../api/client";
import type { ItemScheduleDates } from "../contentExplorer/itemScheduleDates";
import { parseServerScheduleDate } from "../contentExplorer/itemScheduleDates";
import { message, MSG } from "../i18n/message";
import { mapIdParam } from "./deepLinkMap";

/**
 * Item ids for one batch schedule run. Split first, then sanitize each
 * token ({@link mapIdParam} rejects comma/space-joined input as a whole).
 * Duplicates are written once, order is preserved.
 */
export function parseScheduleItemIds(
  raw: string | null | undefined,
): string[] {
  if (raw == null) {
    return [];
  }
  const seen = new Set<string>();
  const ids: string[] = [];
  for (const token of raw.split(/[\s,;]+/)) {
    const safe = mapIdParam(token);
    if (!safe || seen.has(safe)) {
      continue;
    }
    seen.add(safe);
    ids.push(safe);
  }
  return ids;
}

/** One item that did not save/load/clear, with its named reason. */
export interface ScheduleBatchFailure {
  id: string;
  message: string;
}

/** Outcome of one multi-item run. A per-item failure never stops the rest. */
export interface ScheduleBatchReport {
  total: number;
  saved: string[];
  failures: ScheduleBatchFailure[];
}

/** Visible per-item detail: `id (reason); …`. Item ids are data, not chrome. */
export function formatScheduleBatchDetail(
  failures: readonly ScheduleBatchFailure[],
): string {
  return failures.map((failure) => `${failure.id} (${failure.message})`).join("; ");
}

/** Named failure text for one schedule row (shared by single + batch). */
export function scheduleBatchFailureMessage(err: unknown): string {
  return failureMessage(err, message(MSG.PUBLISH_ERROR));
}

function failureMessage(err: unknown, fallback: string): string {
  if (err instanceof Error && err.message.trim()) {
    const text = err.message.trim();
    if (/\bFORBIDDEN\b/i.test(text)) {
      return message(MSG.PUBLISH_SCHEDULE_FORBIDDEN);
    }
    if (/\bBADCONFIG\b/i.test(text) || /\bINVALID\b/i.test(text)) {
      return message(MSG.PUBLISH_SCHEDULE_INVALID);
    }
    return text;
  }
  if (isApiError(err)) {
    if (err.status === 403) {
      return formatApiError(err, message(MSG.PUBLISH_SCHEDULE_FORBIDDEN));
    }
    if (err.status === 400) {
      return formatApiError(err, message(MSG.PUBLISH_SCHEDULE_INVALID));
    }
    if (err.status === 409) {
      return formatApiError(err, message(MSG.PUBLISH_SCHEDULE_CONFLICT));
    }
    return formatApiError(err, fallback);
  }
  return fallback;
}

export interface ScheduleBatchDates {
  startDate: string;
  endDate: string;
  comments: string;
}

/**
 * True when a reloaded schedule still carries the dates that were sent.
 * Empty matches empty; two set dates match when they parse to the same
 * instant (formatting-only drift such as `AM` vs `am` still counts).
 * Unparseable pairs fall back to a case-insensitive text compare so a
 * server echo in another format is not misread as a dropped save.
 * Comments are excluded: the dates lookup does not return them.
 */
export function scheduleDatesPersisted(
  sent: ScheduleBatchDates,
  stored: ItemScheduleDates,
): boolean {
  return (
    scheduleSidePersisted(sent.startDate, stored.startDate) &&
    scheduleSidePersisted(sent.endDate, stored.endDate)
  );
}

function scheduleSidePersisted(sent: string, stored: string): boolean {
  const a = sent.trim();
  const b = stored.trim();
  if (!a && !b) {
    return true;
  }
  if (!a || !b) {
    return false;
  }
  const pa = parseServerScheduleDate(a);
  const pb = parseServerScheduleDate(b);
  if (pa && pb) {
    return pa.getTime() === pb.getTime();
  }
  return a.toLowerCase() === b.toLowerCase();
}

/**
 * Save the same dates to every id, then reload each one (round-trip) so a
 * successful row is confirmed against the stored schedule. A row whose
 * reload no longer carries the sent dates counts as a failure with
 * {@code notSavedMessage}, mirroring the clear batch — a silent server
 * drop is never reported as saved. Continues after a per-item failure so
 * a later row can still save.
 */
export async function saveScheduleDatesBatch(
  ids: readonly string[],
  dates: ScheduleBatchDates,
  notSavedMessage: string,
  save: (dated: ItemScheduleDates) => Promise<void> = saveItemScheduleDates,
  fetch: (itemId: string) => Promise<ItemScheduleDates> = fetchItemScheduleDates,
): Promise<ScheduleBatchReport> {
  const saved: string[] = [];
  const failures: ScheduleBatchFailure[] = [];
  for (const id of ids) {
    try {
      await save({ ...dates, itemId: id });
      const reloaded = await fetch(id);
      if (scheduleDatesPersisted(dates, reloaded)) {
        saved.push(id);
      } else {
        failures.push({ id, message: notSavedMessage });
      }
    } catch (err: unknown) {
      failures.push({ id, message: failureMessage(err, "Schedule failed") });
    }
  }
  return { total: ids.length, saved, failures };
}

export interface ScheduleBatchLoaded {
  id: string;
  dates: ItemScheduleDates;
}

export interface ScheduleBatchLoadReport {
  total: number;
  loaded: ScheduleBatchLoaded[];
  failures: ScheduleBatchFailure[];
}

/** Load every id. A per-item load failure never stops the rest. */
export async function loadScheduleDatesBatch(
  ids: readonly string[],
  fetch: (itemId: string) => Promise<ItemScheduleDates> = fetchItemScheduleDates,
): Promise<ScheduleBatchLoadReport> {
  const loaded: ScheduleBatchLoaded[] = [];
  const failures: ScheduleBatchFailure[] = [];
  for (const id of ids) {
    try {
      loaded.push({ id, dates: await fetch(id) });
    } catch (err: unknown) {
      failures.push({ id, message: failureMessage(err, "Load failed") });
    }
  }
  return { total: ids.length, loaded, failures };
}

/**
 * Clear every id through the existing setitemdates route (empty dates).
 * A row counts as cleared only when the reload has neither date, mirroring
 * the single-item clear. {@code notClearedMessage} names the
 * still-scheduled outcome per item.
 */
export async function clearScheduleDatesBatch(
  ids: readonly string[],
  notClearedMessage: string,
  save: (dated: ItemScheduleDates) => Promise<void> = saveItemScheduleDates,
  fetch: (itemId: string) => Promise<ItemScheduleDates> = fetchItemScheduleDates,
): Promise<ScheduleBatchReport> {
  const saved: string[] = [];
  const failures: ScheduleBatchFailure[] = [];
  for (const id of ids) {
    try {
      await save({ itemId: id, startDate: "", endDate: "", comments: "" });
      const reloaded = await fetch(id);
      if (!reloaded.startDate && !reloaded.endDate) {
        saved.push(id);
      } else {
        failures.push({ id, message: notClearedMessage });
      }
    } catch (err: unknown) {
      failures.push({ id, message: failureMessage(err, "Clear failed") });
    }
  }
  return { total: ids.length, saved, failures };
}
