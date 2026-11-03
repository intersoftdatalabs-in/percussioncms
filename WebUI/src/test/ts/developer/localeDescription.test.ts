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
  localeDescriptionWrite,
  savedLocaleDescription,
  storedLocaleDescription,
} from "../../../main/ts/developer/localeDescription";

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
    currencyCode: "USD",
  },
};

describe("localeDescriptionWrite", () => {
  it("sends the description only and omits language, label, status, and format", () => {
    const sent = localeDescriptionWrite(baseline, " note ");
    expect(sent).toEqual({ description: "note" });
    expect(sent).not.toHaveProperty("languageString");
    expect(sent).not.toHaveProperty("label");
    expect(sent).not.toHaveProperty("status");
    expect(sent).not.toHaveProperty("baseLocale");
    expect(sent).not.toHaveProperty("hasFormatProfile");
    expect(sent).not.toHaveProperty("format");
  });

  it("sends an empty description to clear and skips an unchanged description", () => {
    expect(localeDescriptionWrite(baseline, "   ")).toEqual({ description: "" });
    expect(localeDescriptionWrite(baseline, " US English ")).toBe("unchanged");
    expect(localeDescriptionWrite({ description: "  " }, "\n")).toBe("unchanged");
    expect(storedLocaleDescription(" a\nb ")).toBe("a b");
  });
});

describe("savedLocaleDescription", () => {
  it("accepts a description when language, label, status, and format stay", () => {
    const sent = localeDescriptionWrite(baseline, "note");
    expect(sent).not.toBe("unchanged");
    if (sent === "unchanged") {
      return;
    }
    expect(
      savedLocaleDescription(sent, baseline, {
        LocaleDetail: { ...baseline, description: " note " },
      }),
    ).toBe("note");
  });

  it("accepts a blank description that clears and keeps label and format", () => {
    const sent = localeDescriptionWrite(baseline, "");
    expect(sent).toEqual({ description: "" });
    if (sent === "unchanged") {
      return;
    }
    expect(savedLocaleDescription(sent, baseline, { ...baseline, description: "" })).toBe("");
  });

  it("rejects a response that changes the language, label, status, or format", () => {
    const sent = { description: "note" };
    expect(
      savedLocaleDescription(sent, baseline, {
        ...baseline,
        description: "note",
        languageString: "fr-ca",
      }),
    ).toBeNull();
    expect(
      savedLocaleDescription(sent, baseline, { ...baseline, description: "note", label: "Anglais" }),
    ).toBeNull();
    expect(
      savedLocaleDescription(sent, baseline, {
        ...baseline,
        description: "note",
        status: "inactive",
      }),
    ).toBeNull();
    expect(
      savedLocaleDescription(sent, baseline, {
        ...baseline,
        description: "note",
        format: { ...baseline.format, datePattern: "yyyy-MM-dd" },
      }),
    ).toBeNull();
    expect(
      savedLocaleDescription(
        { description: "note", label: "English" },
        baseline,
        { ...baseline, description: "note" },
      ),
    ).toBeNull();
  });
});
