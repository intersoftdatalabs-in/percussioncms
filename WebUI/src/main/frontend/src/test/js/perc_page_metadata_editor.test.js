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
 * Tests for the `$.perc_page_metadata_editor` published by
 * `WebUI/src/main/webapp/cm/widgets/perc_page_metadata_editor.js`
 * (#5733: React replacement entry point for the retired classic
 * `perc_page_edit_dialog`).
 *
 * Loading pattern: the file under test is a legacy IIFE that expects a
 * global jQuery. We install a stub jQuery, eval the file (mirror the
 * legacy <script> runtime), then exercise the helper via the global it
 * publishes (`$.perc_page_metadata_editor`).
 */

import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import { beforeEach, afterEach, describe, it, expect } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));

// Resolve to the legacy helper script:
//   file: WebUI/src/main/frontend/src/test/js/perc_page_metadata_editor.test.js
//   src:  WebUI/src/main/webapp/cm/widgets/perc_page_metadata_editor.js
// Up four levels from the file -> .../main, then descend into webapp/.
const SOURCE_PATH = resolve(
  __dirname,
  "../../../../webapp/cm/widgets/perc_page_metadata_editor.js",
);

// ---------------------------------------------------------------------------
// Bootstrap helpers
// ---------------------------------------------------------------------------

/**
 * Builds a minimal jQuery stub sufficient to load
 * perc_page_metadata_editor.js. The file's IIFE only assigns
 * `$.perc_page_metadata_editor = ...`, so the stub only needs to be a
 * regular object that accepts that property.
 */
function installJQueryStub() {
  globalThis.jQuery = {};
  globalThis.$ = globalThis.jQuery;
}

// ---------------------------------------------------------------------------
// Lifecycle
// ---------------------------------------------------------------------------

beforeEach(() => {
  installJQueryStub();
  // Clear any prior helper registration so the source IIFE re-runs.
  if (globalThis.$) {
    delete globalThis.$.perc_page_metadata_editor;
  }
  const code = readFileSync(SOURCE_PATH, "utf8");
  (0, eval)(code);
});

afterEach(() => {
  delete globalThis.jQuery;
  delete globalThis.$;
});

// ---------------------------------------------------------------------------
// API surface
// ---------------------------------------------------------------------------

describe("perc_page_metadata_editor API surface", () => {
  it("is exposed on $ after perc_page_metadata_editor.js loads", () => {
    expect(globalThis.$.perc_page_metadata_editor).toBeDefined();
    expect(typeof globalThis.$.perc_page_metadata_editor.open).toBe(
      "function",
    );
    expect(typeof globalThis.$.perc_page_metadata_editor.url).toBe(
      "function",
    );
    expect(typeof globalThis.$.perc_page_metadata_editor.mode).toBe(
      "function",
    );
    expect(typeof globalThis.$.perc_page_metadata_editor.windowName).toBe(
      "function",
    );
  });

  it("exposes the React editor window features", () => {
    expect(globalThis.$.perc_page_metadata_editor.windowFeatures).toBe(
      "popup=yes,width=960,height=800,scrollbars=yes,resizable=yes",
    );
  });
});

// ---------------------------------------------------------------------------
// URL building (mirrors buildEditorHostUrl + withCmsContextPrefix)
// ---------------------------------------------------------------------------

describe("perc_page_metadata_editor.url", () => {
  it("builds the editor entry URL for a page id in edit mode", () => {
    const url = globalThis.$.perc_page_metadata_editor.url("42", "edit");
    expect(url).toContain("/cm/app/spa.jsp");
    expect(url).toContain("entry=editor");
    expect(url).toContain(`contentId=${encodeURIComponent("42")}`);
    expect(url).toContain("mode=edit");
  });

  it("builds the view-mode URL for read-only metadata", () => {
    const url = globalThis.$.perc_page_metadata_editor.url("42", "view");
    expect(url).toContain("mode=view");
  });

  it("URL-encodes page ids", () => {
    const url = globalThis.$.perc_page_metadata_editor.url("1-2-3", "edit");
    expect(url).toContain(`contentId=${encodeURIComponent("1-2-3")}`);
  });

  it("defaults to the resolved navigation mode when omitted", () => {
    const url = globalThis.$.perc_page_metadata_editor.url("42");
    expect(url).toContain("mode=edit");
  });
});

// ---------------------------------------------------------------------------
// Mode resolution (mirrors the retired dialog edit/read-only switch)
// ---------------------------------------------------------------------------

describe("perc_page_metadata_editor.mode", () => {
  it("defaults to edit when no navigation manager is present", () => {
    expect(globalThis.$.perc_page_metadata_editor.mode()).toBe("edit");
  });

  it("returns edit in navigation EDIT mode", () => {
    globalThis.$.PercNavigationManager = {
      MODE_EDIT: "EDIT",
      MODE_READONLY: "READONLY",
      getMode: () => "EDIT",
    };
    expect(globalThis.$.perc_page_metadata_editor.mode()).toBe("edit");
  });

  it("returns view outside navigation EDIT mode", () => {
    globalThis.$.PercNavigationManager = {
      MODE_EDIT: "EDIT",
      MODE_READONLY: "READONLY",
      getMode: () => "READONLY",
    };
    expect(globalThis.$.perc_page_metadata_editor.mode()).toBe("view");
  });
});

// ---------------------------------------------------------------------------
// Window handling
// ---------------------------------------------------------------------------

describe("perc_page_metadata_editor window handling", () => {
  it("names one editor window per page id", () => {
    expect(globalThis.$.perc_page_metadata_editor.windowName("42")).toBe(
      "percEditor_42",
    );
  });

  it("open() delegates to window.open with url, name and features", () => {
    const calls = [];
    const opener = globalThis.window.open;
    globalThis.window.open = (...args) => {
      calls.push(args);
      return null;
    };
    try {
      globalThis.$.perc_page_metadata_editor.open("42");
    } finally {
      globalThis.window.open = opener;
    }
    expect(calls).toHaveLength(1);
    const [url, name, features] = calls[0];
    expect(url).toContain("entry=editor");
    expect(url).toContain("mode=edit");
    expect(name).toBe("percEditor_42");
    expect(features).toBe(
      globalThis.$.perc_page_metadata_editor.windowFeatures,
    );
  });
});
