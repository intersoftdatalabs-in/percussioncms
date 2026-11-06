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

import { describe, expect, it } from "vitest";
import type { LocaleDetail } from "../../../main/ts/api/developer/types";
import {
  LOCALE_CALENDAR_MAX_LENGTH,
  localeCalendarWrite,
  parseLocaleCalendar,
  savedLocaleCalendar,
  storedLocaleCalendar,
} from "../../../main/ts/developer/localeCalendar";

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
    numberingSystem: "latn",
    calendar: "gregory",
  },
};

function savedDetail(overrides: Partial<LocaleDetail> = {}): LocaleDetail {
  return {
    ...baseline,
    format: { ...(baseline.format || {}) },
    ...overrides,
  };
}

describe("storedLocaleCalendar", () => {
  it("trims and keeps the stored calendar text", () => {
    expect(storedLocaleCalendar("  gregory ")).toBe("gregory");
    expect(storedLocaleCalendar("gregory\n")).toBe("gregory");
    expect(storedLocaleCalendar("")).toBe("");
    expect(storedLocaleCalendar(null)).toBe("");
  });
});

describe("parseLocaleCalendar", () => {
  it("parses a calendar, clears on blank, and rejects an overlong value", () => {
    expect(parseLocaleCalendar("gregory")).toBe("gregory");
    expect(parseLocaleCalendar("  islamic  ")).toBe("islamic");
    expect(parseLocaleCalendar("")).toBeNull();
    expect(parseLocaleCalendar("   ")).toBeNull();
    expect(parseLocaleCalendar(null)).toBeNull();
    expect(
      parseLocaleCalendar("z".repeat(LOCALE_CALENDAR_MAX_LENGTH + 1)),
    ).toBe("invalid");
    expect(
      parseLocaleCalendar("z".repeat(LOCALE_CALENDAR_MAX_LENGTH)),
    ).not.toBe("invalid");
  });
});

describe("localeCalendarWrite", () => {
  it("sends the calendar only and omits language, label, description, status, and other format fields", () => {
    const sent = localeCalendarWrite(baseline, "  ");
    expect(sent).toEqual({ format: { calendar: "" } });
    const next = localeCalendarWrite(baseline, " islamic ");
    expect(next).toEqual({ format: { calendar: "islamic" } });
    expect(next).not.toHaveProperty("languageString");
    expect(next).not.toHaveProperty("label");
    expect(next).not.toHaveProperty("description");
    expect(next).not.toHaveProperty("status");
    expect(next).not.toHaveProperty("baseLocale");
    expect(next).not.toHaveProperty("hasFormatProfile");
    if (typeof next === "object") {
      expect(Object.keys(next.format)).toEqual(["calendar"]);
    }
  });

  it("refuses an overlong calendar", () => {
    const sent = localeCalendarWrite(
      baseline,
      "z".repeat(LOCALE_CALENDAR_MAX_LENGTH + 1),
    );
    expect(sent).toBe("invalid");
  });

  it("treats the same calendar as unchanged", () => {
    expect(localeCalendarWrite(baseline, " gregory ")).toBe("unchanged");
    expect(localeCalendarWrite(baseline, "")).toEqual({
      format: { calendar: "" },
    });
    const blankBaseline: LocaleDetail = {
      ...baseline,
      format: { ...(baseline.format || {}), calendar: "" },
    };
    expect(localeCalendarWrite(blankBaseline, "   ")).toBe("unchanged");
  });
});

describe("savedLocaleCalendar", () => {
  it("accepts the saved calendar when nothing else changed", () => {
    const sent = localeCalendarWrite(baseline, "islamic");
    expect(
      savedLocaleCalendar(
        sent,
        baseline,
        savedDetail({
          format: { ...(baseline.format || {}), calendar: "islamic" },
        }),
      ),
    ).toBe("islamic");
  });

  it("accepts a cleared calendar", () => {
    const sent = localeCalendarWrite(baseline, "");
    expect(
      savedLocaleCalendar(
        sent,
        baseline,
        savedDetail({
          format: { ...(baseline.format || {}), calendar: "" },
        }),
      ),
    ).toBe("");
  });

  it("rejects a response that changed another field", () => {
    const sent = localeCalendarWrite(baseline, "islamic");
    expect(
      savedLocaleCalendar(
        sent,
        baseline,
        savedDetail({
          label: "Renamed",
          format: { ...(baseline.format || {}), calendar: "islamic" },
        }),
      ),
    ).toBeNull();
    expect(
      savedLocaleCalendar(
        sent,
        baseline,
        savedDetail({
          format: {
            ...(baseline.format || {}),
            calendar: "islamic",
            decimalSep: ",",
          },
        }),
      ),
    ).toBeNull();
    expect(
      savedLocaleCalendar(
        sent,
        baseline,
        savedDetail({
          format: {
            ...(baseline.format || {}),
            calendar: "islamic",
            numberingSystem: "arab",
          },
        }),
      ),
    ).toBeNull();
    expect(
      savedLocaleCalendar(
        sent,
        baseline,
        savedDetail({
          format: { ...(baseline.format || {}), calendar: "buddhist" },
        }),
      ),
    ).toBeNull();
  });

  it("rejects a body that sent more than the calendar", () => {
    const sent = {
      format: { calendar: "islamic", numberingSystem: "latn" },
    };
    expect(savedLocaleCalendar(sent, baseline, savedDetail())).toBeNull();
  });
});
