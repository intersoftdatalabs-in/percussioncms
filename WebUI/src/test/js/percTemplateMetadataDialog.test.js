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
 * Regression tests for `setMetaContent` in
 * `WebUI/src/main/webapp/cm/widgets/perc_template_metadata_dialog.js`
 * (issue #6702: `complexity.cognitive` on `setMetaContent`, extracted
 * helpers `applyDocTypeOptions` / `selectDoctypeRadio` — behavior
 * preserving, no behavior change).
 *
 * Loading pattern: the file under test is a legacy IIFE that publishes
 * `$.perc_template_metadata_dialog(templateId)` on the global jQuery and
 * immediately builds the dialog via `$(...).perc_dialog(...)`. The test
 * installs real jQuery over jsdom, stubs the CMS collaborators
 * (`perc_dialog`, `$.ajax`, `PercSiteTemplatesController`,
 * `PercNavigationManager`, `perc_filterField`), evals the source (mirror
 * the legacy <script> runtime), then drives the dialog exactly the way
 * production does:
 *
 *   factory() -> dialog open() -> ajax success -> iframe load ->
 *   loadTemplateMetadata callback
 *
 * The iframe's `.contents()` is stubbed to a controlled fake frame
 * document holding the doctype radios plus the custom-doctype textarea,
 * so the observable effects of `setMetaContent` — which radio gets
 * clicked and which preset value lands in the textarea via the
 * `_addFieldGroups` radio handler — are asserted end to end.
 *
 * These tests were written against the pre-refactor implementation and
 * kept green through the extraction, proving behavior parity.
 */

import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import jquery from "jquery";
import { beforeEach, afterEach, describe, it, expect, vi } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));
const SRC_PATH = resolve(
  __dirname,
  "../../main/webapp/cm/widgets/perc_template_metadata_dialog.js",
);

let $;
let dialogOptions;
let ajaxOptions;
let metadataCallback;
let metadataTemplateId;

function seedDom() {
  // NOTE: #edit-page-metadata is created by the dialog factory itself via
  // the perc_dialog stub; only the fake iframe document is seeded here.
  document.body.innerHTML = `
    <div id="fake-frame-doc">
      <input type="radio" name="doctype" value="xhtml" id="radio-xhtml" />
      <input type="radio" name="doctype" value="html5" id="radio-html5" />
      <input type="radio" name="doctype" value="custom" id="radio-custom" />
      <textarea id="perc_template_metadata_custom_doctype"></textarea>
    </div>
  `;
}

function installShims() {
  $.PercNavigationManager = {
    MODE_EDIT: "EDIT",
    getMode: () => "EDIT",
  };
  $.perc_paths = { TEMPLATE_EDITURL: "/Rhythmyx/services/template/edit" };
  $.PercDirtyController = { setDirty: vi.fn() };
  $.perc_filterField = vi.fn();
  $.perc_textFilters = { URL: "URL" };
  $.perc_utils = { alert_dialog: vi.fn() };
  $.PercSiteTemplatesController = vi.fn(() => ({
    loadTemplateMetadata: (templateId, cb) => {
      metadataTemplateId = templateId;
      metadataCallback = cb;
    },
    saveTemplateMetadata: vi.fn(),
  }));
  $.ajax = vi.fn((opts) => {
    ajaxOptions = opts;
  });
  $.fn.perc_dialog = function (options) {
    dialogOptions = options;
    if (this[0] && this[0].nodeType === 1) {
      document.body.appendChild(this[0]);
    }
    return this;
  };
  // The real dialog reads the iframe document; point it at the fake frame
  // document seeded above so the test can observe the radio clicks.
  $.fn.contents = function () {
    return $("#fake-frame-doc");
  };
  globalThis.I18N = { message: (key) => `[${key}]` };
}

function loadSource() {
  // With vitest jsdom globals, the jquery default export is already bound
  // to the jsdom window; without globals it is a factory needing window.
  let jq = null;
  if (typeof jquery === "function" && jquery.fn) {
    jq = jquery;
  } else if (typeof jquery === "function") {
    const bound = jquery(globalThis.window);
    jq = typeof bound === "function" ? bound : globalThis.jQuery;
  } else {
    jq = globalThis.jQuery;
  }
  if (!jq.fn && jq.prototype) jq.fn = jq.prototype;
  if (!jq.fn) throw new Error("jquery has neither .fn nor .prototype");
  $ = jq;
  globalThis.jQuery = $;
  globalThis.$ = $;
  installShims();
  // Run the source as a script in the global scope so its IIFE sees
  // globalThis.jQuery.
  (0, eval)(readFileSync(SRC_PATH, "utf8"));
}

