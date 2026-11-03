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
  localeDateTimePatternWrite,
  savedLocaleDateTimePattern,
  storedLocaleDateTimePattern,
} from "../../../main/ts/developer/localeDateTimePattern";

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
    currencyCode: "USD",
  },
};

describe("localeDateTimePatternWrite", () => {
  it("sends the date-time pattern only and omits language, label, description, status, and other format fields", () => {
    const sent = localeDateTimePatternWrite(baseline, " yyyy-MM-dd HH:mm:ss ");
    expect(sent).toEqual({ format: { dateTimePattern: "yyyy-MM-dd HH:mm:ss" } });
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
    expect(sent.format).not.toHaveProperty("timePattern");
    expect(sent.format).not.toHaveProperty("currencyCode");
  });

  it("sends an empty date-time pattern to clear and skips an unchanged pattern", () => {
    expect(localeDateTimePatternWrite(baseline, "   ")).toEqual({
      format: { dateTimePattern: "" },
    });
    expect(localeDateTimePatternWrite(baseline, " MM/dd/yyyy HH:mm:ss ")).toBe("unchanged");
    expect(localeDateTimePatternWrite({ format: { dateTimePattern: "  " } }, "\n")).toBe(
      "unchanged",
    );
    expect(storedLocaleDateTimePattern(" yyyy-MM-dd HH:mm:ss ")).toBe("yyyy-MM-dd HH:mm:ss");
  });
});

describe("savedLocaleDateTimePattern", () => {
  it("accepts a date-time pattern when language, label, description, status, and other format fields stay", () => {
    const sent = localeDateTimePatternWrite(baseline, "yyyy-MM-dd HH:mm:ss");
    expect(sent).not.toBe("unchanged");
    if (sent === "unchanged") {
      return;
    }
    expect(
      savedLocaleDateTimePattern(sent, baseline, {
        LocaleDetail: {
          ...baseline,
          format: { ...baseline.format, dateTimePattern: " yyyy-MM-dd HH:mm:ss " },
        },
      }),
    ).toBe("yyyy-MM-dd HH:mm:ss");
  });

  it("accepts a blank date-time pattern that clears and keeps other format fields", () => {
    const sent = localeDateTimePatternWrite(baseline, "");
    expect(sent).toEqual({ format: { dateTimePattern: "" } });
    if (sent === "unchanged") {
      return;
    }
    expect(
      savedLocaleDateTimePattern(sent, baseline, {
        ...baseline,
        format: { ...baseline.format, dateTimePattern: "" },
      }),
    ).toBe("");
  });

  it("rejects a response that changes the language, label, description, status, or other format fields", () => {
    const sent = { format: { dateTimePattern: "yyyy-MM-dd HH:mm:ss" } };
    expect(
      savedLocaleDateTimePattern(sent, baseline, {
        ...baseline,
        format: { ...baseline.format, dateTimePattern: "yyyy-MM-dd HH:mm:ss" },
        languageString: "fr-ca",
      }),
    ).toBeNull();
    expect(
      savedLocaleDateTimePattern(sent, baseline, {
        ...baseline,
        label: "Anglais",
        format: { ...baseline.format, dateTimePattern: "yyyy-MM-dd HH:mm:ss" },
      }),
    ).toBeNull();
    expect(
      savedLocaleDateTimePattern(sent, baseline, {
        ...baseline,
        description: "changed",
        format: { ...baseline.format, dateTimePattern: "yyyy-MM-dd HH:mm:ss" },
      }),
    ).toBeNull();
    expect(
      savedLocaleDateTimePattern(sent, baseline, {
        ...baseline,
        status: "inactive",
        format: { ...baseline.format, dateTimePattern: "yyyy-MM-dd HH:mm:ss" },
      }),
    ).toBeNull();
    expect(
      savedLocaleDateTimePattern(sent, baseline, {
        ...baseline,
        format: {
          ...baseline.format,
          dateTimePattern: "yyyy-MM-dd HH:mm:ss",
          datePattern: "yyyy-MM-dd",
        },
      }),
    ).toBeNull();
    expect(
      savedLocaleDateTimePattern(
        { format: { dateTimePattern: "yyyy-MM-dd HH:mm:ss", textDir: "ltr" } },
        baseline,
        { ...baseline, format: { ...baseline.format, dateTimePattern: "yyyy-MM-dd HH:mm:ss" } },
      ),
    ).toBeNull();
    expect(
      savedLocaleDateTimePattern(
        { format: { dateTimePattern: "yyyy-MM-dd HH:mm:ss" }, label: "English" },
        baseline,
        { ...baseline, format: { ...baseline.format, dateTimePattern: "yyyy-MM-dd HH:mm:ss" } },
      ),
    ).toBeNull();
  });
});
