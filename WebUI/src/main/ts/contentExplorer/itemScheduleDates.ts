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
 * {@code ItemDates} envelope). Paths stay here so PublishingShell
 * {@code itemPublishPaths} is not shared with #4537.
 */

import { get, post } from "../api/client";
import { asJsonRecord } from "../api/jsonList";
import { SERVICES_ROOT } from "../api/paths";
import type { PSPathItem } from "../api/contentExplorer/types";
import { mapPublishResponse } from "../publishing/publishActions";
import { EXPLORER_MSG } from "./messages";
import { resolvePublishKind } from "./itemPublish";

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

function actionNameKey(name: string | undefined | null): string {
  return (name ?? "").replace(/[\s-]/g, "_").toLowerCase();
}

/** Catalog / toolbar names for Explorer Schedule (Finder “Schedule”). */
export function isScheduleActionName(name: string | undefined | null): boolean {
  return SCHEDULE_ACTION_KEYS.has(actionNameKey(name));
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
  const id = (item.id ?? "").trim();
  if (!id) {
    return false;
  }
  if (resolvePublishKind(item) === "none") {
    return false;
  }
  await setItemScheduleDates({ ...dates, itemId: id });
  return true;
}
