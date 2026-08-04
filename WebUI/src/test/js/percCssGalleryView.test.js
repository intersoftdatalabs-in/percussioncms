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
 * Regression tests for WebUI/src/main/webapp/cm/views/PercCSSGalleryView.js
 *
 * Closes GitHub CodeQL alerts (js/xss-through-dom) flagged on the
 * `renderGalleryEntry(name, thumbUrl, nameAsEntered)` helper inside the
 * `render()` function. The helper concatenated the three theme-metadata
 * fields (a theme name, a thumbnail URL, and an entered display name)
 * into an HTML string and inserted the result into the DOM with
 * `.append(stringHTML)`.
 *
 * Pre-fix code parses attacker-controlled theme names and thumb URLs as
 * HTML, so a theme whose `name` contains `<script>...</script>` or whose
 * `thumbUrl` contains `<img src=x onerror=...>` produces live DOM
 * elements inside the theme gallery.
 *
 * Test strategy (Constitution III fail-then-pass):
 *   - Drive the live source end-to-end via `$.Percussion.cssGalleryView(controller)`.
 *   - Mock `controller.getThemeList(cb)` and `controller.getTemplateTheme(cb)`
 *     so the callback runs synchronously with attacker-controlled theme data.
 *   - Assert that no live `<script>` or `<img>` elements with inline event
 *     handlers are produced and that the malicious string is present as
 *     inert text/attribute content.
 */

import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import jquery from "jquery";
import { beforeEach, afterEach, describe, it, expect, vi } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));
const SRC_PATH = resolve(
  __dirname,
  "../../main/webapp/cm/views/PercCSSGalleryView.js",
);

let $;
let getThemeListSpy;
let getTemplateThemeSpy;

function seedDom() {
  document.body.innerHTML = `
    <div id="perc-css-gallery"></div>
    <div id="perc-css-gallery-status"></div>
    <div id="perc-css-gallery-button-bar"></div>
    <table id="perc-themes-table"><tr id="perc-themes-table-row"></tr></table>
  `;
}

function installPercShims() {
  $.Percussion = $.Percussion || {};
  $.PercDirtyController = { setDirty: vi.fn() };
  $.PercNavigationManager = {
    getView: () => "editor",
    VIEW_EDITOR: "editor",
    VIEW_EDIT_TEMPLATE: "edit_template",
  };
  globalThis.I18N = { message: (key) => `[${key}]` };
}

function loadSource() {
  $ = jquery(globalThis.window);
  let jq = $;
  if (typeof jq !== "function") {
    jq = typeof jquery === "function" ? jquery : jquery.fn || jquery;
    if (!jq.fn && jq.prototype) jq.fn = jq.prototype;
  }
  if (!jq.fn) throw new Error("jquery has no .fn");
  $ = jq;
  globalThis.jQuery = $;
  globalThis.$ = $;
  installPercShims();
  // Run the source as a script in the global scope so its IIFE sees
  // globalThis.jQuery and globalThis.jQuery.Percussion.
  (0, eval)(readFileSync(SRC_PATH, "utf8"));
}

function makeController() {
  // Capture callbacks so tests can drive them synchronously.
  getThemeListSpy = vi.fn(function (cb) {
    getThemeListSpy.cb = cb;
    return undefined;
  });
  getTemplateThemeSpy = vi.fn(function (cb) {
    getTemplateThemeSpy.cb = cb;
    return undefined;
  });
  return {
    getThemeList: getThemeListSpy,
    setTemplateTheme: vi.fn(function (name, cb) {
      cb(true);
    }),
    getTemplateTheme: getTemplateThemeSpy,
    save: vi.fn(),
  };
}

function fireGetThemeList(themes) {
  getThemeListSpy.cb(true, { ThemeSummary: themes });
}

beforeEach(() => {
  seedDom();
  loadSource();
});

afterEach(() => {
  vi.restoreAllMocks();
  document.body.innerHTML = "";
  delete globalThis.I18N;
});

// ---------------------------------------------------------------------------
// Source-pattern tests — pin the security-relevant pattern in the source
// so any future regression that reintroduces the string-template render
// fails immediately. Erlang rules warn against pure grep tests for
// non-trivial logic; here the security property IS the absence of the
// unsafe pattern, so a presence/absence check is the right tool.
// ---------------------------------------------------------------------------
describe("source-pattern (anti-regression for js/xss-through-dom)", () => {
  const src = readFileSync(SRC_PATH, "utf8");

  it("does not concatenate theme fields into an HTML string template", () => {
    // Pre-fix renderGalleryEntry builds a string via `'...<td>' + name + '...'`
    // and returns it for `.append(stringHTML)`. Post-fix must build the
    // element via the jQuery DOM API instead.
    expect(src).not.toMatch(/<td><div id="theme-"\s*\+/);
    expect(src).not.toMatch(/'"\s*\+\s*thumbUrl\s*\+\s*'/);
  });
});

