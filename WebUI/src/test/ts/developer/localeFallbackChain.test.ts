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
  formatLocaleFallbackChainForDisplay,
  isFallbackChainCode,
  localeFallbackChainWrite,
  parseLocaleFallbackChainInput,
  savedLocaleFallbackChain,
  storedLocaleFallbackChain,
} from "../../../main/ts/developer/localeFallbackChain";

const baseline: LocaleDetail = {
  id: 1,
  languageString: "de-de",
  label: "German",
  description: "Standard German",
  status: "active",
  baseLocale: false,
  hasFormatProfile: true,
  format: {
    languageString: "de-de",
    calendar: "gregory",
    fallbackChain: ["en-us", "es"],
  },
};

describe("localeFallbackChain", () => {
  it("storedLocaleFallbackChain normalizes arrays, scalars, and blanks", () => {
    expect(storedLocaleFallbackChain(["en-us", "es"])).toEqual(["en-us", "es"]);
    expect(storedLocaleFallbackChain("en-us")).toEqual(["en-us"]);
    expect(storedLocaleFallbackChain(null)).toEqual([]);
    expect(storedLocaleFallbackChain(undefined)).toEqual([]);
    expect(storedLocaleFallbackChain(["EN_us", "es", "en-us", " ", 7 as never])).toEqual([
      "en-us",
      "es",
    ]);
  });

  it("isFallbackChainCode mirrors the REST language pattern", () => {
    expect(isFallbackChainCode("en-us")).toBe(true);
    expect(isFallbackChainCode("EN_us")).toBe(true);
    expect(isFallbackChainCode("es")).toBe(true);
    expect(isFallbackChainCode("")).toBe(false);
    expect(isFallbackChainCode("en-us!!")).toBe(false);
    expect(isFallbackChainCode(null)).toBe(false);
  });

  it("parseLocaleFallbackChainInput splits on comma/semicolon/newline", () => {
    expect(parseLocaleFallbackChainInput("en-us, es;fr-fr\nit")).toEqual([
      "en-us",
      "es",
      "fr-fr",
      "it",
    ]);
    expect(parseLocaleFallbackChainInput("  ")).toEqual([]);
    expect(parseLocaleFallbackChainInput("en-us, en-us")).toEqual(["en-us"]);
    expect(parseLocaleFallbackChainInput("en-us, no!!")).toBe("invalid");
  });

  it("formatLocaleFallbackChainForDisplay joins with commas", () => {
    expect(formatLocaleFallbackChainForDisplay(["en-us", "es"])).toBe("en-us, es");
    expect(formatLocaleFallbackChainForDisplay([])).toBe("");
  });

  it("localeFallbackChainWrite sends a chain-only body", () => {
    expect(localeFallbackChainWrite(baseline, ["en-us", "fr-fr"])).toEqual({
      format: { fallbackChain: ["en-us", "fr-fr"] },
    });
  });

  it("localeFallbackChainWrite clears with an empty list via the blank-only wire encoding", () => {
    expect(localeFallbackChainWrite(baseline, [])).toEqual({
      format: { fallbackChain: [""] },
    });
  });

  it("localeFallbackChainWrite normalizes blanks and duplicates before compare", () => {
    expect(localeFallbackChainWrite(baseline, ["EN_us", "es", "en-us"])).toBe("unchanged");
  });

  it("localeFallbackChainWrite reports unchanged for the stored chain", () => {
    expect(localeFallbackChainWrite(baseline, ["en-us", "es"])).toBe("unchanged");
  });

  it("localeFallbackChainWrite rejects bad codes and self reference", () => {
    expect(localeFallbackChainWrite(baseline, ["en-us!!"])).toBe("invalid");
    expect(localeFallbackChainWrite(baseline, ["en-us", "de-de"])).toBe("invalid");
  });

  it("savedLocaleFallbackChain accepts the cleared chain echo", () => {
    const cleared = { format: { fallbackChain: [""] } };
    const clearedSaved = {
      ...baseline,
      format: { ...(baseline.format || {}), fallbackChain: undefined },
    };
    expect(savedLocaleFallbackChain(cleared, baseline, { LocaleDetail: clearedSaved })).toEqual(
      [],
    );
  });

  it("savedLocaleFallbackChain accepts the echoed chain and guards other fields", () => {
    const sent = { format: { fallbackChain: ["en-us", "fr-fr"] } };
    const saved = {
      ...baseline,
      format: { ...(baseline.format || {}), fallbackChain: ["en-us", "fr-fr"] },
    };
    expect(savedLocaleFallbackChain(sent, baseline, { LocaleDetail: saved })).toEqual([
      "en-us",
      "fr-fr",
    ]);
  });

  it("savedLocaleFallbackChain rejects touched label/status/format", () => {
    const sent = { format: { fallbackChain: ["en-us"] } };
    const renamed = { ...baseline, label: "Renamed" };
    expect(savedLocaleFallbackChain(sent, baseline, { LocaleDetail: renamed })).toBeNull();
    const reformatted = {
      ...baseline,
      format: { ...(baseline.format || {}), fallbackChain: ["en-us"], calendar: "buddhist" },
    };
    expect(savedLocaleFallbackChain(sent, baseline, { LocaleDetail: reformatted })).toBeNull();
    const mismatched = {
      ...baseline,
      format: { ...(baseline.format || {}), fallbackChain: ["it"] },
    };
    expect(savedLocaleFallbackChain(sent, baseline, { LocaleDetail: mismatched })).toBeNull();
  });

  it("savedLocaleFallbackChain rejects non-chain bodies", () => {
    expect(
      savedLocaleFallbackChain(
        { label: "x" },
        baseline,
        { LocaleDetail: baseline },
      ),
    ).toBeNull();
    expect(
      savedLocaleFallbackChain(
        { format: { calendar: "islamic" } },
        baseline,
        { LocaleDetail: baseline },
      ),
    ).toBeNull();
  });
});
