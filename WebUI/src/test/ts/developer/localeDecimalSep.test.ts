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
  localeDecimalSepWrite,
  savedLocaleDecimalSep,
  storedLocaleDecimalSep,
} from "../../../main/ts/developer/localeDecimalSep";

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

describe("localeDecimalSepWrite", () => {
  it("sends the decimal separator only and omits language, label, description, status, and other format fields", () => {
    const sent = localeDecimalSepWrite(baseline, " , ");
    expect(sent).toEqual({ format: { decimalSep: "," } });
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
    expect(sent.format).not.toHaveProperty("dateTimePattern");
    expect(sent.format).not.toHaveProperty("groupingSep");
    expect(sent.format).not.toHaveProperty("currencyCode");
  });

  it("sends an empty decimal separator to clear and skips an unchanged separator", () => {
    expect(localeDecimalSepWrite(baseline, "   ")).toEqual({
      format: { decimalSep: "" },
    });
    expect(localeDecimalSepWrite(baseline, " . ")).toBe("unchanged");
    expect(localeDecimalSepWrite({ format: { decimalSep: "  " } }, "\n")).toBe("unchanged");
    expect(storedLocaleDecimalSep(" , ")).toBe(",");
  });
});

describe("savedLocaleDecimalSep", () => {
  it("accepts a decimal separator when language, label, description, status, and other format fields stay", () => {
    const sent = localeDecimalSepWrite(baseline, ",");
    expect(sent).not.toBe("unchanged");
    if (sent === "unchanged") {
      return;
    }
    expect(
      savedLocaleDecimalSep(sent, baseline, {
        LocaleDetail: {
          ...baseline,
          format: { ...baseline.format, decimalSep: " , " },
        },
      }),
    ).toBe(",");
  });

  it("accepts a blank decimal separator that clears and keeps other format fields", () => {
    const sent = localeDecimalSepWrite(baseline, "");
    expect(sent).toEqual({ format: { decimalSep: "" } });
    if (sent === "unchanged") {
      return;
    }
    expect(
      savedLocaleDecimalSep(sent, baseline, {
        ...baseline,
        format: { ...baseline.format, decimalSep: "" },
      }),
    ).toBe("");
  });

  it("rejects a response that changes the language, label, description, status, or other format fields", () => {
    const sent = { format: { decimalSep: "," } };
    expect(
      savedLocaleDecimalSep(sent, baseline, {
        ...baseline,
        format: { ...baseline.format, decimalSep: "," },
        languageString: "fr-ca",
      }),
    ).toBeNull();
    expect(
      savedLocaleDecimalSep(sent, baseline, {
        ...baseline,
        label: "Anglais",
        format: { ...baseline.format, decimalSep: "," },
      }),
    ).toBeNull();
    expect(
      savedLocaleDecimalSep(sent, baseline, {
        ...baseline,
        description: "changed",
        format: { ...baseline.format, decimalSep: "," },
      }),
    ).toBeNull();
    expect(
      savedLocaleDecimalSep(sent, baseline, {
        ...baseline,
        status: "inactive",
        format: { ...baseline.format, decimalSep: "," },
      }),
    ).toBeNull();
    expect(
      savedLocaleDecimalSep(sent, baseline, {
        ...baseline,
        format: {
          ...baseline.format,
          decimalSep: ",",
          groupingSep: " ",
        },
      }),
    ).toBeNull();
    expect(
      savedLocaleDecimalSep(
        { format: { decimalSep: ",", textDir: "ltr" } },
        baseline,
        { ...baseline, format: { ...baseline.format, decimalSep: "," } },
      ),
    ).toBeNull();
    expect(
      savedLocaleDecimalSep(
        { format: { decimalSep: "," }, label: "English" },
        baseline,
        { ...baseline, format: { ...baseline.format, decimalSep: "," } },
      ),
    ).toBeNull();
  });
});
