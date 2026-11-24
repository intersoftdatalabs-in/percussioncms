/*
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
 * Behavioral parity for the npm-served claviska dropdown
 * (issue #7176, parent audit #5627).
 *
 * The deleted 2013 vendored twins are replaced by the author's scoped
 * package `@claviska/jquery-dropdown` 2.0.3 (same upstream lineage, MIT)
 * at the same stable jslib URLs. The plugin has zero in-repo callers, so
 * these pins exercise the shipped `jqDropdown` API directly against the
 * npm dist with the repo's jQuery: attach/detach attribute round-trip,
 * show/hide open-state, disable/enable gating, document-click delegation
 * via `data-jq-dropdown`, and the `.jq-dropdown-*` classes in the
 * companion CSS. Any npm upgrade that changes this surface fails here
 * instead of silently changing classic dropdown behavior.
 */

import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import jquery from "jquery";
import { describe, it, expect } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));
const FRONTEND_ROOT = dirname(dirname(dirname(__dirname)));
const NPM_ROOT = join(FRONTEND_ROOT, "node_modules/@claviska/jquery-dropdown");

function loadPlugin() {
  let jq = jquery(globalThis.window);
  if (typeof jq !== "function") {
    jq = typeof jquery === "function" ? jquery : jquery.fn || jquery;
    if (!jq.fn && jq.prototype) jq.fn = jq.prototype;
  }
  if (!jq.fn) throw new Error("jquery has no .fn");
  globalThis.jQuery = jq;
  globalThis.$ = jq;

  const src = readFileSync(join(NPM_ROOT, "jquery.dropdown.js"), "utf8");
  const run = new Function("window", "jQuery", "$", src);
  run(globalThis.window, jq, jq);
  return jq;
}

const $ = loadPlugin();

function buildDropdown() {
  document.body.innerHTML =
    `<a href="#" id="trigger" data-jq-dropdown="#menu">open</a>` +
    `<div id="menu" class="jq-dropdown jq-dropdown-tip">` +
    `<ul class="jq-dropdown-menu">` +
    `<li><a href="#a">one</a></li>` +
    `</ul></div>`;
  return { trigger: $("#trigger"), menu: $("#menu") };
}

describe("claviska dropdown served from npm (issue #7176)", () => {
  it("exposes the jqDropdown plugin entry point", () => {
    expect(typeof $.fn.jqDropdown).toBe("function");
  });

  it("attaches and detaches the menu selector", () => {
    const { trigger } = buildDropdown();
    trigger.jqDropdown("attach", "#other");
    expect(trigger.attr("data-jq-dropdown")).toBe("#other");
    trigger.jqDropdown("detach");
    expect(trigger.attr("data-jq-dropdown")).toBeUndefined();
  });

  it("opens the menu on show and closes on hide", () => {
    const { trigger, menu } = buildDropdown();
    const ret = trigger.jqDropdown("show");
    expect(ret.get(0)).toBe(trigger.get(0));
    expect(trigger.hasClass("jq-dropdown-open")).toBe(true);
    expect(menu.get(0).style.display).not.toBe("none");
    expect(menu.data("jq-dropdown-trigger").get(0)).toBe(trigger.get(0));
    trigger.jqDropdown("hide");
    expect(trigger.hasClass("jq-dropdown-open")).toBe(false);
  });

  it("blocks show while disabled and re-opens after enable", () => {
    const { trigger } = buildDropdown();
    trigger.jqDropdown("disable");
    expect(trigger.hasClass("jq-dropdown-disabled")).toBe(true);
    trigger.jqDropdown("show");
    expect(trigger.hasClass("jq-dropdown-open")).toBe(false);
    const ret = trigger.jqDropdown("enable");
    expect(ret.get(0)).toBe(trigger.get(0));
    expect(trigger.hasClass("jq-dropdown-disabled")).toBe(false);
    trigger.jqDropdown("show");
    expect(trigger.hasClass("jq-dropdown-open")).toBe(true);
  });

  it("toggles through document-click delegation", () => {
    const { trigger } = buildDropdown();
    trigger.trigger("click");
    expect(trigger.hasClass("jq-dropdown-open")).toBe(true);
    $(document).trigger("click");
    expect(trigger.hasClass("jq-dropdown-open")).toBe(false);
  });

  it("ships the jq-dropdown CSS classes in the npm dist", () => {
    const css = readFileSync(join(NPM_ROOT, "jquery.dropdown.css"), "utf8");
    for (const cls of [
      ".jq-dropdown",
      ".jq-dropdown-menu",
      ".jq-dropdown-panel",
      ".jq-dropdown-tip",
      ".jq-dropdown-divider",
    ]) {
      expect(css, `npm dropdown css lost ${cls}`).toContain(cls);
    }
  });
});
