/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  createLocale,
  deleteLocale,
  isLocaleWriteReady,
  isValidLanguageString,
  listLocales,
  normalizeLanguageString,
  unwrapLocaleDetail,
  updateLocale,
  wrapLocaleDetailForWire,
} from "../../../../main/ts/api/developer/localesApi";
import { PATHS } from "../../../../main/ts/api/paths";

describe("locale language validation", () => {
  it("normalizes case and underscore", () => {
    expect(normalizeLanguageString("FR_CA")).toBe("fr-ca");
    expect(normalizeLanguageString("  en-US ")).toBe("en-us");
    expect(normalizeLanguageString("")).toBe("");
  });

  it("accepts BCP-47 style keys and rejects junk", () => {
    expect(isValidLanguageString("en-us")).toBe(true);
    expect(isValidLanguageString("xx-qa4005")).toBe(true);
    expect(isValidLanguageString("FR_CA")).toBe(true);
    expect(isValidLanguageString("")).toBe(false);
    expect(isValidLanguageString("../x")).toBe(false);
    expect(isValidLanguageString("1en")).toBe(false);
  });

  it("disables write until language (create) and label are valid", () => {
    expect(isLocaleWriteReady({ isNew: true, language: "", label: "" })).toBe(false);
    expect(isLocaleWriteReady({ isNew: true, language: "fr-ca", label: "" })).toBe(false);
    expect(isLocaleWriteReady({ isNew: true, language: "", label: "French" })).toBe(false);
    expect(isLocaleWriteReady({ isNew: true, language: "fr-ca", label: "French" })).toBe(
      true,
    );
    expect(isLocaleWriteReady({ isNew: false, language: "", label: "English" })).toBe(true);
    expect(isLocaleWriteReady({ isNew: false, language: "en-us", label: "  " })).toBe(false);
  });
});

describe("locale detail wire wrap", () => {
  it("wraps POST/PUT under LocaleDetail root", () => {
    expect(wrapLocaleDetailForWire({ languageString: "fr-ca", label: "French" })).toEqual({
      LocaleDetail: { languageString: "fr-ca", label: "French" },
    });
  });

  it("unwraps LocaleDetail envelope and flat bodies", () => {
    expect(
      unwrapLocaleDetail({ LocaleDetail: { languageString: "en-us", label: "English" } }),
    ).toEqual({ languageString: "en-us", label: "English" });
    expect(unwrapLocaleDetail({ languageString: "ar", label: "Arabic" })).toEqual({
      languageString: "ar",
      label: "Arabic",
    });
    expect(unwrapLocaleDetail(null)).toEqual({});
  });
});

describe("localesApi write paths", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  function jsonResponse(body: unknown, status = 200): Response {
    return new Response(JSON.stringify(body), {
      status,
      headers: { "Content-Type": "application/json" },
    });
  }

  it("POSTs create body to /services/locales", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ languageString: "fr-ca", label: "French Canada" }),
    );
    const saved = await createLocale({
      languageString: "fr-ca",
      label: "French Canada",
    });
    expect(saved.languageString).toBe("fr-ca");
    expect(fetchMock).toHaveBeenCalledTimes(1);
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("POST");
    expect(String(fetchMock.mock.calls[0][0])).toContain(PATHS.LOCALES);
    expect(JSON.parse(String(init.body))).toEqual({
      LocaleDetail: {
        languageString: "fr-ca",
        label: "French Canada",
      },
    });
  });

  it("PUTs update body to /services/locales/{idOrLang}", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ languageString: "en-us", label: "US English" }),
    );
    const saved = await updateLocale("en-us", { label: "US English" });
    expect(saved.label).toBe("US English");
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("PUT");
    expect(String(fetchMock.mock.calls[0][0])).toContain(`${PATHS.LOCALES}/en-us`);
  });

  it("DELETEs /services/locales/{idOrLang}", async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }));
    await deleteLocale("fr-ca");
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("DELETE");
    expect(String(fetchMock.mock.calls[0][0])).toContain(`${PATHS.LOCALES}/fr-ca`);
  });

  it("lists locales from GET /services/locales", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ LocaleSummary: [{ languageString: "en-us", label: "English" }] }),
    );
    const list = await listLocales();
    expect(list).toEqual([{ languageString: "en-us", label: "English" }]);
  });
});
