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

/**
 * Explorer schedule publish dates — classic PercItemPublisherService
 * get/setScheduleDates (GET getitemdates/{id}, POST setitemdates with
 * {@code ItemDates} envelope). PublishingShell uses
 * {@code itemPublishPaths} + {@code itemScheduleDatesApi} (#4537).
 */

import { formatApiError, get, isApiError, post } from "../api/client";
import { asJsonRecord } from "../api/jsonList";
import { SERVICES_ROOT } from "../api/paths";
import type { PSPathItem } from "../api/contentExplorer/types";
import { message } from "../i18n/message";
import { mapPublishResponse } from "../publishing/publishActions";
import { EXPLORER_MSG } from "./messages";
import { resolvePublishKind } from "./itemPublish";
import { isFolder } from "./selection";

export interface ItemScheduleDates {
  itemId: string;
  startDate: string;
  endDate: string;
  comments: string;
}

const SCHEDULE_ACTION_KEYS: ReadonlySet<string> = new Set([
  "schedule",
  "schedule_dates",
  "schedule_publish",
]);

const CLEAR_SCHEDULE_ACTION_KEYS: ReadonlySet<string> = new Set([
  "clear_scheduled_dates",
  "clear_schedule_dates",
]);

const ROW_START_KEYS = ["startdate", "sys_contentstartdate", "publishdate"];
const ROW_END_KEYS = ["enddate", "sys_contentexpirydate", "removaldate"];

function actionNameKey(name: string | undefined | null): string {
  return (name ?? "").replace(/[\s-]/g, "_").toLowerCase();
}

/** Catalog / toolbar names for Explorer Schedule (Finder “Schedule”). */
export function isScheduleActionName(name: string | undefined | null): boolean {
  return SCHEDULE_ACTION_KEYS.has(actionNameKey(name));
}

/** Clear of both publish and removal dates (#4968, multi-select #4987). Not Schedule. */
export function isClearScheduledDatesActionName(
  name: string | undefined | null,
): boolean {
  return CLEAR_SCHEDULE_ACTION_KEYS.has(actionNameKey(name));
}

function displayProp(item: PSPathItem, keys: readonly string[]): string {
  const props = item.displayProperties;
  if (!props || typeof props !== "object") {
    return "";
  }
  for (const [key, value] of Object.entries(props)) {
    if (!keys.includes(key.toLowerCase())) {
      continue;
    }
    if (value == null) {
      continue;
    }
    const text = String(value).trim();
    if (text) {
      return text;
    }
  }
  return "";
}

/** Dates shown on a folder-list row when the path payload includes them. */
export function rowScheduleDates(item: PSPathItem): {
  startDate: string;
  endDate: string;
} {
  return {
    startDate: displayProp(item, ROW_START_KEYS),
    endDate: displayProp(item, ROW_END_KEYS),
  };
}

/** Message for a failed clear. HTTP 400/403/409 stay failures, not success. */
export function clearScheduleFailureMessage(err: unknown): string {
  return formatApiError(err, message(EXPLORER_MSG.CLEAR_SCHEDULE_FAILED));
}

function itemDatesRoot(root = SERVICES_ROOT): {
  getDates: string;
  setDates: string;
} {
  const base = `${root}/itemmanagement/item`;
  return {
    getDates: `${base}/getitemdates`,
    setDates: `${base}/setitemdates`,
  };
}

function asText(value: unknown): string {
  if (value == null) {
    return "";
  }
  return String(value).trim();
}

/**
 * Unwrap JAXB {@code ItemDates} (classic {@code eval(result).ItemDates}).
 */
export function parseItemScheduleDates(
  body: unknown,
  fallbackItemId = "",
): ItemScheduleDates {
  const rec = asJsonRecord(body);
  const inner =
    rec != null && asJsonRecord(rec.ItemDates)
      ? (rec.ItemDates as Record<string, unknown>)
      : rec;
  if (!inner) {
    return {
      itemId: fallbackItemId,
      startDate: "",
      endDate: "",
      comments: "",
    };
  }
  return {
    itemId: asText(inner.itemId) || fallbackItemId,
    startDate: asText(inner.startDate),
    endDate: asText(inner.endDate),
    comments: asText(inner.comments),
  };
}

const SERVER_DATE_RE =
  /^(\d{1,2})\/(\d{1,2})\/(\d{4})\s+(\d{1,2}):(\d{2})\s*(am|pm)$/i;
const LOCAL_DATE_RE = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})$/;

function pad2(n: number): string {
  return n < 10 ? `0${n}` : String(n);
}

/**
 * Parse classic {@code MM/dd/yyyy hh:mm a} (GET lowercases am/pm).
 * Local calendar fields — not UTC ISO.
 */
