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
 * Parse CMS date / datetime field strings for native date widgets.
 * Itemmanagement stores calendar values as {@code yyyy-MM-dd} or
 * {@code yyyy-MM-dd HH:mm[:ss]} (optional {@code T} separator).
 */

export type EditorDateKind = "date" | "datetime";

const DATE_ONLY = /^(\d{4})-(\d{2})-(\d{2})$/;
const DATE_TIME =
  /^(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2})(?::(\d{2}))?$/;

function pad2(n: number): string {
  return String(n).padStart(2, "0");
}

function isValidYmd(year: number, month: number, day: number): boolean {
  if (month < 1 || month > 12 || day < 1 || day > 31) {
    return false;
  }
  const dt = new Date(Date.UTC(year, month - 1, day));
  return (
    dt.getUTCFullYear() === year &&
    dt.getUTCMonth() === month - 1 &&
    dt.getUTCDate() === day
  );
}

function isValidHms(hour: number, minute: number, second: number): boolean {
  return (
    hour >= 0 &&
    hour <= 23 &&
    minute >= 0 &&
    minute <= 59 &&
    second >= 0 &&
    second <= 59
  );
}

/** True when a non-empty value is not a valid calendar date/datetime. */
export function isInvalidEditorDate(
  kind: EditorDateKind,
  raw: string,
): boolean {
  const value = (raw ?? "").trim();
  if (!value) {
    return false;
  }
  return toWidgetValue(kind, value) === "";
}

/**
 * Widget value: {@code yyyy-MM-dd} or {@code yyyy-MM-ddTHH:mm}.
 * Unparseable input yields empty (native date inputs cannot display it).
 */
export function toWidgetValue(kind: EditorDateKind, raw: string): string {
  const value = (raw ?? "").trim();
  if (!value) {
    return "";
  }
  const dateOnly = DATE_ONLY.exec(value);
  if (dateOnly) {
    const y = Number(dateOnly[1]);
    const m = Number(dateOnly[2]);
    const d = Number(dateOnly[3]);
    if (!isValidYmd(y, m, d)) {
      return "";
    }
    const ymd = `${dateOnly[1]}-${dateOnly[2]}-${dateOnly[3]}`;
    return kind === "datetime" ? `${ymd}T00:00` : ymd;
  }
  const dateTime = DATE_TIME.exec(value);
  if (dateTime) {
    const y = Number(dateTime[1]);
    const m = Number(dateTime[2]);
    const d = Number(dateTime[3]);
    const h = Number(dateTime[4]);
    const min = Number(dateTime[5]);
    const s = Number(dateTime[6] ?? "0");
    if (!isValidYmd(y, m, d) || !isValidHms(h, min, s)) {
      return "";
    }
    const ymd = `${dateTime[1]}-${dateTime[2]}-${dateTime[3]}`;
    if (kind === "date") {
      return ymd;
    }
    return `${ymd}T${dateTime[4]}:${dateTime[5]}`;
  }
  return "";
}

/** Persist widget value as CMS field text. */
export function fromWidgetValue(kind: EditorDateKind, widget: string): string {
  const value = (widget ?? "").trim();
  if (!value) {
    return "";
  }
  if (kind === "date") {
    const m = DATE_ONLY.exec(value);
    if (!m) {
      return value;
    }
    const y = Number(m[1]);
    const mo = Number(m[2]);
    const d = Number(m[3]);
    if (!isValidYmd(y, mo, d)) {
      return value;
    }
    return `${m[1]}-${m[2]}-${m[3]}`;
  }
  const dt = DATE_TIME.exec(value.replace("T", " "));
  if (!dt) {
    const only = DATE_ONLY.exec(value);
    if (only && isValidYmd(Number(only[1]), Number(only[2]), Number(only[3]))) {
      return `${only[1]}-${only[2]}-${only[3]} 00:00:00`;
    }
    return value;
  }
  const y = Number(dt[1]);
  const mo = Number(dt[2]);
  const d = Number(dt[3]);
  const h = Number(dt[4]);
  const min = Number(dt[5]);
  const s = Number(dt[6] ?? "0");
  if (!isValidYmd(y, mo, d) || !isValidHms(h, min, s)) {
    return value;
  }
  return `${dt[1]}-${dt[2]}-${dt[3]} ${dt[4]}:${dt[5]}:${pad2(s)}`;
}
