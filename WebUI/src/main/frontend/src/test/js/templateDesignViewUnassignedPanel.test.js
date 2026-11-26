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
 * Behavioural tests for the unassigned-panel slice of
 * WebUI/src/main/webapp/cm/views/PercTemplateDesignView.js
 * (mirrored in cm/app/js/legacy/views/ and WebUI/war/views/).
 *
 * Issue #7256 extracted the per-concern helpers (preference load,
 * expander wiring, grid init, pagination, menu a11y) out of the
 * cognitive-35 initializeUnassignedPanel body. These tests drive the
 * live source end-to-end through P.templateDesignView() and pin the
 * preserved behaviour: expander toggle, keyboard activation, initial
 * state from the persisted cookie, paginationControls, and the
 * missing-cookie hardening (npm jquery.cookie returns undefined where
 * the old rewrite returned null).
 */

import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import jquery from "jquery";
import { beforeEach, afterEach, describe, it, expect, vi } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));
const SRC_PATH = resolve(
  __dirname,
  "../../../../webapp/cm/views/PercTemplateDesignView.js",
);

const SITE = "test-site-7256";
const cookieKey = `perc-unassigned-panel-${SITE}-settings`;

let $;
let P;
let cookieStore;
let pageServiceCalls;
let templateSelectedCalls;
let filterFieldCalls;
let scheduledIntervals;
let clearedIntervals;
let siteName;
let serviceResult;
let realSetInterval;
let realClearInterval;

function successResult(children, total) {
  const totalPages = typeof total === "number" ? total : 10;
  return {
    UnassignedResults: {
      importStatus: {
        catalogedPageCount: totalPages - 5,
        importedPageCount: 5,
      },
      unassignedItemList: {
        startIndex: 1,
        childrenInPage: children,
      },
    },
  };
}

function skeleton(expanderClass) {
  document.body.innerHTML = `
    <div id="frame"></div>
    <div id="perc-pageEditor-menu-name">Site menu</div>
    <span id="perc-site-templates-label"></span>
    <span id="perc-site-templates-inline-help"></span>
    <a id="perc-temp-lib-expander"></a>
    <a id="perc-temp-lib-maximizer"></a>
    <div id="tabs"></div>
    <div id="perc-dropdown-actions"><div><div><span></span><a href="#a1">Action</a></div></div></div>
    <div id="perc-dropdown-view"><div><div><a href="#v1">View</a></div></div></div>
    <a href="#" class="perc-panel-expander-icon ${expanderClass}"></a>
    <div class="perc-unassigned-panel perc-closed">
      <div class="perc-template-pages-controls">
        <button type="button" class="previous">Prev</button>
        <button type="button" class="next">Next</button>
        <input class="perc-jump" value="" />
      </div>
      <div class="perc-panel-pages-list"></div>
      <span class="perc-panel-page-group-range"></span>
      <span class="perc-panel-total"></span>
    </div>
    <div id="perc-assigned-templates"></div>
    <div class="perc-panel-progress">
      <span class="perc-progress-finished"></span>
      <div class="perc-progress-bar-container"><div class="perc-progress-bar"></div></div>
      <span class="perc-progress-message"></span>
    </div>`;
}

function bootstrapJquery() {
  let jq = jquery(globalThis.window);
  if (typeof jq !== "function") {
    jq = typeof jquery === "function" ? jquery : jquery.fn || jquery;
    if (!jq.fn && jq.prototype) jq.fn = jq.prototype;
  }
  if (!jq.fn) throw new Error("jquery has no .fn");
  $ = jq;
  globalThis.jQuery = $;
  globalThis.$ = $;
}

function stubPercussionServices() {
  $.perc_iframe_fix = () => {};
  $.PercDirtyController = {};
  $.perc_utils = { convertCXFArray: (value) => value };
  $.PercNavigationManager = { getSiteName: () => siteName };
  $.PercServiceUtils = { STATUS_SUCCESS: "OK" };
  $.PercPageService = {
    getUnassignedPagesBySite: (site, start, max, callback) => {
      pageServiceCalls.push({ site, start, max });
      // Mirror the server: the returned page echoes the requested start.
      const result = JSON.parse(JSON.stringify(serviceResult));
      result.UnassignedResults.unassignedItemList.startIndex = start;
      callback("OK", result);
    },
  };
}

