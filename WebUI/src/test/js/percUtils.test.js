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
 * Regression tests for WebUI/src/main/webapp/cm/plugins/perc_utils.js
 *
 * Closes CodeQL alerts across 3 rules:
 *
 * 1. js/unsafe-jquery-plugin on `$.fn.perc_toggle`: string targets are
 *    resolved via `percResolveToggleTarget` / `.find()` (Sizzle only), never
 *    the HTML-sniffing `$()` constructor.
 *
 * 2. js/xss-through-dom on dialog bodies: `alert_dialog` / `confirm_dialog`
 *    / `prompt_dialog` render `content`/`question` with `.text()` by default.
 *    Callers that intentionally need markup pass `contentIsHtml` /
 *    `questionIsHtml` and are responsible for sanitizing.
 *
 * 3. js/incomplete-sanitization on `htmlEntities()`: the apostrophe replace
 *    must use the global flag.
 *
 * Dialog strategy: stub jQuery UI `.dialog()` so the production
 * `$.fn.perc_dialog` (which delegates to it) attaches the dialog root to
 * `document.body` and tags it for DOM assertions in jsdom.
 */

import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import jquery from "jquery";
import { beforeEach, afterEach, describe, it, expect, vi } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));
const SRC_PATH = resolve(
  __dirname,
  "../../main/webapp/cm/plugins/perc_utils.js",
);

const XSS_MARKER = 'onerror="globalThis.__perc_utils_pwned__=true"';

let realJQ;
let rawCalls;

function loadSource() {
  let jq = jquery(globalThis.window);
  if (typeof jq !== "function") {
    jq = typeof jquery === "function" ? jquery : jquery.fn || jquery;
  }
  realJQ = jq;
  globalThis.I18N = { message: (key) => `[${key}]` };

  rawCalls = [];
  function spy(arg) {
    rawCalls.push(arg);
    return realJQ.apply(this, arguments);
  }
  Object.assign(spy, realJQ);
  spy.fn = realJQ.fn;

  // jQuery UI .dialog() stub: production perc_dialog delegates here.
  // Tag the dialog root and append to body so sink assertions work in jsdom.
  spy.fn.dialog = function (opts) {
    this.attr("data-jquery-ui-dialog-title", opts && opts.title);
    this.data("dialog_opts", opts);
    if (this.parent().length === 0) {
      document.body.appendChild(this[0]);
    }
    return this;
  };

  globalThis.jQuery = spy;
  globalThis.$ = spy;

  const src = readFileSync(SRC_PATH, "utf8");
  // Indirect eval so top-level `htmlEntities` becomes a real global binding.
  // eslint-disable-next-line no-eval
  const indirectEval = eval;
  indirectEval(src);
}

beforeEach(() => {
  document.body.innerHTML = "";
  loadSource();
});

afterEach(() => {
  document.body.innerHTML = "";
  delete globalThis.__perc_utils_pwned__;
  vi.restoreAllMocks();
});

describe("perc_utils.js $.fn.perc_toggle XSS regression (js/unsafe-jquery-plugin)", () => {
  it("never hands a hostile string argument to the jQuery HTML-parsing constructor", () => {
    document.body.innerHTML =
      '<div id="perc-toggle-target" class="perc-hidden"></div>';
    const payload = `<img src=x ${XSS_MARKER}>`;

    try {
      globalThis.jQuery(document.body).perc_toggle(payload);
    } catch {
      // Sizzle rejecting an invalid selector is expected and fine.
    }

    const sawRawMarkup = rawCalls.some(
      (call) => typeof call === "string" && call.includes(XSS_MARKER),
    );
    expect(sawRawMarkup).toBe(false);
    expect(document.querySelectorAll("img").length).toBe(0);
  });

  it("still toggles perc-hidden/perc-visible for a CSS selector string", () => {
    document.body.innerHTML =
      '<div id="perc-toggle-target" class="perc-hidden"></div>';

    globalThis.jQuery(document.body).perc_toggle("#perc-toggle-target");

    const target = document.getElementById("perc-toggle-target");
    expect(target.classList.contains("perc-visible")).toBe(true);
    expect(target.classList.contains("perc-hidden")).toBe(false);
  });

  it("still toggles perc-hidden/perc-visible for a jQuery object argument", () => {
    document.body.innerHTML =
      '<div id="perc-toggle-target" class="perc-visible"></div>';
    const $target = globalThis.jQuery("#perc-toggle-target");

    globalThis.jQuery(document.body).perc_toggle($target);

    expect($target.hasClass("perc-hidden")).toBe(true);
    expect($target.hasClass("perc-visible")).toBe(false);
  });

  it("still toggles perc-hidden/perc-visible for a raw DOM element argument", () => {
    document.body.innerHTML =
      '<div id="perc-toggle-target" class="perc-hidden"></div>';
    const el = document.getElementById("perc-toggle-target");

    globalThis.jQuery(document.body).perc_toggle(el);

    expect(el.classList.contains("perc-visible")).toBe(true);
  });
});

