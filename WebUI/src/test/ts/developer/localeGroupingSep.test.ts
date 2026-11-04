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
  localeGroupingSepWrite,
  savedLocaleGroupingSep,
  storedLocaleGroupingSep,
} from "../../../main/ts/developer/localeGroupingSep";

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

describe("localeGroupingSepWrite", () => {
  it("sends the grouping separator only and omits language, label, description, status, and other format fields", () => {
    const sent = localeGroupingSepWrite(baseline, "  ");
    expect(sent).toEqual({ format: { groupingSep: "" } });
    const spaceSent = localeGroupingSepWrite(baseline, " . ");
    expect(spaceSent).toEqual({ format: { groupingSep: "." } });
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
    expect(spaceSent.format).not.toHaveProperty("currencyCode");
  });

  it("sends an empty grouping separator to clear and skips an unchanged separator", () => {
    expect(localeGroupingSepWrite(baseline, "   ")).toEqual({
      format: { groupingSep: "" },
    });
    expect(localeGroupingSepWrite(baseline, " , ")).toBe("unchanged");
    expect(localeGroupingSepWrite({ format: { groupingSep: "  " } }, "\n")).toBe("unchanged");
    expect(storedLocaleGroupingSep(" , ")).toBe(",");
  });
});

describe("savedLocaleGroupingSep", () => {
  it("accepts a grouping separator when language, label, description, status, and other format fields stay", () => {
    const sent = localeGroupingSepWrite(baseline, ".");
    expect(sent).not.toBe("unchanged");
    if (sent === "unchanged") {
      return;
    }
    expect(
      savedLocaleGroupingSep(sent, baseline, {
        LocaleDetail: {
          ...baseline,
          format: { ...baseline.format, groupingSep: " . " },
        },
      }),
    ).toBe(".");
  });

  it("accepts a blank grouping separator that clears and keeps other format fields", () => {
    const sent = localeGroupingSepWrite(baseline, "");
    expect(sent).toEqual({ format: { groupingSep: "" } });
    if (sent === "unchanged") {
      return;
    }
    expect(
      savedLocaleGroupingSep(sent, baseline, {
        ...baseline,
        format: { ...baseline.format, groupingSep: "" },
      }),
    ).toBe("");
  });

  it("rejects a response that changes the language, label, description, status, or other format fields", () => {
    const sent = { format: { groupingSep: "." } };
    expect(
      savedLocaleGroupingSep(sent, baseline, {
        ...baseline,
        format: { ...baseline.format, groupingSep: "." },
        languageString: "fr-ca",
      }),
    ).toBeNull();
    expect(
      savedLocaleGroupingSep(sent, baseline, {
        ...baseline,
        label: "Anglais",
        format: { ...baseline.format, groupingSep: "." },
      }),
    ).toBeNull();
    expect(
      savedLocaleGroupingSep(sent, baseline, {
        ...baseline,
        description: "changed",
        format: { ...baseline.format, groupingSep: "." },
      }),
    ).toBeNull();
    expect(
      savedLocaleGroupingSep(sent, baseline, {
        ...baseline,
        status: "inactive",
        format: { ...baseline.format, groupingSep: "." },
      }),
    ).toBeNull();
    expect(
      savedLocaleGroupingSep(sent, baseline, {
        ...baseline,
        format: {
          ...baseline.format,
          groupingSep: ".",
          decimalSep: ",",
        },
      }),
    ).toBeNull();
    expect(
      savedLocaleGroupingSep(
        { format: { groupingSep: ".", textDir: "ltr" } },
        baseline,
        { ...baseline, format: { ...baseline.format, groupingSep: "." } },
      ),
    ).toBeNull();
    expect(
      savedLocaleGroupingSep(
        { format: { groupingSep: "." }, label: "English" },
        baseline,
        { ...baseline, format: { ...baseline.format, groupingSep: "." } },
      ),
    ).toBeNull();
  });
});