export function parseServerScheduleDate(value: string): Date | null {
  const trimmed = value.trim();
  if (!trimmed) {
    return null;
  }
  const m = SERVER_DATE_RE.exec(trimmed);
  if (!m) {
    return null;
  }
  const month = Number(m[1]);
  const day = Number(m[2]);
  const year = Number(m[3]);
  let hour = Number(m[4]);
  const minute = Number(m[5]);
  const ampm = m[6].toLowerCase();
  if (month < 1 || month > 12 || day < 1 || day > 31 || hour < 1 || hour > 12) {
    return null;
  }
  if (ampm === "pm" && hour < 12) {
    hour += 12;
  }
  if (ampm === "am" && hour === 12) {
    hour = 0;
  }
  const d = new Date(year, month - 1, day, hour, minute, 0, 0);
  if (Number.isNaN(d.getTime())) {
    return null;
  }
  return d;
}

/** Format local date as {@code MM/dd/yyyy hh:mm a} (lowercase am/pm). */
export function formatServerScheduleDate(value: Date): string {
  let hour = value.getHours();
  const ampm = hour >= 12 ? "pm" : "am";
  hour = hour % 12;
  if (hour === 0) {
    hour = 12;
  }
  return `${pad2(value.getMonth() + 1)}/${pad2(value.getDate())}/${value.getFullYear()} ${pad2(hour)}:${pad2(value.getMinutes())} ${ampm}`;
}

