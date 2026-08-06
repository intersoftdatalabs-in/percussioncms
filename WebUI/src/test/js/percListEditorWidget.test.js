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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Regression tests for WebUI/src/main/webapp/cm/plugins/PercListEditorWidget.js
 *
 * Closes GitHub CodeQL alerts (js/xss-through-dom) flagged on the widget's
 * HTML-string template (which interpolates `options.title1`, `options.title2`,
 * and per-item `username` placeholders) and the resulting
 * `containerId.html(html)` / `list.append(li)` sinks.
 *
 * Pre-fix code parses attacker-controlled titles and usernames as HTML, so
 * a title/username containing `<script>...</script>` or
 * `<img onerror=...>` produces live DOM elements inside the widget.
 *
 * Test strategy (Constitution III fail-then-pass):
 *   - Drive the live source end-to-end via `$.PercListEditorWidget(options)`
 *     and its `setListItems(...)` / `addListItem(...)` API.
 *   - Seed titles via `options.title1`/`title2` and usernames via the
 *     setListItems API, with attacker-controlled HTML payloads.
 *   - Assert that no live `<script>` / `<img>` elements are produced
 *     and that the malicious string is present as inert text.
 */

import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import jquery from "jquery";
import { beforeEach, afterEach, describe, it, expect, vi } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));
const SRC_PATH = resolve(
  __dirname,
  "../../main/webapp/cm/plugins/PercListEditorWidget.js",
);

let $;
let widget;
let togglerEl;

function seedDom() {
  document.body.innerHTML = `
    <div id="perc-list-editor-host"></div>
    <input id="toggler" type="checkbox" />
  `;
  togglerEl = document.getElementById("toggler");
}

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
  $.perc_utils = { sortCaseInsensitive: (arr) => arr.sort() };
  $.browser = { msie: false, version: "0" };
  $.fn.autocomplete = function () {
    return {
      result: () => this,
      flushCache: () => this,
      setOptions: () => this,
    };
  };
  $.fn.setOptions = function () {
    return this;
  };
  globalThis.I18N = { message: (key) => `[${key}]` };
  // Run the source as a script in the global scope so its IIFE sees
  // globalThis.jQuery.
  (0, eval)(readFileSync(SRC_PATH, "utf8"));
}

