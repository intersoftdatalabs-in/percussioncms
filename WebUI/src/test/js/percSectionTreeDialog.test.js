/**
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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Regression tests for WebUI/src/main/webapp/cm/plugins/PercSectionTreeDialog.js
 *
 * Closes WebUI code-scanning alerts (js/incomplete-sanitization) flagged on
 * the `.html(treeLabel)` and `.attr("data", "sectionName:'" + title + "'")`
 * sinks inside `openDialog` / `buildSectionTreeList`.
 *
 * Test strategy (per Constitution III, fail-then-pass):
 *   - Drive the IIFE-bound `$.PercSectionTreeDialog.open(...)` against a real
 *     jQuery on a fresh jsdom window for each test.
 *   - Feed attacker-controlled strings through the documented public API
 *     (`treeLabel` and `sectionNode.title`).
 *   - Assert that NO element created from those inputs becomes a live script /
 *     image / event-handler tag (which is what `js/incomplete-sanitization`
 *     is reporting).
 *   - The same inputs passed through `.html()` on pre-fix code WOULD render as
 *     DOM elements; through `.text()` (or quote-escaped attribute values) on
 *     post-fix code they become inert text/attribute content.
 */

import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import jquery from "jquery";
import { beforeEach, afterEach, describe, it, expect, vi } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));
const SRC_PATH = resolve(
  __dirname,
  "../../main/webapp/cm/plugins/PercSectionTreeDialog.js",
);

let $;
let getTreeSpy;

// ---------------------------------------------------------------------------
// Bootstrap helper: rebind jQuery against the current test window, register
// plugin stubs, and load PercSectionTreeDialog.js into the test scope.
//
// Vitest already provides jsdom (`environment: "jsdom"` per vite.config.ts),
// so we use the global `window` / `document` and reset the body between
// tests instead of constructing a new JSDOM every time. Each test still
// starts from a clean DOM because `beforeEach` clears `document.body`.
// ---------------------------------------------------------------------------
function installDialogEnvironment() {
  document.body.innerHTML = "";

  // The `jquery` CommonJS export auto-binds to `globalThis.window` the first
  // time it is required inside Vitest's jsdom environment. Re-call it on the
  // current window to obtain a usable jQuery instance with `.fn` populated.
  if (typeof jquery === "function") {
    $ = jquery(globalThis.window);
    if (typeof $ !== "function" || !$.fn) {
      // jquery(globalThis.window) returned an empty jQuery object (the
      // window was treated as a CSS selector). Fall back to the function
      // form by assigning the module default to a selector-free alias.
      $ = jquery;
      if (!$.fn) $.fn = $.prototype;
    }
  } else {
    // Default export is an empty jQuery collection (e.g. when jQuery has
    // already bound to globalThis.window). Look up the bound instance.
    $ = globalThis.window.jQuery || globalThis.window.$;
    if (!$.fn) $.fn = $.prototype;
  }
  if (!$.fn || typeof $.fn !== "object") {
    throw new Error("Could not obtain a usable jQuery instance with .fn");
  }

  // Capture the callback so tests can drive it synchronously.
  getTreeSpy = vi.fn(function (siteName, cb) {
    getTreeSpy.cb = cb;
  });

  const SECTION_TYPE = "perc-section";
  const STATUS_SUCCESS = "success";
  const STATUS_ERROR = "error";

  $.Perc_SectionServiceClient = {
    PERC_SECTION_TYPE: { SECTION: SECTION_TYPE },
    getTree: getTreeSpy,
  };
  $.PercServiceUtils = { STATUS_SUCCESS, STATUS_ERROR };
  $.perc_utils = { alert_dialog: vi.fn() };
  $.ui = { fancytree: { getTree: vi.fn(() => null) } };

  // No-op jQuery plugin stubs used by the dialog. The `perc_dialog`
  // stub additionally appends the constructed dialog to the document body
  // so individual tests can inspect the resulting DOM (and so jsdom can
  // parse the rendered `.html(...)` content meaningfully).
  $.fn.perc_dialog = vi.fn(function () {
    if (this[0] && this[0].nodeType === 1 /* ELEMENT_NODE */) {
      document.body.appendChild(this[0]);
    }
    return this;
  });
  $.fn.fancytree = vi.fn(function () {
    return this;
  });

  // Load PercSectionTreeDialog.js into a sandboxed scope that receives
  // `$` / `jQuery` / `I18N` parameters so the IIFE can attach to our shim.
  const factory = new Function(
    "jQuery",
    "$",
    "I18N",
    readFileSync(SRC_PATH, "utf8"),
  );
  factory($, $, { message: (key) => key });
}

beforeEach(() => {
  installDialogEnvironment();
});

afterEach(() => {
  vi.restoreAllMocks();
  document.body.innerHTML = "";
});

// ---------------------------------------------------------------------------
// Fixture builders
// ---------------------------------------------------------------------------
function makeSectionNode({
  id = "n1",
  title = "Home",
  sectionType,
  childNodes = "",
} = {}) {
  return {
    id,
    title,
    sectionType:
      sectionType ?? $.Perc_SectionServiceClient.PERC_SECTION_TYPE.SECTION,
    childNodes,
  };
}

