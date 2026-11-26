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
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Behavioral parity for the retired bassistance-era form plugins
 * (issue #7178, parent audit #5627).
 *
 * The vendored copies are deleted from all three trees (cm/jslib,
 * cm/vendor/js/legacy, WebUI/war — pinned absent + unreferenced by the
 * RETIRED_7178 block in jslibVendoredAudit.test.js). This file pins the
 * replacement behavior the migrated call sites rely on:
 *
 *   - jQuery UI 1.14 (already npm-tracked, loaded by common_js.jsp) owns
 *     $.fn.tooltip / $.fn.autocomplete now: the widget APIs used by the
 *     ported finder/publish tooltips and list-editor/gadgets/tag-list
 *     autocompletes behave (items/content/show-delay/position/open for
 *     tooltips; source/minLength/search/select/option-source for
 *     autocompletes, including the multi-value source-function pattern).
 *   - The bassistance namespaces are gone: $.tooltip, $.Autocompleter,
 *     $.fn.caret, $.fn.textOverflow, $.metadata are all undefined.
 *   - The first-party caret/text-overflow replacements in
 *     jquery.percutils.js behave: $.PercTextOverflow sets the CSS ellipsis
 *     props (no plugin), and $.perc_filterFieldText keeps caret
 *     positioning via selectionStart/setSelectionRange.
 *   - The npm print-this 2.0.0 dist (same jasonday lineage as the deleted
 *     bannerless copy) exposes $.fn.printThis with the v1-compatible
 *     default path the zero-options PercSiteSummaryDialog call rides.
 *   - Static pins tie the ported call-site sources to exactly these API
 *     shapes, so a future edit that drifts (e.g. reintroduces a
 *     bassistance-only option) fails here.
 */

import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import jquery from "jquery";
import { describe, it, expect, vi, beforeEach } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));
const FRONTEND_ROOT = dirname(dirname(dirname(__dirname)));
const WEBAPP_CM = join(FRONTEND_ROOT, "..", "webapp", "cm");
const NPM_ROOT = join(FRONTEND_ROOT, "node_modules");
const PACKAGES_ROOT = join(
  FRONTEND_ROOT,
  "..",
  "..",
  "..",
  "..",
  "modules",
  "perc-packages",
  "src",
  "main",
  "resources",
  "Packages",
);

function loadPlugins() {
  let jq = jquery(globalThis.window);
  if (typeof jq !== "function") {
    jq = typeof jquery === "function" ? jquery : jquery.fn || jquery;
    if (!jq.fn && jq.prototype) jq.fn = jq.prototype;
  }
  if (!jq.fn) throw new Error("jquery has no .fn");
  globalThis.jQuery = jq;
  globalThis.$ = jq;

  // Production order on classic pages: core -> migrate -> jQuery UI.
  // jquery-migrate restores the $.isFunction the printThis v2 dist uses.
  for (const file of [
    join(NPM_ROOT, "jquery-migrate/dist/jquery-migrate.js"),
    join(NPM_ROOT, "jquery-ui/dist/jquery-ui.js"),
    join(NPM_ROOT, "print-this/printThis.js"),
  ]) {
    const src = readFileSync(file, "utf8");
    const run = new Function("window", "jQuery", "$", src);
    run(globalThis.window, jq, jq);
  }
  return jq;
}

const $ = loadPlugins();

/** Load the real jquery.percutils.js with its load-time deps stubbed. */
function loadPercutils() {
  // percutils derives $.browser from bowser at load; all-false keeps the
  // helpers on their modern path.
  globalThis.bowser = { getParser: () => ({ isBrowser: () => false }) };
  // The $(document).ready block registers a jeditable input type.
  $.editable = { addInputType() {} };
  const src = readFileSync(
    join(
      WEBAPP_CM,
      "jslib/profiles/3x/jquery/plugins/jquery-percutils/jquery.percutils.js",
    ),
    "utf8",
  );
  const run = new Function("window", "jQuery", "$", "bowser", src);
  run(globalThis.window, $, $, globalThis.bowser);
}

loadPercutils();

beforeEach(() => {
  document.body.innerHTML = "";
});

