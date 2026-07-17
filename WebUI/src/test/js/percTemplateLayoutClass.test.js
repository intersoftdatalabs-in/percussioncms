/**
 * Copyright 1999-2026 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Regression tests for
 * WebUI/src/main/webapp/cm/classes/perc_template_layout_class.js
 *
 * Closes the GitHub CodeQL alert (js/xss-through-dom) flagged on
 * `Perc_Template_class.parseRegions()`:
 *
 *   var $templateCode = $($children.find("code templateCode:first").text());
 *   newRegion.setVertical(!$templateCode.hasClass("perc-horizontal"));
 *
 * This is the same vulnerable pattern (and the same fix) as
 * `perc_template_layout_helper.js` — see
 * `percTemplateLayoutHelper.test.js` for the full `parseRegions()`
 * end-to-end regression test with a jQuery-call spy.
 *
 * `Perc_Template_class` itself is dead code: it is defined but never
 * instantiated anywhere else in the codebase (grep confirms no other
 * caller), and `parseRegions()` already throws unconditionally on
 * `this.Type.TEMPLATE` (`this.Type` is never initialized by the
 * constructor) — a pre-existing, unrelated bug that predates this CodeQL
 * fix and is out of scope here. Because the surrounding method cannot be
 * exercised end-to-end, this test instead extracts the new
 * `percTemplateCodeHasClass()` helper directly from the checked-in file
 * (via `readFileSync`, same "test the real committed source" pattern used
 * elsewhere in this directory) and exercises it directly and
 * behaviorally with a range of markup inputs, including hostile payloads.
 */

import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import { describe, it, expect } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));
const SRC_PATH = resolve(
  __dirname,
  "../../main/webapp/cm/classes/perc_template_layout_class.js"
);

function loadPercTemplateCodeHasClass() {
  const src = readFileSync(SRC_PATH, "utf8");
  const match = /function percTemplateCodeHasClass\([\s\S]*?\n {2}}/.exec(
    src
  );
  if (!match) {
    throw new Error(
      "percTemplateCodeHasClass() not found in perc_template_layout_class.js " +
        "— has the fix for js/xss-through-dom been reverted?"
    );
  }
  // eslint-disable-next-line no-eval
  return eval("(" + match[0].replace(/^function /, "function ") + ")");
}

describe("perc_template_layout_class percTemplateCodeHasClass regression", () => {
  it("does not reference jQuery/DOM APIs (no HTML-parsing sink)", () => {
    const src = readFileSync(SRC_PATH, "utf8");
    const match = /function percTemplateCodeHasClass\([\s\S]*?\n {2}}/.exec(
      src
    );
    expect(match).not.toBeNull();
    const fnBody = match[0];
    expect(fnBody).not.toMatch(/\$\(/);
    expect(fnBody).not.toMatch(/\.html\(/);
    expect(fnBody).not.toMatch(/innerHTML/);
  });

  it("detects the perc-horizontal class in benign markup", () => {
    const hasClass = loadPercTemplateCodeHasClass();
    const markup = '<div class="perc-horizontal"></div>';
    expect(hasClass(markup, "perc-horizontal")).toBe(true);
  });

  it("returns false when the class is absent", () => {
    const hasClass = loadPercTemplateCodeHasClass();
    const markup = '<div class="some-other-class"></div>';
    expect(hasClass(markup, "perc-horizontal")).toBe(false);
  });

  it("detects the class inside a hostile payload without executing it", () => {
    const hasClass = loadPercTemplateCodeHasClass();
    globalThis.__perc_template_class_pwned__ = undefined;
    const payload =
      '<div class="perc-horizontal"><img src=x onerror="globalThis.__perc_template_class_pwned__=true"></div>';

    expect(hasClass(payload, "perc-horizontal")).toBe(true);
    // A pure regex match never parses/executes the markup — there is no
    // DOM, no <img>, and no event dispatch involved.
    expect(globalThis.__perc_template_class_pwned__).toBeUndefined();

    delete globalThis.__perc_template_class_pwned__;
  });

  it("handles empty/undefined/malformed markup safely", () => {
    const hasClass = loadPercTemplateCodeHasClass();
    expect(hasClass("", "perc-horizontal")).toBe(false);
    expect(hasClass(undefined, "perc-horizontal")).toBe(false);
    expect(hasClass("not html at all", "perc-horizontal")).toBe(false);
  });

  it("only inspects the root element, matching $(markup).hasClass() semantics", () => {
    // PR #1320 review: the original regex matched the first class="..."
    // occurrence anywhere in the markup, so a *nested* element carrying
    // the class (with no class attribute on the root) incorrectly
    // returned true — diverging from jQuery's hasClass(), which only
    // ever inspects the root element.
    const hasClass = loadPercTemplateCodeHasClass();
    const markup = '<div><span class="perc-horizontal"></span></div>';
    expect(hasClass(markup, "perc-horizontal")).toBe(false);
  });

  it("matches a single-quoted class attribute on the root element", () => {
    // PR #1320 review: the original regex only matched double-quoted
    // class="..." attributes, silently missing single-quoted
    // class='...' attributes that jQuery's hasClass() matches fine.
    const hasClass = loadPercTemplateCodeHasClass();
    const markup = "<div class='perc-horizontal'></div>";
    expect(hasClass(markup, "perc-horizontal")).toBe(true);
  });

  it("still finds the root tag when the markup has leading whitespace", () => {
    // The root-tag extraction regex (/^\s*<[^>]*>/) explicitly tolerates
    // leading whitespace/newlines before the first tag, matching how
    // Velocity template code stored in the CMS is often indented; verify
    // that tolerance doesn't silently break class detection.
    const hasClass = loadPercTemplateCodeHasClass();
    const markup = '\n   <div class="perc-horizontal"></div>';
    expect(hasClass(markup, "perc-horizontal")).toBe(true);
  });
});