function stubCookieHarness() {
  // In-memory cookie with npm jquery.cookie 1.4.1 semantics: a missing
  // key reads back as undefined (not null).
  $.cookie = (key, value) => {
    if (typeof value === "undefined") return cookieStore[key];
    cookieStore[key] = String(value);
    return undefined;
  };
}

function stubJqueryPlugins() {
  $.perc_filterField = (field) => {
    filterFieldCalls.push(field);
  };
  $.perc_textFilters = { ONLY_DIGITS: "only-digits" };

  $.fn.PercDropdown = function () {
    // Mirror the real plugin: it renders the option links into the menu
    // container (the view empties the containers with .html("") first).
    // Other callers (page-row dropdowns) just need the chainable no-op.
    this.each(function () {
      if (
        this.id === "perc-dropdown-actions" ||
        this.id === "perc-dropdown-view"
      ) {
        $(this).append('<div><div><a href="#">Option</a></div></div>');
      }
    });
    return this;
  };
  $.fn.PercImageTooltip = function () {
    return this;
  };
  $.fn.tabs = function () {
    return this;
  };
  $.fn.template_selected = function () {
    templateSelectedCalls.push(Array.from(arguments));
    return this;
  };
  // jQuery-UI animated show/hide are not loaded in this harness; run the
  // completion callback synchronously so toggle assertions stay direct.
  $.fn.show = function () {
    const callback = Array.from(arguments).find(
      (arg) => typeof arg === "function",
    );
    if (callback) callback.call(this);
    return this;
  };
  $.fn.hide = function () {
    const callback = Array.from(arguments).find(
      (arg) => typeof arg === "function",
    );
    if (callback) callback.call(this);
    return this;
  };
}

function evalViewSource() {
  globalThis.I18N = { message: (key) => `[${key}]` };
  // fixTemplateHeight is an implicit global from cm/views/header.js, called
  // once at the end of templateDesignView().
  globalThis.fixTemplateHeight = () => {};
  $.Percussion = $.Percussion || {};
  (0, eval)(readFileSync(SRC_PATH, "utf8"));
  P = $.Percussion;
}

function loadSource() {
  bootstrapJquery();
  stubPercussionServices();
  stubCookieHarness();
  stubJqueryPlugins();
  evalViewSource();
}

beforeEach(() => {
  cookieStore = {};
  pageServiceCalls = [];
  templateSelectedCalls = [];
  filterFieldCalls = [];
  scheduledIntervals = [];
  clearedIntervals = [];
  siteName = SITE;
  serviceResult = successResult([
    { id: "p1", path: "/a", name: "A", status: "NotImported" },
  ]);
  realSetInterval = globalThis.setInterval;
  realClearInterval = globalThis.clearInterval;
  globalThis.setInterval = (fn, ms) => {
    scheduledIntervals.push({ fn, ms });
    return 7256;
  };
  globalThis.clearInterval = (id) => {
    clearedIntervals.push(id);
  };
  if (globalThis.window) {
    globalThis.window.clearInterval = globalThis.clearInterval;
  }
});

afterEach(() => {
  globalThis.setInterval = realSetInterval;
  globalThis.clearInterval = realClearInterval;
  document.body.innerHTML = "";
  delete globalThis.I18N;
  delete globalThis.fixTemplateHeight;
  vi.restoreAllMocks();
});

function startView(expanderClass = "perc-collapsed") {
  skeleton(expanderClass);
  loadSource();
  P.templateDesignView();
  return {
    panel: $(".perc-unassigned-panel"),
    expander: $(".perc-panel-expander-icon"),
  };
}

