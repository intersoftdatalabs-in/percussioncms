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
 * Editor behavior for the percCalendarTwo Google-Calendar control
 * (issue #6723, parent #6686).
 *
 * The XSL control (`percGoogleCalendarControl.xsl`) renders a
 * `tr.hide` template row whose color inputs are driven by the shipped
 * `percCalendarTwo.js` through the MiniColors payload twins. The
 * pre-#6723 editor broke every added row: the template carried duplicate
 * `id`s, rows were added with `.clone(true)` (copying initialized picker
 * state), and the add-row re-init targeted those ids (first row only) as
 * a no-op. The fixed editor clones without data/events, strips ids, and
 * initializes each row's color inputs independently by class, reading the
 * input's own `data-*` options.
 *
 * These tests evaluate the SHIPPED payload bytes verbatim (both the
 * SupportFile preview twin and the sys__UserDependency published twin,
 * each with its MiniColors twin) in jsdom and drive the real editor
 * flow: data load, +-row add, swatch pick, remove, and save persistence
 * through the captured pre-submit handler.
 */

import { describe, expect, it } from "vitest";
import fs from "node:fs";
import path from "node:path";
import { createRequire } from "node:module";
import { fileURLToPath } from "node:url";

const require = createRequire(import.meta.url);
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// src/test/js -> frontend root (peer: widgetPayloadNpmParity.test.js), then
// up to the repo root so the perc-packages payloads resolve portably.
const FRONTEND_ROOT = path.dirname(
  path.dirname(path.dirname(__dirname)),
);
const REPO_ROOT = path.join(FRONTEND_ROOT, "..", "..", "..", "..");
const NODE_MODULES = path.join(FRONTEND_ROOT, "node_modules");

const CALENDAR_BASE = path.join(
  "modules",
  "perc-packages",
  "src",
  "main",
  "resources",
  "Packages",
  "perc.widget.calendar",
);

const EDITOR_TWINS = [
  {
    label: "SupportFile-rx_resources (preview)",
    tree: "SupportFile-rx_resources",
  },
  {
    label: "sys__UserDependency--rx_resources (published)",
    tree: "sys__UserDependency--rx_resources",
  },
];

const SAVED_CONFIG = {
  config: [
    {
      calendarName: "Team",
      calendarId: "team@example.com",
      apiKey: "KEY-ONE",
      backgroundColor: "#3a87ad",
      textColor: "#ffffff",
    },
    {
      calendarName: "Holidays",
      calendarId: "holidays@example.com",
      apiKey: "KEY-TWO",
      backgroundColor: "#ff0000",
      textColor: "#000000",
    },
  ],
};

function templateRow() {
  // Mirrors percGoogleCalendarControl.xsl after the #6723 fix: class-only
  // template inputs (no ids — rows are cloned), color defaults plus the
  // screen-reader labels added as UX polish in the same slice.
  return (
    '<tr class="hide perc-google-calendar-row" role="presentation">' +
    '<td class="perc-input-td"><input class="perc-google-calendar-name" type="text"></td>' +
    '<td class="perc-input-td"><input class="perc-google-calendar-id" type="text"></td>' +
    '<td class="perc-input-td"><input class="perc-google-calendar-api-key" type="text"></td>' +
    '<td class="perc-input-td"><input class="perc-google-calendar-background-color" ' +
    'data-control="hue" data-swatches="#ff0000|#00ff00|#0000ff" value="#3a87ad" type="text" ' +
    'aria-label="Background color"></td>' +
    '<td class="perc-input-td"><input class="perc-google-calendar-text-color" ' +
    'data-control="hue" value="#ffffff" type="text" aria-label="Text color"></td>' +
    '<td class="perc-input-td"><span class="perc-table-remove fa fa-minus"></span>' +
    '<span class="perc-table-up"></span><span class="perc-table-down"></span></td>' +
    "</tr>"
  );
}

function renderEditorMarkup(savedJson) {
  // Mirrors the XSL editor shell: the .perc-google-calendar div id names
  // the hidden field carrying the widget JSON; the toggle/help chrome and
  // the setup table carry the template row.
  // toggles the setup section visible; the editor hides it on render until
  // toggled, mirroring production (focus can only land in a visible tree).
  document.body.innerHTML =
    '<input type="hidden" name="percCalendarField" id="perc-content-edit-percCalendarField" ' +
    `value='${savedJson}'>` +
    '<span class="perc-toggle-google-setup">toggle</span>' +
    '<div class="perc-google-calendar" id="percCalendarField">' +
    "<h2>Google Calendar Setup</h2>" +
    '<div class="perc-help" style="display:none;">help</div>' +
    '<div id="perc-google-calendar-setup-editor" class="perc-google-calendar-setup-editor">' +
    '<span class="perc-table-add fa fa-plus-square"></span>' +
    '<table class="perc-table"><tbody>' +
    "<tr><td>Calendar Name</td><td>Calendar ID</td><td>API Key</td>" +
    "<td>Background Color</td><td>Text Color</td><td></td></tr>" +
    templateRow() +
    "</tbody></table></div></div>";
}

/**
 * Evaluates the shipped editor bytes verbatim on the jsdom page. The
 * editor registers on document ready, so the returned promise resolves
 * after the editor's own ready handler has run (jQuery ready handlers run
 * in registration order).
 */
async function loadEditorTwin(tree) {
  const $ = require(path.join(NODE_MODULES, "jquery", "dist", "jquery.js"));
  globalThis.jQuery = $;
  window.jQuery = $;
  // The editor attaches through window.parent (an iframe in production);
  // capture the pre-submit handler the same shape production provides.
  const submitted = [];
  window.jQuery.PercContentPreSubmitHandlers = {
    addHandler(handler) {
      submitted.push(handler);
    },
  };
  const minicolorsSource = fs.readFileSync(
    path.join(
      REPO_ROOT,
      CALENDAR_BASE,
      tree,
      "widgets",
      "percCalendarTwo",
      "js",
      "jquery.minicolors.js",
    ),
    "utf8",
  );
  window.eval(minicolorsSource);
  const editorSource = fs.readFileSync(
    path.join(
      REPO_ROOT,
      CALENDAR_BASE,
      tree,
      "widgets",
      "percCalendarTwo",
      "js",
      "percCalendarTwo.js",
    ),
    "utf8",
  );
  window.eval(editorSource);
  await new Promise((resolve) => $(resolve));
  return { $, submitted };
}

function visibleRows($) {
  return $(".perc-google-calendar-row").filter(function () {
    return !$(this).hasClass("hide");
  });
}

function colorInputs($row) {
  const $ = window.jQuery;
  return {
    background: $row.find(".perc-google-calendar-background-color"),
    text: $row.find(".perc-google-calendar-text-color"),
  };
}

function initializedPickerCount($row) {
  const { background, text } = colorInputs($row);
  let count = 0;
  for (const $input of [background, text]) {
    if (
      $input.hasClass("minicolors-input") &&
      $input.data("minicolors-initialized") === true &&
      $input.closest(".minicolors").length === 1
    ) {
      count += 1;
    }
  }
  return count;
}

describe.each(EDITOR_TWINS.map(({ label }) => label))(
  "percCalendarTwo editor added-row pickers (%s, issue #6723)",
  (label) => {
    const tree = EDITOR_TWINS.find(({ label: l }) => l === label).tree;

    it("restores saved rows with independent initialized pickers (no duplicate ids)", async () => {
      renderEditorMarkup(JSON.stringify(SAVED_CONFIG).replace(/'/g, "&#39;"));
      const { $, submitted } = await loadEditorTwin(tree);
      expect(submitted).toHaveLength(1);
      try {
        const $rows = visibleRows($);
        expect($rows).toHaveLength(2);
        // Each restored row owns two working pickers reading that row's
        // saved colors — not the template defaults.
        $rows.each(function (index) {
          expect(initializedPickerCount($(this))).toBe(2);
          const { background, text } = colorInputs($(this));
          expect(background.minicolors("value")).toBe(
            SAVED_CONFIG.config[index].backgroundColor,
          );
          expect(text.minicolors("value")).toBe(
            SAVED_CONFIG.config[index].textColor,
          );
        });
        // The hidden template row is never initialized (no wasted picker,
        // no stale state for clones to inherit).
        const $template = $(".perc-google-calendar-row.hide");
        expect($template).toHaveLength(1);
        expect(initializedPickerCount($template)).toBe(0);
        // No duplicate element ids anywhere in the editor.
        const seen = new Set();
        let duplicates = 0;
        $("[id]").each(function () {
          const id = $(this).attr("id");
          if (seen.has(id)) {
            duplicates += 1;
          }
          seen.add(id);
        });
        expect(duplicates).toBe(0);
      } finally {
        document.body.innerHTML = "";
      }
    });

    it("added rows get independent working pickers and keep focus in the new row", async () => {
      renderEditorMarkup(JSON.stringify(SAVED_CONFIG).replace(/'/g, "&#39;"));
      const { $ } = await loadEditorTwin(tree);
      try {
        // Real user path: toggle the setup section open, then add a row.
        $(".perc-toggle-google-setup").trigger("click");
        $(".perc-table-add").trigger("click");
        let $rows = visibleRows($);
        expect($rows).toHaveLength(3);
        // Keyboard polish: focus lands in the new row's first field.
        expect(document.activeElement).toBe(
          $rows.last().find(".perc-google-calendar-name").get(0),
        );
        $(".perc-table-add").trigger("click");
        $rows = visibleRows($);
        expect($rows).toHaveLength(4);
        // Every added row owns two initialized pickers.
        $rows.each(function () {
          expect(initializedPickerCount($(this))).toBe(2);
        });
        // Still no duplicate ids after cloning.
        const seen = new Set();
        let duplicates = 0;
        $("[id]").each(function () {
          const id = $(this).attr("id");
          if (seen.has(id)) {
            duplicates += 1;
          }
          seen.add(id);
        });
        expect(duplicates).toBe(0);
        // Pickers are independent: picking in the last row leaves the
        // first row's value untouched.
        const $first = $rows.first();
        const $last = $rows.last();
        const $lastSwatches = $last.find(
          ".minicolors-swatches .minicolors-swatch",
        );
        // The template's data-swatches surface flows into each row's
        // picker (proves per-input data-* init, not document defaults).
        expect($lastSwatches.length).toBe(3);
        $lastSwatches.eq(1).trigger("click");
        expect(
          $last.find(".perc-google-calendar-background-color").val(),
        ).toBe("#00ff00");
        expect(
          $first.find(".perc-google-calendar-background-color").val(),
        ).toBe(SAVED_CONFIG.config[0].backgroundColor);
      } finally {
        document.body.innerHTML = "";
      }
    });

    it("added rows persist picked colors into the widget JSON on save", async () => {
      renderEditorMarkup(JSON.stringify(SAVED_CONFIG).replace(/'/g, "&#39;"));
      const { $, submitted } = await loadEditorTwin(tree);
      try {
        $(".perc-table-add").trigger("click");
        const $rows = visibleRows($);
        expect($rows).toHaveLength(3);
        const $added = $rows.last();
        $added.find(".perc-google-calendar-name").val("Added");
        $added.find(".perc-google-calendar-id").val("added@example.com");
        $added.find(".perc-google-calendar-api-key").val("KEY-THREE");
        $added
          .find(".perc-google-calendar-background-color")
          .minicolors("value", "#00ff00");
        $added.find(".perc-google-calendar-text-color").minicolors("value", "#123456");
        expect(submitted).toHaveLength(1);
        expect(submitted[0]()).toBe(true);
        const saved = JSON.parse(
          $("input[name='percCalendarField']").val(),
        );
        expect(saved.config).toHaveLength(3);
        expect(saved.config[2]).toMatchObject({
          calendarName: "Added",
          calendarId: "added@example.com",
          apiKey: "KEY-THREE",
          backgroundColor: "#00ff00",
          textColor: "#123456",
          className: "added",
        });
      } finally {
        document.body.innerHTML = "";
      }
    });

    it("delegated row buttons remove an added row (no copied handlers)", async () => {
      renderEditorMarkup(JSON.stringify({ config: [] }));
      const { $ } = await loadEditorTwin(tree);
      try {
        expect(visibleRows($)).toHaveLength(0);
        $(".perc-table-add").trigger("click");
        expect(visibleRows($)).toHaveLength(1);
        // Direct binding would leave this button dead: the row was cloned
        // with clone(false), so only delegation reaches it.
        visibleRows($).first().find(".perc-table-remove").trigger("click");
        expect(visibleRows($)).toHaveLength(0);
      } finally {
        document.body.innerHTML = "";
      }
    });
  },
);
