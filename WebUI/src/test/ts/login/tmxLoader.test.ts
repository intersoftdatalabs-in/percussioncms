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

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  __resetTmxLoaderCache,
  ensureTmxLoaded,
} from "../../../main/ts/login/tmxLoader";

/**
 * Spy on document.head.appendChild so we capture every injected
 * classic script element without actually performing a network
 * fetch. Dispatch the "load" event from the test to resolve the
 * loader promise.
 */
function captureScripts(): {
  scripts: HTMLScriptElement[];
  fireLoad: (s: HTMLScriptElement) => void;
  fireError: (s: HTMLScriptElement) => void;
} {
  const scripts: HTMLScriptElement[] = [];
  const original = document.head.appendChild.bind(document.head);
  const spy = vi
    .spyOn(document.head, "appendChild")
    .mockImplementation((node: Node) => {
      if (node instanceof HTMLScriptElement) {
        scripts.push(node);
      }
      return original(node);
    });
  return {
    scripts,
    fireLoad: (s) => {
      s.dispatchEvent(new Event("load"));
    },
    fireError: (s) => {
      s.dispatchEvent(new Event("error"));
    },
    dispose: () => spy.mockRestore(),
  };
}

describe("login/tmxLoader", () => {
  let capture: ReturnType<typeof captureScripts>;

  beforeEach(() => {
    __resetTmxLoaderCache();
    capture = captureScripts();
  });

  afterEach(() => {
    capture.dispose();
    __resetTmxLoaderCache();
  });

  it("rejects when locale is empty", async () => {
    await expect(ensureTmxLoaded("")).rejects.toThrow(/empty/i);
  });

  it("builds the URL with mode=js, prefix=perc.ui., and normalized sys_lang", async () => {
    const promise = ensureTmxLoaded("EN_US");
    expect(capture.scripts.length).toBe(1);
    const s = capture.scripts[0];
    expect(s.tagName).toBe("SCRIPT");
    expect(s.dataset.percTmxLocale).toBe("en-us");
    // jsdom exposes the src as an absolute URL; assert the meaningful
    // query-string parts directly.
    expect(s.src).toContain("/tmx/tmx.jsp");
    expect(s.src).toContain("mode=js");
    expect(s.src).toContain("prefix=perc.ui.");
    expect(s.src).toContain("sys_lang=en-us");
    capture.fireLoad(s);
    await promise;
  });

  it("honors an explicit baseHref override", async () => {
    const promise = ensureTmxLoaded("fr-fr", "/i18n/tmx.jsp");
    const s = capture.scripts[0];
    expect(s.src).toContain("/i18n/tmx.jsp");
    expect(s.src).toContain("sys_lang=fr-fr");
    capture.fireLoad(s);
    await promise;
  });

  it("dedupes concurrent calls for the same locale", async () => {
    const p1 = ensureTmxLoaded("fr-fr");
    const p2 = ensureTmxLoaded("fr-fr");
    expect(capture.scripts.length).toBe(1);
    capture.fireLoad(capture.scripts[0]);
    await Promise.all([p1, p2]);
  });

  it("returns the cached promise for an already-loaded locale without injecting", async () => {
    const first = ensureTmxLoaded("de-de");
    capture.fireLoad(capture.scripts[0]);
    await first;
    const second = ensureTmxLoaded("de-de");
    await expect(second).resolves.toBeUndefined();
    expect(capture.scripts.length).toBe(1);
  });

  it("rejects and removes the tag on error; subsequent call retries", async () => {
    const failing = ensureTmxLoaded("ja-jp");
    const failingScript = capture.scripts[0];
    capture.fireError(failingScript);
    await expect(failing).rejects.toThrow(/failed to load/);
    // Tag should be removed from the DOM.
    expect(document.head.contains(failingScript)).toBe(false);

    const retry = ensureTmxLoaded("ja-jp");
    expect(capture.scripts.length).toBe(2);
    capture.fireLoad(capture.scripts[1]);
    await retry;
    expect(document.head.contains(capture.scripts[1])).toBe(true);
  });
});