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
 * Regression tests for PercDataTable DOM construction (js/html-constructed-from-input).
 *
 * Pre-fix code built &lt;td&gt;/&lt;th&gt; via HTML string concatenation of header
 * classes and cell indices (CodeQL #1575–#1580). Post-fix uses jQuery
 * element constructors + .addClass()/.attr()/.text() only.
 *
 * Strategy: load the real source with jQuery + a minimal DataTables stub so
 * $.fn.PercDataTable can run without the full plugin.
 */

import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import { describe, it, expect, beforeEach, afterEach } from "vitest";
import jquery from "jquery";

const __dirname = dirname(fileURLToPath(import.meta.url));
const SRC = resolve(
  __dirname,
  "../../main/webapp/cm/widgets/PercDataTable/PercDataTable.js",
);
const WRONG_SRC = resolve(
  __dirname,
  "../../main/webapp/cm/widgets/PercDataTableWrong/PercDataTable.js",
);

function loadPercDataTable($) {
  // Minimal DataTables stub — PercDataTable only needs .DataTable(config)
  $.fn.DataTable = function () {
    return this;
  };
  $.fn.dataTable = function () {
    return this;
  };
  const src = readFileSync(SRC, "utf8");
  // File is an IIFE that expects global jQuery as $
  // eslint-disable-next-line no-new-func
  const run = new Function(
    "jQuery",
    "window",
    "document",
    src + "\n; return jQuery.fn.PercDataTable;",
  );
  run($, globalThis, globalThis.document);
  return $.fn.PercDataTable;
}

function loadPercDataTableWrong($) {
  $.fn.dataTable = function () {
    return this;
  };
  $.fn.dataTableExt = $.fn.dataTableExt || {};
  $.fn.dataTableExt.afnSortData = $.fn.dataTableExt.afnSortData || {};
  $.browser = $.browser || { msie: false, mozilla: false };
  const src = readFileSync(WRONG_SRC, "utf8");
  // eslint-disable-next-line no-new-func
  const run = new Function(
    "jQuery",
    "window",
    "document",
    "gadgets",
    src + "\n; return jQuery.fn.percDataTable;",
  );
  run($, globalThis, globalThis.document, undefined);
  return $.fn.percDataTable;
}

describe("PercDataTable DOM construction safety", () => {
  let $;
  let host;

  beforeEach(() => {
    let jq = jquery(globalThis.window);
    if (typeof jq !== "function") {
      jq = typeof jquery === "function" ? jquery : jquery.fn || jquery;
    }
    $ = jq;
    globalThis.jQuery = $;
    globalThis.$ = $;
    host = $("<div id='perc-dt-host'>").appendTo(document.body);
  });

  afterEach(() => {
    host.remove();
    delete globalThis.jQuery;
    delete globalThis.$;
  });

  it("builds cells without interpreting hostile class tokens as HTML", () => {
    loadPercDataTable($);
    const hostileHeader = 'x" onclick=alert(1) x="';
    const config = {
      percExpandParentFrameVertically: false,
      bPaginate: false,
      bSort: false,
      percHeaders: [hostileHeader, "Col2"],
      aoColumns: [{ sType: "string" }, { sType: "string" }],
      percData: [
        {
          rowContent: ["safe-a", "safe-b"],
          rowData: {},
        },
      ],
    };

    host.PercDataTable(config);

    // No injected attributes from class string breakout
    const tds = host.find("td");
    expect(tds.length).toBeGreaterThan(0);
    tds.each(function () {
      expect(this.getAttribute("onclick")).toBeNull();
    });
    // Cell text still present
    expect(host.text()).toContain("safe-a");
    expect(host.text()).toContain("safe-b");
  });

  it("sets title attributes via DOM API rather than attribute HTML concat", () => {
    loadPercDataTable($);
    const config = {
      percExpandParentFrameVertically: false,
      bPaginate: false,
      bSort: false,
      percHeaders: ["H1"],
      aoColumns: [{ sType: "string" }],
      percData: [
        {
          rowContent: [
            { content: "Body", title: "Title with 'quotes\" & more" },
          ],
          rowData: {},
        },
      ],
    };
    host.PercDataTable(config);
    const titled = host.find("[title]").filter(function () {
      return (
        $(this).attr("title") &&
        $(this).attr("title").indexOf("Title with") === 0
      );
    });
    expect(titled.length).toBeGreaterThan(0);
    expect(titled.first().attr("title")).toContain("Title with");
  });

  it("PercDataTableWrong headers use text nodes for labels", () => {
    loadPercDataTableWrong($);
    const config = {
      percHeaders: ["<img src=x onerror=alert(1)>", "OK"],
      percColsLeft: [0, 1],
      percColsRight: [0, 1],
      percData: [["a", "b"]],
      percHeaderClasses: ["h0", "h1"],
      oLanguage: {},
    };
    host.percDataTable(config);
    // Header label must be text, not a live img element
    expect(host.find("th img").length).toBe(0);
    expect(host.find("th").first().text()).toContain("<img");
  });
});