describe("unassigned panel expander", () => {
  it("expands the collapsed panel, refreshes pages, and schedules polling", () => {
    // Seed a stored "closed" preference: without it the initial load
    // opens the panel from the grid results and there is nothing to expand.
    cookieStore[cookieKey] = JSON.stringify({ showPanel: false, pageNumber: 1 });
    const { panel, expander } = startView("perc-collapsed");
    expect(panel.hasClass("perc-opened")).toBe(false);
    pageServiceCalls.length = 0;
    scheduledIntervals.length = 0;

    expander.trigger("click");

    expect(panel.hasClass("perc-opened")).toBe(true);
    expect(expander.hasClass("perc-expanded")).toBe(true);
    // requestUnassignedPages() with no page defaults to page 1.
    expect(pageServiceCalls).toHaveLength(1);
    expect(pageServiceCalls[0]).toMatchObject({ site: SITE, start: 1 });
    expect(scheduledIntervals).toHaveLength(1);
    expect(scheduledIntervals[0].ms).toBe(10000);
    expect(templateSelectedCalls.length).toBeGreaterThan(0);
    // showPanel preference persisted as true.
    expect(JSON.parse(cookieStore[cookieKey]).showPanel).toBe(true);
  });

  it("collapses the expanded panel, clears polling, and persists showPanel=false", () => {
    cookieStore[cookieKey] = JSON.stringify({ showPanel: true, pageNumber: 1 });
    const { panel, expander } = startView("perc-expanded");
    expect(panel.hasClass("perc-opened")).toBe(true);
    pageServiceCalls.length = 0;

    expander.trigger("click");

    expect(panel.hasClass("perc-closed")).toBe(true);
    expect(expander.hasClass("perc-collapsed")).toBe(true);
    expect(clearedIntervals.length).toBeGreaterThan(0);
    expect(JSON.parse(cookieStore[cookieKey]).showPanel).toBe(false);
  });

  it("keyboard Enter/Space on the expander toggles the panel", () => {
    cookieStore[cookieKey] = JSON.stringify({ showPanel: false, pageNumber: 1 });
    const { panel, expander } = startView("perc-collapsed");
    expander[0].focus();
    expander.trigger({ type: "keydown", code: "Enter" });
    expect(panel.hasClass("perc-opened")).toBe(true);

    expander.trigger({ type: "keydown", code: "Space" });
    expect(panel.hasClass("perc-closed")).toBe(true);
  });

  it("does nothing on click when no site is selected", () => {
    siteName = undefined;
    const { panel, expander } = startView("perc-collapsed");
    expander.trigger("click");
    expect(panel.hasClass("perc-opened")).toBe(false);
    expect(expander.hasClass("perc-disabled")).toBe(true);
  });
});

describe("initial panel state", () => {
  it("opens at the persisted page when the cookie says so", () => {
    cookieStore[cookieKey] = JSON.stringify({ showPanel: true, pageNumber: 3 });
    const { panel, expander } = startView("perc-collapsed");

    expect(panel.hasClass("perc-opened")).toBe(true);
    expect(expander.hasClass("perc-expanded")).toBe(true);
    // Page 3 of 10-per-page starts at index 21.
    const lastCall = pageServiceCalls[pageServiceCalls.length - 1];
    expect(lastCall).toMatchObject({ site: SITE, start: 21 });
    expect(scheduledIntervals).toHaveLength(1);
  });

  it("keeps the panel closed without throwing when no cookie exists", () => {
    serviceResult = successResult([]);
    const { panel } = startView("perc-collapsed");

    expect(panel.hasClass("perc-opened")).toBe(false);
    expect(scheduledIntervals).toHaveLength(0);
  });

  it("derives visibility from grid results when no preference is stored", () => {
    const { panel } = startView("perc-collapsed");
    expect(panel.hasClass("perc-opened")).toBe(true);
  });

  it("disables the expander when no site is selected", () => {
    siteName = undefined;
    const { expander } = startView("perc-collapsed");
    expect(expander.hasClass("perc-disabled")).toBe(true);
    expect(pageServiceCalls).toHaveLength(0);
  });
});

