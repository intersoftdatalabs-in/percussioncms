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
 * Matches the server bound for {@code format.numberingSystem}
 * ({@code LocalesAdaptor.validateMaxLen("numberingSystem", …, 16)}).
 */
export const LOCALE_NUMBERING_SYSTEM_MAX_LENGTH = 16;

export type LocaleNumberingSystemWrite = { format: { numberingSystem: string } };

export type LocaleNumberingSystemRejection = "unchanged" | "invalid";

const OMITTED_ON_NUMBERING_SYSTEM_WRITE = [
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
  "calendar",
] as const;

/** Display form of a stored numbering system. Blank is {@code ""}. */
export function storedLocaleNumberingSystem(value?: string | null): string {
  return (value ?? "").replace(/[\r\n]+/g, " ").trim();
}

/**
 * Parsed numbering system: the stored value, {@code null} to clear, or
 * {@code "invalid"} when the value is longer than the server allows.
 */
export function parseLocaleNumberingSystem(
  value?: string | null,
): string | null | "invalid" {
  const raw = storedLocaleNumberingSystem(value);
  if (raw === "") {
    return null;
  }
  if (raw.length > LOCALE_NUMBERING_SYSTEM_MAX_LENGTH) {
    return "invalid";
  }
  return raw;
}

/**
 * Body for the existing locale PUT that sets the numbering system.
 *
 * <p>Language string, label, description, status, and other format fields are
 * omitted. A blank value is an empty string and clears a stored numbering
 * system. A value longer than {@link LOCALE_NUMBERING_SYSTEM_MAX_LENGTH} is not
 * a write.</p>
 */
export function localeNumberingSystemWrite(
  baseline: Pick<LocaleDetail, "format">,
  nextNumberingSystem: string,
): LocaleNumberingSystemWrite | LocaleNumberingSystemRejection {
  const numberingSystem = parseLocaleNumberingSystem(nextNumberingSystem);
  if (numberingSystem === "invalid") {
    return "invalid";
  }
  const previous = parseLocaleNumberingSystem(baseline.format?.numberingSystem);
  if (previous !== "invalid" && numberingSystem === previous) {
    return "unchanged";
  }
  return { format: { numberingSystem: numberingSystem ?? "" } };
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
 * Numbering system to show after a numbering-system-only update, or null when
 * the response must not replace the previous value.
 */
export function savedLocaleNumberingSystem(
  sent: LocaleWriteBody,
  previous: LocaleDetail,
  payload: unknown,
): string | null {
  for (const key of OMITTED_ON_NUMBERING_SYSTEM_WRITE) {
    if (key in sent) {
      return null;
    }
  }
  if (!("format" in sent) || sent.format == null || typeof sent.format !== "object") {
    return null;
  }
  if (
    !("numberingSystem" in sent.format) ||
    !sentOmitsOtherFormatFields(sent.format)
  ) {
    return null;
  }
  const saved = unwrapLocaleDetail(payload);
  if (!saved || typeof saved !== "object") {
    return null;
  }
  const sentNumberingSystem = parseLocaleNumberingSystem(sent.format.numberingSystem);
  if (sentNumberingSystem === "invalid") {
    return null;
  }
  const savedNumberingSystem = parseLocaleNumberingSystem(saved.format?.numberingSystem);
  if (
    savedNumberingSystem === "invalid" ||
    savedNumberingSystem !== sentNumberingSystem
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
  return sentNumberingSystem == null ? "" : sentNumberingSystem;
}