function driveSuccess(sectionNode, opts = {}) {
  $.PercSectionTreeDialog.open(
    "site-1",
    "exclude-1",
    opts.treeLabel ?? "Tree label",
    opts.dlgTitle ?? "Dialog title",
    opts.okButton ?? "Move",
    () => {},
  );
  getTreeSpy.cb($.PercServiceUtils.STATUS_SUCCESS, {
    SectionNode: sectionNode,
  });
}

// ---------------------------------------------------------------------------
// Tree label (.html sink) — pre-fix `.html(treeLabel)` parses attacker HTML;
// post-fix `.text(treeLabel)` or similar keeps it inert.
// ---------------------------------------------------------------------------
describe("treeLabel sanitization", () => {
  it("does not produce a script tag from an attacker-controlled treeLabel", () => {
    const malicious = "<script>window.__pwned=true</script>";
    driveSuccess(makeSectionNode({ title: "Home" }), { treeLabel: malicious });

    const dialogRoot = document.body.lastElementChild;
    expect(dialogRoot, "dialog root should be appended").toBeTruthy();

    const scripts = dialogRoot.querySelectorAll("script");
    expect(scripts.length, "no <script> elements from treeLabel").toBe(0);
  });

  it("does not inject an event-handler element via treeLabel", () => {
    const malicious = '<img src="x" onerror="window.__pwned=1">';
    driveSuccess(makeSectionNode({ title: "Home" }), { treeLabel: malicious });

    const dialogRoot = document.body.lastElementChild;
    expect(dialogRoot).toBeTruthy();
    const imgs = dialogRoot.querySelectorAll("img");
    expect(imgs.length, "no <img> from treeLabel").toBe(0);
    // No element in the produced tree should carry an inline event handler.
    dialogRoot.querySelectorAll("*").forEach((el) => {
      for (const attr of el.attributes) {
        expect(/^on/i.test(attr.name), `inline handler attr ${attr.name}`).toBe(
          false,
        );
      }
    });
  });

  it("renders plain text labels without element children in the label div", () => {
    driveSuccess(makeSectionNode({ title: "Home" }), {
      treeLabel: "Pick a target section",
    });
    const dialogRoot = document.body.lastElementChild;
    // The label div (which receives `.html(treeLabel)` / `.text(treeLabel)`)
    // is the float-left div that contains the tree; its direct text node is
    // the rendered label. There must not be any element children spawned
    // from the label string.
    const labelContainer = dialogRoot.querySelector("div[style*='float:left']");
    expect(labelContainer).toBeTruthy();
    const firstChild = labelContainer.firstChild;
    expect(firstChild.nodeType, "first label child must be text").toBe(3);
    expect(firstChild.nodeValue).toBe("Pick a target section");
  });
});

// ---------------------------------------------------------------------------
// Section title (.attr "data" sink) — pre-fix interpolates the title into a
// JS-template literal with only quote escaping; post-fix must split the
// attributes so titles containing `"` or `'` or `<` are inert.
//
// Note: attribute VALUES in HTML are inherently inert (no <script> in an
// attribute executes, no <img> in an attribute renders). The attack surface
// for this sink is therefore quote-breakout of the `data="..."` attribute,
// which allows injection of NEW attributes (including event handlers).
// The relevant assertion is the inline-handler scan below; the wider
// DOM-element scan is just defense-in-depth.
// ---------------------------------------------------------------------------
describe("section title sanitization", () => {
  it("does not allow an embedded double-quote to break out of the data attribute", () => {
    const node = makeSectionNode({
      title: 'a" onmouseover="window.__pwned=1" data-x="',
    });
    driveSuccess(node);

    const dialogRoot = document.body.lastElementChild;
    expect(dialogRoot).toBeTruthy();

    // The injected `onmouseover` would land either as an inline event
    // handler (pre-fix, attribute breakout) or as escaped content (post-fix).
    dialogRoot.querySelectorAll("*").forEach((el) => {
      for (const attr of el.attributes) {
        expect(
          /^on/i.test(attr.name),
          `inline handler attr ${attr.name}=${attr.value} should not exist`,
        ).toBe(false);
      }
    });
  });

  it("preserves the original section title text content", () => {
    const node = makeSectionNode({ title: "Reports" });
    driveSuccess(node);

    const dialogRoot = document.body.lastElementChild;
    expect(dialogRoot.textContent).toContain("Reports");
  });

  it("renders a simple two-level section tree", () => {
    const child = makeSectionNode({ id: "c1", title: "Child" });
    const root = makeSectionNode({
      id: "r1",
      title: "Root",
      childNodes: { SectionNode: child },
    });
    driveSuccess(root);

    const dialogRoot = document.body.lastElementChild;
    const listItems = dialogRoot.querySelectorAll("li");
    expect(listItems.length).toBe(2);
    expect(dialogRoot.textContent).toContain("Root");
    expect(dialogRoot.textContent).toContain("Child");
  });
});

// ---------------------------------------------------------------------------
// Public API surface — keep this locked so future refactors don't accidentally
// remove the `open(siteName, excludeId, treeLabel, ...)` signature that
// callers like perc_site_map.js / perc_newSectionDialog.js depend on.
// ---------------------------------------------------------------------------
describe("public API", () => {
  it("exposes $.PercSectionTreeDialog.open as a function", () => {
    expect(typeof $.PercSectionTreeDialog).toBe("object");
    expect(typeof $.PercSectionTreeDialog.open).toBe("function");
  });
});
