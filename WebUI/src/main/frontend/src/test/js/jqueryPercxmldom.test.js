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
 * Behavioral pins for the first-party `jquery.percxmldom.js` shim
 * (issue #7180, parent audit #5627).
 *
 * The shim replaces the retired jQuery xmlDOM Plugin v1.0 (Out West Media,
 * 2009) with the same `$.xmlDOM(xml)` / `$.xmlDOM(xml, onErrorFn)` API
 * consumed by the classic template editing surfaces (PercPageModel,
 * PercTemplateModel, perc_template_layout_class,
 * perc_template_layout_helper, perc_content_viewer): native DOMParser
 * parsing, `<parsererror>` rejection, the `onErrorFn` / `xmlParseError`
 * error contract, and the jQuery-wrapped document return (including the
 * `.context.documentElement` read the layout callers depend on).
 * Every expectation below mirrors the retired implementation's branches
 * so the replacement cannot silently change template/page parsing on
 * those dialogs.
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
  "jslib/profiles/3x/jquery/plugins/jquery-percutils/jquery.percxmldom.js",
);

function loadShim() {
  // Same loading dance as the #7181 percscrollable pins: bind the npm
  // jQuery to the jsdom window, publish it as the globals the shim's
  // `(function ($) {...})(jQuery)` wrapper expects, and run the shim
  // source so the parsed-document, parsererror, and xmlParseError branches
  // behave as they do on a real page.
  let jq = jquery(globalThis.window);
  if (typeof jq !== "function") {
    jq = typeof jquery === "function" ? jquery : jquery.fn || jquery;
    if (!jq.fn && jq.prototype) jq.fn = jq.prototype;
  }
  if (!jq.fn) throw new Error("jquery has no .fn");
  const $ = jq;
  globalThis.jQuery = $;
  globalThis.$ = $;

  const src = readFileSync(SHIM_CANONICAL, "utf8");
  const run = new Function("window", "jQuery", "$", src);
  run(globalThis.window, $, $);
  return $;
}

const PAGE_XML =
  `<Page><templateId>42</templateId>` +
  `<regionTree><rootRegion><regionId>r1</regionId></rootRegion></regionTree></Page>`;

let $;

beforeEach(() => {
  document.body.innerHTML = "";
  $ = loadShim();
});

describe("jquery.percxmldom first-party shim (issue #7180)", () => {
  it("ships byte-identical in all three trees", () => {
    const canonical = readFileSync(SHIM_CANONICAL);
    for (const mirror of [
      join(
        WEBAPP_CM,
        "vendor/js/legacy/profiles/3x/jquery/plugins/jquery-percutils/jquery.percxmldom.js",
      ),
      join(
        WAR_ROOT,
        "jslib/profiles/3x/jquery/plugins/jquery-percutils/jquery.percxmldom.js",
      ),
    ]) {
      expect(existsSync(mirror), `missing shim mirror: ${mirror}`).toBe(true);
      expect(readFileSync(mirror).equals(canonical)).toBe(true);
    }
  });

  it("exposes $.xmlDOM as a function", () => {
    expect(typeof $.xmlDOM).toBe("function");
  });

  it("parses XML and exposes elements via find (PercPageModel shape)", () => {
    const $doc = $.xmlDOM(PAGE_XML);
    expect($doc.find("templateId").text()).toBe("42");
    expect($doc.find("rootRegion > regionId").text()).toBe("r1");
  });

  it("returns the jQuery-wrapped document with .context.documentElement (layout callers)", () => {
    const $doc = $.xmlDOM(PAGE_XML);
    expect($doc.get(0)).toBe($doc.context);
    expect($doc.context.documentElement.nodeName).toBe("Page");
    // The exact read perc_template_layout_class performs.
    const element = $doc.context.documentElement;
    expect(element.getElementsByTagName("templateId")[0].textContent).toBe(
      "42",
    );
  });

  it("supports the .find().each() walk perc_content_viewer performs", () => {
    const xml =
      `<Template><rootRegion><children><region><regionId>a</regionId>` +
      `</region><region><regionId>b</regionId></region></children></rootRegion>` +
      `<widgetItem><id>w1</id></widgetItem></Template>`;
    const seen = [];
    $.xmlDOM(xml)
      .find("widgetItem")
      .each(function () {
        seen.push($(this).find("id").text());
      });
    expect(seen).toEqual(["w1"]);
  });

  it("reports malformed XML through onErrorFn and returns an empty set", () => {
    let reported = null;
    const $doc = $.xmlDOM("<Page><unclosed>", (msg) => {
      reported = msg;
    });
    expect($doc.length).toBe(0);
    expect(typeof reported === "string" && reported.length > 0).toBe(true);
  });

  it("triggers xmlParseError on document when no callback is given", () => {
    let triggered = null;
    $(document).on("xmlParseError.test7180", (_evt, msg) => {
      triggered = msg;
    });
    try {
      const $doc = $.xmlDOM("<Page><unclosed>");
      expect($doc.length).toBe(0);
      expect(typeof triggered === "string" && triggered.length > 0).toBe(true);
    } finally {
      $(document).off("xmlParseError.test7180");
    }
  });

  it("rejects non-XML input through the error contract", () => {
    let reported = null;
    const $doc = $.xmlDOM("just some text", (msg) => {
      reported = msg;
    });
    expect($doc.length).toBe(0);
    expect(typeof reported === "string" && reported.length > 0).toBe(true);
  });
});
