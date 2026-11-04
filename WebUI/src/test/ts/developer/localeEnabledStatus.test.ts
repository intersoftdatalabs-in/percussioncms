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
  localeEnabledStatusWrite,
  savedLocaleEnabledStatus,
  storedLocaleEnabledStatus,
} from "../../../main/ts/developer/localeEnabledStatus";

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

describe("localeEnabledStatusWrite", () => {
  it("sends the status only and omits language, label, description, and format", () => {
    const sent = localeEnabledStatusWrite(baseline, " INACTIVE ");
    expect(sent).toEqual({ status: "inactive" });
    expect(sent).not.toHaveProperty("languageString");
    expect(sent).not.toHaveProperty("label");
    expect(sent).not.toHaveProperty("description");
    expect(sent).not.toHaveProperty("baseLocale");
    expect(sent).not.toHaveProperty("hasFormatProfile");
    expect(sent).not.toHaveProperty("format");
  });

  it("skips an unchanged status and does not write an invalid status", () => {
    expect(localeEnabledStatusWrite(baseline, " Active ")).toBe("unchanged");
    expect(localeEnabledStatusWrite(baseline, "   ")).toBe("invalid");
    expect(localeEnabledStatusWrite(baseline, "nope")).toBe("invalid");
    expect(storedLocaleEnabledStatus(" INACTIVE ")).toBe("inactive");
    expect(storedLocaleEnabledStatus("bogus")).toBe("");
  });
});

describe("savedLocaleEnabledStatus", () => {
  it("accepts a status when language, label, description, and format stay", () => {
    const sent = localeEnabledStatusWrite(baseline, "inactive");
    expect(sent).not.toBe("unchanged");
    expect(sent).not.toBe("invalid");
    if (sent === "unchanged" || sent === "invalid") {
      return;
    }
    expect(
      savedLocaleEnabledStatus(sent, baseline, {
        LocaleDetail: { ...baseline, status: " INACTIVE " },
      }),
    ).toBe("inactive");
  });

  it("rejects a response that changes the language, label, description, or format", () => {
    const sent = { status: "inactive" };
    expect(
      savedLocaleEnabledStatus(sent, baseline, {
        ...baseline,
        status: "inactive",
        languageString: "fr-ca",
      }),
    ).toBeNull();
    expect(
      savedLocaleEnabledStatus(sent, baseline, {
        ...baseline,
        status: "inactive",
        label: "Anglais",
      }),
    ).toBeNull();
    expect(
      savedLocaleEnabledStatus(sent, baseline, {
        ...baseline,
        status: "inactive",
        description: "changed",
      }),
    ).toBeNull();
    expect(
      savedLocaleEnabledStatus(sent, baseline, {
        ...baseline,
        status: "inactive",
        format: { ...baseline.format, groupingSep: " " },
      }),
    ).toBeNull();
    expect(
      savedLocaleEnabledStatus(
        { status: "inactive", label: "English" },
        baseline,
        { ...baseline, status: "inactive" },
      ),
    ).toBeNull();
    expect(
      savedLocaleEnabledStatus(sent, baseline, {
        ...baseline,
        status: "active",
      }),
    ).toBeNull();
  });
});
