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

export type LocaleTextDir = "ltr" | "rtl";

export type LocaleTextDirWrite = { format: { textDir: string } };

export type LocaleTextDirRejection = "unchanged" | "invalid";

const OMITTED_ON_TEXT_DIR_WRITE = [
  "languageString",
  "label",
  "description",
  "status",
  "baseLocale",
  "hasFormatProfile",
] as const;

const OMITTED_FORMAT_FIELDS = [
  "languageString",
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
  "calendar",
] as const;

/**
 * Stored locale text direction. Only {@code ltr} and {@code rtl} are values.
 * Anything else, including blank, is {@code ""}.
 */
export function storedLocaleTextDir(value?: string | null): LocaleTextDir | "" {
  const dir = (value ?? "").replace(/[\r\n]+/g, " ").trim().toLowerCase();
  if (dir === "ltr" || dir === "rtl") {
    return dir;
  }
  return "";
}

/**
 * Body for the existing locale PUT that sets text direction.
 *
 * <p>Language string, label, description, status, and other format fields are
 * omitted. The update leaves an omitted field unchanged. Language string is
 * immutable. Only {@code ltr}, {@code rtl}, or blank are writes. A blank
 * direction is an empty string and clears a stored direction. A value that is
 * not those three is not a write. The same direction is not a write.
 */
export function localeTextDirWrite(
  baseline: Pick<LocaleDetail, "format">,
  nextTextDir: string,
): LocaleTextDirWrite | LocaleTextDirRejection {
  const raw = (nextTextDir ?? "").replace(/[\r\n]+/g, " ").trim().toLowerCase();
  if (raw && raw !== "ltr" && raw !== "rtl") {
    return "invalid";
  }
  const textDir = storedLocaleTextDir(raw);
  if (textDir === storedLocaleTextDir(baseline.format?.textDir)) {
    return "unchanged";
  }
  return { format: { textDir } };
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
 * Text direction to show after a text-direction-only update, or null when the
 * response must not replace the previous direction (language, label,
 * description, status, or other format fields changed, or the direction is
 * not the one sent). The sent body must omit those other fields.
 */
export function savedLocaleTextDir(
  sent: LocaleWriteBody,
  previous: LocaleDetail,
  payload: unknown,
): string | null {
  for (const key of OMITTED_ON_TEXT_DIR_WRITE) {
    if (key in sent) {
      return null;
    }
  }
  if (!("format" in sent) || sent.format == null || typeof sent.format !== "object") {
    return null;
  }
  if (!("textDir" in sent.format) || !sentOmitsOtherFormatFields(sent.format)) {
    return null;
  }
  const saved = unwrapLocaleDetail(payload);
  if (!saved || typeof saved !== "object") {
    return null;
  }
  const sentDir = storedLocaleTextDir(sent.format.textDir);
  if (storedLocaleTextDir(saved.format?.textDir) !== sentDir) {
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
  return sentDir;
}