describe("source-pattern (anti-regression for dialog XSS sinks)", () => {
  const src = readFileSync(SRC_PATH, "utf8");
  const code = src
    .replace(/\/\*[\s\S]*?\*\//g, "")
    .replace(/(^|[^:])\/\/.*$/gm, "$1");

  it("htmlEntities escapes every apostrophe (g flag on the regex)", () => {
    expect(code).not.toMatch(/\.replace\(\s*\/\s*'\s*\/\s*,\s*"/);
    expect(code).toMatch(/\.replace\(\s*\/\s*'\s*\/\s*g\s*,\s*"/);
  });

  it("alert_dialog does not use .append(settings.content) directly", () => {
    expect(code).not.toMatch(/\.append\(\s*settings\.content\s*\)/);
  });

  it("confirm_dialog does not use .append(settings.question) directly", () => {
    expect(code).not.toMatch(/\.append\(\s*settings\.question\s*\)/);
  });
});

describe("perc_utils.js dialog content XSS regression (js/xss-through-dom)", () => {
  it("alert_dialog renders hostile content as inert text by default", () => {
    const payload = `<img src=x ${XSS_MARKER}>`;
    globalThis.jQuery.perc_utils.alert_dialog({
      title: "alert-text-title",
      content: payload,
    });

    expect(document.querySelectorAll("img").length).toBe(0);
    expect(globalThis.__perc_utils_pwned__).toBeUndefined();
    const sink = document.body.querySelector(
      "[data-jquery-ui-dialog-title='alert-text-title']",
    );
    expect(sink, "alert_dialog sink must be attached to body").toBeTruthy();
    expect(sink.textContent).toBe(payload);
  });

  it("alert_dialog strips a disallowed <script> as text (no live script element)", () => {
    const payload =
      "before<script>window.__perc_utils_pwned__=true</script>after";
    globalThis.jQuery.perc_utils.alert_dialog({
      title: "Test",
      content: payload,
    });

    expect(document.querySelectorAll("script").length).toBe(0);
    expect(globalThis.__perc_utils_pwned__).toBeUndefined();
    expect(document.body.textContent).toContain("before");
    expect(document.body.textContent).toContain("after");
  });

  it("confirm_dialog renders hostile question as inert text by default", () => {
    const payload = `<img src=x ${XSS_MARKER}>`;
    globalThis.jQuery.perc_utils.confirm_dialog({
      title: "confirm-text-title",
      question: payload,
      success: () => {},
      cancel: () => {},
    });

    expect(document.querySelectorAll("img").length).toBe(0);
    expect(globalThis.__perc_utils_pwned__).toBeUndefined();
    const sink = document.body.querySelector(
      "[data-jquery-ui-dialog-title='confirm-text-title']",
    );
    expect(sink, "confirm_dialog sink must be attached to body").toBeTruthy();
    expect(sink.textContent).toBe(payload);
  });

  it("prompt_dialog renders hostile question as inert text by default", () => {
    const payload = `<img src=x ${XSS_MARKER}>`;
    globalThis.jQuery.perc_utils.prompt_dialog({
      title: "prompt-text-title",
      question: payload,
      success: () => {},
      cancel: () => {},
    });

    expect(document.querySelectorAll("img").length).toBe(0);
    expect(globalThis.__perc_utils_pwned__).toBeUndefined();
    const label = document.body.querySelector(
      "label[for='perc-prompt-dialog-question']",
    );
    expect(label).toBeTruthy();
    expect(label.textContent).toBe(payload);
  });

  it("alert_dialog renders HTML only when contentIsHtml is true", () => {
    globalThis.jQuery.perc_utils.alert_dialog({
      title: "alert-html-title",
      content: "<span id='perc-warn' style='color:red'>Careful!</span>",
      contentIsHtml: true,
    });

    const span = document.querySelector("#perc-warn");
    expect(span).not.toBeNull();
    expect(span.tagName).toBe("SPAN");
    expect(span.getAttribute("style")).toContain("color");
    expect(span.textContent).toBe("Careful!");
  });

  it("confirm_dialog renders HTML only when questionIsHtml is true", () => {
    const htmlQuestion =
      "<p id='perc-delete-dialog-warning'>Warning</p>" +
      "<strong>This role has active users</strong><br/><br/>" +
      "<p id='perc-delete-warn-msg'>Are you sure you want to delete role 'Editors'?</p>";

    globalThis.jQuery.perc_utils.confirm_dialog({
      title: "Delete Role",
      question: htmlQuestion,
      questionIsHtml: true,
      success: () => {},
      cancel: () => {},
    });

    expect(
      document.querySelector("#perc-delete-dialog-warning").textContent,
    ).toBe("Warning");
    expect(document.querySelector("strong").textContent).toBe(
      "This role has active users",
    );
    expect(document.querySelectorAll("br").length).toBe(2);
    expect(
      document.querySelector("#perc-delete-warn-msg").textContent,
    ).toContain("Editors");
  });

  it("without contentIsHtml, markup is not parsed as elements", () => {
    globalThis.jQuery.perc_utils.alert_dialog({
      title: "Test",
      content: "<span id='perc-x'>text</span>",
    });

    expect(document.querySelector("#perc-x")).toBeNull();
    expect(document.body.textContent).toContain(
      "<span id='perc-x'>text</span>",
    );
  });

  it("alert_dialog with contentIsHtml preserves table markup for LDAP-import warning pattern", () => {
    const table =
      "<div id='perc-users-import-warning-scrollpane'>" +
      "<table id='perc-users-import-warning'>" +
      "<tr><td class='perc-users-row'><span>alice</span></td><td><span></span></td></tr>" +
      "<tr><td class='perc-users-row'><span>bob</span></td><td><span></span></td></tr>" +
      "</table></div>";

    globalThis.jQuery.perc_utils.alert_dialog({
      title: "Error Importing Users",
      content: "LDAP import failed for the following users:<br/><br/>" + table,
      contentIsHtml: true,
    });

    const rows = document.querySelectorAll("#perc-users-import-warning tr");
    expect(rows.length).toBe(2);
    expect(document.body.textContent).toContain("alice");
    expect(document.body.textContent).toContain("bob");
  });

  it("alert_dialog with contentIsHtml preserves replaceURLWithHTMLLinks <a href>", () => {
    const message = globalThis.jQuery.perc_utils.replaceURLWithHTMLLinks(
      "See https://example.com/docs for details.",
    );
    globalThis.jQuery.perc_utils.alert_dialog({
      title: "Test",
      content: message,
      contentIsHtml: true,
    });

    const link = document.querySelector("a");
    expect(link).not.toBeNull();
    expect(link.getAttribute("href")).toBe("https://example.com/docs");
    expect(link.textContent).toBe("https://example.com/docs");
  });
});

describe("perc_utils.js htmlEntities regression (js/incomplete-sanitization)", () => {
  it("escapes every single quote in the input, not just the first", () => {
    const result = globalThis.htmlEntities("it's a 'test' of multiple quotes");
    expect(result).not.toContain("'");
    expect(result).toBe("it&#39;s a &#39;test&#39; of multiple quotes");
  });

  it("still escapes the other special characters", () => {
    const result = globalThis.htmlEntities(`<a href="x">&'</a>`);
    expect(result).toBe("&lt;a href=&quot;x&quot;&gt;&amp;&#39;&lt;/a&gt;");
  });
});

describe("public API", () => {
  it("$.perc_utils exposes the documented dialog helpers", () => {
    expect(typeof globalThis.jQuery.perc_utils.alert_dialog).toBe("function");
    expect(typeof globalThis.jQuery.perc_utils.confirm_dialog).toBe("function");
    expect(typeof globalThis.jQuery.perc_utils.prompt_dialog).toBe("function");
  });
});
