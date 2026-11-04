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
  localeMeasurementSystemWrite,
  parseLocaleMeasurementSystem,
  savedLocaleMeasurementSystem,
  storedLocaleMeasurementSystem,
} from "../../../main/ts/developer/localeMeasurementSystem";

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
    firstDayOfWeek: 1,
    measurementSystem: "us",
  },
};

describe("localeMeasurementSystemWrite", () => {
  it("sends the measurement system only and omits language, label, description, status, and other format fields", () => {
    const sent = localeMeasurementSystemWrite(baseline, "  ");
    expect(sent).toEqual({ format: { measurementSystem: "" } });
    const next = localeMeasurementSystemWrite(baseline, " METRIC ");
    expect(next).toEqual({ format: { measurementSystem: "metric" } });
    expect(next).not.toHaveProperty("languageString");
    expect(next).not.toHaveProperty("label");
    expect(next).not.toHaveProperty("description");
    expect(next).not.toHaveProperty("status");
    expect(next).not.toHaveProperty("baseLocale");
    expect(next).not.toHaveProperty("hasFormatProfile");
    if (next === "unchanged" || next === "invalid") {
      return;
    }
    expect(next.format).not.toHaveProperty("textDir");
    expect(next.format).not.toHaveProperty("datePattern");
    expect(next.format).not.toHaveProperty("timePattern");
    expect(next.format).not.toHaveProperty("dateTimePattern");
    expect(next.format).not.toHaveProperty("decimalSep");
    expect(next.format).not.toHaveProperty("groupingSep");
    expect(next.format).not.toHaveProperty("currencyCode");
    expect(next.format).not.toHaveProperty("currencyPattern");
    expect(next.format).not.toHaveProperty("firstDayOfWeek");
  });

  it("sends an empty measurement system to clear, skips an unchanged value, and rejects unknown values", () => {
    expect(localeMeasurementSystemWrite(baseline, "   ")).toEqual({
      format: { measurementSystem: "" },
    });
    expect(localeMeasurementSystemWrite(baseline, " US ")).toBe("unchanged");
    expect(localeMeasurementSystemWrite({ format: { measurementSystem: "  " } }, "\n")).toBe(
      "unchanged",
    );
    expect(localeMeasurementSystemWrite(baseline, "imperial")).toBe("invalid");
    expect(localeMeasurementSystemWrite(baseline, "si")).toBe("invalid");
    expect(storedLocaleMeasurementSystem(" METRIC ")).toBe("metric");
    expect(parseLocaleMeasurementSystem("uk")).toBe("uk");
    expect(parseLocaleMeasurementSystem("")).toBeNull();
  });
});

describe("savedLocaleMeasurementSystem", () => {
  it("accepts a measurement system when language, label, description, status, and other format fields stay", () => {
    const sent = localeMeasurementSystemWrite(baseline, "metric");
    expect(sent).not.toBe("unchanged");
    expect(sent).not.toBe("invalid");
    if (sent === "unchanged" || sent === "invalid") {
      return;
    }
    expect(
      savedLocaleMeasurementSystem(sent, baseline, {
        LocaleDetail: {
          ...baseline,
          format: { ...baseline.format, measurementSystem: " metric " },
        },
      }),
    ).toBe("metric");
  });

  it("accepts a blank measurement system that clears and keeps other format fields", () => {
    const sent = localeMeasurementSystemWrite(baseline, "");
    expect(sent).toEqual({ format: { measurementSystem: "" } });
    if (sent === "unchanged" || sent === "invalid") {
      return;
    }
    expect(
      savedLocaleMeasurementSystem(sent, baseline, {
        ...baseline,
        format: { ...baseline.format, measurementSystem: "" },
      }),
    ).toBe("");
  });

  it("rejects a response that changes the language, label, description, status, or other format fields", () => {
    const sent = { format: { measurementSystem: "metric" } };
    expect(
      savedLocaleMeasurementSystem(sent, baseline, {
        ...baseline,
        format: { ...baseline.format, measurementSystem: "metric" },
        languageString: "fr-ca",
      }),
    ).toBeNull();
    expect(
      savedLocaleMeasurementSystem(sent, baseline, {
        ...baseline,
        label: "Anglais",
        format: { ...baseline.format, measurementSystem: "metric" },
      }),
    ).toBeNull();
    expect(
      savedLocaleMeasurementSystem(sent, baseline, {
        ...baseline,
        description: "changed",
        format: { ...baseline.format, measurementSystem: "metric" },
      }),
    ).toBeNull();
    expect(
      savedLocaleMeasurementSystem(sent, baseline, {
        ...baseline,
        status: "inactive",
        format: { ...baseline.format, measurementSystem: "metric" },
      }),
    ).toBeNull();
    expect(
      savedLocaleMeasurementSystem(sent, baseline, {
        ...baseline,
        format: {
          ...baseline.format,
          measurementSystem: "metric",
          currencyCode: "EUR",
        },
      }),
    ).toBeNull();
    expect(
      savedLocaleMeasurementSystem(
        { format: { measurementSystem: "metric", textDir: "ltr" } },
        baseline,
        { ...baseline, format: { ...baseline.format, measurementSystem: "metric" } },
      ),
    ).toBeNull();
    expect(
      savedLocaleMeasurementSystem(
        { format: { measurementSystem: "metric" }, label: "English" },
        baseline,
        { ...baseline, format: { ...baseline.format, measurementSystem: "metric" } },
      ),
    ).toBeNull();
  });
});