beforeEach(() => {
  dialogOptions = undefined;
  ajaxOptions = undefined;
  metadataCallback = undefined;
  metadataTemplateId = undefined;
  seedDom();
  loadSource();
});

afterEach(() => {
  vi.restoreAllMocks();
  document.body.innerHTML = "";
  delete globalThis.I18N;
  delete globalThis.jQuery;
  delete globalThis.$;
});

/**
 * Drives the dialog to the point where setMetaContent's metadata callback
 * is captured, fires it with `metadataObj`, and records which doctype
 * radio the dialog clicks. Returns the clicked radio values in order.
 */
function fireMetadataCallback(metadataObj) {
  $.perc_template_metadata_dialog("tmpl-1");
  expect(dialogOptions).toBeTruthy();
  dialogOptions.open();
  expect(ajaxOptions).toBeTruthy();
  ajaxOptions.success("PAGEURL", "success");
  const clicked = [];
  $("#fake-frame-doc input[name='doctype']").on("click", function () {
    clicked.push($(this).attr("value"));
  });
  $("#edit-page-metadata-frame").trigger("load");
  expect(typeof metadataCallback).toBe("function");
  expect(metadataTemplateId).toBe("tmpl-1");
  metadataCallback(metadataObj);
  return clicked;
}

function metadata(selected, options) {
  return { HtmlMetadata: { docType: { selected, options } } };
}

const FULL_OPTIONS = [
  { option: "xhtml", value: "XHTMLV" },
  { option: "html5", value: "HTML5V" },
  { option: "custom", value: "CUSTOMV" },
];

function customTextarea() {
  return document.querySelector(
    "#perc_template_metadata_custom_doctype",
  );
}

// ---------------------------------------------------------------------------
// Behavioural tests — pin setMetaContent's observable contract.
// ---------------------------------------------------------------------------
describe("setMetaContent (issue #6702 behavior parity)", () => {
  it("clicks the radio matching the stored doctype selection", () => {
    const clicked = fireMetadataCallback(metadata("html5", FULL_OPTIONS));
    expect(clicked).toEqual(["html5"]);
  });

  it("syncs the html5 preset value into the textarea and locks it", () => {
    fireMetadataCallback(metadata("html5", FULL_OPTIONS));
    expect(customTextarea().value).toBe("HTML5V");
    expect(customTextarea().getAttribute("readonly")).toBe("readonly");
  });

  it("syncs the xhtml preset value into the textarea and locks it", () => {
    const clicked = fireMetadataCallback(metadata("xhtml", FULL_OPTIONS));
    expect(clicked).toEqual(["xhtml"]);
    expect(customTextarea().value).toBe("XHTMLV");
    expect(customTextarea().getAttribute("readonly")).toBe("readonly");
  });

  it("restores the stored custom value and unlocks the textarea", () => {
    const clicked = fireMetadataCallback(metadata("custom", FULL_OPTIONS));
    expect(clicked).toEqual(["custom"]);
    expect(customTextarea().value).toBe("CUSTOMV");
    expect(customTextarea().getAttribute("readonly")).toBeNull();
  });

  it("ignores unknown option names and lets later entries win", () => {
    const clicked = fireMetadataCallback(
      metadata("xhtml", [
        { option: "bogus", value: "BOGUS" },
        { option: "xhtml", value: "FIRST" },
        { option: "html5", value: "HTML5V" },
        { option: "custom", value: "CUSTOMV" },
        { option: "xhtml", value: "LAST" },
      ]),
    );
    expect(clicked).toEqual(["xhtml"]);
    // The xhtml radio handler writes the mapped value into the textarea,
    // so the textarea pins which duplicate won.
    expect(customTextarea().value).toBe("LAST");
  });
});

// ---------------------------------------------------------------------------
// Source-pattern tests — pin the #6702 extraction so the cognitive load
// does not creep back into setMetaContent unnoticed.
// ---------------------------------------------------------------------------
describe("source-pattern (issue #6702 extraction)", () => {
  const src = readFileSync(SRC_PATH, "utf8");

  it("extracts the doctype option mapping into a helper", () => {
    expect(src).toMatch(/function applyDocTypeOptions/);
  });

  it("extracts the radio selection into a helper", () => {
    expect(src).toMatch(/function selectDoctypeRadio/);
  });

  it("no longer walks the options array with an if/else-if chain", () => {
    expect(src).not.toMatch(/getDocValue\[i\]\.option ==/);
  });
});

// ---------------------------------------------------------------------------
// Public API surface — pin the factory entry point so callers keep working.
// ---------------------------------------------------------------------------
describe("public API", () => {
  it("$.perc_template_metadata_dialog is a function", () => {
    expect(typeof $.perc_template_metadata_dialog).toBe("function");
  });
});
