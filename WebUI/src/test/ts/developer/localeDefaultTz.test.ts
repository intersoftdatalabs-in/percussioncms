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

import { describe, expect, it } from "vitest";
import type { LocaleDetail } from "../../../main/ts/api/developer/types";
import {
  LOCALE_DEFAULT_TZ_MAX_LENGTH,
  localeDefaultTzWrite,
  parseLocaleDefaultTz,
  savedLocaleDefaultTz,
  storedLocaleDefaultTz,
} from "../../../main/ts/developer/localeDefaultTz";

const baseline: LocaleDetail = {
  id: 1,
  languageString: "en-us",
  label: "English",
  description: "US English",
  status: "active",
  baseLocale: false,
  hasFormatProfile: true,
  format: {
    languageString: "en-us",
    textDir: "ltr",
    datePattern: "MM/dd/yyyy",
    timePattern: "HH:mm:ss",
    dateTimePattern: "MM/dd/yyyy HH:mm:ss",
    decimalSep: ".",
    groupingSep: ",",
    currencyCode: "USD",
    currencyPattern: "¤#,##0.00",
    firstDayOfWeek: 1,
    measurementSystem: "us",
    defaultTz: "America/New_York",
  },
};

function savedDetail(overrides: Partial<LocaleDetail> = {}): LocaleDetail {
  return {
    ...baseline,
    format: { ...(baseline.format || {}) },
    ...overrides,
  };
}

describe("storedLocaleDefaultTz", () => {
  it("trims and keeps the stored timezone text", () => {
    expect(storedLocaleDefaultTz("  America/New_York ")).toBe("America/New_York");
    expect(storedLocaleDefaultTz("America/New_York\n")).toBe("America/New_York");
    expect(storedLocaleDefaultTz("")).toBe("");
    expect(storedLocaleDefaultTz(null)).toBe("");
  });
});

describe("parseLocaleDefaultTz", () => {
  it("parses a timezone, clears on blank, and rejects an overlong value", () => {
    expect(parseLocaleDefaultTz("America/New_York")).toBe("America/New_York");
    expect(parseLocaleDefaultTz("  UTC  ")).toBe("UTC");
    expect(parseLocaleDefaultTz("")).toBeNull();
    expect(parseLocaleDefaultTz("   ")).toBeNull();
    expect(parseLocaleDefaultTz(null)).toBeNull();
    expect(parseLocaleDefaultTz("z".repeat(LOCALE_DEFAULT_TZ_MAX_LENGTH + 1))).toBe(
      "invalid",
    );
    expect(
      parseLocaleDefaultTz("z".repeat(LOCALE_DEFAULT_TZ_MAX_LENGTH)),
    ).not.toBe("invalid");
  });
});

describe("localeDefaultTzWrite", () => {
  it("sends the default timezone only and omits language, label, description, status, and other format fields", () => {
    const sent = localeDefaultTzWrite(baseline, "  ");
    expect(sent).toEqual({ format: { defaultTz: "" } });
    const next = localeDefaultTzWrite(baseline, " Europe/London ");
    expect(next).toEqual({ format: { defaultTz: "Europe/London" } });
    expect(next).not.toHaveProperty("languageString");
    expect(next).not.toHaveProperty("label");
    expect(next).not.toHaveProperty("description");
    expect(next).not.toHaveProperty("status");
    expect(next).not.toHaveProperty("baseLocale");
    expect(next).not.toHaveProperty("hasFormatProfile");
    if (typeof next === "object") {
      expect(Object.keys(next.format)).toEqual(["defaultTz"]);
    }
  });

  it("refuses an overlong default timezone", () => {
    const sent = localeDefaultTzWrite(
      baseline,
      "z".repeat(LOCALE_DEFAULT_TZ_MAX_LENGTH + 1),
    );
    expect(sent).toBe("invalid");
  });

  it("treats the same timezone as unchanged", () => {
    expect(localeDefaultTzWrite(baseline, " America/New_York ")).toBe("unchanged");
    expect(localeDefaultTzWrite(baseline, "")).toEqual({
      format: { defaultTz: "" },
    });
    const blankBaseline: LocaleDetail = {
      ...baseline,
      format: { ...(baseline.format || {}), defaultTz: "" },
    };
    expect(localeDefaultTzWrite(blankBaseline, "   ")).toBe("unchanged");
  });
});

describe("savedLocaleDefaultTz", () => {
  it("accepts the saved timezone when nothing else changed", () => {
    const sent = localeDefaultTzWrite(baseline, "Europe/London");
    expect(
      savedLocaleDefaultTz(sent, baseline, savedDetail({
        format: { ...(baseline.format || {}), defaultTz: "Europe/London" },
      })),
    ).toBe("Europe/London");
  });

  it("accepts a cleared default timezone", () => {
    const sent = localeDefaultTzWrite(baseline, "");
    expect(
      savedLocaleDefaultTz(sent, baseline, savedDetail({
        format: { ...(baseline.format || {}), defaultTz: "" },
      })),
    ).toBe("");
  });

  it("rejects a response that changed another field", () => {
    const sent = localeDefaultTzWrite(baseline, "Europe/London");
    expect(
      savedLocaleDefaultTz(sent, baseline, savedDetail({
        label: "Renamed",
        format: { ...(baseline.format || {}), defaultTz: "Europe/London" },
      })),
    ).toBeNull();
    expect(
      savedLocaleDefaultTz(sent, baseline, savedDetail({
        format: {
          ...(baseline.format || {}),
          defaultTz: "Europe/London",
          decimalSep: ",",
        },
      })),
    ).toBeNull();
    expect(
      savedLocaleDefaultTz(sent, baseline, savedDetail({
        format: { ...(baseline.format || {}), defaultTz: "Asia/Tokyo" },
      })),
    ).toBeNull();
  });

  it("rejects a body that sent more than the default timezone", () => {
    const sent = {
      format: { defaultTz: "Europe/London", decimalSep: "," },
    };
    expect(savedLocaleDefaultTz(sent, baseline, savedDetail())).toBeNull();
  });
});
