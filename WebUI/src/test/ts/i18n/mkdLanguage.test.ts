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

import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";

const initMock = vi.fn();
const configureMock = vi.fn();
const rescanMock = vi.fn();
const destroyMock = vi.fn();

vi.mock("@mkd/language", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@mkd/language")>();
  class NoopSubmissionClient {
    constructor(public debug = false) {}
    async submit(): Promise<void> {
      /* no-op */
    }
  }
  return {
    ...actual,
    NoopSubmissionClient,
    init: (...args: unknown[]) => {
      initMock(...args);
      return {
        configure: configureMock,
        rescan: rescanMock,
        destroy: destroyMock,
      };
    },
  };
});

import {
  __resetMkdLanguageForTests,
  destroyMkdLanguage,
  ensureMkdLanguage,
  isMkdLanguageDebug,
  isMkdLanguageEnabled,
  MKD_LANG_STORAGE_KEY,
} from "@/i18n/mkdLanguage";

describe("mkdLanguage adapter", () => {
  beforeEach(() => {
    __resetMkdLanguageForTests();
    initMock.mockClear();
    configureMock.mockClear();
    rescanMock.mockClear();
    destroyMock.mockClear();
    try {
      localStorage.removeItem(MKD_LANG_STORAGE_KEY);
      localStorage.removeItem("perc-mkd-lang-debug");
    } catch {
      // ignore
    }
  });

  afterEach(() => {
    __resetMkdLanguageForTests();
  });

  describe("isMkdLanguageEnabled", () => {
    it("defaults to off", () => {
      expect(isMkdLanguageEnabled("?", null)).toBe(false);
      expect(isMkdLanguageEnabled("", { getItem: () => null })).toBe(false);
    });

    it("reads query param (wins over storage)", () => {
      expect(isMkdLanguageEnabled("?mkdLang=1", { getItem: () => "0" })).toBe(
        true,
      );
      expect(isMkdLanguageEnabled("?mkdLang=0", { getItem: () => "1" })).toBe(
        false,
      );
    });

    it("reads localStorage when query absent", () => {
      expect(
        isMkdLanguageEnabled("?foo=1", {
          getItem: (k) => (k === MKD_LANG_STORAGE_KEY ? "1" : null),
        }),
      ).toBe(true);
      expect(
        isMkdLanguageEnabled("", {
          getItem: (k) => (k === MKD_LANG_STORAGE_KEY ? "true" : null),
        }),
      ).toBe(true);
    });
  });

  describe("isMkdLanguageDebug", () => {
    it("reads mkdLangDebug query", () => {
      expect(isMkdLanguageDebug("?mkdLangDebug=1", null)).toBe(true);
      expect(isMkdLanguageDebug("?mkdLangDebug=0", null)).toBe(false);
    });
  });

  describe("ensureMkdLanguage", () => {
    it("does not call init when experiment is disabled", () => {
      const handle = ensureMkdLanguage({ locale: "en-us" });
      expect(handle).toBeNull();
      expect(initMock).not.toHaveBeenCalled();
    });

    it("calls init once when enabled via query", () => {
      const original = window.location;
      // jsdom location is partially stubbable via history
      window.history.replaceState({}, "", "/login?mkdLang=1");

      const handle = ensureMkdLanguage({ locale: "fr-fr" });
      expect(handle).not.toBeNull();
      expect(initMock).toHaveBeenCalledTimes(1);
      const opts = initMock.mock.calls[0][0] as {
        messageIdAttr: string;
        locale: string;
        getMessageId?: (el: Element) => string | undefined;
      };
      expect(opts.messageIdAttr).toBe("data-i18n-key");
      expect(opts.locale).toBe("fr-fr");
      expect(typeof opts.getMessageId).toBe("function");
      expect((opts as { zIndex?: number }).zIndex).toBe(20000);
      expect((opts as { respectIgnore?: boolean }).respectIgnore).toBe(true);
      expect(document.getElementById("perc-mkd-lang-theme")).toBeTruthy();

      // second call reconfigures rather than stacking
      ensureMkdLanguage({ locale: "de-de" });
      expect(initMock).toHaveBeenCalledTimes(1);
      expect(configureMock).toHaveBeenCalled();
      expect(rescanMock).toHaveBeenCalled();

      destroyMkdLanguage();
      expect(destroyMock).toHaveBeenCalled();

      window.history.replaceState({}, "", original.pathname + original.search);
    });
  });
});
