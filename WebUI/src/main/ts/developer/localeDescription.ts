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

export type LocaleDescriptionWrite = { description: string };

export type LocaleDescriptionRejection = "unchanged";

const OMITTED_ON_DESCRIPTION_WRITE = [
  "languageString",
  "label",
  "status",
  "baseLocale",
  "hasFormatProfile",
  "format",
] as const;

/**
 * Stored locale description with surrounding space and line breaks removed.
 * A blank value is {@code ""} and clears a stored description.
 */
export function storedLocaleDescription(value?: string | null): string {
  return (value ?? "").replace(/[\r\n]+/g, " ").trim();
}

/**
 * Body for the existing locale PUT that sets the description.
 *
 * <p>Language string, label, status (enabled), and format are omitted. The
 * update leaves an omitted field unchanged. Language string is immutable.
 * A blank description is an empty string and clears a stored description.
 * The same description is not a write.
 */
export function localeDescriptionWrite(
  baseline: Pick<LocaleDetail, "description">,
  nextDescription: string,
): LocaleDescriptionWrite | LocaleDescriptionRejection {
  const description = storedLocaleDescription(nextDescription);
  if (description === storedLocaleDescription(baseline.description)) {
    return "unchanged";
  }
  return { description };
}

function storedStatus(value?: string | null): string {
  return (value ?? "").trim().toLowerCase();
}

function formatFingerprint(detail: Pick<LocaleDetail, "hasFormatProfile" | "format">): string {
  const fmt = detail.format;
  const has = Boolean(detail.hasFormatProfile) || Boolean(fmt);
  if (!has) {
    return "";
  }
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
    src.calendar || "",
  ].join("\u0001");
}

/**
 * Description to show after a description-only update, or null when the
 * response must not replace the previous description (language, label,
 * status, or format changed, or the description is not the one sent). The
 * sent body must omit those other fields.
 */
export function savedLocaleDescription(
  sent: LocaleWriteBody,
  previous: LocaleDetail,
  payload: unknown,
): string | null {
  for (const key of OMITTED_ON_DESCRIPTION_WRITE) {
    if (key in sent) {
      return null;
    }
  }
  if (!("description" in sent)) {
    return null;
  }
  const saved = unwrapLocaleDetail(payload);
  if (!saved || typeof saved !== "object") {
    return null;
  }
  const sentDescription = storedLocaleDescription(sent.description);
  if (storedLocaleDescription(saved.description) !== sentDescription) {
    return null;
  }
  const savedLang = normalizeLanguageString(saved.languageString);
  const previousLang = normalizeLanguageString(previous.languageString);
  if (!savedLang || (previousLang && savedLang !== previousLang)) {
    return null;
  }
  if (storedLocaleDescription(saved.label) !== storedLocaleDescription(previous.label)) {
    return null;
  }
  const previousStatus = storedStatus(previous.status);
  if (previousStatus && storedStatus(saved.status) !== previousStatus) {
    return null;
  }
  if ((previous.baseLocale === true) !== (saved.baseLocale === true)) {
    return null;
  }
  if (formatFingerprint(previous) !== formatFingerprint(saved)) {
    return null;
  }
  return sentDescription;
}
