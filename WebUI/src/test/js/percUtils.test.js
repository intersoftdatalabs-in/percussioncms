/**
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
 * Closes 8 GitHub CodeQL alerts across 3 rules:
 *
 * 1. js/unsafe-jquery-plugin (6 alerts) on `$.fn.perc_toggle`: the plugin
 *    handed its `d` argument straight to jQuery's `$()` HTML-parsing
 *    constructor on every branch (`$(d).hasClass(...)`,
 *    `$(d).removeClass(...)`, `$(d).addClass(...)`) with no guard against
 *    `d` being an attacker-influenced string that looks like HTML.
 *    Post-fix, a string `d` is resolved via `.find()` (Sizzle selector
 *    engine only, never HTML-sniffs) instead of the raw `$()` sniffing
 *    constructor.
 *
 * 2. js/xss-through-dom (1 alert, on `alert_dialog`'s
 *    `.append(settings.content)`): the `content`/`question` options of
 *    `alert_dialog()`, `confirm_dialog()`, and `prompt_dialog()` (the
 *    latter two share the exact same pattern, fixed alongside the
 *    flagged one) were appended to the dialog markup unescaped.
 *
 *    Several real first-party callers intentionally pass a narrow set
 *    of formatting HTML (e.g. PercRoleController's "Delete Role"
 *    confirmation uses `<p>`/`<strong>`/`<br/>`; perc_editSiteSectionDialog's
 *    "Disable Site Security" confirmation uses `<span id=... style=...>`;
 *    PercUserView's LDAP-import-failure warning builds a `<table>` of
 *    the affected usernames; perc_utils.replaceURLWithHTMLLinks() turns
 *    bare URLs into `<a href="...">` links) -- so post-fix code runs
 *    the option through `percSafeDialogContent()`, an allowlist HTML
 *    sanitizer that preserves that narrow set of structural/style tags
 *    and `id`/`class`/`style`/`href` attributes while stripping
 *    everything else (event handler attributes, `<script>`/`<img>`/
 *    `<iframe>`/any other tag, `javascript:`/`data:`/`vbscript:` URLs,
 *    CSS `expression()`/`url(javascript:)` tricks), unless the caller
 *    already supplied a jQuery object / DOM element, which passes
 *    through untouched.
 *
 * 3. js/incomplete-sanitization (1 alert) on `htmlEntities()`: the final
 *    `.replace(/'/, "&#39;")` was missing the `g` flag, so only the
 *    *first* single quote in a string was escaped.
 *
 * Test strategy (Constitution III fail-then-pass):
 * - Load the real, checked-in source via `readFileSync` + indirect
 *   `eval` (same general pattern as the other regression tests in this
 *   directory; indirect eval is required here specifically because
 *   `htmlEntities()` is a bare top-level function declaration outside
 *   the file's own IIFE, and a direct `eval()` from this ES module,
 *   which is implicitly strict mode, would not leak that declaration to
 *   `globalThis`).
 * - Spy on the global `$`/`jQuery` entry point to prove `perc_toggle`
 *   never hands a hostile string to the HTML-parsing constructor.
 * - Drive `alert_dialog`/`confirm_dialog`/`prompt_dialog` end-to-end
 *   (via a `$.fn.perc_dialog`/`.dialog()` stub that appends to
 *   `document.body`) and assert hostile `content`/`question` values
 *   never produce a live `<img>`/`<script>` element or survive as a
 *   dangerous attribute, while confirming the real-world formatting
 *   patterns above (span/style, p/strong/br, table/tr/td, a/href) still
 *   render with their structure, attributes, and text intact.
 * - Call `htmlEntities()` directly with multiple single quotes.
 */

import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import jquery from "jquery";
import { beforeEach, afterEach, describe, it, expect } from "vitest";

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

  // Minimal $.fn.perc_dialog stub: real DOM insertion (so we can make
  // genuine DOM assertions) without any of the real perc_dialog plugin's
  // jQuery-UI machinery.
  if (!spy.fn.perc_dialog) {
    spy.fn.perc_dialog = function (opts) {
      this.data("perc_dialog_opts", opts);
      document.body.appendChild(this[0]);
      return this;
    };
  }
  // prompt_dialog() uses the plain jQuery UI .dialog() (not .perc_dialog());
  // stub it the same way.
  if (!spy.fn.dialog) {
    spy.fn.dialog = function (opts) {
      this.data("dialog_opts", opts);
      document.body.appendChild(this[0]);
      return this;
    };
  }

  globalThis.jQuery = spy;
  globalThis.$ = spy;

  const src = readFileSync(SRC_PATH, "utf8");
  // Indirect eval (aliasing `eval` before calling it) runs the source as
  // global code: top-level function declarations outside the file's own
  // IIFE (e.g. `htmlEntities`) become real global bindings, which a
  // direct `eval()` call would NOT do from this (implicitly strict) ES
  // module. See the module doc comment for details.
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
});

