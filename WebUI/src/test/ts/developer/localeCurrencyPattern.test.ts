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
  localeCurrencyPatternWrite,
  savedLocaleCurrencyPattern,
  storedLocaleCurrencyPattern,
} from "../../../main/ts/developer/localeCurrencyPattern";

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
  },
};

describe("localeCurrencyPatternWrite", () => {
  it("sends the currency pattern only and omits language, label, description, status, and other format fields", () => {
    const sent = localeCurrencyPatternWrite(baseline, "  ");
    expect(sent).toEqual({ format: { currencyPattern: "" } });
    const spaceSent = localeCurrencyPatternWrite(baseline, " #,##0.00¤ ");
    expect(spaceSent).toEqual({ format: { currencyPattern: "#,##0.00¤" } });
    expect(spaceSent).not.toHaveProperty("languageString");
    expect(spaceSent).not.toHaveProperty("label");
    expect(spaceSent).not.toHaveProperty("description");
    expect(spaceSent).not.toHaveProperty("status");
    expect(spaceSent).not.toHaveProperty("baseLocale");
    expect(spaceSent).not.toHaveProperty("hasFormatProfile");
    if (spaceSent === "unchanged") {
      return;
    }
    expect(spaceSent.format).not.toHaveProperty("textDir");
    expect(spaceSent.format).not.toHaveProperty("datePattern");
    expect(spaceSent.format).not.toHaveProperty("timePattern");
    expect(spaceSent.format).not.toHaveProperty("dateTimePattern");
    expect(spaceSent.format).not.toHaveProperty("decimalSep");
    expect(spaceSent.format).not.toHaveProperty("groupingSep");
    expect(spaceSent.format).not.toHaveProperty("currencyCode");
  });

  it("sends an empty currency pattern to clear and skips an unchanged pattern", () => {
    expect(localeCurrencyPatternWrite(baseline, "   ")).toEqual({
      format: { currencyPattern: "" },
    });
    expect(localeCurrencyPatternWrite(baseline, " ¤#,##0.00 ")).toBe("unchanged");
    expect(localeCurrencyPatternWrite({ format: { currencyPattern: "  " } }, "\n")).toBe(
      "unchanged",
    );
    expect(storedLocaleCurrencyPattern(" #,##0.00¤ ")).toBe("#,##0.00¤");
  });
});

describe("savedLocaleCurrencyPattern", () => {
  it("accepts a currency pattern when language, label, description, status, and other format fields stay", () => {
    const sent = localeCurrencyPatternWrite(baseline, "#,##0.00¤");
    expect(sent).not.toBe("unchanged");
    if (sent === "unchanged") {
      return;
    }
    expect(
      savedLocaleCurrencyPattern(sent, baseline, {
        LocaleDetail: {
          ...baseline,
          format: { ...baseline.format, currencyPattern: " #,##0.00¤ " },
        },
      }),
    ).toBe("#,##0.00¤");
  });

  it("accepts a blank currency pattern that clears and keeps other format fields", () => {
    const sent = localeCurrencyPatternWrite(baseline, "");
    expect(sent).toEqual({ format: { currencyPattern: "" } });
    if (sent === "unchanged") {
      return;
    }
    expect(
      savedLocaleCurrencyPattern(sent, baseline, {
        ...baseline,
        format: { ...baseline.format, currencyPattern: "" },
      }),
    ).toBe("");
  });

  it("rejects a response that changes the language, label, description, status, or other format fields", () => {
    const sent = { format: { currencyPattern: "#,##0.00¤" } };
    expect(
      savedLocaleCurrencyPattern(sent, baseline, {
        ...baseline,
        format: { ...baseline.format, currencyPattern: "#,##0.00¤" },
        languageString: "fr-ca",
      }),
    ).toBeNull();
    expect(
      savedLocaleCurrencyPattern(sent, baseline, {
        ...baseline,
        label: "Anglais",
        format: { ...baseline.format, currencyPattern: "#,##0.00¤" },
      }),
    ).toBeNull();
    expect(
      savedLocaleCurrencyPattern(sent, baseline, {
        ...baseline,
        description: "changed",
        format: { ...baseline.format, currencyPattern: "#,##0.00¤" },
      }),
    ).toBeNull();
    expect(
      savedLocaleCurrencyPattern(sent, baseline, {
        ...baseline,
        status: "inactive",
        format: { ...baseline.format, currencyPattern: "#,##0.00¤" },
      }),
    ).toBeNull();
    expect(
      savedLocaleCurrencyPattern(sent, baseline, {
        ...baseline,
        format: {
          ...baseline.format,
          currencyPattern: "#,##0.00¤",
          currencyCode: "EUR",
        },
      }),
    ).toBeNull();
    expect(
      savedLocaleCurrencyPattern(
        { format: { currencyPattern: "#,##0.00¤", textDir: "ltr" } },
        baseline,
        { ...baseline, format: { ...baseline.format, currencyPattern: "#,##0.00¤" } },
      ),
    ).toBeNull();
    expect(
      savedLocaleCurrencyPattern(
        { format: { currencyPattern: "#,##0.00¤" }, label: "English" },
        baseline,
        { ...baseline, format: { ...baseline.format, currencyPattern: "#,##0.00¤" } },
      ),
    ).toBeNull();
  });
});
