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
  localeCurrencyCodeWrite,
  savedLocaleCurrencyCode,
  storedLocaleCurrencyCode,
} from "../../../main/ts/developer/localeCurrencyCode";

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

describe("localeCurrencyCodeWrite", () => {
  it("sends the currency code only and omits language, label, description, status, and other format fields", () => {
    const sent = localeCurrencyCodeWrite(baseline, "  ");
    expect(sent).toEqual({ format: { currencyCode: "" } });
    const spaceSent = localeCurrencyCodeWrite(baseline, " EUR ");
    expect(spaceSent).toEqual({ format: { currencyCode: "EUR" } });
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
    expect(spaceSent.format).not.toHaveProperty("currencyPattern");
  });

  it("sends an empty currency code to clear and skips an unchanged code", () => {
    expect(localeCurrencyCodeWrite(baseline, "   ")).toEqual({
      format: { currencyCode: "" },
    });
    expect(localeCurrencyCodeWrite(baseline, " USD ")).toBe("unchanged");
    expect(localeCurrencyCodeWrite({ format: { currencyCode: "  " } }, "\n")).toBe("unchanged");
    expect(storedLocaleCurrencyCode(" EUR ")).toBe("EUR");
  });
});

describe("savedLocaleCurrencyCode", () => {
  it("accepts a currency code when language, label, description, status, and other format fields stay", () => {
    const sent = localeCurrencyCodeWrite(baseline, "EUR");
    expect(sent).not.toBe("unchanged");
    if (sent === "unchanged") {
      return;
    }
    expect(
      savedLocaleCurrencyCode(sent, baseline, {
        LocaleDetail: {
          ...baseline,
          format: { ...baseline.format, currencyCode: " EUR " },
        },
      }),
    ).toBe("EUR");
  });

  it("accepts a blank currency code that clears and keeps other format fields", () => {
    const sent = localeCurrencyCodeWrite(baseline, "");
    expect(sent).toEqual({ format: { currencyCode: "" } });
    if (sent === "unchanged") {
      return;
    }
    expect(
      savedLocaleCurrencyCode(sent, baseline, {
        ...baseline,
        format: { ...baseline.format, currencyCode: "" },
      }),
    ).toBe("");
  });

  it("rejects a response that changes the language, label, description, status, or other format fields", () => {
    const sent = { format: { currencyCode: "EUR" } };
    expect(
      savedLocaleCurrencyCode(sent, baseline, {
        ...baseline,
        format: { ...baseline.format, currencyCode: "EUR" },
        languageString: "fr-ca",
      }),
    ).toBeNull();
    expect(
      savedLocaleCurrencyCode(sent, baseline, {
        ...baseline,
        label: "Anglais",
        format: { ...baseline.format, currencyCode: "EUR" },
      }),
    ).toBeNull();
    expect(
      savedLocaleCurrencyCode(sent, baseline, {
        ...baseline,
        description: "changed",
        format: { ...baseline.format, currencyCode: "EUR" },
      }),
    ).toBeNull();
    expect(
      savedLocaleCurrencyCode(sent, baseline, {
        ...baseline,
        status: "inactive",
        format: { ...baseline.format, currencyCode: "EUR" },
      }),
    ).toBeNull();
    expect(
      savedLocaleCurrencyCode(sent, baseline, {
        ...baseline,
        format: {
          ...baseline.format,
          currencyCode: "EUR",
          groupingSep: " ",
        },
      }),
    ).toBeNull();
    expect(
      savedLocaleCurrencyCode(
        { format: { currencyCode: "EUR", textDir: "ltr" } },
        baseline,
        { ...baseline, format: { ...baseline.format, currencyCode: "EUR" } },
      ),
    ).toBeNull();
    expect(
      savedLocaleCurrencyCode(
        { format: { currencyCode: "EUR" }, label: "English" },
        baseline,
        { ...baseline, format: { ...baseline.format, currencyCode: "EUR" } },
      ),
    ).toBeNull();
  });
});
