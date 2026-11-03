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
  localeTimePatternWrite,
  savedLocaleTimePattern,
  storedLocaleTimePattern,
} from "../../../main/ts/developer/localeTimePattern";

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
    currencyCode: "USD",
  },
};

describe("localeTimePatternWrite", () => {
  it("sends the time pattern only and omits language, label, description, status, and other format fields", () => {
    const sent = localeTimePatternWrite(baseline, " hh:mm a ");
    expect(sent).toEqual({ format: { timePattern: "hh:mm a" } });
    expect(sent).not.toHaveProperty("languageString");
    expect(sent).not.toHaveProperty("label");
    expect(sent).not.toHaveProperty("description");
    expect(sent).not.toHaveProperty("status");
    expect(sent).not.toHaveProperty("baseLocale");
    expect(sent).not.toHaveProperty("hasFormatProfile");
    if (sent === "unchanged") {
      return;
    }
    expect(sent.format).not.toHaveProperty("textDir");
    expect(sent.format).not.toHaveProperty("datePattern");
    expect(sent.format).not.toHaveProperty("currencyCode");
  });

  it("sends an empty time pattern to clear and skips an unchanged pattern", () => {
    expect(localeTimePatternWrite(baseline, "   ")).toEqual({ format: { timePattern: "" } });
    expect(localeTimePatternWrite(baseline, " HH:mm:ss ")).toBe("unchanged");
    expect(localeTimePatternWrite({ format: { timePattern: "  " } }, "\n")).toBe("unchanged");
    expect(storedLocaleTimePattern(" hh:mm a ")).toBe("hh:mm a");
  });
});

describe("savedLocaleTimePattern", () => {
  it("accepts a time pattern when language, label, description, status, and other format fields stay", () => {
    const sent = localeTimePatternWrite(baseline, "hh:mm a");
    expect(sent).not.toBe("unchanged");
    if (sent === "unchanged") {
      return;
    }
    expect(
      savedLocaleTimePattern(sent, baseline, {
        LocaleDetail: {
          ...baseline,
          format: { ...baseline.format, timePattern: " hh:mm a " },
        },
      }),
    ).toBe("hh:mm a");
  });

  it("accepts a blank time pattern that clears and keeps other format fields", () => {
    const sent = localeTimePatternWrite(baseline, "");
    expect(sent).toEqual({ format: { timePattern: "" } });
    if (sent === "unchanged") {
      return;
    }
    expect(
      savedLocaleTimePattern(sent, baseline, {
        ...baseline,
        format: { ...baseline.format, timePattern: "" },
      }),
    ).toBe("");
  });

  it("rejects a response that changes the language, label, description, status, or other format fields", () => {
    const sent = { format: { timePattern: "hh:mm a" } };
    expect(
      savedLocaleTimePattern(sent, baseline, {
        ...baseline,
        format: { ...baseline.format, timePattern: "hh:mm a" },
        languageString: "fr-ca",
      }),
    ).toBeNull();
    expect(
      savedLocaleTimePattern(sent, baseline, {
        ...baseline,
        label: "Anglais",
        format: { ...baseline.format, timePattern: "hh:mm a" },
      }),
    ).toBeNull();
    expect(
      savedLocaleTimePattern(sent, baseline, {
        ...baseline,
        description: "changed",
        format: { ...baseline.format, timePattern: "hh:mm a" },
      }),
    ).toBeNull();
    expect(
      savedLocaleTimePattern(sent, baseline, {
        ...baseline,
        status: "inactive",
        format: { ...baseline.format, timePattern: "hh:mm a" },
      }),
    ).toBeNull();
    expect(
      savedLocaleTimePattern(sent, baseline, {
        ...baseline,
        format: { ...baseline.format, timePattern: "hh:mm a", datePattern: "yyyy-MM-dd" },
      }),
    ).toBeNull();
    expect(
      savedLocaleTimePattern(
        { format: { timePattern: "hh:mm a", textDir: "ltr" } },
        baseline,
        { ...baseline, format: { ...baseline.format, timePattern: "hh:mm a" } },
      ),
    ).toBeNull();
    expect(
      savedLocaleTimePattern(
        { format: { timePattern: "hh:mm a" }, label: "English" },
        baseline,
        { ...baseline, format: { ...baseline.format, timePattern: "hh:mm a" } },
      ),
    ).toBeNull();
  });
});
