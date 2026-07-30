/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

import { afterEach, describe, expect, it } from "vitest";
import {
  __resetLocaleLabelsCache,
  localeLabel,
  normalizeTag,
  SHIP_LOCALE_ENDONYMS,
} from "../../../main/ts/login/localeLabels";

describe("login/localeLabels", () => {
  afterEach(() => {
    __resetLocaleLabelsCache();
  });

  describe("normalizeTag", () => {
    it("lowercases and converts underscores to hyphens", () => {
      expect(normalizeTag("EN_US")).toBe("en-us");
      expect(normalizeTag("fr-FR")).toBe("fr-fr");
      expect(normalizeTag("ja_JP")).toBe("ja-jp");
    });

    it("trims surrounding whitespace", () => {
      expect(normalizeTag("  en-us  ")).toBe("en-us");
    });

    it("preserves generic (language-only) tags", () => {
      expect(normalizeTag("es")).toBe("es");
      expect(normalizeTag("hi")).toBe("hi");
    });

    it("returns empty string for nullish / empty input", () => {
      expect(normalizeTag("")).toBe("");
      expect(normalizeTag("   ")).toBe("");
    });
  });

  describe("localeLabel", () => {
    it("ship endonyms still work when Intl.DisplayNames is unavailable", () => {
      const original = (Intl as unknown as { DisplayNames?: unknown })
        .DisplayNames;
      // @ts-expect-error simulate runtime without DisplayNames
      delete Intl.DisplayNames;
      try {
        // Curated map does not depend on Intl.
        expect(localeLabel("fr-fr", "en-us", "French (France)")).toBe(
          "fr-fr - français (France)",
        );
        // Unknown codes still use the English server fallback.
        expect(localeLabel("zz", "en-us", "Made Up")).toBe("zz - Made Up");
      } finally {
        (Intl as unknown as { DisplayNames?: unknown }).DisplayNames = original;
      }
    });

    it("ship endonyms still work when Intl.DisplayNames.of throws", () => {
      const original = Intl.DisplayNames;
      // @ts-expect-error stub ctor that throws
      Intl.DisplayNames = function () {
        throw new Error("nope");
      };
      try {
        expect(localeLabel("ja-jp", "en-us", "Japanese (Japan)")).toBe(
          "ja-jp - 日本語 (日本)",
        );
      } finally {
        Intl.DisplayNames = original;
      }
    });

    it("renders regional code as endonym 'code - Language (Region)'", () => {
      // French endonym: language name in French regardless of UI locale.
      expect(localeLabel("fr-fr", "en-us", "French (France)")).toBe(
        "fr-fr - français (France)",
      );
    });

    it("renders generic code as endonym 'code - Language' (no region)", () => {
      expect(localeLabel("es", "en-us", "Spanish")).toBe("es - español");
      expect(localeLabel("hi", "en-us", "Hindi")).toMatch(/^hi - /);
    });

    it("keeps endonyms stable when the UI viewer locale changes (GH-1608)", () => {
      // Same option, different "viewer" — labels must not re-translate.
      const asEnglishUi = localeLabel("es", "en-us", "Spanish");
      const asFrenchUi = localeLabel("es", "fr-fr", "Spanish");
      const asHindiUi = localeLabel("es", "hi-in", "Spanish");
      expect(asEnglishUi).toBe("es - español");
      expect(asFrenchUi).toBe(asEnglishUi);
      expect(asHindiUi).toBe(asEnglishUi);

      const deEn = localeLabel("de-de", "en-us", "German (Germany)");
      const deHi = localeLabel("de-de", "hi-in", "German (Germany)");
      expect(deEn).toMatch(/^de-de - Deutsch/);
      expect(deHi).toBe(deEn);
    });

    it("uses curated ship endonyms for the product locale matrix", () => {
      expect(localeLabel("ar", "en-us", "Arabic")).toBe("ar - العربية");
      expect(localeLabel("ja-jp", "en-us", "Japanese (Japan)")).toBe(
        "ja-jp - 日本語 (日本)",
      );
      expect(localeLabel("tr-tr", "en-us", "Turkish (Turkey)")).toBe(
        "tr-tr - Türkçe (Türkiye)",
      );
      expect(localeLabel("hi-in", "en-us", "Hindi (India)")).toBe(
        "hi-in - हिन्दी (भारत)",
      );
      // Server English fallback is ignored when a ship endonym exists.
      expect(localeLabel("de-de", "en-us", "German (Germany)")).toBe(
        "de-de - Deutsch (Deutschland)",
      );
    });

    it("covers every key in SHIP_LOCALE_ENDONYMS", () => {
      for (const [code, endonym] of Object.entries(SHIP_LOCALE_ENDONYMS)) {
        expect(localeLabel(code, "en-us", "EnglishFallback")).toBe(
          `${code} - ${endonym}`,
        );
      }
    });

    it("falls back to server displayName for unknown codes", () => {
      // "zz" is not a valid ISO 639 code; Intl.DisplayNames.of returns undefined.
      expect(localeLabel("zz", "en-us", "Made Up")).toBe("zz - Made Up");
    });

    it("uses supplied fallback when language display name is empty/missing", () => {
      expect(localeLabel("xx-yy", "en-us", "Mystery")).toBe("xx-yy - Mystery");
    });

    it("does not crash for regional tags and includes language endonym", () => {
      const out = localeLabel("es-es", "en-us", "Spanish (Spain)");
      expect(out.startsWith("es-es - español")).toBe(true);
    });

    it("caches Intl.DisplayNames across calls (returns same instance)", () => {
      const first = localeLabel("en-us", "fr-fr", "English");
      const second = localeLabel("de-de", "fr-fr", "German");
      expect(first.startsWith("en-us -")).toBe(true);
      expect(second.startsWith("de-de -")).toBe(true);
    });
  });
});
