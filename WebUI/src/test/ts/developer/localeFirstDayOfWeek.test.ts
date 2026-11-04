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
  localeFirstDayOfWeekWrite,
  parseLocaleFirstDayOfWeek,
  savedLocaleFirstDayOfWeek,
  storedLocaleFirstDayOfWeek,
} from "../../../main/ts/developer/localeFirstDayOfWeek";

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
  },
};

describe("localeFirstDayOfWeekWrite", () => {
  it("sends the first day of week only and omits language, label, description, status, and other format fields", () => {
    const sent = localeFirstDayOfWeekWrite(baseline, "  ");
    expect(sent).toEqual({ format: { firstDayOfWeek: null } });
    const next = localeFirstDayOfWeekWrite(baseline, " 2 ");
    expect(next).toEqual({ format: { firstDayOfWeek: 2 } });
    expect(next).not.toHaveProperty("languageString");
    expect(next).not.toHaveProperty("label");
    expect(next).not.toHaveProperty("description");
    expect(next).not.toHaveProperty("status");
    expect(next).not.toHaveProperty("baseLocale");
    expect(next).not.toHaveProperty("hasFormatProfile");
    if (next === "unchanged" || next === "invalid") {
      return;
    }
    expect(next.format).not.toHaveProperty("textDir");
    expect(next.format).not.toHaveProperty("datePattern");
    expect(next.format).not.toHaveProperty("timePattern");
    expect(next.format).not.toHaveProperty("dateTimePattern");
    expect(next.format).not.toHaveProperty("decimalSep");
    expect(next.format).not.toHaveProperty("groupingSep");
    expect(next.format).not.toHaveProperty("currencyCode");
    expect(next.format).not.toHaveProperty("currencyPattern");
  });

  it("sends JSON null to clear, skips an unchanged value, and rejects out-of-range values", () => {
    expect(localeFirstDayOfWeekWrite(baseline, "   ")).toEqual({
      format: { firstDayOfWeek: null },
    });
    expect(localeFirstDayOfWeekWrite(baseline, " 1 ")).toBe("unchanged");
    expect(localeFirstDayOfWeekWrite({ format: { firstDayOfWeek: null } }, "\n")).toBe(
      "unchanged",
    );
    expect(localeFirstDayOfWeekWrite(baseline, "0")).toBe("invalid");
    expect(localeFirstDayOfWeekWrite(baseline, "8")).toBe("invalid");
    expect(localeFirstDayOfWeekWrite(baseline, "1.5")).toBe("invalid");
    expect(storedLocaleFirstDayOfWeek(2)).toBe("2");
    expect(parseLocaleFirstDayOfWeek("7")).toBe(7);
    expect(parseLocaleFirstDayOfWeek("")).toBeNull();
  });
});

describe("savedLocaleFirstDayOfWeek", () => {
  it("accepts a first day of week when language, label, description, status, and other format fields stay", () => {
    const sent = localeFirstDayOfWeekWrite(baseline, "2");
    expect(sent).not.toBe("unchanged");
    expect(sent).not.toBe("invalid");
    if (sent === "unchanged" || sent === "invalid") {
      return;
    }
    expect(
      savedLocaleFirstDayOfWeek(sent, baseline, {
        LocaleDetail: {
          ...baseline,
          format: { ...baseline.format, firstDayOfWeek: 2 },
        },
      }),
    ).toBe("2");
  });

  it("accepts a blank first day of week that clears and keeps other format fields", () => {
    const sent = localeFirstDayOfWeekWrite(baseline, "");
    expect(sent).toEqual({ format: { firstDayOfWeek: null } });
    if (sent === "unchanged" || sent === "invalid") {
      return;
    }
    expect(
      savedLocaleFirstDayOfWeek(sent, baseline, {
        ...baseline,
        format: { ...baseline.format, firstDayOfWeek: null },
      }),
    ).toBe("");
  });

  it("rejects a response that changes the language, label, description, status, or other format fields", () => {
    const sent = { format: { firstDayOfWeek: 2 } };
    expect(
      savedLocaleFirstDayOfWeek(sent, baseline, {
        ...baseline,
        format: { ...baseline.format, firstDayOfWeek: 2 },
        languageString: "fr-ca",
      }),
    ).toBeNull();
    expect(
      savedLocaleFirstDayOfWeek(sent, baseline, {
        ...baseline,
        label: "Anglais",
        format: { ...baseline.format, firstDayOfWeek: 2 },
      }),
    ).toBeNull();
    expect(
      savedLocaleFirstDayOfWeek(sent, baseline, {
        ...baseline,
        description: "changed",
        format: { ...baseline.format, firstDayOfWeek: 2 },
      }),
    ).toBeNull();
    expect(
      savedLocaleFirstDayOfWeek(sent, baseline, {
        ...baseline,
        status: "inactive",
        format: { ...baseline.format, firstDayOfWeek: 2 },
      }),
    ).toBeNull();
    expect(
      savedLocaleFirstDayOfWeek(sent, baseline, {
        ...baseline,
        format: {
          ...baseline.format,
          firstDayOfWeek: 2,
          currencyCode: "EUR",
        },
      }),
    ).toBeNull();
    expect(
      savedLocaleFirstDayOfWeek(
        { format: { firstDayOfWeek: 2, textDir: "ltr" } },
        baseline,
        { ...baseline, format: { ...baseline.format, firstDayOfWeek: 2 } },
      ),
    ).toBeNull();
    expect(
      savedLocaleFirstDayOfWeek(
        { format: { firstDayOfWeek: 2 }, label: "English" },
        baseline,
        { ...baseline, format: { ...baseline.format, firstDayOfWeek: 2 } },
      ),
    ).toBeNull();
  });
});
