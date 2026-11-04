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

export type LocaleEnabledStatus = "active" | "inactive";

export type LocaleEnabledStatusWrite = { status: LocaleEnabledStatus };

export type LocaleEnabledStatusRejection = "unchanged" | "invalid";

const OMITTED_ON_STATUS_WRITE = [
  "languageString",
  "label",
  "description",
  "baseLocale",
  "hasFormatProfile",
  "format",
] as const;

/**
 * Stored locale enabled status. Only {@code active} and {@code inactive} are
 * writes. Anything else is {@code ""}.
 */
export function storedLocaleEnabledStatus(value?: string | null): LocaleEnabledStatus | "" {
  const status = (value ?? "").trim().toLowerCase();
  if (status === "active" || status === "inactive") {
    return status;
  }
  return "";
}

/**
 * Body for the existing locale PUT that sets enabled status.
 *
 * <p>Language string, label, description, and format are omitted. The update
 * leaves an omitted field unchanged. Language string is immutable. A value
 * that is not {@code active} or {@code inactive} is not a write. The same
 * status is not a write.
 */
export function localeEnabledStatusWrite(
  baseline: Pick<LocaleDetail, "status">,
  nextStatus: string,
): LocaleEnabledStatusWrite | LocaleEnabledStatusRejection {
  const status = storedLocaleEnabledStatus(nextStatus);
  if (!status) {
    return "invalid";
  }
  if (status === storedLocaleEnabledStatus(baseline.status)) {
    return "unchanged";
  }
  return { status };
}

function storedText(value?: string | null): string {
  return (value ?? "").replace(/[\r\n]+/g, " ").trim();
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
 * Status to show after a status-only update, or null when the response must
 * not replace the previous status (language, label, description, or format
 * changed, or the status is not the one sent). The sent body must omit those
 * other fields.
 */
export function savedLocaleEnabledStatus(
  sent: LocaleWriteBody,
  previous: LocaleDetail,
  payload: unknown,
): LocaleEnabledStatus | null {
  for (const key of OMITTED_ON_STATUS_WRITE) {
    if (key in sent) {
      return null;
    }
  }
  if (!("status" in sent)) {
    return null;
  }
  const saved = unwrapLocaleDetail(payload);
  if (!saved || typeof saved !== "object") {
    return null;
  }
  const sentStatus = storedLocaleEnabledStatus(sent.status);
  const savedStatus = storedLocaleEnabledStatus(saved.status);
  if (!sentStatus || savedStatus !== sentStatus) {
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
  if ((previous.baseLocale === true) !== (saved.baseLocale === true)) {
    return null;
  }
  if (formatFingerprint(previous) !== formatFingerprint(saved)) {
    return null;
  }
  return sentStatus;
}
