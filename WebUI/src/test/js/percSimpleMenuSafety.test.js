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
 * Regression for PercSimpleMenu js/unsafe-jquery-plugin (#1681 / #434).
 *
 * Pre-fix: $(menuLabels[ml]) passed label strings into jQuery's HTML-sniffing
 * constructor. Post-fix: resolveMenuLabel never does that for plain/hostile
 * strings.
 */

import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import { describe, it, expect, beforeEach, afterEach } from "vitest";
import jquery from "jquery";

const __dirname = dirname(fileURLToPath(import.meta.url));
const SRC = resolve(
  __dirname,
  "../../main/webapp/cm/widgets/PercSimpleMenu.js"
);

function loadPlugin($) {
  $.browser = $.browser || { msie: false, mozilla: false };
  const src = readFileSync(SRC, "utf8");
  // eslint-disable-next-line no-new-func
  const run = new Function("jQuery", "window", "document", src);
  run($, globalThis, globalThis.document);
  return $.fn.percSimpleMenu;
}

describe("percSimpleMenu label resolution (js/unsafe-jquery-plugin)", () => {
  let $;
  let host;
  let rawCalls;

  beforeEach(() => {
    let jq = jquery(globalThis.window);
    if (typeof jq !== "function") {
      jq = typeof jquery === "function" ? jquery : jquery.fn || jquery;
    }
    rawCalls = [];
    function spy(arg) {
      rawCalls.push(arg);
      return jq.apply(this, arguments);
    }
    Object.assign(spy, jq);
    spy.fn = jq.fn;
    $ = spy;
    globalThis.jQuery = spy;
    globalThis.$ = spy;
    host = $("<div id='menu-host'>").appendTo(document.body);
    loadPlugin($);
  });

  afterEach(() => {
    if (host) host.remove();
    delete globalThis.jQuery;
    delete globalThis.$;
  });

  it("never hands a hostile HTML string to the jQuery constructor", () => {
    const payload = '<img src=x onerror="window.__menu_pwned=1">';
    host.percSimpleMenu({
      menuLabels: [payload, "Edit"],
      callbacks: [function () {}, function () {}],
      callbackData: [{}, {}],
      menuTitleCollapsed: "[+]",
      menuTitleExpanded: "[-]",
    });

    // Hostile markup must not become a live img
    expect(host.find("img").length).toBe(0);
    expect(globalThis.__menu_pwned).toBeUndefined();
    // Label text is still visible (escaped as text)
    expect(host.text()).toContain("img");
    // $() was never called with the raw payload string as sole argument
    const hostileCtor = rawCalls.filter(
      (a) => typeof a === "string" && a.indexOf("onerror") !== -1
    );
    expect(hostileCtor.length).toBe(0);
  });

  it("builds plain text labels as text nodes", () => {
    host.percSimpleMenu({
      menuLabels: ["Preview", "Delete"],
      callbacks: [function () {}, function () {}],
      callbackData: ["a", "b"],
      menuTitleCollapsed: "Menu",
      menuTitleExpanded: "Menu",
    });
    expect(host.text()).toContain("Preview");
    expect(host.text()).toContain("Delete");
    expect(host.find(".perc-simplemenu-menuitem").length).toBe(2);
  });

  it("creates a real anchor for simple first-party <a> labels without $(html)", () => {
    host.percSimpleMenu({
      menuLabels: ["<a>Export</a>"],
      callbacks: [function () {}],
      callbackData: [
        { formSummary: { totalSubmissions: 0, name: "f1" } },
      ],
      menuTitleCollapsed: "[+]",
      menuTitleExpanded: "[-]",
    });
    const a = host.find("a");
    expect(a.length).toBe(1);
    expect(a.text()).toBe("Export");
    expect(host.find("a img").length).toBe(0);
  });
});