// ---------------------------------------------------------------------------
// Behavioural tests — exercise the live source end-to-end.
// ---------------------------------------------------------------------------
describe("renderGalleryEntry (name sink)", () => {
  it("does not produce a <script> element from a malicious theme name", () => {
    const malicious = "<script>window.__pwned=true</script>";
    const controller = makeController();
    $.Percussion.cssGalleryView(controller);
    fireGetThemeList([{ name: malicious, thumbUrl: "/img/t.png" }]);

    const scripts = document.querySelectorAll(
      "#perc-css-gallery #perc-themes-table script",
    );
    expect(scripts.length, "no <script> from theme name").toBe(0);
    expect(window.__pwned).toBeUndefined();
  });

  it("does not inject an event-handler <img> from a malicious theme name", () => {
    const malicious = '<img src="x" onerror="window.__pwned=1">';
    const controller = makeController();
    $.Percussion.cssGalleryView(controller);
    fireGetThemeList([{ name: malicious, thumbUrl: "/img/t.png" }]);

    // Secure render always creates exactly one thumbnail <img> from thumbUrl
    // via the jQuery DOM API. Under jQuery 4 that element is present; the
    // theme *name* must never parse as HTML or add an onerror handler img.
    const imgs = document.querySelectorAll(
      "#perc-css-gallery #perc-themes-table img",
    );
    expect(imgs.length, "only the thumbUrl thumbnail <img>").toBe(1);
    expect(imgs[0].getAttribute("src")).toBe("/img/t.png");
    expect(imgs[0].hasAttribute("onerror")).toBe(false);
    expect(imgs[0].getAttribute("onerror")).toBeNull();

    const names = document.querySelectorAll(".perc-css-gallery-item-name");
    expect(names.length).toBe(1);
    expect(names[0].textContent).toBe(malicious);
    expect(names[0].querySelector("img")).toBeNull();
    expect(window.__pwned).toBeUndefined();
  });

  it("renders a benign theme name as inert text inside the gallery item", () => {
    const controller = makeController();
    $.Percussion.cssGalleryView(controller);
    fireGetThemeList([{ name: "Plain Theme", thumbUrl: "/img/t.png" }]);

    const names = document.querySelectorAll(".perc-css-gallery-item-name");
    expect(names.length).toBeGreaterThan(0);
    expect(names[0].textContent).toBe("Plain Theme");
  });
});

describe("renderGalleryEntry (thumbUrl sink)", () => {
  it("does not produce a <script> element from a malicious thumbUrl", () => {
    const malicious = '/img.png"><script>window.__pwned=true</script>';
    const controller = makeController();
    $.Percussion.cssGalleryView(controller);
    fireGetThemeList([{ name: "Plain", thumbUrl: malicious }]);

    const scripts = document.querySelectorAll(
      "#perc-css-gallery #perc-themes-table script",
    );
    expect(scripts.length, "no <script> from thumbUrl").toBe(0);
    expect(window.__pwned).toBeUndefined();
  });

  it("does not produce an event-handler <img> from a malicious thumbUrl", () => {
    const malicious = '/img.png" onerror="window.__pwned=1" data-x="';
    const controller = makeController();
    $.Percussion.cssGalleryView(controller);
    fireGetThemeList([{ name: "Plain", thumbUrl: malicious }]);

    // No inline handler attribute should appear on any element.
    document.querySelectorAll("#perc-css-gallery *").forEach((el) => {
      for (const attr of el.attributes) {
        expect(/^on/i.test(attr.name), `inline handler ${attr.name}`).toBe(
          false,
        );
      }
    });
    expect(window.__pwned).toBeUndefined();
  });
});

// ---------------------------------------------------------------------------
// Public API surface — pin the namespace and exported factory so callers
// keep working.
// ---------------------------------------------------------------------------
describe("public API", () => {
  it("$.Percussion.cssGalleryView is a function", () => {
    expect(typeof $.Percussion.cssGalleryView).toBe("function");
  });
});
