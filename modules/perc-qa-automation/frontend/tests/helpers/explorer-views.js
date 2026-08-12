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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Explorer Views Playwright helpers (#3117 / parent #3110 / #3102 / #2400).
 *
 * <p>Pure helpers for the V3 surface + a11y spec. Product catalog chrome
 * (V2 / #3116 / PR #3252) is <strong>not</strong> re-implemented here —
 * testids match {@code ViewsCatalogTree} / {@code ViewResultsPanel} when
 * that UI is on the build, and the spec soft-skips when it is absent.</p>
 *
 * <p>Do not import {@code explorer-views-catalog.js} (that helper lands
 * with V2). Keep this file self-contained so V3 can merge independently.</p>
 */

"use strict";

/** Stable product test ids for the Explorer Views surface (V2 chrome). */
const TEST_IDS = Object.freeze({
  shell: "content-explorer-shell",
  nav: "explorer-nav",
  viewsTree: "explorer-views-tree",
  viewsRoot: "explorer-views-root",
  viewsRootRow: "explorer-views-root-row",
  viewsLoading: "explorer-views-loading",
  viewsError: "explorer-views-error",
  viewsRetry: "explorer-views-retry",
  /** parentCategory 1 = My Content (DCE Views.html sys_category). */
  group: (n) => `explorer-views-group-${n}`,
  groupRow: (n) => `explorer-views-group-${n}-row`,
  leaf: (key) => `explorer-views-leaf-${key}`,
  inbox: "explorer-views-inbox",
  inboxLeaf: "explorer-views-leaf-Inbox",
  inboxIcon: "explorer-views-inbox-icon",
  results: "explorer-view-results",
  resultsHeading: "explorer-view-results-heading",
  resultsList: "explorer-view-results-list",
  resultsEmpty: "explorer-view-results-empty",
  resultsError: "explorer-view-results-error",
  resultsLoading: "explorer-view-results-loading",
  resultsRetry: "explorer-view-results-retry",
  resultRow: "explorer-view-result-row",
});

/** CX views catalog (same path as WebUI {@code PATHS.VIEWS} / ViewResource). */
const PATH_VIEWS = "/Rhythmyx/services/views";

/** My Content parentCategory in DCE / ViewDef. */
const PARENT_MY_CONTENT = 1;

/**
 * Build the modern Explorer SPA entry URL with cache-buster.
 *
 * @param {string} baseUrl CMS origin (no trailing slash required)
 * @param {{ cacheBuster?: string | number }} [opts]
 * @returns {string}
 */
function explorerEntryUrl(baseUrl, opts = {}) {
  const root = String(baseUrl || "")
    .trim()
    .replace(/\/+$/, "");
  const bust =
    opts.cacheBuster != null ? String(opts.cacheBuster) : String(Date.now());
  return `${root}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${encodeURIComponent(bust)}`;
}

/**
 * Absolute URL for GET design-view catalog.
 *
 * @param {string} baseUrl
 * @returns {string}
 */
function viewsCatalogUrl(baseUrl) {
  const root = String(baseUrl || "")
    .trim()
    .replace(/\/+$/, "");
  return `${root}${PATH_VIEWS}`;
}

/**
 * Absolute URL for POST execute of one standard view (V1 façade).
 *
 * @param {string} baseUrl
 * @param {string} idOrName
 * @returns {string}
 */
function viewsExecuteUrl(baseUrl, idOrName) {
  const root = String(baseUrl || "")
    .trim()
    .replace(/\/+$/, "");
  const key = encodeURIComponent(String(idOrName || "").trim());
  return `${root}${PATH_VIEWS}/${key}/execute`;
}

/**
 * Unwrap Jackson / list wrappers for {@code ViewDef[]} catalog payloads.
 *
 * @param {unknown} payload
 * @returns {Record<string, unknown>[]}
 */
function unwrapViewDefs(payload) {
  if (payload == null) {
    return [];
  }
  if (Array.isArray(payload)) {
    return payload.filter((x) => x != null && typeof x === "object");
  }
  if (typeof payload === "object") {
    const obj = /** @type {Record<string, unknown>} */ (payload);
    const raw =
      obj.ViewDef ?? obj.viewDef ?? obj.ViewDefList ?? obj.items ?? obj.views;
    if (raw == null) {
      return [];
    }
    if (Array.isArray(raw)) {
      return raw.filter((x) => x != null && typeof x === "object");
    }
    if (typeof raw === "object") {
      return [/** @type {Record<string, unknown>} */ (raw)];
    }
  }
  return [];
}

/**
 * Stable leaf testid key for a ViewDef (matches V2 {@code viewKey}).
 *
 * @param {Record<string, unknown> | null | undefined} def
 * @returns {string}
 */
function viewDefKey(def) {
  if (def == null || typeof def !== "object") {
    return "";
  }
  const name = def.name != null ? String(def.name).trim() : "";
  if (name) {
    return name;
  }
  const id = def.id != null ? String(def.id).trim() : "";
  return id;
}

/**
 * Human label for a ViewDef.
 *
 * @param {Record<string, unknown> | null | undefined} def
 * @returns {string}
 */
function viewDefLabel(def) {
  if (def == null || typeof def !== "object") {
    return "";
  }
  const label =
    def.label != null
      ? String(def.label).trim()
      : def.displayName != null
        ? String(def.displayName).trim()
        : "";
  if (label) {
    return label;
  }
  return viewDefKey(def);
}

/**
 * True when the view is a custom-URL view (Inbox family — not V3 execute).
 *
 * @param {Record<string, unknown> | null | undefined} def
 * @returns {boolean}
 */
function isCustomUrlView(def) {
  if (def == null || typeof def !== "object") {
    return false;
  }
  return def.customView === true || def.customView === "true";
}

/**
 * True when the def is the Inbox custom view (not a standard-view execute).
 *
 * @param {Record<string, unknown> | null | undefined} def
 * @returns {boolean}
 */
function isInboxView(def) {
  if (def == null || typeof def !== "object") {
    return false;
  }
  const name = def.name != null ? String(def.name).trim() : "";
  const label = viewDefLabel(def);
  if (name.toLowerCase() === "inbox" || label.toLowerCase() === "inbox") {
    return true;
  }
  const key = viewDefKey(def).replace(/\\/g, "/");
  return /\/\/Views\/\/MyContent\/Inbox$/i.test(key);
}

/**
 * Numeric parentCategory (1=My Content, 2=Community, 3=All, 4=Other).
 *
 * @param {Record<string, unknown> | null | undefined} def
 * @returns {number}
 */
function viewParentCategory(def) {
  if (def == null || typeof def !== "object") {
    return 0;
  }
  const raw = def.parentCategory;
  const n = typeof raw === "number" ? raw : Number.parseInt(String(raw), 10);
  return Number.isFinite(n) ? n : 0;
}

/**
 * Pick the first runnable standard (non-custom-URL) view.
 * Prefers My Content (parentCategory=1), then any other standard view.
 *
 * @param {unknown} payload raw REST body or ViewDef[]
 * @returns {{ key: string, label: string, parentCategory: number, def: Record<string, unknown> } | null}
 */
function pickRunnableStandardView(payload) {
  const defs = unwrapViewDefs(payload);
  let first = null;
  for (const def of defs) {
    if (isCustomUrlView(def) || isInboxView(def)) {
      continue;
    }
    const key = viewDefKey(def);
    if (!key) {
      continue;
    }
    const picked = {
      key,
      label: viewDefLabel(def) || key,
      parentCategory: viewParentCategory(def),
      def,
    };
    if (picked.parentCategory === PARENT_MY_CONTENT) {
      return picked;
    }
    if (first == null) {
      first = picked;
    }
  }
  return first;
}

/**
 * Soft-skip when V2 Views catalog chrome is not on the build under test.
 *
 * @returns {string}
 */
function noViewsChromeSkipMessage() {
  return [
    "V2 Explorer Views catalog chrome not on this build (#3117 soft-skip).",
    "Expected data-testid=explorer-views-tree after Explorer shell load.",
    "Depends on #3116 / PR #3252 (do not treat as a product UI failure).",
  ].join(" ");
}

/**
 * Soft-skip when QA fixture has no runnable standard views.
 *
 * @param {{ empty?: boolean, onlyCustom?: boolean, restStatus?: number }} [detail]
 * @returns {string}
 */
function noRunnableViewSkipMessage(detail = {}) {
  const parts = [
    "No runnable standard view in fixture for Explorer Views E2E (#3117).",
    "Catalog empty or only custom-URL views — soft skip after asserting Views chrome.",
  ];
  if (detail.empty) {
    parts.push("catalog empty.");
  }
  if (detail.onlyCustom) {
    parts.push("only customView entries.");
  }
  if (detail.restStatus != null) {
    parts.push(`REST status=${detail.restStatus}.`);
  }
  return parts.join(" ");
}

/**
 * CSS selector joining post-execute result regions.
 *
 * @returns {string}
 */
function postExecuteRegionSelector() {
  return [
    `[data-testid="${TEST_IDS.results}"]`,
    `[data-testid="${TEST_IDS.resultsLoading}"]`,
    `[data-testid="${TEST_IDS.resultsError}"]`,
    `[data-testid="${TEST_IDS.resultsEmpty}"]`,
    `[data-testid="${TEST_IDS.resultsList}"]`,
  ].join(", ");
}

/**
 * CSS selector for mounted Views catalog chrome (loading / error / tree root).
 *
 * @returns {string}
 */
function viewsChromeSelector() {
  return [
    `[data-testid="${TEST_IDS.viewsTree}"]`,
    `[data-testid="${TEST_IDS.viewsLoading}"]`,
    `[data-testid="${TEST_IDS.viewsError}"]`,
    `[data-testid="${TEST_IDS.viewsRoot}"]`,
  ].join(", ");
}

/**
 * Ignore known fixture/network console noise (404/400 static probes).
 *
 * @param {string} text
 * @returns {boolean}
 */
function isIgnorableConsoleError(text) {
  const s = String(text || "");
  return /Failed to load resource: the server responded with a status of (404|400)/i.test(
    s,
  );
}

/**
 * Attach pageerror + console error collectors. Returns the shared array.
 *
 * @param {import("@playwright/test").Page} page
 * @returns {string[]}
 */
function attachConsoleErrorCollector(page) {
  const consoleErrors = [];
  page.on("pageerror", (err) => {
    consoleErrors.push(String(err && err.message ? err.message : err));
  });
  page.on("console", (msg) => {
    if (msg.type() !== "error") {
      return;
    }
    const text = msg.text();
    if (isIgnorableConsoleError(text)) {
      return;
    }
    consoleErrors.push(text);
  });
  return consoleErrors;
}

module.exports = {
  TEST_IDS,
  PATH_VIEWS,
  PARENT_MY_CONTENT,
  explorerEntryUrl,
  viewsCatalogUrl,
  viewsExecuteUrl,
  unwrapViewDefs,
  viewDefKey,
  viewDefLabel,
  isCustomUrlView,
  isInboxView,
  viewParentCategory,
  pickRunnableStandardView,
  noViewsChromeSkipMessage,
  noRunnableViewSkipMessage,
  postExecuteRegionSelector,
  viewsChromeSelector,
  isIgnorableConsoleError,
  attachConsoleErrorCollector,
};
