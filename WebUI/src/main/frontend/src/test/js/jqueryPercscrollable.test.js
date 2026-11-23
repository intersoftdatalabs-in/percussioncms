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
 * Behavioral pins for the first-party `jquery.percscrollable.js` carousel
 * shim (issue #7181, parent audit #5627).
 *
 * The shim replaces the retired jQuery Tools Scrollable 1.1.2 with the
 * same `$.fn.scrollable` API consumed by the classic template dialogs
 * (PercNewAssetDialog, PercNewPageDialog, PercChangeTemplateDialog,
 * perc_newSectionDialog, PercScrollingTemplateBrowser,
 * PercSiteTemplatesWidget): init options, prev/next/page anchors,
 * `.data("scrollable")` with begin/seekTo/updateItemWrap/getConf, and the
 * runtime `getConf().size` mutation the site-templates widget relies on.
 * Every expectation below mirrors the retired implementation's math
 * (including its end-clamp and movePage remainder handling) so the
 * replacement cannot silently change paging behavior on those dialogs.
 */

import { readFileSync, existsSync } from "fs";
import { resolve, dirname, join } from "path";
import { fileURLToPath } from "url";
import jquery from "jquery";
import { describe, it, expect, beforeEach } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));
const FRONTEND_ROOT = dirname(dirname(dirname(__dirname)));
const WEBAPP_CM = resolve(FRONTEND_ROOT, "..", "webapp", "cm");
const WAR_ROOT = resolve(FRONTEND_ROOT, "..", "..", "..", "war");

const SHIM_CANONICAL = join(
  WEBAPP_CM,
  "jslib/profiles/3x/jquery/plugins/jquery-percutils/jquery.percscrollable.js",
);

let $;

function loadShim() {
  let jq = jquery(globalThis.window);
  if (typeof jq !== "function") {
    jq = typeof jquery === "function" ? jquery : jquery.fn || jquery;
    if (!jq.fn && jq.prototype) jq.fn = jq.prototype;
  }
  if (!jq.fn) throw new Error("jquery has no .fn");
  $ = jq;
  globalThis.jQuery = $;
  globalThis.$ = $;

  const src = readFileSync(SHIM_CANONICAL, "utf8");
  const run = new Function("window", "jQuery", "$", src);
  run(globalThis.window, $, $);
}

function buildCarousel(itemCount = 10) {
  document.body.innerHTML =
    `<div id="parent">` +
    `<a class="prev browse left"></a>` +
    `<a class="next browse right"></a>` +
    `<div class="perc-scrollable"><div class="perc-items">` +
    Array.from(
      { length: itemCount },
      (_, i) => `<div class="item" id="item-${i}"></div>`,
    ).join("") +
    `</div></div>` +
    `</div>`;
  return $("div.perc-scrollable");
}

beforeEach(() => {
  loadShim();
});

