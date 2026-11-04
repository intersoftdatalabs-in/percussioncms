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

export const LOCALE_MEASUREMENT_SYSTEMS = ["us", "uk", "metric"] as const;

export type LocaleMeasurementSystem = (typeof LOCALE_MEASUREMENT_SYSTEMS)[number];

export type LocaleMeasurementSystemWrite = { format: { measurementSystem: string } };

export type LocaleMeasurementSystemRejection = "unchanged" | "invalid";

const OMITTED_ON_MEASUREMENT_WRITE = [
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
  "defaultTz",
  "numberingSystem",
  "calendar",
] as const;

/** Display form of a stored measurement system. Blank is {@code ""}. */
export function storedLocaleMeasurementSystem(value?: string | null): string {
  return (value ?? "").replace(/[\r\n]+/g, " ").trim().toLowerCase();
}

/**
 * Parsed measurement system: {@code us}, {@code uk}, {@code metric}, {@code null}
 * to clear, or {@code "invalid"}.
 */
export function parseLocaleMeasurementSystem(
  value?: string | null,
): LocaleMeasurementSystem | null | "invalid" {
  const raw = storedLocaleMeasurementSystem(value);
  if (raw === "") {
    return null;
  }
  if ((LOCALE_MEASUREMENT_SYSTEMS as readonly string[]).includes(raw)) {
    return raw as LocaleMeasurementSystem;
  }
  return "invalid";
}

/**
 * Body for the existing locale PUT that sets the measurement system.
 *
 * <p>Language string, label, description, status, and other format fields are
 * omitted. A blank value is an empty string and clears a stored measurement
 * system. Values other than {@code us}, {@code uk}, and {@code metric} are not
 * a write.
 */
export function localeMeasurementSystemWrite(
  baseline: Pick<LocaleDetail, "format">,
  nextMeasurement: string,
): LocaleMeasurementSystemWrite | LocaleMeasurementSystemRejection {
  const measurementSystem = parseLocaleMeasurementSystem(nextMeasurement);
  if (measurementSystem === "invalid") {
    return "invalid";
  }
  const previous = parseLocaleMeasurementSystem(baseline.format?.measurementSystem);
  if (previous !== "invalid" && measurementSystem === previous) {
    return "unchanged";
  }
  return { format: { measurementSystem: measurementSystem ?? "" } };
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
 * Measurement system to show after a measurement-system-only update, or null
 * when the response must not replace the previous value.
 */
export function savedLocaleMeasurementSystem(
  sent: LocaleWriteBody,
  previous: LocaleDetail,
  payload: unknown,
): string | null {
  for (const key of OMITTED_ON_MEASUREMENT_WRITE) {
    if (key in sent) {
      return null;
    }
  }
  if (!("format" in sent) || sent.format == null || typeof sent.format !== "object") {
    return null;
  }
  if (!("measurementSystem" in sent.format) || !sentOmitsOtherFormatFields(sent.format)) {
    return null;
  }
  const saved = unwrapLocaleDetail(payload);
  if (!saved || typeof saved !== "object") {
    return null;
  }
  const sentMeasurement = parseLocaleMeasurementSystem(sent.format.measurementSystem);
  if (sentMeasurement === "invalid") {
    return null;
  }
  const savedMeasurement = parseLocaleMeasurementSystem(saved.format?.measurementSystem);
  if (savedMeasurement === "invalid" || savedMeasurement !== sentMeasurement) {
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
  return sentMeasurement == null ? "" : sentMeasurement;
}
