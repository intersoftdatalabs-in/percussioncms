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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { afterEach, describe, expect, it, vi } from "vitest";
import {
  availableTargetLocales,
  createTranslations,
  listItemTranslationVariants,
  TranslationAuthError,
  type ItemTranslationVariants,
} from "../../../main/ts/api/contentExplorer/translationsApi";

const SAMPLE: ItemTranslationVariants = {
  itemId: 335,
  locale: "en-us",
  variants: [
    {
      contentId: 335,
      locale: "en-us",
      role: "source",
      revision: 1,
    },
    {
      contentId: 900,
      locale: "fr-fr",
      role: "translation",
      sourceContentId: 335,
    },
  ],
};

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe("translationsApi", () => {
  it("listItemTranslationVariants returns typed payload on 200", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(
        async () =>
          new Response(JSON.stringify(SAMPLE), {
            status: 200,
            headers: { "Content-Type": "application/json" },
          }),
      ),
    );
    const result = await listItemTranslationVariants("335");
    expect(result).toEqual(SAMPLE);
    expect(globalThis.fetch).toHaveBeenCalledWith(
      expect.stringContaining("/Rhythmyx/rest/content-explorer/translations/335"),
      expect.objectContaining({ method: "GET" }),
    );
  });

  it("listItemTranslationVariants keeps hyphenated GUID in the path (#3703)", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(
        async () =>
          new Response(JSON.stringify(SAMPLE), {
            status: 200,
            headers: { "Content-Type": "application/json" },
          }),
      ),
    );
    await listItemTranslationVariants("16777215-101-551");
    expect(globalThis.fetch).toHaveBeenCalledWith(
      expect.stringContaining(
        "/Rhythmyx/rest/content-explorer/translations/16777215-101-551",
      ),
      expect.objectContaining({ method: "GET" }),
    );
    const url = String((globalThis.fetch as ReturnType<typeof vi.fn>).mock.calls[0][0]);
    expect(url).not.toMatch(/\/translations\/551(?:\?|$)/);
  });

  it("listItemTranslationVariants maps 403 to TranslationAuthError", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => new Response("denied", { status: 403 })),
    );
    await expect(listItemTranslationVariants("private")).rejects.toBeInstanceOf(
      TranslationAuthError,
    );
  });

  it("createTranslations POSTs JSON body and returns created rows", async () => {
    const created = {
      created: [
        {
          contentId: 901,
          locale: "de-de",
          role: "translation",
          sourceContentId: 335,
        },
      ],
    };
    vi.stubGlobal(
      "fetch",
      vi.fn(async (_url: string, init?: RequestInit) => {
        expect(init?.method).toBe("POST");
        const body = JSON.parse(String(init?.body ?? "{}"));
        expect(body.itemIds).toEqual([335]);
        expect(body.locales).toEqual(["de-de"]);
        return new Response(JSON.stringify(created), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      }),
    );
    const result = await createTranslations({
      itemIds: [335],
      locales: ["de-de"],
    });
    expect(result.created?.[0]?.locale).toBe("de-de");
  });

  it("createTranslations maps 403 to TranslationAuthError", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => new Response("nope", { status: 403 })),
    );
    await expect(
      createTranslations({ itemIds: [1], locales: ["fr-fr"] }),
    ).rejects.toBeInstanceOf(TranslationAuthError);
  });
});

describe("availableTargetLocales", () => {
  it("excludes current locale and existing variant locales", () => {
    const targets = availableTargetLocales(
      [
        { languageString: "en-us", label: "English" },
        { languageString: "fr-fr", label: "French" },
        { languageString: "de-de", label: "German" },
        { languageString: "  ", label: "blank" },
      ],
      [{ contentId: 1, locale: "fr-fr", role: "translation" }],
      "en-us",
    );
    expect(targets.map((t) => t.languageString)).toEqual(["de-de"]);
    expect(targets[0].label).toBe("German");
  });

  it("dedupes catalog rows case-insensitively", () => {
    const targets = availableTargetLocales(
      [
        { languageString: "es-es", label: "Spanish" },
        { languageString: "ES-ES", label: "Spanish again" },
      ],
      [],
      null,
    );
    expect(targets).toHaveLength(1);
    expect(targets[0].languageString).toBe("es-es");
  });
});