function makeWidget(opts = {}) {
  return $.PercListEditorWidget({
    container: "perc-list-editor-host",
    title1: opts.title1 ?? "T1",
    title2: opts.title2 ?? "T2",
    help: opts.help ?? "help text",
    toggler: $(togglerEl),
    results: opts.results ?? [],
    ...opts,
  });
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
// so any future regression that reintroduces the unsafe `.html(stringHTML)`
// with caller-controlled interpolation fails immediately. Erlang rules warn
// against pure grep tests for non-trivial logic; here the security property
// IS the absence of the unsafe pattern, so a presence/absence check is the
// right tool.
// ---------------------------------------------------------------------------
describe("source-pattern (anti-regression for js/xss-through-dom)", () => {
  const src = readFileSync(SRC_PATH, "utf8");

  it("does not concatenate options.title1/title2 into an HTML string", () => {
    // Pre-fix: `html = '...div... + options.title1 + ...'`.
    // Post-fix: built via jQuery DOM API with `.text(options.title1)`.
    expect(src).not.toMatch(/\+\s*options\.title1\s*\+/);
    expect(src).not.toMatch(/\+\s*options\.title2\s*\+/);
  });

  it("does not substitute _username_ into an HTML string template", () => {
    // Pre-fix `listItem.replace(/_username_/g, listItems[u])` builds a
    // string with attacker-controlled usernames, then `.append(li)`
    // parses it as HTML.
    expect(src).not.toMatch(
      /\.replace\(\s*\/_username_\/g\s*,\s*listItems\[u\]\s*\)/,
    );
  });
});

// ---------------------------------------------------------------------------
// Behavioural tests — exercise the live widget end-to-end.
// ---------------------------------------------------------------------------
describe("title1 / title2 sink", () => {
  it("does not produce a <script> element from a malicious title1", () => {
    const malicious = "<script>window.__pwned=true</script>";
    makeWidget({ title1: malicious });
    const scripts = document.querySelectorAll(
      "#perc-list-editor-host #perc-ui-permission-users-title script",
    );
    expect(scripts.length, "no <script> from title1").toBe(0);
    expect(window.__pwned).toBeUndefined();
  });

  it("does not produce an event-handler <img> from a malicious title1", () => {
    const malicious = '<img src="x" onerror="window.__pwned=1">';
    makeWidget({ title1: malicious });
    const imgs = document.querySelectorAll(
      "#perc-list-editor-host #perc-ui-permission-users-title img",
    );
    expect(imgs.length, "no <img> from title1").toBe(0);
    expect(window.__pwned).toBeUndefined();
  });

  it("renders a benign title1 as inert text", () => {
    makeWidget({ title1: "Hello World" });
    const t1 = document.querySelector(
      "#perc-list-editor-host #perc-ui-permission-users-title",
    );
    expect(t1).toBeTruthy();
    expect(t1.textContent).toBe("Hello World");
    expect(t1.children.length, "title1 has no element children").toBe(0);
  });
});

describe("username sink via setListItems", () => {
  it("does not produce a <script> element from a malicious username", () => {
    widget = makeWidget();
    widget.setListItems(["<script>window.__pwned=true</script>"]);
    const scripts = document.querySelectorAll(
      "#perc-list-editor-host #perc-ui-permission-user-list script",
    );
    expect(scripts.length, "no <script> from username").toBe(0);
    expect(window.__pwned).toBeUndefined();
  });

  it("does not inject an event-handler <img> from a malicious username", () => {
    widget = makeWidget();
    widget.setListItems(['<img src="x" onerror="window.__pwned=1">']);
    document
      .querySelectorAll(
        "#perc-list-editor-host #perc-ui-permission-user-list *",
      )
      .forEach((el) => {
        for (const attr of el.attributes) {
          expect(/^on/i.test(attr.name), `inline handler ${attr.name}`).toBe(
            false,
          );
        }
      });
    expect(window.__pwned).toBeUndefined();
  });

  it("does not break out of the delete-button id attribute via embedded quote", () => {
    widget = makeWidget();
    widget.setListItems(['a" onmouseover="window.__pwned=1" data-x="']);
    document
      .querySelectorAll(
        "#perc-list-editor-host #perc-ui-permission-user-list *",
      )
      .forEach((el) => {
        for (const attr of el.attributes) {
          expect(/^on/i.test(attr.name), `inline handler ${attr.name}`).toBe(
            false,
          );
        }
      });
    expect(window.__pwned).toBeUndefined();
  });

  it("renders benign usernames as inert text inside .perc-ui-permission-username", () => {
    widget = makeWidget();
    widget.setListItems(["Alice", "Bob"]);
    const items = document.querySelectorAll(
      "#perc-list-editor-host #perc-ui-permission-user-list li",
    );
    expect(items.length).toBe(2);
    const names = Array.from(items).map((li) => {
      const span = li.querySelector("span");
      return span ? span.textContent : null;
    });
    expect(names).toContain("Alice");
    expect(names).toContain("Bob");
  });
});

// ---------------------------------------------------------------------------
// Public API surface — pin the widget's exported method names so callers
// keep working.
// ---------------------------------------------------------------------------
describe("public API", () => {
  it("$.PercListEditorWidget(options) returns the documented methods", () => {
    const api = makeWidget();
    const expected = [
      "isEnabled",
      "setListItems",
      "getListItems",
      "removeListItems",
      "addListItem",
      "removeListItem",
      "enable",
      "disable",
      "highlightListItem",
      "scrollToListItem",
    ];
    for (const name of expected) {
      expect(typeof api[name], `api.${name} should be a function`).toBe(
        "function",
      );
    }
  });
});