describe("jQuery UI owns tooltip/autocomplete after the bassistance retirement (issue #7178)", () => {
  it("exposes the widget namespaces and drops the bassistance ones", () => {
    expect(typeof $.ui).toBe("object");
    expect(typeof $.ui.tooltip).toBe("function");
    expect(typeof $.ui.autocomplete).toBe("function");
    expect(typeof $.fn.tooltip).toBe("function");
    expect(typeof $.fn.autocomplete).toBe("function");
    // Bassistance-only namespaces must not come back via any load.
    expect($.tooltip, "bassistance $.tooltip resurrected").toBeUndefined();
    expect($.Autocompleter, "bassistance $.Autocompleter resurrected").toBeUndefined();
    expect($.fn.caret, "retired $.fn.caret resurrected").toBeUndefined();
    expect($.fn.textOverflow, "retired $.fn.textOverflow resurrected").toBeUndefined();
    expect($.metadata, "retired $.metadata resurrected").toBeUndefined();
  });

  it("opens a finder-style tooltip with the ported option shape", async () => {
    vi.useFakeTimers();
    try {
      document.body.innerHTML =
        `<a style="float: right;" tooltip="some comment" class="perc-last-comment-menubar">` +
        `<span class="perc-font-icon icon-comment"></span></a>`;
      const commentIcon = $(".perc-last-comment-menubar");
      let opened = null;
      commentIcon.tooltip({
        items: "a.perc-last-comment-menubar",
        content: () => "<p style='padding:5px 20px 5px 20px;'>some comment</p>",
        show: { delay: 500 },
        position: {
          my: "right top",
          at: "right top",
          offset: "-150 25",
          collision: "flipfit",
        },
        open: function (event, ui) {
          opened = ui.tooltip;
          ui.tooltip.css("margin-right", "20px");
        },
      });
      commentIcon.find("span").trigger("mouseover");
      await vi.advanceTimersByTimeAsync(600);
      const tip = $(".ui-tooltip");
      expect(tip.length, "jQuery UI tooltip bubble never opened").toBe(1);
      expect(tip.find(".ui-tooltip-content").html()).toContain("some comment");
      expect(opened && opened.length).toBe(1);
      expect(opened.css("margin-right")).toBe("20px");
    } finally {
      vi.useRealTimers();
    }
  });

  it("drives a list-editor-style autocomplete: search, select, source swap", () => {
    document.body.innerHTML = `<input id="ac" type="text" />`;
    const inputField = $("#ac");
    let selected = 0;
    inputField
      .autocomplete({
        source: ["Alpha", "Beta"],
        minLength: 0,
        open: function () {
          inputField
            .autocomplete("widget")
            .css({ width: "334px", "max-height": "101px", "overflow-y": "auto" });
        },
      })
      .on("focus", function () {
        inputField.autocomplete("search", "");
      })
      // Production assigns the value first: autocompleteselect fires before
      // jQuery UI inserts the selection (the retired result fired after).
      .on("autocompleteselect", function (event, ui) {
        inputField.val(ui.item.value);
        selected += 1;
      });
    // minLength: 0 + focus search shows the whole list (minChars: 0 parity).
    inputField.trigger("focus");
    let items = $(".ui-autocomplete .ui-menu-item");
    expect(items.length, "autocomplete menu did not open on focus search").toBe(2);
    expect($(".ui-autocomplete").css("max-height")).toBe("101px");
    // Partial text + row click inserts the full selection, then selects.
    inputField.val("Al");
    inputField.autocomplete("search", "Al");
    $(".ui-autocomplete .ui-menu-item").first().trigger("click");
    expect(selected, "autocompleteselect did not fire on row click").toBe(1);
    expect(inputField.val(), "selection was not inserted before select").toBe("Alpha");
    // option("source", ...) replaces the data (bassistance setOptions parity).
    inputField.autocomplete("option", "source", ["Gamma"]);
    inputField.autocomplete("search", "");
    items = $(".ui-autocomplete .ui-menu-item");
    expect(items.length, "source swap did not take").toBe(1);
    expect(items.first().text()).toContain("Gamma");
  });

  it("supports the tag-list multi-value source-function pattern", () => {
    document.body.innerHTML = `<input id="tags" type="text" />`;
    const field = $("#tags");
    const split = (val) => val.split(/,\s*/);
    const extractLast = (term) => split(term).pop();
    field.autocomplete({
      minLength: 1,
      autoFocus: false,
      source: function (request, response) {
        response($.ui.autocomplete.filter(["red", "green", "blue"], extractLast(request.term)));
      },
      focus: () => false,
      select: function (event, ui) {
        const terms = split(this.value);
        terms.pop();
        terms.push(ui.item.value);
        terms.push("");
        this.value = terms.join(", ");
        return false;
      },
    });
    field.val("red, gr");
    field.autocomplete("search", "red, gr");
    const items = $(".ui-autocomplete .ui-menu-item");
    expect(items.length, "multi-value filter did not narrow to green").toBe(1);
    expect(items.first().text()).toContain("green");
    items.first().trigger("click");
    expect(field.val(), "multi-value select did not join the tag").toBe("red, green, ");
  });
});

