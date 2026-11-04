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

/**
 * Matches the server bound for {@code format.defaultTz}
 * ({@code LocalesAdaptor.validateMaxLen("defaultTz", …, 64)}).
 */
export const LOCALE_DEFAULT_TZ_MAX_LENGTH = 64;

export type LocaleDefaultTzWrite = { format: { defaultTz: string } };

export type LocaleDefaultTzRejection = "unchanged" | "invalid";

const OMITTED_ON_DEFAULT_TZ_WRITE = [
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
  "numberingSystem",
  "calendar",
] as const;

/** Display form of a stored default timezone. Blank is {@code ""}. */
export function storedLocaleDefaultTz(value?: string | null): string {
  return (value ?? "").replace(/[\r\n]+/g, " ").trim();
}

/**
 * Parsed default timezone: the stored value, {@code null} to clear, or
 * {@code "invalid"} when the value is longer than the server allows.
 */
export function parseLocaleDefaultTz(
  value?: string | null,
): string | null | "invalid" {
  const raw = storedLocaleDefaultTz(value);
  if (raw === "") {
    return null;
  }
  if (raw.length > LOCALE_DEFAULT_TZ_MAX_LENGTH) {
    return "invalid";
  }
  return raw;
}

/**
 * Body for the existing locale PUT that sets the default timezone.
 *
 * <p>Language string, label, description, status, and other format fields are
 * omitted. A blank value is an empty string and clears a stored default
 * timezone. A value longer than {@link LOCALE_DEFAULT_TZ_MAX_LENGTH} is not a
 * write.</p>
 */
export function localeDefaultTzWrite(
  baseline: Pick<LocaleDetail, "format">,
  nextDefaultTz: string,
): LocaleDefaultTzWrite | LocaleDefaultTzRejection {
  const defaultTz = parseLocaleDefaultTz(nextDefaultTz);
  if (defaultTz === "invalid") {
    return "invalid";
  }
  const previous = parseLocaleDefaultTz(baseline.format?.defaultTz);
  if (previous !== "invalid" && defaultTz === previous) {
    return "unchanged";
  }
  return { format: { defaultTz: defaultTz ?? "" } };
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
 * Default timezone to show after a default-timezone-only update, or null when
 * the response must not replace the previous value.
 */
export function savedLocaleDefaultTz(
  sent: LocaleWriteBody,
  previous: LocaleDetail,
  payload: unknown,
): string | null {
  for (const key of OMITTED_ON_DEFAULT_TZ_WRITE) {
    if (key in sent) {
      return null;
    }
  }
  if (!("format" in sent) || sent.format == null || typeof sent.format !== "object") {
    return null;
  }
  if (!("defaultTz" in sent.format) || !sentOmitsOtherFormatFields(sent.format)) {
    return null;
  }
  const saved = unwrapLocaleDetail(payload);
  if (!saved || typeof saved !== "object") {
    return null;
  }
  const sentDefaultTz = parseLocaleDefaultTz(sent.format.defaultTz);
  if (sentDefaultTz === "invalid") {
    return null;
  }
  const savedDefaultTz = parseLocaleDefaultTz(saved.format?.defaultTz);
  if (savedDefaultTz === "invalid" || savedDefaultTz !== sentDefaultTz) {
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
  return sentDefaultTz == null ? "" : sentDefaultTz;
}