describe("jquery.percscrollable first-party shim (issue #7181)", () => {
  it("ships byte-identical in all three trees", () => {
    const canonical = readFileSync(SHIM_CANONICAL);
    for (const mirror of [
      join(
        WEBAPP_CM,
        "vendor/js/legacy/profiles/3x/jquery/plugins/jquery-percutils/jquery.percscrollable.js",
      ),
      join(
        WAR_ROOT,
        "jslib/profiles/3x/jquery/plugins/jquery-percutils/jquery.percscrollable.js",
      ),
    ]) {
      expect(existsSync(mirror), `missing shim mirror: ${mirror}`).toBe(true);
      expect(readFileSync(mirror).equals(canonical)).toBe(true);
    }
  });

  it("exposes $.fn.scrollable and returns the chainable set on init", () => {
    expect(typeof $.fn.scrollable).toBe("function");
    const root = buildCarousel();
    const ret = root.scrollable({ items: ".perc-items", size: 4 });
    expect(ret.get(0)).toBe(root.get(0));
  });

  it("returns the same API on repeat and no-arg calls", () => {
    const root = buildCarousel();
    root.scrollable({ items: ".perc-items", size: 4 });
    const viaData = root.data("scrollable");
    expect(viaData).toBeDefined();
    expect(root.scrollable()).toBe(viaData);
    expect(root.scrollable(0)).toBe(viaData);
    expect(viaData.getIndex()).toBe(0);
    expect(viaData.getSize()).toBe(10);
    expect(viaData.getConf().size).toBe(4);
    expect(viaData.getConf().keyboardSteps).toBe(4);
  });

  it("accepts a jQuery object for the items option", () => {
    const root = buildCarousel();
    const api = root
      .scrollable({ items: root.find(".perc-items"), size: 4 })
      .scrollable();
    expect(api.getSize()).toBe(10);
  });

  it("seeks, clamps, begins, and ends like the retired plugin", () => {
    const root = buildCarousel();
    const api = root
      .scrollable({ items: ".perc-items", size: 4, keyboard: false })
      .scrollable();
    api.seekTo(2, 0);
    expect(api.getIndex()).toBe(2);
    api.seekTo(-5, 0);
    expect(api.getIndex()).toBe(0);
    // 10 items, size 4: seeking past 10 - 4 lands on the end.
    api.seekTo(9, 0);
    expect(api.getIndex()).toBe(6);
    api.begin(0);
    expect(api.getIndex()).toBe(0);
    api.end(0);
    expect(api.getIndex()).toBe(6);
    api.next(0);
    expect(api.getIndex()).toBe(6);
    api.prev(0);
    expect(api.getIndex()).toBe(5);
  });

  it("pages with the retired movePage remainder math", () => {
    const root = buildCarousel();
    const api = root
      .scrollable({ items: ".perc-items", size: 4, keyboard: false })
      .scrollable();
    api.nextPage(0);
    expect(api.getIndex()).toBe(4);
    // index 4 is page-aligned: a full page would overshoot the end (10 - 4),
    // so the plugin holds the end position.
    api.nextPage(0);
    expect(api.getIndex()).toBe(6);
    api.prevPage(0);
    expect(api.getIndex()).toBe(4);
    api.prevPage(0);
    expect(api.getIndex()).toBe(0);
  });

  it("reads a mutated getConf().size on subsequent seeks", () => {
    // PercSiteTemplatesWidget.updateTemplatesPagingSize mutates size at
    // runtime; the next seek must honor the new page width.
    const root = buildCarousel();
    const api = root
      .scrollable({ items: ".perc-items", size: 4, keyboard: false })
      .scrollable();
    api.getConf().size = 2;
    api.seekTo(8, 0);
    expect(api.getIndex()).toBe(8);
    api.nextPage(0);
    expect(api.getIndex()).toBe(8);
  });

  it("toggles the disabled class on nav anchors at the ends", () => {
    const root = buildCarousel();
    const api = root
      .scrollable({ items: ".perc-items", size: 4, keyboard: false })
      .scrollable();
    expect($("a.prev").hasClass("disabled")).toBe(true);
    expect($("a.next").hasClass("disabled")).toBe(false);
    api.seekTo(2, 0);
    expect($("a.prev").hasClass("disabled")).toBe(false);
    expect($("a.next").hasClass("disabled")).toBe(false);
    api.end(0);
    expect($("a.next").hasClass("disabled")).toBe(true);
  });

  it("wires sibling prev/next anchors to move the carousel", () => {
    const root = buildCarousel();
    const api = root
      .scrollable({ items: ".perc-items", size: 4, keyboard: false })
      .scrollable();
    $("a.next").click();
    expect(api.getIndex()).toBe(1);
    $("a.prev").click();
    expect(api.getIndex()).toBe(0);
  });

  it("re-resolves the item wrap on updateItemWrap", () => {
    const root = buildCarousel();
    const api = root
      .scrollable({ items: ".perc-items", size: 4, keyboard: false })
      .scrollable();
    expect(api.getSize()).toBe(10);
    root.find(".perc-items").append('<div class="item" id="item-10"></div>');
    api.updateItemWrap();
    expect(api.getSize()).toBe(11);
  });
});
