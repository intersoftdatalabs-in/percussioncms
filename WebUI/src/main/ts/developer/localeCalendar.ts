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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {
  normalizeLanguageString,
  unwrapLocaleDetail,
  type LocaleWriteBody,
} from "../api/developer/localesApi";
import type { LocaleDetail, LocaleFormatSummary } from "../api/developer/types";

/**
 * Matches the server bound for {@code format.calendar}
 * ({@code LocalesAdaptor.validateMaxLen("calendar", …, 32)}).
 */
export const LOCALE_CALENDAR_MAX_LENGTH = 32;

export type LocaleCalendarWrite = { format: { calendar: string } };

export type LocaleCalendarRejection = "unchanged" | "invalid";

const OMITTED_ON_CALENDAR_WRITE = [
  "languageString",
  "label",
  "description",
  "status",
  "baseLocale",
  "hasFormatProfile",
] as const;

const OMITTED_FORMAT_FIELDS = [
  "languageString",
  "textDir",
  "datePattern",
  "timePattern",
  "dateTimePattern",
  "decimalSep",
  "groupingSep",
  "currencyCode",
  "currencyPattern",
  "firstDayOfWeek",
  "measurementSystem",
  "defaultTz",
  "numberingSystem",
] as const;

/** Display form of a stored calendar. Blank is {@code ""}. */
export function storedLocaleCalendar(value?: string | null): string {
  return (value ?? "").replace(/[\r\n]+/g, " ").trim();
}

/**
 * Parsed calendar: the stored value, {@code null} to clear, or
 * {@code "invalid"} when the value is longer than the server allows.
 */
export function parseLocaleCalendar(
  value?: string | null,
): string | null | "invalid" {
  const raw = storedLocaleCalendar(value);
  if (raw === "") {
    return null;
  }
  if (raw.length > LOCALE_CALENDAR_MAX_LENGTH) {
    return "invalid";
  }
  return raw;
}

/**
 * Body for the existing locale PUT that sets the calendar.
 *
 * <p>Language string, label, description, status, and other format fields are
 * omitted. A blank value is an empty string and clears a stored calendar.
 * A value longer than {@link LOCALE_CALENDAR_MAX_LENGTH} is not
 * a write.</p>
 */
export function localeCalendarWrite(
  baseline: Pick<LocaleDetail, "format">,
  nextCalendar: string,
): LocaleCalendarWrite | LocaleCalendarRejection {
  const calendar = parseLocaleCalendar(nextCalendar);
  if (calendar === "invalid") {
    return "invalid";
  }
  const previous = parseLocaleCalendar(baseline.format?.calendar);
  if (previous !== "invalid" && calendar === previous) {
    return "unchanged";
  }
  return { format: { calendar: calendar ?? "" } };
}

function storedStatus(value?: string | null): string {
  return (value ?? "").trim().toLowerCase();
}

function storedText(value?: string | null): string {
  return (value ?? "").replace(/[\r\n]+/g, " ").trim();
}

function otherFormatFingerprint(fmt?: LocaleFormatSummary | null): string {
  const src: LocaleFormatSummary = fmt || {};
  return [
    src.textDir || "",
    src.datePattern || "",
    src.timePattern || "",
    src.dateTimePattern || "",
    src.decimalSep || "",
    src.groupingSep || "",
    src.currencyCode || "",
    src.currencyPattern || "",
    src.firstDayOfWeek == null ? "" : String(src.firstDayOfWeek),
    src.measurementSystem || "",
    src.defaultTz || "",
    src.numberingSystem || "",
  ].join("");
}

function sentOmitsOtherFormatFields(fmt: LocaleFormatSummary): boolean {
  for (const key of OMITTED_FORMAT_FIELDS) {
    if (key in fmt) {
      return false;
    }
  }
  return true;
}

/**
 * Calendar to show after a calendar-only update, or null when
 * the response must not replace the previous value.
 */
export function savedLocaleCalendar(
  sent: LocaleWriteBody,
  previous: LocaleDetail,
  payload: unknown,
): string | null {
  for (const key of OMITTED_ON_CALENDAR_WRITE) {
    if (key in sent) {
      return null;
    }
  }
  if (!("format" in sent) || sent.format == null || typeof sent.format !== "object") {
    return null;
  }
  if (
    !("calendar" in sent.format) ||
    !sentOmitsOtherFormatFields(sent.format)
  ) {
    return null;
  }
  const saved = unwrapLocaleDetail(payload);
  if (!saved || typeof saved !== "object") {
    return null;
  }
  const sentCalendar = parseLocaleCalendar(sent.format.calendar);
  if (sentCalendar === "invalid") {
    return null;
  }
  const savedCalendar = parseLocaleCalendar(saved.format?.calendar);
  if (
    savedCalendar === "invalid" ||
    savedCalendar !== sentCalendar
  ) {
    return null;
  }
  const savedLang = normalizeLanguageString(saved.languageString);
  const previousLang = normalizeLanguageString(previous.languageString);
  if (!savedLang || (previousLang && savedLang !== previousLang)) {
    return null;
  }
  if (storedText(saved.label) !== storedText(previous.label)) {
    return null;
  }
  if (storedText(saved.description) !== storedText(previous.description)) {
    return null;
  }
  const previousStatus = storedStatus(previous.status);
  if (previousStatus && storedStatus(saved.status) !== previousStatus) {
    return null;
  }
  if ((previous.baseLocale === true) !== (saved.baseLocale === true)) {
    return null;
  }
  if (
    otherFormatFingerprint(previous.format) !== otherFormatFingerprint(saved.format)
  ) {
    return null;
  }
  return sentCalendar == null ? "" : sentCalendar;
}
