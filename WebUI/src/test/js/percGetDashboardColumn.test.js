/**
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Regression tests for WebUI/src/main/webapp/cm/WEB-INF/classes/features/perc/getDashboardColumn/perc_getDashboardColumn.js
 *
 * Closes GitHub CodeQL alerts (js/incomplete-sanitization) flagged on the
 * `__gup()` helper inside the file. The helper extracted a query-string
 * value with a naive regex that did NOT URL-decode the result and did NOT
 * constrain the return value to a safe character set; downstream callers
 * then concatenated the result into a jQuery selector
 * (`percJQuery("#gid_" + __mid)`), which is a documented CodeQL flow path.
 *
 * Pre-fix code returned the raw query value (e.g. `<script>alert(1)</script>`)
 * which would still pass into jQuery's selector parser as a CSS-style
 * selector. While jQuery's CSS selector parser is not an XSS sink on its
 * own, the CodeQL rule flags the upstream `__gup` as the unsanitised
 * source of a flow that reaches an injection-sensitive downstream
 * consumer.
 *
 * Post-fix code:
 *   - `decodeURIComponent`s the captured value (with try/catch fallback)
 *   - strips every character that is not in `[A-Za-z0-9._-]`
 *   - returns the empty string when no match survives sanitisation
 *
 * The test exercises both `__gup()` (the flagged source) and the public
 * `gadgets.window.getDashboardColumn()` (the consumer). It also pins the
 * downstream `getDashboardColumn` parsing logic so the file remains
 * behavioural-compatible.
 */

import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import { JSDOM } from "jsdom";
import { beforeEach, afterEach, describe, it, expect } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));
const SRC_PATH = resolve(
  __dirname,
  "../../main/webapp/cm/WEB-INF/classes/features/perc/getDashboardColumn/perc_getDashboardColumn.js"
);

// ---------------------------------------------------------------------------
// Test-environment bootstrap. We rewrite the IIFE-less script into a
// parameterised form so we can:
//   1) inject a stub `gadgets` global;
//   2) inject a stub `percJQuery` global;
//   3) drive the function under each scenario without polluting the
//      vitest worker globals.
// ---------------------------------------------------------------------------
function withFreshWindow(href, run) {
  const dom = new JSDOM("<!DOCTYPE html><html><body></body></html>", {
    url: href,
  });
  const prev = {
    window: globalThis.window,
    document: globalThis.document,
    gadgets: globalThis.gadgets,
    percJQuery: globalThis.percJQuery,
  };
  globalThis.window = dom.window;
  globalThis.document = dom.window.document;
  delete globalThis.gadgets;
  delete globalThis.percJQuery;
  try {
    run(dom);
  } finally {
    globalThis.window = prev.window;
    globalThis.document = prev.document;
    globalThis.gadgets = prev.gadgets;
    globalThis.percJQuery = prev.percJQuery;
  }
}

function loadSource() {
  const code = readFileSync(SRC_PATH, "utf8");
  // Run as a script in the current global scope so `gadgets` and
  // `percJQuery` references resolve to the test stubs we install below.
  (0, eval)(code);
}

