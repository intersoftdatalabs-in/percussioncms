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
 * Closes GitHub CodeQL alerts (US2 T024 / 004 / US3) flagged on sinks
 * inside `perc_utils.js`, specifically:
 *
 *   - js/incomplete-sanitization -- htmlEntities() must use the global
 *     flag on the apostrophe replace.
 *
 *   - js/xss-through-dom -- alert_dialog / confirm_dialog / prompt_dialog
 *     route content/question through `percSafeDialogContent()`, an
 *     allowlist HTML sanitizer that preserves legitimate structural tags
 *     used by first-party callers while stripping hostile tags/attrs.
 *
 *   - js/unsafe-jquery-plugin -- `$.fn.perc_toggle` resolves string
 *     targets via `percResolveToggleTarget` / `.find()` (Sizzle only).
 *
 * Test strategy: drive the live source end-to-end via the dialog APIs
 * with stubbed jQuery UI `.dialog()`, then assert DOM sinks.
 */

import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import jquery from "jquery";
import { beforeEach, afterEach, describe, it, expect, vi } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));
const SRC_PATH = resolve(
  __dirname,
  "../../../../webapp/cm/plugins/perc_utils.js",
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
  htmlEntities =
    globalThis.htmlEntities || ($.perc_utils && $.perc_utils.htmlEntities);
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
    // Content must go through percSafeDialogContent (or equivalent), never
    // raw .append(settings.content) which would HTML-parse attacker input.
    expect(code).not.toMatch(/\.append\(\s*settings\.content\s*\)/);
    expect(code).toMatch(/percSafeDialogContent\s*\(\s*settings\.content\s*\)/);
  });

  it("confirm_dialog does not use .append(settings.question) directly", () => {
    expect(code).not.toMatch(/\.append\(\s*settings\.question\s*\)/);
    expect(code).toMatch(
      /percSafeDialogContent\s*\(\s*settings\.question\s*\)/,
    );
  });

  it("perc_toggle forwards 'd' through percResolveToggleTarget (no raw $)", () => {
    // The fix for js/unsafe-jquery-plugin is to avoid handing a raw
    // caller-supplied value to jQuery's $() HTML-parsing constructor. The
    // resolved target is the only thing that ever sees the DOM API. We
    // pin that contract by asserting the function body goes through the
    // helper and that no $(d) remains in the if/else branches.
    expect(src).toMatch(
      /\$\.fn\.perc_toggle\s*=\s*function\s*\(\s*d\s*\)\s*\{[\s\S]{0,200}?percResolveToggleTarget\(d\)/,
    );
    expect(src).not.toMatch(
      /\$\.fn\.perc_toggle\s*=\s*function[\s\S]{0,200}?\$\(d\)/,
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

  it('escapes & < > " characters (preserves prior coverage)', () => {
    const fn = loadHtmlEntities();
    const out = fn(`<a href="x">&'</a>`);
    expect(out).toBe("&lt;a href=&quot;x&quot;&gt;&amp;&#39;&lt;/a&gt;");
  });
});

describe("alert_dialog sink", () => {
  it("strips hostile <script>/<img> tags from content", () => {
    const malicious =
      "before<script>window.__pwned_alert=1</script><img src=x onerror=1>after";
    $.perc_utils.alert_dialog({
      title: "alert-text-title",
      content: malicious,
    });
    expect(document.body.querySelectorAll("script").length).toBe(0);
    expect(document.body.querySelectorAll("img").length).toBe(0);
    expect(window.__pwned_alert).toBeUndefined();
    // perc_dialog delegates to jQuery UI .dialog(), which our stub tags.
    const sink = document.body.querySelector(
      "[data-jquery-ui-dialog-title='alert-text-title']",
    );
    expect(sink, "alert_dialog sink must be attached to body").toBeTruthy();
    expect(sink.textContent).toContain("before");
    expect(sink.textContent).toContain("after");
  });

  it("preserves allowlisted formatting HTML (no flag required)", () => {
    $.perc_utils.alert_dialog({
      title: "alert-html-title",
      content: "<b id='alert-html-marker'>bold</b>",
    });
    const marker = document.getElementById("alert-html-marker");
    expect(marker, "<b id='alert-html-marker'> should be present").toBeTruthy();
    expect(marker.tagName).toBe("B");
  });
});

describe("confirm_dialog sink", () => {
  it("strips hostile <script> tags from question", () => {
    const malicious = "safe<script>window.__pwned_confirm=1</script>text";
    $.perc_utils.confirm_dialog({
      title: "confirm-text-title",
      question: malicious,
    });
    expect(document.body.querySelectorAll("script").length).toBe(0);
    expect(window.__pwned_confirm).toBeUndefined();
    const sink = document.body.querySelector(
      "[data-jquery-ui-dialog-title='confirm-text-title']",
    );
    expect(sink, "confirm_dialog sink must be attached to body").toBeTruthy();
    expect(sink.textContent).toContain("safe");
    expect(sink.textContent).toContain("text");
  });

  it("preserves allowlisted formatting HTML in question", () => {
    $.perc_utils.confirm_dialog({
      title: "confirm-html-title",
      question: "<b id='confirm-html-marker'>bold</b>",
    });
    const marker = document.getElementById("confirm-html-marker");
    expect(
      marker,
      "<b id='confirm-html-marker'> should be present",
    ).toBeTruthy();
    expect(marker.tagName).toBe("B");
  });
});

describe("prompt_dialog sink", () => {
  it("strips hostile <script> tags from question", () => {
    const malicious = "ask<script>window.__pwned_prompt=1</script>?";
    $.perc_utils.prompt_dialog({
      title: "prompt-text-title",
      question: malicious,
    });
    expect(document.body.querySelectorAll("script").length).toBe(0);
    expect(window.__pwned_prompt).toBeUndefined();
    const label = document.body.querySelector(
      "label[for='perc-prompt-dialog-question']",
    );
    expect(label).toBeTruthy();
    expect(label.querySelector("script")).toBeNull();
    expect(label.textContent).toContain("ask");
    expect(label.textContent).toContain("?");
  });

  it("preserves allowlisted formatting HTML in question", () => {
    $.perc_utils.prompt_dialog({
      title: "prompt-html-title",
      question: "<b id='prompt-html-marker'>bold</b>",
    });
    const marker = document.getElementById("prompt-html-marker");
    expect(
      marker,
      "<b id='prompt-html-marker'> should be present in the prompt label",
    ).toBeTruthy();
    expect(marker.tagName).toBe("B");
  });
});

// ---------------------------------------------------------------------------
// First-party HTML callers (e.g. PercNewPageDialog filename error, role
// delete confirmations) pass allowlisted markup without needing a flag.
// contentIsHtml remains accepted for backward compatibility but is ignored
// — the sanitizer always runs.
// ---------------------------------------------------------------------------
describe("allowlisted HTML caller pattern (PercNewPageDialog scenario)", () => {
  it("renders a styled <span> as a real span (sanitizer allowlist)", () => {
    const html =
      '<span style="color:red">The FileName cannot be empty and must not exceed 255 characters.</span>';
    $.perc_utils.alert_dialog({
      title: "html-span",
      content: html,
      // legacy flag still accepted; sanitizer handles safety
      contentIsHtml: true,
    });
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
