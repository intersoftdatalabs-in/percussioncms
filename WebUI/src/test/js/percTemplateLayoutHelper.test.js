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
 * Regression tests for
 * WebUI/src/main/webapp/cm/plugins/perc_template_layout_helper.js
 *
 * Closes GitHub CodeQL alerts (js/xss-through-dom) flagged on
 * `parseRegions()`:
 *
 *   var $templateCode = $($children.find("code templateCode:first").text());
 *   newRegion.setVertical(!$templateCode.hasClass("perc-horizontal"));
 *
 * The `templateCode` text comes from the Template object's XML
 * (`regionTree ... code templateCode`), which is CMS/server content that a
 * template author can influence. Pre-fix code hands that raw string
 * straight to jQuery's `$()` constructor purely to answer a `.hasClass()`
 * question — `$()` parses the string as HTML, which is exactly the
 * "DOM text reinterpreted as HTML" sink the rule flags.
 *
 * Post-fix code answers the same question with a plain regex against the
 * markup text and never hands the string to an HTML parser.
 *
 * Test strategy (Constitution III fail-then-pass):
 * - Spy on the global `$`/`jQuery` entry point used by the module under
 *   test (the source is loaded via `readFileSync` + `eval`, same pattern
 *   as the other WidgetBuilder/UserView regression tests in this
 *   directory) and record every string passed to it.
 * - Call `parseRegions()` with an XML region whose `templateCode` text
 *   contains an XSS marker plus a `perc-horizontal` class.
 * - Assert the marker string is never handed to `$()` (fails on pre-fix
 *   code, which calls `$(templateCodeText)`), and that the region's
 *   vertical/horizontal classification is still computed correctly.
 */

import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import jquery from "jquery";
import { beforeEach, afterEach, describe, it, expect } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));
const SRC_PATH = resolve(
  __dirname,
  "../../main/webapp/cm/plugins/perc_template_layout_helper.js",
);

const XSS_MARKER = 'onerror="window.__perc_template_layout_pwned__=true"';

let realJQ;
let rawCalls;
let PercTemplateLayoutHelper;

function makeXmlRegion(templateCodeMarkup) {
  const xmlString =
    "<region>" +
    "<regionId>test-region</regionId>" +
    "<children>" +
    "<code><templateCode><![CDATA[" +
    templateCodeMarkup +
    "]]></templateCode></code>" +
    "</children>" +
    "</region>";
  const xmlDoc = realJQ.parseXML(xmlString);
  return realJQ(xmlDoc).find("region");
}

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

  // Minimal stand-in for $.Perc_Region_class (defined in the sibling
  // perc_template_layout_class.js, not loaded by this test) — just enough
  // to record what the module under test does with it.
  spy.Perc_Region_class = function (regionid) {
    this.regionid = regionid;
    this.vertical = null;
    this.setVertical = function (v) {
      this.vertical = v;
    };
    this.addSubRegion = function () {};
    this.hasSubRegions = function () {
      return false;
    };
  };

  globalThis.jQuery = spy;
  globalThis.$ = spy;
}

beforeEach(() => {
  loadSource();
  const src = readFileSync(SRC_PATH, "utf8");
  // eslint-disable-next-line no-eval
  eval(src);
  PercTemplateLayoutHelper = globalThis.jQuery.perc_template_layout_helper;
  globalThis.__perc_template_layout_pwned__ = undefined;
});

afterEach(() => {
  delete globalThis.__perc_template_layout_pwned__;
});

describe("perc_template_layout_helper parseRegions XSS regression", () => {
  it("never hands untrusted templateCode markup to the jQuery HTML parser", () => {
    const payload = `<div class="perc-horizontal"><img src=x ${XSS_MARKER}></div>`;
    const $region = makeXmlRegion(payload);

    const helper = new PercTemplateLayoutHelper();
    helper.parseRegions($region, null);

    const sawRawMarkup = rawCalls.some(
      (call) => typeof call === "string" && call.includes(XSS_MARKER),
    );
    expect(sawRawMarkup).toBe(false);
  });

  it("still classifies a perc-horizontal region correctly", () => {
    const payload = `<div class="perc-horizontal"><img src=x ${XSS_MARKER}></div>`;
    const $region = makeXmlRegion(payload);

    const helper = new PercTemplateLayoutHelper();
    helper.parseRegions($region, null);

    // setVertical(!hasClass) -> hasClass true -> setVertical(false)
    expect(helper.rootRegion.vertical).toBe(false);
  });

  it("still classifies a vertical (non-horizontal) region correctly", () => {
    const payload = `<div class="some-other-class"><img src=x ${XSS_MARKER}></div>`;
    const $region = makeXmlRegion(payload);

    const helper = new PercTemplateLayoutHelper();
    helper.parseRegions($region, null);

    // setVertical(!hasClass) -> hasClass false -> setVertical(true)
    expect(helper.rootRegion.vertical).toBe(true);
  });

  it("only inspects the root element, matching $(markup).hasClass() semantics", () => {
    // PR #1320 review: a nested element carrying the class (with no
    // class attribute on the root) must not be treated as horizontal —
    // that would diverge from jQuery's hasClass(), which only ever
    // inspects the root element.
    const payload = `<div><span class="perc-horizontal">${XSS_MARKER}</span></div>`;
    const $region = makeXmlRegion(payload);

    const helper = new PercTemplateLayoutHelper();
    helper.parseRegions($region, null);

    // setVertical(!hasClass) -> hasClass false (nested, not root) -> setVertical(true)
    expect(helper.rootRegion.vertical).toBe(true);
  });

  it("matches a single-quoted class attribute on the root element", () => {
    // PR #1320 review: single-quoted class='...' attributes must be
    // recognized the same way jQuery's hasClass() recognizes them.
    const payload = `<div class='perc-horizontal'><img src=x ${XSS_MARKER}></div>`;
    const $region = makeXmlRegion(payload);

    const helper = new PercTemplateLayoutHelper();
    helper.parseRegions($region, null);

    // setVertical(!hasClass) -> hasClass true -> setVertical(false)
    expect(helper.rootRegion.vertical).toBe(false);
  });
});
