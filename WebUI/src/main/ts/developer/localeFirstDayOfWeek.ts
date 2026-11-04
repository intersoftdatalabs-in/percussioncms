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
  normalizeLanguageString,
  unwrapLocaleDetail,
  type LocaleWriteBody,
} from "../api/developer/localesApi";
import type { LocaleDetail, LocaleFormatSummary } from "../api/developer/types";

export type LocaleFirstDayOfWeekWrite = { format: { firstDayOfWeek: number | null } };

export type LocaleFirstDayOfWeekRejection = "unchanged" | "invalid";

const OMITTED_ON_FIRST_DAY_WRITE = [
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
  "measurementSystem",
  "defaultTz",
  "numberingSystem",
  "calendar",
] as const;

/** Display form of a stored first day of week. Blank is {@code ""}. */
export function storedLocaleFirstDayOfWeek(value?: number | string | null): string {
  if (value == null) {
    return "";
  }
  if (typeof value === "number") {
    return Number.isFinite(value) ? String(value) : "";
  }
  return value.replace(/[\r\n]+/g, " ").trim();
}

/**
 * Parsed first day of week: integer 1–7, {@code null} to clear, or {@code "invalid"}.
 */
export function parseLocaleFirstDayOfWeek(
  value?: number | string | null,
): number | null | "invalid" {
  if (value == null) {
    return null;
  }
  const raw =
    typeof value === "number" ? (Number.isFinite(value) ? String(value) : "") : storedLocaleFirstDayOfWeek(value);
  if (raw === "") {
    return null;
  }
  if (!/^[1-7]$/.test(raw)) {
    return "invalid";
  }
  return Number(raw);
}

/**
 * Body for the existing locale PUT that sets the first day of week.
 *
 * <p>Language string, label, description, status, and other format fields are
 * omitted. A blank value is JSON {@code null} and clears a stored first day.
 * Out-of-range values are not a write.
 */
export function localeFirstDayOfWeekWrite(
  baseline: Pick<LocaleDetail, "format">,
  nextFirstDay: string,
): LocaleFirstDayOfWeekWrite | LocaleFirstDayOfWeekRejection {
  const firstDayOfWeek = parseLocaleFirstDayOfWeek(nextFirstDay);
  if (firstDayOfWeek === "invalid") {
    return "invalid";
  }
  const previous = parseLocaleFirstDayOfWeek(baseline.format?.firstDayOfWeek);
  if (previous !== "invalid" && firstDayOfWeek === previous) {
    return "unchanged";
  }
  return { format: { firstDayOfWeek } };
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
    src.measurementSystem || "",
    src.defaultTz || "",
    src.numberingSystem || "",
    src.calendar || "",
  ].join("\u0001");
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
 * First day of week to show after a first-day-only update, or null when the
 * response must not replace the previous value.
 */
export function savedLocaleFirstDayOfWeek(
  sent: LocaleWriteBody,
  previous: LocaleDetail,
  payload: unknown,
): string | null {
  for (const key of OMITTED_ON_FIRST_DAY_WRITE) {
    if (key in sent) {
      return null;
    }
  }
  if (!("format" in sent) || sent.format == null || typeof sent.format !== "object") {
    return null;
  }
  if (!("firstDayOfWeek" in sent.format) || !sentOmitsOtherFormatFields(sent.format)) {
    return null;
  }
  const saved = unwrapLocaleDetail(payload);
  if (!saved || typeof saved !== "object") {
    return null;
  }
  const sentDay = parseLocaleFirstDayOfWeek(sent.format.firstDayOfWeek);
  if (sentDay === "invalid") {
    return null;
  }
  const savedDay = parseLocaleFirstDayOfWeek(saved.format?.firstDayOfWeek);
  if (savedDay === "invalid" || savedDay !== sentDay) {
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
  if (otherFormatFingerprint(previous.format) !== otherFormatFingerprint(saved.format)) {
    return null;
  }
  return sentDay == null ? "" : String(sentDay);
}
