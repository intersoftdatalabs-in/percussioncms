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
  LOCALE_NUMBERING_SYSTEM_MAX_LENGTH,
  localeNumberingSystemWrite,
  parseLocaleNumberingSystem,
  savedLocaleNumberingSystem,
  storedLocaleNumberingSystem,
} from "../../../main/ts/developer/localeNumberingSystem";

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
  },
};

function savedDetail(overrides: Partial<LocaleDetail> = {}): LocaleDetail {
  return {
    ...baseline,
    format: { ...(baseline.format || {}) },
    ...overrides,
  };
}

describe("storedLocaleNumberingSystem", () => {
  it("trims and keeps the stored numbering system text", () => {
    expect(storedLocaleNumberingSystem("  latn ")).toBe("latn");
    expect(storedLocaleNumberingSystem("latn\n")).toBe("latn");
    expect(storedLocaleNumberingSystem("")).toBe("");
    expect(storedLocaleNumberingSystem(null)).toBe("");
  });
});

describe("parseLocaleNumberingSystem", () => {
  it("parses a numbering system, clears on blank, and rejects an overlong value", () => {
    expect(parseLocaleNumberingSystem("latn")).toBe("latn");
    expect(parseLocaleNumberingSystem("  arab  ")).toBe("arab");
    expect(parseLocaleNumberingSystem("")).toBeNull();
    expect(parseLocaleNumberingSystem("   ")).toBeNull();
    expect(parseLocaleNumberingSystem(null)).toBeNull();
    expect(
      parseLocaleNumberingSystem("z".repeat(LOCALE_NUMBERING_SYSTEM_MAX_LENGTH + 1)),
    ).toBe("invalid");
    expect(
      parseLocaleNumberingSystem("z".repeat(LOCALE_NUMBERING_SYSTEM_MAX_LENGTH)),
    ).not.toBe("invalid");
  });
});

describe("localeNumberingSystemWrite", () => {
  it("sends the numbering system only and omits language, label, description, status, and other format fields", () => {
    const sent = localeNumberingSystemWrite(baseline, "  ");
    expect(sent).toEqual({ format: { numberingSystem: "" } });
    const next = localeNumberingSystemWrite(baseline, " arab ");
    expect(next).toEqual({ format: { numberingSystem: "arab" } });
    expect(next).not.toHaveProperty("languageString");
    expect(next).not.toHaveProperty("label");
    expect(next).not.toHaveProperty("description");
    expect(next).not.toHaveProperty("status");
    expect(next).not.toHaveProperty("baseLocale");
    expect(next).not.toHaveProperty("hasFormatProfile");
    if (typeof next === "object") {
      expect(Object.keys(next.format)).toEqual(["numberingSystem"]);
    }
  });

  it("refuses an overlong numbering system", () => {
    const sent = localeNumberingSystemWrite(
      baseline,
      "z".repeat(LOCALE_NUMBERING_SYSTEM_MAX_LENGTH + 1),
    );
    expect(sent).toBe("invalid");
  });

  it("treats the same numbering system as unchanged", () => {
    expect(localeNumberingSystemWrite(baseline, " latn ")).toBe("unchanged");
    expect(localeNumberingSystemWrite(baseline, "")).toEqual({
      format: { numberingSystem: "" },
    });
    const blankBaseline: LocaleDetail = {
      ...baseline,
      format: { ...(baseline.format || {}), numberingSystem: "" },
    };
    expect(localeNumberingSystemWrite(blankBaseline, "   ")).toBe("unchanged");
  });
});

describe("savedLocaleNumberingSystem", () => {
  it("accepts the saved numbering system when nothing else changed", () => {
    const sent = localeNumberingSystemWrite(baseline, "arab");
    expect(
      savedLocaleNumberingSystem(
        sent,
        baseline,
        savedDetail({
          format: { ...(baseline.format || {}), numberingSystem: "arab" },
        }),
      ),
    ).toBe("arab");
  });

  it("accepts a cleared numbering system", () => {
    const sent = localeNumberingSystemWrite(baseline, "");
    expect(
      savedLocaleNumberingSystem(
        sent,
        baseline,
        savedDetail({
          format: { ...(baseline.format || {}), numberingSystem: "" },
        }),
      ),
    ).toBe("");
  });

  it("rejects a response that changed another field", () => {
    const sent = localeNumberingSystemWrite(baseline, "arab");
    expect(
      savedLocaleNumberingSystem(
        sent,
        baseline,
        savedDetail({
          label: "Renamed",
          format: { ...(baseline.format || {}), numberingSystem: "arab" },
        }),
      ),
    ).toBeNull();
    expect(
      savedLocaleNumberingSystem(
        sent,
        baseline,
        savedDetail({
          format: {
            ...(baseline.format || {}),
            numberingSystem: "arab",
            decimalSep: ",",
          },
        }),
      ),
    ).toBeNull();
    expect(
      savedLocaleNumberingSystem(
        sent,
        baseline,
        savedDetail({
          format: {
            ...(baseline.format || {}),
            numberingSystem: "arab",
            defaultTz: "Europe/London",
          },
        }),
      ),
    ).toBeNull();
    expect(
      savedLocaleNumberingSystem(
        sent,
        baseline,
        savedDetail({
          format: { ...(baseline.format || {}), numberingSystem: "thai" },
        }),
      ),
    ).toBeNull();
  });

  it("rejects a body that sent more than the numbering system", () => {
    const sent = {
      format: { numberingSystem: "arab", decimalSep: "," },
    };
    expect(savedLocaleNumberingSystem(sent, baseline, savedDetail())).toBeNull();
  });
});