describe("first-party caret/text-overflow replacements in jquery.percutils.js (issue #7178)", () => {
  it("truncates via CSS only, with no plugin call", () => {
    document.body.innerHTML = `<div class="perc-text-overflow">label</div>`;
    const element = $(".perc-text-overflow");
    $.PercTextOverflow(element, 122);
    expect(element.css("white-space")).toBe("nowrap");
    expect(element.css("text-overflow")).toBe("ellipsis");
    expect(element.css("overflow")).toBe("hidden");
    expect(element.css("width")).toBe("122px");
  });

  it("rejects a filtered char without moving the caret", () => {
    document.body.innerHTML = `<input id="f" type="text" value="12" />`;
    const tgt = $("#f");
    $.perc_filterFieldText(tgt, () => "");
    const field = tgt.get(0);
    field.setSelectionRange(1, 1);
    const evt = $.Event("keypress", { charCode: 65, which: 65 });
    tgt.trigger(evt);
    expect(evt.isDefaultPrevented(), "rejected char was not prevented").toBe(true);
    expect(field.value, "rejected char leaked into the value").toBe("12");
  });

  it("replaces via the filter and retains caret position natively", () => {
    document.body.innerHTML = `<input id="g" type="text" value="axc" />`;
    const tgt = $("#g");
    $.perc_filterFieldText(tgt, (txt) => txt.replace(/a/g, "b"));
    const field = tgt.get(0);
    field.setSelectionRange(1, 1);
    const evt = $.Event("keypress", { charCode: 97, which: 97 });
    tgt.trigger(evt);
    expect(evt.isDefaultPrevented(), "replacement did not prevent default").toBe(true);
    expect(field.value, "filter replacement wrote the wrong value").toBe("bbxc");
    expect(field.selectionStart, "caret was not retained after replacement").toBe(2);
  });
});

describe("npm print-this 2.0.0 serves the site-summary print call (issue #7178)", () => {
  it("exposes $.fn.printThis with the v1-compatible default path", () => {
    expect(typeof $.fn.printThis).toBe("function");
    expect($.fn.printThis.defaults).toMatchObject({
      debug: false,
      importCSS: true,
      printContainer: true,
      loadCSS: "",
      pageTitle: "",
      removeInline: false,
    });
  });

  it("ships the jasonday MIT lineage in the npm dist", () => {
    const dist = readFileSync(join(NPM_ROOT, "print-this/printThis.js"), "utf8");
    expect(dist).toMatch(/printThis v2\.0\.0/);
    expect(dist).toMatch(/Jason Day/);
    expect(dist).toMatch(/MIT licence/);
    expect(typeof $.isFunction, "migrate must restore $.isFunction for the v2 dist").toBe(
      "function",
    );
  });
});

