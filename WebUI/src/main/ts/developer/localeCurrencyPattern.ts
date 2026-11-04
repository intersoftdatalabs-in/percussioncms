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

export type LocaleCurrencyPatternWrite = { format: { currencyPattern: string } };

export type LocaleCurrencyPatternRejection = "unchanged";

const OMITTED_ON_CURRENCY_PATTERN_WRITE = [
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
  "firstDayOfWeek",
  "measurementSystem",
  "defaultTz",
  "numberingSystem",
  "calendar",
] as const;

/**
 * Stored locale currency pattern with surrounding space removed.
 * A blank value is {@code ""} and clears a stored currency pattern.
 */
export function storedLocaleCurrencyPattern(value?: string | null): string {
  return (value ?? "").replace(/[\r\n]+/g, " ").trim();
}

/**
 * Body for the existing locale PUT that sets the currency pattern.
 *
 * <p>Language string, label, description, status, and other format fields are
 * omitted. The update leaves an omitted field unchanged. Language string is
 * immutable. A blank currency pattern is an empty string and clears a stored
 * currency pattern. The same currency pattern is not a write.
 */
export function localeCurrencyPatternWrite(
  baseline: Pick<LocaleDetail, "format">,
  nextCurrencyPattern: string,
): LocaleCurrencyPatternWrite | LocaleCurrencyPatternRejection {
  const currencyPattern = storedLocaleCurrencyPattern(nextCurrencyPattern);
  if (currencyPattern === storedLocaleCurrencyPattern(baseline.format?.currencyPattern)) {
    return "unchanged";
  }
  return { format: { currencyPattern } };
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
    src.firstDayOfWeek == null ? "" : String(src.firstDayOfWeek),
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
 * Currency pattern to show after a currency-pattern-only update, or null when
 * the response must not replace the previous pattern (language, label,
 * description, status, or other format fields changed, or the pattern is not
 * the one sent). The sent body must omit those other fields.
 */
export function savedLocaleCurrencyPattern(
  sent: LocaleWriteBody,
  previous: LocaleDetail,
  payload: unknown,
): string | null {
  for (const key of OMITTED_ON_CURRENCY_PATTERN_WRITE) {
    if (key in sent) {
      return null;
    }
  }
  if (!("format" in sent) || sent.format == null || typeof sent.format !== "object") {
    return null;
  }
  if (!("currencyPattern" in sent.format) || !sentOmitsOtherFormatFields(sent.format)) {
    return null;
  }
  const saved = unwrapLocaleDetail(payload);
  if (!saved || typeof saved !== "object") {
    return null;
  }
  const sentPattern = storedLocaleCurrencyPattern(sent.format.currencyPattern);
  if (storedLocaleCurrencyPattern(saved.format?.currencyPattern) !== sentPattern) {
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
  return sentPattern;
}