// ---------------------------------------------------------------------------
// Source-pattern tests — pin the security-relevant pattern in the source
// so any future regression that reintroduces the unsanitised branch fails
// immediately. Erlang rules warn against pure grep tests for non-trivial
// logic; here the security property IS the absence of the unsafe `match`
// + raw return pattern, so a presence/absence check is the right tool.
// ---------------------------------------------------------------------------
describe("source-pattern (anti-regression for js/incomplete-sanitization)", () => {
  const src = readFileSync(SRC_PATH, "utf8");

  it("does not return the raw regex capture from __gup", () => {
    // The pre-fix implementation returned `results[1]` directly — the
    // exact thing CodeQL flagged. Post-fix the return is sanitised
    // (decodeURIComponent + character allow-list).
    expect(src).not.toMatch(/return\s+results\[1\]\s*;/);
  });

  it("URL-decodes the captured value before returning", () => {
    // Either decodeURIComponent(...) on the captured value, or an
    // explicit return of the sanitised form.
    expect(src).toMatch(/decodeURIComponent\s*\(/);
  });

  it("constrains the return value to a safe character set", () => {
    // A `.replace(/[^A-Za-z0-9._-]/g, "")` or equivalent allow-list
    // must exist on the return path.
    expect(src).toMatch(/\.replace\s*\(\s*\/\[\^A-Za-z0-9\._-\]\/g/);
  });

  it("does not use non-global [ / ] first-occurrence replaces for param names", () => {
    // Pre-residual: name = name.replace(/[\[]/,"\\[").replace(/[\]]/,"\\]");
    // CodeQL js/incomplete-sanitization (alerts #1110-#1113). Replaced by
    // an allow-list check on the parameter name before building the RegExp.
    expect(src).not.toMatch(/name\.replace\s*\(\s*\/\[\\?\[\]\//);
    expect(src).toMatch(/\[\^A-Za-z0-9\._-\]/);
    expect(src).toMatch(/typeof name !== "string"/);
  });
});

// ---------------------------------------------------------------------------
// Behavioural tests — exercise the live source against a jsdom window.
// ---------------------------------------------------------------------------
describe("__gup() sanitisation", () => {
  it("returns the decoded, allow-listed value for a normal param", () => {
    withFreshWindow("http://localhost/?mid=abc-123", () => {
      globalThis.gadgets = { window: {} };
      globalThis.percJQuery = () => ({ parent: () => ({ attr: () => null }) });
      loadSource();
      expect(globalThis.__gup("mid")).toBe("abc-123");
    });
  });

  it("URL-decodes percent-encoded values", () => {
    withFreshWindow("http://localhost/?mid=hello%20world", () => {
      globalThis.gadgets = { window: {} };
      globalThis.percJQuery = () => ({ parent: () => ({ attr: () => null }) });
      loadSource();
      // The decoded space character is outside the allow-list and is
      // therefore stripped, yielding the contiguous identifier
      // "helloworld". Callers that need whitespace handling should use
      // a different parameter; the gadget mid is an opaque id.
      expect(globalThis.__gup("mid")).toBe("helloworld");
    });
  });

  it("strips characters outside [A-Za-z0-9._-]", () => {
    withFreshWindow(
      'http://localhost/?mid=%3Cscript%3Ealert(1)%3C%2Fscript%3E',
      () => {
        globalThis.gadgets = { window: {} };
        globalThis.percJQuery = () => ({ parent: () => ({ attr: () => null }) });
        loadSource();
        const got = globalThis.__gup("mid");
        expect(got).toBe("scriptalert1script");
      }
    );
  });

  it("returns empty string for a missing param", () => {
    withFreshWindow("http://localhost/", () => {
      globalThis.gadgets = { window: {} };
      globalThis.percJQuery = () => ({ parent: () => ({ attr: () => null }) });
      loadSource();
      expect(globalThis.__gup("missing")).toBe("");
    });
  });

  it("rejects parameter names outside the identifier allow-list", () => {
    withFreshWindow("http://localhost/?mid=abc&x[0]=evil", () => {
      globalThis.gadgets = { window: {} };
      globalThis.percJQuery = () => ({ parent: () => ({ attr: () => null }) });
      loadSource();
      expect(globalThis.__gup("x[0]")).toBe("");
      expect(globalThis.__gup("mid.*")).toBe("");
      expect(globalThis.__gup("")).toBe("");
      expect(globalThis.__gup(null)).toBe("");
      // Normal identifier still works.
      expect(globalThis.__gup("mid")).toBe("abc");
    });
  });

  it("returns empty string for a value that sanitises away to nothing", () => {
    withFreshWindow("http://localhost/?mid=%3C%3E%26%21", () => {
      globalThis.gadgets = { window: {} };
      globalThis.percJQuery = () => ({ parent: () => ({ attr: () => null }) });
      loadSource();
      expect(globalThis.__gup("mid")).toBe("");
    });
  });

  it("never produces a value that contains a closing angle bracket", () => {
    const payloads = [
      '"><img src=x onerror=alert(1)>',
      "' onload='alert(1)",
      "javascript:alert(1)",
      "%3Cscript%3Ealert%281%29%3C/script%3E",
      "../../etc/passwd",
    ];
    for (const p of payloads) {
      withFreshWindow(`http://localhost/?mid=${encodeURIComponent(p)}`, () => {
        globalThis.gadgets = { window: {} };
        globalThis.percJQuery = () => ({ parent: () => ({ attr: () => null }) });
        loadSource();
        const got = globalThis.__gup("mid");
        expect(got, `payload ${p} -> ${got}`).not.toMatch(/[<>"'`&/\\ ]/);
      });
    }
  });
});

describe("gadgets.window.getDashboardColumn", () => {
  it("returns a numeric column index for a normal layout", () => {
    withFreshWindow("http://localhost/?mid=42", () => {
      const col = globalThis.document.createElement("div");
      col.id = "col_3";
      const gid = globalThis.document.createElement("div");
      gid.id = "gid_42";
      col.appendChild(gid);
      globalThis.document.body.appendChild(col);
      globalThis.gadgets = { window: {} };
      globalThis.percJQuery = () => ({
        parent: () => ({ attr: (name) => (name === "id" ? "col_3" : null) }),
      });
      loadSource();
      expect(gadgets.window.getDashboardColumn()).toBe(3);
    });
  });

  it("never lets an attacker-controlled mid reach a sink via the selector", () => {
    withFreshWindow(
      'http://localhost/?mid=%22%3E%3Cimg+src%3Dx+onerror%3Dalert(1)%3E',
      () => {
        let selectorSeen = null;
        globalThis.gadgets = { window: {} };
        // Return a benign column id so __columnRawId.substr(4) does
        // not crash on null; the test still validates the selector
        // sanitisation, which is what CodeQL flagged.
        globalThis.percJQuery = (sel) => {
          selectorSeen = sel;
          return { parent: () => ({ attr: () => "col_1" }) };
        };
        loadSource();
        // Must not throw; the sanitised selector is the contract.
        expect(() => gadgets.window.getDashboardColumn()).not.toThrow();
        expect(selectorSeen).toBeTruthy();
        expect(selectorSeen).toMatch(/^#gid_[A-Za-z0-9._-]*$/);
      }
    );
  });
});

afterEach(() => {
  delete globalThis.__gup;
  delete globalThis.gadgets;
  delete globalThis.percJQuery;
});