/** {@code datetime-local} value from a server date string. */
export function serverDateToDatetimeLocal(value: string): string {
  const d = parseServerScheduleDate(value);
  if (!d) {
    return "";
  }
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}T${pad2(d.getHours())}:${pad2(d.getMinutes())}`;
}

/** Server date string from a {@code datetime-local} value. */
export function datetimeLocalToServerDate(value: string): string {
  const trimmed = value.trim();
  if (!trimmed) {
    return "";
  }
  const m = LOCAL_DATE_RE.exec(trimmed);
  if (!m) {
    return "";
  }
  const d = new Date(
    Number(m[1]),
    Number(m[2]) - 1,
    Number(m[3]),
    Number(m[4]),
    Number(m[5]),
    0,
    0,
  );
  if (Number.isNaN(d.getTime())) {
    return "";
  }
  return formatServerScheduleDate(d);
}

export function validateScheduleDateRange(
  startDate: string,
  endDate: string,
): string | null {
  const start = startDate.trim() ? parseServerScheduleDate(startDate) : null;
  const end = endDate.trim() ? parseServerScheduleDate(endDate) : null;
  if (startDate.trim() && !start) {
    return EXPLORER_MSG.SCHEDULE_DATE_RANGE;
  }
  if (endDate.trim() && !end) {
    return EXPLORER_MSG.SCHEDULE_DATE_RANGE;
  }
  if (start && end) {
    if (start.getTime() === end.getTime()) {
      return EXPLORER_MSG.SCHEDULE_DATES_SAME;
    }
    if (start.getTime() > end.getTime()) {
      return EXPLORER_MSG.SCHEDULE_DATE_RANGE;
    }
  }
  return null;
}

export async function getItemScheduleDates(
  itemId: string,
): Promise<ItemScheduleDates> {
  const id = itemId.trim();
  if (!id) {
    return { itemId: "", startDate: "", endDate: "", comments: "" };
  }
  const body = await get<unknown>(
    `${itemDatesRoot().getDates}/${encodeURIComponent(id)}`,
  );
  return parseItemScheduleDates(body, id);
}

/**
 * POST {@code ItemDates}. HTTP 200 {@code FORBIDDEN}/{@code BADCONFIG}/
 * {@code INVALID} is a failure ({@code mapPublishResponse}).
 */
export async function setItemScheduleDates(
  dates: ItemScheduleDates,
): Promise<void> {
  const itemId = dates.itemId.trim();
  if (!itemId) {
    throw new Error("itemId");
  }
  const envelope = {
    ItemDates: {
      itemId,
      startDate: dates.startDate ?? "",
      endDate: dates.endDate ?? "",
      comments: dates.comments ?? "",
    },
  };
  const body = await post<unknown>(itemDatesRoot().setDates, envelope);
  const preflight = mapPublishResponse(body);
  if (preflight) {
    throw new Error(preflight.message || preflight.token || "Schedule failed");
  }
}

/**
 * Save schedule dates for a page or asset. Other types return false.
 */
export async function scheduleSelectedItem(
  item: PSPathItem,
  dates: ItemScheduleDates,
): Promise<boolean> {
  const batch = await scheduleSelectedItems([item], dates);
  return batch.saved === 1 && batch.failures.length === 0;
}

export interface ScheduleItemFailure {
  id: string;
  name: string;
  message: string;
}

/** Outcome of one dialog applied to a selection. Skipped rows are not failures. */
export interface ScheduleBatchResult {
  saved: number;
  skipped: number;
  failures: ScheduleItemFailure[];
}

/**
 * Pages and assets only. Folders and other non-publishable rows are omitted.
 * Duplicate ids are written once.
 */
export function publishableScheduleTargets(
  items: readonly PSPathItem[],
): PSPathItem[] {
  const seen = new Set<string>();
  const out: PSPathItem[] = [];
  for (const item of items) {
    const id = (item.id ?? "").trim();
    if (!id || seen.has(id)) {
      continue;
    }
    if (resolvePublishKind(item) === "none") {
      continue;
    }
    seen.add(id);
    out.push(item);
  }
  return out;
}

function failureMessage(err: unknown): string {
  if (err instanceof Error && err.message.trim()) {
    return err.message.trim();
  }
  if (isApiError(err)) {
    return formatApiError(err, "Schedule failed");
  }
  return "Schedule failed";
}

/** Folder labels skipped by a clear. Duplicate names are listed once. */
export function skippedClearFolderNames(items: readonly PSPathItem[]): string[] {
  const names: string[] = [];
  const seen = new Set<string>();
  for (const item of items) {
    if (!isFolder(item)) {
      continue;
    }
    const label =
      (item.name ?? "").trim() ||
      (item.path ?? "").trim() ||
      (item.id ?? "").trim();
    if (!label || seen.has(label)) {
      continue;
    }
    seen.add(label);
    names.push(label);
  }
  return names;
}

export interface ClearScheduleSelectionResult {
  saved: number;
  failures: ScheduleItemFailure[];
  skippedFolderNames: string[];
}

/**
 * Empty start and end on every publishable row. Reuses {@link setItemScheduleDates}.
 * A per-item HTTP or application-level failure does not stop the rest.
 */
export async function clearScheduledDatesOnSelection(
  items: readonly PSPathItem[],
): Promise<ClearScheduleSelectionResult> {
  const targets = publishableScheduleTargets(items);
  const failures: ScheduleItemFailure[] = [];
  let saved = 0;
  for (const item of targets) {
    const id = (item.id ?? "").trim();
    try {
      await setItemScheduleDates({
        itemId: id,
        startDate: "",
        endDate: "",
        comments: "",
      });
      saved += 1;
    } catch (err: unknown) {
      failures.push({
        id,
        name: (item.name ?? "").trim() || id,
        message: clearScheduleFailureMessage(err),
      });
    }
  }
  return {
    saved,
    failures,
    skippedFolderNames: skippedClearFolderNames(items),
  };
}

/** Operator text when folders were skipped or a clear was not complete. */
export function describeClearScheduleSelection(
  result: ClearScheduleSelectionResult,
): { messageText?: string; messageKey?: string; refresh: boolean } {
  const parts: string[] = [];
  if (result.skippedFolderNames.length > 0) {
    parts.push(
      message(EXPLORER_MSG.CLEAR_SCHEDULE_SKIPPED_FOLDERS)
        .split("{names}")
        .join(result.skippedFolderNames.join(", ")),
    );
  }
  if (result.failures.length > 0) {
    const detail = result.failures
      .map((failure) => `${failure.name} (${failure.message})`)
      .join("; ");
    parts.push(
      message(EXPLORER_MSG.CLEAR_SCHEDULE_PARTIAL).split("{detail}").join(detail),
    );
  }
  return {
    messageText: parts.length > 0 ? parts.join(" ") : undefined,
    messageKey:
      result.failures.length > 0
        ? EXPLORER_MSG.CLEAR_SCHEDULE_PARTIAL
        : result.skippedFolderNames.length > 0
          ? EXPLORER_MSG.CLEAR_SCHEDULE_SKIPPED_FOLDERS
          : undefined,
    refresh: result.saved > 0,
  };
}

/**
 * Write the same start/end (or clear) to every publishable row.
 * Continues after a per-item failure so a later row can still save.
 * Any failure means the batch is not a full success.
 */
export async function scheduleSelectedItems(
  items: readonly PSPathItem[],
  dates: ItemScheduleDates,
): Promise<ScheduleBatchResult> {
  const targets = publishableScheduleTargets(items);
  const failures: ScheduleItemFailure[] = [];
  let saved = 0;
  for (const item of targets) {
    const id = (item.id ?? "").trim();
    try {
      await setItemScheduleDates({ ...dates, itemId: id });
      saved += 1;
    } catch (err: unknown) {
      failures.push({
        id,
        name: (item.name ?? "").trim() || id,
        message: failureMessage(err),
      });
    }
  }
  return {
    saved,
    skipped: Math.max(0, items.length - targets.length),
    failures,
  };
}

/** Visible partial-failure text. Item names are data, not chrome. */
export function formatScheduleBatchFailure(result: ScheduleBatchResult): string {
  const detail = result.failures
    .map((f) => `${f.name} (${f.message})`)
    .join("; ");
  return `${message(EXPLORER_MSG.SCHEDULE_PARTIAL)} ${detail}`.trim();
}
