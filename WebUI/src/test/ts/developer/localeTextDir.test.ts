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
  localeTextDirWrite,
  savedLocaleTextDir,
  storedLocaleTextDir,
} from "../../../main/ts/developer/localeTextDir";

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
  },
};

describe("localeTextDirWrite", () => {
  it("sends the text direction only and omits language, label, description, status, and other format fields", () => {
    const sent = localeTextDirWrite(baseline, " RTL ");
    expect(sent).toEqual({ format: { textDir: "rtl" } });
    expect(sent).not.toHaveProperty("languageString");
    expect(sent).not.toHaveProperty("label");
    expect(sent).not.toHaveProperty("description");
    expect(sent).not.toHaveProperty("status");
    expect(sent).not.toHaveProperty("baseLocale");
    expect(sent).not.toHaveProperty("hasFormatProfile");
    if (sent === "unchanged" || sent === "invalid") {
      return;
    }
    expect(sent.format).not.toHaveProperty("datePattern");
    expect(sent.format).not.toHaveProperty("timePattern");
    expect(sent.format).not.toHaveProperty("dateTimePattern");
    expect(sent.format).not.toHaveProperty("decimalSep");
    expect(sent.format).not.toHaveProperty("groupingSep");
    expect(sent.format).not.toHaveProperty("currencyCode");
  });

  it("sends an empty text direction to clear, skips an unchanged direction, and refuses an invalid value", () => {
    expect(localeTextDirWrite(baseline, "   ")).toEqual({
      format: { textDir: "" },
    });
    expect(localeTextDirWrite(baseline, " LTR ")).toBe("unchanged");
    expect(localeTextDirWrite({ format: { textDir: "  " } }, "\n")).toBe("unchanged");
    expect(localeTextDirWrite(baseline, "vertical")).toBe("invalid");
    expect(storedLocaleTextDir(" RTL ")).toBe("rtl");
    expect(storedLocaleTextDir("foo")).toBe("");
  });
});

describe("savedLocaleTextDir", () => {
  it("accepts a text direction when language, label, description, status, and other format fields stay", () => {
    const sent = localeTextDirWrite(baseline, "rtl");
    expect(sent).not.toBe("unchanged");
    expect(sent).not.toBe("invalid");
    if (sent === "unchanged" || sent === "invalid") {
      return;
    }
    expect(
      savedLocaleTextDir(sent, baseline, {
        LocaleDetail: {
          ...baseline,
          format: { ...baseline.format, textDir: " RTL " },
        },
      }),
    ).toBe("rtl");
  });

  it("accepts a blank text direction that clears and keeps other format fields", () => {
    const sent = localeTextDirWrite(baseline, "");
    expect(sent).toEqual({ format: { textDir: "" } });
    if (sent === "unchanged" || sent === "invalid") {
      return;
    }
    expect(
      savedLocaleTextDir(sent, baseline, {
        ...baseline,
        format: { ...baseline.format, textDir: "" },
      }),
    ).toBe("");
  });

  it("rejects a response that changes the language, label, description, status, or other format fields", () => {
    const sent = { format: { textDir: "rtl" } };
    expect(
      savedLocaleTextDir(sent, baseline, {
        ...baseline,
        format: { ...baseline.format, textDir: "rtl" },
        languageString: "fr-ca",
      }),
    ).toBeNull();
    expect(
      savedLocaleTextDir(sent, baseline, {
        ...baseline,
        label: "Anglais",
        format: { ...baseline.format, textDir: "rtl" },
      }),
    ).toBeNull();
    expect(
      savedLocaleTextDir(sent, baseline, {
        ...baseline,
        description: "changed",
        format: { ...baseline.format, textDir: "rtl" },
      }),
    ).toBeNull();
    expect(
      savedLocaleTextDir(sent, baseline, {
        ...baseline,
        status: "inactive",
        format: { ...baseline.format, textDir: "rtl" },
      }),
    ).toBeNull();
    expect(
      savedLocaleTextDir(sent, baseline, {
        ...baseline,
        format: {
          ...baseline.format,
          textDir: "rtl",
          decimalSep: ",",
        },
      }),
    ).toBeNull();
    expect(
      savedLocaleTextDir(
        { format: { textDir: "rtl", decimalSep: "." } },
        baseline,
        { ...baseline, format: { ...baseline.format, textDir: "rtl" } },
      ),
    ).toBeNull();
    expect(
      savedLocaleTextDir(
        { format: { textDir: "rtl" }, label: "English" },
        baseline,
        { ...baseline, format: { ...baseline.format, textDir: "rtl" } },
      ),
    ).toBeNull();
  });
});