describe("pagination controls", () => {
  // updatePaging disables the previous button on page 1 and the next
  // button on the last page, so the fixture spans three pages (30 items
  // at 10 per page) to keep both controls live.
  beforeEach(() => {
    serviceResult = successResult(
      [{ id: "p1", path: "/a", name: "A", status: "NotImported" }],
      30,
    );
  });

  it("next button requests the following page", () => {
    startView("perc-collapsed");
    // Initial load lands on page 1 (jump box reads back "1").
    expect($(".perc-jump").val()).toBe("1");
    pageServiceCalls.length = 0;

    $(".perc-template-pages-controls .next").trigger("click");
    expect(pageServiceCalls).toHaveLength(1);
    expect(pageServiceCalls[0]).toMatchObject({ start: 11 });
    expect(JSON.parse(cookieStore[cookieKey]).pageNumber).toBe(2);
  });

  it("previous button requests the preceding page", () => {
    startView("perc-collapsed");
    $(".perc-jump").val("2");
    $(".perc-template-pages-controls").trigger("submit");
    // Page 2 re-enables the previous control via updatePaging.
    expect($(".perc-template-pages-controls .previous")).toHaveLength(1);
    pageServiceCalls.length = 0;

    $(".perc-template-pages-controls .previous").trigger("click");
    expect(pageServiceCalls).toHaveLength(1);
    expect(pageServiceCalls[0]).toMatchObject({ start: 1 });
    expect(JSON.parse(cookieStore[cookieKey]).pageNumber).toBe(1);
  });

  it("jump-box submit requests the entered page", () => {
    startView("perc-collapsed");
    $(".perc-jump").val("2");
    pageServiceCalls.length = 0;

    $(".perc-template-pages-controls").trigger("submit");
    expect(pageServiceCalls).toHaveLength(1);
    expect(pageServiceCalls[0]).toMatchObject({ start: 11 });
  });

  it("applies the digits-only filter to the jump box", () => {
    startView("perc-collapsed");
    expect(filterFieldCalls).toHaveLength(1);
  });
});

describe("menu accessibility", () => {
  it("marks the first link of each dropdown menu", () => {
    startView("perc-collapsed");
    for (const selector of ["#perc-dropdown-actions", "#perc-dropdown-view"]) {
      const firstLink = $(`${selector} a`).first();
      expect(firstLink.attr("tabindex")).toBe("0");
      expect(firstLink.attr("role")).toBe("button");
      expect(firstLink.attr("title")).toContain("perc.ui.template.design.view");
    }
  });
});

describe("source-pattern (anti-regression for issue #7256)", () => {
  const src = readFileSync(SRC_PATH, "utf8");
  const code = src
    .replace(/\/\*[\s\S]*?\*\//g, "")
    .replace(/(^|[^:])\/\/.*$/gm, "$1");

  it("keeps initializeUnassignedPanel as a thin orchestrator", () => {
    const m = code.match(/function initializeUnassignedPanel\(\)\s*\{([\s\S]*?)\n    \}/);
    expect(m, "initializeUnassignedPanel must exist").toBeTruthy();
    const body = m[1];
    for (const helper of [
      "bindPanelExpander",
      "loadInitialPanelState",
      "bindUnassignedPagination",
      "markFirstMenuLink",
    ]) {
      expect(body).toContain(helper);
    }
    // No nested service callbacks or panel-state branching remain inline.
    expect(body).not.toContain("getUnassignedPagesBySite");
    expect(body).not.toContain("perc-collapsed");
  });

  it("routes every persisted-preference read through the null-safe helper", () => {
    expect(code).toContain("function readPanelSettings");
    // No inline JSON.parse($.cookie(...)) remains: setPanelPreference and
    // applyInitialPanelState both read through readPanelSettings.
    expect(code).not.toMatch(/JSON\.parse\(\s*\$\.cookie\(/);
    expect(code).toMatch(/JSON\.parse\(rawCookie \|\| "null"\)/);
  });

  it("extracts expand/collapse/grid-state/pagination/menu helpers", () => {
    for (const helper of [
      "function toggleUnassignedPanel",
      "function expandUnassignedPanel",
      "function collapseUnassignedPanel",
      "function loadInitialPanelState",
      "function applyInitialPanelState",
      "function hasUnassignedChildren",
      "function bindUnassignedPagination",
      "function markFirstMenuLink",
    ]) {
      expect(code).toContain(helper);
    }
  });
});
