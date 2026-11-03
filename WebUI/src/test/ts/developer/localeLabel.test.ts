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
  localeLabelWrite,
  savedLocaleLabel,
  storedLocaleLabel,
} from "../../../main/ts/developer/localeLabel";

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

describe("localeLabelWrite", () => {
  it("sends the label only and omits language, description, status, and format", () => {
    const sent = localeLabelWrite(baseline, " US English ");
    expect(sent).toEqual({ label: "US English" });
    expect(sent).not.toHaveProperty("languageString");
    expect(sent).not.toHaveProperty("description");
    expect(sent).not.toHaveProperty("status");
    expect(sent).not.toHaveProperty("baseLocale");
    expect(sent).not.toHaveProperty("hasFormatProfile");
    expect(sent).not.toHaveProperty("format");
  });

  it("skips an unchanged label and does not write a blank or whitespace label", () => {
    expect(localeLabelWrite(baseline, " English ")).toBe("unchanged");
    expect(localeLabelWrite(baseline, "   ")).toBe("blank");
    expect(localeLabelWrite(baseline, "\n")).toBe("blank");
    expect(localeLabelWrite({ label: "  " }, "")).toBe("blank");
    expect(storedLocaleLabel(" a\nb ")).toBe("a b");
  });
});

describe("savedLocaleLabel", () => {
  it("accepts a label when language, description, status, and format stay", () => {
    const sent = localeLabelWrite(baseline, "US English");
    expect(sent).not.toBe("unchanged");
    expect(sent).not.toBe("blank");
    if (sent === "unchanged" || sent === "blank") {
      return;
    }
    expect(
      savedLocaleLabel(sent, baseline, {
        LocaleDetail: { ...baseline, label: " US English " },
      }),
    ).toBe("US English");
  });

  it("rejects a response that changes the language, description, status, or format", () => {
    const sent = { label: "US English" };
    expect(
      savedLocaleLabel(sent, baseline, {
        ...baseline,
        label: "US English",
        languageString: "fr-ca",
      }),
    ).toBeNull();
    expect(
      savedLocaleLabel(sent, baseline, {
        ...baseline,
        label: "US English",
        description: "changed",
      }),
    ).toBeNull();
    expect(
      savedLocaleLabel(sent, baseline, {
        ...baseline,
        label: "US English",
        status: "inactive",
      }),
    ).toBeNull();
    expect(
      savedLocaleLabel(sent, baseline, {
        ...baseline,
        label: "US English",
        format: { ...baseline.format, datePattern: "yyyy-MM-dd" },
      }),
    ).toBeNull();
    expect(
      savedLocaleLabel(
        { label: "US English", description: "US English" },
        baseline,
        { ...baseline, label: "US English" },
      ),
    ).toBeNull();
    expect(savedLocaleLabel({ label: "" }, baseline, { ...baseline, label: "" })).toBeNull();
  });
});
