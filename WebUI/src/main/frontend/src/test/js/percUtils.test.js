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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Regression tests for WebUI/src/main/webapp/cm/plugins/perc_utils.js
 *
 * Closes GitHub CodeQL alerts (US2 T024 / 004) flagged on the
 * stale `shared-common.js` bundle for sinks inside `perc_utils.js`,
 * specifically:
 *
 *   - js/incomplete-sanitization (alert #1481) -- htmlEntities() in the
 *     pre-fix source used `.replace(/'/, "&#39;")` without the global
 *     flag, so only the first apostrophe was escaped. The fix flips the
 *     regex to `.replace(/'/g, "&#39;")`. This test exercises the live
 *     function with multiple apostrophes and asserts every one is escaped.
 *
 *   - js/xss-through-dom (alert #1608) -- alert_dialog() built the body
 *     via `$("<div/>").append(settings.content)`, which parses the
 *     caller-supplied string as HTML. The fix switches to `.text()` by
 *     default, with an explicit `settings.contentIsHtml` opt-in for HTML
 *     content. This test exercises both modes through the live
 *     `$.perc_utils.alert_dialog` API (the dialog content element is the
 *     actual sink).
 *
 *   - js/xss-through-dom (alert #1603) -- confirm_dialog() built the body
 *     via `$("<div/>").append(settings.question)`. Same fix: `.text()` by
 *     default with `settings.questionIsHtml` opt-in. This test exercises
 *     both modes through the live `$.perc_utils.confirm_dialog` API.
 *
 *   - js/unsafe-jquery-plugin (alerts #1669-#1674) -- `$.fn.perc_toggle`
 *     forwards its argument `d` only to .hasClass(), .addClass(), and
 *     .removeClass(), none of which parse `d` as HTML, so the alert is a
 *     false positive. The source carries an inline `codeql[...]`
 *     suppression; this test pins the public API contract that callers
 *     rely on and would catch any future regression that re-introduces
 *     a true XSS sink in perc_toggle.
 *
 * Pre-fix code would let a title/question/content string containing
 * `<script>window.__pwned=true</script>` produce live DOM elements; the
 * post-fix code keeps the string as inert text by default.
 *
 * Test strategy (Constitution III fail-then-pass):
 *   - Drive the live source end-to-end via `$.perc_utils.alert_dialog`
 *     and `$.perc_utils.confirm_dialog`.
 *   - Provide stub dependencies ($.perc_dialog, I18N.message) so the
 *     dialogs render without throwing.
 *   - Inspect the actual DOM sink element to confirm the content/question
 *     was rendered as text or HTML as appropriate.
 *   - Assert that htmlEntities() escapes every apostrophe in the input.
 */

import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import jquery from "jquery";
import { beforeEach, afterEach, describe, it, expect, vi } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));
const SRC_PATH = resolve(
  __dirname,
  "../../../../webapp/cm/plugins/perc_utils.js"
);

let $;
let htmlEntities;

function loadSource() {
  let jq = jquery(globalThis.window);
  if (typeof jq !== "function") {
    jq = typeof jquery === "function" ? jquery : jquery.fn || jquery;
    if (!jq.fn && jq.prototype) jq.fn = jq.prototype;
  }
  if (!jq.fn) throw new Error("jquery has no .fn");
  $ = jq;
  globalThis.jQuery = $;
  globalThis.$ = $;

  // Stub jQuery UI .dialog() so the dialog helpers can run without
  // throwing. The real source's $.fn.perc_dialog is loaded later by eval
  // and it delegates to .dialog(); we don't need to stub perc_dialog
  // itself -- we just tag the element when the inner .dialog() runs so
  // tests can locate it. Auto-attach to document.body so DOM assertions
  // can find the sink without the test having to thread the jQuery
  // return value through.
  $.fn.dialog = function (opts) {
    this.attr("data-jquery-ui-dialog-title", opts && opts.title);
    if (this.parent().length === 0) this.appendTo(document.body);
    return this;
  };

  globalThis.I18N = { message: (key) => `[${key}]` };
  // Run the source as a script in the global scope so its IIFE sees
  // globalThis.jQuery / globalThis.I18N. Note: the source re-defines
  // $.fn.perc_dialog; our stub for .dialog() above will be invoked when
  // perc_dialog delegates to it.
  (0, eval)(readFileSync(SRC_PATH, "utf8"));
  htmlEntities = globalThis.htmlEntities || $.perc_utils && $.perc_utils.htmlEntities;
}

beforeEach(() => {
  document.body.innerHTML = "";
  loadSource();
});

afterEach(() => {
  vi.restoreAllMocks();
  document.body.innerHTML = "";
  delete globalThis.I18N;
});

// ---------------------------------------------------------------------------
// Source-pattern tests — pin the security-relevant pattern in the source so
// any future regression that reintroduces the unsafe call fails immediately.
// Comments are stripped before matching so we don't false-positive on the
// "do not reintroduce ..." warning comments.
// ---------------------------------------------------------------------------
describe("source-pattern (anti-regression for js/xss-through-dom, js/incomplete-sanitization)", () => {
  const src = readFileSync(SRC_PATH, "utf8");
  // Strip line comments and block comments so the pattern checks only
  // match executable code, not the explanatory comments around the fix.
  const code = src
    .replace(/\/\*[\s\S]*?\*\//g, "")
    .replace(/(^|[^:])\/\/.*$/gm, "$1");

  it("htmlEntities escapes every apostrophe (g flag on the regex)", () => {
    expect(code).not.toMatch(/\.replace\(\s*\/\s*'\s*\/\s*,\s*"/);
    expect(code).toMatch(/\.replace\(\s*\/\s*'\s*\/\s*g\s*,\s*"/);
  });

  it("alert_dialog does not use .append(settings.content) directly", () => {
    // The fixed alert_dialog routes content through .text() or .html()
    // depending on the contentIsHtml opt-in; it must not concatenate or
    // .append() the raw settings.content string into the dialog body.
    expect(code).not.toMatch(/\.append\(\s*settings\.content\s*\)/);
  });

  it("confirm_dialog does not use .append(settings.question) directly", () => {
    expect(code).not.toMatch(/\.append\(\s*settings\.question\s*\)/);
  });

  it("perc_toggle forwards 'd' through percResolveToggleTarget (no raw $)", () => {
    // The fix for js/unsafe-jquery-plugin is to avoid handing a raw
    // caller-supplied value to jQuery's $() HTML-parsing constructor. The
    // resolved target is the only thing that ever sees the DOM API. We
    // pin that contract by asserting the function body goes through the
    // helper and that no $(d) remains in the if/else branches.
    expect(src).toMatch(
      /\$\.fn\.perc_toggle\s*=\s*function\s*\(\s*d\s*\)\s*\{[\s\S]{0,200}?percResolveToggleTarget\(d\)/
    );
    expect(src).not.toMatch(
      /\$\.fn\.perc_toggle\s*=\s*function[\s\S]{0,200}?\$\(d\)/
    );
  });
});

// ---------------------------------------------------------------------------
// Behavioural tests — exercise the live dialog APIs end-to-end.
// ---------------------------------------------------------------------------
describe("htmlEntities sink", () => {
  function loadHtmlEntities() {
    // htmlEntities is a module-level function in perc_utils.js. We extract
    // its definition from the source so the test is bundler-agnostic and
    // does not depend on how the IIFE attaches it. We rewrite the body
    // into a self-contained function that takes a `str` parameter.
    const src = readFileSync(SRC_PATH, "utf8");
    const m = src.match(/function htmlEntities\(str\)\s*\{([\s\S]*?)\n\}/);
    expect(m, "htmlEntities definition must be present in source").toBeTruthy();
    // eslint-disable-next-line no-new-func
    return new Function("str", m[1] + "\nreturn htmlEntities(str);");
  }

  it("escapes every apostrophe in the input (post-fix g flag)", () => {
    const fn = loadHtmlEntities();
    // Input contains 6 apostrophes (two flanking each of a/b/c).
    // Pre-fix .replace(/'/, ...) escapes only the FIRST one. Post-fix
    // .replace(/'/g, ...) escapes all 6.
    const out = fn("'a' 'b' 'c'");
    expect(out).not.toMatch(/'/);
    expect((out.match(/&#39;/g) || []).length).toBe(6);
  });

  it("escapes & < > \" characters (preserves prior coverage)", () => {
    const fn = loadHtmlEntities();
    const out = fn(`<a href="x">&'</a>`);
    expect(out).toBe("&lt;a href=&quot;x&quot;&gt;&amp;&#39;&lt;/a&gt;");
  });
});

describe("alert_dialog sink", () => {
  it("renders caller-supplied content as inert text by default", () => {
    const malicious = "<script>window.__pwned_alert=1</script>";
    $.perc_utils.alert_dialog({ title: "alert-text-title", content: malicious });
    const scripts = document.body.querySelectorAll("script");
    expect(scripts.length, "no <script> from content").toBe(0);
    expect(window.__pwned_alert).toBeUndefined();
    // The source's $.fn.perc_dialog delegates to jQuery UI .dialog(),
    // which our stub tags with data-jquery-ui-dialog-title.
    const sink = document.body.querySelector(
      "[data-jquery-ui-dialog-title='alert-text-title']"
    );
    expect(sink, "alert_dialog sink must be attached to body").toBeTruthy();
    expect(sink.querySelector("script")).toBeNull();
    expect(sink.querySelector("img")).toBeNull();
    expect(sink.textContent).toBe(malicious);
  });

  it("renders content as HTML only when contentIsHtml is true", () => {
    $.perc_utils.alert_dialog({
      title: "alert-html-title",
      content: "<b id='alert-html-marker'>bold</b>",
      contentIsHtml: true,
    });
    const marker = document.getElementById("alert-html-marker");
    expect(marker, "<b id='alert-html-marker'> should be present").toBeTruthy();
    expect(marker.tagName).toBe("B");
  });
});

describe("confirm_dialog sink", () => {
  it("renders caller-supplied question as inert text by default", () => {
    const malicious = "<script>window.__pwned_confirm=1</script>";
    $.perc_utils.confirm_dialog({ title: "confirm-text-title", question: malicious });
    const scripts = document.body.querySelectorAll("script");
    expect(scripts.length, "no <script> from question").toBe(0);
    expect(window.__pwned_confirm).toBeUndefined();
    const sink = document.body.querySelector(
      "[data-jquery-ui-dialog-title='confirm-text-title']"
    );
    expect(sink, "confirm_dialog sink must be attached to body").toBeTruthy();
    expect(sink.querySelector("script")).toBeNull();
    expect(sink.textContent).toBe(malicious);
  });

  it("renders question as HTML only when questionIsHtml is true", () => {
    $.perc_utils.confirm_dialog({
      title: "confirm-html-title",
      question: "<b id='confirm-html-marker'>bold</b>",
      questionIsHtml: true,
    });
    const marker = document.getElementById("confirm-html-marker");
    expect(marker, "<b id='confirm-html-marker'> should be present").toBeTruthy();
    expect(marker.tagName).toBe("B");
  });
});

describe("prompt_dialog sink", () => {
  it("renders caller-supplied question as inert text by default", () => {
    const malicious = "<script>window.__pwned_prompt=1</script>";
    $.perc_utils.prompt_dialog({ title: "prompt-text-title", question: malicious });
    const scripts = document.body.querySelectorAll("script");
    expect(scripts.length, "no <script> from prompt question").toBe(0);
    expect(window.__pwned_prompt).toBeUndefined();
    const label = document.body.querySelector("label[for='perc-prompt-dialog-question']");
    expect(label).toBeTruthy();
    expect(label.querySelector("script")).toBeNull();
    expect(label.textContent).toBe(malicious);
  });

  it("renders question as HTML only when questionIsHtml is true", () => {
    $.perc_utils.prompt_dialog({
      title: "prompt-html-title",
      question: "<b id='prompt-html-marker'>bold</b>",
      questionIsHtml: true,
    });
    const marker = document.getElementById("prompt-html-marker");
    expect(
      marker,
      "<b id='prompt-html-marker'> should be present in the prompt label"
    ).toBeTruthy();
    expect(marker.tagName).toBe("B");
  });
});

// ---------------------------------------------------------------------------
// HTML-opt-in caller pattern — pin the contract that legitimate HTML-passing
// callers (e.g. PercNewPageDialog's "filename illegal characters" error) rely
// on. Without the contentIsHtml:true opt-in, the styled <span> would be
// escaped and the user would see the literal <span>...</span> text.
// Regression found by Erlang pre-commit review of the 004/US2-T024-shared-
// common-rebuild change set.
// ---------------------------------------------------------------------------
describe("HTML opt-in caller pattern (PercNewPageDialog scenario)", () => {
  it("renders a styled <span> as a real span when contentIsHtml is true", () => {
    const html =
      '<span style="color:red">The FileName cannot be empty and must not exceed 255 characters.</span>';
    $.perc_utils.alert_dialog({ title: "html-span", content: html, contentIsHtml: true });
    const span = document.querySelector("span[style*='color:red']");
    expect(span, "the styled span should be parsed as an element").toBeTruthy();
    expect(span.style.color).toBe("red");
    expect(span.textContent).toContain("The FileName cannot be empty");
  });
});

// ---------------------------------------------------------------------------
// Public API surface — pin the exported method names so callers keep working.
// ---------------------------------------------------------------------------
describe("public API", () => {
  it("$.perc_utils exposes the documented dialog helpers", () => {
    expect(typeof $.perc_utils.alert_dialog).toBe("function");
    expect(typeof $.perc_utils.confirm_dialog).toBe("function");
    expect(typeof $.perc_utils.prompt_dialog).toBe("function");
  });
});