describe("perc_utils.js $.fn.perc_toggle XSS regression (js/unsafe-jquery-plugin)", () => {
  it("never hands a hostile string argument to the jQuery HTML-parsing constructor", () => {
    document.body.innerHTML =
      '<div id="perc-toggle-target" class="perc-hidden"></div>';
    const payload = `<img src=x ${XSS_MARKER}>`;

    // perc_toggle is invoked as $anything.perc_toggle(d); the jQuery
    // object it's called on is irrelevant to the implementation, which
    // operates on the `d` argument. A hostile, non-selector-looking
    // string is resolved via the Sizzle selector engine (.find()), which
    // throws on invalid selector syntax rather than ever falling back to
    // HTML parsing -- unlike pre-fix `$(d)`, which silently parsed it
    // into a live <img> element. Either a thrown error or a no-op is an
    // acceptable, non-vulnerable outcome here; what matters is that the
    // raw markup is never handed to the HTML-parsing constructor and no
    // <img> element is ever created.
    try {
      globalThis.jQuery(document.body).perc_toggle(payload);
    } catch (e) {
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

describe("perc_utils.js dialog content XSS regression (js/xss-through-dom)", () => {
  it("alert_dialog does not create a live <img> element from a hostile content string", () => {
    globalThis.jQuery.perc_utils.alert_dialog({
      title: "Test",
      content: `<img src=x ${XSS_MARKER}>`,
    });

    expect(document.querySelectorAll("img").length).toBe(0);
    expect(globalThis.__perc_utils_pwned__).toBeUndefined();
  });

  it("alert_dialog strips a disallowed <script> tag entirely, plain text still renders", () => {
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

  it("confirm_dialog does not create a live <img> element from a hostile question string", () => {
    globalThis.jQuery.perc_utils.confirm_dialog({
      title: "Test",
      question: `<img src=x ${XSS_MARKER}>`,
      success: () => {},
      cancel: () => {},
    });

    expect(document.querySelectorAll("img").length).toBe(0);
    expect(globalThis.__perc_utils_pwned__).toBeUndefined();
  });

  it("prompt_dialog does not create a live <img> element from a hostile question string", () => {
    globalThis.jQuery.perc_utils.prompt_dialog({
      title: "Test",
      question: `<img src=x ${XSS_MARKER}>`,
      success: () => {},
      cancel: () => {},
    });

    expect(document.querySelectorAll("img").length).toBe(0);
    expect(globalThis.__perc_utils_pwned__).toBeUndefined();
  });

  // Regression coverage for real first-party callers that intentionally
  // format their content/question with a narrow set of structural/style
  // tags (see e.g. PercRoleController's "Delete Role" confirmation and
  // perc_editSiteSectionDialog's "Disable Site Security" confirmation) --
  // a blanket text-escape would have visibly broken these dialogs.
  it("alert_dialog preserves an allowlisted <span id/style> wrapper and its text", () => {
    globalThis.jQuery.perc_utils.alert_dialog({
      title: "Test",
      content: "<span id='perc-warn' style='color:red'>Careful!</span>",
    });

    const span = document.querySelector("#perc-warn");
    expect(span).not.toBeNull();
    expect(span.tagName).toBe("SPAN");
    expect(span.getAttribute("style")).toContain("color");
    expect(span.textContent).toBe("Careful!");
  });

  it("confirm_dialog preserves the real-world <p>/<strong>/<br/> pattern used by PercRoleController's delete-role confirmation", () => {
    const htmlQuestion =
      "<p id='perc-delete-dialog-warning'>Warning</p>" +
      "<strong>This role has active users</strong><br/><br/>" +
      "<p id='perc-delete-warn-msg'>Are you sure you want to delete role 'Editors'?</p>";

    globalThis.jQuery.perc_utils.confirm_dialog({
      title: "Delete Role",
      question: htmlQuestion,
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

  it("strips a disallowed attribute (onerror) even on an allowlisted tag", () => {
    globalThis.jQuery.perc_utils.alert_dialog({
      title: "Test",
      content: `<span id="perc-x" ${XSS_MARKER}>text</span>`,
    });

    const span = document.querySelector("#perc-x");
    expect(span).not.toBeNull();
    expect(span.getAttribute("onerror")).toBeNull();
    expect(globalThis.__perc_utils_pwned__).toBeUndefined();
  });

  it("strips a javascript: value even from an allowlisted attribute", () => {
    globalThis.jQuery.perc_utils.alert_dialog({
      title: "Test",
      content:
        '<span id="perc-y" style="background:url(javascript:alert(1))">text</span>',
    });

    const span = document.querySelector("#perc-y");
    expect(span).not.toBeNull();
    expect(span.getAttribute("style")).toBeNull();
  });

  // Regression test for the real-world pattern used by
  // PercUserView.js#showImportWarning(): a <table> listing the
  // usernames that failed LDAP import. An earlier, narrower version of
  // the allowlist did not include TABLE/TR/TD and silently deleted the
  // entire table (defeating the purpose of the dialog); this locks in
  // that the table structure and every username survive.
  it("alert_dialog preserves the real-world <table>/<tr>/<td> pattern used by PercUserView's LDAP-import warning", () => {
    const table =
      "<div id='perc-users-import-warning-scrollpane'>" +
      "<table id='perc-users-import-warning'>" +
      "<tr><td class='perc-users-row'><span>alice</span></td><td><span></span></td></tr>" +
      "<tr><td class='perc-users-row'><span>bob</span></td><td><span></span></td></tr>" +
      "</table></div>";

    globalThis.jQuery.perc_utils.alert_dialog({
      title: "Error Importing Users",
      content: "LDAP import failed for the following users:<br/><br/>" + table,
    });

    const rows = document.querySelectorAll("#perc-users-import-warning tr");
    expect(rows.length).toBe(2);
    expect(document.body.textContent).toContain("alice");
    expect(document.body.textContent).toContain("bob");
  });

  it("still strips a hostile tag nested inside an allowlisted <table> cell (defense in depth)", () => {
    const table =
      "<table id='perc-users-import-warning'>" +
      `<tr><td><span><img src=x ${XSS_MARKER}></span></td></tr>` +
      "</table>";

    globalThis.jQuery.perc_utils.alert_dialog({
      title: "Test",
      content: table,
    });

    expect(document.querySelectorAll("img").length).toBe(0);
    expect(globalThis.__perc_utils_pwned__).toBeUndefined();
    expect(document.querySelectorAll("table tr").length).toBe(1);
  });

  // Regression test for perc_utils.replaceURLWithHTMLLinks(), which
  // turns a bare http(s)/ftp/file URL inside a message into a real
  // <a href="...">...</a> link before the message is handed to
  // alert_dialog (see e.g. PercUserView's showImportWarning()).
  it("alert_dialog preserves an <a href> link produced by replaceURLWithHTMLLinks()", () => {
    const message = globalThis.jQuery.perc_utils.replaceURLWithHTMLLinks(
      "See https://example.com/docs for details.",
    );
    globalThis.jQuery.perc_utils.alert_dialog({
      title: "Test",
      content: message,
    });

    const link = document.querySelector("a");
    expect(link).not.toBeNull();
    expect(link.getAttribute("href")).toBe("https://example.com/docs");
    expect(link.textContent).toBe("https://example.com/docs");
  });

  it("strips a javascript: href from an <a> tag but keeps its (inert) text", () => {
    globalThis.jQuery.perc_utils.alert_dialog({
      title: "Test",
      content: `<a href="javascript:${XSS_MARKER}">click me</a>`,
    });

    const link = document.querySelector("a");
    expect(link).not.toBeNull();
    expect(link.getAttribute("href")).toBeNull();
    expect(link.textContent).toBe("click me");
    expect(globalThis.__perc_utils_pwned__).toBeUndefined();
  });

  // Real URL parsers (WHATWG URL Standard, used by browsers for href
  // navigation) strip TAB/LF/CR from anywhere in a URL before resolving
  // its scheme, so an embedded control character inside the literal
  // scheme name still resolves to a live javascript: URL even though it
  // doesn't contain the literal substring "javascript:". A naive
  // substring-only check would miss this.
  it.each([
    ["tab", "java\tscript:alert(1)"],
    ["newline", "java\nscript:alert(1)"],
    ["carriage return", "java\rscript:alert(1)"],
    ["multiple embedded tabs", "j\tav\tascript:alert(1)"],
  ])(
    "strips a javascript: href obfuscated with an embedded %s",
    (_label, hostileHref) => {
      globalThis.jQuery.perc_utils.alert_dialog({
        title: "Test",
        content: `<a href="${hostileHref}">click me</a>`,
      });

      const link = document.querySelector("a");
      expect(link).not.toBeNull();
      expect(link.getAttribute("href")).toBeNull();
    },
  );

  it("strips a style value obfuscated with an embedded tab the same way", () => {
    globalThis.jQuery.perc_utils.alert_dialog({
      title: "Test",
      content:
        '<span id="perc-z" style="background:url(java\tscript:alert(1))">text</span>',
    });

    const span = document.querySelector("#perc-z");
    expect(span).not.toBeNull();
    expect(span.getAttribute("style")).toBeNull();
  });

  it("strips a disallowed <iframe> tag entirely (not in the allowlist)", () => {
    globalThis.jQuery.perc_utils.alert_dialog({
      title: "Test",
      content: `<iframe src="javascript:${XSS_MARKER}"></iframe>`,
    });

    expect(document.querySelectorAll("iframe").length).toBe(0);
    expect(globalThis.__perc_utils_pwned__).toBeUndefined();
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
