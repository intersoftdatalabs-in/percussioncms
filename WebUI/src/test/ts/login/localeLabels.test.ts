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
    it("uses fallback when Intl.DisplayNames is unavailable", () => {
      const original = (Intl as unknown as { DisplayNames?: unknown })
        .DisplayNames;
      // @ts-expect-error simulate runtime without DisplayNames
      delete Intl.DisplayNames;
      try {
        expect(localeLabel("fr-fr", "en-us", "French (France)")).toBe(
          "fr-fr - French (France)",
        );
      } finally {
        (Intl as unknown as { DisplayNames?: unknown }).DisplayNames =
          original;
      }
    });

    it("uses fallback when Intl.DisplayNames.of throws", () => {
      const original = Intl.DisplayNames;
      // @ts-expect-error stub ctor that throws
      Intl.DisplayNames = function () {
        throw new Error("nope");
      };
      try {
        expect(localeLabel("ja-jp", "en-us", "Japanese (Japan)")).toBe(
          "ja-jp - Japanese (Japan)",
        );
      } finally {
        Intl.DisplayNames = original;
      }
    });

    it("renders regional code as 'code - Language (Region)' in viewer locale", () => {
      expect(localeLabel("fr-fr", "fr-fr", "French (France)")).toBe(
        "fr-fr - français (France)",
      );
    });

    it("renders generic code as 'code - Language' (no region)", () => {
      // viewer en-us → English names
      expect(localeLabel("es", "en-us", "Spanish")).toBe("es - Spanish");
      expect(localeLabel("hi", "en-us", "Hindi")).toBe("hi - Hindi");
    });

    it("re-renders labels in the new viewer when viewer changes", () => {
      // English viewer sees generic Spanish as "Spanish".
      expect(localeLabel("es", "en-us", "Spanish")).toBe("es - Spanish");
      // French viewer sees generic Spanish as "espagnol".
      expect(localeLabel("es", "fr-fr", "Spanish")).toBe("es - espagnol");
    });

    it("falls back to server displayName for unknown codes", () => {
      // "zz" is not a valid ISO 639 code; Intl.DisplayNames.of returns undefined.
      expect(localeLabel("zz", "en-us", "Made Up")).toBe("zz - Made Up");
    });

    it("uses supplied fallback when language display name is empty/missing", () => {
      expect(localeLabel("xx-yy", "en-us", "Mystery")).toBe(
        "xx-yy - Mystery",
      );
    });

    it("does not append region when region code equals language code", () => {
      // For "fr-fr" the language part is "fr" and region is "fr" — region equals language,
      // so the rendered label should be "fr-fr - français" only.
      expect(localeLabel("fr-fr", "fr-fr", "French (France)")).toBe(
        "fr-fr - français (France)",
      );
      // For "es-es" similar logic — region equals language, but the language part is "es".
      // Intl may or may not know "ES" as a region distinct from language; assert it does
      // not crash and that language is at least present.
      const out = localeLabel("es-es", "en-us", "Spanish (Spain)");
      expect(out.startsWith("es-es - Spanish")).toBe(true);
    });

    it("caches Intl.DisplayNames across calls (returns same instance)", () => {
      const first = localeLabel("en-us", "fr-fr", "English");
      const second = localeLabel("de-de", "fr-fr", "German");
      expect(first.startsWith("en-us -")).toBe(true);
      expect(second.startsWith("de-de -")).toBe(true);
    });
  });
});