describe("ported call-site sources use only the replacement APIs (issue #7178)", () => {
  const FINDER = [
    "app/js/legacy/views/PercFinderView.js",
    "views/PercFinderView.js",
  ];
  const PUBLISH = [
    "app/js/legacy/views/PublishView.js",
    "views/PublishView.js",
  ];
  const LIST_EDITOR = [
    "app/js/legacy/plugins/PercListEditorWidget.js",
    "plugins/PercListEditorWidget.js",
  ];
  const GADGETS = [
    "app/js/legacy/plugins/perc_gadgets_search_criteria_dialog.js",
    "plugins/perc_gadgets_search_criteria_dialog.js",
  ];
  const read = (rel) => readFileSync(join(WEBAPP_CM, rel), "utf8");

  it("tooltips use the jQuery UI widget shape, not bassistance options", () => {
    for (const rel of [...FINDER, ...PUBLISH]) {
      const src = read(rel);
      for (const needle of [
        "items:",
        "content: function",
        "show: { delay: 500 }",
        ".tooltip({",
      ]) {
        expect(src, `${rel} lost ${needle}`).toContain(needle);
      }
      for (const banned of [
        "bodyHandler: function",
        "minChars:",
        ".result(function",
        '$("#tooltip")',
        "jquery.tooltip",
      ]) {
        expect(src, `${rel} still uses bassistance ${banned}`).not.toContain(banned);
      }
    }
  });

  it("autocompletes use the jQuery UI widget shape, not bassistance options", () => {
    for (const rel of [...LIST_EDITOR, ...GADGETS]) {
      const src = read(rel);
      for (const needle of [
        "source: resultItems",
        "minLength: 0",
        "autocompleteselect",
        'autocomplete("search", "")',
        'autocomplete("widget")',
      ]) {
        expect(src, `${rel} lost ${needle}`).toContain(needle);
      }
      for (const banned of [
        "minChars: 0,",
        ".result(function",
        "setOptions({",
        "scrollHeight:",
        "jquery.autocomplete",
      ]) {
        expect(src, `${rel} still uses bassistance ${banned}`).not.toContain(banned);
      }
    }
    for (const rel of LIST_EDITOR) {
      expect(read(rel), `${rel} lost the source-swap port`).toContain(
        'autocomplete("option", "source", resultItems)',
      );
    }
  });

  it("tag-list control ports autocomplete and caret without bassistance APIs", () => {
    const src = readFileSync(
      join(
        PACKAGES_ROOT,
        "perc.Baseline/SupportFile-rx_resources/js/percTagListControl.js",
      ),
      "utf8",
    );
    for (const needle of [
      "minLength: 1",
      "autoFocus: false",
      "$.ui.autocomplete.filter",
      "extractLast(request.term)",
      "selectionStart",
      "setSelectionRange",
    ]) {
      expect(src, `percTagListControl.js lost ${needle}`).toContain(needle);
    }
    for (const banned of ["minChars: 1,", ".result(", "$(field).caret(", ".autocomplete(percTagListSource"]) {
      expect(src, `percTagListControl.js still uses ${banned}`).not.toContain(banned);
    }
    // The control declares the npm-backed jQuery UI URL for its widget;
    // no per-URL autocomplete descriptors remain (404 by design).
    const xsl = readFileSync(
      join(
        PACKAGES_ROOT,
        "perc.Baseline/SupportFile-rx_resources/stylesheets/controls/percTagListControl.xsl",
      ),
      "utf8",
    );
    expect(xsl).not.toContain("jquery-perc-retiredjs/jquery.autocomplete");
    expect(xsl).toContain("jquery/libraries/jquery-ui/jquery-ui.min.js");
  });

  it("image dimension fields and percutils use native caret, not .caret()", () => {
    for (const rel of [
      "SupportFile-rx_resources/widgets/image/js/jquery.imageAssetControl.js",
      "sys__UserDependency--rx_resources/widgets/image/js/jquery.imageAssetControl.js",
    ]) {
      const src = readFileSync(
        join(PACKAGES_ROOT, "perc.widgets.image", rel),
        "utf8",
      );
      expect(src).toContain("selectionStart");
      expect(src).not.toContain(".caret(");
    }
    const percutils = read(
      "jslib/profiles/3x/jquery/plugins/jquery-percutils/jquery.percutils.js",
    );
    expect(percutils).toContain("percCaretStart");
    expect(percutils).toContain("setSelectionRange");
    expect(percutils).not.toContain(".caret(");
    expect(percutils).not.toContain(".textOverflow(");
    expect(percutils).toContain("text-overflow: ellipsis");
  });

  it("site map no longer touches the retired metadata plugin", () => {
    const src = read("widgets/perc_site_map.js");
    // No executable .metadata( call may remain (the #1097 docblock and the
    // #7178 removal note mention the old API in prose only).
    const executable = src
      .split("\n")
      .filter((line) => line.includes(".metadata("))
      .filter((line) => !line.trim().startsWith("*") && !line.trim().startsWith("//"));
    expect(executable, `live .metadata( calls remain: ${executable.join(" | ")}`).toEqual([]);
    expect(src).not.toContain("jquery.metadata");
  });

  it("classic stylesheets carry the jQuery UI tooltip bubble parity rules", () => {
    const css = readFileSync(join(WEBAPP_CM, "css/styles.css"), "utf8");
    expect(css).toContain(".ui-tooltip");
    expect(css).toContain("background-color: #eee");
  });
});
