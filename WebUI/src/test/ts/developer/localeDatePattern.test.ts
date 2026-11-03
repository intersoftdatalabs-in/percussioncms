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
  localeDatePatternWrite,
  savedLocaleDatePattern,
  storedLocaleDatePattern,
} from "../../../main/ts/developer/localeDatePattern";

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

describe("localeDatePatternWrite", () => {
  it("sends the date pattern only and omits language, label, description, status, and other format fields", () => {
    const sent = localeDatePatternWrite(baseline, " yyyy-MM-dd ");
    expect(sent).toEqual({ format: { datePattern: "yyyy-MM-dd" } });
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
    expect(sent.format).not.toHaveProperty("timePattern");
    expect(sent.format).not.toHaveProperty("currencyCode");
  });

  it("sends an empty date pattern to clear and skips an unchanged pattern", () => {
    expect(localeDatePatternWrite(baseline, "   ")).toEqual({ format: { datePattern: "" } });
    expect(localeDatePatternWrite(baseline, " MM/dd/yyyy ")).toBe("unchanged");
    expect(localeDatePatternWrite({ format: { datePattern: "  " } }, "\n")).toBe("unchanged");
    expect(storedLocaleDatePattern(" yyyy-MM-dd ")).toBe("yyyy-MM-dd");
  });
});

describe("savedLocaleDatePattern", () => {
  it("accepts a date pattern when language, label, description, status, and other format fields stay", () => {
    const sent = localeDatePatternWrite(baseline, "yyyy-MM-dd");
    expect(sent).not.toBe("unchanged");
    if (sent === "unchanged") {
      return;
    }
    expect(
      savedLocaleDatePattern(sent, baseline, {
        LocaleDetail: {
          ...baseline,
          format: { ...baseline.format, datePattern: " yyyy-MM-dd " },
        },
      }),
    ).toBe("yyyy-MM-dd");
  });

  it("accepts a blank date pattern that clears and keeps other format fields", () => {
    const sent = localeDatePatternWrite(baseline, "");
    expect(sent).toEqual({ format: { datePattern: "" } });
    if (sent === "unchanged") {
      return;
    }
    expect(
      savedLocaleDatePattern(sent, baseline, {
        ...baseline,
        format: { ...baseline.format, datePattern: "" },
      }),
    ).toBe("");
  });

  it("rejects a response that changes the language, label, description, status, or other format fields", () => {
    const sent = { format: { datePattern: "yyyy-MM-dd" } };
    expect(
      savedLocaleDatePattern(sent, baseline, {
        ...baseline,
        format: { ...baseline.format, datePattern: "yyyy-MM-dd" },
        languageString: "fr-ca",
      }),
    ).toBeNull();
    expect(
      savedLocaleDatePattern(sent, baseline, {
        ...baseline,
        label: "Anglais",
        format: { ...baseline.format, datePattern: "yyyy-MM-dd" },
      }),
    ).toBeNull();
    expect(
      savedLocaleDatePattern(sent, baseline, {
        ...baseline,
        description: "changed",
        format: { ...baseline.format, datePattern: "yyyy-MM-dd" },
      }),
    ).toBeNull();
    expect(
      savedLocaleDatePattern(sent, baseline, {
        ...baseline,
        status: "inactive",
        format: { ...baseline.format, datePattern: "yyyy-MM-dd" },
      }),
    ).toBeNull();
    expect(
      savedLocaleDatePattern(sent, baseline, {
        ...baseline,
        format: { ...baseline.format, datePattern: "yyyy-MM-dd", timePattern: "hh:mm a" },
      }),
    ).toBeNull();
    expect(
      savedLocaleDatePattern(
        { format: { datePattern: "yyyy-MM-dd", textDir: "ltr" } },
        baseline,
        { ...baseline, format: { ...baseline.format, datePattern: "yyyy-MM-dd" } },
      ),
    ).toBeNull();
    expect(
      savedLocaleDatePattern(
        { format: { datePattern: "yyyy-MM-dd" }, label: "English" },
        baseline,
        { ...baseline, format: { ...baseline.format, datePattern: "yyyy-MM-dd" } },
      ),
    ).toBeNull();
  });
});
